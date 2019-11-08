/*
 * Copyright (c) 2001-2019 Territorium Online Srl / TOL GmbH. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as defined in and that are
 * subject to the Territorium Online License Version 1.0. You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at http://www.tol.info/license/
 * and read it before using this file.
 *
 * The Original Code and all software distributed under the License are distributed on an 'AS IS'
 * basis, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, AND TERRITORIUM ONLINE HEREBY
 * DISCLAIMS ALL SUCH WARRANTIES, INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT. Please see the License for
 * the specific language governing rights and limitations under the License.
 */

package org.eclipse.jst.server.smartio.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The {@link ServerTools} class.
 */
public abstract class ServerTools {

  /**
   * Get the absolute {@link IPath}.
   *
   * @param runtimeBase
   * @param directory
   */
  public static boolean isRelativePath(IPath runtimeBase, IPath path) {
    return (path == null) || !path.isAbsolute() || runtimeBase.isPrefixOf(path);
  }

  /**
   * Get the absolute {@link IPath}.
   *
   * @param runtimeBase
   * @param directory
   */
  public static IPath getRelativePath(IPath runtimeBase, IPath path) {
    if (path == null) {
      return null;
    }

    return path.isAbsolute() && runtimeBase.isPrefixOf(path) ? path.makeRelativeTo(runtimeBase) : path;
  }

  /**
   * Get the absolute {@link IPath}.
   *
   * @param runtimeBase
   * @param directory
   */
  public static String getRelativePath(IPath runtimeBase, String directory) {
    if (directory == null) {
      return null;
    }

    IPath path = new Path(directory);
    return (path.isAbsolute() && runtimeBase.isPrefixOf(path) ? path.makeRelativeTo(runtimeBase) : path)
        .toPortableString();
  }

  /**
   * Get the absolute {@link IPath}.
   *
   * @param runtimeBase
   * @param directory
   */
  public static IPath getAbsolutePath(IPath runtimeBase, String directory) {
    IPath path = new Path(directory);
    return path.isAbsolute() ? path : runtimeBase.append(path);
  }

  /**
   * Returns the web modules that the utility module is contained within.
   *
   * @param module
   * @param monitor
   */
  public static IModule[] getWebModules(IModule module) {
    List<IModule> list = new ArrayList<>();
    IModule[] modules = ServerUtil.getModules(IConstants.JST_WEB_FACET_ID);
    if (modules != null) {
      for (IModule module2 : modules) {
        IWebModule web = (IWebModule) module2.loadAdapter(IWebModule.class, null);
        if (web != null) {
          IModule[] modules2 = web.getModules();
          if (modules2 != null) {
            for (IModule m : modules2) {
              if (module.equals(m)) {
                list.add(module2);
              }
            }
          }
        }
      }
    }
    return list.toArray(new IModule[list.size()]);
  }

  /**
   * Merge a single classpath entry into the classpath list.
   *
   * @param entries
   * @param entry
   */
  public static void mergeClasspath(List<IRuntimeClasspathEntry> entries, IRuntimeClasspathEntry entry) {
    for (IRuntimeClasspathEntry e : entries) {
      if (e.getPath().equals(entry.getPath())) {
        return;
      }
    }
    entries.add(entry);
  }

  public static String renderCommandLine(String[] commandLine, String separator) {
    if ((commandLine == null) || (commandLine.length < 1)) {
      return "";
    }
    StringBuffer buf = new StringBuffer(commandLine[0]);
    for (int i = 1; i < commandLine.length; i++) {
      buf.append(separator);
      buf.append(commandLine[i]);
    }
    return buf.toString();
  }

  /**
   * Merge the given arguments into the original argument string, replacing invalid values if they
   * have been changed. Special handling is provided if the keepActionLast argument is true and the
   * last vmArg is a simple string. The vmArgs will be merged such that the last vmArg is guaranteed
   * to be the last argument in the merged string.
   *
   * @param parserdArg String of original arguments.
   * @param vmArgs Arguments to merge into the original arguments string
   */
  public static String mergeArguments(String existingArgs, String[] vmArgs) {
    Map<String, String> map = new HashMap<>();

    int offset = 0;
    while ((existingArgs != null) && (offset >= 0)) {
      offset = ServerTools.parseArgs(map, existingArgs, offset);
    }
    Arrays.asList(vmArgs).forEach(arg -> ServerTools.parseArgs(map, arg, 0));

    return map.keySet().stream().sorted(new Comparator<String>() {

      @Override
      public int compare(String o1, String o2) {
        if (o1.startsWith("-D") && !o2.startsWith("-D")) {
          return -1;
        } else if (!o1.startsWith("-D") && o2.startsWith("-D")) {
          return 1;
        }
        return o1.compareTo(o2);
      }
    }).map(k -> k + map.get(k)).collect(Collectors.joining(" "));
  }

  private static int parseArgs(Map<String, String> args, String text, int offset) {
    int start = ServerTools.nextArg(text, offset);
    if (start < 0) {
      return -1;
    }

    int center = ServerTools.getParam(text, start);

    if (center == 0) {
      args.put(text.substring(start), "");
      return -1;
    }

    int end = ServerTools.getValue(text, center);

    String key = text.substring(start, center);
    String value = text.substring(center, end);

    if (key.startsWith("--")) {
      args.put(key + value, "");
    } else {
      args.put(key, value);
    }
    return end;
  }

  private static int getParam(String text, int offset) {
    int space = text.indexOf(' ', offset) + 1;
    int equal = text.indexOf('=', offset) + 1;
    if ((space > 0) && (equal > 0)) {
      return Math.min(space, equal);
    }
    return (space > 0) ? space : equal;
  }


  private static int getValue(String text, int offset) {
    boolean isString = false;
    while (offset < text.length()) {
      switch (text.charAt(offset)) {
        case ' ':
        case '\t':
        case '\r':
        case '\n':
          if (!isString) {
            return offset;
          }
          break;

        case '"':
          isString = !isString;
          break;
      }
      offset++;
    }
    return offset;
  }


  private static int nextArg(String text, int offset) {
    while (offset < text.length()) {
      switch (text.charAt(offset)) {
        case ' ':
        case '\t':
        case '\r':
        case '\n':
          offset++;
          break;

        default:
          return offset;
      }
    }
    return -1;
  }


  public static void main(String[] args) {


    // System.out.println(mergeArguments(cArgs, vmArgs));

    System.out.println(ServerTools.mergeArguments("--shutdown 5000 start", new String[0]));
  }

  /**
   * Utility method to verify an installation directory according to the specified server ID. The
   * verification includes checking the installation directory name to see if it indicates a
   * different version of the server.
   *
   * @param installPath
   * @param version
   */
  public static IStatus verifyInstallPathWithFolderCheck(IPath installPath, String version) {
    if (version == null) {
      return new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, Messages.errorVersionEmpty, null);
    }
    return (installPath == null) ? ServerPlugin.EmptyInstallDirStatus : Status.OK_STATUS;
  }

  /**
   * Utility method to throw a CoreException based on the contents of a list of error and warning
   * status.
   *
   * @param status a List containing error and warning IStatus
   * @throws CoreException
   */
  public static void throwException(List<IStatus> status) throws CoreException {
    if ((status == null) || (status.size() == 0)) {
      return;
    }

    if (status.size() == 1) {
      IStatus status2 = status.get(0);
      throw new CoreException(status2);
    }
    IStatus[] children = new IStatus[status.size()];
    status.toArray(children);
    String message = Messages.errorPublish;
    MultiStatus status2 = new MultiStatus(ServerPlugin.PLUGIN_ID, 0, children, message, null);
    throw new CoreException(status2);
  }

  public static void addArrayToList(List<IStatus> list, IStatus[] a) {
    if ((list == null) || (a == null) || (a.length == 0)) {
      return;
    }

    int size = a.length;
    for (int i = 0; i < size; i++) {
      list.add(a[i]);
    }
  }
}
