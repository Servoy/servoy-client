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


import javax.swing.ButtonModel;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.util.FixedToggleButtonModel;

/**
 * @author jblok
 */
public class CmdExtendFind extends AbstractCmd
{
/*
 * _____________________________________________________________ Declaration of attributes
 */


/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public CmdExtendFind(ISmartClientApplication app)
	{
		super(
			app,
			"CmdExtendFind", app.getI18NMessage("servoy.menuitem.extendSearch"), "servoy.menuitem.extendSearch", app.getI18NMessage("servoy.menuitem.extendSearch.mnemonic").charAt(0), app.loadImage("find_extend.gif")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		setActionCommand("search"); //$NON-NLS-1$
//		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass AbstractCmd
 */
	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		IFormManager fm = application.getFormManager();
		IForm cf = fm.getCurrentForm();
		if (cf != null && !cf.isInFindMode())
		{
			cf = null;
		}

		if (cf == null)
		{
			IForm[] rfif = ((FormManager)fm).getVisibleRootFormsInFind();
			cf = rfif.length > 0 ? rfif[0] : null;
			// rfif.length > 1 would be a strange case where more than one visible form relation hierarchy is in find mode;
			// we choose the first one - I or should we use all of them?! this is an ansupported case anyway
		}

		if (cf != null)
		{
			cf.performFind(false, false, true);
		}

		return null;
	}

	@Override
	protected ButtonModel createButtonModel()
	{
		return new FixedToggleButtonModel();
	}

/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */


/*
 * _____________________________________________________________ The methods below belong to this class
 */


}