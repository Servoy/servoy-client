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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * Shows a msg dialog with one option (checkbox) for use like "never show this dialog again"
 * @author jblok
 */
public class SingleOptionMessageDialog extends JEscapeDialog implements ActionListener
{
	private JButton setButton;
	private JCheckBox option;
	private boolean optionPreselect;
	
	protected SingleOptionMessageDialog(Dialog owner) 
	{
		super(owner, true);
	}

	protected SingleOptionMessageDialog(Frame owner) 
	{
		super(owner, true);
	}

	protected void cancel()
	{
		option.setSelected(optionPreselect);
		setVisible(false);
	}

	public static boolean showMessageDialog(Window window, String msg, String title, String optionText,boolean optionPreselect, String buttonText)
	{
		SingleOptionMessageDialog dialog = null;
		if (window instanceof Frame)
		{
			dialog = new SingleOptionMessageDialog((Frame)window);
		}
		else
		{
			dialog = new SingleOptionMessageDialog((Dialog)window);
		}
		return dialog.showDialog(msg,title,optionText,optionPreselect,buttonText);
	}
	
	private boolean showDialog(String msg, String title, String optionText,boolean optionPreselect, String buttonText)
	{
		this.optionPreselect = optionPreselect;
		getContentPane().setLayout(new BorderLayout());

		setButton = new JButton(buttonText); //$NON-NLS-1$
		setButton.addActionListener(this);
		setButton.setActionCommand("ok"); //$NON-NLS-1$

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(setButton);

		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		JPanel mainPane = new JPanel();
		mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
		mainPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 40));

		mainPane.add(new JLabel(msg,JLabel.LEFT));
		mainPane.add(Box.createRigidArea(new Dimension(0,J2DBClient.BUTTON_SPACING)));
		mainPane.add(option = new JCheckBox(optionText,optionPreselect));

		getContentPane().add(mainPane, BorderLayout.CENTER);

		getRootPane().setDefaultButton(setButton);

		//loadBounds("SingleOptionMessageDialog");
		pack();
		setLocationRelativeTo(getOwner());
			
		setTitle(title);
		setVisible(true);
		
		boolean b = option.isSelected();
		this.dispose();
		return b;
	}
	public void actionPerformed(ActionEvent e)
	{
		setVisible(false);
	}
}
