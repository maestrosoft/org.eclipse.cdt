/**********************************************************************
 * Copyright (c) 2002,2003 QNX Software Systems and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
***********************************************************************/
package org.eclipse.cdt.make.internal.core.makefile;

import java.io.File;

import org.eclipse.cdt.make.core.makefile.*;

public class Target implements ITarget {

	String target;

	public Target(String t) {
		target = t;
	}

	public String toString() {
		return target;
	}

	public boolean exits() {
		return new File(target).exists();
	}

	public int make(boolean echo) {
		return 0;
	}

	public boolean isUptodate() {
		return false;
	}

	public long lastModified() {
		return new File(target).lastModified();
	}
}
