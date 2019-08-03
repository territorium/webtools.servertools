/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jst.server.core.ServerProfilerDelegate;
import org.eclipse.jst.server.smartio.core.ServerPlugin.Level;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The {@link ServerLaunchConfiguration} is responsible to start the Java VM
 * with the smart.IO Server.
 */
public class ServerLaunchConfiguration extends AbstractJavaLaunchConfigurationDelegate {

  /**
   * Constructs an instance of {@link ServerLaunchConfiguration}.
   */
  public ServerLaunchConfiguration() {
    allowAdvancedSourcelookup();
  }

  /**
   * Launches the Java VM with the smart.IO Server.
   *
   * @param conf
   * @param mode
   * @param launch
   * @param monitor
   */
  @Override
  public void launch(ILaunchConfiguration conf, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    IServer server = ServerUtil.getServer(conf);
    if (server == null) {
      ServerPlugin.log(Level.FINEST, "Launch configuration could not find server");
      // throw CoreException();
      return;
    }

    if (server.shouldPublish() && ServerCore.isAutoPublishing()) {
      server.publish(IServer.PUBLISH_INCREMENTAL, monitor);
    }

    ServerBehaviour behavior = (ServerBehaviour) server.loadAdapter(ServerBehaviour.class, null);

    IVMInstallType installType = new StandardVMType();
    IVMInstall vm = installType.createVMInstall("Smart.IO VM");
    vm.setInstallLocation(behavior.getServerRuntime().getRuntime().getLocation().toFile());

    IVMRunner runner = vm.getVMRunner(mode);
    if (runner == null) {
      runner = vm.getVMRunner(ILaunchManager.RUN_MODE);
    }

    File workingDir = verifyWorkingDirectory(conf);
    String workingDirName = null;
    if (workingDir != null) {
      workingDirName = workingDir.getAbsolutePath();
    }

    // Program arguments & Java VM arguments
    ExecutionArguments args = new ExecutionArguments(getVMArguments(conf), getProgramArguments(conf));
    String[][] classAndModulePath = getClasspathAndModulepath(conf);

    // Create VM configuration
    VMRunnerConfiguration runConfig = new VMRunnerConfiguration(behavior.getRuntimeClass(), classAndModulePath[0]);
    runConfig.setModulepath(classAndModulePath[1]);
    runConfig.setProgramArguments(args.getProgramArgumentsArray());
    runConfig.setVMArguments(getVMArguments(conf, mode, args));
    runConfig.setWorkingDirectory(workingDirName);
    runConfig.setEnvironment(getEnvironment(conf));
    runConfig.setVMSpecificAttributesMap(getVMSpecificAttributesMap(conf));

    // Bootpath
    String[] bootpath = getBootpath(conf);
    if ((bootpath != null) && (bootpath.length > 0)) {
      runConfig.setBootClassPath(bootpath);
    }
    setDefaultSourceLocator(launch, conf);

    if (ILaunchManager.PROFILE_MODE.equals(mode)) {
      try {
        ServerProfilerDelegate.configureProfiling(launch, vm, runConfig, monitor);
      } catch (CoreException ce) {
        behavior.stopImpl();
        throw ce;
      }
    }

    // Launch the configuration
    behavior.setupLaunch(launch, mode, monitor);
    try {
      runner.run(runConfig, launch, monitor);
      behavior.addProcessListener(launch.getProcesses()[0]);
    } catch (Exception e) {
      // Ensure we don't continue to think the server is starting
      behavior.stopImpl();
    }
  }

  /**
   * Get the VM arguments. Enable source lookup java agent, if
   * allowAdvancedSourcelookup() was invoked.
   *
   * @param conf
   * @param mode
   * @param args
   */
  private final String[] getVMArguments(ILaunchConfiguration conf, String mode, ExecutionArguments args)
      throws CoreException {
    List<String> vmArguments = new ArrayList<>();
    vmArguments.addAll(Arrays.asList(DebugPlugin.parseArguments(getVMArguments(conf, mode))));
    vmArguments.addAll(Arrays.asList(args.getVMArgumentsArray()));
    return vmArguments.toArray(new String[vmArguments.size()]);
  }
  
  
}