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

import org.eclipse.jst.server.smartio.core.internal.IServerWrapper;
import org.eclipse.jst.server.smartio.core.internal.Messages;

/**
 * Command to change the server security option.
 */
public class SetSecureCommand extends ServerCommand {

  private final boolean secure;
  private boolean       oldSecure;

  /**
   * SetSecureCommand constructor comment.
   * 
   * @param server
   * @param secure <code>true</code> for security on
   */
  public SetSecureCommand(IServerWrapper server, boolean secure) {
    super(server, Messages.serverEditorActionSetSecure);
    this.secure = secure;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    oldSecure = server.isSecure();
    server.setSecure(secure);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    server.setSecure(oldSecure);
  }
}