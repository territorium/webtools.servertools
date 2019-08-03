/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal.command;

import org.eclipse.jst.server.smartio.core.internal.IServerWrapperWorkingCopy;
import org.eclipse.jst.server.smartio.core.internal.Messages;

/**
 * Command to enable or disable serving modules without publishing
 */
public class SetModulesReloadableByDefaultCommand extends ServerCommand {

  private final boolean mrbd;
  private boolean       oldMrbd;

  /**
   * SetSeparateContextFilesCommand constructor comment.
   * 
   * @param server
   * @param mrbd <code>true</code> to enable saving separate context XML files. Otherwise contexts
   *        are kept in server.xml when published.
   */
  public SetModulesReloadableByDefaultCommand(IServerWrapperWorkingCopy server, boolean mrbd) {
    super(server, Messages.serverEditorActionSetModulesReloadableByDefault);
    this.mrbd = mrbd;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    oldMrbd = server.isModulesReloadableByDefault();
    server.setModulesReloadableByDefault(mrbd);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    server.setModulesReloadableByDefault(oldMrbd);
  }
}