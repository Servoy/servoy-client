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
package com.servoy.j2db.preference;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.gui.JEscapeDialog;

/**
 * This class shows the application preferences
 */
public class ApplicationPreferences extends JEscapeDialog implements ActionListener, ChangeListener
{
	private final JTabbedPane tabs;
	private final List preferenceTabs;

	private final IApplication _application;
	private final JLabel label;

	/**
	 * Constructor
	 */
	public ApplicationPreferences(ISmartClientApplication app)
	{
		super(app.getMainApplicationFrame(), true /* modal */);
		setTitle(app.getI18NMessage("servoy.preferencedialog.title")); //$NON-NLS-1$
		preferenceTabs = new ArrayList();
		_application = app;

		final JButton setButton = new JButton(app.getI18NMessage("servoy.button.ok")); //$NON-NLS-1$
		setButton.addActionListener(this);
		setButton.setActionCommand("ok"); //$NON-NLS-1$
		JButton cancelButton = new JButton(app.getI18NMessage("servoy.button.cancel")); //$NON-NLS-1$
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel"); //$NON-NLS-1$

		getRootPane().setDefaultButton(setButton);

		tabs = new JTabbedPane();

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		label = new JLabel(" ", SwingConstants.LEFT); //$NON-NLS-1$
		label.setForeground(Color.red);
		label.setPreferredSize(new Dimension(280, 20));
		buttonPane.add(label, BorderLayout.SOUTH);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(setButton);
		buttonPane.add(Box.createRigidArea(new Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.add(cancelButton);

		getContentPane().setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		panel.add(buttonPane, BorderLayout.SOUTH);
		//		panel.add(namePanel,BorderLayout.WEST);
		panel.add(tabs, BorderLayout.CENTER);

		loadBounds("application_prefs"); //$NON-NLS-1$
		setButton.requestFocus();
	}

	public void addPreferenceTab(PreferencePanel tab)
	{
		for (int i = 0; i < preferenceTabs.size(); i++)
		{
			PreferencePanel pp = (PreferencePanel)preferenceTabs.get(i);
			if (pp.getTabName().equals(tab.getTabName()))
			{
				tabs.removeTabAt(i);
				break;
			}
		}
		tab.addChangeListener(this);
		tabs.addTab(tab.getTabName(), tab);
		preferenceTabs.add(tab);
	}

	/**
	 * Dispatch the actions
	 */
	public void actionPerformed(ActionEvent event)
	{
		// Get the "action command" of the event, and dispatch based on that.
		// This method calls a lot of the interesting methods in this class.
		String command = event.getActionCommand();
		if (command.equals("cancel")) //$NON-NLS-1$
		cancel();
		else if (command.equals("ok")) //$NON-NLS-1$
		ok();
	}

	/**
	 * Ok button hit
	 */
	private void ok()
	{
		for (int i = 0; i < preferenceTabs.size(); i++)
		{
			if (!((PreferencePanel)preferenceTabs.get(i)).handleOK())
			{
				// Show this panel.. But there is now tabs.selectTab(String)
				return;
			}
		}
		setVisible(false);
	}

	@Override
	public void cancel()
	{
		for (int i = 0; i < preferenceTabs.size(); i++)
		{
			((PreferencePanel)preferenceTabs.get(i)).handleCancel();
		}
		setVisible(false);
	}

	public void stateChanged(ChangeEvent e)
	{
		int userAction = ((PreferencePanel)e.getSource()).getRequiredUserAction();

		if (userAction == PreferencePanel.NO_USER_ACTION_REQUIRED)
		{
			label.setText(" "); //$NON-NLS-1$
		}
		else if (userAction == PreferencePanel.APPLICATION_RESTART_NEEDED)
		{
			label.setText(_application.getI18NMessage("servoy.preferencedialog.warning.restartapplication")); //$NON-NLS-1$
		}
		else if (userAction == PreferencePanel.SOLUTION_RELOAD_NEEDED)
		{
			label.setText(_application.getI18NMessage("servoy.preferencedialog.warning.reloadsolution", null)); //$NON-NLS-1$
		}
	}
}