/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerPort;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Generic server configuration.
 */
public interface IServerConfiguration {

  public static final String MODIFY_PORT_PROPERTY       = "modifyPort";

  public static final String WEB_MODULE_PROPERTY_ADD    = "addWebModule";
  public static final String WEB_MODULE_PROPERTY_MODIFY = "modifyWebModule";
  public static final String WEB_MODULE_PROPERTY_REMOVE = "removeWebModule";

  /**
   * Returns a list of ServerPorts that this configuration uses.
   *
   */
  public List<ServerPort> getServerPorts();

  /**
   * Return a list of the web modules in this server.
   *
   * @return the web modules
   */
  public List<WebModule> getWebModules();

  /**
   * Change a web module.
   *
   * @param index int
   * @param docBase java.lang.String
   * @param path java.lang.String
   * @param reloadable boolean
   */
  public void modifyWebModule(int index, String docBase, String path, boolean reloadable);

  /**
   * Modify the port with the given id.
   *
   * @param id java.lang.String
   * @param port int
   */
  public void modifyServerPort(String id, int port);

  /**
   * Copies all files from the given directory in the workbench to the given
   * location. Can be overridden by version specific class to modify or enhance
   * what publish does.
   *
   * @param destDir
   * @param monitor
   */
  public IStatus backupAndPublish(IPath destDir, IProgressMonitor monitor);

  public IStatus cleanupServer(IPath confDir, IPath installDir, IProgressMonitor monitor);

  /**
   * Make any local changes to the server configuration at the specified runtime
   * base directory needed to complete publishing the server.
   *
   * @param baseDir runtime base directory for the server
   * @param deployDir deployment directory for the server
   * @param server server being localized
   * @param monitor a progress monitor
   * @return result of operation
   */
  public IStatus localizeConfiguration(IPath baseDir, IPath deployDir, ServerWrapper server, IProgressMonitor monitor);

  /**
   * Returns the main server port.
   *
   * @return ServerPort
   */
  public ServerPort getMainPort();

  /**
   * Returns the prefix that is used in front of the web module path property.
   * (e.g. "webapps")
   *
   * @return java.lang.String
   */
  public String getDocBasePrefix();

  /**
   * Returns the partial URL applicable to this module.
   *
   * @param webModule a web module
   * @return the partial URL
   */
  public String getWebModuleURL(IModule webModule);

  /**
   * Returns the given module from the config.
   *
   * @param module a web module
   * @return a web module
   */
  public WebModule getWebModule(IModule module);

  public abstract void save(IFolder folder, IProgressMonitor monitor) throws CoreException;


  public void load(IPath path, IProgressMonitor monitor) throws CoreException;

  public void load(IFolder folder, IProgressMonitor monitor) throws CoreException;

  public void addWebModule(int index, IServerWebModule module);

  public void removeWebModule(int index);

  /**
   * Gets the work directory for the server.
   *
   * @param basePath path to server runtime directory
   * @return path for the server's work directory
   */
  public IPath getServerWorkDirectory(IPath basePath);

  /**
   * Gets the work directory for the specified module on the server.
   *
   * @param basePath
   * @param module
   */
  public IPath getContextWorkDirectory(IPath basePath, IServerWebModule module);

  /**
   * Adds a property change listener to this server.
   *
   * @param listener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener);

  /**
   * Removes a property change listener from this server.
   *
   * @param listener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener);
}
