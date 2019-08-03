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
public class SetServeModulesWithoutPublishCommand extends ServerCommand {

  private final boolean smwp;
  private boolean       oldSmwp;

  /**
   * SetServeModulesWithoutPublishCommand constructor comment.
   * 
   * @param server
   * @param smwp <code>true</code> to enable serving modules without publishing. Otherwise modules
   *        are served with standard publishing.
   */
  public SetServeModulesWithoutPublishCommand(IServerWrapperWorkingCopy server, boolean smwp) {
    super(server, Messages.serverEditorActionSetServeWithoutPublish);
    this.smwp = smwp;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    oldSmwp = server.isServeModulesWithoutPublish();
    server.setServeModulesWithoutPublish(smwp);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    server.setServeModulesWithoutPublish(oldSmwp);
  }
}