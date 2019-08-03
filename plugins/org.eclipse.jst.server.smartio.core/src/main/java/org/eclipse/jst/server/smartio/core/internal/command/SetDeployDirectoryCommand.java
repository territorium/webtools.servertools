/*******************************************************************************
 * Copyright (c) 2007 SAS Institute, Inc. and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Larry Isaacs - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal.command;

import org.eclipse.jst.server.smartio.core.internal.IServerWrapperWorkingCopy;
import org.eclipse.jst.server.smartio.core.internal.Messages;

/**
 * Command to change the deploy directory
 */
public class SetDeployDirectoryCommand extends ServerCommand {

  private final String deployDir;
  private String       oldDeployDir;

  /**
   * Constructs command to set the deploy directory.
   * 
   * @param server
   * @param deployDir deployment directory to set
   */
  public SetDeployDirectoryCommand(IServerWrapperWorkingCopy server, String deployDir) {
    super(server, Messages.serverEditorActionSetDeployDirectory);
    this.deployDir = deployDir;
  }

  /**
   * Execute setting the deploy directory
   */
  @Override
  public void execute() {
    oldDeployDir = server.getDeployDirectory();
    server.setDeployDirectory(deployDir);
  }

  /**
   * Restore prior deploy directory
   */
  @Override
  public void undo() {
    server.setDeployDirectory(oldDeployDir);
  }
}
