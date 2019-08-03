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
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.ServerType;
import org.osgi.framework.BundleContext;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The server plugin.
 */
public class ServerPlugin extends Plugin implements IResourceChangeListener {


  public static final String PLUGIN_NS  = "org.eclipse.jst.server.smartio.";
  public static final String PLUGIN_ID  = "org.eclipse.jst.server.smartio.core";

  public static final String SERVER_10  = "org.eclipse.jst.server.smartio.10";
  public static final String RUNTIME_10 = "org.eclipse.jst.server.smartio.runtime.10";


  public static final IStatus EmptyInstallDirStatus =
      new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorInstallDirEmpty, null);


  public enum Level {
    CONFIG,
    WARNING,
    SEVERE,
    FINER,
    FINEST;
  }

  private static final String           spacer       = "                                   ";

  private static final SimpleDateFormat sdf          = new SimpleDateFormat("dd/MM/yy HH:mm.ss.SSS");

  private static int                    pluginLength = -1;

  private static ServerPlugin           singleton;


  private IProject serversProject;

  /**
   * {@link ServerPlugin} constructor comment.
   */
  public ServerPlugin() {
    ServerPlugin.singleton = this;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    super.stop(context);
  }

  /**
   * Returns the singleton instance of this plugin.
   */
  static ServerPlugin getInstance() {
    return ServerPlugin.singleton;
  }

  /**
   * Currently, only changes to server configuration files are detected and the associated server's
   * state updated. This method needs to be as brief as possible if the change is unrelated to
   * server configuration changes. Since the Servers project would change so rarely, it is worth
   * saving some cycles in the resource listener by caching this project.
   */
  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
      if (this.serversProject == null) {
        IProject project;
        try {
          project = ServerType.getServerProject();
          synchronized (this) {
            this.serversProject = project;
          }
        } catch (CoreException e) {
          // Ignore
        }
      }
      if (this.serversProject != null) {
        IResourceDelta delta = event.getDelta();
        if (delta != null) {
          IResourceDelta serversProjectDelta = delta.findMember(this.serversProject.getFullPath());
          if (serversProjectDelta != null) {
            // The change occurred within the Servers project.
            IResourceDelta[] childDelta = serversProjectDelta.getAffectedChildren();
            if (childDelta.length > 0) {
              IServer[] servers = ServerCore.getServers();
              for (IResourceDelta element : childDelta) {
                // Check if this subfolder of the Servers folder matches a
                // server configuration
                // folder
                for (IServer server : servers) {
                  IServerType serverType = server.getServerType();
                  if (serverType.getId().startsWith(ServerPlugin.PLUGIN_NS)) {
                    IFolder configFolder = server.getServerConfiguration();
                    if (configFolder != null) {
                      if (element.getFullPath().equals(configFolder.getFullPath())) {
                        // Found a server server affected by this delta. Update
                        // this server's
                        // publish state.
                        ServerBehaviour tcServerBehaviour =
                            (ServerBehaviour) server.loadAdapter(ServerBehaviour.class, null);
                        if (tcServerBehaviour != null) {
                          // Indicate that this server needs to publish and
                          // restart if running
                          tcServerBehaviour.setServerStates(IServer.PUBLISH_STATE_INCREMENTAL, true);
                        }
                        break;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Gets state of debug flag for the plug-in.
   *
   * @return true if tracing is enabled
   */
  public static boolean isTraceEnabled() {
    return ServerPlugin.getInstance().isDebugging();
  }

  /**
   * Convenience method for logging.
   *
   * @param status a status object
   */
  public static void log(IStatus status) {
    ServerPlugin.getInstance().getLog().log(status);
  }

  /**
   * Trace the given text.
   *
   * @param level
   * @param message
   */
  public static void log(Level level, String message) {
    ServerPlugin.log(level, message, null);
  }

  /**
   * Trace the given message and exception.
   *
   * @param level
   * @param message
   * @param throwable
   */
  public static void log(Level level, String message, Throwable throwable) {
    if (!ServerPlugin.getInstance().isDebugging()) {
      return;
    }

    if (message == null) {
      return;
    }

    if (!ServerPlugin.getInstance().isDebugging()) {
      return;
    }

    StringBuffer sb = new StringBuffer(ServerPlugin.PLUGIN_ID);
    if (ServerPlugin.PLUGIN_ID.length() > ServerPlugin.pluginLength) {
      ServerPlugin.pluginLength = ServerPlugin.PLUGIN_ID.length();
    } else if (ServerPlugin.PLUGIN_ID.length() < ServerPlugin.pluginLength) {
      sb.append(ServerPlugin.spacer.substring(0, ServerPlugin.pluginLength - ServerPlugin.PLUGIN_ID.length()));
    }
    sb.append(" ");
    sb.append(level.name());
    sb.append(" ");
    sb.append(ServerPlugin.sdf.format(new Date()));
    sb.append(" ");
    sb.append(message);
    // Platform.getDebugOption(ServerCore.PLUGIN_ID + "/" + "resources");

    System.out.println(sb.toString());
    if (throwable != null) {
      throwable.printStackTrace();
    }
  }
}
