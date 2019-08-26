/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui.internal;

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
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jst.server.smartio.core.internal.IServerRuntimeWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Wizard page to set the server install directory.
 */
class RuntimeComposite extends Composite {

  private IRuntimeWorkingCopy       runtimeWC;
  private IServerRuntimeWorkingCopy runtime;

  private final IWizardHandle       wizard;

  private Text                      installDir;
  private Text                      name;
  private Combo                     combo;
  private List<IVMInstall>          installedJREs;
  private String[]                  jreNames;
  private IInstallableRuntime       ir;
  private Job                       installRuntimeJob;
  private IJobChangeListener        jobListener;
  private Label                     installLabel;
  private Button                    install;

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
      runtimeWC = null;
      runtime = null;
    } else {
      runtimeWC = newRuntime;
      runtime = (IServerRuntimeWorkingCopy) newRuntime.loadAdapter(IServerRuntimeWorkingCopy.class, null);
    }

    if (runtimeWC == null) {
      ir = null;
      install.setEnabled(false);
      installLabel.setText("");
    } else {
      ir = ServerPlugin.findInstallableRuntime(runtimeWC.getRuntimeType().getId());
      if (ir != null) {
        install.setEnabled(true);
        installLabel.setText(ir.getName());
      }
    }

    init();
    validate();
  }

  @Override
  public void dispose() {
    super.dispose();
    if (installRuntimeJob != null) {
      installRuntimeJob.removeJobChangeListener(jobListener);
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

    name = new Text(this, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    name.setLayoutData(data);
    name.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        runtimeWC.setName(name.getText());
        validate();
      }
    });

    label = new Label(this, SWT.NONE);
    label.setText(Messages.installDir);
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    installDir = new Text(this, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    installDir.setLayoutData(data);
    installDir.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        runtimeWC.setLocation(new Path(installDir.getText()));
        validate();
      }
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

    installLabel = new Label(this, SWT.RIGHT);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalIndent = 10;
    installLabel.setLayoutData(data);

    install = SWTUtil.createButton(this, Messages.install);
    install.setEnabled(false);
    install.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent se) {
        String license = null;
        try {
          license = ir.getLicense(new NullProgressMonitor());
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
        dialog.setFilterPath(installDir.getText());
        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
          // ir.install(new Path(selectedDirectory));
          final IPath installPath = new Path(selectedDirectory);
          installRuntimeJob = new Job("Installing server runtime environment") {

            @Override
            public boolean belongsTo(Object family) {
              return ServerPlugin.PLUGIN_ID.equals(family);
            }

            @Override
            protected IStatus run(IProgressMonitor monitor) {
              try {
                ir.install(installPath, monitor);
              } catch (CoreException ce) {
                return ce.getStatus();
              }

              return Status.OK_STATUS;
            }
          };

          installDir.setText(selectedDirectory);
          jobListener = new JobChangeAdapter() {

            @Override
            public void done(IJobChangeEvent event) {
              installRuntimeJob.removeJobChangeListener(this);
              installRuntimeJob = null;
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
          installRuntimeJob.addJobChangeListener(jobListener);
          installRuntimeJob.schedule();
        }
      }
    });

    updateJREs();

    // JDK location
    label = new Label(this, SWT.NONE);
    label.setText(Messages.installedJRE);
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    combo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
    combo.setItems(jreNames);
    data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    combo.setLayoutData(data);

    combo.addSelectionListener(new SelectionListener() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        int sel = combo.getSelectionIndex();
        IVMInstall vmInstall = null;
        if (sel > 0) {
          vmInstall = installedJREs.get(sel - 1);
        }

        runtime.setVMInstall(vmInstall);
        validate();
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }
    });

    Button button = SWTUtil.createButton(this, Messages.installedJREs);
    button.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        String currentVM = combo.getText();
        if (showPreferencePage()) {
          updateJREs();
          combo.setItems(jreNames);
          combo.setText(currentVM);
          if (combo.getSelectionIndex() == -1) {
            combo.select(0);
          }
          validate();
        }
      }
    });

    init();
    validate();

    Dialog.applyDialogFont(this);

    name.forceFocus();
  }

  private void updateJREs() {
    // get all installed JVMs
    installedJREs = new ArrayList<>();
    IVMInstallType[] vmInstallTypes = JavaRuntime.getVMInstallTypes();
    int size = vmInstallTypes.length;
    for (int i = 0; i < size; i++) {
      IVMInstall[] vmInstalls = vmInstallTypes[i].getVMInstalls();
      int size2 = vmInstalls.length;
      for (int j = 0; j < size2; j++) {
        installedJREs.add(vmInstalls[j]);
      }
    }

    // get names
    size = installedJREs.size();
    jreNames = new String[size + 1];
    jreNames[0] = Messages.runtimeDefaultJRE;
    for (int i = 0; i < size; i++) {
      IVMInstall vmInstall = installedJREs.get(i);
      jreNames[i + 1] = vmInstall.getName();
    }
  }

  private boolean showPreferencePage() {
    String id = "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage";

    // should be using the following API, but it only allows a single preference page instance.
    // see bug 168211 for details
    // PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), id, new
    // String[] { id }, null);
    // return (dialog.open() == Window.OK);

    PreferenceManager manager = PlatformUI.getWorkbench().getPreferenceManager();
    IPreferenceNode node = manager.find("org.eclipse.jdt.ui.preferences.JavaBasePreferencePage").findSubNode(id);
    PreferenceManager manager2 = new PreferenceManager();
    manager2.addToRoot(node);
    PreferenceDialog dialog = new PreferenceDialog(getShell(), manager2);
    dialog.create();
    return (dialog.open() == Window.OK);
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

    // set selection
    if (runtime.isUsingDefaultJRE()) {
      combo.select(0);
    } else {
      boolean found = false;
      int size = installedJREs.size();
      for (int i = 0; i < size; i++) {
        IVMInstall vmInstall = installedJREs.get(i);
        if (vmInstall.equals(runtime.getVMInstall())) {
          combo.select(i + 1);
          found = true;
        }
      }
      if (!found) {
        combo.select(0);
      }
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
