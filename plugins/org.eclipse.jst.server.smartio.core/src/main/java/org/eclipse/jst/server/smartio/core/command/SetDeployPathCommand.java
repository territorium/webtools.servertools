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
public class SetDeployPathCommand extends ServerCommand {

  // private final String newDirectory;
  // private String oldDirectory;

  /**
   * SetSecureCommand constructor comment.
   *
   * @param server
   * @param directory
   */
  public SetDeployPathCommand(IServerWrapper server, String directory) {
    super(server, Messages.serverEditorActionSetSecure);
    // newDirectory = directory;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    // oldDirectory = server.getDeployDirectory();
    // server.setDeployDirectory(newDirectory);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    // server.setDeployDirectory(oldDirectory);
  }
}