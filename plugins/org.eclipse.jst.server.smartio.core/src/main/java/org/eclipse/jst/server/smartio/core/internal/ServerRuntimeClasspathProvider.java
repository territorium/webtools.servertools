/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * Classpath provider for the runtime.
 */
public class ServerRuntimeClasspathProvider extends RuntimeClasspathProviderDelegate {

  private static final String JST_WEB_FACET_ID = "jst.web";

  private final String getEEJavadocLocation(IProject project) {
    int eeVersion = 8;
    try {
      IFacetedProject faceted = ProjectFacetsManager.create(project);
      if ((faceted != null)
          && ProjectFacetsManager.isProjectFacetDefined(ServerRuntimeClasspathProvider.JST_WEB_FACET_ID)) {
        IProjectFacet webModuleFacet =
            ProjectFacetsManager.getProjectFacet(ServerRuntimeClasspathProvider.JST_WEB_FACET_ID);
        if (faceted.hasProjectFacet(webModuleFacet)) {
          String servletVersionStr = faceted.getInstalledVersion(webModuleFacet).getVersionString();
          if (servletVersionStr.equals("4.0")) {
            eeVersion = 8;
          } else if (servletVersionStr.equals("3.1")) {
            eeVersion = 7;
          } else if (servletVersionStr.equals("3.0")) {
            eeVersion = 6;
          } else if (servletVersionStr.equals("2.5")) {
            eeVersion = 5;
          } else if (servletVersionStr.equals("2.4")) {
            eeVersion = 4;
          } else if (servletVersionStr.equals("2.3")) {
            eeVersion = 3;
          }
        }
      }
    } catch (NumberFormatException e) {
      // default to the latest
    } catch (CoreException e) {
      // default to the latest
    }

    String url = "https://javaee.github.io/javaee-spec/javadocs/";
    switch (eeVersion) {
      case 3:
        url = "https://docs.oracle.com/javaee/3/api/";
        break;
      case 4:
        url = "https://docs.oracle.com/javaee/4/api/";
        break;
      case 5:
        url = "https://docs.oracle.com/javaee/5/api/";
        break;
      case 6:
        url = "https://docs.oracle.com/javaee/6/api/";
        break;
      case 7:
        url = "https://docs.oracle.com/javaee/7/api/";
        break;
      case 8:
        url = "https://javaee.github.io/javaee-spec/javadocs/";
        break;
      default:
        url = "https://javaee.github.io/javaee-spec/javadocs/";
        break;
    }

    return url;
  }

  private String getTomcatJavadocLocation(IRuntime runtime) {
    return "https://tomcat.apache.org/tomcat-9.0-doc/api/";
  }

  /**
   * @see RuntimeClasspathProviderDelegate#resolveClasspathContainer(IProject, IRuntime)
   */
  @Override
  public IClasspathEntry[] resolveClasspathContainer(IProject project, IRuntime runtime) {
    IPath installPath = runtime.getLocation();
    if (installPath == null) {
      return new IClasspathEntry[0];
    }

    List<IClasspathEntry> list = new ArrayList<>();
    String runtimeId = runtime.getRuntimeType().getId();
    if (runtimeId.indexOf("10") > 0) {
      // TODO May need some flexibility in case the installation has been configured differently
      // This lib "simplification" may cause issues for some.
      // Not known yet whether packaged Linux installs will go along.
      IPath path = installPath.append("lib");
      RuntimeClasspathProviderDelegate.addLibraryEntries(list, path.toFile(), true);
    } else {
      IPath path = installPath.append("common");
      RuntimeClasspathProviderDelegate.addLibraryEntries(list, path.append("lib").toFile(), true);
      RuntimeClasspathProviderDelegate.addLibraryEntries(list, path.append("endorsed").toFile(), true);
    }

    IClasspathEntry[] entries = list.toArray(new IClasspathEntry[list.size()]);
    String apiJavadocLocation = getEEJavadocLocation(project);
    String javaDocLocation = getTomcatJavadocLocation(runtime);
    for (int i = 0; i < entries.length; i++) {
      IClasspathEntry entry = entries[i];
      String jarName = entry.getPath().lastSegment();
      if (jarName.endsWith("-api.jar") && !jarName.startsWith("tomcat")) {
        // these are assumed to be the API jars for the runtime standards
        IClasspathEntry apiLibraryEntry = JavaCore.newLibraryEntry(entry.getPath(), entry.getSourceAttachmentPath(),
            entry.getSourceAttachmentRootPath(), entry.getAccessRules(),
            new IClasspathAttribute[] { JavaCore
                .newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, apiJavadocLocation) },
            entry.isExported());
        entries[i] = apiLibraryEntry;
      } else {
        IClasspathEntry libraryEntry = JavaCore.newLibraryEntry(entry.getPath(), entry.getSourceAttachmentPath(),
            entry.getSourceAttachmentRootPath(), entry.getAccessRules(),
            new IClasspathAttribute[] { JavaCore
                .newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javaDocLocation) },
            entry.isExported());
        entries[i] = libraryEntry;
      }
    }
    return entries;
  }
}
