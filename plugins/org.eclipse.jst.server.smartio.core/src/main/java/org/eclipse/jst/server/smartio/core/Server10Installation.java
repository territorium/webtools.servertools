/**********************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 **********************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jst.server.smartio.core.command.AddModuleCommand;
import org.eclipse.jst.server.smartio.core.util.FileUtil;
import org.eclipse.jst.server.smartio.core.util.VersionHelper;
import org.eclipse.wst.server.core.IModule;

import java.util.Collections;
import java.util.List;

/**
 * smart.IO handler.
 */
class Server10Installation implements IServerInstallation {

  /**
   * Gets the base directory for this server. This directory is used as the "base" property for the
   * server.
   *
   * @param wrapper
   */
  @Override
  public IPath getRuntimeBaseDirectory(ServerWrapper wrapper) {
    return wrapper.getServer().getRuntime().getLocation();
  }

  /**
   * Get the runtime class to start the server
   */
  @Override
  public String getRuntimeClass() {
    return VMArgsBuilder.BOOT_CLASS;
  }

  /**
   * Get the runtime class-path to start the server.
   *
   * @param installPath
   */
  @Override
  public List<IRuntimeClasspathEntry> getRuntimeClasspath(IPath installPath) {
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
  public final String[] getRuntimeVMArguments(IPath installPath, IPath configPath, IPath deployPath, IFolder config) {
    VMArgsBuilder args = new VMArgsBuilder();
    args.add("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
    args.addPath(VMArgsBuilder.SMARTIO_USER, ServerTools.getRelativePath(installPath, configPath));
    args.addPath(VMArgsBuilder.SMARTIO_CONF, config.getLocation());

    args.addPath(VMArgsBuilder.USER_DIR, config.getLocation());

    args.addOpens("java.base/java.lang");
    args.addOpens("java.rmi/sun.rmi.transport");
    args.addOpens("java.base/java.io", "tomcat.embed");
    return args.build(VMArgsBuilder.BOOT_MODULE);
  }

  /**
   * @see IServerInstallation#getRuntimeProgramArguments(IPath, boolean, boolean)
   */
  @Override
  public String[] getRuntimeProgramArguments(IPath configPath, boolean starting) {
    return new String[] { starting ? "start" : "stop" };
  }

  /**
   * @see IServerInstallation#verifyInstallPath(IPath)
   */
  @Override
  public IStatus verifyInstallPath(IPath installPath) {
    IStatus result = VersionHelper.checkVersion(installPath, ServerPlugin.SERVER_10);
    // If check was canceled, use folder check
    if (result.getSeverity() == IStatus.CANCEL) {
      result = ServerTools.verifyInstallPathWithFolderCheck(installPath, ServerPlugin.SERVER_10);
    }
    return result;
  }

  /**
   * @see IServerInstallation#canAddModule(IModule)
   */
  @Override
  public IStatus canAddModule(IModule module) {
    return WebModule.canAddModule(module) ? Status.OK_STATUS
        : new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorSpec10, null);
  }

  /**
   * @see IServerInstallation#prepareDeployDirectory(IPath)
   */
  @Override
  public IStatus prepareDeployDirectory(IPath deployPath) {
    return FileUtil.createDeploymentDirectory(deployPath, WebModule.DEFAULT_WEBXML_SERVLET25);
  }

  /**
   * @see IServerInstallation#prepareForServingDirectly(IPath, ServerWrapper)
   */
  @Override
  public IStatus prepareForServingDirectly(IPath baseDir, ServerWrapper server, String version) {
    return Status.OK_STATUS;
  }
}
