/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This program and the
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
public class SetSaveSeparateContextFilesCommand extends ServerCommand {

  private final boolean sscf;
  private boolean       oldSscf;

  /**
   * SetSeparateContextFilesCommand constructor comment.
   * 
   * @param server
   * @param sscf <code>true</code> to enable saving separate context XML files. Otherwise contexts
   *        are kept in server.xml when published.
   */
  public SetSaveSeparateContextFilesCommand(IServerWrapperWorkingCopy server, boolean sscf) {
    super(server, Messages.serverEidtorActionSetSeparateContextFiles);
    this.sscf = sscf;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    oldSscf = server.isSaveSeparateContextFiles();
    server.setSaveSeparateContextFiles(sscf);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    server.setSaveSeparateContextFiles(oldSscf);
  }
}