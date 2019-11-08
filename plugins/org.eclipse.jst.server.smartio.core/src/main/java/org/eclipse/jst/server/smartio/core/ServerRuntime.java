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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.wst.server.core.model.RuntimeDelegate;

import java.io.File;
import java.util.List;

/**
 * Implements a server {@link RuntimeDelegate}.
 */
public class ServerRuntime extends RuntimeDelegate implements IServerRuntime {

  /**
   * Returns the runtime class-path that is used by this runtime. If installPath is relative,
   * convert to canonical path and hope for the best.
   *
   * @param configPath
   */
  @Override
  public final List<IRuntimeClasspathEntry> getRuntimeClasspath(IPath configPath) {
    return getHandler().getRuntimeClasspath(configPath);
  }

  /**
   * Validate the runtime.
   *
   * @see RuntimeDelegate#validate()
   */
  @Override
  public IStatus validate() {
    IStatus status = super.validate();
    if (!status.isOK()) {
      return status;
    }

    status = getHandler().verifyInstallPath(getRuntime().getLocation());
    if (!status.isOK()) {
      return status;
    }

    // don't accept trailing space since that can cause startup problems
    if (getRuntime().getLocation().hasTrailingSeparator()) {
      return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorInstallDirTrailingSlash, null);
    }

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

    File file = getRuntime().getLocation().append("conf").toFile();
    File[] conf = file.listFiles();
    if (conf != null) {
      int size = conf.length;
      for (int i = 0; i < size; i++) {
        if (!file.canRead()) {
          return new Status(IStatus.WARNING, ServerPlugin.PLUGIN_ID, 0, Messages.warningCantReadConfig, null);
        }
      }
    }
    return Status.OK_STATUS;
  }

  /**
   * Get the {@link IServerInstallation} for the specified version.
   */
  protected final IServerInstallation getHandler() {
    String id = getRuntime().getRuntimeType().getId();
    if (ServerPlugin.RUNTIME_10.equals(id)) {
      return new Server10Installation();
    }
    return null;
  }
}
