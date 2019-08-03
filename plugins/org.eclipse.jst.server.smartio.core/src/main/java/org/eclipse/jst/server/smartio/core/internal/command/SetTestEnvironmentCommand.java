/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others. All rights reserved. This program and the
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
 * Command to change the server test mode. The server instance directory is cleared in conjunction
 * with this command for legacy support.
 */
public class SetTestEnvironmentCommand extends ServerCommand {

  private final boolean te;
  private boolean       oldTe;
  private String        oldInstanceDir;

  /**
   * SetTestEnvironmentCommand constructor comment.
   * 
   * @param server
   * @param te <code>true</code> for a test environment.
   */
  public SetTestEnvironmentCommand(IServerWrapperWorkingCopy server, boolean te) {
    super(server, Messages.serverEditorActionSetServerDirectory);
    this.te = te;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    oldTe = server.isTestEnvironment();
    // save old instance directory
    oldInstanceDir = server.getInstanceDirectory();
    server.setTestEnvironment(te);
    // ensure instance directory is cleared
    server.setInstanceDirectory(null);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    server.setTestEnvironment(oldTe);
    server.setInstanceDirectory(oldInstanceDir);
  }
}
