/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class with an assortment of useful file methods.
 */
class FileUtil {

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
  static IStatus copyFile(InputStream in, String to) {
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
      Trace.trace(Trace.SEVERE, "Error copying file", e);
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
  static IStatus copyFile(String from, String to) {
    try {
      return FileUtil.copyFile(new FileInputStream(from), to);
    } catch (Exception e) {
      Trace.trace(Trace.SEVERE, "Error copying file", e);
      return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0,
          NLS.bind(Messages.errorCopyingFile, new String[] { to, e.getLocalizedMessage() }), e);
    }
  }
}
