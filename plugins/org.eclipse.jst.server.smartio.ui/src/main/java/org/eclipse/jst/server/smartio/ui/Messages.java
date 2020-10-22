/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {

  public static String wizardTitle;
  public static String wizardDescription;
  public static String runtimeName;
  public static String browse;
  public static String install;
  public static String installDir;
  public static String installDialogTitle;
  public static String selectInstallDir;
  public static String contextCleanup;


  public static String editorEdit;
  public static String editorRemove;
  public static String editorBrowse;
  public static String errorMissingWebModule;


  public static String configurationEditorPortsSection;
  public static String configurationEditorPortsDescription;
  public static String configurationEditorPortNameColumn;
  public static String configurationEditorPortValueColumn;
  public static String configurationEditorWebModulesPageTitle;
  public static String configurationEditorWebModulesSection;
  public static String configurationEditorWebModulesDescription;
  public static String configurationEditorPathColumn;
  public static String configurationEditorDocBaseColumn;
  public static String configurationEditorProjectColumn;
  public static String configurationEditorReloadColumn;
  public static String configurationEditorAddProjectModule;
  public static String configurationEditorAddExternalModule;
  public static String configurationEditorProjectMissing;
  public static String configurationEditorReloadEnabled;
  public static String configurationEditorReloadDisabled;

  public static String serverEditorGeneralSection;
  public static String serverEditorGeneralDescription;


  public static String projectConfDir;
  public static String serverEditorReloadableByDefault;
  public static String errorServerDirIsRoot;
  public static String serverEditorBrowseConfMessage;
  public static String configurationEditorWebModuleDialogTitleEdit;
  public static String configurationEditorWebModuleDialogTitleAdd;
  public static String configurationEditorWebModuleDialogProjects;
  public static String configurationEditorWebModuleDialogDocumentBase;
  public static String configurationEditorWebModuleDialogSelectDirectory;
  public static String configurationEditorWebModuleDialogPath;
  public static String configurationEditorWebModuleDialogReloadEnabled;

  public static String confirmCleanWorkDirTitle;
  public static String cleanServerStateChanging;
  public static String cleanModuleWorkDir;
  public static String cleanServerWorkDir;
  public static String cleanServerRunning;
  public static String cleanServerTask;

  public static String errorCouldNotCleanStateChange;
  public static String errorCouldNotCleanCantStop;
  public static String errorCouldNotCleanStopFailed;
  public static String errorCantIdentifyWebApp;
  public static String errorCantDeleteServerNotStopped;
  public static String errorErrorDuringClean;
  public static String errorErrorDuringCleanWasRunning;
  public static String errorCleanCantRestart;

  public static String cleanTerminateServerDialogTitle;
  public static String cleanTerminateServerDialogMessage;

  static {
    NLS.initializeMessages(ServerUIPlugin.PLUGIN_ID + ".Messages", Messages.class);
  }
}