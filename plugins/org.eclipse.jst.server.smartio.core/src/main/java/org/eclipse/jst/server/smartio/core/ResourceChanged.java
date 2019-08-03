/*
 * Copyright (c) 2001-2019 Territorium Online Srl / TOL GmbH. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as defined in and that are
 * subject to the Territorium Online License Version 1.0. You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at http://www.tol.info/license/
 * and read it before using this file.
 *
 * The Original Code and all software distributed under the License are distributed on an 'AS IS'
 * basis, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, AND TERRITORIUM ONLINE HEREBY
 * DISCLAIMS ALL SUCH WARRANTIES, INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT. Please see the License for
 * the specific language governing rights and limitations under the License.
 */

package org.eclipse.jst.server.smartio.core;

import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;

/**
 * The {@link ResourceChanged} class.
 */
public class ResourceChanged {

  private final IModuleResourceDelta[] delta;

  /**
   * Constructs an instance of {@link ResourceChanged}.
   *
   * @param delta
   */
  public ResourceChanged(IModuleResourceDelta[] delta) {
    this.delta = delta;
  }


  public final boolean containsNonResourceChange() {
    return containsNonResourceChange(delta);
  }


  private boolean containsNonResourceChange(IModuleResourceDelta[] delta) {
    int size = delta.length;
    for (int i = 0; i < size; i++) {
      IModuleResourceDelta d = delta[i];
      if (d.getModuleRelativePath().segmentCount() == 0) {
        if ("WEB-INF".equals(d.getModuleResource().getName())) {
          return containsNonResourceChange(d.getAffectedChildren());
        }
        continue;
      }
      if (d.getModuleResource() instanceof IModuleFile) {
        return true;
      }

      boolean bool = containsNonAddChange(d.getAffectedChildren());
      if (bool) {
        return true;
      }
    }
    return false;
  }

  private boolean containsNonAddChange(IModuleResourceDelta[] delta) {
    if (delta == null) {
      return false;
    }
    int size = delta.length;
    for (int i = 0; i < size; i++) {
      IModuleResourceDelta d = delta[i];
      if (d.getModuleResource() instanceof IModuleFile) {
        if (d.getKind() != IModuleResourceDelta.ADDED) {
          return true;
        }
      }

      boolean bool = containsNonAddChange(d.getAffectedChildren());
      if (bool) {
        return true;
      }
    }
    return false;
  }
}
