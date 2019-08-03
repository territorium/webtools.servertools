/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.smartio.core.ServerPlugin.Level;
import org.eclipse.jst.server.smartio.core.util.FileUtil;
import org.eclipse.jst.server.smartio.core.util.ProgressUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Generic server configuration.
 */
public abstract class ServerConfiguration implements IServerConfiguration {

  private transient PropertyChangeSupport listener = new PropertyChangeSupport(this);

  /**
   * Adds a property change listener to this server.
   *
   * @param listener
   */
  @Override
  public final void addPropertyChangeListener(PropertyChangeListener listener) {
    this.listener.addPropertyChangeListener(listener);
  }

  /**
   * Removes a property change listener from this server.
   *
   * @param listener
   */
  @Override
  public final void removePropertyChangeListener(PropertyChangeListener listener) {
    this.listener.removePropertyChangeListener(listener);
  }

  /**
   * Fire a {@link PropertyChangeEvent}.
   *
   * @param propertyName
   * @param oldValue
   * @param newValue
   */
  protected final void firePropertyChangeEvent(String propertyName, Object oldValue, Object newValue) {
    PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
    for (PropertyChangeListener listener : this.listener.getPropertyChangeListeners()) {
      try {
        listener.propertyChange(event);
      } catch (Exception e) {
        ServerPlugin.log(Level.SEVERE, "Error firing property change event", e);
      }
    }
  }

  /**
   * Copies all files from the given directory in the workbench to the given location. Can be
   * overridden by version specific class to modify or enhance what publish does.
   *
   * @param folder
   * @param confPath
   * @param monitor
   */
  @Override
  public IStatus backupAndPublish(IFolder folder, IPath confPath, IProgressMonitor monitor) {
    MultiStatus ms = new MultiStatus(ServerPlugin.PLUGIN_ID, 0, Messages.publishConfigurationTask, null);
    if (ServerPlugin.isTraceEnabled()) {
      ServerPlugin.log(Level.FINER, "Backup and publish");
    }
    monitor = ProgressUtil.getMonitorFor(monitor);

    try {
      backupFolder(folder, confPath, null, ms, monitor);
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "backupAndPublish() error", e);
      IStatus s = new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorPublishConfiguration, new String[] { e.getLocalizedMessage() }), e);
      ms.add(s);
    }

    monitor.done();
    return ms;
  }

  private void backupFolder(IFolder folder, IPath confDir, IPath backup, MultiStatus ms, IProgressMonitor monitor)
      throws CoreException {
    IResource[] children = folder.members();
    if (children == null) {
      return;
    }

    int size = children.length;
    monitor.beginTask(Messages.publishConfigurationTask, size * 100);
    for (int i = 0; i < size; i++) {
      if (children[i] instanceof IFile) {
        try {
          IFile file = (IFile) children[i];
          String name = file.getName();
          monitor.subTask(NLS.bind(Messages.publisherPublishTask, new String[] { name }));
          if (ServerPlugin.isTraceEnabled()) {
            ServerPlugin.log(Level.FINEST, "Publishing " + name);
          }

          // backup and copy file
          boolean copy = true;
          if ((backup != null) && !(backup.append(name).toFile().exists())) {
            IStatus status = FileUtil.copyFile(confDir.append(name).toOSString(), backup + File.separator + name);
            ms.add(status);
            if (!status.isOK()) {
              copy = false;
            }
          }

          if (copy) {
            String destPath = confDir.append(name).toOSString();
            String destContents = null;
            String srcContents = null;
            File dest = new File(destPath);
            if (dest.exists()) {
              InputStream fis = new FileInputStream(destPath);
              destContents = FileUtil.getFileContents(fis);
              if (destContents != null) {
                fis = file.getContents();
                srcContents = FileUtil.getFileContents(fis);
              }
            }
            if ((destContents == null) || (srcContents == null) || !srcContents.equals(destContents)) {
              InputStream in = file.getContents();
              ms.add(FileUtil.copyFile(in, destPath));
            }
          }
        } catch (Exception e) {
          ServerPlugin.log(Level.SEVERE, "backupAndPublish() error", e);
          ms.add(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
              NLS.bind(Messages.errorPublishConfiguration, new String[] { e.getLocalizedMessage() }), e));
        }
      }
      monitor.worked(100);
    }
  }

  @Override
  public IStatus cleanupServer(IPath confDir, IPath installDir, IProgressMonitor monitor) {
    // Default implementation assumes nothing to clean
    return Status.OK_STATUS;
  }

  /**
   * Make any local changes to the server configuration at the specified runtime base directory
   * needed to complete publishing the server.
   *
   * @param baseDir runtime base directory for the server
   * @param deployDir deployment directory for the server
   * @param server server being localized
   * @param monitor a progress monitor
   * @return result of operation
   */
  @Override
  public abstract IStatus localizeConfiguration(IPath baseDir, IPath deployDir, ServerWrapper server,
      IProgressMonitor monitor);

  /**
   * Returns the prefix that is used in front of the web module path property. (e.g. "webapps")
   *
   * @return java.lang.String
   */
  @Override
  public String getDocBasePrefix() {
    return "";
  }

  /**
   * Returns the partial URL applicable to this module.
   *
   * @param webModule a web module
   * @return the partial URL
   */
  @Override
  public String getWebModuleURL(IModule webModule) {
    WebModule module = getWebModule(webModule);
    if (module != null) {
      return module.getPath();
    }

    IWebModule webModule2 = (IWebModule) webModule.loadAdapter(IWebModule.class, null);
    return "/" + webModule2.getContextRoot();
  }

  /**
   * Returns the given module from the config.
   *
   * @param module a web module
   * @return a web module
   */
  @Override
  public WebModule getWebModule(IModule module) {
    if (module == null) {
      return null;
    }

    String memento = module.getId();

    List<WebModule> modules = getWebModules();
    int size = modules.size();
    for (int i = 0; i < size; i++) {
      WebModule webModule = modules.get(i);
      if (memento.equals(webModule.getMemento())) {
        return webModule;
      }
    }
    return null;
  }


  /**
   * Return a string representation of this object.
   */
  @Override
  public String toString() {
    return "ServerConfiguration";
  }
}
