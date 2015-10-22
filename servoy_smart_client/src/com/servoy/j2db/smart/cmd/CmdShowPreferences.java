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
package com.servoy.j2db.smart.cmd;


import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.SwingHelper;

/**
 * @author jblok
 */
public class CmdShowPreferences extends AbstractCmd
{
	public CmdShowPreferences(ISmartClientApplication app)
	{
		super(
			app,
			"CmdShowPreferences", app.getI18NMessage("servoy.menuitem.preferences"), "servoy.menuitem.preferences", app.getI18NMessage("servoy.menuitem.preferences.mnemonic").charAt(0), app.loadImage("preferences.gif")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		setActionCommand("applicationprefs"); //$NON-NLS-1$
		setEnabled(true);
	}

	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		SwingHelper.dispatchEvents(500);//hide menu

		J2DBClient client = ((J2DBClient)application);
		client.showAppPrefs();
		return null;
	}
}