/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.server.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.wst.server.core.TaskModel;
/**
 * A wizard fragment is a node within a wizard that provides a completely
 * extendable wizard flow by supporting a flexible tree structure for the
 * pages. As the user walks pages through the wizard, they are actually
 * traversing the nodes of a tree, and each node can add or remove children
 * at any time.
 * 
 * Each node may be non-UI (useful for injecting behaviour into the tree)
 * or contain a single wizard page (@see hasComposite() and
 * createComposite(Composite, IWizardHandle)). The node may also have
 * children (@see getChildFragments(), which should be updated or refreshed
 * whenever the updateChildFragments() method is called by another node to
 * let this node know that it's state may have changed.
 * 
 * This implementation uses a createChildFragments() method to allow the
 * fragment to add it's children into the tree. Note that this method may
 * be called multiple times as the tree is updated and it must return the
 * same instance of any children that have previously been returned.
 * 
 * @since 1.0
 */
public abstract class WizardFragment {
	private TaskModel taskModel;
	private boolean isComplete = true;
	private boolean isforceLastFragment = false;
	private List<WizardFragment> listImpl;

	/**
	 * Returns <code>true</code> if this fragment has an associated UI,
	 * and <code>false</code> otherwise.
	 * 
	 * @return true if the fragment has a composite
	 */
	public boolean hasComposite() {
		return false;
	}

	/**
	 * Creates the composite associated with this fragment.
	 * This method is only called when hasComposite() returns true.
	 * 
	 * @param parent a parent composite
	 * @param handle a wizard handle
	 * @return the created composite
	 */
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		return null;
	}

	/**
	 * Sets the wizard task model.
	 * <p>
	 * A task model is shared by all fragments in the wizard and is used to share
	 * data. It contains information about the overall task flow and allows
	 * fragments to store and retrieve data. Its usage allows multiple fragments
	 * to be chained together and share data from the output of one fragment to
	 * the input of another.
	 * </p>
	 * 
	 * @param taskModel the task model
	 * @see #getTaskModel()
	 */
	public void setTaskModel(TaskModel taskModel) {
		this.taskModel = taskModel;
	}

	/**
	 * Returns the wizard task model.
	 * 
	 * @return the task model
	 * @see #setTaskModel(TaskModel)
	 */
	public TaskModel getTaskModel() {
		return taskModel;
	}

	/**
	 * Called when the wizard that this fragment belongs to has traversed
	 * into this wizard fragment. It is called to give the fragment the
	 * opportunity to initialize any values shown in the composite or
	 * update the task model.
	 * <p>
	 * When finish is pressed, the current fragment is exit()ed, and then
	 * performFinish() is called on all of the fragments in the tree.
	 * enter() and exit() are not called on the remaining fragments.
	 * </p>
	 */
	public void enter() {
		// do nothing
	}

	/**
	 * Called when the wizard that this fragment belongs to has traversed
	 * out of this wizard fragment. It is called to give the fragment the
	 * opportunity to save any values entered into the composite or
	 * update the task model.
	 * <p>
	 * When finish is pressed, the current fragment is exit()ed, and then
	 * performFinish() is called on all of the fragments in the tree.
	 * enter() and exit() are not called on the remaining fragments. 
	 * </p>
	 */
	public void exit() {
		// do nothing
	}

	/**
	 * Called when the wizard that this fragment belongs to is finished.
	 * After exit()ing the current page, all fragment's performFinish()
	 * methods are called in order.
	 * <p>
	 * This method is not called on the UI thread and must not access the
	 * composite. Not only might the user never have accessed the fragment's
	 * composite, but this method may be called asynchronously on a job
	 * once the wizard has closed.
	 * </p>
	 * 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @throws CoreException if something goes wrong
	 */
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		// do nothing
	}

	/**
	 * Called when the wizard that this fragment belongs to is canceled.
	 * After exit()ing the current page, all fragment's performCancel()
	 * methods are called in order.
	 * <p>
	 * This method is not called on the UI thread and must not access the
	 * composite. Not only might the user never have accessed the fragment's
	 * composite, but this method may be called asynchronously on a job
	 * once the wizard has closed.
	 * </p>
	 * 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @throws CoreException if something goes wrong
	 */
	public void performCancel(IProgressMonitor monitor) throws CoreException {
		// do nothing
	}

	/**
	 * Returns the child fragments. Child fragments come directly after this fragment
	 * in the wizard flow.
	 * 
	 * @return a list of child fragments
	 */
	public List getChildFragments() {
		if (listImpl == null) {
			listImpl = new ArrayList<WizardFragment>();
			createChildFragments(listImpl);
		}
		return listImpl;
	}

	/**
	 * Called to give the fragment a chance to update it's child fragments in
	 * response to other changes within the wizard or task model.
	 */
	public void updateChildFragments() {
		listImpl = null;
	}

	/**
	 * This method is called by the implementation of getChildFragments() to
	 * allow this fragment to add it's children. This method must cache and
	 * return the same instance of any child fragment created. If new
	 * instances are created each time the wizard is updated, the enablement
	 * state and the flow of the wizard will be incorrect. 
	 * 
	 * @param list a list to add the child fragments to
	 */
	protected void createChildFragments(List<WizardFragment> list) {
		// do nothing
	}

	/**
	 * Returns true if this fragment is complete (can finish). If it is
	 * complete the user will be allowed to go to the next fragment or
	 * finish the wizard. If the fragment is not complete, the Next button
	 * will be disabled. If the fragment is complete but another fragment
	 * is not complete, the Finish button will not be enabled.
	 * 
	 * @return <code>true</code> if the fragment is complete, and
	 *    <code>false</code> otherwise
	 */
	public boolean isComplete() {
		return isComplete;
	}

	/**
	 * Set the isComplete state.
	 * 
	 * @param complete <code>true</code> if the fragment is complete, and
	 *    <code>false</code> otherwise
	 */
	protected void setComplete(boolean complete) {
		this.isComplete = complete;
	}
	
	/**
	 * Returns true if this fragment is forced to be the last fragment. If it is
	 * not forceLastFragment the user will be allowed to go to the next fragment.
	 * If the fragment is isForceLastFragment, the Next button
	 * will be enabled if more fragment exists. If forceLastFragment, clicking Finish
	 * will still execute the finish of remaining fragments.
	 * 
	 * @return <code>true</code> if the fragment is forceLastFragment, and
	 *    <code>false</code> otherwise
	 */
	public boolean isForceLastFragment() {
		return isforceLastFragment;
	}
	
	/**
	 * Set the isforceLastFragment state.
	 * 
	 * @param isforceLastFragment <code>true</code> if the fragment is forced to be last fragment, and
	 *    <code>false</code> otherwise
	 */
	protected void setForceLastFragment(boolean isforceLastFragment) {
		this.isforceLastFragment = isforceLastFragment;
	}
}