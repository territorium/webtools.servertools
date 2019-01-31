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
package org.eclipse.wst.server.ui.internal.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.EclipseUtil;
import org.eclipse.wst.server.ui.internal.ServerUIPlugin;
import org.eclipse.wst.server.ui.internal.Trace;
import org.eclipse.wst.server.ui.internal.wizard.fragment.DownloadableAdapterLicenseWizardFragment;
import org.eclipse.wst.server.ui.internal.wizard.page.WorkspaceRunnableAdapter;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
/**
 * A wizard used to execute tasks.
 */
public class TaskWizard implements IWizard {
	private static final byte FINISH = 2;
	private static final byte CANCEL = 3;

	private List<IWizardPage> pages;
	private boolean addingPages;
	private Map<WizardFragment, TaskWizardPage> fragmentData = new HashMap<WizardFragment, TaskWizardPage>();
	protected TaskModel taskModel;
	
	private IWizardContainer container = null;
	
	private boolean needsProgressMonitor = false;
	
	private boolean forcePreviousAndNextButtons = false;

	private boolean isHelpAvailable = false;
	
	private Image defaultImage = null;
	
	private RGB titleBarColor = null;

	private String windowTitle = null;
	
	private IDialogSettings dialogSettings = null;
	
	private WizardFragment rootFragment;
	private WizardFragment currentFragment;

	/**
	 * Create a new TaskWizard with the given title and root fragment.
	 * 
	 * @param title a title
	 * @param rootFragment a root fragment
	 */
	public TaskWizard(String title, WizardFragment rootFragment) {
		this(title, rootFragment, null);
		setWindowTitle(title);
	}

	/**
	 * Create a new TaskWizard with the given title, root fragment, and task model.
	 * 
	 * @param title a title
	 * @param rootFragment a root fragment
	 * @param taskModel a task model
	 */
	public TaskWizard(String title, WizardFragment rootFragment, TaskModel taskModel) {
		super();
		if (title != null)
			setWindowTitle(title);
		this.rootFragment = rootFragment;
		this.taskModel = taskModel;
		if (taskModel == null)
			this.taskModel = new TaskModel();
		
		setNeedsProgressMonitor(true);
		setForcePreviousAndNextButtons(true);
	}

	public void setTaskModel(TaskModel taskModel) {
		this.taskModel = taskModel;
	}
	
	public TaskModel getTaskModel() {
		return taskModel;
	}

	public void setRootFragment(WizardFragment rootFragment) {
		this.rootFragment = rootFragment;
	}
	
	public WizardFragment getRootFragment() {
		return rootFragment;
	}

	/**
	 * Cancel the client selection.
	 *
	 * @return boolean
	 */
	public boolean performCancel() {
		final List list = getAllWizardFragments();
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					Iterator iterator = list.iterator();
					while (iterator.hasNext())
						executeTask((WizardFragment) iterator.next(), CANCEL, monitor);
				} catch (CoreException ce) {
					throw new InvocationTargetException(ce);
				}
			}
		};
		
		Throwable t = null;
		try {
			if (getContainer() != null)
				getContainer().run(true, true, runnable);
			else
				runnable.run(new NullProgressMonitor());
			return true;
		} catch (InvocationTargetException te) {
			t = te.getCause();
		} catch (Exception e) {
			t = e;
		}
		if (Trace.SEVERE) {
			Trace.trace(Trace.STRING_SEVERE, "Error cancelling task wizard", t);
		}
		
		if (t instanceof CoreException) {
			EclipseUtil.openError(t.getLocalizedMessage(), ((CoreException)t).getStatus());
		} else
			EclipseUtil.openError(t.getLocalizedMessage());
		
		return false;
		
	}

	public boolean performFinish() {
		if (currentFragment != null)
			currentFragment.exit();
		
		final WizardFragment cFragment = currentFragment;

		final List list = getAllWizardFragments();
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				// enter & exit the remaining pages
				int index = list.indexOf(cFragment);
				while (index > 0 && index < list.size() - 1) {
					final WizardFragment fragment = (WizardFragment) list.get(++index);
					try {
						Display.getDefault().syncExec(new Runnable() {
							public void run() {
								fragment.enter();
								fragment.exit();
							}
						});
					} catch (Exception e) {
						if (Trace.WARNING) {
							Trace.trace(Trace.STRING_WARNING, "Could not enter/exit page", e);
						}
					}
				}
				
				if (useJob()) {
					class FinishWizardJob extends Job {
						public FinishWizardJob() {
							super(getJobTitle());
						}
						
						public boolean belongsTo(Object family) {
							return "org.eclipse.wst.server.ui.family".equals(family);
						}

						public IStatus run(IProgressMonitor monitor2) {
							try {
								Iterator iterator = list.iterator();
								while (iterator.hasNext())
									executeTask((WizardFragment) iterator.next(), FINISH, monitor2);
							} catch (CoreException ce) {
								if (Trace.SEVERE) {
									Trace.trace(Trace.STRING_SEVERE, "Error finishing wizard job", ce);
								}
								return new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID, 0, ce.getLocalizedMessage(), null);
							}
							return Status.OK_STATUS;
						}
					}
					
					FinishWizardJob job = new FinishWizardJob();
					job.setUser(true);
					job.schedule();
				} else {
					Iterator iterator = list.iterator();
					while (iterator.hasNext())
						executeTask((WizardFragment) iterator.next(), FINISH, monitor);
				}
			}
		};
		
		Throwable t = null;
		try {
			if (getContainer() != null)
				getContainer().run(true, true, new WorkspaceRunnableAdapter(runnable));
			else
				runnable.run(new NullProgressMonitor());
			return true;
		} catch (InvocationTargetException te) {
			if (Trace.SEVERE) {
				Trace.trace(Trace.STRING_SEVERE, "Error finishing task wizard", te);
			}
			t = te.getCause();
		} catch(InterruptedException interruptedEx) {
			if (Trace.INFO) {
				Trace.trace(Trace.STRING_INFO, "The task wizard was cancelled.", interruptedEx);
			}
			return false; // return false since the request was canceled
		}
		catch (Exception e) {
			if (Trace.SEVERE) {
				Trace.trace(Trace.STRING_SEVERE, "Error finishing task wizard 2", e);
			}
			t = e;
		}
		
		if (Trace.WARNING) {
			Trace.trace(Trace.STRING_WARNING, "Error completing wizard", t);
		}
		if (t instanceof CoreException) {
			EclipseUtil.openError(t.getLocalizedMessage(), ((CoreException)t).getStatus());
		} else if (t instanceof NullPointerException)
			EclipseUtil.openError("NullPointerException");
		else
			EclipseUtil.openError(t.getLocalizedMessage());
		
		return false;
	}
	
	public void addPage(IWizardPage page) {
		pages.add(page);
		page.setWizard(this);
	}
	
	protected void executeTask(WizardFragment fragment, byte type, IProgressMonitor monitor) throws CoreException {
		if (fragment == null)
			return;
		
		if (type == FINISH)
			fragment.performFinish(monitor);
		else if (type == CANCEL)
			fragment.performCancel(monitor);
	}
	
	protected WizardFragment getCurrentWizardFragment() {
		return currentFragment;
	}
	
	protected void switchWizardFragment(WizardFragment newFragment) {
		List list = getAllWizardFragments();
		int oldIndex = list.indexOf(currentFragment);
		int newIndex = list.indexOf(newFragment);
		if (oldIndex == newIndex)
			return;
		
		//safeExecuteTask(currentFragment, DEPARTURE);
		if (currentFragment != null)
			currentFragment.exit();
		
		if (oldIndex < newIndex)
			oldIndex ++;
		else
			oldIndex --;
		
		while (oldIndex != newIndex) {
			WizardFragment fragment = (WizardFragment) list.get(oldIndex);
			//safeExecuteTask(fragment, ARRIVAL);
			//safeExecuteTask(fragment, DEPARTURE);
			fragment.enter();
			fragment.exit();
			if (oldIndex < newIndex)
				oldIndex ++;
			else
				oldIndex --;
		}
		
		currentFragment = newFragment;
		//safeExecuteTask(currentFragment, ARRIVAL);
		currentFragment.enter();
	}
	
	private List<WizardFragment> getAllWizardFragments() {
		List<WizardFragment> list = new ArrayList<WizardFragment>();
		list.add(rootFragment);
		addSubWizardFragments(rootFragment, list);
		
		Iterator iterator = list.iterator();
		while (iterator.hasNext()) {
			WizardFragment fragment = (WizardFragment) iterator.next();
			if ((!(fragment instanceof DownloadableAdapterLicenseWizardFragment))&& !taskModel.equals(fragment.getTaskModel()))
				fragment.setTaskModel(taskModel);
		}
		return list;
	}

	private void addSubWizardFragments(WizardFragment fragment, List<WizardFragment> list) {
		Iterator iterator = fragment.getChildFragments().iterator();
		while (iterator.hasNext()) {
			WizardFragment child = (WizardFragment) iterator.next();
			list.add(child);
			addSubWizardFragments(child, list);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		if (addingPages)
			return;
		
		try {
			addingPages = true;
			pages = new ArrayList<IWizardPage>();
			Iterator<WizardFragment> iterator = getAllWizardFragments().iterator();
			while (iterator.hasNext()) {
				WizardFragment fragment = iterator.next();
				TaskWizardPage page = getFragmentData(fragment);
				if (fragment.hasComposite()) {
					if (page != null)
						addPage(page);
					else {
						TaskWizardPage page2 = new TaskWizardPage(fragment);
						fragmentData.put(fragment, page2);
						addPage(page2);
					}
				}
			}
		} catch (Exception e) {
			if (Trace.SEVERE) {
				Trace.trace(Trace.STRING_SEVERE, "Error adding fragments to wizard", e);
			}
		} finally {
			addingPages = false;
		}
	}

	/*private static void updateWizardPages() {
		try {
			current.updatePages();
			current.getContainer().updateButtons();
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error updating wizard pages", e);
		}
	}*/

	private TaskWizardPage getFragmentData(WizardFragment fragment) {
		try {
			TaskWizardPage page = fragmentData.get(fragment);
			if (page != null)
				return page;
		} catch (Exception e) {
			if (Trace.SEVERE) {
				Trace.trace(Trace.STRING_SEVERE, "Error getting fragment data", e);
			}
		}
		
		return null;
	}

	protected void updatePages() {
		addPages();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#canFinish()
	 */
	public boolean canFinish() {
		// Default implementation is to check if all pages are complete.
		for (int i= 0; i < pages.size(); i++) {
			if (!(pages.get(i)).isPageComplete())
				return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#createPageControls(org.eclipse.swt.widgets.Composite)
	 */
	public void createPageControls(Composite pageContainer) {
		// the default behavior is to create all the pages controls
		for (int i = 0; i < pages.size(); i++){
			IWizardPage page = pages.get(i);
			page.createControl(pageContainer);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#dispose()
	 */
	public void dispose() {
		// notify pages
		for (int i = 0; i < pages.size(); i++) {
			pages.get(i).dispose();
		}

		// dispose of image
		if (defaultImage != null) {
			defaultImage.dispose();
			defaultImage = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getContainer()
	 */
	public IWizardContainer getContainer() {
		return container;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getDefaultPageImage()
	 */
	public Image getDefaultPageImage() {
		return defaultImage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getDialogSettings()
	 */
	public IDialogSettings getDialogSettings() {
		return dialogSettings;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getNextPage(IWizardPage page) {
		int index = pages.indexOf(page);
		if (index == pages.size() - 1 || index == -1)
			// last page or page not found
			return null;
		
		return pages.get(index + 1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getPage(java.lang.String)
	 */
	public IWizardPage getPage(String name) {
		for (int i= 0; i < pages.size(); i++) {
			IWizardPage page = pages.get(i);
			String pageName = page.getName();
			if (pageName.equals(name))
				return page;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getPageCount()
	 */
	public int getPageCount() {
		return pages.size();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getPages()
	 */
	public IWizardPage[] getPages() {
		return pages.toArray(new IWizardPage[pages.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getPreviousPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public IWizardPage getPreviousPage(IWizardPage page) {
		int index = pages.indexOf(page);
		if (index == 0 || index == -1)
			// first page or page not found
			return null;
		return pages.get(index - 1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getStartingPage()
	 */
	public IWizardPage getStartingPage() {
		if (pages.size() == 0)
			return null;
		
		return pages.get(0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getTitleBarColor()
	 */
	public RGB getTitleBarColor() {
		return titleBarColor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#getWindowTitle()
	 */
	public String getWindowTitle() {
		return windowTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#isHelpAvailable()
	 */
	public boolean isHelpAvailable() {
		return isHelpAvailable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#needsPreviousAndNextButtons()
	 */
	public boolean needsPreviousAndNextButtons() {
		return forcePreviousAndNextButtons || pages.size() > 1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#needsProgressMonitor()
	 */
	public boolean needsProgressMonitor() {
		return needsProgressMonitor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#setContainer(org.eclipse.jface.wizard.IWizardContainer)
	 */
	public void setContainer(IWizardContainer wizardContainer) {
		this.container = wizardContainer;
	}
	
	public void setDialogSettings(IDialogSettings settings) {
		dialogSettings = settings;
	}
	
	public void setNeedsProgressMonitor(boolean b) {
		needsProgressMonitor = b;
	}
	
	public void setForcePreviousAndNextButtons(boolean b) {
		forcePreviousAndNextButtons = b;
	}
	
	public void setWindowTitle(String title) {
		windowTitle = title;
	}

	protected boolean useJob() {
		return false;
	}

	protected String getJobTitle() {
		return getWindowTitle();
	}
}