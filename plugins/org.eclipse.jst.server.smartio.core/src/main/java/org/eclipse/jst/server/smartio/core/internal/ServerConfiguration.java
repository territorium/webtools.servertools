/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

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
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerPort;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Generic server configuration.
 */
public abstract class ServerConfiguration {

  public static final String MODIFY_PORT_PROPERTY       = "modifyPort";
  
  public static final String MAPPING_PROPERTY_ADD       = "addMapping";
  public static final String MAPPING_PROPERTY_MODIFY    = "modifyMapping";
  public static final String MAPPING_PROPERTY_REMOVE    = "removeMapping";

  public static final String WEB_MODULE_PROPERTY_ADD    = "addWebModule";
  public static final String WEB_MODULE_PROPERTY_MODIFY = "modifyWebModule";
  public static final String WEB_MODULE_PROPERTY_REMOVE = "removeWebModule";

  private final IFolder      configPath;

  private transient List<PropertyChangeListener> propertyListeners;

  /**
   * {@link ServerConfiguration} constructor.
   *
   * @param path a path
   */
  ServerConfiguration(IFolder path) {
    super();
    configPath = path;
  }

  private IFolder getFolder() {
    return configPath;
  }

  /**
   * Returns a list of mime mappings.
   *
   * @return mime mappings
   */
  public abstract List<MimeMapping> getMimeMappings();

  /**
   * Returns a list of ServerPorts that this configuration uses.
   *
   * @return the server ports
   */
  public abstract List<ServerPort> getServerPorts();

  /**
   * Return a list of the web modules in this server.
   *
   * @return the web modules
   */
  public abstract List<WebModule> getWebModules();

  /**
   * Change a web module.
   *
   * @param index int
   * @param docBase java.lang.String
   * @param path java.lang.String
   * @param reloadable boolean
   */
  public abstract void modifyWebModule(int index, String docBase, String path, boolean reloadable);

  /**
   * Adds a mime mapping.
   *
   * @param index int
   * @param map MimeMapping
   */
  public abstract void addMimeMapping(int index, MimeMapping map);

  /**
   * Change a mime mapping.
   *
   * @param index int
   * @param map MimeMapping
   */
  public abstract void modifyMimeMapping(int index, MimeMapping map);

  /**
   * Modify the port with the given id.
   *
   * @param id java.lang.String
   * @param port int
   */
  public abstract void modifyServerPort(String id, int port);

  /**
   * Remove a mime mapping.
   *
   * @param index int
   */
  public abstract void removeMimeMapping(int index);

  /**
   * Copies all files from the given directory in the workbench to the given location. Can be
   * overridden by version specific class to modify or enhance what publish does.
   *
   * @param destDir
   * @param doBackup
   * @param monitor
   */
  protected IStatus backupAndPublish(IPath destDir, boolean doBackup, IProgressMonitor monitor) {
    MultiStatus ms = new MultiStatus(ServerPlugin.PLUGIN_ID, 0, Messages.publishConfigurationTask, null);
    if (Trace.isTraceEnabled()) {
      Trace.trace(Trace.FINER, "Backup and publish");
    }
    monitor = ProgressUtil.getMonitorFor(monitor);

    try {
      IPath backup = null;
      if (doBackup) {
        // create backup directory
        backup = destDir.append("backup");
        if (!backup.toFile().exists()) {
          backup.toFile().mkdir();
        }
      }
      backupFolder(getFolder(), destDir.append("conf"), backup, ms, monitor);
    } catch (Exception e) {
      Trace.trace(Trace.SEVERE, "backupAndPublish() error", e);
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
          if (Trace.isTraceEnabled()) {
            Trace.trace(Trace.FINEST, "Publishing " + name);
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
              destContents = TomcatVersionHelper.getFileContents(fis);
              if (destContents != null) {
                fis = file.getContents();
                srcContents = TomcatVersionHelper.getFileContents(fis);
              }
            }
            if ((destContents == null) || (srcContents == null) || !srcContents.equals(destContents)) {
              InputStream in = file.getContents();
              ms.add(FileUtil.copyFile(in, destPath));
            }
          }
        } catch (Exception e) {
          Trace.trace(Trace.SEVERE, "backupAndPublish() error", e);
          ms.add(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
              NLS.bind(Messages.errorPublishConfiguration, new String[] { e.getLocalizedMessage() }), e));
        }
      }
      monitor.worked(100);
    }
  }

  protected IStatus publishContextConfig(IPath baseDir, IPath deployDir, IProgressMonitor monitor) {
    // Default implementation assumes nothing to do
    return Status.OK_STATUS;
  }


  protected IStatus updateContextsToServeDirectly(IPath baseDir, String version, String loader,
      IProgressMonitor monitor) {
    // Default implementation assumes nothing to do
    return Status.OK_STATUS;
  }

  protected IStatus cleanupServer(IPath confDir, IPath installDir, boolean removeKeptContextFiles,
      IProgressMonitor monitor) {
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
  public abstract IStatus localizeConfiguration(IPath baseDir, IPath deployDir, ServerWrapper server,
      IProgressMonitor monitor);

  /**
   * Returns the main server port.
   *
   * @return ServerPort
   */
  public abstract ServerPort getMainPort();

  /**
   * Returns the prefix that is used in front of the web module path property. (e.g. "webapps")
   *
   * @return java.lang.String
   */
  public String getDocBasePrefix() {
    return "";
  }

  /**
   * Returns the partial URL applicable to this module.
   *
   * @param webModule a web module
   * @return the partial URL
   */
  protected String getWebModuleURL(IModule webModule) {
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

  protected abstract void save(IFolder folder, IProgressMonitor monitor) throws CoreException;

  protected void firePropertyChangeEvent(String propertyName, Object oldValue, Object newValue) {
    if (propertyListeners == null) {
      return;
    }

    PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
    try {
      Iterator<PropertyChangeListener> iterator = propertyListeners.iterator();
      while (iterator.hasNext()) {
        try {
          PropertyChangeListener listener = iterator.next();
          listener.propertyChange(event);
        } catch (Exception e) {
          Trace.trace(Trace.SEVERE, "Error firing property change event", e);
        }
      }
    } catch (Exception e) {
      Trace.trace(Trace.SEVERE, "Error in property event", e);
    }
  }

  /**
   * Adds a property change listener to this server.
   *
   * @param listener java.beans.PropertyChangeListener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    if (propertyListeners == null) {
      propertyListeners = new ArrayList<>();
    }
    propertyListeners.add(listener);
  }

  /**
   * Removes a property change listener from this server.
   *
   * @param listener java.beans.PropertyChangeListener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    if (propertyListeners != null) {
      propertyListeners.remove(listener);
    }
  }

  public void importFromPath(IPath path, boolean isTestEnv, IProgressMonitor monitor) throws CoreException {
    load(path, monitor);
  }

  protected abstract void load(IPath path, IProgressMonitor monitor) throws CoreException;

  protected abstract void load(IFolder folder, IProgressMonitor monitor) throws CoreException;

  public abstract void addWebModule(int index, IServerWebModule module);

  public abstract void removeWebModule(int index);

  /**
   * Gets the work directory for the server.
   *
   * @param basePath path to server runtime directory
   * @return path for the server's work directory
   */
  public abstract IPath getServerWorkDirectory(IPath basePath);

  /**
   * Gets the work directory for the specified module on the server.
   *
   * @param basePath
   * @param module
   */
  public abstract IPath getContextWorkDirectory(IPath basePath, IServerWebModule module);

  /**
   * Return a string representation of this object.
   *
   * @return java.lang.String
   */
  @Override
  public String toString() {
    return "ServerConfiguration[" + getFolder() + "]";
  }
}
