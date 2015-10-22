/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/
package com.servoy.j2db.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * @author sebster
 */
public class MultiSelectionDialog extends JEscapeDialog implements ActionListener
{
	private MultiSelectionPanel multiSelectionPanel;
	private int[] selection;

	public MultiSelectionDialog(Dialog owner, String title, String message, String label1, String label2, Object[] entries, boolean[] isSelected, int[] targetListIndices, boolean hasUpDownButtons)
	{
		super(owner, true);
		init(entries, isSelected, targetListIndices, title, message, label1, label2, hasUpDownButtons);
	}
	
	public MultiSelectionDialog(Frame owner, String title, String message, String label1, String label2, Object[] entries, boolean[] isSelected, int[] targetListIndices, boolean hasUpDownButtons)
	{
		super(owner, true);
		init(entries, isSelected, targetListIndices, title, message, label1, label2, hasUpDownButtons);
	}
	
	public static int[] showDialog(Window window, String title, String msg, String label1, String label2, Object[] entries, boolean[] isSelected, int[] targetListIndices, boolean hasUpDownButtons)
	{
		MultiSelectionDialog dialog = null;
		if (window instanceof Frame)
		{
			dialog = new MultiSelectionDialog((Frame) window, title, msg, label1, label2, entries, isSelected, targetListIndices, hasUpDownButtons);
		}
		else
		{
			dialog = new MultiSelectionDialog((Dialog) window, title, msg, label1, label2, entries, isSelected, targetListIndices, hasUpDownButtons);
		}
		return dialog.showDialog();
	}

	private void init(Object[] entries, boolean[] isSelected, int[] targetListIndices, String title, String message, String label1, String label2, boolean hasUpDownButtons)
	{
		setTitle(title);

		multiSelectionPanel = new MultiSelectionPanel(entries, isSelected, targetListIndices, message, label1, label2, hasUpDownButtons);

		// OK/Cancel button pane.
		final JButton okButton = new JButton("OK");
		okButton.setMnemonic(KeyEvent.VK_O);
		okButton.addActionListener(this);
		okButton.setActionCommand("ok"); //$NON-NLS-1$
		getRootPane().setDefaultButton(okButton);

		final JButton cancelButton = new JButton("Cancel");
		cancelButton.setMnemonic(KeyEvent.VK_C);
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel"); //$NON-NLS-1$

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(okButton);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(cancelButton);

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(multiSelectionPanel, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.SOUTH);

		okButton.requestFocus();

		pack();
		setLocationRelativeTo(getOwner());
	}

	public void cancel()
	{
		setVisible(false);
		selection = null;
	}
	
	public void ok()
	{
		setVisible(false);
		selection = multiSelectionPanel.getSelectedValues();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if ("ok".equals(command)) //$NON-NLS-1$
			ok();
		else if ("cancel".equals(command)) //$NON-NLS-1$
			cancel();
	}
	
	public int[] showDialog()
	{
		setVisible(true);
		this.dispose();
		return selection;
	}

}
