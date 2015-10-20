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
import java.awt.GridLayout;
import java.net.URL;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.Utils;

/**
 * Preference panel related to services
 * @author jblok
 */
public class ServicePanel extends PreferencePanel implements ChangeListener
{
	private final IApplication application;
	private final JCheckBox checkBox;

	/**
	 * Constructor for ServicePanel that shows the 2 way socket option, will only be called/shown when the DefaultRMISocketfactory is used.
	 * 
	 * @param application
	 */
	public ServicePanel(IApplication app)
	{
		super(new BorderLayout());
		application = app;
		JPanel namePanel = new JPanel(new GridLayout(0, 1, J2DBClient.BUTTON_SPACING, J2DBClient.BUTTON_SPACING));
		JPanel fieldPanel = new JPanel(new GridLayout(0, 1, J2DBClient.BUTTON_SPACING, J2DBClient.BUTTON_SPACING));

		URL base = application.getServerURL();
		JLabel label = new JLabel(""); //$NON-NLS-1$
		boolean enabled = Utils.getAsBoolean(application.getSettings().getProperty(base.getHost() + base.getPort() + "SocketFactory.useTwoWaySocket", "true")); //$NON-NLS-1$//$NON-NLS-2$
		checkBox = new JCheckBox(application.getI18NMessage("servoy.preference.service.twoWaySocket"), enabled); //$NON-NLS-1$
		checkBox.addChangeListener(this);
		namePanel.add(label);
		fieldPanel.add(checkBox);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(namePanel, BorderLayout.WEST);
		panel.add(fieldPanel, BorderLayout.CENTER);
		add(panel, BorderLayout.NORTH);
	}

	/**
	 * @see PreferencePanel#cancel()
	 */
	@Override
	public boolean handleCancel()
	{
		return true;
	}

	/**
	 * @see PreferencePanel#ok()
	 */
	@Override
	public boolean handleOK()
	{
		URL base = application.getServerURL();
		application.getSettings().put(base.getHost() + base.getPort() + "SocketFactory.useTwoWaySocket", Boolean.toString(checkBox.isSelected())); //$NON-NLS-1$
		return true;
	}

	/**
	 * @see PreferencePanel#getTabName()
	 */
	@Override
	public String getTabName()
	{
		return application.getI18NMessage("servoy.preference.service.tabName"); //$NON-NLS-1$
	}

	private ChangeListener listener;

	@Override
	public void addChangeListener(ChangeListener l)
	{
		listener = l;
	}

	private void fireChangeEvent()
	{
		changed = true;
		listener.stateChanged(new ChangeEvent(this));
	}

	private boolean changed = false;

	public void stateChanged(ChangeEvent e)
	{
		fireChangeEvent();
	}

	@Override
	public int getRequiredUserAction()
	{
		int retval = PreferencePanel.NO_USER_ACTION_REQUIRED;
		if (changed)
		{
			retval = PreferencePanel.APPLICATION_RESTART_NEEDED;
		}
		changed = false;
		return retval;
	}
}
