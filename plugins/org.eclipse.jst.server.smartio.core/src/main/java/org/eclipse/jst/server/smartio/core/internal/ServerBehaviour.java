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

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.jst.server.smartio.core.internal.ServerPlugin.Level;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.internal.IModulePublishHelper;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;
import org.eclipse.wst.server.core.util.SocketUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Generic {@link ServerBehaviour} server.
 */
public class ServerBehaviour extends ServerBehaviourDelegate implements IModulePublishHelper {

  private static final String   ATTR_STOP        = "stop-server";

  private static final String[] JMX_EXCLUDE_ARGS =
      new String[] { "-Dcom.sun.management.jmxremote", "-Dcom.sun.management.jmxremote.port=",
          "-Dcom.sun.management.jmxremote.ssl=", "-Dcom.sun.management.jmxremote.authenticate=" };

  // the thread used to ping the server to check for startup
  private transient PingThread             ping = null;
  private transient IDebugEventSetListener processListener;


  /**
   * Get the {@link ServerWrapper} for the current
   * {@link ServerBehaviourDelegate}
   */
  public final ServerWrapper getWrapper() {
    return (ServerWrapper) getServer().loadAdapter(ServerWrapper.class, null);
  }


  @Override
  public void initialize(IProgressMonitor monitor) {
    // do nothing
  }

  public ServerRuntime getServerRuntime() {
    if (getServer().getRuntime() == null) {
      return null;
    }

    return (ServerRuntime) getServer().getRuntime().loadAdapter(ServerRuntime.class, null);
  }

  public IServerVersionHandler getVersionHandler() {
    return getWrapper().getVersionHandler();
  }

  public IServerConfiguration getConfiguration() throws CoreException {
    return getWrapper().getConfiguration();
  }

  /**
   * Return the runtime class name.
   *
   * @return the class name
   */
  public String getRuntimeClass() {
    return getVersionHandler().getRuntimeClass();
  }

  /**
   * Returns the runtime base path for relative paths in the server
   * configuration.
   * 
   * @return the base path
   */
  public IPath getRuntimeBaseDirectory() {
    return getWrapper().getRuntimeBaseDirectory();
  }

  /**
   * Return the program's runtime arguments to start or stop.
   *
   * @param starting true if starting
   * @return an array of runtime program arguments
   */
  private String[] getRuntimeProgramArguments(boolean starting) {
    IPath configPath = null;
    return getVersionHandler().getRuntimeProgramArguments(configPath, starting);
  }

  /**
   * Return the runtime (VM) arguments.
   *
   * @return an array of runtime arguments
   */
  private String[] getRuntimeVMArguments() {
    IPath installPath = getServer().getRuntime().getLocation();
    // If installPath is relative, convert to canonical path and hope for the
    // best
    if (!installPath.isAbsolute()) {
      try {
        String installLoc = (new File(installPath.toOSString())).getCanonicalPath();
        installPath = new Path(installLoc);
      } catch (IOException e) {
        // Ignore if there is a problem
      }
    }
    IPath configPath = getRuntimeBaseDirectory();
    IPath deployPath;
    deployPath = getServerDeployDirectory();
    // If deployPath is relative, convert to canonical path and hope for the
    // best
    if (!deployPath.isAbsolute()) {
      try {
        String deployLoc = (new File(deployPath.toOSString())).getCanonicalPath();
        deployPath = new Path(deployLoc);
      } catch (IOException e) {
        // Ignore if there is a problem
      }
    }
    return getVersionHandler().getRuntimeVMArguments(installPath, configPath, deployPath);
  }

  private static String renderCommandLine(String[] commandLine, String separator) {
    if ((commandLine == null) || (commandLine.length < 1)) {
      return "";
    }
    StringBuffer buf = new StringBuffer(commandLine[0]);
    for (int i = 1; i < commandLine.length; i++) {
      buf.append(separator);
      buf.append(commandLine[i]);
    }
    return buf.toString();
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

  protected void setServerStarted() {
    setServerState(IServer.STATE_STARTED);
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
    IPath confDir = installDir;
    IStatus status = getVersionHandler().prepareDeployDirectory(getServerDeployDirectory());
    if ((status != null) && !status.isOK()) {
      throw new CoreException(status);
    }

    monitor = ProgressUtil.getMonitorFor(monitor);
    monitor.beginTask(Messages.publishServerTask, 600);

    status = getConfiguration().cleanupServer(confDir, installDir, ProgressUtil.getSubMonitorFor(monitor, 100));
    if ((status != null) && !status.isOK()) {
      throw new CoreException(status);
    }

    status = getConfiguration().backupAndPublish(confDir, ProgressUtil.getSubMonitorFor(monitor, 400));
    if ((status != null) && !status.isOK()) {
      throw new CoreException(status);
    }

    status = getConfiguration().localizeConfiguration(confDir, getServerDeployDirectory(), getWrapper(),
        ProgressUtil.getSubMonitorFor(monitor, 100));
    if ((status != null) && !status.isOK()) {
      throw new CoreException(status);
    }

    monitor.done();

    setServerPublishState(IServer.PUBLISH_STATE_NONE);
  }

  /*
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

    Properties p = loadModulePublishLocations();

    PublishHelper helper = new PublishHelper(getRuntimeBaseDirectory().append("temp").toFile());
    // If parent web module
    if (moduleTree.length == 1) {
      publishDir(deltaKind, p, moduleTree, helper, monitor);
    }
    // Else a child module
    else {
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
        publishArchiveModule(childURI, kind, deltaKind, p, moduleTree, helper, monitor);
      } else {
        publishJar(childURI, kind, deltaKind, p, moduleTree, helper, monitor);
      }
    }

    setModulePublishState(moduleTree, IServer.PUBLISH_STATE_NONE);

    saveModulePublishLocations(p);
  }

  /**
   * Publish a web module.
   * 
   * @param deltaKind
   * @param p
   * @param module
   * @param monitor
   * @throws CoreException
   */
  private void publishDir(int deltaKind, Properties p, IModule module[], PublishHelper helper, IProgressMonitor monitor)
      throws CoreException {
    List<IStatus> status = new ArrayList<>();
    // Remove if requested or if previously published and are now serving
    // without publishing
    if ((deltaKind == ServerBehaviourDelegate.REMOVED)) {
      String publishPath = (String) p.get(module[0].getId());
      if (publishPath != null) {
        try {
          File f = new File(publishPath);
          if (f.exists()) {
            IStatus[] stat = PublishHelper.deleteDirectory(f, monitor);
            PublishOperation2.addArrayToList(status, stat);
          }
        } catch (Exception e) {
          throw new CoreException(new Status(IStatus.WARNING, ServerPlugin.PLUGIN_ID, 0,
              NLS.bind(Messages.errorPublishCouldNotRemoveModule, module[0].getName()), e));
        }
        p.remove(module[0].getId());
      }
    } else {
      IPath path = getModuleDeployDirectory(module[0]);
      IModuleResource[] mr = getResources(module);
      IPath[] jarPaths = null;
      IWebModule webModule = (IWebModule) module[0].loadAdapter(IWebModule.class, monitor);
      IModule[] childModules = getServer().getChildModules(module, monitor);
      if ((childModules != null) && (childModules.length > 0)) {
        jarPaths = new IPath[childModules.length];
        for (int i = 0; i < childModules.length; i++) {
          if (webModule != null) {
            jarPaths[i] = new Path(webModule.getURI(childModules[i]));
          } else {
            IJ2EEModule childModule = (IJ2EEModule) childModules[i].loadAdapter(IJ2EEModule.class, monitor);
            if ((childModule != null) && childModule.isBinary()) {
              jarPaths[i] = new Path("WEB-INF/lib").append(childModules[i].getName());
            } else {
              jarPaths[i] = new Path("WEB-INF/lib").append(childModules[i].getName() + ".jar");
            }
          }
        }
      }
      IStatus[] stat = helper.publishSmart(mr, path, jarPaths, monitor);
      PublishOperation2.addArrayToList(status, stat);
      p.put(module[0].getId(), path.toOSString());
    }
    PublishOperation2.throwException(status);
  }

  /**
   * Publish a jar file.
   * 
   * @param deltaKind
   * @param p
   * @param module
   * @param monitor
   * @throws CoreException
   */
  private void publishJar(String jarURI, int kind, int deltaKind, Properties p, IModule[] module, PublishHelper helper,
      IProgressMonitor monitor) throws CoreException {
    // Remove if requested or if previously published and are now serving
    // without publishing
    if ((deltaKind == ServerBehaviourDelegate.REMOVED)) {
      try {
        String publishPath = (String) p.get(module[1].getId());
        if (publishPath != null) {
          new File(publishPath).delete();
          p.remove(module[1].getId());
        }
      } catch (Exception e) {
        throw new CoreException(new Status(IStatus.WARNING, ServerPlugin.PLUGIN_ID, 0, "Could not remove module", e));
      }
    } else {
      IPath path = getModuleDeployDirectory(module[0]);
      if (jarURI == null) {
        jarURI = "WEB-INF/lib" + module[1].getName() + ".jar";
      }
      IPath jarPath = path.append(jarURI);
      path = jarPath.removeLastSegments(1);
      if (!path.toFile().exists()) {
        path.toFile().mkdirs();
      } else {
        // If file still exists and we are not forcing a new one to be built
        if (jarPath.toFile().exists() && (kind != IServer.PUBLISH_CLEAN) && (kind != IServer.PUBLISH_FULL)) {
          // avoid changes if no changes to module since last publish
          IModuleResourceDelta[] delta = getPublishedResourceDelta(module);
          if ((delta == null) || (delta.length == 0)) {
            return;
          }
        }
      }

      IModuleResource[] mr = getResources(module);
      IStatus[] stat = helper.publishZip(mr, jarPath, monitor);
      List<IStatus> status = new ArrayList<>();
      PublishOperation2.addArrayToList(status, stat);
      PublishOperation2.throwException(status);
      p.put(module[1].getId(), jarPath.toOSString());
    }
  }

  private void publishArchiveModule(String jarURI, int kind, int deltaKind, Properties p, IModule[] module,
      PublishHelper helper, IProgressMonitor monitor) throws CoreException {
    // Remove if requested or if previously published and are now serving
    // without publishing
    if ((deltaKind == ServerBehaviourDelegate.REMOVED)) {
      try {
        String publishPath = (String) p.get(module[1].getId());
        if (publishPath != null) {
          new File(publishPath).delete();
          p.remove(module[1].getId());
        }
      } catch (Exception e) {
        throw new CoreException(
            new Status(IStatus.WARNING, ServerPlugin.PLUGIN_ID, 0, "Could not remove archive module", e));
      }
    } else {
      List<IStatus> status = new ArrayList<>();
      IPath path = getModuleDeployDirectory(module[0]);
      if (jarURI == null) {
        jarURI = "WEB-INF/lib" + module[1].getName();
      }
      IPath jarPath = path.append(jarURI);
      path = jarPath.removeLastSegments(1);
      if (!path.toFile().exists()) {
        path.toFile().mkdirs();
      } else {
        // If file still exists and we are not forcing a new one to be built
        if (jarPath.toFile().exists() && (kind != IServer.PUBLISH_CLEAN) && (kind != IServer.PUBLISH_FULL)) {
          // avoid changes if no changes to module since last publish
          IModuleResourceDelta[] delta = getPublishedResourceDelta(module);
          if ((delta == null) || (delta.length == 0)) {
            return;
          }
        }
      }

      IModuleResource[] mr = getResources(module);
      IStatus[] stat = helper.publishToPath(mr, jarPath, monitor);
      PublishOperation2.addArrayToList(status, stat);
      PublishOperation2.throwException(status);
      p.put(module[1].getId(), jarPath.toOSString());
    }
  }

  @Override
  protected void publishFinish(IProgressMonitor monitor) throws CoreException {
    IStatus status;
    IPath baseDir = getRuntimeBaseDirectory();
    IServerVersionHandler handler = getVersionHandler();
    String serverTypeID = getServer().getServerType().getId();
    String serverVersion = VersionHelper.getVersion(baseDir, serverTypeID);
    // Include or remove loader jar depending on state of serving directly
    status = handler.prepareForServingDirectly(baseDir, getWrapper(), serverVersion);
    if (!status.isOK()) {
      throw new CoreException(status);
    }
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
    IServerConfiguration configuration = getConfiguration();

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

      ILaunchConfiguration launchConfig = ((Server) getServer()).getLaunchConfiguration(true, null);
      ILaunchConfigurationWorkingCopy wc = launchConfig.getWorkingCopy();

      String args = ServerBehaviour.renderCommandLine(getRuntimeProgramArguments(false), " ");
      // Remove JMX arguments if present
      String existingVMArgs = wc.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);
      if (existingVMArgs.indexOf(ServerBehaviour.JMX_EXCLUDE_ARGS[0]) >= 0) {
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
            ServerBehaviour.mergeArguments(existingVMArgs, new String[] {}, ServerBehaviour.JMX_EXCLUDE_ARGS, false));
      }
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
  public IPath getTempDirectory() {
    return super.getTempDirectory(false);
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

  private static int getNextToken(String s, int start) {
    int i = start;
    int length = s.length();
    char lookFor = ' ';

    while (i < length) {
      char c = s.charAt(i);
      if (lookFor == c) {
        if (lookFor == '"') {
          return i + 1;
        }
        return i;
      }
      if (c == '"') {
        lookFor = '"';
      }
      i++;
    }
    return -1;
  }

  /**
   * Merge the given arguments into the original argument string, replacing
   * invalid values if they have been changed. Special handling is provided if
   * the keepActionLast argument is true and the last vmArg is a simple string.
   * The vmArgs will be merged such that the last vmArg is guaranteed to be the
   * last argument in the merged string.
   * 
   * @param originalArg String of original arguments.
   * @param vmArgs Arguments to merge into the original arguments string
   * @param excludeArgs Arguments to exclude from the original arguments string
   * @param keepActionLast If <b>true</b> the vmArguments are assumed to be
   *        server program arguments, the last of which is the action to perform
   *        which must remain the last argument. This only has an impact if the
   *        last vmArg is a simple string argument, like &quot;start&quot;.
   * @return merged argument string
   */
  private static String mergeArguments(String originalArg, String[] vmArgs, String[] excludeArgs,
      boolean keepActionLast) {
    if (vmArgs == null) {
      return originalArg;
    }

    if (originalArg == null) {
      originalArg = "";
    }

    // replace and null out all vmargs that already exist
    int size = vmArgs.length;
    for (int i = 0; i < size; i++) {
      int ind = vmArgs[i].indexOf(" ");
      int ind2 = vmArgs[i].indexOf("=");
      if ((ind >= 0) && ((ind2 == -1) || (ind < ind2))) { // -a bc style
        int index = originalArg.indexOf(vmArgs[i].substring(0, ind + 1));
        if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
          // replace
          String s = originalArg.substring(0, index);
          int index2 = ServerBehaviour.getNextToken(originalArg, index + ind + 1);
          if (index2 >= 0) {
            originalArg = s + vmArgs[i] + originalArg.substring(index2);
          } else {
            originalArg = s + vmArgs[i];
          }
          vmArgs[i] = null;
        }
      } else if (ind2 >= 0) { // a=b style
        int index = originalArg.indexOf(vmArgs[i].substring(0, ind2 + 1));
        if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
          // replace
          String s = originalArg.substring(0, index);
          int index2 = ServerBehaviour.getNextToken(originalArg, index);
          if (index2 >= 0) {
            originalArg = s + vmArgs[i] + originalArg.substring(index2);
          } else {
            originalArg = s + vmArgs[i];
          }
          vmArgs[i] = null;
        }
      } else { // abc style
        int index = originalArg.indexOf(vmArgs[i]);
        if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
          // replace
          String s = originalArg.substring(0, index);
          int index2 = ServerBehaviour.getNextToken(originalArg, index);
          if (!keepActionLast || (i < (size - 1))) {
            if (index2 >= 0) {
              originalArg = s + vmArgs[i] + originalArg.substring(index2);
            } else {
              originalArg = s + vmArgs[i];
            }
            vmArgs[i] = null;
          } else {
            // The last VM argument needs to remain last,
            // remove original arg and append the vmArg later
            if (index2 >= 0) {
              originalArg = s + originalArg.substring(index2);
            } else {
              originalArg = s;
            }
          }
        }
      }
    }

    // remove excluded arguments
    if ((excludeArgs != null) && (excludeArgs.length > 0)) {
      for (String excludeArg : excludeArgs) {
        int ind = excludeArg.indexOf(" ");
        int ind2 = excludeArg.indexOf("=");
        if ((ind >= 0) && ((ind2 == -1) || (ind < ind2))) { // -a bc style
          int index = originalArg.indexOf(excludeArg.substring(0, ind + 1));
          if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
            // remove
            String s = originalArg.substring(0, index);
            int index2 = ServerBehaviour.getNextToken(originalArg, index + ind + 1);
            if (index2 >= 0) {
              // If remainder will become the first argument, remove leading
              // blanks
              while ((index2 < originalArg.length()) && Character.isWhitespace(originalArg.charAt(index2))) {
                index2 += 1;
              }
              originalArg = s + originalArg.substring(index2);
            } else {
              originalArg = s;
            }
          }
        } else if (ind2 >= 0) { // a=b style
          int index = originalArg.indexOf(excludeArg.substring(0, ind2 + 1));
          if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
            // remove
            String s = originalArg.substring(0, index);
            int index2 = ServerBehaviour.getNextToken(originalArg, index);
            if (index2 >= 0) {
              // If remainder will become the first argument, remove leading
              // blanks
              while ((index2 < originalArg.length()) && Character.isWhitespace(originalArg.charAt(index2))) {
                index2 += 1;
              }
              originalArg = s + originalArg.substring(index2);
            } else {
              originalArg = s;
            }
          }
        } else { // abc style
          int index = originalArg.indexOf(excludeArg);
          if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
            // remove
            String s = originalArg.substring(0, index);
            int index2 = ServerBehaviour.getNextToken(originalArg, index);
            if (index2 >= 0) {
              // Remove leading blanks
              while ((index2 < originalArg.length()) && Character.isWhitespace(originalArg.charAt(index2))) {
                index2 += 1;
              }
              originalArg = s + originalArg.substring(index2);
            } else {
              originalArg = s;
            }
          }
        }
      }
    }

    // add remaining vmargs to the end
    for (int i = 0; i < size; i++) {
      if (vmArgs[i] != null) {
        if ((originalArg.length() > 0) && !originalArg.endsWith(" ")) {
          originalArg += " ";
        }
        originalArg += vmArgs[i];
      }
    }

    return originalArg;
  }

  /**
   * Merge a single classpath entry into the classpath list.
   * 
   * @param cp
   * @param entry
   */
  private static void mergeClasspath(List<IRuntimeClasspathEntry> cp, IRuntimeClasspathEntry entry) {
    Iterator<IRuntimeClasspathEntry> iterator = cp.iterator();
    while (iterator.hasNext()) {
      IRuntimeClasspathEntry entry2 = iterator.next();

      if (entry2.getPath().equals(entry.getPath())) {
        return;
      }
    }

    cp.add(entry);
  }

  @Override
  public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor)
      throws CoreException {
    String existingProgArgs =
        workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String) null);
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
        ServerBehaviour.mergeArguments(existingProgArgs, getRuntimeProgramArguments(true), null, true));

    String existingVMArgs =
        workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);
    String[] parsedVMArgs = null;
    if (null != existingVMArgs) {
      parsedVMArgs = DebugPlugin.parseArguments(existingVMArgs);
    }
    String[] configVMArgs = getRuntimeVMArguments();
    if (getWrapper().isSecure()) {
      boolean addSecurityArgs = true;
      if (null != parsedVMArgs) {
        for (String parsedVMArg : parsedVMArgs) {
          if (parsedVMArg.startsWith("wtp.configured.security")) {
            addSecurityArgs = false;
            break;
          }
        }
      }
      if (addSecurityArgs) {
        String[] newVMArgs = new String[configVMArgs.length + 3];
        System.arraycopy(configVMArgs, 0, newVMArgs, 0, configVMArgs.length);
        newVMArgs[configVMArgs.length] = "-Djava.security.manager";
        newVMArgs[configVMArgs.length + 2] = "-Dwtp.configured.security=true";
        configVMArgs = newVMArgs;
      }
    } else if (null != parsedVMArgs) {
      boolean removeSecurityArgs = false;
      for (String parsedVMArg : parsedVMArgs) {
        if (parsedVMArg.startsWith("-Dwtp.configured.security")) {
          removeSecurityArgs = true;
          break;
        }
      }
      if (removeSecurityArgs) {
        StringBuffer filteredVMArgs = new StringBuffer();
        for (String arg : parsedVMArgs) {
          if (!arg.startsWith("-Djava.security.manager") && !arg.startsWith("-Djava.security.policy=")
              && !arg.startsWith("-Dwtp.configured.security=")) {
            if (filteredVMArgs.length() > 0) {
              filteredVMArgs.append(' ');
            }
            filteredVMArgs.append(arg);
          }
        }
        existingVMArgs = filteredVMArgs.toString();
      }
    }
    String mergedVMArguments = ServerBehaviour.mergeArguments(existingVMArgs, configVMArgs, null, false);
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
      ServerBehaviour.mergeClasspath(oldCp, entry);
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

  @Override
  protected IModuleResource[] getResources(IModule[] module) {
    return super.getResources(module);
  }

  @Override
  protected IModuleResourceDelta[] getPublishedResourceDelta(IModule[] module) {
    return super.getPublishedResourceDelta(module);
  }

  /**
   * @see ServerBehaviourDelegate#handleResourceChange()
   */
  @Override
  public void handleResourceChange() {
    if (getServer().getServerRestartState()) {
      return;
    }

    Iterator<IModule[]> iterator = getAllModules().iterator();
    while (iterator.hasNext()) {
      IModule[] module = iterator.next();
      IModuleResourceDelta[] delta = getPublishedResourceDelta(module);
      if ((delta == null) || (delta.length == 0)) {
        continue;
      }

      if (containsNonResourceChange(delta)) {
        setServerRestartState(true);
        return;
      }
    }
  }

  private boolean containsNonResourceChange(IModuleResourceDelta[] delta) {
    int size = delta.length;
    for (int i = 0; i < size; i++) {
      IModuleResourceDelta d = delta[i];
      if (d.getModuleRelativePath().segmentCount() == 0) {
        if ("WEB-INF".equals(d.getModuleResource().getName())) {
          return containsNonResourceChange(d.getAffectedChildren());
        }
        continue;
      }
      if (d.getModuleResource() instanceof IModuleFile) {
        return true;
      }

      boolean b = containsNonAddChange(d.getAffectedChildren());
      if (b) {
        return true;
      }
    }
    return false;
  }

  private boolean containsNonAddChange(IModuleResourceDelta[] delta) {
    if (delta == null) {
      return false;
    }
    int size = delta.length;
    for (int i = 0; i < size; i++) {
      IModuleResourceDelta d = delta[i];
      if (d.getModuleResource() instanceof IModuleFile) {
        if (d.getKind() != IModuleResourceDelta.ADDED) {
          return true;
        }
      }

      boolean b = containsNonAddChange(d.getAffectedChildren());
      if (b) {
        return true;
      }
    }
    return false;
  }

  /**
   * Cleans the entire work directory for this server. This involves deleting
   * all subdirectories of the server's work directory.
   * 
   * @param monitor a progress monitor
   * @return results of the clean operation
   * @throws CoreException
   */
  public IStatus cleanServerWorkDir(IProgressMonitor monitor) throws CoreException {
    IStatus result;
    IPath basePath = getRuntimeBaseDirectory();
    IPath workPath = getConfiguration().getServerWorkDirectory(basePath);
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
   * Cleans the work directory associated with the specified module on this
   * server.
   * 
   * @param module module whose work directory should be cleaned
   * @param monitor a progress monitor
   * @return result of the clean operation
   * @throws CoreException
   */
  public IStatus cleanContextWorkDir(IServerWebModule module, IProgressMonitor monitor) throws CoreException {
    IStatus result;
    IPath basePath = getRuntimeBaseDirectory();
    IPath workPath = getConfiguration().getContextWorkDirectory(basePath, module);
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
   * 
   * @return full path to deployment directory for the server
   */
  public IPath getServerDeployDirectory() {
    return getWrapper().getServerDeployDirectory();
  }

  /**
   * Gets the directory to which to deploy a module's web application.
   * 
   * @param module a module
   * @return full path to deployment directory for the module
   */
  IPath getModuleDeployDirectory(IModule module) {
    return getServerDeployDirectory().append(module.getName());
  }

  /**
   * Temporary method to help web services team. Returns the path that the
   * module is published to.
   * 
   * @param module a module on the server
   * @return the path that the module is published to when in test environment
   *         mode, or null if the module is not a web module
   */
  @Override
  public IPath getPublishDirectory(IModule[] module) {
    if ((module == null) || (module.length != 1)) {
      return null;
    }

    return getModuleDeployDirectory(module[0]);
  }

  void setModulePublishState2(IModule[] module, int state) {
    setModulePublishState(module, state);
  }

  Properties loadModulePublishLocations() {
    Properties p = new Properties();
    IPath path = getTempDirectory().append("publish.txt");
    FileInputStream fin = null;
    try {
      fin = new FileInputStream(path.toFile());
      p.load(fin);
    } catch (Exception e) {
      // ignore
    } finally {
      try {
        fin.close();
      } catch (Exception ex) {
        // ignore
      }
    }
    return p;
  }

  void saveModulePublishLocations(Properties p) {
    IPath path = getTempDirectory().append("publish.txt");
    FileOutputStream fout = null;
    try {
      fout = new FileOutputStream(path.toFile());
      p.store(fout, "smart.IO publish data");
    } catch (Exception e) {
      // ignore
    } finally {
      try {
        fout.close();
      } catch (Exception ex) {
        // ignore
      }
    }
  }

  public void setServerStates(int publishState, boolean restartState) {
    setServerPublishState(publishState);
    setServerRestartState(restartState);
  }
}
