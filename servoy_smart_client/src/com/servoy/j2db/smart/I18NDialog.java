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
package com.servoy.j2db.smart;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.property.I18NPanel;
import com.servoy.j2db.util.gui.JEscapeDialog;

public class I18NDialog extends JEscapeDialog implements ActionListener
{
	I18NPanel panel = null;
	boolean save = true;

	public I18NDialog(IApplication application, Frame f, boolean endUser)
	{
		super(f, false);
		init(application, endUser);
	}

	public I18NDialog(IApplication application, Dialog f, boolean endUser)
	{
		super(f, true);
		init(application, endUser);
	}

	public void setEndUser(boolean endUser)
	{
		panel.setEndUser(endUser);
	}

	private void init(IApplication application, boolean endUser)
	{
		setName("I18NDialog"); //$NON-NLS-1$
		setTitle(application.getI18NMessage("servoy.i18nDialog.title"));
		getContentPane().setLayout(new BorderLayout());
		panel = new I18NPanel();
		panel.setApplication(application, endUser);
		panel.setText(""); //$NON-NLS-1$
		getContentPane().add(panel, BorderLayout.CENTER);

		JPanel buttonPane = new JPanel();
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));

		JButton copyButton = new JButton(application.getI18NMessage("servoy.button.copy")); //$NON-NLS-1$
		copyButton.addActionListener(this);
		copyButton.setActionCommand("copy"); //$NON-NLS-1$

		JButton save = new JButton(application.getI18NMessage("servoy.button.apply"));
		save.setActionCommand("save"); //$NON-NLS-1$
		save.addActionListener(this);

		final JButton ok = new JButton(application.getI18NMessage("servoy.button.ok"));
		ok.setActionCommand("ok"); //$NON-NLS-1$
		ok.addActionListener(this);

		JButton cancel = new JButton(application.getI18NMessage("servoy.button.cancel"));
		cancel.setActionCommand("cancel"); //$NON-NLS-1$
		cancel.addActionListener(this);

//			ok.setPreferredSize(cancel.getPreferredSize());
//			save.setPreferredSize(cancel.getPreferredSize());

		if (!endUser) buttonPane.add(copyButton);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(save);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(ok);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(cancel);

		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(ok);
		loadBounds("I18NDialog");
	}

	public String showDialog(String preselect_key, String preselect_language)
	{
		if (preselect_key != null)
		{
			if (preselect_key.startsWith("i18n:")) //$NON-NLS-1$
			{
				preselect_key = preselect_key.substring(5);
			}
			panel.findAndSelectKey(preselect_key);
		}
		if (preselect_language != null)
		{
			panel.selectLanguage(preselect_language);
		}
		setVisible(true);
		String ret = save ? panel.getText() : null;
		save = true;
		return ret;
	}

	@Override
	protected void cancel()
	{
		if (save)
		{
			panel.getText();
		}
		else
		{
			panel.cancel();
		}
		setVisible(false);
	}

	/*
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		String actionCommand = e.getActionCommand();
		if ("save".equals(actionCommand)) //$NON-NLS-1$
		{
			panel.getText();
		}
		else if ("ok".equals(actionCommand)) //$NON-NLS-1$
		{
			save = true;
			cancel();
		}
		else if ("copy".equals(actionCommand)) //$NON-NLS-1$
		{
			RemoteRunnerChecker.getInstance().setClipboardContent(panel.getText());
		}
		else if ("cancel".equals(actionCommand)) //$NON-NLS-1$
		{
			save = false;
			cancel();
		}
	}
}