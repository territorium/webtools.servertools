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
import org.eclipse.jst.server.smartio.core.internal.ServerConfiguration;
import org.eclipse.jst.server.smartio.core.internal.WebModule;

/**
 * Command to remove a web module.
 */
public class RemoveWebModuleCommand extends ConfigurationCommand {

  private final int index;
  private WebModule module;

  /**
   * RemoveWebModuleCommand constructor comment.
   * 
   * @param configuration
   * @param index an index
   */
  public RemoveWebModuleCommand(ServerConfiguration configuration, int index) {
    super(configuration, Messages.configurationEditorActionRemoveWebModule);
    this.index = index;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    module = configuration.getWebModules().get(index);
    configuration.removeWebModule(index);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    configuration.addWebModule(index, module);
  }
}