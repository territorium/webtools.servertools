/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
package org.eclipse.jst.server.core.tests;

import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.tests.impl.TestWebModule;
import junit.framework.TestCase;

public class WebModuleTestCase extends TestCase {
	protected static IWebModule module;

	protected IWebModule getWebModule() {
		if (module == null) {
			module = new TestWebModule();
		}
		return module;
	}

	public void testCreate() {
		getWebModule();
	}

	public void testContextRoot() {
		getWebModule().getContextRoot();
	}
}