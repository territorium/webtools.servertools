/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.model.RuntimeDelegate;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 */
public class ServerRuntime extends RuntimeDelegate implements IServerRuntime {

  public IServerVersionHandler getVersionHandler() {
    String id = getRuntime().getRuntimeType().getId();
    if (id.indexOf("runtime") > 0) {
      id = id.substring(0, 30) + id.substring(38);
    }
    // id = id.substring(0, id.length() - 8);
    if (ServerPlugin.SERVER_10.equals(id)) {
      return new Server10Handler();
    } else {
      return null;
    }
  }

  @Override
  public List<IRuntimeClasspathEntry> getRuntimeClasspath(IPath configPath) {
    IPath installPath = getRuntime().getLocation();
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


    status = getVersionHandler().verifyInstallPath(getRuntime().getLocation());
    if (!status.isOK()) {
      return status;
    }
    // return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
    // Messages.errorInstallDir, null);
    // don't accept trailing space since that can cause startup problems
    if (getRuntime().getLocation().hasTrailingSeparator()) {
      return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorInstallDirTrailingSlash, null);
    }

    // check for tools.jar (contains the javac compiler on Windows & Linux) to
    // see whether
    // server will be able to compile JSPs.
    boolean found = false;
    String id = getRuntime().getRuntimeType().getId();
    if (!found) {
      if (id.indexOf("10") > 0) {
        found = true;
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

    return Status.OK_STATUS;
  }

  /**
   * @see RuntimeDelegate#setDefaults(IProgressMonitor)
   */
  @Override
  public void setDefaults(IProgressMonitor monitor) {
    IRuntimeType type = getRuntimeWorkingCopy().getRuntimeType();
    Path path = new Path(ServerPlugin.getPreference("location" + type.getId()));
    if ((path != null) && path.toFile().exists() && getVersionHandler().verifyInstallPath(path).isOK()) {
      getRuntimeWorkingCopy().setLocation(path);
    }
  }
}
