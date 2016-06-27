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

import java.time.Instant;

public class FileEntry {

	private final String name;
	private final long size;
	private final Instant timestamp;

	public FileEntry(final String name, final long size, final Instant timestamp) {
		this.name = name;
		this.size = size;
		this.timestamp = timestamp;
	}

	public String getName() {
		return this.name;
	}

	public long getSize() {
		return this.size;
	}

	public Instant getTimestamp() {
		return this.timestamp;
	}

}
