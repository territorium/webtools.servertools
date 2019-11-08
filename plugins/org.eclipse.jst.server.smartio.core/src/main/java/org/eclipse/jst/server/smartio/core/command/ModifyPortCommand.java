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

import org.eclipse.jst.server.smartio.core.IServerConfiguration;
import org.eclipse.jst.server.smartio.core.Messages;
import org.eclipse.wst.server.core.ServerPort;

import java.util.Iterator;

/**
 * Command to change the configuration port.
 */
public class ModifyPortCommand extends ConfigurationCommand {

  private final String id;
  private final int    port;
  private int          oldPort;

  /**
   * ModifyPortCommand constructor.
   *
   * @param configuration
   * @param id a port id
   * @param port new port number
   */
  public ModifyPortCommand(IServerConfiguration configuration, String id, int port) {
    super(configuration, Messages.configurationEditorActionModifyPort);
    this.id = id;
    this.port = port;
  }

  /**
   * Execute the command.
   */
  @Override
  public void execute() {
    // find old port number
    Iterator<ServerPort> iterator = configuration.getServerPorts().iterator();
    while (iterator.hasNext()) {
      ServerPort temp = iterator.next();
      if (id.equals(temp.getId())) {
        oldPort = temp.getPort();
      }
    }

    // make the change
    configuration.setServerPort(id, port);
  }

  /**
   * Undo the command.
   */
  @Override
  public void undo() {
    configuration.setServerPort(id, oldPort);
  }
}