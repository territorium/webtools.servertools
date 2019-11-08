/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core;

import org.eclipse.wst.server.core.IModule;

/**
 * A Web module.
 */
public class WebModule {

  // Default web.xml contents for a Servlet 2.5 web application.
  public static final String DEFAULT_WEBXML_SERVLET25 = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
      + "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\" version=\"2.5\">\n"
      + "</web-app>";


  private final String  docBase;
  private final String  path;
  private final String  memento;
  private final boolean reloadable;

  /**
   * WebModule constructor comment.
   *
   * @param path
   * @param docBase
   * @param memento
   * @param reloadable
   */
  public WebModule(String path, String docBase, String memento, boolean reloadable) {
    this.path = path;
    this.docBase = docBase;
    this.memento = memento;
    this.reloadable = reloadable;
  }

  /**
   * Get the document base.
   */
  public final String getDocumentBase() {
    return docBase;
  }

  /**
   * Return the path. (context root)
   */
  public final String getPath() {
    return path;
  }

  /**
   * Return the memento.
   */
  public final String getMemento() {
    return memento;
  }

  /**
   * Return true if the web module is auto-reloadable.
   */
  public final boolean isReloadable() {
    return reloadable;
  }

  /**
   * @see Object#equals(Object)
   */
  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof WebModule)) {
      return false;
    }

    WebModule wm = (WebModule) obj;
    if (!getDocumentBase().equals(wm.getDocumentBase())) {
      return false;
    }
    if (!getPath().equals(wm.getPath())) {
      return false;
    }
    if (!getMemento().equals(wm.getMemento())) {
      return false;
    }
    return true;
  }


  /**
   * @see IServerInstallation#canAddModule(IModule)
   */
  public static boolean canAddModule(IModule module) {
    String version = module.getModuleType().getVersion();
    if ("2.2".equals(version) || "2.3".equals(version) || "2.4".equals(version) || "2.5".equals(version)
        || "3.0".equals(version) || "3.1".equals(version) || "4.0".equals(version)) {
      return true;
    }
    return false;
  }
}