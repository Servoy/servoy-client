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


import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.cmd.IHandleUndoRedo;
import com.servoy.j2db.smart.J2DBClient;

/**
 * @author jcompagner
 */
public class RedoAction extends MnemonicCheckAction
{
	private final IHandleUndoRedo manager;

	public RedoAction(IApplication app, IHandleUndoRedo manager)
	{
		super(app.getI18NMessage("servoy.menuitem.redo"), "servoy.menuitem.redo", app.loadImage("redo.gif")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		this.manager = manager;
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, J2DBClient.menuShortcutKeyMask));
		putValue(Action.ACTION_COMMAND_KEY, "redo"); //$NON-NLS-1$
		putValue(Action.MNEMONIC_KEY, new Integer(app.getI18NMessage("servoy.menuitem.redo.mnemonic").charAt(0))); //$NON-NLS-1$
		//			setEnabled(false);
	}

	/*
	 * @see ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		manager.redo();
	}
}
