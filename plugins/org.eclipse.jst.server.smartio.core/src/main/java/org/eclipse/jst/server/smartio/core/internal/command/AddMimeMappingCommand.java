/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others. All rights reserved. This program and the
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
 * Command to add a mime mapping.
 */
public class AddMimeMappingCommand extends ConfigurationCommand {

  private final MimeMapping map;

  /**
   * AddMimeMappingCommand constructor.
   * 
   * @param configuration
   * @param map
   */
  public AddMimeMappingCommand(ServerConfiguration configuration, MimeMapping map) {
    super(configuration, Messages.configurationEditorActionAddMimeMapping);
    this.map = map;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    configuration.addMimeMapping(0, map);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    configuration.removeMimeMapping(0);
  }
}
