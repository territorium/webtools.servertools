/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.model.RuntimeDelegate;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class ServerRuntime extends RuntimeDelegate implements IServerRuntime, IServerRuntimeWorkingCopy {

  private static final String       PROP_VM_INSTALL_TYPE_ID = "vm-install-type-id";
  private static final String       PROP_VM_INSTALL_ID      = "vm-install-id";

  private static Map<File, Boolean> sdkMap                  = new HashMap<>(2);

  public ServerRuntime() {
    // do nothing
  }

  private String getVMInstallTypeId() {
    return getAttribute(ServerRuntime.PROP_VM_INSTALL_TYPE_ID, (String) null);
  }

  private String getVMInstallId() {
    return getAttribute(ServerRuntime.PROP_VM_INSTALL_ID, (String) null);
  }

  /**
   * Verifies the installation directory. If it is correct, true is returned. Otherwise, the
   * user is notified and false is returned.
   * 
   * @return boolean
   */
  private IStatus verifyLocation() {
    return verifyLocation(getRuntime().getLocation());
  }

  private IStatus verifyLocation(IPath loc) {
    return getVersionHandler().verifyInstallPath(loc);
  }

  public IServerVersionHandler getVersionHandler() {
    IRuntimeType type = getRuntime().getRuntimeType();
    return ServerPlugin.getVersionHandler(type.getId());
  }

  @Override
  public boolean isUsingDefaultJRE() {
    return getVMInstallTypeId() == null;
  }

  @Override
  public IVMInstall getVMInstall() {
    if (getVMInstallTypeId() == null) {
      return JavaRuntime.getDefaultVMInstall();
    }
    try {
      IVMInstallType vmInstallType = JavaRuntime.getVMInstallType(getVMInstallTypeId());
      IVMInstall[] vmInstalls = vmInstallType.getVMInstalls();
      int size = vmInstalls.length;
      String id = getVMInstallId();
      for (int i = 0; i < size; i++) {
        if (id.equals(vmInstalls[i].getId())) {
          return vmInstalls[i];
        }
      }
    } catch (Exception e) {
      // ignore
    }
    return null;
  }

  @Override
  public List<IRuntimeClasspathEntry> getRuntimeClasspath(IPath configPath) {
    IPath installPath = getRuntime().getLocation();
    // If installPath is relative, convert to canonical path and hope for the best
    if (!installPath.isAbsolute()) {
      try {
        String installLoc = (new File(installPath.toOSString())).getCanonicalPath();
        installPath = new Path(installLoc);
      } catch (IOException e) {
        // Ignore if there is a problem
      }
    }
    return getVersionHandler().getRuntimeClasspath(installPath, configPath);
  }

  /*
   * Validate the runtime
   */
  @Override
  public IStatus validate() {
    IStatus status = super.validate();
    if (!status.isOK()) {
      return status;
    }

    status = verifyLocation();
    if (!status.isOK()) {
      return status;
    }
    // return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorInstallDir, null);
    // don't accept trailing space since that can cause startup problems
    if (getRuntime().getLocation().hasTrailingSeparator()) {
      return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorInstallDirTrailingSlash, null);
    }
    if (getVMInstall() == null) {
      return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorJRE, null);
    }

    // check for tools.jar (contains the javac compiler on Windows & Linux) to see whether
    // server will be able to compile JSPs.
    boolean found = false;
    File file = getVMInstall().getInstallLocation();
    if (file != null) {
      File toolsJar = new File(file, "lib" + File.separator + "tools.jar");
      if (toolsJar.exists()) {
        found = true;
      }
    }

    String id = getRuntime().getRuntimeType().getId();
    if (!found) {
      if (id.indexOf("10") > 0) {
        found = true;
      }
    }

    // on Mac, tools.jar is merged into classes.zip. if tools.jar wasn't found,
    // try loading the javac class by running a check inside the VM
    if (!found) {
      String os = Platform.getOS();
      if ((os != null) && (os.toLowerCase().indexOf("mac") >= 0)) {
        found = checkForCompiler();
      }
    }

    if (!found) {
      return new Status(IStatus.WARNING, ServerPlugin.PLUGIN_ID, 0, Messages.warningJRE, null);
    }

    File f = getRuntime().getLocation().append("conf").toFile();
    File[] conf = f.listFiles();
    if (conf != null) {
      int size = conf.length;
      for (int i = 0; i < size; i++) {
        if (!f.canRead()) {
          return new Status(IStatus.WARNING, ServerPlugin.PLUGIN_ID, 0, Messages.warningCantReadConfig, null);
        }
      }
    }

    // smart.IO 1.0, ensure we have J2SE 8.0
    if ((id != null) && (id.indexOf("10") > 0)) {
      IVMInstall vmInstall = getVMInstall();
      if (vmInstall instanceof IVMInstall2) {
        String javaVersion = ((IVMInstall2) vmInstall).getJavaVersion();
        if ((javaVersion != null) && !isVMMinimumVersion(javaVersion, 108)) {
          return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorJREServer10, null);
        }
      }
    }

    return Status.OK_STATUS;
  }

  /**
   * @see RuntimeDelegate#setDefaults(IProgressMonitor)
   */
  @Override
  public void setDefaults(IProgressMonitor monitor) {
    IRuntimeType type = getRuntimeWorkingCopy().getRuntimeType();
    Path p = new Path(ServerPlugin.getPreference("location" + type.getId()));
    if ((p != null) && p.toFile().exists() && verifyLocation(p).isOK()) {
      getRuntimeWorkingCopy().setLocation(p);
    }
  }

  @Override
  public void setVMInstall(IVMInstall vmInstall) {
    if (vmInstall == null) {
      setVMInstall(null, null);
    } else {
      setVMInstall(vmInstall.getVMInstallType().getId(), vmInstall.getId());
    }
  }

  private void setVMInstall(String typeId, String id) {
    if (typeId == null) {
      setAttribute(ServerRuntime.PROP_VM_INSTALL_TYPE_ID, (String) null);
    } else {
      setAttribute(ServerRuntime.PROP_VM_INSTALL_TYPE_ID, typeId);
    }

    if (id == null) {
      setAttribute(ServerRuntime.PROP_VM_INSTALL_ID, (String) null);
    } else {
      setAttribute(ServerRuntime.PROP_VM_INSTALL_ID, id);
    }
  }

  /**
   * Checks for the existence of the Java compiler in the given java executable. A main program is
   * run (<code>org.eclipse.jst.smartio.core. internal.ClassDetector</code>), that dumps a true or
   * false value depending on whether the compiler is found. This output is then parsed and cached
   * for future reference.
   * 
   * @return true if the compiler was found
   */
  private boolean checkForCompiler() {
    // first try the cache
    File javaHome = getVMInstall().getInstallLocation();
    try {
      Boolean b = ServerRuntime.sdkMap.get(javaHome);
      return b.booleanValue();
    } catch (Exception e) {
      // ignore
    }

    // locate tomcatcore.jar - it contains the class detector main program
    File file = ServerPlugin.getPlugin();
    if ((file != null) && file.exists()) {
      IVMRunner vmRunner = getVMInstall().getVMRunner(ILaunchManager.RUN_MODE);
      VMRunnerConfiguration config = new VMRunnerConfiguration(
          "org.eclipse.jst.server.smartio.core.internal.ClassDetector", new String[] { file.getAbsolutePath() });
      config.setProgramArguments(new String[] { "com.sun.tools.javac.Main" });
      ILaunch launch = new Launch(null, ILaunchManager.RUN_MODE, null);
      try {
        vmRunner.run(config, launch, null);
        for (int i = 0; i < 600; i++) {
          // wait no more than 30 seconds (600 * 50 mils)
          if (launch.isTerminated()) {
            break;
          }
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            // ignore
          }
        }
        IStreamsProxy streamsProxy = launch.getProcesses()[0].getStreamsProxy();
        String text = null;
        if (streamsProxy != null) {
          text = streamsProxy.getOutputStreamMonitor().getContents();

          if ((text != null) && (text.length() > 0)) {
            boolean found = false;
            if ("true".equals(text)) {
              found = true;
            }

            ServerRuntime.sdkMap.put(javaHome, new Boolean(found));
            return found;
          }
        }
      } catch (Exception e) {
        Trace.trace(Trace.SEVERE, "Error checking for JDK", e);
      } finally {
        if (!launch.isTerminated()) {
          try {
            launch.terminate();
          } catch (Exception ex) {
            // ignore
          }
        }
      }
    }

    // log error that we were unable to check for the compiler
    String message = MessageFormat.format("Failed compiler check for {0}", (Object[]) new String[] { javaHome.getAbsolutePath() });
    ServerPlugin.log(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, IStatus.ERROR, message, null));
    return false;
  }

  private static Map<String, Integer> javaVersionMap = new ConcurrentHashMap<>();

  private boolean isVMMinimumVersion(String javaVersion, int minimumVersion) {
    Integer version = ServerRuntime.javaVersionMap.get(javaVersion);
    if (version == null) {
      int index = javaVersion.indexOf('.');
      if (index > 0) {
        try {
          int major = Integer.parseInt(javaVersion.substring(0, index)) * 100;
          index++;
          int index2 = javaVersion.indexOf('.', index);
          if (index2 > 0) {
            int minor = Integer.parseInt(javaVersion.substring(index, index2));
            version = new Integer(major + minor);
            ServerRuntime.javaVersionMap.put(javaVersion, version);
          }
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    // If we have a version, and it's less than the minimum, fail the check
    if ((version != null) && (version.intValue() < minimumVersion)) {
      return false;
    }
    return true;
  }
}
