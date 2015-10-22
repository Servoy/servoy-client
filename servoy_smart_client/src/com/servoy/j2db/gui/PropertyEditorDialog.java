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
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.servoy.j2db.IBasicApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.gui.IPropertyEditorDialog;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * <class description>
 * 
 * @author jblok
 */
public class PropertyEditorDialog extends JEscapeDialog implements ActionListener, IPropertyEditorDialog
{
/*
 * _____________________________________________________________ Declaration of attributes
 */
	private int state = CANCEL_OPTION;
	public static final String ActionCommand_CANCEL = "CANCEL"; //$NON-NLS-1$
	public static final String ActionCommand_OK = "OK"; //$NON-NLS-1$

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	/**
	 * This class is used by the customproperty editor (see that class)
	 */


	private JPanel borderPanel;

//	public PropertyEditorDialog(IBasicApplication app,String title)
//	{
//		this(app.getMainApplicationFrame(),app,title);
//	}
	public PropertyEditorDialog(Dialog owner, IBasicApplication app, String title)
	{
		super(owner, title, true);
		init();
	}

	public PropertyEditorDialog(Frame owner, IBasicApplication app, String title)
	{
		super(owner, title, true);
		init();
	}

	/**
	 * 
	 */
	private void init()
	{
		getContentPane().setLayout(new BorderLayout());
		borderPanel = new JPanel();
		borderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		borderPanel.setLayout(new BorderLayout());
		//buttons
		final JButton setButton = new JButton(Messages.getString("servoy.button.ok")); //$NON-NLS-1$
		setButton.setMnemonic('O');
		setButton.addActionListener(this);
		setButton.setActionCommand(ActionCommand_OK);

		JButton cancelButton = new JButton(Messages.getString("servoy.button.cancel")); //$NON-NLS-1$
		cancelButton.setMnemonic('C');
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand(ActionCommand_CANCEL);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(setButton);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(cancelButton);
		borderPanel.add(buttonPane, BorderLayout.SOUTH);
//		borderPanel.add(comp,BorderLayout.CENTER);
		getContentPane().add(borderPanel, BorderLayout.CENTER);
		getRootPane().setDefaultButton(setButton);

	}

	private Component oldcomp = null;;

	public void setComponent(Component comp)
	{
		if (oldcomp != null) borderPanel.remove(oldcomp);

		borderPanel.add(comp, BorderLayout.CENTER);
		oldcomp = comp;
		loadBounds(comp.getName());
	}

	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (command.equals("OK")) ok(); //$NON-NLS-1$
		else if (command.equals("CANCEL")) cancel(); //$NON-NLS-1$
	}

	public void ok()
	{
		state = OK_OPTION;
		setVisible(false);
		if (oldcomp != null)
		{
			if (oldcomp instanceof JComponent)
			{
				((JComponent)oldcomp).setPreferredSize(oldcomp.getSize());
			}
		}
	}

	@Override
	public void cancel()
	{
		state = CANCEL_OPTION;
		setVisible(false);
		if (oldcomp != null)
		{
			if (oldcomp instanceof JComponent)
			{
				((JComponent)oldcomp).setPreferredSize(oldcomp.getSize());
			}
		}
	}

	public int showDialog()
	{
		setVisible(true);
		return state;
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */


/*
 * _____________________________________________________________ The methods below belong to this class
 */


}
