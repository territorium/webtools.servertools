/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.server.core.tests.model;

import junit.framework.TestCase;

import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.model.RuntimeLocatorDelegate;
import org.eclipse.wst.server.core.tests.impl.TestRuntimeLocatorDelegate;

public class RuntimeLocatorDelegateTestCase extends TestCase {
	protected static RuntimeLocatorDelegate delegate;

	public void test00CreateDelegate() throws Exception {
		delegate = new TestRuntimeLocatorDelegate();
	}
	
	public void test01Search() throws Exception {
		delegate.searchForRuntimes(null, null, null);
	}
	
	public void test02Listener() {
		RuntimeLocatorDelegate.IRuntimeSearchListener listener = new RuntimeLocatorDelegate.IRuntimeSearchListener() {
			public void runtimeFound(IRuntimeWorkingCopy runtime) {
				// ignore
			}
		};
		
		listener.runtimeFound(null);
	}
}