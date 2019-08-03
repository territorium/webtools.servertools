/*
 * Copyright (c) 2001-2019 Territorium Online Srl / TOL GmbH. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as defined in and that are
 * subject to the Territorium Online License Version 1.0. You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at http://www.tol.info/license/
 * and read it before using this file.
 *
 * The Original Code and all software distributed under the License are distributed on an 'AS IS'
 * basis, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, AND TERRITORIUM ONLINE HEREBY
 * DISCLAIMS ALL SUCH WARRANTIES, INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT. Please see the License for
 * the specific language governing rights and limitations under the License.
 */

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.smartio.core.util.ServerTools;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The {@link PublishModule} class.
 */
public class PublishModule extends PublishHelper {

  private final ServerBehaviour  behaviour;
  private final IProgressMonitor monitor;

  /**
   * Constructs an instance of {@link PublishModule}.
   *
   * @param temp
   * @param behaviour
   * @param monitor
   */
  public PublishModule(File temp, ServerBehaviour behaviour, IProgressMonitor monitor) {
    super(temp);
    this.behaviour = behaviour;
    this.monitor = monitor;
  }

  /**
   * Gets the {@link ServerBehaviour}
   */
  public final ServerBehaviour getBehaviour() {
    return behaviour;
  }


  /**
   * Gets the {@link IProgressMonitor}.
   */
  public final IProgressMonitor getMonitor() {
    return monitor;
  }

  /**
   * Publish a web module.
   *
   * @param deltaKind
   * @param publish
   * @param module
   * @throws CoreException
   */
  public final void publishDirectory(int deltaKind, Properties publish, IModule module[]) throws CoreException {
    List<IStatus> status = new ArrayList<>();
    // Remove if requested or if previously published and are now serving
    // without publishing
    switch (deltaKind) {
      case ServerBehaviourDelegate.REMOVED:
        String publishPath = (String) publish.get(module[0].getId());
        if (publishPath != null) {
          try {
            File f = new File(publishPath);
            if (f.exists()) {
              IStatus[] stat = PublishHelper.deleteDirectory(f, getMonitor());
              ServerTools.addArrayToList(status, stat);
            }
          } catch (Exception e) {
            throw new CoreException(new Status(IStatus.WARNING, ServerPlugin.PLUGIN_ID, 0,
                NLS.bind(Messages.errorPublishCouldNotRemoveModule, module[0].getName()), e));
          }
          publish.remove(module[0].getId());
        }
        break;

      default:
        IPath path = getBehaviour().getPublishDirectory(module);
        IModuleResource[] mr = getBehaviour().getResources(module);
        IPath[] jarPaths = null;
        IWebModule webModule = (IWebModule) module[0].loadAdapter(IWebModule.class, getMonitor());
        IModule[] childModules = getBehaviour().getServer().getChildModules(module, getMonitor());
        if ((childModules != null) && (childModules.length > 0)) {
          jarPaths = new IPath[childModules.length];
          for (int i = 0; i < childModules.length; i++) {
            if (webModule != null) {
              jarPaths[i] = new Path(webModule.getURI(childModules[i]));
            } else {
              IJ2EEModule childModule = (IJ2EEModule) childModules[i].loadAdapter(IJ2EEModule.class, getMonitor());
              if ((childModule != null) && childModule.isBinary()) {
                jarPaths[i] = new Path("WEB-INF/lib").append(childModules[i].getName());
              } else {
                jarPaths[i] = new Path("WEB-INF/lib").append(childModules[i].getName() + ".jar");
              }
            }
          }
        }
        IStatus[] stat = publishSmart(mr, path, jarPaths, getMonitor());
        ServerTools.addArrayToList(status, stat);
        publish.put(module[0].getId(), path.toOSString());
        break;
    }
    ServerTools.throwException(status);
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
  public final void publishJar(String jarURI, int kind, int deltaKind, Properties publish, IModule[] module)
      throws CoreException {
    // Remove if requested or if previously published and are now serving
    // without publishing
    switch (deltaKind) {
      case ServerBehaviourDelegate.REMOVED:
        try {
          String publishPath = (String) publish.get(module[1].getId());
          if (publishPath != null) {
            new File(publishPath).delete();
            publish.remove(module[1].getId());
          }
        } catch (Exception e) {
          throw new CoreException(new Status(IStatus.WARNING, ServerPlugin.PLUGIN_ID, 0, "Could not remove module", e));
        }
        break;

      default:
        IPath path = getBehaviour().getPublishDirectory(module);
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
            IModuleResourceDelta[] delta = getBehaviour().getPublishedResourceDelta(module);
            if ((delta == null) || (delta.length == 0)) {
              return;
            }
          }
        }

        IModuleResource[] mr = getBehaviour().getResources(module);
        IStatus[] stat = publishZip(mr, jarPath, getMonitor());
        List<IStatus> status = new ArrayList<>();
        ServerTools.addArrayToList(status, stat);
        ServerTools.throwException(status);
        publish.put(module[1].getId(), jarPath.toOSString());
    }
  }

  public final void publishArchive(String jarURI, int kind, int deltaKind, Properties publish, IModule[] module)
      throws CoreException {
    // Remove if requested or if previously published and are now serving
    // without publishing
    switch (deltaKind) {
      case ServerBehaviourDelegate.REMOVED:
        try {
          String publishPath = (String) publish.get(module[1].getId());
          if (publishPath != null) {
            new File(publishPath).delete();
            publish.remove(module[1].getId());
          }
        } catch (Exception e) {
          throw new CoreException(
              new Status(IStatus.WARNING, ServerPlugin.PLUGIN_ID, 0, "Could not remove archive module", e));
        }
        break;

      default:
        List<IStatus> status = new ArrayList<>();
        IPath path = getBehaviour().getPublishDirectory(module);
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
            IModuleResourceDelta[] delta = getBehaviour().getPublishedResourceDelta(module);
            if ((delta == null) || (delta.length == 0)) {
              return;
            }
          }
          break;
        }

        IModuleResource[] mr = getBehaviour().getResources(module);
        IStatus[] stat = publishToPath(mr, jarPath, getMonitor());
        ServerTools.addArrayToList(status, stat);
        ServerTools.throwException(status);
        publish.put(module[1].getId(), jarPath.toOSString());
    }
  }
}
