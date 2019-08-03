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

package org.eclipse.jst.server.smartio.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

/**
 *
 */
public class RuntimeWizardFragment extends WizardFragment {

  private RuntimeComposite comp;

  @Override
  public final boolean hasComposite() {
    return true;
  }

  @Override
  public final boolean isComplete() {
    IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
    if (runtime == null) {
      return false;
    }
    IStatus status = runtime.validate(null);
    return ((status == null) || (status.getSeverity() != IStatus.ERROR));
  }

  @Override
  public final Composite createComposite(Composite parent, IWizardHandle wizard) {
    this.comp = new RuntimeComposite(parent, wizard);
    return this.comp;
  }

  @Override
  public final void enter() {
    if (this.comp != null) {
      IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
      this.comp.setRuntime(runtime);
    }
  }

  @Override
  public final void exit() {
    IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
    if (runtime.validate(null).getSeverity() != IStatus.ERROR) {
//      runtime.getAdapter(IServerRuntime.class).getDefaults();
    }
  }
}