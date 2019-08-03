/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others. All rights reserved. This program and the
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
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.model.RuntimeLocatorDelegate;

import java.io.File;
import java.io.FileFilter;

/**
 *
 */
public class ServerRuntimeLocator extends RuntimeLocatorDelegate {

  private static final String[] runtimeTypes = new String[] { "org.eclipse.jst.server.smartio.runtime.10" };

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.server.core.model.IRuntimeFactoryDelegate#getKnownRuntimes()
   */
  @Override
  public void searchForRuntimes(IPath path, IRuntimeSearchListener listener, IProgressMonitor monitor) {
    ServerRuntimeLocator.searchForRuntimes2(path, listener, monitor);
  }

  protected static void searchForRuntimes2(IPath path, IRuntimeSearchListener listener, IProgressMonitor monitor) {
    File[] files = null;
    if (path != null) {
      File f = path.toFile();
      if (f.exists()) {
        files = f.listFiles();
      } else {
        return;
      }
    } else {
      files = File.listRoots();
    }

    if (files != null) {
      int size = files.length;
      int work = 100 / size;
      int workLeft = 100 - (work * size);
      for (int i = 0; i < size; i++) {
        if (monitor.isCanceled()) {
          return;
        }
        if ((files[i] != null) && files[i].isDirectory()) {
          ServerRuntimeLocator.searchDir(listener, files[i], 4, monitor);
        }
        monitor.worked(work);
      }
      monitor.worked(workLeft);
    } else {
      monitor.worked(100);
    }
  }

  private static void searchDir(IRuntimeSearchListener listener, File dir, int depth, IProgressMonitor monitor) {
    if ("conf".equals(dir.getName())) {
      IRuntimeWorkingCopy runtime = ServerRuntimeLocator.getRuntimeFromDir(dir.getParentFile(), monitor);
      if (runtime != null) {
        listener.runtimeFound(runtime);
        return;
      }
    }

    if (depth == 0) {
      return;
    }

    File[] files = dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File file) {
        return file.isDirectory();
      }
    });
    if (files != null) {
      int size = files.length;
      for (int i = 0; i < size; i++) {
        if (monitor.isCanceled()) {
          return;
        }
        ServerRuntimeLocator.searchDir(listener, files[i], depth - 1, monitor);
      }
    }
  }

  private static IRuntimeWorkingCopy getRuntimeFromDir(File dir, IProgressMonitor monitor) {
    for (String runtimeType2 : ServerRuntimeLocator.runtimeTypes) {
      try {
        IRuntimeType runtimeType = ServerCore.findRuntimeType(runtimeType2);
        String absolutePath = dir.getAbsolutePath();
        String id = absolutePath.replace(File.separatorChar, '_').replace(':', '-');
        IRuntimeWorkingCopy runtime = runtimeType.createRuntime(id, monitor);
        runtime.setName(dir.getName());
        runtime.setLocation(new Path(absolutePath));
        IServerRuntimeWorkingCopy wc =
            (IServerRuntimeWorkingCopy) runtime.loadAdapter(IServerRuntimeWorkingCopy.class, null);
        wc.setVMInstall(JavaRuntime.getDefaultVMInstall());
        IStatus status = runtime.validate(monitor);
        if ((status == null) || (status.getSeverity() != IStatus.ERROR)) {
          return runtime;
        }

        Trace.trace(Trace.FINER, "False runtime found at " + dir.getAbsolutePath() + ": " + status.getMessage());
      } catch (Exception e) {
        Trace.trace(Trace.SEVERE, "Could not find runtime", e);
      }
    }
    return null;
  }
}
