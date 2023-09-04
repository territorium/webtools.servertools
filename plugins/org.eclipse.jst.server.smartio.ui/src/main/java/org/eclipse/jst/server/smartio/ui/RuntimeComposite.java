/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jst.server.smartio.core.IServerRuntime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

/**
 * Wizard page to set the server install directory.
 */
class RuntimeComposite extends Composite {

  private final IWizardHandle wizard;

  private IServerRuntime      runtime;
  private IRuntimeWorkingCopy runtimeWC;


  private Text name;
  private Text installDir;

  /**
   * {@link RuntimeComposite} constructor comment.
   *
   * @param parent the parent composite
   * @param wizard the wizard handle
   */
  protected RuntimeComposite(Composite parent, IWizardHandle wizard) {
    super(parent, SWT.NONE);
    this.wizard = wizard;
    this.wizard.setTitle(Messages.wizardTitle);
    this.wizard.setDescription(Messages.wizardDescription);
    this.wizard.setImageDescriptor(ServerUIPlugin.getImageDescriptor(ServerUIPlugin.IMG_WIZ));

    createControl();
  }

  protected void setRuntime(IRuntimeWorkingCopy newRuntime) {
    if (newRuntime == null) {
      runtimeWC = null;
      runtime = null;
    } else {
      runtimeWC = newRuntime;
      runtime = (IServerRuntime) newRuntime.loadAdapter(IServerRuntime.class, null);
    }

    init();
    validate();
  }

  /**
   * Provide a wizard page to change the smart.IO installation directory.
   */
  private void createControl() {
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    setLayout(layout);
    setLayoutData(new GridData(GridData.FILL_BOTH));

    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ContextIds.RUNTIME_COMPOSITE);

    Label label = new Label(this, SWT.NONE);
    label.setText(Messages.runtimeName);
    GridData data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    name = new Text(this, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    name.setLayoutData(data);
    name.addModifyListener(e -> {
      runtimeWC.setName(name.getText());
      validate();
    });

    label = new Label(this, SWT.NONE);
    label.setText(Messages.installDir);
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    installDir = new Text(this, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    installDir.setLayoutData(data);
    installDir.addModifyListener(e -> {
      runtimeWC.setLocation(new Path(installDir.getText()));
      validate();
    });

    Button browse = SWTUtil.createButton(this, Messages.browse);
    browse.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent se) {
        DirectoryDialog dialog = new DirectoryDialog(RuntimeComposite.this.getShell());
        dialog.setMessage(Messages.selectInstallDir);
        dialog.setFilterPath(installDir.getText());
        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
          installDir.setText(selectedDirectory);
        }
      }
    });

    init();
    validate();

    Dialog.applyDialogFont(this);

    name.forceFocus();
  }

  private void init() {
    if ((name == null) || (runtime == null)) {
      return;
    }

    if (runtimeWC.getName() != null) {
      name.setText(runtimeWC.getName());
    } else {
      name.setText("");
    }

    if (runtimeWC.getLocation() != null) {
      installDir.setText(runtimeWC.getLocation().toOSString());
    } else {
      installDir.setText("");
    }
  }

  private void validate() {
    if (runtime == null) {
      wizard.setMessage("", IMessageProvider.ERROR);
      return;
    }

    IStatus status = runtimeWC.validate(null);
    if ((status == null) || status.isOK()) {
      wizard.setMessage(null, IMessageProvider.NONE);
    } else if (status.getSeverity() == IStatus.WARNING) {
      wizard.setMessage(status.getMessage(), IMessageProvider.WARNING);
    } else {
      wizard.setMessage(status.getMessage(), IMessageProvider.ERROR);
    }
    wizard.update();
  }
}
