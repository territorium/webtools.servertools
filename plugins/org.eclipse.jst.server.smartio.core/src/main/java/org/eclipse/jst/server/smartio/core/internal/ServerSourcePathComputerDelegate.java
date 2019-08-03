/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others. All rights reserved. This program and the
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jst.server.smartio.core.internal.wst.IModuleVisitor;
import org.eclipse.jst.server.smartio.core.internal.wst.ModuleTraverser;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class ServerSourcePathComputerDelegate implements ISourcePathComputerDelegate {

  /**
   * {@inheritDoc}
   */
  @Override
  public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    IServer server = ServerUtil.getServer(configuration);

    SourcePathComputerVisitor visitor = new SourcePathComputerVisitor(configuration);

    IModule[] modules = server.getModules();
    for (IModule module : modules) {
      ModuleTraverser.traverse(module, visitor, monitor);
    }

    return visitor.getSourceContainers();
  }

  private class SourcePathComputerVisitor implements IModuleVisitor {

    private final ILaunchConfiguration         configuration;

    /**
     * List<IRuntimeClasspathEntry> of unresolved IRuntimeClasspathEntries
     */
    private final List<IRuntimeClasspathEntry> runtimeClasspath = new ArrayList<>();

    private SourcePathComputerVisitor(ILaunchConfiguration configuration) {
      this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitWebComponent(IVirtualComponent component) throws CoreException {
      IProject project = component.getProject();
      if (project.hasNature(JavaCore.NATURE_ID)) {
        IJavaProject javaProject = JavaCore.create(project);
        runtimeClasspath.add(JavaRuntime.newDefaultProjectClasspathEntry(javaProject));
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisitWebComponent(IVirtualComponent component) throws CoreException {
      // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitArchiveComponent(IPath runtimePath, IPath workspacePath) {
      // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitDependentJavaProject(IJavaProject javaProject) {
      // Ensure dependent projects are listed directly in the classpath list.
      // This is needed because JavaRuntime.getSourceContainers() won't resolve them
      // correctly if they have non-default output folders. In this case, they resolve to
      // binary archive folders with no associated source folder for some reason.
      runtimeClasspath.add(JavaRuntime.newDefaultProjectClasspathEntry(javaProject));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitDependentComponent(IPath runtimePath, IPath workspacePath) {
      // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitWebResource(IPath runtimePath, IPath workspacePath) {
      // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitDependentContentResource(IPath runtimePath, IPath workspacePath) {
      // do nothing
    }

    private ISourceContainer[] getSourceContainers() throws CoreException {
      runtimeClasspath.addAll(Arrays.asList(JavaRuntime.computeUnresolvedSourceLookupPath(configuration)));
      IRuntimeClasspathEntry[] entries = runtimeClasspath.toArray(new IRuntimeClasspathEntry[runtimeClasspath.size()]);
      IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath(entries, configuration);
      return JavaRuntime.getSourceContainers(resolved);
    }

  }
}
