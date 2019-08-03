/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.provisional.ServerLocatorDelegate;

/**
 *
 */
public class ServerLocator extends ServerLocatorDelegate {

  @Override
  public void searchForServers(String host, final IServerSearchListener listener, final IProgressMonitor monitor) {
    ServerRuntimeLocator.IRuntimeSearchListener listener2 = new ServerRuntimeLocator.IRuntimeSearchListener() {

      @Override
      public void runtimeFound(IRuntimeWorkingCopy runtime) {
        String runtimeTypeId = runtime.getRuntimeType().getId();
        String serverTypeId = runtimeTypeId.substring(0, runtimeTypeId.length() - 8);
        IServerType serverType = ServerCore.findServerType(serverTypeId);
        try {
          IServerWorkingCopy server = serverType.createServer(serverTypeId, null, runtime, monitor);
          listener.serverFound(server);
        } catch (Exception e) {
          Trace.trace(Trace.WARNING, "Could not create smart.IO server", e);
        }
      }
    };
    ServerRuntimeLocator.searchForRuntimes2(null, listener2, monitor);
  }
}