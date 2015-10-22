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
@SuppressWarnings("nls")
public class CmdOpenSolution extends AbstractCmd
{
	public CmdOpenSolution(ISmartClientApplication app)
	{
		super(app, "CmdOpenSolution", app.getI18NMessage("servoy.menuitem.openSolution"), "servoy.menuitem.openSolution", app.getI18NMessage(
			"servoy.menuitem.openSolution.mnemonic").charAt(0), app.loadImage("open_project.gif"));
		setActionCommand("open");
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, J2DBClient.menuShortcutKeyMask));
	}

	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		application.blockGUI(application.getI18NMessage("servoy.menuitem.openSolution.status.text"));
		try
		{
			if (application.getSolution() != null)
			{
				application.closeSolution(false, null);
			}
			else
			{
				application.invokeAndWait(new Runnable()
				{
					public void run()
					{
						if (application.getSolution() == null)
						{
							((J2DBClient)application).selectAndOpenSolution(); // select solution will be shown.
						}
					}
				});
			}
		}
		finally
		{
			application.releaseGUI();
		}
		return null;
	}
}