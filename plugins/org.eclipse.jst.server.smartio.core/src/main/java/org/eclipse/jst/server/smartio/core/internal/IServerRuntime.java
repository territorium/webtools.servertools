/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jst.server.core.IJavaRuntime;

import java.util.List;

/**
 *
 */
interface IServerRuntime extends IJavaRuntime {

  /**
   * Returns the runtime classpath that is used by this runtime.
   * 
   * @return the runtime classpath
   */
  public List<IRuntimeClasspathEntry> getRuntimeClasspath(IPath configPath);
}