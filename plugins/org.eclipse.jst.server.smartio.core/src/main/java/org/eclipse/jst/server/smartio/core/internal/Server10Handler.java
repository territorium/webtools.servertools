/**********************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.wst.server.core.IModule;

import java.util.ArrayList;
import java.util.List;

/**
 * smart.IO handler.
 */
class Server10Handler implements IServerVersionHandler {

  /**
   * @see IServerVersionHandler#verifyInstallPath(IPath)
   */
  @Override
  public IStatus verifyInstallPath(IPath installPath) {
    IStatus result = TomcatVersionHelper.checkCatalinaVersion(installPath, ServerPlugin.SERVER_10);
    // If check was canceled, use folder check
    if (result.getSeverity() == IStatus.CANCEL) {
      result = ServerPlugin.verifyInstallPathWithFolderCheck(installPath, ServerPlugin.SERVER_10);
    }
    return result;
  }

  /**
   * @see IServerVersionHandler#getRuntimeClass()
   */
  @Override
  public String getRuntimeClass() {
    return "org.apache.catalina.startup.Bootstrap";
  }

  /**
   * @see IServerVersionHandler#getRuntimeClasspath(IPath, IPath)
   */
  @Override
  public List<IRuntimeClasspathEntry> getRuntimeClasspath(IPath installPath, IPath configPath) {
    List<IRuntimeClasspathEntry> cp = new ArrayList<>();

    // 9.0 - add bootstrap.jar and tomcat-juli.jar from the Tomcat bin directory
    IPath binPath = installPath.append("bin");
    if (binPath.toFile().exists()) {
      IPath path = binPath.append("bootstrap.jar");
      cp.add(JavaRuntime.newArchiveRuntimeClasspathEntry(path));
      // Add tomcat-juli.jar if it exists
      path = binPath.append("tomcat-juli.jar");
      if (path.toFile().exists()) {
        cp.add(JavaRuntime.newArchiveRuntimeClasspathEntry(path));
      }
      // If tomcat-juli.jar is not found in the install, check the config directory
      else if (configPath != null) {
        path = configPath.append("bin/tomcat-juli.jar");
        if (path.toFile().exists()) {
          cp.add(JavaRuntime.newArchiveRuntimeClasspathEntry(path));
        }
      }
    }

    return cp;
  }

  /**
   * @see IServerVersionHandler#getRuntimeProgramArguments(IPath, boolean, boolean)
   */
  @Override
  public String[] getRuntimeProgramArguments(IPath configPath, boolean debug, boolean starting) {
    List<String> list = new ArrayList<>();

    if (starting) {
      list.add("start");
    } else {
      list.add("stop");
    }

    String[] temp = new String[list.size()];
    list.toArray(temp);
    return temp;
  }

  /**
   * @see IServerVersionHandler#getExcludedRuntimeProgramArguments(boolean, boolean)
   */
  @Override
  public String[] getExcludedRuntimeProgramArguments(boolean debug, boolean starting) {
    return null;
  }

  /**
   * @see IServerVersionHandler#getRuntimeVMArguments(IPath, IPath, IPath, boolean)
   */
  @Override
  public String[] getRuntimeVMArguments(IPath installPath, IPath configPath, IPath deployPath, boolean isTestEnv) {
    return TomcatVersionHelper.getCatalinaVMArguments(installPath, configPath, deployPath, isTestEnv);
  }

  /**
   * @see IServerVersionHandler#getRuntimePolicyFile(IPath)
   */
  @Override
  public String getRuntimePolicyFile(IPath configPath) {
    return configPath.append("conf").append("catalina.policy").toOSString();
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
   * @see IServerVersionHandler#getRuntimeBaseDirectory(ServerWrapper)
   */
  @Override
  public IPath getRuntimeBaseDirectory(ServerWrapper server) {
    return TomcatVersionHelper.getStandardBaseDirectory(server);
  }

  /**
   * @see IServerVersionHandler#prepareRuntimeDirectory(IPath)
   */
  @Override
  public IStatus prepareRuntimeDirectory(IPath baseDir) {
    return TomcatVersionHelper.createCatalinaInstanceDirectory(baseDir);
  }

  /**
   * @see IServerVersionHandler#prepareDeployDirectory(IPath)
   */
  @Override
  public IStatus prepareDeployDirectory(IPath deployPath) {
    return TomcatVersionHelper.createDeploymentDirectory(deployPath, TomcatVersionHelper.DEFAULT_WEBXML_SERVLET25);
  }

  /**
   * @see IServerVersionHandler#prepareForServingDirectly(IPath, ServerWrapper)
   */
  @Override
  public IStatus prepareForServingDirectly(IPath baseDir, ServerWrapper server, String version) {
    // Nothing beyond configuration required for Tomcat 9
    return Status.OK_STATUS;
  }

  /**
   * @see IServerVersionHandler#getSharedLoader(IPath)
   */
  @Override
  public String getSharedLoader(IPath baseDir) {
    return "common";
  }

  /**
   * Returns true since Tomcat 9.x supports this feature.
   * 
   * @return true since feature is supported
   */
  @Override
  public boolean supportsServeModulesWithoutPublish() {
    return true;
  }

  /**
   * @see IServerVersionHandler#supportsDebugArgument()
   */
  @Override
  public boolean supportsDebugArgument() {
    return false;
  }

  /**
   * @see IServerVersionHandler#supportsSeparateContextFiles()
   */
  @Override
  public boolean supportsSeparateContextFiles() {
    return true;
  }

  /**
   * @see IServerVersionHandler#getEndorsedDirectories(IPath)
   */
  @Override
  public String getEndorsedDirectories(IPath installPath) {
    return installPath.append("endorsed").toOSString();
  }
}
