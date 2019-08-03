/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * Progress Monitor utility.
 */
class ProgressUtil {

  /**
   * ProgressUtil constructor comment.
   */
  private ProgressUtil() {
    super();
  }

  /**
   * Return a valid progress monitor.
   *
   * @param monitor org.eclipse.core.runtime.IProgressMonitor
   * @return org.eclipse.core.runtime.IProgressMonitor
   */
  static IProgressMonitor getMonitorFor(IProgressMonitor monitor) {
    if (monitor == null) {
      return new NullProgressMonitor();
    }
    return monitor;
  }

  /**
   * Return a sub-progress monitor with the given amount on the current progress monitor.
   *
   * @param monitor org.eclipse.core.runtime.IProgressMonitor
   * @param ticks int
   * @return org.eclipse.core.runtime.IProgressMonitor
   */
  static IProgressMonitor getSubMonitorFor(IProgressMonitor monitor, int ticks) {
    if (monitor == null) {
      return new NullProgressMonitor();
    }
    if (monitor instanceof NullProgressMonitor) {
      return monitor;
    }
    return new SubProgressMonitor(monitor, ticks);
  }
}