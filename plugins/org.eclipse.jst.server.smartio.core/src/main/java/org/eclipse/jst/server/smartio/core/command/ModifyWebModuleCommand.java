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
 * Command to change a web module.
 */
public class ModifyWebModuleCommand extends ConfigurationCommand {

  private final int       index;
  private WebModule       oldModule;
  private final WebModule newModule;

  public ModifyWebModuleCommand(IServerConfiguration configuration, int index, WebModule module) {
    super(configuration, Messages.configurationEditorActionModifyWebModule);
    this.index = index;
    this.newModule = module;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    this.oldModule = this.configuration.getWebModules().get(this.index);
    this.configuration.modifyWebModule(this.index, this.newModule.getDocumentBase(), this.newModule.getPath(),
        this.newModule.isReloadable());
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    this.configuration.modifyWebModule(this.index, this.oldModule.getDocumentBase(), this.oldModule.getPath(),
        this.oldModule.isReloadable());
  }
}