/**********************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Igor Fedorenko & Fabrizio Giustina - Initial API and implementation
 **********************************************************************/

package org.eclipse.jst.server.smartio.core.runtime;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.server.smartio.core.IConstants;
import org.eclipse.jst.server.smartio.core.ServerPlugin;
import org.eclipse.jst.server.smartio.core.ServerPlugin.Level;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.UnresolveableURIException;
import org.eclipse.wst.common.componentcore.internal.ComponentResource;
import org.eclipse.wst.common.componentcore.internal.ReferencedComponent;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.eclipse.wst.common.componentcore.internal.impl.ModuleURIUtil;
import org.eclipse.wst.common.componentcore.internal.impl.PlatformURLModuleConnection;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.server.core.IModule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Temporary solution for https://bugs.eclipse.org/bugs/show_bug.cgi?id=103888
 */
@SuppressWarnings("restriction")
public class ModuleTraverser {

  /**
   * Facet type for Web modules
   */
  private static final String WEB_MODULE                                               = IConstants.JST_WEB_MODULE;

  /**
   * Name of the custom Java classpath entry attribute that is used to flag entries which should be
   * exposed as module dependencies via the virtual component API.
   */
  private static final String CLASSPATH_COMPONENT_DEPENDENCY                           =
      "org.eclipse.jst.component.dependency";                                                                      // $NON-NLS-1

  /**
   * Name of the custom Java classpath entry attribute that is used to flag the resolved entries of
   * classpath containers that should not be exposed via the virtual component API.
   */
  private static final String CLASSPATH_COMPONENT_NON_DEPENDENCY                       =
      "org.eclipse.jst.component.nondependency";                                                                   // $NON-NLS-1

  /**
   * Argument values that are used to select component dependency attribute type.
   */
  private static final int    DEPENDECYATTRIBUTETYPE_DEPENDENCY_OR_NONDEPENDENCY       = 0;
  private static final int    DEPENDECYATTRIBUTETYPE_CLASSPATH_COMPONENT_DEPENDENCY    = 1;
  private static final int    DEPENDECYATTRIBUTETYPE_CLASSPATH_COMPONENT_NONDEPENDENCY = 2;

  /**
   * Scans the module using the specified visitor.
   *
   * @param module module to traverse
   * @param visitor visitor to handle resources
   * @param monitor a progress monitor
   * @throws CoreException
   */
  public static void traverse(IModule module, IModuleVisitor visitor, IProgressMonitor monitor) throws CoreException {
    if ((module == null) || (module.getModuleType() == null)) {
      return;
    }

    String typeId = module.getModuleType().getId();
    IVirtualComponent component = ComponentCore.createComponent(module.getProject());

    if (component == null) {
      // can happen if project has been closed
      ServerPlugin.log(Level.WARNING, "Unable to create component for module " + module.getName());
      return;
    }

    if (ModuleTraverser.WEB_MODULE.equals(typeId)) {
      ModuleTraverser.traverseWebComponent(component, visitor, monitor);
    }
  }

  private static void traverseWebComponent(IVirtualComponent component, IModuleVisitor visitor,
      IProgressMonitor monitor) throws CoreException {

    visitor.visitWebComponent(component);

    IProject proj = component.getProject();
    StructureEdit warStruct = StructureEdit.getStructureEditForRead(proj);
    try {
      WorkbenchComponent comp = warStruct.getComponent();
      if (comp == null) {
        ServerPlugin.log(Level.SEVERE, "Error getting WorkbenchComponent from war project. IProject=\"" + proj
            + "\" StructureEdit=\"" + warStruct + "\" WorkbenchComponent=\"" + comp + "\"");
        return;
      }
      ModuleTraverser.traverseWebComponentLocalEntries(comp, visitor, monitor);

      // traverse referenced components
      @SuppressWarnings("unchecked")
      EList<ReferencedComponent> children = comp.getReferencedComponents();
      for (ReferencedComponent childRef : children) {
        IPath rtFolder = childRef.getRuntimePath();
        URI refHandle = childRef.getHandle();

        if (PlatformURLModuleConnection.CLASSPATH
            .equals(refHandle.segment(ModuleURIUtil.ModuleURI.SUB_PROTOCOL_INDX))) {
          IPath refPath = ModuleTraverser.getResolvedPathForArchiveComponent(refHandle);
          // If an archive component, add to list
          if (refPath != null) {
            if (!refPath.isAbsolute()) {
              IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(refPath);
              IPath refPath2 = file.getLocation();
              if (refPath2 != null) {
                visitor.visitArchiveComponent(rtFolder, refPath2);
              } else {
                ServerPlugin.log(Level.WARNING, NLS.bind(
                    "Could not get the location of a referenced component.  It may not exist.  Project={0}, Parent Component={1}, Referenced Component Path={2}",
                    new Object[] { proj.getName(), comp.getName(), refPath }));
              }
            } else {
              visitor.visitArchiveComponent(rtFolder, refPath);
            }
          } else {
            // TODO Determine if any use case would arrive here.
          }
        } else {
          try {
            WorkbenchComponent childCom = warStruct.findComponentByURI(refHandle);
            if (childCom == null) {
              continue;
            }

            ModuleTraverser.traverseDependentEntries(visitor, rtFolder, childCom, monitor);
          } catch (UnresolveableURIException e) {
            ServerPlugin.log(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e));
          }
        }
      }
    } finally {
      warStruct.dispose();
    }

    visitor.endVisitWebComponent(component);
  }

  private static void traverseWebComponentLocalEntries(WorkbenchComponent comp, IModuleVisitor visitor,
      IProgressMonitor monitor) throws CoreException {
    IProject warProject = StructureEdit.getContainingProject(comp);
    if ((warProject == null) || !warProject.hasNature(JavaCore.NATURE_ID)) {
      return;
    }
    IJavaProject project = JavaCore.create(warProject);

    @SuppressWarnings("unchecked")
    EList<ComponentResource> res = comp.getResources();
    for (ComponentResource childComp : res) {
      IClasspathEntry cpe = ModuleTraverser.getClasspathEntry(project, childComp.getSourcePath());
      if (cpe == null) {
        continue;
      }
      visitor.visitWebResource(childComp.getRuntimePath(),
          ModuleTraverser.getOSPath(warProject, project, cpe.getOutputLocation()));
    }

    // Include tagged classpath entries
    Map<IClasspathEntry, IClasspathAttribute> classpathDeps =
        ModuleTraverser.getComponentClasspathDependencies(project, true);
    for (IClasspathEntry entry : classpathDeps.keySet()) {
      IClasspathAttribute attrib = classpathDeps.get(entry);
      boolean isClassFolder = ModuleTraverser.isClassFolderEntry(entry);
      String rtFolder = attrib.getValue();
      if (rtFolder == null) {
        if (isClassFolder) {
          rtFolder = "/WEB-INF/classes";
        } else {
          rtFolder = "/WEB-INF/lib";
        }
      }
      IPath entryPath = entry.getPath();
      IResource entryRes = ResourcesPlugin.getWorkspace().getRoot().findMember(entryPath);
      if (entryRes != null) {
        entryPath = entryRes.getLocation();
      }
      // TODO Determine if different handling is needed for some use cases
      if (isClassFolder) {
        visitor.visitWebResource(new Path(rtFolder), ModuleTraverser.getOSPath(warProject, project, entry.getPath()));
      } else {
        visitor.visitArchiveComponent(new Path(rtFolder), entryPath);
      }
    }
  }

  private static void traverseDependentEntries(IModuleVisitor visitor, IPath runtimeFolder,
      WorkbenchComponent component, IProgressMonitor monitor) throws CoreException {
    IProject dependentProject = StructureEdit.getContainingProject(component);
    if (!dependentProject.hasNature(JavaCore.NATURE_ID)) {
      return;
    }
    IJavaProject project = JavaCore.create(dependentProject);
    visitor.visitDependentJavaProject(project);

    String name = component.getName(); // assume it is the same as URI

    // go thru all entries
    @SuppressWarnings("unchecked")
    EList<ComponentResource> res = component.getResources();
    for (ComponentResource childComp : res) {
      IPath rtPath = childComp.getRuntimePath();
      IPath srcPath = childComp.getSourcePath();
      IClasspathEntry cpe = ModuleTraverser.getClasspathEntry(project, srcPath);
      if (cpe != null) {
        visitor.visitDependentComponent(runtimeFolder.append(rtPath).append(name + ".jar"),
            ModuleTraverser.getOSPath(dependentProject, project, cpe.getOutputLocation()));
      }
      // Handle META-INF/resources
      String path = rtPath.toString();
      IFolder resFolder = null;
      String targetPath = "";
      if ("/".equals(path)) {
        resFolder = dependentProject.getFolder(srcPath.append("META-INF/resources"));
      } else if ("/META-INF".equals(path)) {
        resFolder = dependentProject.getFolder(srcPath.append("resources"));
      } else if ("/META-INF/resources".equals(path)) {
        resFolder = dependentProject.getFolder(srcPath);
      } else if (path.startsWith("/META-INF/resources/")) {
        resFolder = dependentProject.getFolder(srcPath);
        targetPath = path.substring("/META-INF/resources".length());
      }
      if ((resFolder != null) && resFolder.exists()) {
        visitor.visitDependentContentResource(new Path(targetPath), resFolder.getLocation());
      }
    }

    // Include tagged classpath entries
    Map<IClasspathEntry, ?> classpathDeps = ModuleTraverser.getComponentClasspathDependencies(project, false);
    for (IClasspathEntry entry : classpathDeps.keySet()) {
      boolean isClassFolder = ModuleTraverser.isClassFolderEntry(entry);
      String rtFolder = null;
      if (isClassFolder) {
        rtFolder = "/";
      } else {
        rtFolder = "/WEB-INF/lib";
      }
      IPath entryPath = entry.getPath();
      IResource entryRes = ResourcesPlugin.getWorkspace().getRoot().findMember(entryPath);
      if (entryRes != null) {
        entryPath = entryRes.getLocation();
      }
      // TODO Determine if different handling is needed for some use cases
      if (isClassFolder) {
        visitor.visitDependentComponent(runtimeFolder.append(rtFolder).append(name + ".jar"),
            ModuleTraverser.getOSPath(dependentProject, project, entry.getPath()));
      } else {
        visitor.visitArchiveComponent(new Path(rtFolder), entryPath);
      }
    }
  }

  private static IClasspathEntry getClasspathEntry(IJavaProject project, IPath sourcePath) throws JavaModelException {
    sourcePath = project.getPath().append(sourcePath);
    IClasspathEntry[] cp = project.getRawClasspath();
    for (IClasspathEntry element : cp) {
      if (sourcePath.equals(element.getPath())) {
        return JavaCore.getResolvedClasspathEntry(element);
      }
    }
    return null;
  }

  private static IPath getOSPath(IProject project, IJavaProject javaProject, IPath outputPath)
      throws JavaModelException {
    if (outputPath == null) {
      outputPath = javaProject.getOutputLocation();
    }
    // If we have the root of a project, return project location
    if (outputPath.segmentCount() == 1) {
      return ResourcesPlugin.getWorkspace().getRoot().getProject(outputPath.lastSegment()).getLocation();
    }
    // Otherwise return project folder location
    return ResourcesPlugin.getWorkspace().getRoot().getFolder(outputPath).getLocation();
  }

  /*
   * Derived from J2EEProjectUtilities.getResolvedPathForArchiveComponent()
   */
  private static IPath getResolvedPathForArchiveComponent(URI uri) {

    String resourceType = uri.segment(1);
    URI contenturi = ModuleURIUtil.trimToRelativePath(uri, 2);
    String contentName = contenturi.toString();

    if (resourceType.equals("lib")) { //$NON-NLS-1$
      // module:/classpath/lib/D:/foo/foo.jar
      return Path.fromOSString(contentName);

    } else if (resourceType.equals("var")) { //$NON-NLS-1$

      // module:/classpath/var/<CLASSPATHVAR>/foo.jar
      String classpathVar = contenturi.segment(0);
      URI remainingPathuri = ModuleURIUtil.trimToRelativePath(contenturi, 1);
      String remainingPath = remainingPathuri.toString();

      String[] classpathvars = JavaCore.getClasspathVariableNames();
      boolean found = false;
      for (String classpathvar2 : classpathvars) {
        if (classpathVar.equals(classpathvar2)) {
          found = true;
          break;
        }
      }
      if (found) {
        IPath path = JavaCore.getClasspathVariable(classpathVar);
        if (path != null) {
          URI finaluri = URI.createURI(path.toOSString() + IPath.SEPARATOR + remainingPath);
          return Path.fromOSString(finaluri.toString());
        }
      }
      ServerPlugin.log(Level.WARNING, NLS.bind(
          "smart.IO publishing could not resolve dependency URI \"{0}\".  A value for classpath variable {1} was not found.",
          uri, classpathVar));
    }
    return null;
  }

  /*
   * Derived from ClasspathDependencyUtil.getComponentClasspathDependencies()
   */
  private static Map<IClasspathEntry, IClasspathAttribute> getComponentClasspathDependencies(
      final IJavaProject javaProject, final boolean isWebApp) throws CoreException {

    // get the raw entries
    final Map<IClasspathEntry, IClasspathAttribute> referencedRawEntries =
        ModuleTraverser.getRawComponentClasspathDependencies(javaProject);
    final Map<IClasspathEntry, IClasspathAttribute> validRawEntries = new HashMap<>();

    // filter out non-valid referenced raw entries
    final Iterator<IClasspathEntry> i = referencedRawEntries.keySet().iterator();
    while (i.hasNext()) {
      final IClasspathEntry entry = i.next();
      final IClasspathAttribute attrib = referencedRawEntries.get(entry);
      if (ModuleTraverser.isValid(entry, attrib, isWebApp, javaProject.getProject())) {
        validRawEntries.put(entry, attrib);
      }
    }

    // if we have no valid raw entries, return empty map
    if (validRawEntries.isEmpty()) {
      return Collections.emptyMap();
    }

    // XXX Would like to replace the code below with use of a public JDT API
    // that returns
    // the raw IClasspathEntry for a given resolved IClasspathEntry (see see
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=183995)
    // The code must currently leverage IPackageFragmentRoot to determine this
    // mapping and, because IPackageFragmentRoots do not maintain
    // IClasspathEntry data, a prior
    // call is needed to getResolvedClasspath() and the resolved
    // IClasspathEntries have to be stored
    // in a Map from IPath-to-IClasspathEntry to
    // support retrieval using the resolved IPackageFragmentRoot

    // retrieve the resolved classpath
    final IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
    final Map<IPath, IClasspathEntry> pathToResolvedEntry = new HashMap<>();

    // store in a map from path to entry
    // Note: We need to check the non-dependency attribute for each entry since
    // it
    // might be a child of a classpath container.
    for (IClasspathEntry entrie : entries) {
      IClasspathAttribute attrib = ModuleTraverser.checkForComponentDependencyAttribute(entrie,
          ModuleTraverser.DEPENDECYATTRIBUTETYPE_CLASSPATH_COMPONENT_NONDEPENDENCY);
      // If not a non-dependency (i.e. not excluded), then add this entry.
      if (attrib == null) {
        pathToResolvedEntry.put(entrie.getPath(), entrie);
      }
    }

    final Map<IClasspathEntry, IClasspathAttribute> referencedEntries = new LinkedHashMap<>();

    // grab all IPackageFragmentRoots
    final IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
    for (final IPackageFragmentRoot root : roots) {
      final IClasspathEntry rawEntry = root.getRawClasspathEntry();

      // is the raw entry valid?
      IClasspathAttribute attrib = validRawEntries.get(rawEntry);
      if (attrib == null) {
        continue;
      }

      final IPath pkgFragPath = root.getPath();
      final IClasspathEntry resolvedEntry = pathToResolvedEntry.get(pkgFragPath);
      // If the resolvedEntry is not present for this path, then it was excluded
      // above due to being
      // a non-dependency
      if (resolvedEntry == null) {
        continue;
      }
      final IClasspathAttribute resolvedAttrib = ModuleTraverser.checkForComponentDependencyAttribute(resolvedEntry,
          ModuleTraverser.DEPENDECYATTRIBUTETYPE_DEPENDENCY_OR_NONDEPENDENCY);
      // attribute for the resolved entry must either be unspecified or it must
      // be the
      // dependency attribute for it to be included
      if ((resolvedAttrib == null) || resolvedAttrib.getName().equals(ModuleTraverser.CLASSPATH_COMPONENT_DEPENDENCY)) {
        // filter out resolved entry if it doesn't pass the validation rules
        if (ModuleTraverser.isValid(resolvedEntry, resolvedAttrib != null ? resolvedAttrib : attrib, isWebApp,
            javaProject.getProject())) {
          if (resolvedAttrib != null) {
            // if there is an attribute on the sub-entry, use that
            attrib = resolvedAttrib;
          }
          referencedEntries.put(resolvedEntry, attrib);
        }
      }
    }

    return referencedEntries;
  }

  /*
   * Derived from ClasspathDependencyUtil.getRawComponentClasspathDependencies()
   */
  private static Map<IClasspathEntry, IClasspathAttribute> getRawComponentClasspathDependencies(
      final IJavaProject javaProject) throws CoreException {
    if (javaProject == null) {
      return Collections.emptyMap();
    }
    final Map<IClasspathEntry, IClasspathAttribute> referencedRawEntries = new HashMap<>();
    final IClasspathEntry[] entries = javaProject.getRawClasspath();
    for (final IClasspathEntry entry : entries) {
      final IClasspathAttribute attrib = ModuleTraverser.checkForComponentDependencyAttribute(entry,
          ModuleTraverser.DEPENDECYATTRIBUTETYPE_CLASSPATH_COMPONENT_DEPENDENCY);
      if (attrib != null) {
        referencedRawEntries.put(entry, attrib);
      }
    }
    return referencedRawEntries;
  }

  /*
   * Derived from ClasspathDependencyUtil.checkForComponentDependencyAttribute()
   */
  private static IClasspathAttribute checkForComponentDependencyAttribute(final IClasspathEntry entry,
      final int attributeType) {
    if (entry == null) {
      return null;
    }
    final IClasspathAttribute[] attributes = entry.getExtraAttributes();
    for (final IClasspathAttribute attribute : attributes) {
      final String name = attribute.getName();
      if (name.equals(ModuleTraverser.CLASSPATH_COMPONENT_DEPENDENCY)) {
        if ((attributeType == ModuleTraverser.DEPENDECYATTRIBUTETYPE_DEPENDENCY_OR_NONDEPENDENCY)
            || (attributeType == ModuleTraverser.DEPENDECYATTRIBUTETYPE_CLASSPATH_COMPONENT_DEPENDENCY)) {
          return attribute;
        }
      } else if (name.equals(ModuleTraverser.CLASSPATH_COMPONENT_NON_DEPENDENCY)) {
        if ((attributeType == ModuleTraverser.DEPENDECYATTRIBUTETYPE_DEPENDENCY_OR_NONDEPENDENCY)
            || (attributeType == ModuleTraverser.DEPENDECYATTRIBUTETYPE_CLASSPATH_COMPONENT_NONDEPENDENCY)) {
          return attribute;
        }
      }
    }
    return null;
  }

  /*
   * Derived from ClasspathDependencyValidator.validateVirtualComponentEntry()
   */
  private static boolean isValid(final IClasspathEntry entry, final IClasspathAttribute attrib, boolean isWebApp,
      final IProject project) {
    int kind = entry.getEntryKind();
    boolean isClassFolder = ModuleTraverser.isClassFolderEntry(entry);

    if ((kind == IClasspathEntry.CPE_PROJECT) || (kind == IClasspathEntry.CPE_SOURCE)) {
      return false;
    }

    String runtimePath = ModuleTraverser.getRuntimePath(attrib, isWebApp, isClassFolder);
    if (!isWebApp) {
      if (!runtimePath.equals("../") && !runtimePath.equals("/")) {
        return false;
      }
      if (isClassFolder && !runtimePath.equals("/")) {
        return false;
      }
    } else {
      if ((runtimePath != null) && !runtimePath.equals("/WEB-INF/lib") && !runtimePath.equals("/WEB-INF/classes")
          && !runtimePath.equals("../")) {
        return false;
      }
      if (isClassFolder && !runtimePath.equals("/WEB-INF/classes")) {
        return false;
      }
    }
    return true;
  }

  /*
   * Derived from ClasspathDependencyUtil.isClassFolderEntry()
   */
  private static boolean isClassFolderEntry(final IClasspathEntry entry) {
    if ((entry == null) || (entry.getEntryKind() != IClasspathEntry.CPE_LIBRARY)) {
      return false;
    }
    // does the path refer to a file or a folder?
    final IPath entryPath = entry.getPath();
    IPath entryLocation = entryPath;
    final IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(entryPath);
    if (resource != null) {
      entryLocation = resource.getLocation();
    }
    boolean isFile = true; // by default, assume a jar file
    if (entryLocation.toFile().isDirectory()) {
      isFile = false;
    }
    return !isFile;
  }

  /*
   * Derived from ClasspathDependencyUtil.getRuntimePath()
   */
  private static String getRuntimePath(final IClasspathAttribute attrib, final boolean isWebApp,
      final boolean isClassFolder) {
    if ((attrib != null) && !attrib.getName().equals(ModuleTraverser.CLASSPATH_COMPONENT_DEPENDENCY)) {
      return null;
    }
    if ((attrib == null) || (attrib.getValue() == null) || (attrib.getValue().length() == 0)) {
      if (isWebApp) {
        return isClassFolder ? "/WEB_INF/classes" : "WEB-INF/lib";
      }
      return isClassFolder ? "/" : "../";
    }
    return attrib.getValue();
  }
}
