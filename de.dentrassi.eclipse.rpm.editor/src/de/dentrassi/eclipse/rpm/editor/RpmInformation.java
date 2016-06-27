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

import java.util.List;

import org.eclipse.packagedrone.utils.rpm.RpmLead;
import org.eclipse.packagedrone.utils.rpm.RpmSignatureTag;
import org.eclipse.packagedrone.utils.rpm.RpmTag;
import org.eclipse.packagedrone.utils.rpm.parse.InputHeader;

public class RpmInformation {
	private final RpmLead lead;
	private final InputHeader<RpmTag> header;
	private final InputHeader<RpmSignatureTag> signatureHeader;
	private final List<FileEntry> files;

	public RpmInformation(final RpmLead lead, final InputHeader<RpmTag> header,
			final InputHeader<RpmSignatureTag> sigHeader, final List<FileEntry> files) {
		this.lead = lead;
		this.header = header;
		this.signatureHeader = sigHeader;
		this.files = files;
	}

	public RpmLead getLead() {
		return this.lead;
	}

	public InputHeader<RpmTag> getHeader() {
		return this.header;
	}

	public InputHeader<RpmSignatureTag> getSignatureHeader() {
		return this.signatureHeader;
	}

	public List<FileEntry> getFiles() {
		return this.files;
	}
}
