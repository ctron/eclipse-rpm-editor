/*******************************************************************************
 * Copyright (c) 2016 IBH SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package de.dentrassi.eclipse.rpm.editor;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.packagedrone.utils.rpm.RpmTag;
import org.eclipse.packagedrone.utils.rpm.parse.HeaderValue;
import org.eclipse.packagedrone.utils.rpm.parse.InputHeader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ContentTable {

	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);
	private final TreeViewer viewer;
	private final Composite wrapper;
	private Map<String, MetaInformation> meta;
	private final LocalResourceManager resourceManager;
	private Color dimmedColor;

	private static abstract class Node {

		private final String name;
		private final Node parent;

		public Node(final String name, final Node parent) {
			this.name = name;
			this.parent = parent;
		}

		public String getName() {
			return this.name;
		}

		public abstract long getSize();

		public abstract Object[] getChildren();

		public String getFullName() {
			if (this.parent != null) {
				return this.parent.getFullName() + "/" + this.name;
			} else if (this.name != null) {
				return "/" + this.name;
			} else {
				return "";
			}
		}
	}

	private static class File extends Node {
		private final FileEntry file;

		public File(final String name, final Node parent, final FileEntry file) {
			super(name, parent);
			this.file = file;
		}

		@Override
		public Object[] getChildren() {
			return null;
		}

		public FileEntry getFile() {
			return this.file;
		}

		@Override
		public long getSize() {
			return this.file.getSize();
		}
	}

	private static class Directory extends Node {
		private final Map<String, Node> nodes = new TreeMap<>();

		private long size;

		public Directory(final String name, final Node parent) {
			super(name, parent);
		}

		@Override
		public Object[] getChildren() {
			return this.nodes.values().toArray();
		}

		public void addFile(final LinkedList<String> segs, final FileEntry fe) {
			final String seg = segs.pollFirst();
			this.size += fe.getSize();
			if (segs.isEmpty()) {
				this.nodes.put(seg, new File(seg, this, fe));
			} else {
				final Node node = this.nodes.get(seg);
				if (node instanceof Directory) {
					((Directory) node).addFile(segs, fe);
				} else {
					final Directory dir = new Directory(seg, this);
					this.nodes.put(seg, dir);
					dir.addFile(segs, fe);
				}
			}
		}

		@Override
		public long getSize() {
			return this.size;
		}
	}

	public static class MetaInformation {
		private final String user;
		private final String group;
		private final Short mode;
		private final String linkTo;
		private final Instant timestamp;

		public MetaInformation(final String user, final String group, final Short mode, final String linkTo,
				final Integer mtime) {
			this.user = user;
			this.group = group;
			this.mode = mode;
			this.linkTo = linkTo;
			this.timestamp = mtime != null ? Instant.ofEpochSecond(mtime) : null;
		}

		public String getGroup() {
			return this.group;
		}

		public String getUser() {
			return this.user;
		}

		public Short getMode() {
			return this.mode;
		}

		public String getLinkTo() {
			return this.linkTo;
		}

		public Instant getTimestamp() {
			return this.timestamp;
		}
	}

	public ContentTable(final Composite parent) {
		this.wrapper = new Composite(parent, SWT.NO_BACKGROUND);
		parent.setLayout(new FillLayout());

		this.resourceManager = new LocalResourceManager(JFaceResources.getResources(parent.getDisplay()));
		this.wrapper.addDisposeListener((evt) -> this.resourceManager.dispose());

		this.dimmedColor = this.resourceManager.createColor(new RGB(127, 127, 127));
		final ImageDescriptor fileIconDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
				"$nl$/icons/obj16/file.png"); //$NON-NLS-1$
		final ImageDescriptor folderIconDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
				"$nl$/icons/obj16/folder.png"); //$NON-NLS-1$
		final ImageDescriptor linkIconDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
				"$nl$/icons/obj16/link.png"); //$NON-NLS-1$

		this.viewer = new TreeViewer(this.wrapper, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		final TreeColumnLayout layout = new TreeColumnLayout();

		createColumn(layout, "Name", 4, SWT.NONE, (node, cell) -> {
			cell.setText(node.getName());
			final MetaInformation meta = ContentTable.this.meta.get(node.getFullName());
			if (meta != null && meta.getMode() != null) {
				if ((meta.getMode() & 0x8000) > 0) {
					cell.setImage(this.resourceManager.createImage(fileIconDescriptor));
				} else if ((meta.getMode() & 0x4000) > 0) {
					cell.setImage(this.resourceManager.createImage(folderIconDescriptor));
				} else if ((meta.getMode() & 0xA000) > 0) {
					cell.setImage(this.resourceManager.createImage(linkIconDescriptor));
				}
			}
		});

		createColumn(layout, "Size", 1, SWT.RIGHT,
				(node, cell) -> cell.setText(NumberFormat.getIntegerInstance().format(node.getSize())));

		createMetaColumn(layout, "User", 1, SWT.NONE, (meta, cell) -> cell.setText(meta.getUser()));
		createMetaColumn(layout, "Group", 1, SWT.NONE, (meta, cell) -> cell.setText(meta.getGroup()));
		createMetaColumn(layout, "Mode", 1, SWT.NONE, (meta, cell) -> cell.setText(makeMode(meta.getMode())));

		createMetaColumn(layout, "Link", 1, SWT.NONE, (meta, cell) -> cell.setText(meta.getLinkTo()));

		createColumn(layout, "Timestamp", 1, SWT.NONE, (node, cell) -> {
			Instant ts = null;

			final MetaInformation meta = ContentTable.this.meta.get(node.getFullName());
			if (meta != null) {
				// use meta entry
				ts = meta.getTimestamp();
			}
			if (ts == null && node instanceof File) {
				// use CPIO entry
				ts = ((File) node).getFile().getTimestamp();
			}

			if (ts != null) {
				cell.setText(DATETIME_FORMATTER.format(ts.atZone(ZoneId.systemDefault())));
			}
		});

		this.wrapper.setLayout(layout);
		this.viewer.getTree().setHeaderVisible(true);

		final ITreePathContentProvider p = new ITreePathContentProvider() {

			@Override
			public boolean hasChildren(final TreePath path) {
				final Object[] children = getChildren(path);
				return children != null && children.length > 0;
			}

			@Override
			public TreePath[] getParents(final Object element) {
				return new TreePath[0];
			}

			@Override
			public Object[] getElements(final Object inputElement) {
				if (inputElement instanceof Object[]) {
					return (Object[]) inputElement;
				} else if (inputElement instanceof Directory) {
					return ((Directory) inputElement).getChildren();
				}
				return null;
			}

			@Override
			public Object[] getChildren(final TreePath parentPath) {
				return getElements(parentPath.getLastSegment());
			}
		};
		this.viewer.setContentProvider(p);
	}

	private String makeMode(Short mode) {
		if (mode == null) {
			return "";
		}

		mode = (short) (mode & (short) 07777);

		return String.format("%04o", mode);
	}

	private void createMetaColumn(final TreeColumnLayout layout, final String label, final int weight, final int flags,
			final BiConsumer<MetaInformation, ViewerCell> consumer) {
		createColumn(layout, label, weight, flags, (node, cell) -> {
			final MetaInformation meta = ContentTable.this.meta.get(node.getFullName());
			if (meta != null) {
				consumer.accept(meta, cell);
			}
		});
	}

	private void createColumn(final TreeColumnLayout layout, final String label, final int weight, final int flags,
			final BiConsumer<Node, ViewerCell> consumer) {
		{
			final TreeViewerColumn col = new TreeViewerColumn(this.viewer, flags);
			col.getColumn().setText(label);
			col.setLabelProvider(new CellLabelProvider() {

				@Override
				public void update(final ViewerCell cell) {
					final Object ele = cell.getElement();
					if (ele instanceof Node) {
						final MetaInformation meta = ContentTable.this.meta.get(((Node) ele).getFullName());
						if (meta == null) {
							cell.setForeground(ContentTable.this.dimmedColor);
						}
						consumer.accept((Node) ele, cell);
					}
				}
			});
			layout.setColumnData(col.getColumn(), new ColumnWeightData(weight));
		}
	}

	public void setInformation(final RpmInformation ri) {
		final Directory root = new Directory(null, null);

		this.meta = buildMetaInformation(ri);

		for (final FileEntry fe : ri.getFiles()) {
			final String s = fe.getName().replaceFirst("^\\.\\/", "");
			final String[] toks = s.split("\\/");
			final LinkedList<String> segs = new LinkedList<>(Arrays.asList(toks));

			root.addFile(segs, fe);
		}

		this.viewer.setInput(root);
		this.viewer.getTree().layout();
	}

	private <T> Optional<T[]> fileArray(final InputHeader<RpmTag> header, final RpmTag tag, final Class<T[]> clazz) {
		final Optional<HeaderValue> val = header.getEntry(tag);
		if (!val.isPresent()) {
			return Optional.empty();
		}

		final Object o = val.get().getValue();
		if (o == null) {
			return Optional.empty();
		}

		if (clazz.isAssignableFrom(o.getClass())) {
			return Optional.of(clazz.cast(o));
		}
		return Optional.empty();
	}

	private <T> T from(final Optional<T[]> values, final int i) {
		if (!values.isPresent()) {
			return null;
		}

		final T[] array = values.get();
		if (i < array.length) {
			return array[i];
		} else {
			return null;
		}
	}

	private Map<String, MetaInformation> buildMetaInformation(final RpmInformation ri) {
		final Optional<HeaderValue> basenamesValue = ri.getHeader().getEntry(RpmTag.BASENAMES);
		final Optional<HeaderValue> dirnamesValue = ri.getHeader().getEntry(RpmTag.DIRNAMES);
		final Optional<HeaderValue> dirIdxValue = ri.getHeader().getEntry(RpmTag.DIR_INDEXES);

		if (!basenamesValue.isPresent() || !dirnamesValue.isPresent() || !dirIdxValue.isPresent()) {
			return Collections.emptyMap();
		}
		if (!(dirnamesValue.get().getValue() instanceof String[])) {
			return Collections.emptyMap();
		}
		if (!(dirIdxValue.get().getValue() instanceof Integer[])) {
			return Collections.emptyMap();
		}

		final String[] basenames = (String[]) basenamesValue.get().getValue();
		final String[] dirnames = (String[]) dirnamesValue.get().getValue();
		final Integer[] dirIdx = (Integer[]) dirIdxValue.get().getValue();

		final Optional<String[]> users = fileArray(ri.getHeader(), RpmTag.FILE_USERNAME, String[].class);
		final Optional<String[]> groups = fileArray(ri.getHeader(), RpmTag.FILE_GROUPNAME, String[].class);
		final Optional<Short[]> modes = fileArray(ri.getHeader(), RpmTag.FILE_MODES, Short[].class);

		final Optional<String[]> links = fileArray(ri.getHeader(), RpmTag.FILE_LINKTO, String[].class);

		final Optional<Integer[]> mtimes = fileArray(ri.getHeader(), RpmTag.FILE_MTIMES, Integer[].class);

		final Map<String, MetaInformation> metaInformation = new HashMap<>();

		for (int i = 0; i < basenames.length; i++) {
			final String basename = basenames[i];
			final String dirname = dirnames[dirIdx[i]];
			final String name;
			if (dirname.endsWith("/")) {
				name = dirname + basename;
			} else {
				name = dirname + "/" + basename;
			}

			metaInformation.put(name, new MetaInformation(from(users, i), from(groups, i), from(modes, i),
					from(links, i), from(mtimes, i)));
		}

		return metaInformation;
	}

	public Control getContainer() {
		return this.wrapper;
	}

}
