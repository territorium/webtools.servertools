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

package org.eclipse.jst.server.smartio.core.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.smartio.core.IConstants;
import org.eclipse.jst.server.smartio.core.Messages;
import org.eclipse.jst.server.smartio.core.ServerPlugin;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  public static IPath getPath(IPath runtimeBase, String directory) {
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
    List<String> arguments = new ArrayList<>();
    simplify(existingArgs).forEach(a -> arguments.add(a));
    simplify(String.join(" ", vmArgs)).stream().filter(a -> !arguments.contains(a)).forEach(a -> arguments.add(a));
    return String.join(" ", arguments);
  }

  private static final Pattern ARG =
      Pattern.compile("([^\\s=\"]+(\\s+|=)(\"[^\"]+\"|[^\\s]+))", Pattern.CASE_INSENSITIVE);

  private static List<String> simplify(String args) {
    if (args == null) {
      return Collections.emptyList();
    }

    List<String> arguments = new ArrayList<>();
    Matcher matcher = ARG.matcher(args);

    while (matcher.find()) {
      arguments.add(matcher.group(1));
    }
    return arguments;
  }

  public static void main(String... args) {
    System.out.println(simplify(
        "-Dsmartio.config=\"/data/smartIO/workspace/runtime-EclipseApplication/Servers/smart.IO v1.0 Server at localhost-config\" -Dsmartio.deploy=\"/data/smartIO/develop/platform/jlink/target/smartIO-19.07.9/webapps\" --add-opens=\"java.base/java.lang=ALL-UNNAMED\" --add-opens=\"java.rmi/sun.rmi.transport=ALL-UNNAMED\" --add-opens=\"java.base/java.io=tomcat.embed\" -m smartio.daemon"));
  }

  /**
   * Merge the given arguments into the original argument string, replacing invalid values if they
   * have been changed. Special handling is provided if the keepActionLast argument is true and the
   * last vmArg is a simple string. The vmArgs will be merged such that the last vmArg is guaranteed
   * to be the last argument in the merged string.
   *
   * @param originalArg String of original arguments.
   * @param vmArgs Arguments to merge into the original arguments string
   * @param excludeArgs Arguments to exclude from the original arguments string
   * @param keepActionLast If <b>true</b> the vmArguments are assumed to be server program
   *        arguments, the last of which is the action to perform which must remain the last
   *        argument. This only has an impact if the last vmArg is a simple string argument, like
   *        &quot;start&quot;.
   * @return merged argument string
   */
  public static String mergeArguments(String originalArg, String[] vmArgs, String[] excludeArgs,
      boolean keepActionLast) {
    if (vmArgs == null) {
      return originalArg;
    }

    if (originalArg == null) {
      originalArg = "";
    }

    // replace and null out all vmargs that already exist
    int size = vmArgs.length;
    for (int i = 0; i < size; i++) {
      int ind = vmArgs[i].indexOf(" ");
      int ind2 = vmArgs[i].indexOf("=");
      if ((ind >= 0) && ((ind2 == -1) || (ind < ind2))) { // -a bc style
        int index = originalArg.indexOf(vmArgs[i].substring(0, ind + 1));
        if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
          // replace
          String s = originalArg.substring(0, index);
          int index2 = ServerTools.getNextToken(originalArg, index + ind + 1);
          if (index2 >= 0) {
            originalArg = s + vmArgs[i] + originalArg.substring(index2);
          } else {
            originalArg = s + vmArgs[i];
          }
          vmArgs[i] = null;
        }
      } else if (ind2 >= 0) { // a=b style
        int index = originalArg.indexOf(vmArgs[i].substring(0, ind2 + 1));
        if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
          // replace
          String s = originalArg.substring(0, index);
          int index2 = ServerTools.getNextToken(originalArg, index);
          if (index2 >= 0) {
            originalArg = s + vmArgs[i] + originalArg.substring(index2);
          } else {
            originalArg = s + vmArgs[i];
          }
          vmArgs[i] = null;
        }
      } else { // abc style
        int index = originalArg.indexOf(vmArgs[i]);
        if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
          // replace
          String s = originalArg.substring(0, index);
          int index2 = ServerTools.getNextToken(originalArg, index);
          if (!keepActionLast || (i < (size - 1))) {
            if (index2 >= 0) {
              originalArg = s + vmArgs[i] + originalArg.substring(index2);
            } else {
              originalArg = s + vmArgs[i];
            }
            vmArgs[i] = null;
          } else {
            // The last VM argument needs to remain last,
            // remove original arg and append the vmArg later
            if (index2 >= 0) {
              originalArg = s + originalArg.substring(index2);
            } else {
              originalArg = s;
            }
          }
        }
      }
    }

    // remove excluded arguments
    if ((excludeArgs != null) && (excludeArgs.length > 0)) {
      for (String excludeArg : excludeArgs) {
        int ind = excludeArg.indexOf(" ");
        int ind2 = excludeArg.indexOf("=");
        if ((ind >= 0) && ((ind2 == -1) || (ind < ind2))) { // -a bc style
          int index = originalArg.indexOf(excludeArg.substring(0, ind + 1));
          if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
            // remove
            String s = originalArg.substring(0, index);
            int index2 = ServerTools.getNextToken(originalArg, index + ind + 1);
            if (index2 >= 0) {
              // If remainder will become the first argument, remove leading
              // blanks
              while ((index2 < originalArg.length()) && Character.isWhitespace(originalArg.charAt(index2))) {
                index2 += 1;
              }
              originalArg = s + originalArg.substring(index2);
            } else {
              originalArg = s;
            }
          }
        } else if (ind2 >= 0) { // a=b style
          int index = originalArg.indexOf(excludeArg.substring(0, ind2 + 1));
          if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
            // remove
            String s = originalArg.substring(0, index);
            int index2 = ServerTools.getNextToken(originalArg, index);
            if (index2 >= 0) {
              // If remainder will become the first argument, remove leading
              // blanks
              while ((index2 < originalArg.length()) && Character.isWhitespace(originalArg.charAt(index2))) {
                index2 += 1;
              }
              originalArg = s + originalArg.substring(index2);
            } else {
              originalArg = s;
            }
          }
        } else { // abc style
          int index = originalArg.indexOf(excludeArg);
          if ((index == 0) || ((index > 0) && Character.isWhitespace(originalArg.charAt(index - 1)))) {
            // remove
            String s = originalArg.substring(0, index);
            int index2 = ServerTools.getNextToken(originalArg, index);
            if (index2 >= 0) {
              // Remove leading blanks
              while ((index2 < originalArg.length()) && Character.isWhitespace(originalArg.charAt(index2))) {
                index2 += 1;
              }
              originalArg = s + originalArg.substring(index2);
            } else {
              originalArg = s;
            }
          }
        }
      }
    }

    // add remaining vmargs to the end
    for (int i = 0; i < size; i++) {
      if (vmArgs[i] != null) {
        if ((originalArg.length() > 0) && !originalArg.endsWith(" ")) {
          originalArg += " ";
        }
        originalArg += vmArgs[i];
      }
    }

    return originalArg;
  }

  private static int getNextToken(String s, int start) {
    int i = start;
    int length = s.length();
    char lookFor = ' ';

    while (i < length) {
      char c = s.charAt(i);
      if (lookFor == c) {
        if (lookFor == '"') {
          return i + 1;
        }
        return i;
      }
      if (c == '"') {
        lookFor = '"';
      }
      i++;
    }
    return -1;
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
