/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
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
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.smartio.core.internal.Messages;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Command to remove a web module from a server.
 */
public class RemoveModuleCommand extends AbstractOperation {

  private final IServerWorkingCopy server;
  private final IModule            module;

  /**
   * AddModuleCommand constructor comment.
   * 
   * @param server a server
   * @param module a web module
   */
  public RemoveModuleCommand(IServerWorkingCopy server, IModule module) {
    super(Messages.configurationEditorActionRemoveWebModule);
    this.server = server;
    this.module = module;
  }

  @Override
  public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    try {
      server.modifyModules(null, new IModule[] { module }, monitor);
    } catch (Exception e) {
      // ignore
    }
    return Status.OK_STATUS;
  }

  @Override
  public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    return execute(monitor, info);
  }

  @Override
  public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    try {
      server.modifyModules(new IModule[] { module }, null, monitor);
    } catch (Exception e) {
      // ignore
    }
    return Status.OK_STATUS;
  }
}
