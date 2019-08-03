/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.smartio.core.Messages;
import org.eclipse.jst.server.smartio.core.ServerPlugin;
import org.eclipse.jst.server.smartio.core.ServerPlugin.Level;
import org.eclipse.osgi.util.NLS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Utility class with an assortment of useful file methods.
 */
public class FileUtil {

  // size of the buffer
  private static final int BUFFER = 10240;

  // the buffer
  private static byte[] buf = new byte[FileUtil.BUFFER];

  /**
   * FileUtil cannot be created. Use static methods.
   */
  private FileUtil() {
    super();
  }

  /**
   * Copy a file from a to b. Closes the input stream after use.
   *
   * @param in java.io.InputStream
   * @param to java.lang.String
   * @return a status
   */
  public static IStatus copyFile(InputStream in, String to) {
    OutputStream out = null;

    try {
      out = new FileOutputStream(to);

      int avail = in.read(FileUtil.buf);
      while (avail > 0) {
        out.write(FileUtil.buf, 0, avail);
        avail = in.read(FileUtil.buf);
      }
      return Status.OK_STATUS;
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Error copying file", e);
      return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorCopyingFile, new String[] { to, e.getLocalizedMessage() }), e);
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (Exception ex) {
        // ignore
      }
      try {
        if (out != null) {
          out.close();
        }
      } catch (Exception ex) {
        // ignore
      }
    }
  }

  /**
   * Copy a file from a to b.
   *
   * @param from java.lang.String
   * @param to java.lang.String
   * @return a status
   */
  public static IStatus copyFile(String from, String to) {
    try {
      return FileUtil.copyFile(new FileInputStream(from), to);
    } catch (Exception e) {
      ServerPlugin.log(Level.SEVERE, "Error copying file", e);
      return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorCopyingFile, new String[] { to, e.getLocalizedMessage() }), e);
    }
  }

  /**
   * Reads the from the specified InputStream and returns the result as a
   * String. Each line is terminated by &quot;\n&quot;. Returns whatever is read
   * regardless of any errors that occurs while reading.
   *
   * @param stream
   */
  public static String getFileContents(InputStream stream) throws IOException {
    BufferedReader br = null;
    StringBuffer sb = new StringBuffer();
    try {
      br = new BufferedReader(new InputStreamReader(stream));
      String temp = br.readLine();
      while (temp != null) {
        sb.append(temp).append("\n");
        temp = br.readLine();
      }
    } catch (Exception e) {
      ServerPlugin.log(Level.WARNING, "Could not load file contents.", e);
    } finally {
      if (br != null) {
        br.close();
      }
    }
    return sb.toString();
  }

  /**
   * Creates the specified deployment directory if it does not already exist. It
   * will include a default ROOT web application using the specified web.xml.
   *
   * @param deployDir path to deployment directory to create
   * @param webxml web.xml context to use for the ROOT web application.
   * @return result status of the operation
   */
  public static IStatus createDeploymentDirectory(IPath deployDir, String webxml) {
    if (ServerPlugin.isTraceEnabled()) {
      ServerPlugin.log(Level.FINER, "Creating deployment directory at " + deployDir.toOSString());
    }

    // TODO Add more error handling.
    File temp = deployDir.toFile();
    if (!temp.exists()) {
      temp.mkdirs();
    }

    IPath tempPath = deployDir.append("ROOT/WEB-INF");
    temp = tempPath.toFile();
    if (!temp.exists()) {
      temp.mkdirs();
    }
    temp = tempPath.append("web.xml").toFile();
    if (!temp.exists()) {
      FileWriter fw;
      try {
        fw = new FileWriter(temp);
        fw.write(webxml);
        fw.close();
      } catch (IOException e) {
        ServerPlugin.log(Level.WARNING, "Unable to create web.xml for ROOT context.", e);
      }
    }

    return Status.OK_STATUS;
  }
}
