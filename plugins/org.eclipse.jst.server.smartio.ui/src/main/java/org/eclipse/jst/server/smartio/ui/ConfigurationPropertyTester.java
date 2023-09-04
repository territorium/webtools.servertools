/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jst.server.smartio.core.IServerWrapper;
import org.eclipse.wst.server.core.IServerAttributes;

/**
 * The {@link ConfigurationPropertyTester} class.
 */
public class ConfigurationPropertyTester extends PropertyTester {

  /**
   *
   */
  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    try {
      IServerAttributes server = (IServerAttributes) receiver;
      IServerWrapper wrapper = (IServerWrapper) server.loadAdapter(IServerWrapper.class, null);
      return (wrapper != null) && (wrapper.loadConfiguration() != null);
    } catch (Exception e) {}
    return false;
  }
}