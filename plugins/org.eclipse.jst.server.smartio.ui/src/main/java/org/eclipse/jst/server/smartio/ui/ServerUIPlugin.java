/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.server.core.IServer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * The actual {@link ServerUIPlugin}. It allows the rest of the classes to load images and get a
 * handle to the desktop.
 */
public class ServerUIPlugin extends AbstractUIPlugin {

  private static ServerUIPlugin              singleton;

  private final Map<String, ImageDescriptor> imageDescriptors = new HashMap<>();

  // base url for icons
  private static URL          ICON_BASE_URL;

  private static final String URL_OBJ             = "obj16/";
  private static final String URL_WIZBAN          = "wizban/";

  public static final String  PLUGIN_ID           = "org.eclipse.jst.server.smartio.ui";

  static final String         IMG_WIZ             = "wizSmartIO";

  public static final String  IMG_WEB_MODULE      = "webModule";
  public static final String  IMG_MIME_MAPPING    = "mimeMapping";
  public static final String  IMG_MIME_EXTENSION  = "mimeExtension";
  public static final String  IMG_PORT            = "port";
  public static final String  IMG_PROJECT_MISSING = "projectMissing";

  /**
   * {@link ServerUIPlugin} constructor comment.
   */
  public ServerUIPlugin() {
    super();
    ServerUIPlugin.singleton = this;
  }

  @Override
  protected ImageRegistry createImageRegistry() {
    ImageRegistry registry = new ImageRegistry();

    registerImage(registry, ServerUIPlugin.IMG_WIZ, ServerUIPlugin.URL_WIZBAN + "smartio_wiz.png");

    registerImage(registry, ServerUIPlugin.IMG_WEB_MODULE, ServerUIPlugin.URL_OBJ + "web_module.gif");
    registerImage(registry, ServerUIPlugin.IMG_MIME_MAPPING, ServerUIPlugin.URL_OBJ + "mime_mapping.gif");
    registerImage(registry, ServerUIPlugin.IMG_MIME_EXTENSION, ServerUIPlugin.URL_OBJ + "mime_extension.gif");
    registerImage(registry, ServerUIPlugin.IMG_PORT, ServerUIPlugin.URL_OBJ + "port.gif");
    registerImage(registry, ServerUIPlugin.IMG_PROJECT_MISSING, ServerUIPlugin.URL_OBJ + "project_missing.gif");

    return registry;
  }

  /**
   * Return the image with the given key from the image registry.
   *
   * @param key java.lang.String
   * @return org.eclipse.jface.parts.IImage
   */
  public static Image getImage(String key) {
    return ServerUIPlugin.getInstance().getImageRegistry().get(key);
  }

  /**
   * Return the image with the given key from the image registry.
   *
   * @param key java.lang.String
   * @return org.eclipse.jface.parts.IImage
   */
  static ImageDescriptor getImageDescriptor(String key) {
    try {
      ServerUIPlugin.getInstance().getImageRegistry();
      return ServerUIPlugin.getInstance().imageDescriptors.get(key);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the singleton instance of this plugin.
   */
  static ServerUIPlugin getInstance() {
    return ServerUIPlugin.singleton;
  }


  /**
   * Convenience method to get a Display. The method first checks, if the thread calling this method
   * has an associated display. If so, this display is returned. Otherwise the method returns the
   * default display.
   *
   * @return the display
   */
  private static Display getStandardDisplay() {
    Display display = Display.getCurrent();
    if (display == null) {
      display = Display.getDefault();
    }
    return display;
  }


  static boolean queryCleanTermination(IServer server) {
    CleanTerminationRunnable tr = new CleanTerminationRunnable(server);
    Display.getDefault().syncExec(tr);
    return tr.shouldTerminate();
  }

  private static class CleanTerminationRunnable implements Runnable {

    private final IServer server;
    private boolean       terminate;

    private CleanTerminationRunnable(IServer server) {
      this.server = server;
    }

    @Override
    public void run() {
      Shell shell = ServerUIPlugin.getShell();
      TerminationDialog dialog = new TerminationDialog(shell, Messages.cleanTerminateServerDialogTitle,
          NLS.bind(Messages.cleanTerminateServerDialogMessage, server.getName()));
      dialog.open();
      if (dialog.getReturnCode() == IDialogConstants.OK_ID) {
        terminate = true;
      }
    }

    private boolean shouldTerminate() {
      return terminate;
    }
  }

  /**
   * Convenience method to get a shell
   *
   * @return Shell
   */
  private static Shell getShell() {
    return ServerUIPlugin.getStandardDisplay().getActiveShell();
  }

  /**
   * Register an image with the registry.
   *
   * @param key java.lang.String
   * @param partialURL java.lang.String
   */
  private void registerImage(ImageRegistry registry, String key, String partialURL) {
    if (ServerUIPlugin.ICON_BASE_URL == null) {
      String pathSuffix = "icons/";
      ServerUIPlugin.ICON_BASE_URL = ServerUIPlugin.singleton.getBundle().getEntry(pathSuffix);
    }

    try {
      ImageDescriptor id = ImageDescriptor.createFromURL(new URL(ServerUIPlugin.ICON_BASE_URL, partialURL));
      registry.put(key, id);
      imageDescriptors.put(key, id);
    } catch (Exception e) {
      Trace.trace(Trace.WARNING, "Error registering image", e);
    }
  }
}
