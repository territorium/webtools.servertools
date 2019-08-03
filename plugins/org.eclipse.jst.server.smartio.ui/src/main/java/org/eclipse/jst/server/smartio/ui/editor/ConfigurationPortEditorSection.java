/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jst.server.smartio.core.IServerConfiguration;
import org.eclipse.jst.server.smartio.core.IServerWrapper;
import org.eclipse.jst.server.smartio.core.command.ModifyPortCommand;
import org.eclipse.jst.server.smartio.ui.ContextIds;
import org.eclipse.jst.server.smartio.ui.Messages;
import org.eclipse.jst.server.smartio.ui.ServerUIPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * smart.IO configuration port editor page.
 */
public class ConfigurationPortEditorSection extends ServerEditorSection {

  private IServerConfiguration   configuration;

  private Table                  ports;
  private TableViewer            viewer;

  private PropertyChangeListener listener;

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
    section.setText(Messages.configurationEditorPortsSection);
    section.setDescription(Messages.configurationEditorPortsDescription);
    section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

    // ports
    Composite composite = toolkit.createComposite(section);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 8;
    layout.marginWidth = 8;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL));
    IWorkbenchHelpSystem whs = PlatformUI.getWorkbench().getHelpSystem();
    whs.setHelp(composite, ContextIds.CONFIGURATION_EDITOR_PORTS);
    toolkit.paintBordersFor(composite);
    section.setClient(composite);

    this.ports = toolkit.createTable(composite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
    this.ports.setHeaderVisible(true);
    this.ports.setLinesVisible(true);
    whs.setHelp(this.ports, ContextIds.CONFIGURATION_EDITOR_PORTS_LIST);

    TableLayout tableLayout = new TableLayout();

    TableColumn col = new TableColumn(this.ports, SWT.NONE);
    col.setText(Messages.configurationEditorPortNameColumn);
    ColumnWeightData colData = new ColumnWeightData(15, 150, true);
    tableLayout.addColumnData(colData);

    col = new TableColumn(this.ports, SWT.NONE);
    col.setText(Messages.configurationEditorPortValueColumn);
    colData = new ColumnWeightData(8, 80, true);
    tableLayout.addColumnData(colData);

    GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
    data.widthHint = 230;
    data.heightHint = 100;
    this.ports.setLayoutData(data);
    this.ports.setLayout(tableLayout);

    this.viewer = new TableViewer(this.ports);
    this.viewer.setColumnProperties(new String[] { "name", "port" });

    initialize();
  }

  /*
   * (non-Javadoc) Initializes the editor part with a site and input.
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) {
    super.init(site, input);

    IServerWrapper wrapper = this.server.getAdapter(IServerWrapper.class);
    try {
      this.configuration = wrapper.loadConfiguration();
    } catch (Exception e) {
      // ignore
    }
    addChangeListener();
  }

  @Override
  public void dispose() {
    if (this.configuration != null) {
      this.configuration.removePropertyChangeListener(this.listener);
    }
  }

  /**
   * Initialize the fields in this editor.
   */
  protected void initialize() {
    this.ports.removeAll();

    for (ServerPort port : this.configuration.getServerPorts()) {
      TableItem item = new TableItem(this.ports, SWT.NONE);

      String portStr = "-";
      if (port.getPort() >= 0) {
        portStr = port.getPort() + "";
      }
      String[] s = new String[] { port.getName(), portStr };
      item.setText(s);
      item.setImage(ServerUIPlugin.getImage(ServerUIPlugin.IMG_PORT));
      item.setData(port);
    }

    if (this.readOnly) {
      this.viewer.setCellEditors(new CellEditor[] { null, null });
      this.viewer.setCellModifier(null);
    } else {
      setupPortEditors();
    }
  }

  /**
   *
   */
  protected void addChangeListener() {
    this.listener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (IServerConfiguration.SET_PORT_PROPERTY.equals(event.getPropertyName())) {
          String id = (String) event.getOldValue();
          Integer i = (Integer) event.getNewValue();
          changePortNumber(id, i.intValue());
        }
      }
    };
    this.configuration.addPropertyChangeListener(this.listener);
  }

  /**
   *
   * @param id java.lang.String
   * @param port int
   */
  protected void changePortNumber(String id, int port) {
    TableItem[] items = this.ports.getItems();
    int size = items.length;
    for (int i = 0; i < size; i++) {
      ServerPort sp = (ServerPort) items[i].getData();
      if (sp.getId().equals(id)) {
        items[i].setData(new ServerPort(id, sp.getName(), port, sp.getProtocol()));
        items[i].setText(1, port + "");
        return;
      }
    }
  }

  protected void setupPortEditors() {
    this.viewer.setCellEditors(new CellEditor[] { null, new TextCellEditor(this.ports) });

    ICellModifier cellModifier = new ICellModifier() {

      @Override
      public Object getValue(Object element, String property) {
        ServerPort sp = (ServerPort) element;
        if (sp.getPort() < 0) {
          return "-";
        }
        return sp.getPort() + "";
      }

      @Override
      public boolean canModify(Object element, String property) {
        if ("port".equals(property)) {
          return true;
        }

        return false;
      }

      @Override
      public void modify(Object element, String property, Object value) {
        try {
          Item item = (Item) element;
          ServerPort sp = (ServerPort) item.getData();
          int port = Integer.parseInt((String) value);
          execute(new ModifyPortCommand(ConfigurationPortEditorSection.this.configuration, sp.getId(), port));
        } catch (Exception ex) {
          // ignore
        }
      }
    };
    this.viewer.setCellModifier(cellModifier);

    // preselect second column (Windows-only)
    String os = System.getProperty("os.name");
    if ((os != null) && (os.toLowerCase().indexOf("win") >= 0)) {
      this.ports.addSelectionListener(new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent event) {
          try {
            int n = ConfigurationPortEditorSection.this.ports.getSelectionIndex();
            ConfigurationPortEditorSection.this.viewer
                .editElement(ConfigurationPortEditorSection.this.ports.getItem(n).getData(), 1);
          } catch (Exception e) {
            // ignore
          }
        }
      });
    }
  }
}
