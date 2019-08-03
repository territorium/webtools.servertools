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
 * Command to remove a mime mapping.
 */
public class RemoveMimeMappingCommand extends ConfigurationCommand {

  private final int   index;
  private MimeMapping mapping;

  /**
   * RemoveMimeMappingCommand constructor.
   * 
   * @param configuration
   * @param index an index
   */
  public RemoveMimeMappingCommand(ServerConfiguration configuration, int index) {
    super(configuration, Messages.configurationEditorActionRemoveMimeMapping);
    this.index = index;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    mapping = configuration.getMimeMappings().get(index);
    configuration.removeMimeMapping(index);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    configuration.addMimeMapping(index, mapping);
  }
}