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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jst.server.smartio.core.internal.IServerWrapper;
import org.eclipse.jst.server.smartio.core.internal.ServerWrapper;

/**
 * A command on a smart.IO server.
 */
abstract class ServerCommand extends AbstractOperation {

  protected ServerWrapper server;

  /**
   * ServerCommand constructor comment.
   * 
   * @param server
   * @param label a label
   */
  ServerCommand(IServerWrapper server, String label) {
    super(label);
    this.server = (ServerWrapper) server;
  }

  @Override
  public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    return execute(monitor, info);
  }

  public abstract void execute();

  @Override
  public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    execute();
    return null;
  }

  public abstract void undo();

  @Override
  public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    undo();
    return null;
  }
}