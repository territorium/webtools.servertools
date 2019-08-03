/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * Progress Monitor utility.
 */
public class ProgressUtil {

  /**
   * ProgressUtil constructor comment.
   */
  private ProgressUtil() {}

  /**
   * Return a valid progress monitor.
   *
   * @param monitor
   */
  public static IProgressMonitor getMonitorFor(IProgressMonitor monitor) {
    return (monitor == null) ? new NullProgressMonitor() : monitor;
  }

  /**
   * Return a sub-progress monitor with the given amount on the current progress
   * monitor.
   *
   * @param monitor
   * @param ticks
   */
  public static IProgressMonitor getSubMonitorFor(IProgressMonitor monitor, int ticks) {
    if (monitor == null) {
      return new NullProgressMonitor();
    }
    return (monitor instanceof NullProgressMonitor) ? monitor : new SubProgressMonitor(monitor, ticks);
  }
}