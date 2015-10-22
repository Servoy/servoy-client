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


import javax.swing.Action;

import com.servoy.j2db.IApplication;

/**
 * File menu action
 * @author jblok
 */
public class MenuFileAction extends MnemonicCheckAction
{
	public MenuFileAction(IApplication app)
	{
		super(app.getI18NMessage("servoy.menuitem.file"), "servoy.menuitem.file"); //$NON-NLS-1$ //$NON-NLS-2$
		putValue(Action.MNEMONIC_KEY, new Integer(app.getI18NMessage("servoy.menuitem.file.mnemonic").charAt(0))); //$NON-NLS-1$
		app.getCmdManager().registerAction("MenuFileAction", this); //$NON-NLS-1$
	}

	public void actionPerformed(java.awt.event.ActionEvent e)
	{
	}
}
