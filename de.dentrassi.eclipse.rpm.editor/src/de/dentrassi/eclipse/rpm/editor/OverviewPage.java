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

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.packagedrone.utils.rpm.Architecture;
import org.eclipse.packagedrone.utils.rpm.OperatingSystem;
import org.eclipse.packagedrone.utils.rpm.RpmLead;
import org.eclipse.packagedrone.utils.rpm.Type;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class OverviewPage {
	private final Composite container;
	private final Text type;
	private final Text arch;
	private final Text version;
	private final Text os;
	private final Text name;
	private final Text sigVersion;

	public OverviewPage(final Composite parent) {
		this.container = new Composite(parent, SWT.INHERIT_DEFAULT);
		this.container.setLayout(new GridLayout(2, false));

		this.name = createField("Name");
		this.type = createField("Type");
		this.arch = createField("Architecture");
		this.os = createField("O/S");
		this.version = createField("Version");
		this.sigVersion = createField("Signature Version");
	}

	private Text createField(final String string) {
		Label label;
		label = new Label(this.container, SWT.NONE);

		label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		label.setText(string);

		final Text text = new Text(this.container, SWT.BORDER | SWT.READ_ONLY);

		text.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

		return text;
	}

	public void setInformation(final RpmInformation info) {
		final RpmLead lead = info.getLead();

		this.name.setText(lead.getName());
		this.type.setText(makeEnumFormat(Type::fromValue, () -> (int) lead.getType(), "0x%02X"));
		this.arch.setText(makeEnumFormat(Architecture::fromValue, () -> (int) lead.getArchitecture(), "0x%02X"));
		this.os.setText(makeEnumFormat(OperatingSystem::fromValue, () -> (int) lead.getOperatingSystem(), "0x%02X"));

		this.version.setText(String.format("%s.%s", lead.getMajor(), lead.getMinor()));
		this.sigVersion.setText(String.format("%d", lead.getSignatureVersion()));
	}

	private <T, E extends Enum<?>> String makeEnumFormat(final Function<T, Optional<E>> from,
			final Supplier<T> supplier, final String format) {
		return makeEnum(from, supplier, (v) -> String.format(format, v));
	}

	private <T, E extends Enum<?>> String makeEnum(final Function<T, Optional<E>> from, final Supplier<T> supplier,
			final Function<T, String> cvt) {
		final T value = supplier.get();
		final Optional<E> entry = from.apply(value);
		if (entry.isPresent()) {
			return String.format("%s (%s)", entry.get().name(), cvt.apply(value));
		} else {
			return cvt.apply(value);
		}
	}

	public Composite getContainer() {
		return this.container;
	}
}
