/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.internet.monitor.core.internal;

import org.eclipse.osgi.util.NLS;
/**
 * Translated messages.
 */
public class Messages extends NLS {
	public static String errorInvalidLocalPort;
	public static String errorInvalidRemotePort;
	public static String errorInvalidRemoteHost;
	public static String errorConnectToServer;
	public static String errorConnectTimeout;
	public static String monitorValid;
	public static String errorPortInUse;

	static {
		NLS.initializeMessages(MonitorPlugin.PLUGIN_ID + ".internal.Messages", Messages.class);
	}
}