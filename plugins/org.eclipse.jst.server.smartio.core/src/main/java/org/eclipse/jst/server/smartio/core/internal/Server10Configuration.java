/**********************************************************************
 * Copyright (c) 2016 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 **********************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.smartio.core.internal.xml.Factory;
import org.eclipse.jst.server.smartio.core.internal.xml.XMLUtil;
import org.eclipse.jst.server.smartio.core.internal.xml.server40.Connector;
import org.eclipse.jst.server.smartio.core.internal.xml.server40.Context;
import org.eclipse.jst.server.smartio.core.internal.xml.server40.Server;
import org.eclipse.jst.server.smartio.core.internal.xml.server40.ServerInstance;
import org.eclipse.jst.server.smartio.core.internal.xml.server40.Service;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.ServerPort;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * smart.IO server configuration.
 */
class Server10Configuration extends ServerConfiguration {


  private Server                           server;
  private ServerInstance                   instance;
  private Factory                          factory;
  private boolean                          isServerDirty;

  private WebAppDocument                   webAppDocument;

  private Document                         contextDocument;

  private Document                         usersDocument;

  private String                           policyFile;

  private String                           propertiesFile;

  private static final Map<String, String> protocolHandlerMap = new HashMap<>();
  static {
    Server10Configuration.protocolHandlerMap.put("org.apache.coyote.http11.Http11Protocol", "HTTP/1.1");
    Server10Configuration.protocolHandlerMap.put("org.apache.coyote.http11.Http11NioProtocol", "HTTP/1.1");
    Server10Configuration.protocolHandlerMap.put("org.apache.coyote.http11.Http11AprProtocol", "HTTP/1.1");
    Server10Configuration.protocolHandlerMap.put("org.apache.coyote.ajp.AjpAprProtocol", "AJP/1.3");
    Server10Configuration.protocolHandlerMap.put("org.apache.jk.server.JkCoyoteHandler", "AJP/1.3");
  }

  /**
   * {@link Server10Configuration} constructor.
   * 
   * @param path a path
   */
  Server10Configuration(IFolder path) {
    super(path);
  }

  /**
   * Return the port number.
   * 
   * @return int
   */
  @Override
  public ServerPort getMainPort() {
    Iterator<ServerPort> iterator = getServerPorts().iterator();
    while (iterator.hasNext()) {
      ServerPort port = iterator.next();
      // Return only an HTTP port from the selected Service
      if (port.getProtocol().toLowerCase().equals("http") && (port.getId().indexOf('/') < 0)) {
        return port;
      }
    }
    return null;
  }

  /**
   * Returns the mime mappings.
   * 
   * @return java.util.List
   */
  @Override
  public List<MimeMapping> getMimeMappings() {
    return webAppDocument.getMimeMappings();
  }

  /**
   * Returns a list of ServerPorts that this configuration uses.
   *
   * @return java.util.List
   */
  @Override
  public List<ServerPort> getServerPorts() {
    List<ServerPort> ports = new ArrayList<>();

    // first add server port
    try {
      int port = Integer.parseInt(server.getPort());
      ports.add(new ServerPort("server", Messages.portServer, port, "TCPIP"));
    } catch (Exception e) {
      // ignore
    }

    // add connectors
    try {
      String instanceServiceName = instance.getService().getName();
      int size = server.getServiceCount();
      for (int i = 0; i < size; i++) {
        Service service = server.getService(i);
        int size2 = service.getConnectorCount();
        for (int j = 0; j < size2; j++) {
          Connector connector = service.getConnector(j);
          String name = "HTTP/1.1";
          String protocol2 = "HTTP";
          boolean advanced = true;
          String[] contentTypes = null;
          int port = -1;
          try {
            port = Integer.parseInt(connector.getPort());
          } catch (Exception e) {
            // ignore
          }
          String protocol = connector.getProtocol();
          if ((protocol != null) && (protocol.length() > 0)) {
            if (protocol.startsWith("HTTP")) {
              name = protocol;
            } else if (protocol.startsWith("AJP")) {
              name = protocol;
              protocol2 = "AJP";
            } else {
              // Get server equivalent name if protocol handler class specified
              name = Server10Configuration.protocolHandlerMap.get(protocol);
              if (name != null) {
                // Prepare simple protocol string for ServerPort protocol
                int index = name.indexOf('/');
                if (index > 0) {
                  protocol2 = name.substring(0, index);
                } else {
                  protocol2 = name;
                }
              }
              // Specified protocol is unknown, just use as is
              else {
                name = protocol;
                protocol2 = protocol;
              }
            }
          }
          if (protocol2.toLowerCase().equals("http")) {
            contentTypes = new String[] { "web", "webservices" };
          }
          String secure = connector.getSecure();
          if ((secure != null) && (secure.length() > 0)) {
            name = "SSL";
            protocol2 = "SSL";
          } else {
            advanced = false;
          }
          String portId;
          if ((instanceServiceName != null) && instanceServiceName.equals(service.getName())) {
            portId = Integer.toString(j);
          } else {
            portId = i + "/" + j;
          }
          ports.add(new ServerPort(portId, name, port, protocol2, contentTypes, advanced));
        }
      }
    } catch (Exception e) {
      Trace.trace(Trace.SEVERE, "Error getting server ports", e);
    }
    return ports;
  }

  /**
   * Return a list of the web modules in this server.
   * 
   * @return java.util.List
   */
  @Override
  public List<WebModule> getWebModules() {
    List<WebModule> list = new ArrayList<>();

    try {
      Context[] contexts = instance.getContexts();
      if (contexts != null) {
        for (Context context : contexts) {
          String reload = context.getReloadable();
          if (reload == null) {
            reload = "false";
          }
          WebModule module = new WebModule(context.getPath(), context.getDocBase(), context.getSource(),
              reload.equalsIgnoreCase("true") ? true : false);
          list.add(module);
        }
      }
    } catch (Exception e) {
      Trace.trace(Trace.SEVERE, "Error getting project refs", e);
    }
    return list;
  }

  /**
   * @see ServerConfiguration#getServerWorkDirectory(IPath)
   */
  @Override
  public IPath getServerWorkDirectory(IPath basePath) {
    return instance.getHostWorkDirectory(basePath);
  }

  /**
   * @see ServerConfiguration#getContextWorkDirectory(IPath, IServerWebModule)
   */
  @Override
  public IPath getContextWorkDirectory(IPath basePath, IServerWebModule module) {
    Context context = instance.getContext(module.getPath());
    if (context != null) {
      return instance.getContextWorkDirectory(basePath, context);
    }

    return null;
  }

  /**
   * @see ServerConfiguration#load(IPath, IProgressMonitor)
   */
  @Override
  public void load(IPath path, IProgressMonitor monitor) throws CoreException {
    try {
      monitor = ProgressUtil.getMonitorFor(monitor);
      monitor.beginTask(Messages.loadingTask, 7);

      // check for catalina.policy to verify that this is a v9.0 config
      InputStream in = new FileInputStream(path.append("catalina.policy").toFile());
      in.read();
      in.close();
      monitor.worked(1);

      factory = new Factory();
      server = (Server) factory.loadDocument(new FileInputStream(path.append("server.xml").toFile()));
      instance = new ServerInstance(server, null, null);
      monitor.worked(1);

      webAppDocument = new WebAppDocument(path.append("web.xml"));
      monitor.worked(1);

      File file = path.append("context.xml").toFile();
      if (file.exists()) {
        contextDocument = XMLUtil.getDocumentBuilder().parse(new InputSource(new FileInputStream(file)));
      }
      monitor.worked(1);

      usersDocument = XMLUtil.getDocumentBuilder()
          .parse(new InputSource(new FileInputStream(path.append("tomcat-users.xml").toFile())));
      monitor.worked(1);

      // load policy file
      policyFile = TomcatVersionHelper.getFileContents(new FileInputStream(path.append("catalina.policy").toFile()));
      monitor.worked(1);

      // load properties file
      file = path.append("catalina.properties").toFile();
      if (file.exists()) {
        propertiesFile = TomcatVersionHelper.getFileContents(new FileInputStream(file));
      } else {
        propertiesFile = null;
      }
      monitor.worked(1);

      if (monitor.isCanceled()) {
        return;
      }
      monitor.done();
    } catch (Exception e) {
      Trace.trace(Trace.WARNING,
          "Could not load Tomcat v9.0 configuration from " + path.toOSString() + ": " + e.getMessage());
      throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorCouldNotLoadConfiguration, path.toOSString()), e));
    }
  }

  /**
   * @see ServerConfiguration#importFromPath(IPath, boolean, IProgressMonitor)
   */
  @Override
  public void importFromPath(IPath path, boolean isTestEnv, IProgressMonitor monitor) throws CoreException {
    load(path, monitor);

    // for test environment, remove existing contexts since a separate
    // catalina.base will be used
    if (isTestEnv) {
      while (instance.removeContext(0)) {
        // no-op
      }
    }
  }

  /**
   * @see ServerConfiguration#load(IFolder, IProgressMonitor)
   */
  @Override
  public void load(IFolder folder, IProgressMonitor monitor) throws CoreException {
    try {
      monitor = ProgressUtil.getMonitorFor(monitor);
      monitor.beginTask(Messages.loadingTask, 1200);

      // check for catalina.policy to verify that this is a v4.0 config
      IFile file = folder.getFile("catalina.policy");
      if (!file.exists()) {
        throw new CoreException(new Status(IStatus.WARNING, ServerPlugin.PLUGIN_ID, 0,
            NLS.bind(Messages.errorCouldNotLoadConfiguration, folder.getFullPath().toOSString()), null));
      }

      // load server.xml
      file = folder.getFile("server.xml");
      InputStream in = file.getContents();
      factory = new Factory();
      server = (Server) factory.loadDocument(in);
      instance = new ServerInstance(server, null, null);
      monitor.worked(200);

      // load web.xml
      file = folder.getFile("web.xml");
      webAppDocument = new WebAppDocument(file);
      monitor.worked(200);

      // load context.xml
      file = folder.getFile("context.xml");
      if (file.exists()) {
        in = file.getContents();
        contextDocument = XMLUtil.getDocumentBuilder().parse(new InputSource(in));
      } else {
        contextDocument = null;
      }
      monitor.worked(200);

      // load tomcat-users.xml
      file = folder.getFile("tomcat-users.xml");
      in = file.getContents();

      usersDocument = XMLUtil.getDocumentBuilder().parse(new InputSource(in));
      monitor.worked(200);

      // load catalina.policy
      file = folder.getFile("catalina.policy");
      in = file.getContents();
      policyFile = TomcatVersionHelper.getFileContents(in);
      monitor.worked(200);

      // load catalina.properties
      file = folder.getFile("catalina.properties");
      if (file.exists()) {
        in = file.getContents();
        propertiesFile = TomcatVersionHelper.getFileContents(in);
      } else {
        propertiesFile = null;
      }
      monitor.worked(200);

      if (monitor.isCanceled()) {
        throw new Exception("Cancelled");
      }
      monitor.done();
    } catch (Exception e) {
      Trace.trace(Trace.WARNING,
          "Could not reload Tomcat v9.0 configuration from: " + folder.getFullPath() + ": " + e.getMessage());
      throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorCouldNotLoadConfiguration, folder.getFullPath().toOSString()), e));
    }
  }

  /**
   * Save the information held by this object to the given directory.
   *
   * @param folder a folder
   * @param monitor a progress monitor
   * @throws CoreException
   */
  @Override
  public void save(IFolder folder, IProgressMonitor monitor) throws CoreException {
    try {
      monitor = ProgressUtil.getMonitorFor(monitor);
      monitor.beginTask(Messages.savingTask, 1200);

      // save server.xml
      byte[] data = factory.getContents();
      InputStream in = new ByteArrayInputStream(data);
      IFile file = folder.getFile("server.xml");
      if (file.exists()) {
        if (isServerDirty) {
          file.setContents(in, true, true, ProgressUtil.getSubMonitorFor(monitor, 200));
        } else {
          monitor.worked(200);
        }
      } else {
        file.create(in, true, ProgressUtil.getSubMonitorFor(monitor, 200));
      }
      isServerDirty = false;

      // save web.xml
      webAppDocument.save(folder.getFile("web.xml"), ProgressUtil.getSubMonitorFor(monitor, 200));

      // save context.xml
      if (contextDocument != null) {
        data = XMLUtil.getContents(contextDocument);
        in = new ByteArrayInputStream(data);
        file = folder.getFile("context.xml");
        if (file.exists()) {
          monitor.worked(200);
          // file.setContents(in, true, true, ProgressUtil.getSubMonitorFor(monitor, 200));
        } else {
          file.create(in, true, ProgressUtil.getSubMonitorFor(monitor, 200));
        }
      }

      // save tomcat-users.xml
      data = XMLUtil.getContents(usersDocument);
      in = new ByteArrayInputStream(data);
      file = folder.getFile("tomcat-users.xml");
      if (file.exists()) {
        monitor.worked(200);
        // file.setContents(in, true, true, ProgressUtil.getSubMonitorFor(monitor, 200));
      } else {
        file.create(in, true, ProgressUtil.getSubMonitorFor(monitor, 200));
      }

      // save catalina.policy
      in = new ByteArrayInputStream(policyFile.getBytes());
      file = folder.getFile("catalina.policy");
      if (file.exists()) {
        monitor.worked(200);
        // file.setContents(in, true, true, ProgressUtil.getSubMonitorFor(monitor, 200));
      } else {
        file.create(in, true, ProgressUtil.getSubMonitorFor(monitor, 200));
      }

      // save catalina.properties
      if (propertiesFile != null) {
        in = new ByteArrayInputStream(propertiesFile.getBytes());
        file = folder.getFile("catalina.properties");
        if (file.exists()) {
          monitor.worked(200);
          // file.setContents(in, true, true, ProgressUtil.getSubMonitorFor(monitor, 200));
        } else {
          file.create(in, true, ProgressUtil.getSubMonitorFor(monitor, 200));
        }
      } else {
        monitor.worked(200);
      }

      if (monitor.isCanceled()) {
        return;
      }
      monitor.done();
    } catch (Exception e) {
      Trace.trace(Trace.SEVERE, "Could not save Tomcat v9.0 configuration to " + folder.toString(), e);
      throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorCouldNotSaveConfiguration, new String[] { e.getLocalizedMessage() }), e));
    }
  }

  @Override
  public void addMimeMapping(int index, MimeMapping map) {
    webAppDocument.addMimeMapping(index, map);
    firePropertyChangeEvent(ServerConfiguration.MAPPING_PROPERTY_ADD, new Integer(index), map);
  }

  @Override
  public void addWebModule(int index, IServerWebModule module) {
    try {
      Context context = instance.createContext(index);
      if (context != null) {
        context.setDocBase(module.getDocumentBase());
        context.setPath(module.getPath());
        context.setReloadable(module.isReloadable() ? "true" : "false");
        if ((module.getMemento() != null) && (module.getMemento().length() > 0)) {
          context.setSource(module.getMemento());
        }
        isServerDirty = true;
        firePropertyChangeEvent(ServerConfiguration.WEB_MODULE_PROPERTY_ADD, null, module);
      }
    } catch (Exception e) {
      Trace.trace(Trace.SEVERE, "Error adding web module " + module.getPath(), e);
    }
  }

  /**
   * Change the extension of a mime mapping.
   * 
   * @param index
   * @param map
   */
  @Override
  public void modifyMimeMapping(int index, MimeMapping map) {
    webAppDocument.modifyMimeMapping(index, map);
    firePropertyChangeEvent(ServerConfiguration.MAPPING_PROPERTY_MODIFY, new Integer(index), map);
  }

  /**
   * Modify the port with the given id.
   *
   * @param id java.lang.String
   * @param port int
   */
  @Override
  public void modifyServerPort(String id, int port) {
    try {
      if ("server".equals(id)) {
        server.setPort(port + "");
        isServerDirty = true;
        firePropertyChangeEvent(ServerConfiguration.MODIFY_PORT_PROPERTY, id, new Integer(port));
        return;
      }

      int i = id.indexOf("/");
      // If a connector in the instance Service
      if (i < 0) {
        int connNum = Integer.parseInt(id);
        Connector connector = instance.getConnector(connNum);
        if (connector != null) {
          connector.setPort(port + "");
          isServerDirty = true;
          firePropertyChangeEvent(ServerConfiguration.MODIFY_PORT_PROPERTY, id, new Integer(port));
        }
      }
      // Else a connector in another Service
      else {
        int servNum = Integer.parseInt(id.substring(0, i));
        int connNum = Integer.parseInt(id.substring(i + 1));

        Service service = server.getService(servNum);
        Connector connector = service.getConnector(connNum);
        connector.setPort(port + "");
        isServerDirty = true;
        firePropertyChangeEvent(ServerConfiguration.MODIFY_PORT_PROPERTY, id, new Integer(port));
      }
    } catch (Exception e) {
      Trace.trace(Trace.SEVERE, "Error modifying server port " + id, e);
    }
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
    try {
      Context context = instance.getContext(index);
      if (context != null) {
        context.setPath(path);
        context.setDocBase(docBase);
        context.setReloadable(reloadable ? "true" : "false");
        isServerDirty = true;
        WebModule module = new WebModule(path, docBase, null, reloadable);
        firePropertyChangeEvent(ServerConfiguration.WEB_MODULE_PROPERTY_MODIFY, new Integer(index), module);
      }
    } catch (Exception e) {
      Trace.trace(Trace.SEVERE, "Error modifying web module " + index, e);
    }
  }

  /**
   * Removes a mime mapping.
   * 
   * @param index int
   */
  @Override
  public void removeMimeMapping(int index) {
    webAppDocument.removeMimeMapping(index);
    firePropertyChangeEvent(ServerConfiguration.MAPPING_PROPERTY_REMOVE, null, new Integer(index));
  }

  /**
   * Removes a web module.
   * 
   * @param index int
   */
  @Override
  public void removeWebModule(int index) {
    try {
      instance.removeContext(index);
      isServerDirty = true;
      firePropertyChangeEvent(ServerConfiguration.WEB_MODULE_PROPERTY_REMOVE, null, new Integer(index));
    } catch (Exception e) {
      Trace.trace(Trace.SEVERE, "Error removing module ref " + index, e);
    }
  }

  /**
   * Add context configuration found in META-INF/context.xml files present in projects to published
   * server.xml.
   * 
   * @param baseDir path to catalina instance directory
   * @param deployDir path to deployment directory
   * @param monitor a progress monitor or null
   * @return result of operation
   */
  @Override
  protected IStatus publishContextConfig(IPath baseDir, IPath deployDir, IProgressMonitor monitor) {
    return TomcatVersionHelper.publishCatalinaContextConfig(baseDir, deployDir, monitor);
  }

  /**
   * Update contexts in server.xml to serve projects directly without publishing.
   * 
   * @param baseDir path to catalina instance directory
   * @param monitor a progress monitor or null
   * @return result of operation
   */
  @Override
  protected IStatus updateContextsToServeDirectly(IPath baseDir, String version, String loader,
      IProgressMonitor monitor) {
    return TomcatVersionHelper.updateContextsToServeDirectly(baseDir, version, loader, true, monitor);
  }

  /**
   * Cleanup the server instance. This consists of deleting the work directory associated with
   * Contexts that are going away in the up coming publish.
   * 
   * @param baseDir path to server instance directory, i.e. catalina.base
   * @param installDir path to server installation directory (not currently used)
   * @param monitor a progress monitor or null
   * @return MultiStatus containing results of the cleanup operation
   */
  @Override
  protected IStatus cleanupServer(IPath baseDir, IPath installDir, boolean removeKeptContextFiles,
      IProgressMonitor monitor) {
    return TomcatVersionHelper.cleanupCatalinaServer(baseDir, installDir, removeKeptContextFiles, getWebModules(),
        monitor);
  }

  /**
   * @see ServerConfiguration#localizeConfiguration(IPath, IPath, ServerWrapper, IProgressMonitor)
   */
  @Override
  public IStatus localizeConfiguration(IPath baseDir, IPath deployDir, ServerWrapper wrapper,
      IProgressMonitor monitor) {
    return TomcatVersionHelper.localizeConfiguration(baseDir, deployDir, wrapper, monitor);
  }
}
