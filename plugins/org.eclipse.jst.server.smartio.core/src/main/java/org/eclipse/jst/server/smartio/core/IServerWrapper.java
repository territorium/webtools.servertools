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

  // Property which specifies the directory where configurations are stored
  // published.
  public static final String PROPERTY_CONF_DIR = "confDir";

  // Property which specifies the directory where configurations are stored
  // published.
//  public static final String PROPERTY_DEPLOY_DIR = "deployDir";

  // Property which specifies contexts should be reloadable by default.
  public static final String PROPERTY_MODULES_RELOADABLE = "modulesReloadable";

  /**
   * Gets the directory to which web applications are to be deployed. If relative, it is relative to
   * the runtime base directory for the server.
   */
  public String getConfDirectory();

  public void setConfDirectory(String directory);

  /**
   * Gets the directory to which web applications are to be deployed. If relative, it is relative to
   * the runtime base directory for the server.
   */
  public String getDeployDirectory();

//  /**
//   * Gets the directory to which web applications are to be deployed. If relative, it is relative to
//   * the runtime base directory for the server.
//   */
//  public void setDeployDirectory(String directory);

  /**
   * Returns true if contexts should be made reloadable by default.
   */
  public boolean isModulesReloadable();

  /**
   * Get the related {@link IServer}.
   */
  public IServer getServer();

  /**
   * Gets the server handler for the instantiated version.
   */
  public IServerInstallation getHandler();

  /**
   * Gets the base directory where the server instance runs. This path can vary depending on the
   * configuration. Null may be returned if a runtime hasn't been specified for the server.
   */
  public IPath getRuntimeBaseDirectory();

  public IServerConfiguration loadConfiguration() throws CoreException;

  public void saveConfiguration(IProgressMonitor monitor) throws CoreException;
}
