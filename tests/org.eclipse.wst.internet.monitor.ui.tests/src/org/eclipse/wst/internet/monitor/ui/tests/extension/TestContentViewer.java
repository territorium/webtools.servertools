/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.internet.monitor.ui.tests.extension;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.internet.monitor.ui.internal.provisional.ContentViewer;

public class TestContentViewer extends ContentViewer {
	public void init(Composite parent) {
		// do nothing
	}

	public void setContent(byte[] b) {
		// do nothing
	}

	public byte[] getContent() {
		return null;
	}
}