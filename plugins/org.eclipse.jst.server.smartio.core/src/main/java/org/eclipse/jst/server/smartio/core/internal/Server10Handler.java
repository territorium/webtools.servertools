/**********************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 **********************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.wst.server.core.IModule;

import java.util.Collections;
import java.util.List;

/**
 * smart.IO handler.
 */
class Server10Handler implements IServerVersionHandler {


  /**
   * Gets the base directory for this server. This directory is used as the
   * "base" property for the server.
   * 
   * @param server
   */
  @Override
  public IPath getRuntimeBaseDirectory(ServerWrapper server) {
    // Return runtime path
    return server.getServer().getRuntime().getLocation();
  }

  /**
   * Get the runtime class to start the server
   */
  @Override
  public String getRuntimeClass() {
    return "it.smartio.startup.StartUp";
  }

  /**
   * Get the runtime class-path to start the server.
   */
  @Override
  public List<IRuntimeClasspathEntry> getRuntimeClasspath(IPath installPath, IPath configPath) {
    return Collections.emptyList();
  }

  /**
   * Gets the startup VM arguments for the server.
   * 
   * @param installPath
   * @param configPath
   * @param deployPath
   */
  @Override
  public final String[] getRuntimeVMArguments(IPath installPath, IPath configPath, IPath deployPath) {
    return new String[] { "-m", "smartio.startup" };
  }

  /**
   * @see IServerVersionHandler#getRuntimeProgramArguments(IPath, boolean,
   *      boolean)
   */
  @Override
  public String[] getRuntimeProgramArguments(IPath configPath, boolean starting) {
    return starting ? new String[] { "start" } : new String[] { "stop" };
  }

  /**
   * @see IServerVersionHandler#verifyInstallPath(IPath)
   */
  @Override
  public IStatus verifyInstallPath(IPath installPath) {
    IStatus result = VersionHelper.checkVersion(installPath, ServerPlugin.SERVER_10);
    // If check was canceled, use folder check
    if (result.getSeverity() == IStatus.CANCEL) {
      result = IServerVersionHandler.verifyInstallPathWithFolderCheck(installPath, ServerPlugin.SERVER_10);
    }
    return result;
  }

  /**
   * @see IServerVersionHandler#canAddModule(IModule)
   */
  @Override
  public IStatus canAddModule(IModule module) {
    String version = module.getModuleType().getVersion();
    if ("2.2".equals(version) || "2.3".equals(version) || "2.4".equals(version) || "2.5".equals(version)
        || "3.0".equals(version) || "3.1".equals(version) || "4.0".equals(version)) {
      return Status.OK_STATUS;
    }

    return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorSpec10, null);
  }

  /**
   * @see IServerVersionHandler#prepareDeployDirectory(IPath)
   */
  @Override
  public IStatus prepareDeployDirectory(IPath deployPath) {
    return FileUtil.createDeploymentDirectory(deployPath, WebModule.DEFAULT_WEBXML_SERVLET25);
  }

  /**
   * @see IServerVersionHandler#prepareForServingDirectly(IPath, ServerWrapper)
   */
  @Override
  public IStatus prepareForServingDirectly(IPath baseDir, ServerWrapper server, String version) {
    // Nothing beyond configuration required for Tomcat 9
    return Status.OK_STATUS;
  }
}
