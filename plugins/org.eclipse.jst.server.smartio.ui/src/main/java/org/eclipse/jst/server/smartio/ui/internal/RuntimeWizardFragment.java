/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jst.server.smartio.core.internal.ServerPlugin;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

/**
 *
 */
public class RuntimeWizardFragment extends WizardFragment {

  protected RuntimeComposite comp;

  public RuntimeWizardFragment() {
    // do nothing
  }

  @Override
  public boolean hasComposite() {
    return true;
  }

  @Override
  public boolean isComplete() {
    IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);

    if (runtime == null) {
      return false;
    }
    IStatus status = runtime.validate(null);
    return ((status == null) || (status.getSeverity() != IStatus.ERROR));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.server.ui.task.WizardFragment#createComposite()
   */
  @Override
  public Composite createComposite(Composite parent, IWizardHandle wizard) {
    comp = new RuntimeComposite(parent, wizard);
    return comp;
  }

  @Override
  public void enter() {
    if (comp != null) {
      IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
      comp.setRuntime(runtime);
    }
  }

  @Override
  public void exit() {
    IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
    IPath path = runtime.getLocation();
    if (runtime.validate(null).getSeverity() != IStatus.ERROR) {
      ServerPlugin.setPreference("location" + runtime.getRuntimeType().getId(), path.toString());
    }
  }
}