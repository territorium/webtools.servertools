/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Server publish helper.
 */
class PublishOperation2 extends PublishOperation {

  private final ServerBehaviour server;
  private final IModule[]       module;
  private final int             kind;
  private final int             deltaKind;
  private PublishHelper         helper;

  /**
   * Construct the operation object to publish the specified module to the specified server.
   * 
   * @param server server to which the module will be published
   * @param kind kind of publish
   * @param module module to publish
   * @param deltaKind kind of change
   */
  PublishOperation2(ServerBehaviour server, int kind, IModule[] module, int deltaKind) {
    super("Publish to server", "Publish Web module to smart.IO server");
    this.server = server;
    this.module = module;
    this.kind = kind;
    this.deltaKind = deltaKind;
    IPath base = server.getRuntimeBaseDirectory();
    if (base != null) {
      helper = new PublishHelper(base.append("temp").toFile());
    } else {
      // We are doomed without a base directory. However, allow the catastrophe
      // to occur elsewhere and hope for a useful error message.
      helper = new PublishHelper(null);
    }
  }

  /**
   * @see PublishOperation#getOrder()
   */
  @Override
  public int getOrder() {
    return 0;
  }

  /**
   * @see PublishOperation#getKind()
   */
  @Override
  public int getKind() {
    return PublishOperation.REQUIRED;
  }

  /**
   * @see PublishOperation#execute(IProgressMonitor, IAdaptable)
   */
  @Override
  public void execute(IProgressMonitor monitor, IAdaptable info) throws CoreException {
    List<IStatus> status = new ArrayList<>();
    // If parent web module
    if (module.length == 1) {
      publishDir(module[0], status, monitor);
    }
    // Else a child module
    else {
      Properties p = server.loadModulePublishLocations();

      // Try to determine the URI for the child module
      IWebModule webModule = (IWebModule) module[0].loadAdapter(IWebModule.class, monitor);
      String childURI = null;
      if (webModule != null) {
        childURI = webModule.getURI(module[1]);
      }
      // Try to determine if child is binary
      IJ2EEModule childModule = (IJ2EEModule) module[1].loadAdapter(IJ2EEModule.class, monitor);
      boolean isBinary = false;
      if (childModule != null) {
        isBinary = childModule.isBinary();
      }

      if (isBinary) {
        publishArchiveModule(childURI, p, status, monitor);
      } else {
        publishJar(childURI, p, status, monitor);
      }
      server.saveModulePublishLocations(p);
    }
    PublishOperation2.throwException(status);
    server.setModulePublishState2(module, IServer.PUBLISH_STATE_NONE);
  }

  private void publishDir(IModule module2, List<IStatus> status, IProgressMonitor monitor) throws CoreException {
    IPath path = server.getModuleDeployDirectory(module2);

    // Remove if requested or if previously published and are now serving without publishing
    if ((kind == IServer.PUBLISH_CLEAN) || (deltaKind == ServerBehaviourDelegate.REMOVED)
        || server.getWrapper().isServeModulesWithoutPublish()) {
      File f = path.toFile();
      if (f.exists()) {
        IStatus[] stat = PublishHelper.deleteDirectory(f, monitor);
        PublishOperation2.addArrayToList(status, stat);
      }

      if ((deltaKind == ServerBehaviourDelegate.REMOVED) || server.getWrapper().isServeModulesWithoutPublish()) {
        return;
      }
    }

    if ((kind == IServer.PUBLISH_CLEAN) || (kind == IServer.PUBLISH_FULL)) {
      IModuleResource[] mr = server.getResources(module);
      IStatus[] stat = helper.publishFull(mr, path, monitor);
      PublishOperation2.addArrayToList(status, stat);
      return;
    }

    IModuleResourceDelta[] delta = server.getPublishedResourceDelta(module);

    int size = delta.length;
    for (int i = 0; i < size; i++) {
      IStatus[] stat = helper.publishDelta(delta[i], path, monitor);
      PublishOperation2.addArrayToList(status, stat);
    }
  }

  private void publishJar(String jarURI, Properties p, List<IStatus> status, IProgressMonitor monitor)
      throws CoreException {
    IPath path = server.getModuleDeployDirectory(module[0]);
    boolean moving = false;
    // Get URI used for previous publish, if known
    String oldURI = (String) p.get(module[1].getId());
    if (oldURI != null) {
      // If old URI found, detect if jar is moving or changing its name
      if (jarURI != null) {
        moving = !oldURI.equals(jarURI);
      }
    }
    // If we don't have a jar URI, make a guess so we have one if we need it
    if (jarURI == null) {
      jarURI = "WEB-INF/lib/" + module[1].getName() + ".jar";
    }
    IPath jarPath = path.append(jarURI);
    // Make our best determination of the path to the old jar
    IPath oldJarPath = jarPath;
    if (oldURI != null) {
      oldJarPath = path.append(oldURI);
    }
    // Establish the destination directory
    path = jarPath.removeLastSegments(1);

    // Remove if requested or if previously published and are now serving without publishing
    if (moving || (kind == IServer.PUBLISH_CLEAN) || (deltaKind == ServerBehaviourDelegate.REMOVED)
        || server.getWrapper().isServeModulesWithoutPublish()) {
      File file = oldJarPath.toFile();
      if (file.exists()) {
        file.delete();
      }
      p.remove(module[1].getId());

      if ((deltaKind == ServerBehaviourDelegate.REMOVED) || server.getWrapper().isServeModulesWithoutPublish()) {
        return;
      }
    }
    if (!moving && (kind != IServer.PUBLISH_CLEAN) && (kind != IServer.PUBLISH_FULL)) {
      // avoid changes if no changes to module since last publish
      IModuleResourceDelta[] delta = server.getPublishedResourceDelta(module);
      if ((delta == null) || (delta.length == 0)) {
        return;
      }
    }

    // make directory if it doesn't exist
    if (!path.toFile().exists()) {
      path.toFile().mkdirs();
    }

    IModuleResource[] mr = server.getResources(module);
    IStatus[] stat = helper.publishZip(mr, jarPath, monitor);
    PublishOperation2.addArrayToList(status, stat);
    p.put(module[1].getId(), jarURI);
  }

  private void publishArchiveModule(String jarURI, Properties p, List<IStatus> status, IProgressMonitor monitor) {
    IPath path = server.getModuleDeployDirectory(module[0]);
    boolean moving = false;
    // Get URI used for previous publish, if known
    String oldURI = (String) p.get(module[1].getId());
    if (oldURI != null) {
      // If old URI found, detect if jar is moving or changing its name
      if (jarURI != null) {
        moving = !oldURI.equals(jarURI);
      }
    }
    // If we don't have a jar URI, make a guess so we have one if we need it
    if (jarURI == null) {
      jarURI = "WEB-INF/lib/" + module[1].getName();
    }
    IPath jarPath = path.append(jarURI);
    // Make our best determination of the path to the old jar
    IPath oldJarPath = jarPath;
    if (oldURI != null) {
      oldJarPath = path.append(oldURI);
    }
    // Establish the destination directory
    path = jarPath.removeLastSegments(1);

    // Remove if requested or if previously published and are now serving without publishing
    if (moving || (kind == IServer.PUBLISH_CLEAN) || (deltaKind == ServerBehaviourDelegate.REMOVED)
        || server.getWrapper().isServeModulesWithoutPublish()) {
      File file = oldJarPath.toFile();
      if (file.exists()) {
        file.delete();
      }
      p.remove(module[1].getId());

      if ((deltaKind == ServerBehaviourDelegate.REMOVED) || server.getWrapper().isServeModulesWithoutPublish()) {
        return;
      }
    }
    if (!moving && (kind != IServer.PUBLISH_CLEAN) && (kind != IServer.PUBLISH_FULL)) {
      // avoid changes if no changes to module since last publish
      IModuleResourceDelta[] delta = server.getPublishedResourceDelta(module);
      if ((delta == null) || (delta.length == 0)) {
        return;
      }
    }

    // make directory if it doesn't exist
    if (!path.toFile().exists()) {
      path.toFile().mkdirs();
    }

    IModuleResource[] mr = server.getResources(module);
    IStatus[] stat = helper.publishToPath(mr, jarPath, monitor);
    PublishOperation2.addArrayToList(status, stat);
    p.put(module[1].getId(), jarURI);
  }

  /**
   * Utility method to throw a CoreException based on the contents of a list of error and warning
   * status.
   * 
   * @param status a List containing error and warning IStatus
   * @throws CoreException
   */
  protected static void throwException(List<IStatus> status) throws CoreException {
    if ((status == null) || (status.size() == 0)) {
      return;
    }

    if (status.size() == 1) {
      IStatus status2 = status.get(0);
      throw new CoreException(status2);
    }
    IStatus[] children = new IStatus[status.size()];
    status.toArray(children);
    String message = Messages.errorPublish;
    MultiStatus status2 = new MultiStatus(ServerPlugin.PLUGIN_ID, 0, children, message, null);
    throw new CoreException(status2);
  }

  protected static void addArrayToList(List<IStatus> list, IStatus[] a) {
    if ((list == null) || (a == null) || (a.length == 0)) {
      return;
    }

    int size = a.length;
    for (int i = 0; i < size; i++) {
      list.add(a[i]);
    }
  }
}
