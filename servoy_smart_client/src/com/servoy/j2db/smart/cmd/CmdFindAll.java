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

import com.servoy.j2db.IForm;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.smart.J2DBClient;

/**
 * @author jblok
 */
public class CmdFindAll extends AbstractCmd
{
/*
 * _____________________________________________________________ Declaration of attributes
 */


/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public CmdFindAll(ISmartClientApplication app)
	{
		super(
			app,
			"CmdFindAll", app.getI18NMessage("servoy.menuitem.showAll"), "servoy.menuitem.showAll", app.getI18NMessage("servoy.menuitem.showAll.mnemonic").charAt(0)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		setActionCommand("findall"); //$NON-NLS-1$
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_J, J2DBClient.menuShortcutKeyMask));
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass AbstractCmd
 */
	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		try
		{
			application.blockGUI(application.getI18NMessage("servoy.menuitem.showAll.status.text")); //$NON-NLS-1$
			IFormManager fm = application.getFormManager();
			IForm dm = fm.getCurrentForm();
			if (dm != null) dm.loadAllRecords();
		}
		finally
		{
			application.releaseGUI();
		}
		return null;
	}


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */


/*
 * _____________________________________________________________ The methods below belong to this class
 */


}