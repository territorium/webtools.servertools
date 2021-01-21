/**********************************************************************
 * Copyright (c) 2007, 2017 SAS Institute, Inc and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: SAS Institute, Inc - Initial API and implementation
 **********************************************************************/

package org.eclipse.jst.server.smartio.core.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.smartio.core.Messages;
import org.eclipse.jst.server.smartio.core.ServerPlugin;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for methods that are used by more that one version of Tomcat. Use of these methods
 * makes it clear that more than one version will be impacted by changes.
 *
 */
public class VersionHelper {

  /**
   * Map of server type ID to expected version string fragment for version checking.
   */
  private static final Map<IPath, String>  versionPath         = new ConcurrentHashMap<>();
  private static final Map<IPath, Long>    versionLastModified = new ConcurrentHashMap<>();


  private static volatile long lastCheck = 0;

  /**
   * Checks if the version of Tomcat installed at the specified location matches the specified
   * server type. The return status indicates if the version matches or not, or can't be determined.
   *
   * Because this can get called repeatedly for certain operations, some caching is provided. The
   * first check for an installPath in the current Eclipse session will query the catalina.jar for
   * its version. Any additional checks will compare the catalina.jar's time stamp and will use the
   * previously cached version if it didn't change. Additional checks that occur within 2 seconds of
   * the last check, regardless of Tomcat version, don't bother with checking the jar time stamp and
   * just use the cached values.
   *
   * @param installPath
   * @param serverType
   */
  public static IStatus checkVersion(IPath installPath, String serverType) {
    String serverVersion = null;
    IPath versionPath = null;
    File versionFile = null;

    if (ServerPlugin.SERVER_10.equals(serverType)) {
      versionPath = installPath.append("version");
      versionFile = versionPath.toFile();
      // If jar is not at expected location, try alternate location
      if (!versionFile.exists()) {
        versionPath = null;
      }
    }
    if (versionPath != null) {
      serverVersion = VersionHelper.versionPath.get(versionPath);
      long checkTime = System.currentTimeMillis();
      // Use some logic to try to determine if a cached value is stale
      // If last check was more than a couple of seconds ago, check the jar time
      // stamp
      if ((serverVersion != null) && ((checkTime - VersionHelper.lastCheck) > 2000)) {
        long curLastModified = versionFile.lastModified();
        Long oldLastModified = VersionHelper.versionLastModified.get(versionPath);
        // If jar time stamps differ, discard the cached version string
        if ((oldLastModified == null) || (curLastModified != oldLastModified.longValue())) {
          serverVersion = null;
        }
      }
      VersionHelper.lastCheck = checkTime;
      // If a version string needs to be acquired
      if (serverVersion == null) {
        Properties props = new Properties();
        try {
          props.load(new FileInputStream(installPath.append("version").toFile()));
          serverVersion = props.getProperty("Version-Number");
          VersionHelper.versionPath.put(versionPath, serverVersion);
          VersionHelper.versionLastModified.put(versionPath, new Long(versionFile.lastModified()));
        } catch (IOException e) {}
      }
      if (serverVersion != null) {
        // If we have an actual version, test the version
        if (serverVersion.length() > 0) {
//          String versionTest = VersionHelper.versionStringMap.get(serverType);
//          if ((versionTest != null) && !serverVersion.startsWith(versionTest)) {
//            return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, NLS.bind(Messages.errorInstallDirWrongVersion2,
//                serverVersion, versionTest.substring(0, versionTest.length() - 1)));
//          }
        }
        // Else we have an unknown version
        else {
          return Status.CANCEL_STATUS;
        }
      } else {
        // Cache blank version string for unknown version
        VersionHelper.versionPath.put(versionPath, "");
        VersionHelper.versionLastModified.put(versionPath, new Long(versionFile.lastModified()));
        return Status.CANCEL_STATUS;
      }
    }
    // Else server type is not supported or jar doesn't exist
    else {
      return Status.CANCEL_STATUS;
    }

    return Status.OK_STATUS;
  }

  /**
   * Checks if the version of Tomcat installed at the specified location matches the specified
   * server type. The return status indicates if the version matches or not, or can't be determined.
   *
   * Because this can get called repeatedly for certain operations, some caching is provided. The
   * first check for an installPath in the current Eclipse session will query the catalina.jar for
   * its version. Any additional checks will compare the catalina.jar's time stamp and will use the
   * previously cached version if it didn't change. Additional checks that occur within 2 seconds of
   * the last check, regardless of Tomcat version, don't bother with checking the jar time stamp and
   * just use the cached values.
   *
   * @param installPath
   * @param serverType
   */

  public static String getVersion(IPath installPath, String serverType) {
    // If not found, we need to initialize the data for this server
    IStatus result = VersionHelper.checkVersion(installPath, serverType);
    // If successful, search again
    if (result.isOK()) {
      for (Map.Entry<IPath, String> entry : VersionHelper.versionPath.entrySet()) {
        IPath versionPath = entry.getKey();
        if (installPath.isPrefixOf(versionPath)) {
          return entry.getValue();
        }
      }
    }
    // Return unknown version
    return "";
  }
}
