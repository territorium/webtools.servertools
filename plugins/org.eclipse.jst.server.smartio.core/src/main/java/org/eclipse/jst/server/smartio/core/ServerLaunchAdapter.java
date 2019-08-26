/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.Servlet;
import org.eclipse.jst.server.smartio.core.ServerPlugin.Level;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.LaunchableAdapterDelegate;
import org.eclipse.wst.server.core.util.HttpLaunchable;
import org.eclipse.wst.server.core.util.WebResource;

import java.net.URL;

/**
 * The {@link ServerLaunchAdapter} delegates for Web resources in the server.
 */
public class ServerLaunchAdapter extends LaunchableAdapterDelegate {

  /**
   * Get the launchable adapter.
   *
   * @param server
   * @param artifact
   */
  @Override
  public Object getLaunchable(IServer server, IModuleArtifact artifact) {
    ServerPlugin.log(Level.FINER, "ServerLaunchAdapter " + server + "-" + artifact);
    if (server.getAdapter(ServerWrapper.class) == null) {
      return null;
    }

    if (!(artifact instanceof Servlet) && !(artifact instanceof WebResource)) {
      return null;
    }

    if (artifact.getModule().loadAdapter(IWebModule.class, null) == null) {
      return null;
    }

    try {
      IURLProvider urlProvider = (IURLProvider) server.loadAdapter(IURLProvider.class, null);
      URL url = urlProvider.getModuleRootURL(artifact.getModule());
      ServerPlugin.log(Level.FINER, "root: " + url);

      if (artifact instanceof Servlet) {
        Servlet servlet = (Servlet) artifact;
        if (servlet.getAlias() != null) {
          String path = servlet.getAlias();
          if (path.startsWith("/")) {
            path = path.substring(1);
          }
          url = new URL(url, path);
        } else {
          url = new URL(url, "servlet/" + servlet.getServletClassName());
        }
      } else if (artifact instanceof WebResource) {
        WebResource resource = (WebResource) artifact;
        String path = resource.getPath().toString();
        ServerPlugin.log(Level.FINER, "path: " + path);
        if ((path != null) && path.startsWith("/") && (path.length() > 0)) {
          path = path.substring(1);
        }
        if ((path != null) && (path.length() > 0)) {
          url = new URL(url, path);
        }
      }
      return new HttpLaunchable(url);
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Error getting URL for " + artifact, e);
      return null;
    }
  }
}
