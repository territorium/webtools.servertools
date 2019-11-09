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

import org.eclipse.core.runtime.IPath;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The {@link VMArgsBuilder} class.
 */
public class VMArgsBuilder {

  public static final String BOOT_CLASS   = "it.smartio.daemon.Bootstrap";
  public static final String BOOT_MODULE  = "smartio.daemon";

  public static final String SMARTIO_USER = "smartio.user";
  public static final String SMARTIO_CONF = "smartio.conf";


  private final List<String> vmArgs = new ArrayList<>();
  private final List<String> opens  = new ArrayList<>();

  /**
   * Constructs an instance of {@link VMArgsBuilder}.
   */
  public VMArgsBuilder() {}

  /**
   * Create a VM parameter with pathes.
   *
   * @param name
   * @param value
   */
  public VMArgsBuilder add(String name, String value) {
    vmArgs.add(String.format("-D%s=%s", name, value));
    return this;
  }

  /**
   * Create a VM parameter with pathes.
   *
   * @param name
   * @param path
   */
  public VMArgsBuilder addPath(String name, IPath path) {
    String value = path.toPortableString();
    return add(name, value.contains(" ") ? "\"" + value + "\"" : value);
  }

  /**
   * Opens a package for all unamed modules
   *
   * @param pkg
   */
  public VMArgsBuilder addOpens(String pkg) {
    return addOpens(pkg, "ALL-UNNAMED");
  }

  /**
   * Opens a package for a module
   *
   * @param pkg
   * @param mod
   */
  public VMArgsBuilder addOpens(String pkg, String mod) {
    opens.add(String.format("--add-opens=\"%s=%s\"", pkg, mod));
    return this;
  }

  /**
   * Create a VM parameter with pathes.
   *
   * @param name
   * @param pathes
   */
  public static String of(String name, Object... pathes) {
    return String.format("-D%s=%s", name, Arrays.asList(pathes).stream().map(p -> String.format("\"%s\"", p))
        .collect(Collectors.joining(File.pathSeparator)));
  }


  /**
   * Build the VM arguments using the module name.
   */
  public String[] build(String name) {
    List<String> args = new ArrayList<>();
    args.addAll(vmArgs);
    opens.forEach(o -> args.add(o));
    args.add("-m " + name);
    return args.toArray(new String[args.size()]);
  }
}
