/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui;

/**
 * Constant ids for context help.
 */
public interface ContextIds {

  public static final String SERVER_EDITOR                                 = ServerUIPlugin.PLUGIN_ID + ".teig0000";

  public static final String CONFIGURATION_EDITOR_WEBMODULES               = ServerUIPlugin.PLUGIN_ID + ".tecw0000";
  public static final String CONFIGURATION_EDITOR_WEBMODULES_LIST          = ServerUIPlugin.PLUGIN_ID + ".tecw0002";
  public static final String CONFIGURATION_EDITOR_WEBMODULES_ADD_PROJECT   = ServerUIPlugin.PLUGIN_ID + ".tecw0004";
  public static final String CONFIGURATION_EDITOR_WEBMODULES_ADD_EXTERNAL  = ServerUIPlugin.PLUGIN_ID + ".tecw0006";
  public static final String CONFIGURATION_EDITOR_WEBMODULES_EDIT          = ServerUIPlugin.PLUGIN_ID + ".tecw0008";
  public static final String CONFIGURATION_EDITOR_WEBMODULES_REMOVE        = ServerUIPlugin.PLUGIN_ID + ".tecw0010";

  public static final String CONFIGURATION_EDITOR_WEBMODULE_DIALOG         = ServerUIPlugin.PLUGIN_ID + ".tdwm0000";
  public static final String CONFIGURATION_EDITOR_WEBMODULE_DIALOG_PROJECT = ServerUIPlugin.PLUGIN_ID + ".tdpr0002";
  public static final String CONFIGURATION_EDITOR_WEBMODULE_DIALOG_PATH    = ServerUIPlugin.PLUGIN_ID + ".tdpr0004";
  public static final String CONFIGURATION_EDITOR_WEBMODULE_DIALOG_DOCBASE = ServerUIPlugin.PLUGIN_ID + ".tdpr0006";
  public static final String CONFIGURATION_EDITOR_WEBMODULE_DIALOG_RELOAD  = ServerUIPlugin.PLUGIN_ID + ".tdpr0008";

  public static final String CONFIGURATION_EDITOR_PORTS                    = ServerUIPlugin.PLUGIN_ID + ".tecp0000";
  public static final String CONFIGURATION_EDITOR_PORTS_LIST               = ServerUIPlugin.PLUGIN_ID + ".tecp0002";

  public static final String RUNTIME_COMPOSITE                             = ServerUIPlugin.PLUGIN_ID + ".twnr0000";

  public static final String SERVER_CLEAN_WORK_DIR                         = ServerUIPlugin.PLUGIN_ID + ".tvcp0000";
  public static final String SERVER_CLEAN_WORK_DIR_TERMINATE               = ServerUIPlugin.PLUGIN_ID + ".tvcp0001";
}
