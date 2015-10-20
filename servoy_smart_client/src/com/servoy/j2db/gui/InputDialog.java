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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.servoy.j2db.Messages;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;
import com.servoy.j2db.util.docvalidator.LengthDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * @author		jblok
 */
public class InputDialog extends JEscapeDialog implements ActionListener
{
	public static final int OK_BUTTON = 1;
	public static final int CANCEL_BUTTON = 2;
	
	private JTextField field;
	private JLabel msg;
	private int button = OK_BUTTON;
	
	public InputDialog(Frame frame, int maxlength, int identType)
	{
		super(frame,true);
		init(maxlength, identType);
	}
	public InputDialog(Dialog d, int maxlength, int identType)
	{
		super(d,true);
		init(maxlength, identType);
	}
	private void init(int maxlength, int identType)
	{
		getContentPane().setLayout(new BorderLayout());

		field = new JTextField();
		if (maxlength == 0)
		{
			field.setDocument(new ValidatingDocument(new IdentDocumentValidator(identType)));
		}
		else
		{
			field.setDocument(new ValidatingDocument(new ValidatingDocument.IDocumentValidator[] {
					new IdentDocumentValidator(identType), new LengthDocumentValidator(maxlength)}));
		}
		JPanel pane = new JPanel();
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		msg = new JLabel();
		msg.setHorizontalAlignment(SwingConstants.LEFT);
		msg.setAlignmentX(Component.LEFT_ALIGNMENT);
		msg.setPreferredSize(new Dimension(170,15));
		pane.add(msg);
		pane.add(Box.createRigidArea(new Dimension(0, J2DBClient.BUTTON_SPACING)));
		field.setAlignmentX(Component.LEFT_ALIGNMENT);
		pane.add(field);
//        msg.setSize(field.getPreferredSize());
		getContentPane().add(pane,BorderLayout.CENTER);


		JButton setButton = new JButton(Messages.getString("servoy.button.ok")); //$NON-NLS-1$
		setButton.addActionListener(this);
		setButton.setActionCommand("ok"); //$NON-NLS-1$

		JButton cancelButton = new JButton(Messages.getString("servoy.button.cancel")); //$NON-NLS-1$
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel"); //$NON-NLS-1$

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(setButton);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(cancelButton);
		buttonPane.setPreferredSize(new Dimension(300,50));

		getContentPane().add(buttonPane,BorderLayout.SOUTH);
		
		getRootPane().setDefaultButton(setButton);
	}

	public static String showInputDialog(Dialog parentComponent, Object message, String title, Object initialValue, int maxlength, int identType)
	{
		InputDialog dialog = new InputDialog(parentComponent, maxlength, identType);
		return showInputDialog(parentComponent,dialog,message, title, initialValue);
	}
	
	public static String showInputDialog(Frame parentComponent, Object message, String title, Object initialValue, int identType)
	{
		InputDialog dialog = new InputDialog(parentComponent, 0, identType);
		return showInputDialog(parentComponent,dialog,message, title, initialValue);
	}

	public static String showInputDialog(Frame parentComponent, Object message, String title, Object initialValue, int maxlength, int identType)
	{
		InputDialog dialog = new InputDialog(parentComponent, maxlength, identType);
		return showInputDialog(parentComponent,dialog,message, title, initialValue);
	}

	private static String showInputDialog(Window parentComponent,final InputDialog dialog, Object message, String title, Object initialValue)
	{
		if (initialValue != null) 
		{
			dialog.field.setText(initialValue.toString());
			dialog.field.selectAll();
		}
		dialog.setTitle(title);
		dialog.msg.setText((message == null) ? "" : message.toString()); //$NON-NLS-1$
		dialog.pack();
		dialog.setLocationRelativeTo(parentComponent);

		final JTextField final_focus = dialog.field;
		SwingUtilities.invokeLater(new Runnable(){
			public void run()
			{
				final_focus.requestFocus();
			}
		});
		dialog.setVisible(true);
		String s = dialog.field.getText();
		
		if(dialog.button == CANCEL_BUTTON)
		{
			s = null;
		}
		else if (s.trim().length() == 0)
		{
			s = null; 
		}
		
		dialog.dispose();
		return s;
	}

	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (command.equals("ok")) //$NON-NLS-1$
		{
			button = OK_BUTTON; 
		} 
		else if (command.equals("cancel")) //$NON-NLS-1$
		{
			button = CANCEL_BUTTON;
		} 
		setVisible(false);
	}
	/**
	 * @see com.servoy.j2db.util.gui.JEscapeDialog#cancel()
	 */
	protected void cancel()
	{
		button = CANCEL_BUTTON;
		setVisible(false);
	}

}
