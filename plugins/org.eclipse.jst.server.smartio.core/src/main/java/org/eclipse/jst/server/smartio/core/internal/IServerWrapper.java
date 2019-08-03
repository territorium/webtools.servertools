/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.wst.server.core.model.IURLProvider;

/**
 *
 */
public interface IServerWrapper extends IURLProvider {

  // The default deployment directory. Avoid "webapps" due to side effects.
  public static final String DEFAULT_DEPLOYDIR = "wtpwebapps";

  // The deployment directory used by default in prior versions.
  public static final String LEGACY_DEPLOYDIR = "webapps";

  // Property which specifies the directory where the server instance exists.
  public static final String PROPERTY_INSTANCE_DIR = "instanceDir";

  // Property which specifies the directory where web applications are
  // published.
  public static final String PROPERTY_DEPLOY_DIR = "deployDir";

  // Property which specifies contexts should be reloadable by default.
  public static final String PROPERTY_MODULES_RELOADABLE_BY_DEFAULT = "modulesReloadableByDefault";

  /**
   * Gets the directory where the server instance exists. If not set, the
   * instance directory is derived from the testEnvironment setting.
   */
  public String getInstanceDirectory();

  /**
   * Gets the directory to which web applications are to be deployed. If
   * relative, it is relative to the runtime base directory for the server.
   */
  public String getDeployDirectory();
}
