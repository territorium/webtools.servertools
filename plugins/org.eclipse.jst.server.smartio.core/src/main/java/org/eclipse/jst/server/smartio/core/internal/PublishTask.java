/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.PublishTaskDelegate;

import java.util.ArrayList;
import java.util.List;

public class PublishTask extends PublishTaskDelegate {

  @Override
  public PublishOperation[] getTasks(IServer server, int kind, List modules, List kindList) {
    if (modules == null) {
      return null;
    }

    ServerBehaviour wrapper = (ServerBehaviour) server.loadAdapter(ServerBehaviour.class, null);
    if (!wrapper.getWrapper().isTestEnvironment()) {
      return null;
    }

    List<PublishOperation> tasks = new ArrayList<>();
    int size = modules.size();
    for (int i = 0; i < size; i++) {
      IModule[] module = (IModule[]) modules.get(i);
      Integer in = (Integer) kindList.get(i);
      tasks.add(new PublishOperation2(wrapper, kind, module, in.intValue()));
    }

    return tasks.toArray(new PublishOperation[tasks.size()]);
  }
}