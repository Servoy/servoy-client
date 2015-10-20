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

import javax.swing.ButtonModel;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.IForm;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.FixedToggleButtonModel;

/**
 * @author jblok
 */
public class CmdFindMode extends AbstractCmd
{
	public CmdFindMode(ISmartClientApplication app)
	{
		super(
			app,
			"CmdFindMode", app.getI18NMessage("servoy.menuitem.find"), "servoy.menuitem.find", app.getI18NMessage("servoy.menuitem.find.mnemonic").charAt(0), app.loadImage("find.gif")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		setActionCommand("find"); //$NON-NLS-1$
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, J2DBClient.menuShortcutKeyMask));
	}

	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		IFormManager fm = application.getFormManager();
		IForm dm = fm.getCurrentForm();
		if (dm != null) dm.find();
		return null;
	}

	@Override
	protected ButtonModel createButtonModel()
	{
		return new FixedToggleButtonModel();
	}
}