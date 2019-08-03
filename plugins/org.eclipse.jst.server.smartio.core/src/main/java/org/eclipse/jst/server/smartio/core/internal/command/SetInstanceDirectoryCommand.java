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
public class SetInstanceDirectoryCommand extends ServerCommand {

  private final String instanceDir;
  private String       oldInstanceDir;
  private boolean      oldTestEnvironment;

  /**
   * Constructs command to set the instance directory. Setting the instance directory also sets
   * testEnvironment true;
   * 
   * @param server
   * @param instanceDir instance directory to set
   */
  public SetInstanceDirectoryCommand(IServerWrapperWorkingCopy server, String instanceDir) {
    super(server, Messages.serverEditorActionSetServerDirectory);
    this.instanceDir = instanceDir;
  }

  /**
   * Execute setting the deploy directory
   */
  @Override
  public void execute() {
    oldTestEnvironment = server.isTestEnvironment();
    oldInstanceDir = server.getInstanceDirectory();
    if (!oldTestEnvironment) {
      server.setTestEnvironment(true);
    }
    server.setInstanceDirectory(instanceDir);
  }

  /**
   * Restore prior deploy directory
   */
  @Override
  public void undo() {
    if (!oldTestEnvironment) {
      server.setTestEnvironment(false);
    }
    server.setInstanceDirectory(oldInstanceDir);
  }
}
