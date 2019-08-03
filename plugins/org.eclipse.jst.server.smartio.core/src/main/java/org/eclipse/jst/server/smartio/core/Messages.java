/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {

  public static String errorCopyingFile;
  public static String errorVersionEmpty;
  public static String errorUnknownVersion;
  public static String errorInstallDirEmpty;
  public static String errorInstallDirWrongVersion2;
  public static String errorInstallDirTrailingSlash;


  public static String warningJRE;
  public static String warningCantReadConfig;
  public static String loadingTask;
  public static String errorCouldNotLoadConfiguration;
  public static String savingTask;
  public static String errorPublish;
  public static String errorCouldNotSaveConfiguration;
  public static String errorSpec10;
  public static String portServer;
  public static String publishConfigurationTask;
  public static String errorPublishConfiguration;
  public static String publisherPublishTask;
  public static String errorNoConfiguration;
  public static String errorConfigurationProjectClosed;
  public static String errorWebModulesOnly;
  public static String errorNoRuntime;
  public static String publishServerTask;
  public static String errorPortInvalid;
  public static String errorPortInUse;
  public static String errorPortsInUse;
  public static String errorDuplicateContextRoot;
  public static String errorPublishCouldNotRemoveModule;

  public static String configurationEditorActionModifyPort;
  public static String configurationEditorActionAddWebModule;
  public static String configurationEditorActionModifyWebModule;
  public static String configurationEditorActionRemoveWebModule;
  public static String serverEditorActionSetSecure;
  public static String serverEditorActionSetDeployDirectory;

  static {
    NLS.initializeMessages(ServerPlugin.PLUGIN_ID + ".Messages", Messages.class);
  }
}