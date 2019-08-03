/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui.editor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.smartio.core.WebModule;
import org.eclipse.jst.server.smartio.ui.ContextIds;
import org.eclipse.jst.server.smartio.ui.Messages;
import org.eclipse.jst.server.smartio.ui.Trace;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.ui.ServerUICore;

/**
 * Dialog to add or modify web modules.
 */
class WebModuleDialog extends Dialog {

  private final IServerAttributes attributes;

  private IModule                 module;
  private WebModule               webModule;
  private boolean                 isEdit;
  private boolean                 isProject;
  private Text                    docBase;


  private Table projTable;

  /**
   * WebModuleDialog constructor comment.
   *
   * @param shell
   * @param attributes
   * @param webModule a module
   */
  WebModuleDialog(Shell shell, IServerAttributes attributes, WebModule webModule) {
    super(shell);
    this.attributes = attributes;
    this.isEdit = true;
    this.webModule = webModule;
  }

  /**
   * WebModuleDialog constructor comment.
   *
   * @param shell
   * @param attributes
   * @param isProject
   */
  WebModuleDialog(Shell shell, IServerAttributes attributes, boolean isProject) {
    this(shell, attributes, new WebModule("/", "", null, true));
    this.isEdit = false;
    this.isProject = isProject;
  }


  /**
   * Gets the {@link IModule}.
   */
  protected final IModule getIModule() {
    return module;
  }

  /**
   *
   */
  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    if (this.isEdit) {
      newShell.setText(Messages.configurationEditorWebModuleDialogTitleEdit);
    } else {
      newShell.setText(Messages.configurationEditorWebModuleDialogTitleAdd);
    }
  }

  /**
   * Creates and returns the contents of the upper part of this dialog (above the button bar). <p>
   * The <code>Dialog</code> implementation of this framework method creates and returns a new
   * <code>Composite</code> with standard margins and spacing. Subclasses should override. </p>
   *
   * @param parent the parent composite to contain the dialog area
   * @return the dialog area control
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    // create a composite with standard margins and spacing
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    composite.setFont(parent.getFont());
    IWorkbenchHelpSystem whs = PlatformUI.getWorkbench().getHelpSystem();
    whs.setHelp(composite, ContextIds.CONFIGURATION_EDITOR_WEBMODULE_DIALOG);
    // add project field if we are adding a project
    if (!this.isEdit && this.isProject) {
      Label l = new Label(composite, SWT.NONE);
      l.setText(Messages.configurationEditorWebModuleDialogProjects);
      GridData data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
      l.setLayoutData(data);

      this.projTable = new Table(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE);
      data = new GridData();
      data.widthHint = 150;
      data.heightHint = 75;
      this.projTable.setLayoutData(data);
      whs.setHelp(this.projTable, ContextIds.CONFIGURATION_EDITOR_WEBMODULE_DIALOG_PROJECT);

      // fill table with web module projects
      ILabelProvider labelProvider = ServerUICore.getLabelProvider();
      IModule[] modules = ServerUtil.getModules(this.attributes.getServerType().getRuntimeType().getModuleTypes());
      if (modules != null) {
        int size = modules.length;
        for (int i = 0; i < size; i++) {
          IModule module3 = modules[i];
          if ("jst.web".equals(module3.getModuleType().getId())) {
            IStatus status = this.attributes.canModifyModules(new IModule[] { module3 }, null, null);
            if ((status != null) && status.isOK()) {
              TableItem item = new TableItem(this.projTable, SWT.NONE);
              item.setText(0, labelProvider.getText(module3));
              item.setImage(0, labelProvider.getImage(module3));
              item.setData(module3);
            }
          }
        }
      }
      labelProvider.dispose();
      new Label(composite, SWT.NONE).setText(" ");
    }

    new Label(composite, SWT.NONE).setText(Messages.configurationEditorWebModuleDialogDocumentBase);
    this.docBase = new Text(composite, SWT.BORDER);
    GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    this.docBase.setLayoutData(data);
    this.docBase.setText(this.webModule.getDocumentBase());
    whs.setHelp(this.docBase, ContextIds.CONFIGURATION_EDITOR_WEBMODULE_DIALOG_DOCBASE);

    // disable document base for project modules
    if (this.isProject || ((this.webModule.getMemento() != null) && (this.webModule.getMemento().length() > 0))) {
      this.docBase.setEditable(false);
    } else {
      this.docBase.addModifyListener(new ModifyListener() {

        @Override
        public void modifyText(ModifyEvent e) {
          WebModuleDialog.this.webModule =
              new WebModule(WebModuleDialog.this.webModule.getPath(), WebModuleDialog.this.docBase.getText(),
                  WebModuleDialog.this.webModule.getMemento(), WebModuleDialog.this.webModule.isReloadable());
          validate();
        }
      });
    }

    if (this.isEdit || this.isProject) {
      new Label(composite, SWT.NONE).setText(" ");
    } else {
      Button browse = new Button(composite, SWT.NONE);
      browse.setText(Messages.browse);
      browse.addSelectionListener(new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent se) {
          try {
            DirectoryDialog dialog = new DirectoryDialog(getShell());
            dialog.setMessage(Messages.configurationEditorWebModuleDialogSelectDirectory);
            String selectedDirectory = dialog.open();
            if (selectedDirectory != null) {
              WebModuleDialog.this.docBase.setText(selectedDirectory);
            }
          } catch (Exception e) {
            Trace.trace(Trace.SEVERE, "Error browsing", e);
          }
        }
      });
    }

    // path (context-root)
    new Label(composite, SWT.NONE).setText(Messages.configurationEditorWebModuleDialogPath);
    final Text path = new Text(composite, SWT.BORDER);
    data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    data.widthHint = 150;
    path.setLayoutData(data);
    path.setText(this.webModule.getPath());
    /*
     * if (module.getMemento() != null && module.getMemento().length() > 0) path.setEditable(false);
     * else
     */
    path.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        WebModuleDialog.this.webModule = new WebModule(path.getText(), WebModuleDialog.this.webModule.getDocumentBase(),
            WebModuleDialog.this.webModule.getMemento(), WebModuleDialog.this.webModule.isReloadable());
      }
    });
    whs.setHelp(path, ContextIds.CONFIGURATION_EDITOR_WEBMODULE_DIALOG_PATH);

    new Label(composite, SWT.NONE).setText("");

    if (!this.isProject) {
      // auto reload
      new Label(composite, SWT.NONE).setText("");
      final Button reloadable = new Button(composite, SWT.CHECK);
      reloadable.setText(Messages.configurationEditorWebModuleDialogReloadEnabled);
      data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
      reloadable.setLayoutData(data);
      reloadable.setSelection(this.webModule.isReloadable());
      reloadable.addSelectionListener(new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent e) {
          WebModuleDialog.this.webModule =
              new WebModule(WebModuleDialog.this.webModule.getPath(), WebModuleDialog.this.webModule.getDocumentBase(),
                  WebModuleDialog.this.webModule.getMemento(), reloadable.getSelection());
        }
      });
      whs.setHelp(reloadable, ContextIds.CONFIGURATION_EDITOR_WEBMODULE_DIALOG_RELOAD);
    }

    if (!this.isEdit && this.isProject) {
      this.projTable.addSelectionListener(new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent event) {
          try {
            IModule module3 = (IModule) WebModuleDialog.this.projTable.getSelection()[0].getData();
            IWebModule module2 = (IWebModule) module3.loadAdapter(IWebModule.class, null);
            String contextRoot = module2.getContextRoot();
            if ((contextRoot != null) && !contextRoot.startsWith("/") && (contextRoot.length() > 0)) {
              contextRoot = "/" + contextRoot;
            }
            WebModuleDialog.this.webModule = new WebModule(contextRoot, module3.getName(), module3.getId(),
                WebModuleDialog.this.webModule.isReloadable());
            WebModuleDialog.this.docBase.setText(module3.getName());
            path.setText(contextRoot);
            WebModuleDialog.this.module = module3;
          } catch (Exception e) {
            // ignore
          }
          validate();
        }
      });
      new Label(composite, SWT.NONE).setText("");
    }

    Dialog.applyDialogFont(composite);
    return composite;
  }

  @Override
  protected Control createButtonBar(Composite parent) {
    Control control = super.createButtonBar(parent);
    validate();

    return control;
  }

  private void validate() {
    boolean ok = true;
    if ((this.webModule.getDocumentBase() == null) || (this.webModule.getDocumentBase().length() < 1)) {
      ok = false;
    }

    getButton(IDialogConstants.OK_ID).setEnabled(ok);
  }

  /**
   * Return the mime mapping.
   *
   * @return org.eclipse.jst.server.smartio.WebModule
   */
  public WebModule getWebModule() {
    return this.webModule;
  }
}
