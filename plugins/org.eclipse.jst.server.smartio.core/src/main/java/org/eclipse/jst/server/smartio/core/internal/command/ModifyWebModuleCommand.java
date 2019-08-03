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
import org.eclipse.jst.server.smartio.core.internal.IServerConfiguration;
import org.eclipse.jst.server.smartio.core.internal.WebModule;

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
    newModule = module;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    oldModule = configuration.getWebModules().get(index);
    configuration.modifyWebModule(index, newModule.getDocumentBase(), newModule.getPath(), newModule.isReloadable());
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    configuration.modifyWebModule(index, oldModule.getDocumentBase(), oldModule.getPath(), oldModule.isReloadable());
  }
}