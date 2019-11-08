/**********************************************************************
 * Copyright (c) 2016 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 **********************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.smartio.core.ServerPlugin.Level;
import org.eclipse.jst.server.smartio.core.conf.Configuration;
import org.eclipse.jst.server.smartio.core.conf.Properties.Format;
import org.eclipse.jst.server.smartio.core.util.ProgressUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.ServerPort;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * smart.IO server configuration.
 */
class Server10Configuration extends ServerConfiguration {

  public static final String SERVER  = "server.properties";
  public static final String LOGGING = "logging.properties";


  private Configuration configuration;

  /**
   * Return the port number.
   *
   * @return int
   */
  @Override
  public ServerPort getMainPort() {
    for (ServerPort port : getServerPorts()) {
      // Return only an HTTP port from the selected Service
      if (port.getProtocol().toLowerCase().equals("http") && (port.getId().indexOf('/') < 0)) {
        return port;
      }
    }
    return null;
  }

  /**
   * Returns a list of ServerPorts that this configuration uses.
   *
   * @return java.util.List
   */
  @Override
  public List<ServerPort> getServerPorts() {
    List<ServerPort> ports = new ArrayList<>();

    // first add admin port
    try {
      int port = configuration.get("server.web.admin", 8888);
      ports.add(new ServerPort("admin", Messages.portServer, port, "TCPIP"));
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Error getting server ports", e);
    }

    try {
      int port = configuration.get("server.web.http", 8888);
      ports.add(new ServerPort("http", "HTTP/1.1", port, "HTTP", new String[] { "web", "webservices" }, false));
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Error getting server ports", e);
    }

    try {
      int port = configuration.get("server.web.ajp", 8888);
      ports.add(new ServerPort("ajp", "AJP/1.3", port, "AJP", null, false));
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Error getting server ports", e);
    }
    return ports;
  }

  /**
   * Modify the port with the given id.
   *
   * @param id java.lang.String
   * @param port int
   */
  @Override
  public void setServerPort(String id, int port) {
    try {
      switch (id) {
        case "admin":
          configuration.set("server.web.admin", "" + port);
          // isServerDirty = true;
          firePropertyChangeEvent(IServerConfiguration.SET_PORT_PROPERTY, id, new Integer(port));
          return;

        case "http":
          configuration.set("server.web.http", "" + port);
          // isServerDirty = true;
          firePropertyChangeEvent(IServerConfiguration.SET_PORT_PROPERTY, id, new Integer(port));
          return;

        case "ajp":
          configuration.set("server.web.ajp", "" + port);
          firePropertyChangeEvent(IServerConfiguration.SET_PORT_PROPERTY, id, new Integer(port));
          return;

        default:
          break;
      }
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Error modifying server port " + id, e);
    }
  }

  /**
   * @see IServerConfiguration#getServerWorkDirectory(IPath)
   */
  @Override
  public IPath getServerWorkDirectory(IPath basePath) {
    return basePath.append("temp").append("work").append("Tomcat").append("localhost");
  }

  /**
   * @see IServerConfiguration#getContextWorkDirectory(IPath, IServerWebModule)
   */
  @Override
  public IPath getContextWorkDirectory(IPath basePath, WebModule module) {
    return getServerWorkDirectory(basePath).append(module.getPath());
  }

  /**
   * Import the runtime configuration.
   *
   * @param path
   * @param monitor
   */
  @Override
  public void importConfiguration(IPath path, IProgressMonitor monitor) throws CoreException {
    try {
      monitor = ProgressUtil.getMonitorFor(monitor);
      monitor.beginTask(Messages.loadingTask, 7);

      // Load server properties
      configuration = new Configuration();
      configuration.load(new FileInputStream(path.append(Server10Configuration.SERVER).toFile()));
      monitor.worked(1);

      if (monitor.isCanceled()) {
        return;
      }
      monitor.done();
    } catch (Exception e) {
      ServerPlugin.log(Level.WARNING,
          "Could not load Tomcat v9.0 configuration from " + path.toOSString() + ": " + e.getMessage());
      throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorCouldNotLoadConfiguration, path.toOSString()), e));
    }
  }


  /**
   * Loads the information held by this object to the given directory.
   *
   * @param path
   * @param folder
   * @param monitor
   */
  @Override
  public void loadConfiguration(IPath path, IFolder folder, IProgressMonitor monitor) throws CoreException {
    try {
      monitor = ProgressUtil.getMonitorFor(monitor);
      monitor.beginTask(Messages.loadingTask, 1200);

      // Load server properties
      IFile file = folder.getFile(Server10Configuration.SERVER);
      configuration = new Configuration();
      configuration.load(file.getContents());
      monitor.worked(200);

      if (monitor.isCanceled()) {
        throw new Exception("Cancelled");
      }
      monitor.done();
    } catch (Exception e) {
      ServerPlugin.log(Level.WARNING,
          "Could not reload Tomcat v9.0 configuration from: " + folder.getFullPath() + ": " + e.getMessage());
      throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorCouldNotLoadConfiguration, folder.getFullPath().toOSString()), e));
    }
  }

  /**
   * Save the information held by this object to the given directory.
   *
   * @param path
   * @param folder
   * @param monitor
   */
  @Override
  public void saveConfiguration(IPath path, IFolder folder, IProgressMonitor monitor) throws CoreException {
    try {
      monitor = ProgressUtil.getMonitorFor(monitor);
      monitor.beginTask(Messages.savingTask, 1200);

      StringWriter writer = new StringWriter();
      configuration.save(writer, "", Format.INI);
      InputStream in = new ByteArrayInputStream(writer.toString().getBytes());

      // save server properties
      IFile file = folder.getFile(Server10Configuration.SERVER);
      if (file.exists()) {
        file.setContents(in, true, true, ProgressUtil.getSubMonitorFor(monitor, 100));
      } else {
        file.create(in, true, ProgressUtil.getSubMonitorFor(monitor, 100));
      }
      monitor.worked(100);

      // save logging properties
      file = folder.getFile(Server10Configuration.LOGGING);
      if (!file.exists()) {
        writer = new StringWriter();
        writer.write(";Global properties\n");
        writer.write("level = INFO\n");
        writer.write("handler = Console\n\n");
        writer.write("[handler.Console]\n");
        writer.write("type  = CONSOLE\n");
        writer.write("level = FINE\n");
        file.create(new ByteArrayInputStream(writer.toString().getBytes()), true,
            ProgressUtil.getSubMonitorFor(monitor, 200));
      }
      monitor.worked(200);

      if (monitor.isCanceled()) {
        return;
      }
      monitor.done();
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Could not save Tomcat v9.0 configuration to " + folder.toString(), e);
      throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorCouldNotSaveConfiguration, new String[] { e.getLocalizedMessage() }), e));
    }
  }

  /**
   * Return a list of the web modules in this server.
   *
   * @return java.util.List
   */
  @Override
  public List<WebModule> getWebModules() {
    List<WebModule> list = new ArrayList<>();

    // try {
    // Context[] contexts = instance.getContexts();
    // if (contexts != null) {
    // for (Context context : contexts) {
    // String reload = context.getReloadable();
    // if (reload == null) {
    // reload = "false";
    // }
    // WebModule module = new WebModule(context.getPath(), context.getDocBase(),
    // context.getSource(),
    // reload.equalsIgnoreCase("true") ? true : false);
    // list.add(module);
    // }
    // }
    // } catch (Exception e) {
    // ServerPlugin.trace(ServerPlugin.SEVERE, "Error getting project refs", e);
    // }
    return list;
  }

  @Override
  public void addWebModule(int index, WebModule module) {
    // try {
    // Context context = instance.createContext(index);
    // if (context != null) {
    // context.setDocBase(module.getDocumentBase());
    // context.setPath(module.getPath());
    // context.setReloadable(module.isReloadable() ? "true" : "false");
    // if ((module.getMemento() != null) && (module.getMemento().length() > 0))
    // {
    // context.setSource(module.getMemento());
    // }
    // isServerDirty = true;
    // firePropertyChangeEvent(ServerConfiguration.WEB_MODULE_PROPERTY_ADD,
    // null, module);
    // }
    // } catch (Exception e) {
    // ServerPlugin.trace(ServerPlugin.SEVERE, "Error adding web module " +
    // module.getPath(),
    // e);
    // }
  }

  /**
   * Change a web module.
   *
   * @param index int
   * @param docBase java.lang.String
   * @param path java.lang.String
   * @param reloadable boolean
   */
  @Override
  public void modifyWebModule(int index, String docBase, String path, boolean reloadable) {
    // try {
    // Context context = instance.getContext(index);
    // if (context != null) {
    // context.setPath(path);
    // context.setDocBase(docBase);
    // context.setReloadable(reloadable ? "true" : "false");
    // isServerDirty = true;
    // WebModule module = new WebModule(path, docBase, null, reloadable);
    // firePropertyChangeEvent(ServerConfiguration.WEB_MODULE_PROPERTY_MODIFY,
    // new Integer(index), module);
    // }
    // } catch (Exception e) {
    // ServerPlugin.trace(ServerPlugin.SEVERE, "Error modifying web module " +
    // index, e);
    // }
  }

  /**
   * Removes a web module.
   *
   * @param index int
   */
  @Override
  public void removeWebModule(int index) {
    // try {
    // instance.removeContext(index);
    // isServerDirty = true;
    // firePropertyChangeEvent(ServerConfiguration.WEB_MODULE_PROPERTY_REMOVE,
    // null, new Integer(index));
    // } catch (Exception e) {
    // ServerPlugin.trace(ServerPlugin.SEVERE, "Error removing module ref " +
    // index, e);
    // }
  }

  /**
   * Cleanup the server instance. This consists of deleting the work directory associated with
   * Contexts that are going away in the up coming publish.
   *
   * @param baseDir
   * @param installDir
   * @param monitor
   */
  @Override
  public IStatus cleanupServer(IPath baseDir, IPath installDir, IProgressMonitor monitor) {
    return Status.OK_STATUS;// TomcatVersionHelper.cleanupCatalinaServer(baseDir,
                            // installDir, getWebModules(), monitor);
  }

  /**
   * If modules are not being deployed to the "webapps" directory, the context for the published
   * modules is updated to contain the corrected docBase.
   *
   * @param baseDir
   * @param deployDir
   * @param server
   *
   * @see IServerConfiguration#localizeConfiguration(IPath, IPath, ServerWrapper, IProgressMonitor)
   */
  @Override
  public IStatus localizeConfiguration(IPath baseDir, IPath deployDir, ServerWrapper wrapper,
      IProgressMonitor monitor) {
    return Status.OK_STATUS;// TomcatVersionHelper.localizeConfiguration(baseDir,
                            // deployDir, wrapper, monitor);
  }
}
