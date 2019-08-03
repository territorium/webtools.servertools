/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

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
  public static String errorJRE;
  public static String errorJREServer10;
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
  public static String publishContextConfigTask;
  public static String savingContextConfigTask;
  public static String checkingContextTask;
  public static String errorPublishConfiguration;
  public static String errorPublishServer;
  public static String cleanupServerTask;
  public static String detectingRemovedProjects;
  public static String deletingContextFilesTask;
  public static String deletingContextFile;
  public static String deletedContextFile;
  public static String errorCouldNotDeleteContextFile;
  public static String errorCleanupServer;
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
  public static String errorCouldNotLoadContextXml;
  public static String errorXMLServiceNotFound;
  public static String errorXMLNoService;
  public static String errorXMLEngineNotFound;
  public static String errorXMLHostNotFound;
  public static String errorXMLContextNotFoundPath;
  public static String errorXMLNullContextArg;
  public static String errorPublishContextNotFound;
  public static String errorPublishCouldNotRemoveModule;
  public static String errorPublishPathDup;
  public static String errorPublishPathConflict;
  public static String errorPublishPathMissing;

  public static String configurationEditorActionModifyPort;
  public static String configurationEditorActionModifyMimeMapping;
  public static String configurationEditorActionAddMimeMapping;
  public static String configurationEditorActionAddWebModule;
  public static String configurationEditorActionModifyWebModule;
  public static String configurationEditorActionRemoveMimeMapping;
  public static String configurationEditorActionRemoveWebModule;
  public static String serverEditorActionSetDebugMode;
  public static String serverEditorActionSetSecure;
  public static String serverEditorActionSetServerDirectory;
  public static String serverEditorActionSetDeployDirectory;
  public static String serverEditorActionSetServeWithoutPublish;
  public static String serverEidtorActionSetSeparateContextFiles;
  public static String serverEditorActionSetModulesReloadableByDefault;

  static {
    NLS.initializeMessages(ServerPlugin.PLUGIN_ID + ".internal.Messages", Messages.class);
  }
}