/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui.editor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.smartio.core.IServerConfiguration;
import org.eclipse.jst.server.smartio.core.IServerWrapper;
import org.eclipse.jst.server.smartio.core.WebModule;
import org.eclipse.jst.server.smartio.core.command.AddModuleCommand;
import org.eclipse.jst.server.smartio.core.command.AddWebModuleCommand;
import org.eclipse.jst.server.smartio.core.command.ModifyWebModuleCommand;
import org.eclipse.jst.server.smartio.core.command.RemoveModuleCommand;
import org.eclipse.jst.server.smartio.core.command.RemoveWebModuleCommand;
import org.eclipse.jst.server.smartio.ui.ContextIds;
import org.eclipse.jst.server.smartio.ui.Messages;
import org.eclipse.jst.server.smartio.ui.ServerUIPlugin;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.ui.ServerUICore;
import org.eclipse.wst.server.ui.editor.ServerEditorPart;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * smart.IO configuration web module editor page.
 */
public class ConfigurationWebModuleEditorPart extends ServerEditorPart {

  private IServerConfiguration   configuration;

  private Table                  webAppTable;
  private int                    selection = -1;
  private Button                 addProject;
  private Button                 addExtProject;
  private Button                 remove;
  private Button                 edit;
  private PropertyChangeListener listener;

  /**
   *
   */
  protected void addChangeListener() {
    this.listener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (IServerConfiguration.WEB_MODULE_PROPERTY_MODIFY.equals(event.getPropertyName())) {
          initialize();
        } else if (IServerConfiguration.WEB_MODULE_PROPERTY_ADD.equals(event.getPropertyName())) {
          initialize();
        } else if (IServerConfiguration.WEB_MODULE_PROPERTY_REMOVE.equals(event.getPropertyName())) {
          initialize();
        }
      }
    };
    this.configuration.addPropertyChangeListener(this.listener);
  }

  /**
   * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    FormToolkit toolkit = getFormToolkit(parent.getDisplay());

    ScrolledForm form = toolkit.createScrolledForm(parent);
    toolkit.decorateFormHeading(form.getForm());
    form.setText(Messages.configurationEditorWebModulesPageTitle);
    form.setImage(ServerUIPlugin.getImage(ServerUIPlugin.IMG_WEB_MODULE));
    GridLayout layout = new GridLayout();
    layout.marginTop = 6;
    layout.marginLeft = 6;
    form.getBody().setLayout(layout);

    Section section = toolkit.createSection(form.getBody(), ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
    section.setText(Messages.configurationEditorWebModulesSection);
    section.setDescription(Messages.configurationEditorWebModulesDescription);
    section.setLayoutData(new GridData(GridData.FILL_BOTH));

    Composite composite = toolkit.createComposite(section);
    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 5;
    layout.marginWidth = 10;
    layout.verticalSpacing = 5;
    layout.horizontalSpacing = 15;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    IWorkbenchHelpSystem whs = PlatformUI.getWorkbench().getHelpSystem();
    whs.setHelp(composite, ContextIds.CONFIGURATION_EDITOR_WEBMODULES);
    toolkit.paintBordersFor(composite);
    section.setClient(composite);

    this.webAppTable = toolkit.createTable(composite, SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
    this.webAppTable.setHeaderVisible(true);
    this.webAppTable.setLinesVisible(true);
    whs.setHelp(this.webAppTable, ContextIds.CONFIGURATION_EDITOR_WEBMODULES_LIST);
    // toolkit.paintBordersFor(webAppTable);

    TableLayout tableLayout = new TableLayout();

    TableColumn col = new TableColumn(this.webAppTable, SWT.NONE);
    col.setText(Messages.configurationEditorPathColumn);
    ColumnWeightData colData = new ColumnWeightData(8, 85, true);
    tableLayout.addColumnData(colData);

    TableColumn col2 = new TableColumn(this.webAppTable, SWT.NONE);
    col2.setText(Messages.configurationEditorDocBaseColumn);
    colData = new ColumnWeightData(13, 135, true);
    tableLayout.addColumnData(colData);

    TableColumn col3 = new TableColumn(this.webAppTable, SWT.NONE);
    col3.setText(Messages.configurationEditorProjectColumn);
    colData = new ColumnWeightData(8, 85, true);
    tableLayout.addColumnData(colData);

    TableColumn col4 = new TableColumn(this.webAppTable, SWT.NONE);
    col4.setText(Messages.configurationEditorReloadColumn);
    colData = new ColumnWeightData(7, 75, true);
    tableLayout.addColumnData(colData);

    this.webAppTable.setLayout(tableLayout);

    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.widthHint = 450;
    data.heightHint = 120;
    this.webAppTable.setLayoutData(data);
    this.webAppTable.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        selectWebApp();
      }
    });

    Composite rightPanel = toolkit.createComposite(composite);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    rightPanel.setLayout(layout);
    data = new GridData();
    rightPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
    // toolkit.paintBordersFor(rightPanel);

    // buttons still to add:
    // add project, add external module, remove module
    this.addProject = toolkit.createButton(rightPanel, Messages.configurationEditorAddProjectModule, SWT.PUSH);
    data = new GridData(GridData.FILL_HORIZONTAL);
    this.addProject.setLayoutData(data);
    whs.setHelp(this.addProject, ContextIds.CONFIGURATION_EDITOR_WEBMODULES_ADD_PROJECT);

    // disable the add project module button if there are no
    // web projects in the workbench
    if (!canAddWebModule()) {
      this.addProject.setEnabled(false);
    } else {
      this.addProject.addSelectionListener(new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent e) {
          WebModuleDialog dialog = new WebModuleDialog(getEditorSite().getShell(), getServer(), true);
          dialog.open();
          if (dialog.getReturnCode() == IDialogConstants.OK_ID) {
            execute(new AddModuleCommand(getServer(), dialog.getIModule()));
          }
        }
      });
    }

    this.addExtProject = toolkit.createButton(rightPanel, Messages.configurationEditorAddExternalModule, SWT.PUSH);
    data = new GridData(GridData.FILL_HORIZONTAL);
    this.addExtProject.setLayoutData(data);
    this.addExtProject.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        WebModuleDialog dialog = new WebModuleDialog(getEditorSite().getShell(), getServer(), false);
        dialog.open();
        if (dialog.getReturnCode() == IDialogConstants.OK_ID) {
          execute(new AddWebModuleCommand(ConfigurationWebModuleEditorPart.this.configuration, dialog.getWebModule()));
        }
      }
    });
    whs.setHelp(this.addExtProject, ContextIds.CONFIGURATION_EDITOR_WEBMODULES_ADD_EXTERNAL);

    this.edit = toolkit.createButton(rightPanel, Messages.editorEdit, SWT.PUSH);
    data = new GridData(GridData.FILL_HORIZONTAL);
    this.edit.setLayoutData(data);
    this.edit.setEnabled(false);
    this.edit.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        if (ConfigurationWebModuleEditorPart.this.selection < 0) {
          return;
        }
        WebModule module = ConfigurationWebModuleEditorPart.this.configuration.getWebModules()
            .get(ConfigurationWebModuleEditorPart.this.selection);
        WebModuleDialog dialog = new WebModuleDialog(getEditorSite().getShell(), getServer(), module);
        dialog.open();
        if (dialog.getReturnCode() == IDialogConstants.OK_ID) {
          execute(new ModifyWebModuleCommand(ConfigurationWebModuleEditorPart.this.configuration,
              ConfigurationWebModuleEditorPart.this.selection, dialog.getWebModule()));
        }
      }
    });
    whs.setHelp(this.edit, ContextIds.CONFIGURATION_EDITOR_WEBMODULES_EDIT);

    this.remove = toolkit.createButton(rightPanel, Messages.editorRemove, SWT.PUSH);
    data = new GridData(GridData.FILL_HORIZONTAL);
    this.remove.setLayoutData(data);
    this.remove.setEnabled(false);
    this.remove.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        if (ConfigurationWebModuleEditorPart.this.selection < 0) {
          return;
        }
        TableItem item =
            ConfigurationWebModuleEditorPart.this.webAppTable.getItem(ConfigurationWebModuleEditorPart.this.selection);
        if (item.getData() != null) {
          IModule module = (IModule) item.getData();
          execute(new RemoveModuleCommand(getServer(), module));
        } else {
          execute(new RemoveWebModuleCommand(ConfigurationWebModuleEditorPart.this.configuration,
              ConfigurationWebModuleEditorPart.this.selection));
        }
        ConfigurationWebModuleEditorPart.this.remove.setEnabled(false);
        ConfigurationWebModuleEditorPart.this.edit.setEnabled(false);
        ConfigurationWebModuleEditorPart.this.selection = -1;
      }
    });
    whs.setHelp(this.remove, ContextIds.CONFIGURATION_EDITOR_WEBMODULES_REMOVE);

    form.setContent(section);
    form.reflow(true);

    initialize();
  }

  protected boolean canAddWebModule() {
    IModule[] modules = ServerUtil.getModules(this.server.getServerType().getRuntimeType().getModuleTypes());
    if (modules != null) {
      int size = modules.length;
      for (int i = 0; i < size; i++) {
        IWebModule webModule = (IWebModule) modules[i].loadAdapter(IWebModule.class, null);
        if (webModule != null) {
          IStatus status = this.server.canModifyModules(new IModule[] { modules[i] }, null, null);
          if ((status != null) && status.isOK()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public void dispose() {
    super.dispose();

    if (this.configuration != null) {
      this.configuration.removePropertyChangeListener(this.listener);
    }
  }

  /*
   * (non-Javadoc) Initializes the editor part with a site and input.
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) {
    super.init(site, input);

    IServerWrapper ts = (IServerWrapper) this.server.loadAdapter(IServerWrapper.class, null);
    try {
      this.configuration = ts.loadConfiguration();
    } catch (Exception e) {
      // ignore
    }
    if (this.configuration != null) {
      addChangeListener();
    }

    initialize();
  }

  /**
   *
   */
  protected void initialize() {
    if (this.webAppTable == null) {
      return;
    }

    this.webAppTable.removeAll();
    setErrorMessage(null);

    ILabelProvider labelProvider = ServerUICore.getLabelProvider();
    List<WebModule> list = this.configuration.getWebModules();
    Iterator<WebModule> iterator = list.iterator();
    while (iterator.hasNext()) {
      WebModule module = iterator.next();
      TableItem item = new TableItem(this.webAppTable, SWT.NONE);

      String memento = module.getMemento();
      String projectName = "";
      Image projectImage = null;
      if ((memento != null) && (memento.length() > 0)) {
        projectName = NLS.bind(Messages.configurationEditorProjectMissing, new String[] { memento });
        projectImage = ServerUIPlugin.getImage(ServerUIPlugin.IMG_PROJECT_MISSING);
        IModule module2 = ServerUtil.getModule(memento);
        if (module2 != null) {
          projectName = labelProvider.getText(module2);
          projectImage = labelProvider.getImage(module2);
          item.setData(module2);
        }
      }

      String reload = module.isReloadable() ? Messages.configurationEditorReloadEnabled
          : Messages.configurationEditorReloadDisabled;
      String[] s = new String[] { module.getPath(), module.getDocumentBase(), projectName, reload };
      item.setText(s);
      item.setImage(0, ServerUIPlugin.getImage(ServerUIPlugin.IMG_WEB_MODULE));
      if (projectImage != null) {
        item.setImage(2, projectImage);
      }

      if (!isDocumentBaseValid(module.getDocumentBase())) {
        item.setImage(1, ServerUIPlugin.getImage(ServerUIPlugin.IMG_PROJECT_MISSING));
        setErrorMessage(NLS.bind(Messages.errorMissingWebModule, module.getDocumentBase()));
      }
    }
    labelProvider = null;

    if (this.readOnly) {
      this.addProject.setEnabled(false);
      this.addExtProject.setEnabled(false);
      this.edit.setEnabled(false);
      this.remove.setEnabled(false);
    } else {
      this.addProject.setEnabled(canAddWebModule());
      this.addExtProject.setEnabled(true);
    }
  }

  /**
   *
   */
  protected void selectWebApp() {
    if (this.readOnly) {
      return;
    }

    try {
      this.selection = this.webAppTable.getSelectionIndex();
      this.remove.setEnabled(true);
      this.edit.setEnabled(true);
    } catch (Exception e) {
      this.selection = -1;
      this.remove.setEnabled(false);
      this.edit.setEnabled(false);
    }
  }

  protected boolean isDocumentBaseValid(String s) {
    if ((s == null) || (s.length() < 2)) {
      return true;
    }

    // check absolute path
    File f = new File(s);
    if (f.exists()) {
      return true;
    }

    // check workspace
    try {
      if (ResourcesPlugin.getWorkspace().getRoot().getProject(s).exists()) {
        return true;
      }
    } catch (Exception e) {
      // bad path
    }

    if (s.startsWith(this.configuration.getDocBasePrefix())) {
      try {
        String t = s.substring(this.configuration.getDocBasePrefix().length());
        if (ResourcesPlugin.getWorkspace().getRoot().getProject(t).exists()) {
          return true;
        }
      } catch (Exception e) {
        // bad path
      }
    }

    // check server relative path
    try {
      f = this.server.getRuntime().getLocation().append(s).toFile();
      if (f.exists()) {
        return true;
      }
    } catch (Exception e) {
      // bad path
    }

    return false;
  }

  /*
   * @see IWorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    if (this.webAppTable != null) {
      this.webAppTable.setFocus();
    }
  }
}
