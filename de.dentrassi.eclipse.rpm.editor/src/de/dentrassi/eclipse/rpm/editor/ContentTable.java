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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ContentTable {

	private final TreeViewer viewer;
	private final Composite wrapper;

	private static abstract class Node {

		private final String name;

		public Node(final String name, final Node parent) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public abstract long getSize();

		public abstract Object[] getChildren();
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

	public ContentTable(final Composite parent) {
		this.wrapper = new Composite(parent, SWT.NO_BACKGROUND);
		parent.setLayout(new FillLayout());

		this.viewer = new TreeViewer(this.wrapper, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		final TreeColumnLayout layout = new TreeColumnLayout();

		{
			final TreeViewerColumn col = new TreeViewerColumn(this.viewer, SWT.NONE);
			col.getColumn().setText("Name");
			col.setLabelProvider(new CellLabelProvider() {

				@Override
				public void update(final ViewerCell cell) {
					final Object ele = cell.getElement();
					if (ele instanceof Node) {
						cell.setText(((Node) ele).getName());
					}
				}
			});
			layout.setColumnData(col.getColumn(), new ColumnWeightData(5));
		}

		{
			final TreeViewerColumn col = new TreeViewerColumn(this.viewer, SWT.NONE);
			col.getColumn().setText("Size");
			col.setLabelProvider(new CellLabelProvider() {

				@Override
				public void update(final ViewerCell cell) {
					final Object ele = cell.getElement();
					if (ele instanceof Node) {
						cell.setText(NumberFormat.getIntegerInstance().format(((Node) ele).getSize()));
					}
				}
			});
			layout.setColumnData(col.getColumn(), new ColumnWeightData(1));
		}

		{
			final TreeViewerColumn col = new TreeViewerColumn(this.viewer, SWT.NONE);
			col.getColumn().setText("Timestamp");
			col.setLabelProvider(new CellLabelProvider() {

				@Override
				public void update(final ViewerCell cell) {
					final Object ele = cell.getElement();
					if (ele instanceof File) {
						cell.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
								.format(((File) ele).getFile().getTimestamp().atZone(ZoneId.systemDefault())));
					}
				}
			});
			layout.setColumnData(col.getColumn(), new ColumnWeightData(2));
		}

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

	public void setInformation(final List<FileEntry> content) {
		final Directory root = new Directory(null, null);

		for (final FileEntry fe : content) {
			final String s = fe.getName().replaceFirst("^\\.\\/", "");
			final String[] toks = s.split("\\/");
			final LinkedList<String> segs = new LinkedList<>(Arrays.asList(toks));

			root.addFile(segs, fe);
		}

		this.viewer.setInput(root);
		this.viewer.getTree().layout();
	}

	public Control getContainer() {
		return this.wrapper;
	}

}
