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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * @author jblok
 */
public class LoginDialog extends JEscapeDialog implements ActionListener
{
	private JButton setButton;

	private JPasswordField passwordF;

	private JTextField nameF;

	private boolean passOnly;

	private JCheckBox rememberF;

	public LoginDialog(Frame frame, IApplication app)
	{
		super(frame, true);
		buildGUI(app, Messages.getString("servoy.logindialog.title"), false); //$NON-NLS-1$
	}

	public LoginDialog(Frame frame, IApplication app, String title, boolean passOnly)
	{
		super(frame, true);
		this.passOnly = passOnly;
		buildGUI(app, title, false);
	}

	public LoginDialog(Frame frame, IApplication app, String title, boolean passOnly, boolean rememberBox)
	{
		super(frame, true);
		this.passOnly = passOnly;
		buildGUI(app, title, rememberBox);
	}

	public LoginDialog(Dialog dialog, IApplication app)
	{
		super(dialog, true);
		buildGUI(app, Messages.getString("servoy.logindialog.title"), false); //$NON-NLS-1$
	}

	public LoginDialog(Dialog dialog, IApplication app, String title, boolean passOnly)
	{
		super(dialog, true);
		this.passOnly = passOnly;
		buildGUI(app, title, false);
	}

	public LoginDialog(Dialog dialog, IApplication app, String title, boolean passOnly, boolean rememberBox)
	{
		super(dialog, true);
		this.passOnly = passOnly;
		buildGUI(app, title, rememberBox);
	}

	private void buildGUI(IApplication app, String title, boolean remember)
	{
		setTitle(title);
		setName("loginDialog"); //$NON-NLS-1$
		getContentPane().setLayout(new BorderLayout());

		setButton = new JButton(Messages.getString("servoy.button.ok")); //$NON-NLS-1$
		setButton.addActionListener(this);
		setButton.setActionCommand("ok"); //$NON-NLS-1$
		// setButton.setEnabled(false);

		JButton cancelButton = new JButton(Messages.getString("servoy.button.cancel")); //$NON-NLS-1$
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel"); //$NON-NLS-1$

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(setButton);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(cancelButton);

		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		// getRootPane().setDefaultButton(setButton);

		// getContentPane().add(TopImage,BorderLayout.NORTH);//top image 60*360
		JPanel pane = new JPanel();
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

		String text = null;
		if (passOnly)
		{
			text = Messages.getString("servoy.logindialog.specify.password"); //$NON-NLS-1$
		}
		else
		{
			text = Messages.getString("servoy.logindialog.specify.username.password"); //$NON-NLS-1$
		}
		JLabel msg = new JLabel(text);
		msg.setAlignmentX(LEFT_ALIGNMENT);
		msg.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
		pane.add(msg);

		// buttonPane.add(Box.createRigidArea(new Dimension(0, 10)));
		Dimension fsize = new Dimension(230, 20);
		nameF = new JTextField();
		if (!passOnly)
		{
			JLabel name = new JLabel(Messages.getString("servoy.logindialog.label.name")); //$NON-NLS-1$
			name.setAlignmentX(LEFT_ALIGNMENT);
			pane.add(name);

			nameF.setAlignmentX(LEFT_ALIGNMENT);
			nameF.setPreferredSize(fsize);
			nameF.setMaximumSize(fsize);
			pane.add(nameF);

			pane.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		JLabel password = new JLabel(Messages.getString("servoy.logindialog.password")); //$NON-NLS-1$
		password.setAlignmentX(LEFT_ALIGNMENT);
		pane.add(password);

		passwordF = new JPasswordField();
		passwordF.setAlignmentX(LEFT_ALIGNMENT);
		passwordF.setPreferredSize(fsize);
		passwordF.setMaximumSize(fsize);
		passwordF.addActionListener(this);
		pane.add(passwordF);

		if (remember)
		{
			JLabel rememberLabel = new JLabel();
			rememberLabel.setAlignmentX(LEFT_ALIGNMENT);
			pane.add(rememberLabel);

			rememberF = createRememberMeCheckbox();
			rememberF.setAlignmentX(LEFT_ALIGNMENT);
			rememberF.setPreferredSize(fsize);
			rememberF.setMaximumSize(fsize);
			pane.add(rememberF);

		}

		// pane.setPreferredSize(new Dimension(290,150));
		getContentPane().add(pane, BorderLayout.CENTER);

		if (app != null)
		{
			JLabel lock = new JLabel(app.loadImage("lock.gif")); //$NON-NLS-1$
			lock.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
			lock.setHorizontalAlignment(SwingConstants.CENTER);
			lock.setVerticalAlignment(SwingConstants.TOP);
			lock.setPreferredSize(new Dimension(50, 95));
			getContentPane().add(lock, BorderLayout.WEST);
		}

		loadBounds("LoginDialog_" + title); //$NON-NLS-1$
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (getX() > screenSize.width || getY() > screenSize.height)
		{
			setLocationRelativeTo(null);
		}
		setResizable(false);// done as last for linux
	}

	protected JCheckBox createRememberMeCheckbox()
	{
		return new JCheckBox(Messages.getString("servoy.logindialog.remember"), false);
	}

	/*
	 * @see JEscapeDialog#cancel()
	 */
	@Override
	protected void cancel()
	{
		retval = null;
		setVisible(false);
	}

	protected void ok()
	{
		String pass = new String(passwordF.getPassword());
		if (rememberF != null)
		{
			retval = new Object[] { nameF.getText(), pass, new Boolean(rememberF.isSelected()) };
		}
		else
		{
			retval = new Object[] { nameF.getText(), pass };
		}
		setVisible(false);
	}

	public char[] getPassword()
	{
		return passwordF.getPassword();
	}

	private Object[] retval = null;

	public Object[] showDialog(String name)
	{
		JTextField focus = nameF;
		if (name != null && name.length() != 0)
		{
			nameF.setText(name);
			focus = passwordF;
		}
		if (passOnly)
		{
			focus = passwordF;
		}
		passwordF.setText("");// always clear //$NON-NLS-1$
		passwordF.setCaretPosition(0);

		retval = null;

		final JTextField final_focus = focus;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final_focus.requestFocus();
			}
		});
		setVisible(true);
		return retval;
	}

	/*
	 * _____________________________________________________________ The methods below belong to interface <interfacename>
	 */
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (command.equals("cancel")) //$NON-NLS-1$
		{
			cancel();
		}
		else
		// if (command.equals("ok"))
		{
			ok();
		}
	}
}
