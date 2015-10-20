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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.servoy.j2db.Messages;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * @author jblok
 */
public class DetailMsgDialog extends JEscapeDialog implements ActionListener
{
	private JButton detailButton;
	private JLabel msg;
	private JTextArea text;
	private JPanel borderPane;
	private boolean isAdded = false;

	public DetailMsgDialog(Dialog frame, String title)
	{
		super(frame, title, true);
		init();
	}

	public DetailMsgDialog(Frame frame, String title)
	{
		super(frame, title, true);
		init();
	}

	private void init()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JButton setButton = new JButton(Messages.getString("servoy.button.ok")); //$NON-NLS-1$
		setButton.addActionListener(this);
		setButton.setActionCommand("ok"); //$NON-NLS-1$

		detailButton = new JButton(Messages.getString("servoy.button.details")); //$NON-NLS-1$
		detailButton.addActionListener(this);
		detailButton.setActionCommand("details"); //$NON-NLS-1$
		detailButton.setEnabled(false);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(setButton);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(detailButton);
		buttonPane.setPreferredSize(new Dimension(300, 50));
		panel.add(buttonPane, BorderLayout.SOUTH);

		JPanel msgPane = new JPanel();
		msgPane.setLayout(new BorderLayout());
		msg = new JLabel();
		msg.setHorizontalAlignment(SwingConstants.LEFT);
		msg.setAlignmentX(Component.LEFT_ALIGNMENT);
		//        msg.setPreferredSize(new Dimension(400,15));
		msgPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		msgPane.add(msg, BorderLayout.CENTER);
		panel.add(msgPane, BorderLayout.CENTER);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(panel, BorderLayout.NORTH);

		getRootPane().setDefaultButton(setButton);
	}

	/*
	 * @see JEscapeDialog#cancel()
	 */
	@Override
	protected void cancel()
	{
		if (isAdded) details();//make sure its removed
		setVisible(false);
	}

	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (command.equals("ok")) //$NON-NLS-1$
		cancel();
		if (command.equals("details")) //$NON-NLS-1$
		details();
	}

	private String moreText;
	private Object detailMsg;

	public void showDialog(String message, Object detail)
	{
		showDialog(message, detail, false);
	}

	public void showDialog(String message, Object detail, boolean showDetail)
	{
		moreText = ""; //$NON-NLS-1$
		if (detail != null && detail.toString().trim().equals("null")) //$NON-NLS-1$
		detail = null; //fix for nullpointerexception msg
		if (detail != null && detail.toString().trim().length() == 0) detail = null; //fix for empty string
		detailButton.setEnabled(detail != null);

		detailMsg = detail;
		if (message == null) message = ""; //$NON-NLS-1$
		if (message.length() > 100 && message.toLowerCase().indexOf("<html>") == -1) //$NON-NLS-1$
		{
			moreText = message;
			message = message.substring(0, 100) + "..."; //$NON-NLS-1$
			detailButton.setEnabled(true);
		}
		msg.setText(message);

		if ((isAdded && !showDetail) || (!isAdded && showDetail) || ((!detailButton.isEnabled() && isAdded)))
		{
			details();
		}
		pack();
		setLocationRelativeTo(getOwner());
		setVisible(true);
	}

	private void details()
	{
		if (!isAdded)
		{
			detailButton.setText(Messages.getString("servoy.button.hide")); //$NON-NLS-1$
			if (borderPane == null)
			{
				borderPane = new JPanel();
				borderPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
				text = new JTextArea();
				text.setLineWrap(true);
				text.setWrapStyleWord(true);
				JScrollPane scroll = new JScrollPane(text);
				scroll.setPreferredSize(new Dimension(400, 200));
				borderPane.setLayout(new BorderLayout());
				borderPane.add(scroll, BorderLayout.CENTER);
			}
			getContentPane().add(borderPane, BorderLayout.CENTER);
			isAdded = true;

			String exText = ""; //$NON-NLS-1$
			if (detailMsg instanceof Throwable)
			{
				Throwable trowable = (Throwable)detailMsg;
				while (trowable != null)
				{
					exText += trowable.toString();
					exText += "\n";
					if (trowable.getCause() != trowable)
					{
						trowable = trowable.getCause();
					}
					else
					{
						trowable = null;
					}
				}
				if (exText == null)
				{
					exText = ""; //fix for null string //$NON-NLS-1$
				}
				else if (exText.trim().length() == 0)
				{
					exText = ""; //fix for empty string //$NON-NLS-1$
				}
				else if (exText.trim().equals("null")) //$NON-NLS-1$
				{
					exText = ""; //fix for nullpointerexception msg //$NON-NLS-1$
				}
			}
			else
			{
				if (detailMsg != null)
				{
					exText = detailMsg.toString();
				}
			}
			text.setText(moreText + "\n" + exText); //$NON-NLS-1$
			//			StringWriter pw = new StringWriter();
			//			exception.printStackTrace(pw);
			//			text.setText(pw.toString());
		}
		else
		{
			detailButton.setText(Messages.getString("servoy.button.details")); //$NON-NLS-1$
			getContentPane().remove(borderPane);
			isAdded = false;
		}
		pack();
	}
}