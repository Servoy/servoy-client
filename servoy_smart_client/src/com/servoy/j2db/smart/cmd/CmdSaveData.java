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

import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.ISmartClientApplication;

/**
 * @author jblok
 */
public class CmdSaveData extends AbstractCmd
{
	public CmdSaveData(ISmartClientApplication app)
	{
		super(app, "CmdSaveData", app.getI18NMessage("servoy.menuitem.saveData"), "servoy.menuitem.saveData");//,'v',app.loadImage("find_next.gif")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		// remove F6 "toggle focus" from JSplitPane, as we need it for save
		InputMap map = (InputMap)UIManager.get("SplitPane.ancestorInputMap");
		if (map != null)
		{
			KeyStroke keyStrokeF6 = KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0);
			map.remove(keyStrokeF6);
		}

		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass AbstractCmd
 */
	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		application.getFoundSetManager().getEditRecordList().stopEditing(false);
		return null;
	}
}