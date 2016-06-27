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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.packagedrone.utils.rpm.RpmLead;
import org.eclipse.packagedrone.utils.rpm.RpmSignatureTag;
import org.eclipse.packagedrone.utils.rpm.RpmTag;
import org.eclipse.packagedrone.utils.rpm.parse.InputHeader;
import org.eclipse.packagedrone.utils.rpm.parse.RpmInputStream;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

public class EditorImpl extends MultiPageEditorPart {

	private OverviewPage overviewPage;
	private RpmInformation information;
	private HeaderTable headerPage;
	private HeaderTable sigHeaderPage;
	private ContentTable contentPage;

	public EditorImpl() {
	}

	@Override
	protected void createPages() {
		createOverviewPage();
		createHeaderPage();
		createSignatureHeaderPage();
		createContentPage();

		if (this.information != null) {
			setInformation(this.information);
		}
	}

	private void createHeaderPage() {
		this.headerPage = new HeaderTable(getContainer(), RpmTag::find);
		final int idx = addPage(this.headerPage.getContainer());
		setPageText(idx, "Header");
	}

	private void createSignatureHeaderPage() {
		this.sigHeaderPage = new HeaderTable(getContainer(), RpmSignatureTag::find);
		final int idx = addPage(this.sigHeaderPage.getContainer());
		setPageText(idx, "Signature Header");
	}

	private void createOverviewPage() {
		this.overviewPage = new OverviewPage(getContainer());
		final int idx = addPage(this.overviewPage.getContainer());
		setPageText(idx, "Lead");
	}

	private void createContentPage() {
		this.contentPage = new ContentTable(getContainer());
		final int idx = addPage(this.contentPage.getContainer());
		setPageText(idx, "Payload");
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	protected void setInput(final IEditorInput input) {
		super.setInput(input);
		if (input instanceof IPathEditorInput) {
			final IPath path = ((IPathEditorInput) input).getPath();
			final RpmInformation ri = load(path.toFile().toPath());
			setInformation(ri);
		}
	}

	private void setInformation(final RpmInformation ri) {
		if (this.overviewPage != null) {
			this.overviewPage.setInformation(ri);
			this.headerPage.setInformation(ri.getHeader());
			this.sigHeaderPage.setInformation(ri.getSignatureHeader());
			this.contentPage.setInformation(ri.getFiles());
		}
		this.information = ri;
	}

	private RpmInformation load(final Path path) {
		try (RpmInputStream in = new RpmInputStream(Files.newInputStream(path))) {
			final RpmLead lead = in.getLead();

			final InputHeader<RpmTag> header = in.getPayloadHeader();
			final InputHeader<RpmSignatureTag> sigHeader = in.getSignatureHeader();

			final CpioArchiveInputStream cpio = in.getCpioStream();

			ArchiveEntry entry;

			final List<FileEntry> files = new ArrayList<>();
			while ((entry = cpio.getNextEntry()) != null) {
				final FileEntry fe = new FileEntry(entry.getName(), entry.getSize(),
						entry.getLastModifiedDate().toInstant());
				files.add(fe);
			}

			return new RpmInformation(lead, header, sigHeader, files);
		} catch (final IOException e) {
			return null;
		}
	}
}
