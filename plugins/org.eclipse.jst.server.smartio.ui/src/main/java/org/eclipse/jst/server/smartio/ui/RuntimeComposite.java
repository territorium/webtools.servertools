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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jst.server.smartio.core.IServerRuntime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.core.internal.IInstallableRuntime;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.internal.wizard.fragment.LicenseWizardFragment;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import java.util.List;

/**
 * Wizard page to set the server install directory.
 */
class RuntimeComposite extends Composite {

  private IServerRuntime      runtime;
  private IRuntimeWorkingCopy runtimeWC;

  private final IWizardHandle wizard;

  private IInstallableRuntime installRuntime;
  private Job                 installRuntimeJob;
  private IJobChangeListener  jobListener;


  private Text   name;

  private Label  installLabel;
  private Text   installDir;
  private Button install;

  private Text   deployDir;
  private Button deployDirBrowse;

  /**
   * {@link RuntimeComposite} constructor comment.
   *
   * @param parent the parent composite
   * @param wizard the wizard handle
   */
  protected RuntimeComposite(Composite parent, IWizardHandle wizard) {
    super(parent, SWT.NONE);
    this.wizard = wizard;

    wizard.setTitle(Messages.wizardTitle);
    wizard.setDescription(Messages.wizardDescription);
    wizard.setImageDescriptor(ServerUIPlugin.getImageDescriptor(ServerUIPlugin.IMG_WIZ));

    createControl();
  }

  protected void setRuntime(IRuntimeWorkingCopy newRuntime) {
    if (newRuntime == null) {
      this.runtimeWC = null;
      this.runtime = null;
    } else {
      this.runtimeWC = newRuntime;
      this.runtime = (IServerRuntime) newRuntime.loadAdapter(IServerRuntime.class, null);
      this.deployDir.setText(runtime.getDeployDirectory());
    }

    if (this.runtimeWC == null) {
      this.installRuntime = null;
      this.install.setEnabled(false);
      this.installLabel.setText("");
    } else {
      this.installRuntime = ServerPlugin.findInstallableRuntime(this.runtimeWC.getRuntimeType().getId());
      if (this.installRuntime != null) {
        this.install.setEnabled(true);
        this.installLabel.setText(this.installRuntime.getName());
      }
    }

    init();
    validate();
  }

  @Override
  public void dispose() {
    super.dispose();
    if (this.installRuntimeJob != null) {
      this.installRuntimeJob.removeJobChangeListener(this.jobListener);
    }
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

    this.name = new Text(this, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    this.name.setLayoutData(data);
    this.name.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        RuntimeComposite.this.runtimeWC.setName(RuntimeComposite.this.name.getText());
        validate();
      }
    });

    label = new Label(this, SWT.NONE);
    label.setText(Messages.installDir);
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    this.installDir = new Text(this, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    this.installDir.setLayoutData(data);
    this.installDir.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        RuntimeComposite.this.runtimeWC.setLocation(new Path(RuntimeComposite.this.installDir.getText()));
        validate();
      }
    });

    Button browse = SWTUtil.createButton(this, Messages.browse);
    browse.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent se) {
        DirectoryDialog dialog = new DirectoryDialog(RuntimeComposite.this.getShell());
        dialog.setMessage(Messages.selectInstallDir);
        dialog.setFilterPath(RuntimeComposite.this.installDir.getText());
        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
          RuntimeComposite.this.installDir.setText(selectedDirectory);
        }
      }
    });

    this.installLabel = new Label(this, SWT.RIGHT);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalIndent = 10;
    this.installLabel.setLayoutData(data);

    this.install = SWTUtil.createButton(this, Messages.install);
    this.install.setEnabled(false);
    this.install.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent se) {
        String license = null;
        try {
          license = RuntimeComposite.this.installRuntime.getLicense(new NullProgressMonitor());
        } catch (CoreException e) {
          Trace.trace(Trace.SEVERE, "Error getting license", e);
        }
        TaskModel taskModel = new TaskModel();
        taskModel.putObject(LicenseWizardFragment.LICENSE, license);
        TaskWizard wizard2 = new TaskWizard(Messages.installDialogTitle, new WizardFragment() {

          @Override
          protected void createChildFragments(List list) {
            list.add(new LicenseWizardFragment());
          }
        }, taskModel);

        WizardDialog dialog2 = new WizardDialog(getShell(), wizard2);
        if (dialog2.open() == Window.CANCEL) {
          return;
        }

        DirectoryDialog dialog = new DirectoryDialog(RuntimeComposite.this.getShell());
        dialog.setMessage(Messages.selectInstallDir);
        dialog.setFilterPath(RuntimeComposite.this.installDir.getText());
        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
          // ir.install(new Path(selectedDirectory));
          final IPath installPath = new Path(selectedDirectory);
          RuntimeComposite.this.installRuntimeJob = new Job("Installing server runtime environment") {

            @Override
            public boolean belongsTo(Object family) {
              return ServerPlugin.PLUGIN_ID.equals(family);
            }

            @Override
            protected IStatus run(IProgressMonitor monitor) {
              try {
                RuntimeComposite.this.installRuntime.install(installPath, monitor);
              } catch (CoreException ce) {
                return ce.getStatus();
              }

              return Status.OK_STATUS;
            }
          };

          RuntimeComposite.this.installDir.setText(selectedDirectory);
          RuntimeComposite.this.jobListener = new JobChangeAdapter() {

            @Override
            public void done(IJobChangeEvent event) {
              RuntimeComposite.this.installRuntimeJob.removeJobChangeListener(this);
              RuntimeComposite.this.installRuntimeJob = null;
              Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                  if (!isDisposed()) {
                    validate();
                  }
                }
              });
            }
          };
          RuntimeComposite.this.installRuntimeJob.addJobChangeListener(RuntimeComposite.this.jobListener);
          RuntimeComposite.this.installRuntimeJob.schedule();
        }
      }
    });


    // deployment directory
    label = new Label(this, SWT.RIGHT);
    label.setText(Messages.serverEditorDeployDir);
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    this.deployDir = new Text(this, SWT.BORDER);
    this.deployDir.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    this.deployDir.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        RuntimeComposite.this.runtime.setDeployDirectory(RuntimeComposite.this.deployDir.getText());
        validate();
      }
    });

    this.deployDirBrowse = new Button(this, SWT.PUSH);
    this.deployDirBrowse.setText(Messages.editorBrowse);
    this.deployDirBrowse.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent se) {
        DirectoryDialog dialog = new DirectoryDialog(RuntimeComposite.this.deployDir.getShell());
        dialog.setMessage(Messages.serverEditorBrowseDeployMessage);
        dialog.setFilterPath(RuntimeComposite.this.deployDir.getText());
        String selectedDirectory = dialog.open();
        if ((selectedDirectory != null) && !selectedDirectory.equals(RuntimeComposite.this.deployDir.getText())) {
          RuntimeComposite.this.deployDir.setText(selectedDirectory);
          validate();
        }
      }
    });
    this.deployDirBrowse.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    init();
    validate();

    Dialog.applyDialogFont(this);

    this.name.forceFocus();
  }

  private void init() {
    if ((this.name == null) || (this.runtime == null)) {
      return;
    }

    if (this.runtimeWC.getName() != null) {
      this.name.setText(this.runtimeWC.getName());
    } else {
      this.name.setText("");
    }

    if (this.runtimeWC.getLocation() != null) {
      this.installDir.setText(this.runtimeWC.getLocation().toOSString());
    } else {
      this.installDir.setText("");
    }
  }

  private void validate() {
    if (this.runtime == null) {
      this.wizard.setMessage("", IMessageProvider.ERROR);
      return;
    }

    IStatus status = this.runtimeWC.validate(null);
    if ((status == null) || status.isOK()) {
      this.wizard.setMessage(null, IMessageProvider.NONE);
    } else if (status.getSeverity() == IStatus.WARNING) {
      this.wizard.setMessage(status.getMessage(), IMessageProvider.WARNING);
    } else {
      this.wizard.setMessage(status.getMessage(), IMessageProvider.ERROR);
    }
    this.wizard.update();
  }
}
