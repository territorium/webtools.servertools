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

import org.eclipse.jst.server.smartio.core.internal.IServerWrapperWorkingCopy;
import org.eclipse.jst.server.smartio.core.internal.Messages;

/**
 * Command to change the server debug mode.
 */
public class SetDebugModeCommand extends ServerCommand {

  private final boolean debug;
  private boolean       oldDebug;

  /**
   * SetDebugModeCommand constructor comment.
   * 
   * @param server
   * @param debug <code>true</code> for debug mode
   */
  public SetDebugModeCommand(IServerWrapperWorkingCopy server, boolean debug) {
    super(server, Messages.serverEditorActionSetDebugMode);
    this.debug = debug;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    oldDebug = server.isDebug();
    server.setDebug(debug);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    server.setDebug(oldDebug);
  }
}