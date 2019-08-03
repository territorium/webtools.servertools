/**********************************************************************
 * Copyright (c) 2011 SAS Institute, Inc and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: SAS Institute, Inc - Initial API and implementation
 **********************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.ServerType;

class ConfigurationResourceListener implements IResourceChangeListener {

  private IProject serversProject;

  /**
   * Currently, only changes to server configuration files are detected and the associated server's
   * state updated. This method needs to be as brief as possible if the change is unrelated to
   * server configuration changes. Since the Servers project would change so rarely, it is worth
   * saving some cycles in the resource listener by caching this project.
   */
  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
      IProject project = getServersProject();
      if (project != null) {
        IResourceDelta delta = event.getDelta();
        if (delta != null) {
          IResourceDelta serversProjectDelta = delta.findMember(project.getFullPath());
          if (serversProjectDelta != null) {
            // The change occurred within the Servers project.
            IResourceDelta[] childDelta = serversProjectDelta.getAffectedChildren();
            if (childDelta.length > 0) {
              IServer[] servers = ServerCore.getServers();
              for (IResourceDelta element : childDelta) {
                // Check if this subfolder of the Servers folder matches a server configuration
                // folder
                for (IServer server : servers) {
                  IServerType serverType = server.getServerType();
                  if (serverType.getId().startsWith("org.eclipse.jst.server.smartio.")) {
                    IFolder configFolder = server.getServerConfiguration();
                    if (configFolder != null) {
                      if (element.getFullPath().equals(configFolder.getFullPath())) {
                        // Found a server server affected by this delta. Update this server's
                        // publish state.
                        ServerBehaviour tcServerBehaviour =
                            (ServerBehaviour) server.loadAdapter(ServerBehaviour.class, null);
                        if (tcServerBehaviour != null) {
                          // Indicate that this server needs to publish and restart if running
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

  private IProject getServersProject() {
    if (serversProject == null) {
      IProject project;
      try {
        project = ServerType.getServerProject();
        synchronized (this) {
          serversProject = project;
        }
      } catch (CoreException e) {
        // Ignore
      }
    }
    return serversProject;
  }
}
