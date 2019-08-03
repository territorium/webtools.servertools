/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.command;

import org.eclipse.jst.server.smartio.core.IServerConfiguration;
import org.eclipse.jst.server.smartio.core.Messages;
import org.eclipse.jst.server.smartio.core.WebModule;

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
  public RemoveWebModuleCommand(IServerConfiguration configuration, int index) {
    super(configuration, Messages.configurationEditorActionRemoveWebModule);
    this.index = index;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    this.module = this.configuration.getWebModules().get(this.index);
    this.configuration.removeWebModule(this.index);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    this.configuration.addWebModule(this.index, this.module);
  }
}