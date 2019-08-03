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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jst.server.smartio.core.internal.MimeMapping;
import org.eclipse.jst.server.smartio.core.internal.ServerConfiguration;
import org.eclipse.jst.server.smartio.core.internal.ServerWrapper;
import org.eclipse.jst.server.smartio.core.internal.command.AddMimeMappingCommand;
import org.eclipse.jst.server.smartio.core.internal.command.ModifyMimeMappingCommand;
import org.eclipse.jst.server.smartio.core.internal.command.RemoveMimeMappingCommand;
import org.eclipse.jst.server.smartio.ui.internal.ContextIds;
import org.eclipse.jst.server.smartio.ui.internal.Messages;
import org.eclipse.jst.server.smartio.ui.internal.ServerUIPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * smart.IO configuration mime editor section.
 */
public class ConfigurationMimeEditorSection extends ServerEditorSection {

  protected ServerConfiguration    configuration;

  protected boolean                updating;

  protected PropertyChangeListener listener;

  protected Tree                   mimeTypes;
  protected int                    index = -1;
  protected List<MimeMapping>      mappings;
  protected Button                 add;
  protected Button                 remove;
  protected Button                 edit;

  /**
   * ConfigurationMimeEditorSection constructor comment.
   */
  public ConfigurationMimeEditorSection() {
    super();
  }

  /**
   * 
   */
  protected void addChangeListener() {
    listener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (ServerConfiguration.MAPPING_PROPERTY_ADD.equals(event.getPropertyName())) {
          Integer in = (Integer) event.getOldValue();
          MimeMapping mapping = (MimeMapping) event.getNewValue();
          addMimeMapping(in.intValue(), mapping);
        } else if (ServerConfiguration.MAPPING_PROPERTY_REMOVE.equals(event.getPropertyName())) {
          Integer in = (Integer) event.getNewValue();
          removeMimeMapping(in.intValue());
        } else if (ServerConfiguration.MAPPING_PROPERTY_MODIFY.equals(event.getPropertyName())) {
          Integer in = (Integer) event.getOldValue();
          MimeMapping mapping = (MimeMapping) event.getNewValue();
          modifyMimeMapping(in.intValue(), mapping);
        }
      }
    };
    configuration.addPropertyChangeListener(listener);
  }

  @Override
  public void createSection(Composite parent) {
    super.createSection(parent);

    FormToolkit toolkit = getFormToolkit(parent.getDisplay());

    Section section = toolkit.createSection(parent,
        ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
    section.setText(Messages.configurationEditorMimeMappingsSection);
    section.setDescription(Messages.configurationEditorMimeMappingsDescription);
    section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

    Composite composite = toolkit.createComposite(section);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 5;
    layout.marginWidth = 10;
    layout.verticalSpacing = 5;
    layout.horizontalSpacing = 15;
    composite.setLayout(layout);
    GridData data = new GridData(GridData.FILL_BOTH);
    composite.setLayoutData(data);
    IWorkbenchHelpSystem whs = PlatformUI.getWorkbench().getHelpSystem();
    whs.setHelp(composite, ContextIds.CONFIGURATION_EDITOR_MAPPINGS);
    toolkit.paintBordersFor(composite);
    section.setClient(composite);

    mimeTypes = toolkit.createTree(composite, SWT.V_SCROLL | SWT.SINGLE | SWT.H_SCROLL);
    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
    data.heightHint = 200;
    mimeTypes.setLayoutData(data);
    whs.setHelp(mimeTypes, ContextIds.CONFIGURATION_EDITOR_MAPPINGS_LIST);

    // add listener to the table
    mimeTypes.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        selectMimeMapping();
      }
    });

    Composite buttonComp = toolkit.createComposite(composite);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    buttonComp.setLayout(layout);
    buttonComp.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL));

    add = toolkit.createButton(buttonComp, Messages.editorAdd, SWT.PUSH);
    add.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

    add.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        MimeMappingDialog dialog = new MimeMappingDialog(getShell());
        dialog.open();
        if (dialog.getReturnCode() == IDialogConstants.OK_ID) {
          execute(new AddMimeMappingCommand(configuration, dialog.getMimeMapping()));
        }
      }
    });
    whs.setHelp(add, ContextIds.CONFIGURATION_EDITOR_MAPPINGS_ADD);

    edit = toolkit.createButton(buttonComp, Messages.editorEdit, SWT.PUSH);
    edit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
    edit.setEnabled(false);

    edit.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        if (index < 0) {
          return;
        }
        MimeMappingDialog dialog = new MimeMappingDialog(getShell(), configuration.getMimeMappings().get(index));
        dialog.open();
        if (dialog.getReturnCode() == IDialogConstants.OK_ID) {
          execute(new ModifyMimeMappingCommand(configuration, index, dialog.getMimeMapping()));
        }
      }
    });
    whs.setHelp(edit, ContextIds.CONFIGURATION_EDITOR_MAPPINGS_EDIT);

    remove = toolkit.createButton(buttonComp, Messages.editorRemove, SWT.PUSH);
    remove.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
    remove.setEnabled(false);

    remove.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        if (index < 0) {
          return;
        }
        execute(new RemoveMimeMappingCommand(configuration, index));
        index = -1;
        edit.setEnabled(false);
        remove.setEnabled(false);
      }
    });
    whs.setHelp(remove, ContextIds.CONFIGURATION_EDITOR_MAPPINGS_REMOVE);

    initialize();
  }

  @Override
  public void dispose() {
    if (configuration != null) {
      configuration.removePropertyChangeListener(listener);
    }
  }

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
    if (mimeTypes == null) {
      return;
    }

    mimeTypes.removeAll();

    mappings = configuration.getMimeMappings();

    // sort mappings
    int size = mappings.size();
    int[] map = new int[size];
    for (int i = 0; i < size; i++) {
      map[i] = i;
    }

    for (int i = 0; i < (size - 1); i++) {
      for (int j = i + 1; j < size; j++) {
        MimeMapping mappingA = mappings.get(map[i]);
        MimeMapping mappingB = mappings.get(map[j]);
        if ((mappingA.getMimeType().compareTo(mappingB.getMimeType()) > 0)
            || ((mappingA.getMimeType().equals(mappingB.getMimeType()))
                && (mappingA.getExtension().compareTo(mappingB.getExtension()) > 0))) {
          int temp = map[i];
          map[i] = map[j];
          map[j] = temp;
        }
      }
    }

    // display them
    Map<String, TreeItem> hash = new HashMap<>();

    for (int i = 0; i < size; i++) {
      MimeMapping mapping = mappings.get(map[i]);
      // get parent node
      TreeItem parent = hash.get(mapping.getMimeType());
      if (parent == null) {
        parent = new TreeItem(mimeTypes, SWT.NONE);
        parent.setText(mapping.getMimeType());
        parent.setImage(ServerUIPlugin.getImage(ServerUIPlugin.IMG_MIME_MAPPING));
        hash.put(mapping.getMimeType(), parent);
      }

      // add node
      TreeItem item = new TreeItem(parent, SWT.NONE);
      item.setText(mapping.getExtension());
      item.setImage(ServerUIPlugin.getImage(ServerUIPlugin.IMG_MIME_EXTENSION));
      item.setData(new Integer(map[i]));
    }

    if (readOnly) {
      add.setEnabled(false);
      edit.setEnabled(false);
      remove.setEnabled(false);
    } else {
      add.setEnabled(true);
      selectMimeMapping();
    }
  }

  /**
   * Add a mime mapping.
   * 
   * @param index2
   * @param map
   */
  protected void addMimeMapping(int index2, MimeMapping map) {
    mappings.add(index2, map);

    // correct all index numbers
    int size = mimeTypes.getItemCount();
    TreeItem[] parents = mimeTypes.getItems();
    for (int i = 0; i < size; i++) {
      TreeItem parent = parents[i];

      int size2 = parent.getItemCount();
      TreeItem[] children = parent.getItems();
      for (int j = 0; j < size2; j++) {
        Integer in = (Integer) children[j].getData();
        if (in.intValue() >= index2) {
          children[j].setData(new Integer(in.intValue() + 1));
        }
      }
    }

    // check if there is a parent. If so, just add a new child
    for (int i = 0; i < size; i++) {
      TreeItem parent = parents[i];
      if (parent.getText().equals(map.getMimeType())) {
        TreeItem item = new TreeItem(parent, SWT.NONE);
        item.setText(map.getExtension());
        item.setImage(ServerUIPlugin.getImage(ServerUIPlugin.IMG_MIME_EXTENSION));
        item.setData(new Integer(index2));
        mimeTypes.showItem(item);
        return;
      }
    }

    // if not, add a new parent and child to the end
    TreeItem parent = new TreeItem(mimeTypes, SWT.NONE);
    parent.setText(map.getMimeType());
    parent.setImage(ServerUIPlugin.getImage(ServerUIPlugin.IMG_MIME_MAPPING));

    TreeItem item = new TreeItem(parent, SWT.NONE);
    item.setText(map.getExtension());
    item.setImage(ServerUIPlugin.getImage(ServerUIPlugin.IMG_MIME_EXTENSION));
    item.setData(new Integer(index2));
    mimeTypes.showItem(item);
  }

  /**
   * 
   * @param index2
   * @param map
   */
  protected void modifyMimeMapping(int index2, MimeMapping map) {
    MimeMapping oldMap = mappings.get(index2);
    mappings.set(index2, map);

    int size = mimeTypes.getItemCount();
    TreeItem[] parents = mimeTypes.getItems();

    if (oldMap.getMimeType().equals(map.getMimeType())) {
      for (int i = 0; i < size; i++) {
        TreeItem parent = parents[i];

        if (parent.getText().equals(map.getMimeType())) {
          int size2 = parent.getItemCount();
          TreeItem[] children = parent.getItems();
          for (int j = 0; j < size2; j++) {
            Integer in = (Integer) children[j].getData();
            if (in.intValue() == index2) {
              children[j].setText(map.getExtension());
              children[j].setData(new Integer(index2));
              mimeTypes.showItem(children[j]);
              return;
            }
          }
        }
      }
      return;
    }

    // otherwise, let's try a remove and an add
    removeMimeMapping(index2);
    addMimeMapping(index2, map);
  }

  /**
   * Remove the mime mapping at the given index.
   * 
   * @param index2
   */
  protected void removeMimeMapping(int index2) {
    mappings.remove(index2);

    // remove item
    int size = mimeTypes.getItemCount();
    TreeItem[] parents = mimeTypes.getItems();
    for (int i = 0; i < size; i++) {
      TreeItem parent = parents[i];

      int size2 = parent.getItemCount();
      TreeItem[] children = parent.getItems();
      for (int j = 0; j < size2; j++) {
        Integer in = (Integer) children[j].getData();
        if (in.intValue() == index2) {
          children[j].dispose();
          if (size2 == 1) {
            parent.dispose();
          }
          i += size;
          j += size2;
        }
      }
    }

    // correct all index numbers
    size = mimeTypes.getItemCount();
    parents = mimeTypes.getItems();
    for (int i = 0; i < size; i++) {
      TreeItem parent = parents[i];

      int size2 = parent.getItemCount();
      TreeItem[] children = parent.getItems();
      for (int j = 0; j < size2; j++) {
        Integer in = (Integer) children[j].getData();
        if (in.intValue() > index2) {
          children[j].setData(new Integer(in.intValue() - 1));
        }
      }
    }
  }

  /**
   * 
   */
  protected void selectMimeMapping() {
    if (readOnly) {
      return;
    }

    try {
      TreeItem item = mimeTypes.getSelection()[0];
      Integer in = (Integer) item.getData();
      if (in == null) {
        index = -1;
        remove.setEnabled(false);
        edit.setEnabled(false);
        return;
      }
      index = in.intValue();

      remove.setEnabled(true);
      edit.setEnabled(true);
    } catch (Exception e) {
      index = -1;
      remove.setEnabled(false);
      edit.setEnabled(false);
    }
  }
}
