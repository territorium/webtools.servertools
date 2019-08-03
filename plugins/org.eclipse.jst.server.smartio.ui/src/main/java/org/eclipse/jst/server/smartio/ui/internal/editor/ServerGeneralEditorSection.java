/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui.internal.editor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.smartio.core.internal.IServerVersionHandler;
import org.eclipse.jst.server.smartio.core.internal.IServerWrapper;
import org.eclipse.jst.server.smartio.core.internal.ServerWrapper;
import org.eclipse.jst.server.smartio.core.internal.command.SetSecureCommand;
import org.eclipse.jst.server.smartio.ui.internal.ContextIds;
import org.eclipse.jst.server.smartio.ui.internal.Messages;
import org.eclipse.jst.server.smartio.ui.internal.ServerUIPlugin;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * smart.IO server general editor page.
 */
public class ServerGeneralEditorSection extends ServerEditorSection {

  private ServerWrapper          wrapper;

  private Button                 secure;
  private Button                 reloadableByDefault;
  private boolean                updating;

  private PropertyChangeListener listener;

  private boolean                separateContextFilesChanged;

  /**
   * ServerGeneralEditorPart constructor comment.
   */
  public ServerGeneralEditorSection() {
    // do nothing
  }

  /**
   * 
   */
  private void addChangeListener() {
    listener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (updating) {
          return;
        }
        updating = true;
        if (ServerWrapper.PROPERTY_SECURE.equals(event.getPropertyName())) {
          Boolean b = (Boolean) event.getNewValue();
          secure.setSelection(b.booleanValue());
        } else if (IServerWrapper.PROPERTY_MODULES_RELOADABLE_BY_DEFAULT.equals(event.getPropertyName())) {
          Boolean b = (Boolean) event.getNewValue();
          reloadableByDefault.setSelection(b.booleanValue());
        }
        updating = false;
      }
    };
    server.addPropertyChangeListener(listener);
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
    toolkit.paintBordersFor(composite);
    section.setClient(composite);
    
    // TODO Address help
    // whs.setHelp(separateContextFiles, ContextIds.SERVER_EDITOR_SECURE);

    // modules reloadable by default
    reloadableByDefault =
        toolkit.createButton(composite, NLS.bind(Messages.serverEditorReloadableByDefault, ""), SWT.CHECK);
    GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    data.horizontalSpan = 3;
    reloadableByDefault.setLayoutData(data);
    reloadableByDefault.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent se) {
        if (updating) {
          return;
        }
        updating = true;
        updating = false;
      }
    });
    // TODO Address help
    // whs.setHelp(reloadableByDefault, ContextIds.SERVER_EDITOR_SECURE);

    // security
    secure = toolkit.createButton(composite, Messages.serverEditorSecure, SWT.CHECK);
    data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    data.horizontalSpan = 3;
    secure.setLayoutData(data);
    secure.addSelectionListener(new SelectionAdapter() {

      public void widgetSelected(SelectionEvent se) {
        if (updating) {
          return;
        }
        updating = true;
        execute(new SetSecureCommand(wrapper, secure.getSelection()));
        updating = false;
      }
    });
    whs.setHelp(secure, ContextIds.SERVER_EDITOR_SECURE);

    initialize();
  }

  /**
   * @see ServerEditorSection#dispose()
   */
  @Override
  public void dispose() {
    if (server != null) {
      server.removePropertyChangeListener(listener);
    }
  }

  /**
   * @see ServerEditorSection#init(IEditorSite, IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) {
    super.init(site, input);

    if (server != null) {
      wrapper = (ServerWrapper) server.loadAdapter(ServerWrapper.class, null);
      addChangeListener();
    }
    initialize();
  }

  /**
   * Initialize the fields in this editor.
   */
  private void initialize() {
    if ((secure == null) || (wrapper == null)) {
      return;
    }
    updating = true;
    IServerVersionHandler tvh = wrapper.getVersionHandler();

    boolean supported = true; // all versions of smart.IO support reloadable option
    String label = NLS.bind(Messages.serverEditorReloadableByDefault, supported ? "" : Messages.serverEditorNotSupported);
    reloadableByDefault.setText(label);
    reloadableByDefault.setSelection(wrapper.isModulesReloadableByDefault());
    if (readOnly || !supported) {
      reloadableByDefault.setEnabled(false);
    } else {
      reloadableByDefault.setEnabled(true);
    }

    secure.setSelection(wrapper.isSecure());

    if (readOnly) {
      secure.setEnabled(false);
    } else {
      secure.setEnabled(true);
    }

    updating = false;
  }

  /**
   * @see ServerEditorSection#getSaveStatus()
   */
  @Override
  public IStatus[] getSaveStatus() {
    if (separateContextFilesChanged) {
      // If server is running, abort the save since contexts will be moving
      if (wrapper.getServer().getServerState() != IServer.STATE_STOPPED) {
        return new IStatus[] { new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID, NLS
            .bind(Messages.errorServerMustBeStopped, NLS.bind(Messages.serverEditorSeparateContextFiles, "").trim())) };
      }
    }
    // use default implementation to return success
    return super.getSaveStatus();
  }
}
