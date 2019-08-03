/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * The server plugin.
 */
public class ServerPlugin extends Plugin {

  private static ServerPlugin                  singleton;

  public static final String                   PLUGIN_ID             = "org.eclipse.jst.server.smartio.core";

  static final String                          SERVER_10             = "org.eclipse.jst.server.smartio.10";

  private static final IStatus                 emptyInstallDirStatus =
      new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorInstallDirEmpty, null);

  private static ConfigurationResourceListener configurationListener;

  /**
   * {@link ServerPlugin} constructor comment.
   */
  public ServerPlugin() {
    ServerPlugin.singleton = this;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    ServerPlugin.configurationListener = new ConfigurationResourceListener();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(ServerPlugin.configurationListener,
        IResourceChangeEvent.POST_CHANGE);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(ServerPlugin.configurationListener);
    super.stop(context);
  }

  /**
   * Returns the singleton instance of this plugin.
   */
  static ServerPlugin getInstance() {
    return ServerPlugin.singleton;
  }

  /**
   * Return the install location preference.
   * 
   * @param id a runtime type id
   * @return the install location
   */
  static String getPreference(String id) {
    return ServerPlugin.getInstance().getPluginPreferences().getString(id);
  }

  /**
   * Set the install location preference.
   * 
   * @param id the runtimt type id
   * @param value the location
   */
  public static void setPreference(String id, String value) {
    ServerPlugin.getInstance().getPluginPreferences().setValue(id, value);
    ServerPlugin.getInstance().savePluginPreferences();
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
   * Return the version handler.
   * 
   * @param id
   * @return a version handler
   */
  static IServerVersionHandler getVersionHandler(String id) {
    if (id.indexOf("runtime") > 0) {
      id = id.substring(0, 30) + id.substring(38);
    }
    // id = id.substring(0, id.length() - 8);
    if (ServerPlugin.SERVER_10.equals(id)) {
      return new Server10Handler();
    } else {
      return null;
    }
  }

  /**
   * Utility method to verify an installation directory according to the specified server ID. The
   * verification includes checking the installation directory name to see if it indicates a
   * different version of the server.
   * 
   * @param installPath Path to verify
   * @param id Type ID of the server
   * @return Status of the verification. Will be Status.OK_STATUS, if verification was successful,
   *         or error status if not.
   */
  static IStatus verifyInstallPathWithFolderCheck(IPath installPath, String version) {
    if (version == null) {
      return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorVersionEmpty, null);
    }
    if (installPath == null) {
      return ServerPlugin.emptyInstallDirStatus;
    }
    return Status.OK_STATUS;
  }

  /**
   * Return a <code>java.io.File</code> object that corresponds to the specified <code>IPath</code>
   * in the plugin directory.
   * 
   * @return a file
   */
  protected static File getPlugin() {
    try {
      URL installURL = ServerPlugin.getInstance().getBundle().getEntry("/");
      return new File(FileLocator.toFileURL(installURL).getFile());
    } catch (IOException ioe) {
      return null;
    }
  }
}
