/***************************************************************************************************
 * Copyright (c) 2005, 2006 Eteration A.S. and Gorkem Ercan. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: Gorkem Ercan - initial API and implementation
 *               
 **************************************************************************************************/
package org.eclipse.jst.server.generic.ui.internal;


/**
 * 
 * @author Gorkem Ercan
 */
public interface GenericServerCompositeDecorator 
{
	/**
	 * 
	 * @param composite
	 */
	public abstract void decorate(GenericServerComposite composite);
    /**
	 * Called if all the fields are valid. This gives subclasses opportunity to
	 * validate and take necessary actions.
	 * 
	 * @return boolean
	 */
	public abstract boolean validate();
}
