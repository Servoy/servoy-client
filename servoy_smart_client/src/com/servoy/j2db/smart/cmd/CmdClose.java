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


import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.smart.J2DBClient;

/**
 * @author jblok
 */
public class CmdClose extends AbstractCmd
{
	public CmdClose(ISmartClientApplication app)
	{
		super(
			app,
			"CmdClose", app.getI18NMessage("servoy.menuitem.close"), "servoy.menuitem.close", app.getI18NMessage("servoy.menuitem.close.mnemonic").charAt(0), app.loadImage("close_all.gif")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		setActionCommand("close"); //$NON-NLS-1$
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, J2DBClient.menuShortcutKeyMask));
		setEnabled(false);
	}

	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		application.blockGUI(application.getI18NMessage("servoy.menuitem.close.status.text")); //$NON-NLS-1$
		try
		{
			application.closeSolution(false, null);
		}
		finally
		{
			application.releaseGUI();
		}
		return null;
	}

}