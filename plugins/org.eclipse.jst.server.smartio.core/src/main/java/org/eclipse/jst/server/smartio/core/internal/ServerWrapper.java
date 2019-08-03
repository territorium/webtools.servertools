/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others. All rights reserved.
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.smartio.core.internal.ServerPlugin.Level;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ServerDelegate;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic {@link ServerWrapper}.
 */
public class ServerWrapper extends ServerDelegate implements IServerWrapper {

  private static final String WEB_MODULE      = "jst.web";
  public static final String  PROPERTY_SECURE = "secure";


  private transient IServerConfiguration  configuration;
  private transient IServerVersionHandler versionHandler;

  // Configuration version control
  private int          currentVersion;
  private int          loadedVersion;
  private final Object versionLock = new Object();

  /**
   * Gets the server version handler for this server.
   */
  public final IServerVersionHandler getVersionHandler() {
    if (versionHandler == null) {
      IRuntime runtime = getServer().getRuntime();
      ServerRuntime r = (runtime == null) ? null : (ServerRuntime) runtime.loadAdapter(ServerRuntime.class, null);
      versionHandler = (r == null) ? null : r.getVersionHandler();
    }
    return versionHandler;
  }

  public IServerConfiguration getConfiguration() throws CoreException {
    int current;
    IServerConfiguration tcConfig;
    // Grab current state
    synchronized (versionLock) {
      current = currentVersion;
      tcConfig = configuration;
    }
    // If configuration needs loading
    if ((tcConfig == null) || (loadedVersion != current)) {
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
      if (tcConfig == null) {

        String id = getServer().getServerType().getId();
        if (id.indexOf("10") > 0) {
          tcConfig = new Server10Configuration(folder);
        } else {
          throw new CoreException(
              new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorUnknownVersion, null));
        }
      }
      try {
        tcConfig.load(folder, null);
        // Update loaded version
        synchronized (versionLock) {
          // If newer version not already loaded, update version
          if ((configuration == null) || (loadedVersion < current)) {
            configuration = tcConfig;
            loadedVersion = current;
          }
        }
      } catch (CoreException ce) {
        // Ignore
        throw ce;
      }
    }
    return tcConfig;
  }

  @Override
  public void importRuntimeConfiguration(IRuntime runtime, IProgressMonitor monitor) throws CoreException {
    // Initialize state
    synchronized (versionLock) {
      configuration = null;
      currentVersion = 0;
      loadedVersion = 0;
    }
    if (runtime == null) {
      return;
    }
    IPath path = runtime.getLocation().append("conf");

    String id = getServer().getServerType().getId();
    IFolder folder = getServer().getServerConfiguration();
    IServerConfiguration config;
    if (id.indexOf("10") > 0) {
      config = new Server10Configuration(folder);
    } else {
      throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorUnknownVersion, null));
    }

    try {
      config.load(path, monitor);
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

  @Override
  public void saveConfiguration(IProgressMonitor monitor) throws CoreException {
    if (configuration != null) {
      configuration.save(getServer().getServerConfiguration(), monitor);
    }
  }

  @Override
  public void configurationChanged() {
    synchronized (versionLock) {
      // Alter the current version
      currentVersion++;
    }
  }

  /**
   * Return the root URL of this module.
   * 
   * @param module org.eclipse.wst.server.core.model.IModule
   * @return java.net.URL
   */
  @Override
  public URL getModuleRootURL(IModule module) {
    try {
      if (module == null) {
        return null;
      }

      IServerConfiguration config = getConfiguration();
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
   * Returns true if the process is set to run in secure mode.
   *
   * @return boolean
   */
  public boolean isSecure() {
    return getAttribute(ServerWrapper.PROPERTY_SECURE, false);
  }

  /**
   * @see IServerWrapper#getInstanceDirectory()
   */
  @Override
  public String getInstanceDirectory() {
    return getAttribute(IServerWrapper.PROPERTY_INSTANCE_DIR, (String) null);
  }

  /**
   * @see IServerWrapper#getDeployDirectory()
   */
  @Override
  public String getDeployDirectory() {
    return IServerWrapper.LEGACY_DEPLOYDIR;
  }

  /**
   * Returns true if contexts should be made reloadable by default.
   * 
   * @return boolean
   */
  public boolean isModulesReloadableByDefault() {
    // If feature is supported, return current setting
    IServerVersionHandler tvh = getVersionHandler();
    if (tvh != null) {
      return getAttribute(IServerWrapper.PROPERTY_MODULES_RELOADABLE_BY_DEFAULT, true);
    }
    return true;
  }


  /**
   * Gets the base directory where the server instance runs. This path can vary
   * depending on the configuration. Null may be returned if a runtime hasn't
   * been specified for the server.
   * 
   * @return path to base directory for the server or null if runtime hasn't
   *         been specified.
   */
  public IPath getRuntimeBaseDirectory() {
    IServerVersionHandler tvh = getVersionHandler();
    return (tvh == null) ? null : tvh.getRuntimeBaseDirectory(this);
  }

  /**
   * Gets the directory to which modules should be deployed for this server.
   * 
   * @return full path to deployment directory for the server
   */
  public IPath getServerDeployDirectory() {
    String deployDir = getDeployDirectory();
    IPath deployPath = new Path(deployDir);
    if (!deployPath.isAbsolute()) {
      IPath base = getRuntimeBaseDirectory();
      deployPath = base.append(deployPath);
    }
    return deployPath;
  }

  /**
   * Returns the child module(s) of this module.
   * 
   * @param module module from which to get child module(s)
   * @return array of child module(s)
   */
  @Override
  public IModule[] getChildModules(IModule[] module) {
    if (module == null) {
      return null;
    }

    IModuleType moduleType = module[0].getModuleType();

    if ((module.length == 1) && (moduleType != null) && "jst.web".equals(moduleType.getId())) {
      IWebModule webModule = (IWebModule) module[0].loadAdapter(IWebModule.class, null);
      if (webModule != null) {
        IModule[] modules = webModule.getModules();
        // if (modules != null)
        // System.out.println(modules.length);
        return modules;
      }
    }
    return new IModule[0];
  }

  /**
   * Returns the root module(s) of this module.
   * 
   * @param module module from which to get the root module
   * @return root module
   * @throws CoreException
   */
  @Override
  public IModule[] getRootModules(IModule module) throws CoreException {
    if ("jst.web".equals(module.getModuleType().getId())) {
      IStatus status = canModifyModules(new IModule[] { module }, null);
      if ((status == null) || !status.isOK()) {
        throw new CoreException(status);
      }
      return new IModule[] { module };
    }

    return getWebModules(module);
  }

  /**
   * Returns true if the given project is supported by this server, and false
   * otherwise.
   *
   * @param add modules
   * @param remove modules
   * @return the status
   */
  @Override
  public IStatus canModifyModules(IModule[] add, IModule[] remove) {
    if (add != null) {
      int size = add.length;
      for (int i = 0; i < size; i++) {
        IModule module = add[i];
        if (!"jst.web".equals(module.getModuleType().getId())) {
          return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorWebModulesOnly, null);
        }

        if (getVersionHandler() == null) {
          return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorNoRuntime, null);
        }

        IStatus status = getVersionHandler().canAddModule(module);
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

  @Override
  public ServerPort[] getServerPorts() {
    if (getServer().getServerConfiguration() == null) {
      return new ServerPort[0];
    }

    try {
      List<ServerPort> list = getConfiguration().getServerPorts();
      ServerPort[] sp = new ServerPort[list.size()];
      list.toArray(sp);
      return sp;
    } catch (Exception e) {
      return new ServerPort[0];
    }
  }

  @Override
  public void setDefaults(IProgressMonitor monitor) {
    setAttribute("auto-publish-setting", 2);
    setAttribute("auto-publish-time", 1);
  }

  /**
   * Sets this process to secure mode.
   * 
   * @param b boolean
   */
  public void setSecure(boolean b) {
    setAttribute(ServerWrapper.PROPERTY_SECURE, b);
  }

  /**
   * @see ServerDelegate#modifyModules(IModule[], IModule[], IProgressMonitor)
   */
  @Override
  public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
    IStatus status = canModifyModules(add, remove);
    if ((status == null) || !status.isOK()) {
      throw new CoreException(status);
    }

    IServerConfiguration config = getConfiguration();

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
        WebModule module2 = new WebModule(contextRoot, docBase, module3.getId(), isModulesReloadableByDefault());
        config.addWebModule(-1, module2);
      }
    }

    if (remove != null) {
      int size2 = remove.length;
      for (int j = 0; j < size2; j++) {
        IModule module3 = remove[j];
        String memento = module3.getId();
        List<WebModule> modules = getConfiguration().getWebModules();
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
   * Returns the web modules that the utility module is contained within.
   * 
   * @param module a utility module
   * @param monitor a progress monitor, or <code>null</code> if progress
   *        reporting and cancellation are not desired
   * @return a possibly empty array of web modules
   */
  public static IModule[] getWebModules(IModule module) {
    List<IModule> list = new ArrayList<IModule>();
    IModule[] modules = ServerUtil.getModules(WEB_MODULE);
    if (modules != null) {
      for (IModule module2 : modules) {
        IWebModule web = (IWebModule) module2.loadAdapter(IWebModule.class, null);
        if (web != null) {
          IModule[] modules2 = web.getModules();
          if (modules2 != null) {
            for (IModule m : modules2) {
              if (module.equals(m))
                list.add(module2);
            }
          }
        }
      }
    }
    return list.toArray(new IModule[list.size()]);
  }

  /**
   * Return a string representation of this object.
   * 
   * @return java.lang.String
   */
  @Override
  public final String toString() {
    return "ServerWrapper";
  }
}
