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
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.smart.RemoteRunnerChecker;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * @author jblok Exp $
 */
public class CustomColorChooserDialog extends JEscapeDialog implements ActionListener
{
	private Color defaultval, retval;
	private JColorChooser chooserPane;
	private JButton setButton;

	public CustomColorChooserDialog(Dialog parent, IApplication app)
	{
		super(parent, Messages.getString("servoy.colorchooser.title"), true); //$NON-NLS-1$
		init(app);
	}

	public CustomColorChooserDialog(Frame parent, IApplication app)
	{
		super(parent, Messages.getString("servoy.colorchooser.title"), true); //$NON-NLS-1$
		init(app);
	}

	private void init(IApplication app)
	{
		getContentPane().setLayout(new BorderLayout());

		chooserPane = new JColorChooser();
		chooserPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		getContentPane().add(chooserPane, BorderLayout.CENTER);

		setButton = new JButton(Messages.getString("servoy.button.ok")); //$NON-NLS-1$
		setButton.addActionListener(this);
		setButton.setActionCommand("ok"); //$NON-NLS-1$

		JButton cancelButton = new JButton(Messages.getString("servoy.button.cancel")); //$NON-NLS-1$
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel"); //$NON-NLS-1$

		JButton copyButton = new JButton(Messages.getString("servoy.button.copy")); //$NON-NLS-1$
		copyButton.addActionListener(this);
		copyButton.setActionCommand("copy"); //$NON-NLS-1$

		JButton defaultButton = new JButton(Messages.getString("servoy.button.default")); //$NON-NLS-1$
		defaultButton.addActionListener(this);
		defaultButton.setActionCommand("default"); //$NON-NLS-1$

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		buttonPane.add(copyButton);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(setButton);
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(cancelButton);
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(defaultButton);

		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(setButton);

		loadBounds("CustomColorChooserDialog"); //$NON-NLS-1$
	}

	/*
	 * _____________________________________________________________ The methods below belong to interface <interfacename>
	 */
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (command.equals("ok")) //$NON-NLS-1$
			ok();
		else if (command.equals("cancel")) //$NON-NLS-1$
			cancel();
		else if (command.equals("default")) //$NON-NLS-1$
			chooserPane.setColor(defaultval);
		else if (command.equals("copy")) //$NON-NLS-1$
			copy();
	}

	@Override
	public void cancel()
	{
		retval = null;
		setVisible(false);
	}

	private void copy()
	{
		Color c = chooserPane.getColor();
		if (c != null)
		{
			String scolor = PersistHelper.createColorString(c);
			RemoteRunnerChecker.getInstance().setClipboardContent(scolor);
		}
	}

	public void ok()
	{
		retval = chooserPane.getColor();
		setVisible(false);
	}

	public Color showDialog(Color current)
	{
		defaultval = current;
		chooserPane.setColor(defaultval);

		setVisible(true);
		return retval;
	}
}