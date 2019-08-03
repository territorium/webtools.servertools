/**********************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 **********************************************************************/

package org.eclipse.jst.server.smartio.ui.internal.editor;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.smartio.core.internal.IServerWrapper;
import org.eclipse.jst.server.smartio.core.internal.ServerWrapper;
import org.eclipse.jst.server.smartio.ui.internal.ContextIds;
import org.eclipse.jst.server.smartio.ui.internal.Messages;
import org.eclipse.jst.server.smartio.ui.internal.ServerUIPlugin;
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.wst.server.core.IPublishListener;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.util.PublishAdapter;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * smart.IO server general editor page.
 */
public class ServerLocationEditorSection extends ServerEditorSection {

  private Section                section;
  private ServerWrapper          wrapper;

  private Hyperlink              setDefaultDeployDir;

  private boolean                defaultDeployDirIsSet;

  private Text                   serverDir;
  private Text                   deployDir;
  private Button                 deployDirBrowse;
  private boolean                updating;

  private PropertyChangeListener listener;
  private IPublishListener       publishListener;
  private IPath                  workspacePath;
  private IPath                  defaultDeployPath;

  private boolean                allowRestrictedEditing;
  private IPath                  installDirPath;

  // Avoid hardcoding this at some point
  private final static String METADATADIR = ".metadata";

  /**
   * ServerGeneralEditorPart constructor comment.
   */
  public ServerLocationEditorSection() {
    // do nothing
  }

  /**
   * Add listeners to detect undo changes and publishing of the server.
   */
  private void addChangeListeners() {
    listener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (updating) {
          return;
        }
        updating = true;
        if (IServerWrapper.PROPERTY_INSTANCE_DIR.equals(event.getPropertyName())) {
          updateServerDirFields();
          validate();
        } else if (IServerWrapper.PROPERTY_DEPLOY_DIR.equals(event.getPropertyName())) {
          String s = (String) event.getNewValue();
          deployDir.setText(s);
          updateDefaultDeployLink();
          validate();
        }
        updating = false;
      }
    };
    server.addPropertyChangeListener(listener);

    publishListener = new PublishAdapter() {

      @Override
      public void publishFinished(IServer server2, IStatus status) {
        boolean flag = false;
        if (status.isOK() && (server2.getModules().length == 0)) {
          flag = true;
        }
        if (flag != allowRestrictedEditing) {
          allowRestrictedEditing = flag;
          // Update the state of the fields
          PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            @Override
            public void run() {
              if (!setDefaultDeployDir.isDisposed()) {
                setDefaultDeployDir.setEnabled(allowRestrictedEditing);
              }
              if (!deployDir.isDisposed()) {
                deployDir.setEnabled(allowRestrictedEditing);
              }
              if (!deployDirBrowse.isDisposed()) {
                deployDirBrowse.setEnabled(allowRestrictedEditing);
              }
            }
          });
        }
      }
    };
    server.getOriginal().addPublishListener(publishListener);
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

    section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
        | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
    section.setText(Messages.serverEditorLocationsSection);
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

    serverDir = toolkit.createText(composite, null, SWT.SINGLE);
    GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
    data.widthHint = 75;
    serverDir.setLayoutData(data);
    serverDir.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        if (updating) {
          return;
        }
        validate();
      }
    });

    // deployment directory link
    setDefaultDeployDir =
        toolkit.createHyperlink(composite, NLS.bind(Messages.serverEditorSetDefaultDeployDirLink, ""), SWT.WRAP);
    setDefaultDeployDir.addHyperlinkListener(new HyperlinkAdapter() {

      @Override
      public void linkActivated(HyperlinkEvent e) {
        updating = true;
        deployDir.setText(IServerWrapper.DEFAULT_DEPLOYDIR);
        updateDefaultDeployLink();
        updating = false;
        validate();
      }
    });
    data = new GridData(SWT.FILL, SWT.CENTER, true, false);
    data.horizontalSpan = 3;
    setDefaultDeployDir.setLayoutData(data);

    // deployment directory
    Label label = createLabel(toolkit, composite, Messages.serverEditorDeployDir);
    data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
    label.setLayoutData(data);

    deployDir = toolkit.createText(composite, null);
    data = new GridData(SWT.FILL, SWT.CENTER, true, false);
    deployDir.setLayoutData(data);
    deployDir.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        if (updating) {
          return;
        }
        updating = true;
        updateDefaultDeployLink();
        updating = false;
        validate();
      }
    });

    deployDirBrowse = toolkit.createButton(composite, Messages.editorBrowse, SWT.PUSH);
    deployDirBrowse.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent se) {
        DirectoryDialog dialog = new DirectoryDialog(deployDir.getShell());
        dialog.setMessage(Messages.serverEditorBrowseDeployMessage);
        dialog.setFilterPath(deployDir.getText());
        String selectedDirectory = dialog.open();
        if ((selectedDirectory != null) && !selectedDirectory.equals(deployDir.getText())) {
          updating = true;
          deployDir.setText(selectedDirectory);
          updating = false;
          validate();
        }
      }
    });
    deployDirBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

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
    if (server != null) {
      server.removePropertyChangeListener(listener);
      if (server.getOriginal() != null) {
        server.getOriginal().removePublishListener(publishListener);
      }
    }
  }

  /**
   * @see ServerEditorSection#init(IEditorSite, IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) {
    super.init(site, input);

    // Cache workspace and default deploy paths
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    workspacePath = root.getLocation();
    defaultDeployPath = new Path(IServerWrapper.DEFAULT_DEPLOYDIR);

    if (server != null) {
      wrapper = (ServerWrapper) server.loadAdapter(ServerWrapper.class, null);
      addChangeListeners();
    }
    initialize();
  }

  /**
   * Initialize the fields in this editor.
   */
  private void initialize() {
    if ((serverDir == null) || (wrapper == null)) {
      return;
    }
    updating = true;

    IRuntime runtime = server.getRuntime();
    if (runtime != null) {
      installDirPath = runtime.getLocation();
    }

    // determine if editing of locations is allowed
    allowRestrictedEditing = false;
    IPath basePath = wrapper.getRuntimeBaseDirectory();
    if (!readOnly) {
      // If server has not been published, or server is published with no
      // modules, allow editing
      // TODO Find better way to determine if server hasn't been published
      if (((basePath != null) && !basePath.append("conf").toFile().exists())
          || ((server.getOriginal().getServerPublishState() == IServer.PUBLISH_STATE_NONE)
              && (server.getOriginal().getModules().length == 0))) {
        allowRestrictedEditing = true;
      }
    }

    // Update server related fields
    updateServerDirFields();

    // Update deployment related fields
    updateDefaultDeployLink();

    deployDir.setText(wrapper.getDeployDirectory());

    setDefaultDeployDir.setEnabled(allowRestrictedEditing);
    deployDir.setEnabled(allowRestrictedEditing);
    deployDirBrowse.setEnabled(allowRestrictedEditing);

    updating = false;
    validate();
  }

  private void updateServerDirFields() {
    updateServerDir();
    serverDir.setEnabled(allowRestrictedEditing && false);
  }

  private void updateServerDir() {
    IPath path = wrapper.getRuntimeBaseDirectory();
    if (path == null) {
      serverDir.setText("");
    } else if (workspacePath.isPrefixOf(path)) {
      int cnt = path.matchingFirstSegments(workspacePath);
      path = path.removeFirstSegments(cnt).setDevice(null);
      serverDir.setText(path.toOSString());
    } else {
      serverDir.setText(path.toOSString());
    }
  }

  private void updateDefaultDeployLink() {
    boolean newState = defaultDeployPath.equals(new Path(wrapper.getDeployDirectory()));
    if (newState != defaultDeployDirIsSet) {
      setDefaultDeployDir.setText(
          newState ? Messages.serverEditorSetDefaultDeployDirLink2 : Messages.serverEditorSetDefaultDeployDirLink);
      defaultDeployDirIsSet = newState;
    }
  }

  /**
   * @see ServerEditorSection#getSaveStatus()
   */
  @Override
  public IStatus[] getSaveStatus() {
    if (wrapper != null) {
      // Check the instance directory
      String dir = wrapper.getInstanceDirectory();
      if (dir != null) {
        IPath path = new Path(dir);
        // Must not be the same as the workspace location
        if ((dir.length() == 0) || workspacePath.equals(path)) {
          return new IStatus[] { new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID, Messages.errorServerDirIsRoot) };
        }
        // User specified value may not be under the ".metadata" folder of the
        // workspace
        else if (workspacePath.isPrefixOf(path)
            || (!path.isAbsolute() && ServerLocationEditorSection.METADATADIR.equals(path.segment(0)))) {
          int cnt = path.matchingFirstSegments(workspacePath);
          if (ServerLocationEditorSection.METADATADIR.equals(path.segment(cnt))) {
            return new IStatus[] { new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID,
                NLS.bind(Messages.errorServerDirUnderRoot, ServerLocationEditorSection.METADATADIR)) };
          }
        } else if (path.equals(installDirPath)) {
          return new IStatus[] { new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID, "") };
        }
      } else {
        IPath path = wrapper.getRuntimeBaseDirectory();
        // If non-custom instance dir is not the install and metadata isn't the
        // selection, return
        // error
        if (!path.equals(installDirPath)) {
          return new IStatus[] { new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID, "") };
        }
      }

      // Check the deployment directory
      dir = wrapper.getDeployDirectory();
      // Deploy directory must be set
      if ((dir == null) || (dir.length() == 0)) {
        return new IStatus[] {
            new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID, Messages.errorDeployDirNotSpecified) };
      }
    }
    // use default implementation to return success
    return super.getSaveStatus();
  }

  private void validate() {
    if (wrapper != null) {
      // Validate instance directory
      String dir = wrapper.getInstanceDirectory();
      if (dir != null) {
        IPath path = new Path(dir);
        // Must not be the same as the workspace location
        if ((dir.length() == 0) || workspacePath.equals(path)) {
          setErrorMessage(Messages.errorServerDirIsRoot);
          return;
        }
        // User specified value may not be under the ".metadata" folder of the
        // workspace
        else if (workspacePath.isPrefixOf(path)
            || (!path.isAbsolute() && ServerLocationEditorSection.METADATADIR.equals(path.segment(0)))) {
          int cnt = path.matchingFirstSegments(workspacePath);
          if (ServerLocationEditorSection.METADATADIR.equals(path.segment(cnt))) {
            setErrorMessage(NLS.bind(Messages.errorServerDirUnderRoot, ServerLocationEditorSection.METADATADIR));
            return;
          }
        } else if (path.equals(installDirPath)) {
          return;
        }
      }

      // Check the deployment directory
      dir = wrapper.getDeployDirectory();
      // Deploy directory must be set
      if ((dir == null) || (dir.length() == 0)) {
        setErrorMessage(Messages.errorDeployDirNotSpecified);
        return;
      }
    }
    // All is okay, clear any previous error
    setErrorMessage(null);
  }
}