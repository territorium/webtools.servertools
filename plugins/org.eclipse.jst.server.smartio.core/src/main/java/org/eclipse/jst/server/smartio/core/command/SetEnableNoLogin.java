/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.command;

import org.eclipse.jst.server.smartio.core.IServerWrapper;
import org.eclipse.jst.server.smartio.core.Messages;

/**
 * Command to change the server security option.
 */
public class SetEnableNoLogin extends ServerCommand {

  private final boolean newValue;
  private boolean       oldValue;

  /**
   * SetSecureCommand constructor comment.
   *
   * @param server
   * @param value
   */
  public SetEnableNoLogin(IServerWrapper server, boolean value) {
    super(server, Messages.serverEditorActionSetSecure);
    newValue = value;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    oldValue = server.isNoLogin();
    server.setNoLogin(newValue);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    server.setNoLogin(oldValue);
  }
}