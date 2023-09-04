/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
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

  private RuntimeComposite composite;

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
    composite = new RuntimeComposite(parent, wizard);
    return composite;
  }

  @Override
  public final void enter() {
    if (composite != null) {
      IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
      composite.setRuntime(runtime);
    }
  }

  @Override
  public final void exit() {
    IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
    if (runtime.validate(null).getSeverity() != IStatus.ERROR) {
      // runtime.getAdapter(IServerRuntime.class).getDefaults();
    }
  }
}