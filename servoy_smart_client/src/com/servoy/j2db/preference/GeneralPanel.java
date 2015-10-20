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

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.Utils;

/**
 * Panel from some general client preferences
 * @author jblok
 */
public class GeneralPanel extends PreferencePanel
{
	protected JPanel namePanel;
	protected JPanel fieldPanel;

	private JCheckBox useSystemPrintDialog;
	protected final IApplication application;

	public GeneralPanel(IApplication application)
	{
		super(new BorderLayout());
		this.application = application;

		namePanel = new JPanel(new GridLayout(0, 1, J2DBClient.BUTTON_SPACING, J2DBClient.BUTTON_SPACING));
		fieldPanel = new JPanel(new GridLayout(0, 1, J2DBClient.BUTTON_SPACING, J2DBClient.BUTTON_SPACING));


		namePanel.add(new JLabel("")); //$NON-NLS-1$
		fieldPanel.add(useSystemPrintDialog = new JCheckBox(application.getI18NMessage("servoy.preference.general.useSystemPrintDialog"))); //$NON-NLS-1$
		//by default we use old system for mac,new is not working
		useSystemPrintDialog.setSelected(Utils.getAsBoolean(application.getSettings().getProperty("useSystemPrintDialog", "" + Utils.isAppleMacOS()))); //$NON-NLS-1$//$NON-NLS-2$


		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(namePanel, BorderLayout.WEST);
		panel.add(fieldPanel, BorderLayout.CENTER);
		add(panel, BorderLayout.NORTH);
	}

	private ChangeListener listener;

	@Override
	public void addChangeListener(ChangeListener l)
	{
		listener = l;
	}

	protected void fireChangeEvent()
	{
		changed = true;
		listener.stateChanged(new ChangeEvent(this));
	}

	private boolean changed = false;

	@Override
	public int getRequiredUserAction()
	{
		int retval = PreferencePanel.NO_USER_ACTION_REQUIRED;
		if (changed)
		{
			retval = PreferencePanel.SOLUTION_RELOAD_NEEDED;
		}
		changed = false;
		return retval;
	}

	@Override
	public boolean handleCancel()
	{
		return true;
	}

	@Override
	public boolean handleOK()
	{
		application.getSettings().setProperty("useSystemPrintDialog", Boolean.toString(useSystemPrintDialog.isSelected())); //$NON-NLS-1$ 
		return true;
	}

	@Override
	public String getTabName()
	{
		return application.getI18NMessage("servoy.preference.general.tabName"); //$NON-NLS-1$
	}
}
