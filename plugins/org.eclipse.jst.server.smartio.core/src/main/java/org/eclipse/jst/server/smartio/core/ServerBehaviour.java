/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.smartio.core.ServerPlugin.Level;
import org.eclipse.jst.server.smartio.core.util.ProgressUtil;
import org.eclipse.jst.server.smartio.core.util.VersionHelper;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;
import org.eclipse.wst.server.core.util.SocketUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Generic {@link ServerBehaviour} server.
 */
public class ServerBehaviour extends ServerBehaviourDelegate {

  private static final String ATTR_STOP = "stop-server";

  // the thread used to ping the server to check for startup
  private transient PingThread             ping = null;
  private transient IDebugEventSetListener processListener;

  /**
   * Get the {@link ServerWrapper} for the current {@link ServerBehaviourDelegate}
   */
  protected final ServerWrapper getWrapper() {
    return (ServerWrapper) getServer().loadAdapter(ServerWrapper.class, null);
  }

  protected final IServerInstallation getHandler() {
    return getWrapper().getHandler();
  }

  protected final IServerConfiguration getConfig() throws CoreException {
    return getWrapper().loadConfiguration();
  }

  public final ServerRuntime getServerRuntime() {
    IRuntime runtime = getServer().getRuntime();
    return (runtime == null) ? null : (ServerRuntime) runtime.loadAdapter(ServerRuntime.class, null);
  }

  /**
   * Return the runtime class name.
   */
  public final String getRuntimeClass() {
    return getHandler().getRuntimeClass();
  }

  /**
   * Returns the runtime base path for relative paths in the server configuration.
   */
  protected final IPath getRuntimeBaseDirectory() {
    return getWrapper().getRuntimeBaseDirectory();
  }

  @Override
  public void initialize(IProgressMonitor monitor) {
    // do nothing
  }

  /**
   * Return the program's runtime arguments to start or stop.
   *
   * @param starting true if starting
   * @return an array of runtime program arguments
   */
  private String[] getRuntimeProgramArguments(boolean starting) {
    return getHandler().getRuntimeProgramArguments(null, starting);
  }

  /**
   * Return the runtime (VM) arguments.
   *
   * @return an array of runtime arguments
   */
  private String[] getRuntimeVMArguments() {
    return getHandler().getRuntimeVMArguments(getServer().getRuntime().getLocation(), getServerConfDirectory(),
        getServerDeployDirectory(), getServer().getServerConfiguration());
  }

  protected void addProcessListener(final IProcess newProcess) {
    if ((processListener != null) || (newProcess == null)) {
      return;
    }

    processListener = new IDebugEventSetListener() {

      @Override
      public void handleDebugEvents(DebugEvent[] events) {
        if (events != null) {
          int size = events.length;
          for (int i = 0; i < size; i++) {
            if ((newProcess != null) && newProcess.equals(events[i].getSource())
                && (events[i].getKind() == DebugEvent.TERMINATE)) {
              stopImpl();
            }
          }
        }
      }
    };
    DebugPlugin.getDefault().addDebugEventListener(processListener);
  }

  protected void stopImpl() {
    if (ping != null) {
      ping.stop();
      ping = null;
    }
    if (processListener != null) {
      DebugPlugin.getDefault().removeDebugEventListener(processListener);
      processListener = null;
    }
    setServerState(IServer.STATE_STOPPED);
  }

  @Override
  protected void publishServer(int kind, IProgressMonitor monitor) throws CoreException {
    if (getServer().getRuntime() == null) {
      return;
    }

    IPath installDir = getServer().getRuntime().getLocation();
    IPath confDir = getServerConfDirectory();
    IStatus status = getHandler().prepareDeployDirectory(getServerDeployDirectory());
    if ((status != null) && !status.isOK()) {
      throw new CoreException(status);
    }

    monitor = ProgressUtil.getMonitorFor(monitor);
    monitor.beginTask(Messages.publishServerTask, 600);

    status = getConfig().cleanupServer(confDir, installDir, ProgressUtil.getSubMonitorFor(monitor, 100));
    if ((status != null) && !status.isOK()) {
      throw new CoreException(status);
    }

    status = getConfig().localizeConfiguration(confDir, getServerDeployDirectory(), getWrapper(),
        ProgressUtil.getSubMonitorFor(monitor, 100));
    if ((status != null) && !status.isOK()) {
      throw new CoreException(status);
    }

    monitor.done();

    setServerPublishState(IServer.PUBLISH_STATE_NONE);
  }

  /**
   * Publishes the given module to the server.
   */
  @Override
  protected void publishModule(int kind, int deltaKind, IModule[] moduleTree, IProgressMonitor monitor)
      throws CoreException {
    if (getServer().getServerState() != IServer.STATE_STOPPED) {
      if ((deltaKind == ServerBehaviourDelegate.ADDED) || (deltaKind == ServerBehaviourDelegate.REMOVED)) {
        setServerRestartState(true);
      }
    }
    File publishFile = getTempDirectory().append("publish.txt").toFile();

    Properties properties = new Properties();
    try (InputStream istream = new FileInputStream(publishFile)) {
      properties.load(istream);
    } catch (Exception e) {}

    File tmpFile = getRuntimeBaseDirectory().append("temp").toFile();
    PublishModule publishModule = new PublishModule(tmpFile, this, monitor);
    // If parent web module
    if (moduleTree.length == 1) {
      publishModule.publishDirectory(deltaKind, properties, moduleTree);
    } else { // Else a child module
      // Try to determine the URI for the child module
      IWebModule webModule = (IWebModule) moduleTree[0].loadAdapter(IWebModule.class, monitor);
      String childURI = null;
      if (webModule != null) {
        childURI = webModule.getURI(moduleTree[1]);
      }
      // Try to determine if child is binary
      IJ2EEModule childModule = (IJ2EEModule) moduleTree[1].loadAdapter(IJ2EEModule.class, monitor);
      boolean isBinary = false;
      if (childModule != null) {
        isBinary = childModule.isBinary();
      }

      if (isBinary) {
        publishModule.publishArchive(childURI, kind, deltaKind, properties, moduleTree);
      } else {
        publishModule.publishJar(childURI, kind, deltaKind, properties, moduleTree);
      }
    }

    setModulePublishState(moduleTree, IServer.PUBLISH_STATE_NONE);

    try (OutputStream ostream = new FileOutputStream(publishFile)) {
      properties.store(ostream, "smart.IO publish data");
    } catch (Exception e) {
      // ignore
    }
  }

  /*
   * @see org.eclipse.wst.server.core.model.ServerBehaviourDelegate#getResources(org.
   * eclipse.wst.server. core.IModule[])
   */
  @Override
  public final IModuleResource[] getResources(IModule[] module) {
    return super.getResources(module);
  }

  /*
   * @see org.eclipse.wst.server.core.model.ServerBehaviourDelegate#
   * getPublishedResourceDelta(org.eclipse .wst.server.core.IModule[])
   */
  @Override
  public final IModuleResourceDelta[] getPublishedResourceDelta(IModule[] module) {
    return super.getPublishedResourceDelta(module);
  }

  @Override
  protected void publishFinish(IProgressMonitor monitor) throws CoreException {
    IStatus status;
    IPath baseDir = getRuntimeBaseDirectory();
    IServerInstallation handler = getHandler();
    String serverTypeID = getServer().getServerType().getId();
    String serverVersion = VersionHelper.getVersion(baseDir, serverTypeID);
    // Include or remove loader jar depending on state of serving directly
    status = handler.prepareForServingDirectly(baseDir, getWrapper(), serverVersion);
    if (!status.isOK()) {
      throw new CoreException(status);
    }
  }

  /**
   * Cleanly shuts down and terminates the server.
   *
   * @param force <code>true</code> to kill the server
   */
  @Override
  public void stop(boolean force) {
    if (force) {
      terminate();
      return;
    }

    int state = getServer().getServerState();
    // If stopped or stopping, no need to run stop command again
    if ((state == IServer.STATE_STOPPED) || (state == IServer.STATE_STOPPING)) {
      return;
    } else if (state == IServer.STATE_STARTING) {
      terminate();
      return;
    }

    try {
      if (ServerPlugin.isTraceEnabled()) {
        ServerPlugin.log(Level.FINER, "Stopping smart.IO");
      }
      if (state != IServer.STATE_STOPPED) {
        setServerState(IServer.STATE_STOPPING);
      }

      ILaunchConfiguration launchConfig = getServer().getLaunchConfiguration(true, null);
      ILaunchConfigurationWorkingCopy wc = launchConfig.getWorkingCopy();

      String args = ServerTools.renderCommandLine(getRuntimeProgramArguments(false), " ");
      wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, args);
      wc.setAttribute("org.eclipse.debug.ui.private", true);
      wc.setAttribute(ServerBehaviour.ATTR_STOP, "true");
      wc.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Error stopping smart.IO", e);
    }
  }

  /**
   * Terminates the server.
   */
  private void terminate() {
    if (getServer().getServerState() == IServer.STATE_STOPPED) {
      return;
    }

    try {
      setServerState(IServer.STATE_STOPPING);
      if (ServerPlugin.isTraceEnabled()) {
        ServerPlugin.log(Level.FINER, "Killing the smart.IO process");
      }
      ILaunch launch = getServer().getLaunch();
      if (launch != null) {
        launch.terminate();
        stopImpl();
      }
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Error killing the process", e);
    }
  }

  @Override
  public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor)
      throws CoreException {
    String existingProgArgs =
        workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String) null);
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
        ServerTools.mergeArguments(existingProgArgs, getRuntimeProgramArguments(true)));

    String existingVMArgs =
        workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);

    String mergedVMArguments = ServerTools.mergeArguments(existingVMArgs, getRuntimeVMArguments());
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, mergedVMArguments);
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
        getRuntimeBaseDirectory().toString());

    // update classpath
    IRuntimeClasspathEntry[] originalClasspath = JavaRuntime.computeUnresolvedRuntimeClasspath(workingCopy);
    int size = originalClasspath.length;
    List<IRuntimeClasspathEntry> oldCp = new ArrayList<>(originalClasspath.length + 2);
    for (int i = 0; i < size; i++) {
      oldCp.add(originalClasspath[i]);
    }

    List<IRuntimeClasspathEntry> cp2 = getServerRuntime().getRuntimeClasspath(getRuntimeBaseDirectory());
    Iterator<IRuntimeClasspathEntry> iterator = cp2.iterator();
    while (iterator.hasNext()) {
      IRuntimeClasspathEntry entry = iterator.next();
      ServerTools.mergeClasspath(oldCp, entry);
    }

    iterator = oldCp.iterator();
    List<String> list = new ArrayList<>();
    while (iterator.hasNext()) {
      IRuntimeClasspathEntry entry = iterator.next();
      try {
        list.add(entry.getMemento());
      } catch (Exception e) {
        ServerPlugin.log(Level.SEVERE, "Could not resolve classpath entry: " + entry, e);
      }
    }

    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, list);
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
  }

  /**
   * @see ServerBehaviourDelegate#handleResourceChange()
   */
  @Override
  public void handleResourceChange() {
    if (getServer().getServerRestartState()) {
      return;
    }

    for (IModule[] module : getAllModules()) {
      IModuleResourceDelta[] delta = getPublishedResourceDelta(module);
      if ((delta == null) || (delta.length == 0)) {
        continue;
      }

      ResourceChanged changes = new ResourceChanged(delta);
      if (changes.containsNonResourceChange()) {
        setServerRestartState(true);
        return;
      }
    }
  }

  /**
   * Cleans the entire work directory for this server. This involves deleting all subdirectories of
   * the server's work directory.
   *
   * @param monitor a progress monitor
   * @return results of the clean operation
   * @throws CoreException
   */
  public IStatus cleanServerWorkDir(IProgressMonitor monitor) throws CoreException {
    IStatus result;
    IPath basePath = getRuntimeBaseDirectory();
    IPath workPath = getConfig().getServerWorkDirectory(basePath);
    if (workPath != null) {
      File workDir = workPath.toFile();
      result = Status.OK_STATUS;
      if (workDir.exists() && workDir.isDirectory()) {
        // Delete subdirectories of the server's work dir
        File[] files = workDir.listFiles();
        if ((files != null) && (files.length > 0)) {
          MultiStatus ms =
              new MultiStatus(ServerPlugin.PLUGIN_ID, 0, "Problem occurred deleting work directory for module.", null);
          int size = files.length;
          monitor = ProgressUtil.getMonitorFor(monitor);
          monitor.beginTask(NLS.bind("Cleaning Server Work Directory", new String[] { workDir.getAbsolutePath() }),
              size * 10);

          for (int i = 0; i < size; i++) {
            File current = files[i];
            if (current.isDirectory()) {
              IStatus[] results = PublishHelper.deleteDirectory(current, ProgressUtil.getSubMonitorFor(monitor, 10));
              if ((results != null) && (results.length > 0)) {
                for (IStatus result2 : results) {
                  ms.add(result2);
                }
              }
            }
          }
          monitor.done();
          result = ms;
        }
      }
    } else {
      result =
          new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, "Could not determine work directory for module", null);
    }
    return result;
  }

  /**
   * Cleans the work directory associated with the specified module on this server.
   *
   * @param module module whose work directory should be cleaned
   * @param monitor a progress monitor
   * @return result of the clean operation
   * @throws CoreException
   */
  public IStatus cleanContextWorkDir(WebModule module, IProgressMonitor monitor) throws CoreException {
    IStatus result;
    IPath basePath = getRuntimeBaseDirectory();
    IPath workPath = getConfig().getContextWorkDirectory(basePath, module);
    if (workPath != null) {
      File workDir = workPath.toFile();
      result = Status.OK_STATUS;
      if (workDir.exists() && workDir.isDirectory()) {
        IStatus[] results = PublishHelper.deleteDirectory(workDir, monitor);
        MultiStatus ms =
            new MultiStatus(ServerPlugin.PLUGIN_ID, 0, "Problem occurred deleting work directory for module.", null);
        if ((results != null) && (results.length > 0)) {
          for (IStatus result2 : results) {
            ms.add(result2);
          }
        }
        result = ms;
      }
    } else {
      result =
          new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, "Could not determine work directory for module", null);
    }
    return result;
  }

  /**
   * Gets the directory to which modules should be deployed for this server.
   */
  public final IPath getServerConfDirectory() {
    return ServerTools.getAbsolutePath(getRuntimeBaseDirectory(), getWrapper().getConfDirectory());
  }

  /**
   * Gets the directory to which modules should be deployed for this server.
   */
  public final IPath getServerDeployDirectory() {
    return ServerTools.getAbsolutePath(getRuntimeBaseDirectory(), getWrapper().getDeployDirectory());
  }

  /**
   * Temporary method to help web services team. Returns the path that the module is published to.
   *
   * @param module
   */
  public IPath getPublishDirectory(IModule module) {
    if (module == null) {
      return null;
    }

    IWebModule webModule = (IWebModule) module.getAdapter(IWebModule.class);
    if (webModule == null || webModule.getContextRoot() == null) {
      return getServerDeployDirectory().append(module.getName());
    }
    return getServerDeployDirectory().append(webModule.getContextRoot());
  }

  public final void setServerStarted() {
    setServerState(IServer.STATE_STARTED);
  }

  public void setServerStates(int publishState, boolean restartState) {
    setServerPublishState(publishState);
    setServerRestartState(restartState);
  }

  /**
   * Return a string representation of this object.
   *
   * @return java.lang.String
   */
  @Override
  public String toString() {
    return "ServerBehaviour";
  }

  /**
   * Setup for starting the server.
   *
   * @param launch ILaunch
   * @param launchMode String
   * @param monitor IProgressMonitor
   * @throws CoreException if anything goes wrong
   */
  public void setupLaunch(ILaunch launch, String launchMode, IProgressMonitor monitor) throws CoreException {
    if ("true".equals(launch.getLaunchConfiguration().getAttribute(ServerBehaviour.ATTR_STOP, "false"))) {
      return;
    }

    IStatus status = getServerRuntime().validate();
    if ((status != null) && (status.getSeverity() == IStatus.ERROR)) {
      throw new CoreException(status);
    }

    // setRestartNeeded(false);
    IServerConfiguration configuration = getConfig();

    // check that ports are free
    Iterator<ServerPort> iterator = configuration.getServerPorts().iterator();
    List<ServerPort> usedPorts = new ArrayList<>();
    while (iterator.hasNext()) {
      ServerPort sp = iterator.next();
      if (sp.getPort() < 0) {
        throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorPortInvalid, null));
      }
      if (SocketUtil.isPortInUse(sp.getPort(), 5)) {
        usedPorts.add(sp);
      }
    }
    if (usedPorts.size() == 1) {
      ServerPort port = usedPorts.get(0);
      throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorPortInUse, new String[] { port.getPort() + "", getServer().getName() }), null));
    } else if (usedPorts.size() > 1) {
      String portStr = "";
      iterator = usedPorts.iterator();
      boolean first = true;
      while (iterator.hasNext()) {
        if (!first) {
          portStr += ", ";
        }
        first = false;
        ServerPort sp = iterator.next();
        portStr += "" + sp.getPort();
      }
      throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorPortsInUse, new String[] { portStr, getServer().getName() }), null));
    }

    // check that there is only one app for each context root
    Iterator<WebModule> iterator2 = configuration.getWebModules().iterator();
    List<String> contextRoots = new ArrayList<>();
    while (iterator2.hasNext()) {
      WebModule module = iterator2.next();
      String contextRoot = module.getPath();
      if (contextRoots.contains(contextRoot)) {
        throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
            NLS.bind(Messages.errorDuplicateContextRoot, new String[] { contextRoot }), null));
      }

      contextRoots.add(contextRoot);
    }

    setServerRestartState(false);
    setServerState(IServer.STATE_STARTING);
    setMode(launchMode);

    // ping server to check for startup
    try {
      String url = "http://" + getServer().getHost();
      int port = configuration.getMainPort().getPort();
      if (port != 80) {
        url += ":" + port;
      }
      ping = new PingThread(getServer(), url, -1, this);
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Can't ping for smart.IO startup.");
    }
  }
}
