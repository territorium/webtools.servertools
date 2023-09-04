/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IURLProvider;

/**
 *
 */
public interface IServerWrapper extends IURLProvider {

  String PROPERTY_CONF_DIR           = "confDir";
  String PROPERTY_ENABLED_EXTENSIONS = "enabledExtensions";

  /**
   * Gets the directory to which web applications are to be deployed. If relative, it is relative to
   * the runtime base directory for the server.
   */
  String getConfDirectory();

  void setConfDirectory(String directory);

  /**
   * Gets the directory to which web applications are to be deployed. If relative, it is relative to
   * the runtime base directory for the server.
   */
  boolean isNoLogin();

  void setNoLogin(boolean value);

  /**
   * Gets the directory to which web applications are to be deployed. If relative, it is relative to
   * the runtime base directory for the server.
   */
  String getDeployDirectory();

  /**
   * Returns true if contexts should be made reloadable by default.
   */
  boolean enabledExtensions();

  /**
   * Get the related {@link IServer}.
   */
  IServer getServer();

  /**
   * Gets the server handler for the instantiated version.
   */
  IServerInstallation getHandler();

  /**
   * Gets the base directory where the server instance runs. This path can vary depending on the
   * configuration. Null may be returned if a runtime hasn't been specified for the server.
   */
  IPath getRuntimeBaseDirectory();

  IServerConfiguration loadConfiguration() throws CoreException;

  void saveConfiguration(IProgressMonitor monitor) throws CoreException;
}
