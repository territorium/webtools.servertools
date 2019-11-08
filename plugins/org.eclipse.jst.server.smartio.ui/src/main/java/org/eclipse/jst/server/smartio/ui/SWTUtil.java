/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.server.smartio.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * SWT Utility class.
 */
class SWTUtil {

  private static FontMetrics fontMetrics;

  private static void initializeDialogUnits(Control testControl) {
    // Compute and store a font metric
    GC gc = new GC(testControl);
    gc.setFont(JFaceResources.getDialogFont());
    SWTUtil.fontMetrics = gc.getFontMetrics();
    gc.dispose();
  }

  /**
   * Returns a width hint for a button control.
   */
  private static int getButtonWidthHint(Button button) {
    int widthHint = Dialog.convertHorizontalDLUsToPixels(SWTUtil.fontMetrics, IDialogConstants.BUTTON_WIDTH);
    return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
  }

  /**
   * Create a new button with the standard size.
   *
   * @param comp the component to add the button to
   * @param label the button label
   * @return a button
   */
  static Button createButton(Composite comp, String label) {
    Button b = new Button(comp, SWT.PUSH);
    b.setText(label);
    if (SWTUtil.fontMetrics == null) {
      SWTUtil.initializeDialogUnits(comp);
    }
    GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    data.widthHint = SWTUtil.getButtonWidthHint(b);
    b.setLayoutData(data);
    return b;
  }
}