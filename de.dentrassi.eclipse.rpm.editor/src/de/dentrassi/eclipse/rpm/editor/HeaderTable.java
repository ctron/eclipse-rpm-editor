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

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.layout.AbstractColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.packagedrone.utils.rpm.Rpms;
import org.eclipse.packagedrone.utils.rpm.parse.HeaderValue;
import org.eclipse.packagedrone.utils.rpm.parse.InputHeader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class HeaderTable {
	private static class Entry implements Comparable<Entry> {

		private final int key;
		private final HeaderValue value;

		public Entry(final int key, final HeaderValue value) {
			this.key = key;
			this.value = value;
		}

		public int getKey() {
			return this.key;
		}

		public HeaderValue getValue() {
			return this.value;
		}

		@Override
		public int compareTo(final Entry other) {
			return Integer.compare(this.key, other.key);
		}
	}

	private final TreeViewer viewer;
	private final Composite wrapper;

	public HeaderTable(final Composite parent, final Function<Integer, Object> tagNameProvider) {
		this.wrapper = new Composite(parent, SWT.NO_BACKGROUND);
		parent.setLayout(new FillLayout());

		this.viewer = new TreeViewer(this.wrapper,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		final TreeColumnLayout layout = new TreeColumnLayout();

		createColumn(layout, "Tag ID", 1, entry -> String.format("%d", entry.getKey()));
		createColumnCell(layout, "Tag Name", 2, (entry, cell) -> {
			final Object name = tagNameProvider.apply(entry.getKey());
			if (name != null) {
				cell.setText(name.toString());
			}
		});

		createColumn(layout, "Type", 1, entry -> String.format("%d", entry.getValue().getType()));
		createColumn(layout, "Count", 1, entry -> String.format("%d", entry.getValue().getCount()));
		createColumn(layout, "Index", 1, entry -> String.format("%d", entry.getValue().getIndex()));
		createColumnCell(layout, "Value", 10, HeaderTable::updateCellValue);

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
				} else if (inputElement instanceof Entry) {
					final Entry entry = (Entry) inputElement;
					final Object value = entry.getValue().getValue();
					final Object[] childs = makeObjects(value).map(Stream::toArray).orElse(null);
					if (childs != null && childs.length <= 1) {
						return null;
					}
					return childs;
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

	private static Optional<Stream<?>> makeObjects(final Object value) {
		if (value instanceof Short[]) {
			return of(Arrays.stream((Short[]) value));
		} else if (value instanceof Integer[]) {
			return of(Arrays.stream((Integer[]) value));
		} else if (value instanceof Long[]) {
			return of(Arrays.stream((Long[]) value));
		} else if (value instanceof String[]) {
			return of(Arrays.stream((String[]) value));
		}

		return empty();
	}

	private static void updateCellValue(final ViewerCell cell) {

		final Object ele = cell.getElement();

		if (ele instanceof Entry) {
			final Entry entry = (Entry) ele;
			final Object value = entry.getValue().getValue();

			if (value instanceof byte[]) {
				final byte[] data = (byte[]) value;
				final String suffix = data.length > 200 ? "…" : "";
				cell.setText(Rpms.toHex(data, 200) + suffix);
			} else if (value != null) {
				final Optional<Stream<?>> os = makeObjects(value);
				if (os.isPresent()) {
					final Stream<?> s = os.get();
					cell.setText(s.map(Object::toString).collect(Collectors.joining(", ")));
				} else {
					cell.setText(value.toString());
				}
			}

		} else if (ele != null) {
			cell.setText(ele.toString());
		}
	}

	private void createColumn(final AbstractColumnLayout layout, final String name, final int weight,
			final Function<Entry, String> label) {
		createColumnCell(layout, name, weight, (entry, cell) -> {
			final String s = label.apply(entry);
			if (s != null) {
				cell.setText(s);
			}
		});
	}

	private void createColumnCell(final AbstractColumnLayout layout, final String name, final int weight,
			final Consumer<ViewerCell> cellUpdater) {
		final TreeViewerColumn col = new TreeViewerColumn(this.viewer, SWT.NONE);
		col.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {
				cellUpdater.accept(cell);
			}
		});
		col.getColumn().setText(name);
		layout.setColumnData(col.getColumn(), new ColumnWeightData(weight));
	}

	private void createColumnCell(final AbstractColumnLayout layout, final String name, final int weight,
			final BiConsumer<Entry, ViewerCell> cellUpdater) {

		createColumnCell(layout, name, weight, cell -> {
			if (cell.getElement() instanceof Entry) {
				final Entry entry = (Entry) cell.getElement();
				cellUpdater.accept(entry, cell);
			}
		});
	}

	public void setInformation(final InputHeader<?> header) {
		this.viewer.setInput(header.getRawTags().entrySet().stream().map(HeaderTable::toEntry).toArray());
		this.viewer.getTree().layout();
	}

	private static Entry toEntry(final Map.Entry<Integer, HeaderValue> entry) {
		final Entry result = new Entry(entry.getKey(), entry.getValue());
		return result;
	}

	public Control getContainer() {
		return this.wrapper;
	}

}
