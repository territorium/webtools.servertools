/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import java.util.List;

/**
 *
 */
public interface IServerRuntime {

  // The deployment directory used by default in prior versions.
  public static final String LEGACY_DEPLOYDIR = "webapps";

  /**
   * Returns the runtime classpath that is used by this runtime.
   *
   * @param configPath
   */
  public List<IRuntimeClasspathEntry> getRuntimeClasspath(IPath configPath);

  /**
   * Gets the directory to which web applications are to be deployed. If relative, it is relative to
   * the runtime base directory for the server.
   */
  public String getDeployDirectory();

  /**
   * Set the deployment directory for the server. May be absolute or relative to runtime base
   * directory.
   * 
   * @param deployDir deployment directory for the server
   */
  public void setDeployDirectory(String deployDir);
}