/*******************************************************************************
 * Copyright (c) 2007, 2008 SAS Institute, Inc. and others. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Larry Isaacs - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.smartio.core.ServerBehaviour;
import org.eclipse.jst.server.smartio.core.WebModule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.eclipse.wst.server.core.ServerUtil;

/**
 * Dialog to confirm deletion of the work directory for a module on a server, or the work directory
 * for the entire server. Handling includes stopping and restarting the server if it is running at
 * the time of the deletion.
 *
 */
public class CleanWorkDirDialog extends Dialog {

  private final IServer server;
  private final IModule module;
  private int           state;
  private String        mode;
  private IStatus       completionStatus;

  /**
   * Creates a dialog instance confirm deletion of the work directory for a module on a server, or
   * the work directory for the entire server.
   *
   * @param parentShell the parent shell, or <code>null</code> to create a top-level shell
   * @param server server on which to delete the work directory
   * @param module module whose work directory is to be deleted, or <code>null</code> if if these
   *        server's entire work directory is to be deleted.
   */
  public CleanWorkDirDialog(Shell parentShell, IServer server, IModule module) {
    super(parentShell);

    if (server == null) {
      throw new IllegalArgumentException();
    }

    this.server = server;
    this.module = module;

  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(Messages.confirmCleanWorkDirTitle);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    if ((this.state < 0) || (this.state == IServer.STATE_UNKNOWN)) {
      captureServerState();
    }

    // create a composite with standard margins and spacing
    Composite composite = (Composite) super.createDialogArea(parent);
    // Since there are only label widgets on this page, set the help on the
    // parent
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, ContextIds.SERVER_CLEAN_WORK_DIR);

    Label label = new Label(composite, SWT.WRAP);
    if ((this.state == IServer.STATE_STARTING) || (this.state == IServer.STATE_STOPPING)
        || (this.state == IServer.STATE_UNKNOWN)) {
      label.setText(NLS.bind(Messages.cleanServerStateChanging, this.server.getName()));
    } else {
      if (this.module != null) {
        label.setText(NLS.bind(Messages.cleanModuleWorkDir, this.module.getName(), this.server.getName()));
      } else {
        label.setText(NLS.bind(Messages.cleanServerWorkDir, this.server.getName()));
      }
      GridData data = new GridData();
      data.widthHint = 300;
      label.setLayoutData(data);

      if (this.state == IServer.STATE_STARTED) {
        label = new Label(composite, SWT.WRAP);
        label.setText(Messages.cleanServerRunning);
        data = new GridData();
        data.widthHint = 300;
        label.setLayoutData(data);
      }
    }

    Dialog.applyDialogFont(composite);
    return composite;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);

    if ((this.state < 0) || (this.state == IServer.STATE_UNKNOWN)) {
      captureServerState();
    }

    // If server is transitioning, only allow Cancel
    if ((this.state == IServer.STATE_STARTING) || (this.state == IServer.STATE_STOPPING)) {
      Button button = getButton(IDialogConstants.OK_ID);
      if (button != null) {
        button.setEnabled(false);
      }
    }
  }

  @Override
  protected void okPressed() {
    String jobName =
        NLS.bind(Messages.cleanServerTask, this.module != null ? this.module.getName() : this.server.getName());
    // Create job to perform the cleaning, including stopping and starting the
    // server if necessary
    CleanWorkDirJob job = new CleanWorkDirJob(jobName);
    // Note: Since stop and start, if needed, will set scheduling rules in their
    // jobs,
    // don't set one here. Instead do the actual deletion in a child job too
    // with the
    // scheduling rule on that job, like stop and start.
    job.schedule();

    super.okPressed();
  }

  /*
   * Job to clean the appropriate smart.IO work directory. It includes stopping and starting the
   * server if the server is currently running. The stopping, deletion, and starting are all done
   * with child jobs, each using the server scheduling rule. Thus, this job should not use this rule
   * or it will block these child jobs.
   */
  private class CleanWorkDirJob extends Job {

    /**
     * @param name name for job
     */
    private CleanWorkDirJob(String jobName) {
      super(jobName);
    }

    /**
     * @see Job#belongsTo(Object)
     */
    @Override
    public boolean belongsTo(Object family) {
      return ServerUtil.SERVER_JOB_FAMILY.equals(family);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      final Object mutex = new Object();

      IWebModule webModule = null;
      if (CleanWorkDirDialog.this.module != null) {
        webModule = (IWebModule) CleanWorkDirDialog.this.module.loadAdapter(IWebModule.class, null);
        if (webModule == null) {
          return newErrorStatus(NLS.bind(Messages.errorCantIdentifyWebApp, CleanWorkDirDialog.this.module.getName()),
              null);
        }
      }

      // If state has changed since dialog was open, abort
      if (CleanWorkDirDialog.this.server.getServerState() != CleanWorkDirDialog.this.state) {
        return newErrorStatus(
            NLS.bind(Messages.errorCouldNotCleanStateChange, CleanWorkDirDialog.this.server.getName()), null);
      }

      IOperationListener listener = new IOperationListener() {

        @Override
        public void done(IStatus result) {
          synchronized (mutex) {
            CleanWorkDirDialog.this.completionStatus = result;
            mutex.notifyAll();
          }
        }
      };

      boolean restart = false;
      IStatus status = Status.OK_STATUS;
      // If server isn't stopped, try to stop, clean, and restart
      if (CleanWorkDirDialog.this.state != IServer.STATE_STOPPED) {
        status = CleanWorkDirDialog.this.server.canStop();
        if (!status.isOK()) {
          return wrapErrorStatus(status,
              NLS.bind(Messages.errorCouldNotCleanCantStop, CleanWorkDirDialog.this.server.getName()));
        }

        boolean done = false;
        boolean force = false;
        while (!done) {
          // Stop the server and wait for completion
          synchronized (mutex) {
            CleanWorkDirDialog.this.server.stop(force, listener);

            while (CleanWorkDirDialog.this.completionStatus == null) {
              try {
                mutex.wait();
              } catch (InterruptedException e) {
                // Ignore
              }
            }
          }
          // If forced, or there was an error (doesn't include timeout), or we
          // are stopped, time to
          // exit
          if (force || !CleanWorkDirDialog.this.completionStatus.isOK()
              || (CleanWorkDirDialog.this.server.getServerState() == IServer.STATE_STOPPED)) {
            done = true;
          } else {
            force = ServerUIPlugin.queryCleanTermination(CleanWorkDirDialog.this.server);
            CleanWorkDirDialog.this.completionStatus = null;
          }
        }

        if (!CleanWorkDirDialog.this.completionStatus.isOK()) {
          // If stop job failed, assume error was displayed for that job
          return Status.OK_STATUS;
        }
        if (CleanWorkDirDialog.this.server.getServerState() != IServer.STATE_STOPPED) {
          return newErrorStatus(
              NLS.bind(Messages.errorCouldNotCleanStopFailed, CleanWorkDirDialog.this.server.getName()), null);
        }
        restart = true;
        CleanWorkDirDialog.this.completionStatus = null;
      }

      DeleteWorkDirJob deleteJob = new DeleteWorkDirJob(getName(), webModule, restart);
      deleteJob.setRule(ServerUtil.getServerSchedulingRule(CleanWorkDirDialog.this.server));
      deleteJob.addJobChangeListener(new JobChangeAdapter() {

        @Override
        public void done(IJobChangeEvent event) {
          synchronized (mutex) {
            CleanWorkDirDialog.this.completionStatus = event.getResult();
            mutex.notifyAll();
          }

        }
      });

      // Perform the work directory deletion job
      synchronized (mutex) {
        deleteJob.schedule();

        while (CleanWorkDirDialog.this.completionStatus == null) {
          try {
            mutex.wait();
          } catch (InterruptedException e) {
            // Ignore
          }
        }
      }
      if (!CleanWorkDirDialog.this.completionStatus.isOK()) {
        // If delete job failed, assume error was displayed for that job
        return Status.OK_STATUS;
      }
      CleanWorkDirDialog.this.completionStatus = null;

      if (restart) {
        status = CleanWorkDirDialog.this.server.canStart(CleanWorkDirDialog.this.mode);
        if (!status.isOK()) {
          return wrapErrorStatus(status,
              NLS.bind(Messages.errorCleanCantRestart, CleanWorkDirDialog.this.server.getName()));
        }

        // Restart the server and wait for completion
        synchronized (mutex) {
          CleanWorkDirDialog.this.server.start(CleanWorkDirDialog.this.mode, listener);

          while (CleanWorkDirDialog.this.completionStatus == null) {
            try {
              mutex.wait();
            } catch (InterruptedException e) {
              // Ignore
            }
          }
        }

        if (!CleanWorkDirDialog.this.completionStatus.isOK()) {
          // If start job failed, assume error was displayed for that job
          return Status.OK_STATUS;
        }
      }
      return status;
    }
  }

  /*
   * Job to actually delete the work directory. This is done in a separate job so it can be a
   * "sibling" of potential stop and start jobs. This allows it to have a server scheduling rule.
   */
  private class DeleteWorkDirJob extends Job {

    private final IWebModule webModule;
    private final boolean    restart;

    /**
     * @param name name for job
     */
    private DeleteWorkDirJob(String jobName, IWebModule webModule, boolean restart) {
      super(jobName);
      this.webModule = webModule;
      this.restart = restart;
    }

    /**
     * @see Job#belongsTo(Object)
     */
    @Override
    public boolean belongsTo(Object family) {
      return ServerUtil.SERVER_JOB_FAMILY.equals(family);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

      IStatus status = Status.OK_STATUS;
      // If server isn't stopped, abort the attempt to delete the work directory
      if (CleanWorkDirDialog.this.server.getServerState() != IServer.STATE_STOPPED) {
        return newErrorStatus(NLS.bind(Messages.errorCantDeleteServerNotStopped,
            this.webModule != null ? CleanWorkDirDialog.this.module.getName()
                : CleanWorkDirDialog.this.server.getName()),
            null);
      }

      // Delete the work directory
      ServerBehaviour behavior =
          (ServerBehaviour) CleanWorkDirDialog.this.server.loadAdapter(ServerBehaviour.class, monitor);
      try {
        if (this.webModule != null) {
          WebModule tcWebModule = new WebModule(this.webModule.getContextRoot(), "", "", true);
          status = behavior.cleanContextWorkDir(tcWebModule, null);
        } else {
          status = behavior.cleanServerWorkDir(null);
        }
      } catch (CoreException ce) {
        status = ce.getStatus();
      }
      if (!status.isOK()) {
        String cleanName = CleanWorkDirDialog.this.module != null ? CleanWorkDirDialog.this.module.getName()
            : CleanWorkDirDialog.this.server.getName();
        return wrapErrorStatus(status,
            this.restart
                ? NLS.bind(Messages.errorErrorDuringCleanWasRunning, cleanName,
                    CleanWorkDirDialog.this.server.getName())
                : NLS.bind(Messages.errorErrorDuringClean, cleanName));
      }
      return status;
    }
  }

  private void captureServerState() {
    this.state = this.server.getServerState();
    if (this.state != IServer.STATE_STOPPED) {
      this.mode = this.server.getMode();
    }
  }

  private IStatus newErrorStatus(String message, Throwable throwable) {
    return new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID, 0, message, throwable);
  }

  private IStatus wrapErrorStatus(IStatus status, String message) {
    MultiStatus ms = new MultiStatus(ServerUIPlugin.PLUGIN_ID, 0, message, null);
    ms.add(status);
    return ms;
  }
}