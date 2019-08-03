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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jst.server.smartio.core.internal.IServerConfiguration;
import org.eclipse.jst.server.smartio.core.internal.ServerWrapper;
import org.eclipse.jst.server.smartio.core.internal.command.ModifyPortCommand;
import org.eclipse.jst.server.smartio.ui.internal.ContextIds;
import org.eclipse.jst.server.smartio.ui.internal.Messages;
import org.eclipse.jst.server.smartio.ui.internal.ServerUIPlugin;
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
import java.util.Iterator;

/**
 * smart.IO configuration port editor page.
 */
public class ConfigurationPortEditorSection extends ServerEditorSection {

  protected IServerConfiguration    configuration;

  protected boolean                updating;

  protected Table                  ports;
  protected TableViewer            viewer;

  protected PropertyChangeListener listener;

  /**
   * ConfigurationPortEditorSection constructor comment.
   */
  public ConfigurationPortEditorSection() {
    super();
  }

  /**
   * 
   */
  protected void addChangeListener() {
    listener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (IServerConfiguration.MODIFY_PORT_PROPERTY.equals(event.getPropertyName())) {
          String id = (String) event.getOldValue();
          Integer i = (Integer) event.getNewValue();
          changePortNumber(id, i.intValue());
        }
      }
    };
    configuration.addPropertyChangeListener(listener);
  }

  /**
   * 
   * @param id java.lang.String
   * @param port int
   */
  protected void changePortNumber(String id, int port) {
    TableItem[] items = ports.getItems();
    int size = items.length;
    for (int i = 0; i < size; i++) {
      ServerPort sp = (ServerPort) items[i].getData();
      if (sp.getId().equals(id)) {
        items[i].setData(new ServerPort(id, sp.getName(), port, sp.getProtocol()));
        items[i].setText(1, port + "");
        /*
         * if (i == selection) { selectPort(); }
         */
        return;
      }
    }
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

    ports = toolkit.createTable(composite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
    ports.setHeaderVisible(true);
    ports.setLinesVisible(true);
    whs.setHelp(ports, ContextIds.CONFIGURATION_EDITOR_PORTS_LIST);

    TableLayout tableLayout = new TableLayout();

    TableColumn col = new TableColumn(ports, SWT.NONE);
    col.setText(Messages.configurationEditorPortNameColumn);
    ColumnWeightData colData = new ColumnWeightData(15, 150, true);
    tableLayout.addColumnData(colData);

    col = new TableColumn(ports, SWT.NONE);
    col.setText(Messages.configurationEditorPortValueColumn);
    colData = new ColumnWeightData(8, 80, true);
    tableLayout.addColumnData(colData);

    GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
    data.widthHint = 230;
    data.heightHint = 100;
    ports.setLayoutData(data);
    ports.setLayout(tableLayout);

    viewer = new TableViewer(ports);
    viewer.setColumnProperties(new String[] { "name", "port" });

    initialize();
  }

  protected void setupPortEditors() {
    viewer.setCellEditors(new CellEditor[] { null, new TextCellEditor(ports) });

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
          execute(new ModifyPortCommand(configuration, sp.getId(), port));
        } catch (Exception ex) {
          // ignore
        }
      }
    };
    viewer.setCellModifier(cellModifier);

    // preselect second column (Windows-only)
    String os = System.getProperty("os.name");
    if ((os != null) && (os.toLowerCase().indexOf("win") >= 0)) {
      ports.addSelectionListener(new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent event) {
          try {
            int n = ports.getSelectionIndex();
            viewer.editElement(ports.getItem(n).getData(), 1);
          } catch (Exception e) {
            // ignore
          }
        }
      });
    }
  }

  @Override
  public void dispose() {
    if (configuration != null) {
      configuration.removePropertyChangeListener(listener);
    }
  }

  /*
   * (non-Javadoc) Initializes the editor part with a site and input.
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) {
    super.init(site, input);

    ServerWrapper ts = server.getAdapter(ServerWrapper.class);
    try {
      configuration = ts.getConfiguration();
    } catch (Exception e) {
      // ignore
    }
    addChangeListener();
    initialize();
  }

  /**
   * Initialize the fields in this editor.
   */
  protected void initialize() {
    if (ports == null) {
      return;
    }

    ports.removeAll();

    Iterator<ServerPort> iterator = configuration.getServerPorts().iterator();
    while (iterator.hasNext()) {
      ServerPort port = iterator.next();
      TableItem item = new TableItem(ports, SWT.NONE);
      String portStr = "-";
      if (port.getPort() >= 0) {
        portStr = port.getPort() + "";
      }
      String[] s = new String[] { port.getName(), portStr };
      item.setText(s);
      item.setImage(ServerUIPlugin.getImage(ServerUIPlugin.IMG_PORT));
      item.setData(port);
    }

    if (readOnly) {
      viewer.setCellEditors(new CellEditor[] { null, null });
      viewer.setCellModifier(null);
    } else {
      setupPortEditors();
    }
  }
}
