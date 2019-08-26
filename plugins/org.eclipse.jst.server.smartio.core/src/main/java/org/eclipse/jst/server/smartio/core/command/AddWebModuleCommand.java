/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others. All rights reserved.
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
 * Command to add a web module.
 */
public class AddWebModuleCommand extends ConfigurationCommand {

  private final WebModule module;
  private int             modules = -1;

  /**
   * AddWebModuleCommand constructor comment.
   *
   * @param configuration
   * @param module a web module
   */
  public AddWebModuleCommand(IServerConfiguration configuration, WebModule module) {
    super(configuration, Messages.configurationEditorActionAddWebModule);
    this.module = module;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    this.modules = this.configuration.getWebModules().size();
    this.configuration.addWebModule(-1, this.module);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    this.configuration.removeWebModule(this.modules);
  }
}
