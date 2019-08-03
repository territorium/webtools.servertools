/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal.command;

import org.eclipse.jst.server.smartio.core.internal.Messages;
import org.eclipse.jst.server.smartio.core.internal.MimeMapping;
import org.eclipse.jst.server.smartio.core.internal.ServerConfiguration;

/**
 * Command to change a mime type extension.
 */
public class ModifyMimeMappingCommand extends ConfigurationCommand {

  private final int         index;
  private MimeMapping       oldMap;
  private final MimeMapping newMap;

  /**
   * A command to modify a mime mapping.
   * 
   * @param configuration
   * @param index an index
   * @param map a mime mapping
   */
  public ModifyMimeMappingCommand(ServerConfiguration configuration, int index, MimeMapping map) {
    super(configuration, Messages.configurationEditorActionModifyMimeMapping);
    this.index = index;
    newMap = map;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    oldMap = configuration.getMimeMappings().get(index);
    configuration.modifyMimeMapping(index, newMap);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    configuration.modifyMimeMapping(index, oldMap);
  }
}