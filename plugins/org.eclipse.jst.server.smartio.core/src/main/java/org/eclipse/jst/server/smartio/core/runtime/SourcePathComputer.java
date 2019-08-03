/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.core.runtime;

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
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class SourcePathComputer implements ISourcePathComputerDelegate {

  /**
   * Computes the source path of the {@link ILaunchConfiguration}.
   *
   * @param config
   * @param monitor
   */
  @Override
  public ISourceContainer[] computeSourceContainers(ILaunchConfiguration config, IProgressMonitor monitor)
      throws CoreException {
    SourcePathComputerVisitor visitor = new SourcePathComputerVisitor(config);
    for (IModule module : ServerUtil.getServer(config).getModules()) {
      ModuleTraverser.traverse(module, visitor, monitor);
    }
    return visitor.getSourceContainers();
  }

  /**
   * The {@link SourcePathComputerVisitor} implements an {@link IModuleVisitor}
   */
  private class SourcePathComputerVisitor implements IModuleVisitor {

    private final ILaunchConfiguration         configuration;
    private final List<IRuntimeClasspathEntry> classpathes = new ArrayList<>();

    /**
     * Constructs an instance of {@link SourcePathComputerVisitor}.
     *
     * @param configuration
     */
    private SourcePathComputerVisitor(ILaunchConfiguration configuration) {
      this.configuration = configuration;
    }

    /**
     * Get all {@link ISourceContainer}'s reached by the current launch
     * configuration.
     */
    private ISourceContainer[] getSourceContainers() throws CoreException {
      this.classpathes.addAll(Arrays.asList(JavaRuntime.computeUnresolvedSourceLookupPath(this.configuration)));
      IRuntimeClasspathEntry[] entries = this.classpathes.toArray(new IRuntimeClasspathEntry[this.classpathes.size()]);
      IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath(entries, this.configuration);
      return JavaRuntime.getSourceContainers(resolved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitWebComponent(IVirtualComponent component) throws CoreException {
      IProject project = component.getProject();
      if (project.hasNature(JavaCore.NATURE_ID)) {
        IJavaProject javaProject = JavaCore.create(project);
        this.classpathes.add(JavaRuntime.newDefaultProjectClasspathEntry(javaProject));
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
      // This is needed because JavaRuntime.getSourceContainers() won't resolve
      // them
      // correctly if they have non-default output folders. In this case, they
      // resolve to
      // binary archive folders with no associated source folder for some
      // reason.
      this.classpathes.add(JavaRuntime.newDefaultProjectClasspathEntry(javaProject));
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
  }
}
