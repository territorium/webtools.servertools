/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui;

/**
 * Helper class to route trace output.
 */
public class Trace {

  static final byte        WARNING = 1;
  public static final byte SEVERE  = 2;


  /**
   * Trace constructor comment.
   */
  private Trace() {
    super();
  }


  /**
   * Trace the given message and exception.
   *
   * @param level the trace level
   * @param s a message
   * @param t a throwable
   */
  public static void trace(byte level, String s, Throwable t) {
    if (!ServerUIPlugin.getInstance().isDebugging()) {
      return;
    }

    System.out.println(ServerUIPlugin.PLUGIN_ID + " " + s);
    if (t != null) {
      t.printStackTrace();
    }
  }
}
