/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.wst.server.core.IModule;

import java.util.List;

/**
 *
 */
public interface IServerInstallation {

  /**
   * Verifies if the specified path points to a a server installation of this version.
   *
   * @param installPath
   */
  public IStatus verifyInstallPath(IPath installPath);

  /**
   * Gets the startup class for the server.
   */
  public String getRuntimeClass();

  /**
   * Gets the startup classpath for the server.
   *
   * @param installPath
   */
  public List<IRuntimeClasspathEntry> getRuntimeClasspath(IPath installPath);

  /**
   * Return the program's runtime arguments.
   *
   * @param configPath
   * @param starting
   */
  public String[] getRuntimeProgramArguments(IPath configPath, boolean starting);

  /**
   * Gets the subset of the startup VM arguments for the server that apply to all compatible JVM
   * versions.
   *
   * @param installPath
   * @param configPath
   * @param deployPath
   */
  public String[] getRuntimeVMArguments(IPath installPath, IPath configPath, IPath deployPath, IFolder folder);

  /**
   * Returns true if the given project is supported by this server, and false otherwise.
   *
   * @param module
   */
  public IStatus canAddModule(IModule module);

  /**
   * Returns the runtime base path for relative paths in the server configuration.
   *
   * @param server
   */
  public IPath getRuntimeBaseDirectory(ServerWrapper server);

  /**
   * Prepares the specified directory by making sure it exists and is initialized appropriately.
   *
   * @param deployPath
   */
  public IStatus prepareDeployDirectory(IPath deployPath);

  /**
   * Prepare directory for serving contexts directly if enabled. If not enabled, restore directory
   * if necessary.
   *
   * @param baseDir
   * @param server
   */
  public IStatus prepareForServingDirectly(IPath baseDir, ServerWrapper server, String version);
}
