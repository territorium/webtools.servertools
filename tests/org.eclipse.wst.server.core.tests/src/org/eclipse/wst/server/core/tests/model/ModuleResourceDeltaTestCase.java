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

import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.tests.impl.TestModuleResourceDelta;

public class ModuleResourceDeltaTestCase extends TestCase {
	protected static IModuleResourceDelta delta;

	public void test00CreateDelegate() {
		delta = new TestModuleResourceDelta();
	}
	
	public void test01GetModuleResource() {
		assertNull(delta.getModuleResource());
	}
	
	public void test02GetAffectedChildren() {
		assertNull(delta.getAffectedChildren());
	}
	
	public void test03GetModuleRelativePath() {
		assertNull(delta.getModuleRelativePath());
	}
	
	public void test04GetKind() {
		delta.getKind();
	}
}