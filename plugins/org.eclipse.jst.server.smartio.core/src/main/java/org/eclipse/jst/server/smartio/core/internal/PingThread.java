/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.jst.server.smartio.core.internal.ServerPlugin.Level;
import org.eclipse.wst.server.core.IServer;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Thread used to ping server to test when it is started.
 */
class PingThread {

  // delay before pinging starts
  private static final int PING_DELAY = 2000;

  // delay between pings
  private static final int PING_INTERVAL = 250;

  // maximum number of pings before giving up
  private final int             maxPings;

  private boolean               stop = false;
  private final String          url;
  private final IServer         server;
  private final ServerBehaviour behaviour;

  /**
   * Create a new PingThread.
   * 
   * @param server
   * @param url
   * @param maxPings the maximum number of times to try pinging, or -1 to
   *        continue forever
   * @param behaviour
   */
  PingThread(IServer server, String url, int maxPings, ServerBehaviour behaviour) {
    super();
    this.server = server;
    this.url = url;
    this.maxPings = maxPings;
    this.behaviour = behaviour;
    Thread t = new Thread("TOL Ping Thread") {

      @Override
      public void run() {
        ping();
      }
    };
    t.setDaemon(true);
    t.start();
  }

  /**
   * Ping the server until it is started. Then set the server state to
   * STATE_STARTED.
   */
  private void ping() {
    int count = 0;
    try {
      Thread.sleep(PingThread.PING_DELAY);
    } catch (Exception e) {
      // ignore
    }
    while (!stop) {
      try {
        if (count == maxPings) {
          try {
            server.stop(false);
          } catch (Exception e) {
            ServerPlugin.log(Level.FINEST, "Ping: could not stop server");
          }
          stop = true;
          break;
        }
        count++;

        ServerPlugin.log(Level.FINEST, "Ping: pinging " + count);
        URL pingUrl = new URL(url);
        URLConnection conn = pingUrl.openConnection();
        ((HttpURLConnection) conn).setInstanceFollowRedirects(false);
        ((HttpURLConnection) conn).getResponseCode();

        // ping worked - server is up
        if (!stop) {
          ServerPlugin.log(Level.FINEST, "Ping: success");
          Thread.sleep(200);
          behaviour.setServerStarted();
        }
        stop = true;
      } catch (FileNotFoundException fe) {
        try {
          Thread.sleep(200);
        } catch (Exception e) {
          // ignore
        }
        behaviour.setServerStarted();
        stop = true;
      } catch (Exception e) {
        ServerPlugin.log(Level.FINEST, "Ping: failed");
        // pinging failed
        if (!stop) {
          try {
            Thread.sleep(PingThread.PING_INTERVAL);
          } catch (InterruptedException e2) {
            // ignore
          }
        }
      }
    }
  }

  /**
   * Tell the pinging to stop.
   */
  void stop() {
    ServerPlugin.log(Level.FINEST, "Ping: stopping");
    stop = true;
  }
}
