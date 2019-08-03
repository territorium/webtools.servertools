/**********************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 **********************************************************************/

package org.eclipse.jst.server.smartio.ui.editor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.smartio.core.IServerWrapper;
import org.eclipse.jst.server.smartio.core.command.SetConfigPathCommand;
import org.eclipse.jst.server.smartio.ui.ContextIds;
import org.eclipse.jst.server.smartio.ui.Messages;
import org.eclipse.jst.server.smartio.ui.ServerUIPlugin;
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * smart.IO server general editor page.
 */
public class ServerGeneralEditorSection extends ServerEditorSection {

  private IServerWrapper         wrapper;
  private boolean                updating;
  private PropertyChangeListener listener;


  private Button reloadable;

  private Text   confDir;
  private Button confDirBrowse;

  /**
   * Add listeners to detect undo changes and publishing of the server.
   */
  private void addChangeListeners() {
    this.listener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (ServerGeneralEditorSection.this.updating) {
          return;
        }
        ServerGeneralEditorSection.this.updating = true;
        if (IServerWrapper.PROPERTY_CONF_DIR.equals(event.getPropertyName())) {
          String s = (String) event.getNewValue();
          ServerGeneralEditorSection.this.confDir.setText(s);
          validate();
        } else if (IServerWrapper.PROPERTY_MODULES_RELOADABLE.equals(event.getPropertyName())) {
          Boolean b = (Boolean) event.getNewValue();
          ServerGeneralEditorSection.this.reloadable.setSelection(b.booleanValue());
        }
        ServerGeneralEditorSection.this.updating = false;
      }
    };
    this.server.addPropertyChangeListener(this.listener);
  }

  /**
   * Creates the SWT controls for this workbench part.
   *
   * @param parent the parent control
   */
  @Override
  public void createSection(Composite parent) {
    super.createSection(parent);
    FormToolkit toolkit = getFormToolkit(parent.getDisplay());

    Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
        | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
    section.setText(Messages.serverEditorGeneralSection);
    section.setDescription(Messages.serverEditorGeneralDescription);
    section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));


    Composite composite = toolkit.createComposite(section);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    layout.marginHeight = 5;
    layout.marginWidth = 10;
    layout.verticalSpacing = 5;
    layout.horizontalSpacing = 15;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
    IWorkbenchHelpSystem whs = PlatformUI.getWorkbench().getHelpSystem();
    whs.setHelp(composite, ContextIds.SERVER_EDITOR);
    whs.setHelp(section, ContextIds.SERVER_EDITOR);
    toolkit.paintBordersFor(composite);
    section.setClient(composite);


    // modules reloadable by default
    this.reloadable =
        toolkit.createButton(composite, NLS.bind(Messages.serverEditorReloadableByDefault, ""), SWT.CHECK);
    GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    data.horizontalSpan = 3;
    this.reloadable.setLayoutData(data);
    this.reloadable.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent se) {
        if (ServerGeneralEditorSection.this.updating) {
          return;
        }
        ServerGeneralEditorSection.this.updating = true;
        ServerGeneralEditorSection.this.updating = false;
      }
    });

    // configuration directory
    Label label = createLabel(toolkit, composite, Messages.serverEditorConfDir);
    data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
    label.setLayoutData(data);

    this.confDir = toolkit.createText(composite, null);
    this.confDir.setEditable(false);
    data = new GridData(SWT.FILL, SWT.CENTER, true, false);
    this.confDir.setLayoutData(data);

    this.confDirBrowse = toolkit.createButton(composite, Messages.editorBrowse, SWT.PUSH);
    this.confDirBrowse.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent se) {
        DirectoryDialog dialog = new DirectoryDialog(ServerGeneralEditorSection.this.confDir.getShell());
        dialog.setMessage(Messages.serverEditorBrowseConfMessage);
        dialog.setFilterPath(ServerGeneralEditorSection.this.confDir.getText());
        String selectedDirectory = dialog.open();
        if ((selectedDirectory != null)
            && !selectedDirectory.equals(ServerGeneralEditorSection.this.confDir.getText())) {
          ServerGeneralEditorSection.this.updating = true;
          ServerGeneralEditorSection.this.confDir.setText(selectedDirectory);
          // ServerGeneralEditorSection.this.wrapper.setConfDirectory(selectedDirectory);
          execute(new SetConfigPathCommand(wrapper, selectedDirectory));
          ServerGeneralEditorSection.this.updating = false;
          validate();
        }
      }
    });

    this.confDirBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

    initialize();
  }

  private Label createLabel(FormToolkit toolkit, Composite parent, String text) {
    Label label = toolkit.createLabel(parent, text);
    label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
    return label;
  }

  /**
   * @see ServerEditorSection#dispose()
   */
  @Override
  public void dispose() {
    if (this.server != null) {
      this.server.removePropertyChangeListener(this.listener);
    }
  }

  /**
   * @see ServerEditorSection#init(IEditorSite, IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) {
    super.init(site, input);

    // Cache workspace and default deploy paths
    // IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

    if (this.server != null) {
      this.wrapper = (IServerWrapper) this.server.loadAdapter(IServerWrapper.class, null);
      addChangeListeners();
    }
  }

  /**
   * Initialize the fields in this editor.
   */
  private void initialize() {
    this.updating = true;

    this.reloadable.setText(Messages.serverEditorReloadableByDefault);
    this.reloadable.setSelection(this.wrapper.isModulesReloadable());
    this.confDir.setText(this.wrapper.getConfDirectory());

    this.updating = false;
    validate();
  }

  /**
   * @see ServerEditorSection#getSaveStatus()
   */
  @Override
  public IStatus[] getSaveStatus() {
    if (this.wrapper != null) {
      // Check the instance directory
      String dir = this.wrapper.getConfDirectory();
      if (dir != null && dir.length() == 0) {
        // Must not be the same as the workspace location
        return new IStatus[] { new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID, Messages.errorServerDirIsRoot) };
      }
    }
    // use default implementation to return success
    return super.getSaveStatus();
  }

  private void validate() {
    if (this.wrapper != null) {
      // Validate instance directory
      String dir = this.wrapper.getConfDirectory();
      if (dir != null && dir.length() == 0) {
        // Must not be the same as the workspace location
        setErrorMessage(Messages.errorServerDirIsRoot);
        return;
      }
    }
    // All is okay, clear any previous error
    setErrorMessage(null);
  }
}