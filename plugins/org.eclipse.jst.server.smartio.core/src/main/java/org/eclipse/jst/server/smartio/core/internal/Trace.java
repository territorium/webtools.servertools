/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class to route trace output.
 */
public class Trace {

  public static final byte              WARNING      = 1;
  public static final byte              SEVERE       = 2;
  static final byte                     FINEST       = 3;
  static final byte                     FINER        = 4;

  private static final String[]         levelNames   =
      new String[] { "CONFIG   ", "WARNING  ", "SEVERE   ", "FINER    ", "FINEST   " };
  private static final String           spacer       = "                                   ";

  private static final SimpleDateFormat sdf          = new SimpleDateFormat("dd/MM/yy HH:mm.ss.SSS");

  private static int                    pluginLength = -1;

  /**
   * Trace constructor comment.
   */
  private Trace() {
    super();
  }

  /**
   * Trace the given text.
   *
   * @param level the trace level
   * @param s a message
   */
  public static void trace(byte level, String s) {
    Trace.trace(level, s, null);
  }

  /**
   * Trace the given message and exception.
   *
   * @param level the trace level
   * @param s a message
   * @param t a throwable
   */
  public static void trace(byte level, String s, Throwable t) {
    if (!ServerPlugin.getInstance().isDebugging()) {
      return;
    }

    /*
     * System.out.println(ServerPlugin.PLUGIN_ID + " " + s); if (t != null) t.printStackTrace();
     */
    Trace.trace(ServerPlugin.PLUGIN_ID, level, s, t);
  }

  /**
   * Trace the given message and exception.
   *
   * @param level a trace level
   * @param s a message
   * @param t a throwable
   */
  private static void trace(String pluginId, int level, String s, Throwable t) {
    if ((pluginId == null) || (s == null)) {
      return;
    }

    if (!ServerPlugin.getInstance().isDebugging()) {
      return;
    }

    StringBuffer sb = new StringBuffer(pluginId);
    if (pluginId.length() > Trace.pluginLength) {
      Trace.pluginLength = pluginId.length();
    } else if (pluginId.length() < Trace.pluginLength) {
      sb.append(Trace.spacer.substring(0, Trace.pluginLength - pluginId.length()));
    }
    sb.append(" ");
    sb.append(Trace.levelNames[level]);
    sb.append(" ");
    sb.append(Trace.sdf.format(new Date()));
    sb.append(" ");
    sb.append(s);
    // Platform.getDebugOption(ServerCore.PLUGIN_ID + "/" + "resources");

    System.out.println(sb.toString());
    if (t != null) {
      t.printStackTrace();
    }
  }

  /**
   * Gets state of debug flag for the plug-in.
   * 
   * @return true if tracing is enabled
   */
  static boolean isTraceEnabled() {
    return ServerPlugin.getInstance().isDebugging();
  }
}
