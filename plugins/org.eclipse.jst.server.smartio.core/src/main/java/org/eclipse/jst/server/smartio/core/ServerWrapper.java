/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.smartio.core.ServerPlugin.Level;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ServerDelegate;

import java.net.URL;
import java.util.List;

/**
 * Generic {@link ServerWrapper}.
 */
public class ServerWrapper extends ServerDelegate implements IServerWrapper {

  private transient IServerInstallation  handler;
  private transient IServerConfiguration configuration;

  // Configuration version control
  private int          versionLoaded;
  private int          versionCurrent;
  private final Object versionLock = new Object();

  /**
   * Gets the {@link ServerRuntime}.
   */
  protected final ServerRuntime getServerRuntime() {
    IRuntime runtime = getServer().getRuntime();
    return (runtime == null) ? null : (ServerRuntime) runtime.loadAdapter(ServerRuntime.class, null);
  }

  /**
   * Gets the server version handler for this server.
   */
  @Override
  public final IServerInstallation getHandler() {
    if (handler == null) {
      ServerRuntime runtime = getServerRuntime();
      handler = (runtime == null) ? null : runtime.getHandler();
    }
    return handler;
  }

  /**
   * Set the default of the {@link ServerWrapper}.
   *
   * @param monitor
   */
  @Override
  public void setDefaults(IProgressMonitor monitor) {
    setAttribute("auto-publish-setting", 2);
    setAttribute("auto-publish-time", 1);
  }

  /**
   * Import the runtime configuration from the {@link IRuntime}.
   *
   * @param runtime
   * @param monitor
   */
  @Override
  public void importRuntimeConfiguration(IRuntime runtime, IProgressMonitor monitor) throws CoreException {
    // Initialize state
    synchronized (versionLock) {
      configuration = null;
      versionCurrent = 0;
      versionLoaded = 0;
    }
    if (runtime == null) {
      return;
    }

    IServerConfiguration config = createConfig();
    IPath path = ServerTools.getAbsolutePath(getRuntimeBaseDirectory(), getConfDirectory());
    try {
      config.importConfiguration(path, monitor);
    } catch (CoreException ce) {
      throw ce;
    }

    // Update version
    synchronized (versionLock) {
      // If not already initialized by some other thread, save the configuration
      if (configuration == null) {
        configuration = config;
      }
    }
  }

  /**
   * Saves the current {@link IServerConfiguration}.
   *
   * @param monitor
   */
  @Override
  public void saveConfiguration(IProgressMonitor monitor) throws CoreException {
    if (configuration != null) {
      IPath path = ServerTools.getAbsolutePath(getRuntimeBaseDirectory(), getConfDirectory());
      configuration.saveConfiguration(path, getServer().getServerConfiguration(), monitor);
    }
  }

  /**
   * Notifies about a change on the {@link IServerConfiguration}.
   */
  @Override
  public void configurationChanged() {
    synchronized (versionLock) {
      // Alter the current version
      versionCurrent++;
    }
  }

  /**
   * Get the array of {@link ServerPort}'s
   */
  @Override
  public final ServerPort[] getServerPorts() {
    if (getServer().getServerConfiguration() != null) {
      try {
        List<ServerPort> list = loadConfiguration().getServerPorts();
        return list.toArray(new ServerPort[list.size()]);
      } catch (Exception e) {}
    }
    return new ServerPort[0];
  }

  /**
   * Return the root URL of this module.
   *
   * @param module
   */
  @Override
  public URL getModuleRootURL(IModule module) {
    try {
      if (module == null) {
        return null;
      }

      IServerConfiguration config = loadConfiguration();
      if (config == null) {
        return null;
      }

      String url = "http://" + getServer().getHost();
      int port = config.getMainPort().getPort();
      port = ServerUtil.getMonitoredPort(getServer(), port, "web");
      if (port != 80) {
        url += ":" + port;
      }

      url += config.getWebModuleURL(module);
      if (!url.endsWith("/")) {
        url += "/";
      }
      return new URL(url);
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Could not get root URL", e);
      return null;
    }
  }

  /**
   * Returns the root module(s) of this module.
   *
   * @param module
   */
  @Override
  public IModule[] getRootModules(IModule module) throws CoreException {
    if (IConstants.JST_WEB_MODULE.equals(module.getModuleType().getId())) {
      IStatus status = canModifyModules(new IModule[] { module }, null);
      if ((status == null) || !status.isOK()) {
        throw new CoreException(status);
      }
      return new IModule[] { module };
    }
    return ServerTools.getWebModules(module);
  }

  /**
   * Returns the child module(s) of this module.
   *
   * @param module
   */
  @Override
  public final IModule[] getChildModules(IModule[] module) {
    if (module == null) {
      return null;
    }

    IModuleType moduleType = module[0].getModuleType();
    if ((module.length == 1) && (moduleType != null) && IConstants.JST_WEB_MODULE.equals(moduleType.getId())) {
      IWebModule webModule = (IWebModule) module[0].loadAdapter(IWebModule.class, null);
      if (webModule != null) {
        return webModule.getModules();
      }
    }
    return new IModule[0];
  }

  /**
   * Returns true if the given project is supported by this server, and false otherwise.
   *
   * @param add
   * @param remove
   */
  @Override
  public final IStatus canModifyModules(IModule[] add, IModule[] remove) {
    if (add != null) {
      int size = add.length;
      for (int i = 0; i < size; i++) {
        IModule module = add[i];
        if (!IConstants.JST_WEB_MODULE.equals(module.getModuleType().getId())) {
          return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorWebModulesOnly, null);
        }

        if (getHandler() == null) {
          return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorNoRuntime, null);
        }

        IStatus status = getHandler().canAddModule(module);
        if ((status != null) && !status.isOK()) {
          return status;
        }

        if (module.getProject() != null) {
          status = FacetUtil.verifyFacets(module.getProject(), getServer());
          if ((status != null) && !status.isOK()) {
            return status;
          }
        }
      }
    }

    return Status.OK_STATUS;
  }

  /**
   * @see ServerDelegate#modifyModules(IModule[], IModule[], IProgressMonitor)
   */
  @Override
  public final void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
    IStatus status = canModifyModules(add, remove);
    if ((status == null) || !status.isOK()) {
      throw new CoreException(status);
    }

    IServerConfiguration config = loadConfiguration();
    if (add != null) {
      int size = add.length;
      for (int i = 0; i < size; i++) {
        IModule module3 = add[i];
        IWebModule module = (IWebModule) module3.loadAdapter(IWebModule.class, monitor);
        String contextRoot = module.getContextRoot();
        if ((contextRoot != null) && !contextRoot.startsWith("/") && (contextRoot.length() > 0)) {
          contextRoot = "/" + contextRoot;
        }
        String docBase = config.getDocBasePrefix() + module3.getName();
        WebModule module2 = new WebModule(contextRoot, docBase, module3.getId(), isModulesReloadable());
        config.addWebModule(-1, module2);
      }
    }

    if (remove != null) {
      int size2 = remove.length;
      for (int j = 0; j < size2; j++) {
        IModule module3 = remove[j];
        String memento = module3.getId();
        List<WebModule> modules = loadConfiguration().getWebModules();
        int size = modules.size();
        for (int i = 0; i < size; i++) {
          WebModule module = modules.get(i);
          if (memento.equals(module.getMemento())) {
            config.removeWebModule(i);
          }
        }
      }
    }
  }

  /**
   * Get the {@link IServerConfiguration} for the current version.
   */
  @Override
  public final IServerConfiguration loadConfiguration() throws CoreException {
    int current;
    IServerConfiguration configTemp;
    // Grab current state
    synchronized (versionLock) {
      current = versionCurrent;
      configTemp = configuration;
    }

    // If configuration needs loading
    if ((configTemp == null) || (versionLoaded != current)) {
      IFolder folder = getServer().getServerConfiguration();
      if ((folder == null) || !folder.exists()) {
        String path = null;
        if (folder != null) {
          path = folder.getFullPath().toOSString();
          IProject project = folder.getProject();
          if ((project != null) && project.exists() && !project.isOpen()) {
            throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
                NLS.bind(Messages.errorConfigurationProjectClosed, path, project.getName()), null));
          }
        }
        throw new CoreException(
            new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorNoConfiguration, path), null));
      }

      // If not yet loaded
      if (configTemp == null) {
        configTemp = createConfig();
      }

      try {
        IPath path = ServerTools.getAbsolutePath(getRuntimeBaseDirectory(), getConfDirectory());
        configTemp.loadConfiguration(path, folder, new NullProgressMonitor());

        // Update loaded version
        synchronized (versionLock) {
          // If newer version not already loaded, update version
          if ((configuration == null) || (versionLoaded < current)) {
            configuration = configTemp;
            versionLoaded = current;
          }
        }
      } catch (CoreException ce) {
        throw ce;
      }
    }
    return configTemp;
  }

  /**
   * Gets the base directory where the server instance runs. This path can vary depending on the
   * configuration. Null may be returned if a runtime hasn't been specified for the server.
   */
  @Override
  public final IPath getRuntimeBaseDirectory() {
    IServerInstallation handler = getHandler();
    return (handler == null) ? null : handler.getRuntimeBaseDirectory(this);
  }

  /**
   * Returns true if contexts should be made reloadable by default.
   */
  @Override
  public final boolean isModulesReloadable() {
    // If feature is supported, return current setting
    IServerInstallation handler = getHandler();
    return (handler == null) ? true : getAttribute(IServerWrapper.PROPERTY_MODULES_RELOADABLE, true);
  }

  /**
   * Get the location of the web-app deploy directory.
   */
  @Override
  public final String getConfDirectory() {
    return getAttribute(IServerWrapper.PROPERTY_CONF_DIR, "conf");
  }

  /**
   * Sets this process to secure mode.
   *
   * @param bool
   */
  @Override
  public final void setConfDirectory(String directory) {
    setAttribute(IServerWrapper.PROPERTY_CONF_DIR, directory);
  }

  /**
   * Get the location of the web-app deploy directory.
   */
  @Override
  public final String getDeployDirectory() {
    return "webapps";// getAttribute(IServerWrapper.PROPERTY_DEPLOY_DIR, "webapps");
  }

  // /**
  // * Sets this process to secure mode.
  // *
  // * @param bool
  // */
  // @Override
  // public final void setDeployDirectory(String directory) {
  // setAttribute(IServerWrapper.PROPERTY_DEPLOY_DIR, directory);
  // }

  /**
   * Return a string representation of this object.
   */
  @Override
  public final String toString() {
    return "ServerWrapper";
  }

  protected IServerConfiguration createConfig() throws CoreException {
    String id = getServer().getServerType().getId();
    if (id.indexOf("10") > 0) {
      return new Server10Configuration();
    }
    throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorUnknownVersion, null));
  }
}
