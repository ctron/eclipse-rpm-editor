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

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.packagedrone.utils.rpm.deps.Dependencies;
import org.eclipse.packagedrone.utils.rpm.deps.Dependency;
import org.eclipse.packagedrone.utils.rpm.deps.RpmDependencyFlags;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class DependenciesTable {

	private final TreeViewer viewer;
	private final Composite wrapper;

	public DependenciesTable(final Composite parent) {
		this.wrapper = new Composite(parent, SWT.NO_BACKGROUND);
		parent.setLayout(new FillLayout());

		this.viewer = new TreeViewer(this.wrapper, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);

		final TreeColumnLayout layout = new TreeColumnLayout();

		{
			final TreeViewerColumn col = new TreeViewerColumn(this.viewer, SWT.NONE);
			col.getColumn().setText("Name");
			col.setLabelProvider(new CellLabelProvider() {

				@Override
				public void update(final ViewerCell cell) {
					final Object ele = cell.getElement();
					if (ele instanceof Map.Entry<?, ?>) {
						final String label = ((Map.Entry<?, ?>) ele).getKey().toString();
						cell.setText(label);
					} else if (ele instanceof Dependency) {
						final Dependency dep = (Dependency) ele;
						cell.setText(dep.getName());
					}
				}
			});
			layout.setColumnData(col.getColumn(), new ColumnWeightData(5));
		}

		{
			final TreeViewerColumn col = new TreeViewerColumn(this.viewer, SWT.NONE);
			col.getColumn().setText("Op");
			col.setLabelProvider(new CellLabelProvider() {

				@Override
				public void update(final ViewerCell cell) {
					final Object ele = cell.getElement();
					if (ele instanceof Dependency) {
						final Dependency dep = (Dependency) ele;

						final EnumSet<RpmDependencyFlags> flags = dep.getFlags();

						final StringBuilder sb = new StringBuilder();

						for (final RpmDependencyFlags flag : flags) {
							switch (flag) {
							case LESS:
								sb.append("<");
								break;
							case EQUAL:
								sb.append("=");
								break;
							case GREATER:
								sb.append(">");
								break;
							default:
								break;
							}
						}

						cell.setText(sb.toString());
					}
				}
			});
			layout.setColumnData(col.getColumn(), new ColumnWeightData(1));
		}

		{
			final TreeViewerColumn col = new TreeViewerColumn(this.viewer, SWT.NONE);
			col.getColumn().setText("Version");
			col.setLabelProvider(new CellLabelProvider() {

				@Override
				public void update(final ViewerCell cell) {
					final Object ele = cell.getElement();
					if (ele instanceof Dependency) {
						final Dependency dep = (Dependency) ele;
						cell.setText(dep.getVersion());
					}
				}
			});
			layout.setColumnData(col.getColumn(), new ColumnWeightData(5));
		}

		{
			final TreeViewerColumn col = new TreeViewerColumn(this.viewer, SWT.NONE);
			col.getColumn().setText("Flags");
			col.setLabelProvider(new CellLabelProvider() {

				@Override
				public void update(final ViewerCell cell) {
					final Object ele = cell.getElement();
					if (ele instanceof Dependency) {
						final Dependency dep = (Dependency) ele;

						final EnumSet<RpmDependencyFlags> flags = dep.getFlags();

						final String text = flags.stream().map(RpmDependencyFlags::name)
								.collect(Collectors.joining(", "));

						cell.setText(text);
					}
				}
			});
			layout.setColumnData(col.getColumn(), new ColumnWeightData(5));
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
				if (inputElement instanceof Map<?, ?>) {
					return ((Map<?, ?>) inputElement).entrySet().toArray();
				} else if (inputElement instanceof Map.Entry<?, ?>) {
					final Object val = ((Map.Entry<?, ?>) inputElement).getValue();
					if (val instanceof Collection<?>) {
						return ((Collection<?>) val).toArray();
					}
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

	public void setInformation(final RpmInformation ri) {

		final List<Dependency> req = Dependencies.getRequirements(ri.getHeader());
		final List<Dependency> prov = Dependencies.getProvides(ri.getHeader());
		final List<Dependency> conf = Dependencies.getConflicts(ri.getHeader());
		final List<Dependency> obs = Dependencies.getObsoletes(ri.getHeader());

		final Map<String, List<Dependency>> entries = new LinkedHashMap<>();
		entries.put("Requirements", req);
		entries.put("Provides", prov);
		entries.put("Conflicts", conf);
		entries.put("Obsoletes", obs);

		this.viewer.setAutoExpandLevel(2);
		this.viewer.setInput(entries);
		this.viewer.getTree().layout();
	}

	public Control getContainer() {
		return this.wrapper;
	}

}
