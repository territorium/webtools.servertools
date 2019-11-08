/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.runtime;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.jst.server.smartio.core.IConstants;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IRuntime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class-Path provider for the runtime.
 */
public class ClasspathProvider extends RuntimeClasspathProviderDelegate {

  /**
   * Get the class path, when the server is used in other projects as class provider.
   *
   * @param project
   * @param runtime
   */
  @Override
  public IClasspathEntry[] resolveClasspathContainer(IProject project, IRuntime runtime) {
    IPath location = runtime.getLocation();
    if (location == null) {
      return new IClasspathEntry[0];
    }

    List<IClasspathEntry> list = new ArrayList<>();
    String runtimeId = runtime.getRuntimeType().getId();
    if (runtimeId.indexOf("10") > 0) {
      File file = location.append("lib").toFile();
      RuntimeClasspathProviderDelegate.addLibraryEntries(list, file, true);
    }

    IClasspathEntry[] entries = list.toArray(new IClasspathEntry[list.size()]);
    String javadoc = ClasspathProvider.getEEJavaDocLocation(project);
    for (int i = 0; i < entries.length; i++) {
      IClasspathEntry entry = entries[i];
      String jarName = entry.getPath().lastSegment();
      if (jarName.endsWith("-api.jar")) {
        // these are assumed to be the API jars for the runtime standards
        IClasspathAttribute classpathAttr =
            JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadoc);
        IClasspathEntry classpathEntry = JavaCore.newLibraryEntry(entry.getPath(), entry.getSourceAttachmentPath(),
            entry.getSourceAttachmentRootPath(), entry.getAccessRules(), new IClasspathAttribute[] { classpathAttr },
            entry.isExported());
        entries[i] = classpathEntry;
      }
    }
    return entries;
  }

  /**
   * Get the JEE version from the {@link IProject}.
   *
   * @param project
   */
  private static int getEEVersion(IProject project) {
    try {
      IFacetedProject faceted = ProjectFacetsManager.create(project);
      if ((faceted != null) && ProjectFacetsManager.isProjectFacetDefined(IConstants.JST_WEB_FACET_ID)) {
        IProjectFacet webModuleFacet = ProjectFacetsManager.getProjectFacet(IConstants.JST_WEB_FACET_ID);
        if (faceted.hasProjectFacet(webModuleFacet)) {
          String servletVersionStr = faceted.getInstalledVersion(webModuleFacet).getVersionString();
          if (servletVersionStr.equals("4.0")) {
            return 8;
          } else if (servletVersionStr.equals("3.1")) {
            return 7;
          } else if (servletVersionStr.equals("3.0")) {
            return 6;
          } else if (servletVersionStr.equals("2.5")) {
            return 5;
          } else if (servletVersionStr.equals("2.4")) {
            return 4;
          } else if (servletVersionStr.equals("2.3")) {
            return 3;
          }
        }
      }
    } catch (NumberFormatException e) {
      // default to the latest
    } catch (CoreException e) {
      // default to the latest
    }
    return 8;
  }

  /**
   * Get the location of the JEE JavaDocs.
   *
   * @param project
   */
  private static String getEEJavaDocLocation(IProject project) {
    switch (ClasspathProvider.getEEVersion(project)) {
      case 3:
        return "https://docs.oracle.com/javaee/3/api/";
      case 4:
        return "https://docs.oracle.com/javaee/4/api/";
      case 5:
        return "https://docs.oracle.com/javaee/5/api/";
      case 6:
        return "https://docs.oracle.com/javaee/6/api/";
      case 7:
        return "https://docs.oracle.com/javaee/7/api/";
      case 8:
      default:
    }
    return "https://javaee.github.io/javaee-spec/javadocs/";
  }
}
