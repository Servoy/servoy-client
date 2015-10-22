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
import java.util.HashSet;

import javax.swing.KeyStroke;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IForm;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.dataprocessing.IFoundSet;

/**
 * @author jblok
 */
public class CmdPerformFind extends AbstractCmd
{
/*
 * _____________________________________________________________ Declaration of attributes
 */


/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public CmdPerformFind(ISmartClientApplication app)
	{
		super(
			app,
			"CmdPerformFind", app.getI18NMessage("servoy.menuitem.search"), "servoy.menuitem.search", app.getI18NMessage("servoy.menuitem.search.mnemonic").charAt(0), app.loadImage("find_next.gif")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		setActionCommand("search"); //$NON-NLS-1$
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass AbstractCmd
 */
	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		FormManager fm = (FormManager)application.getFormManager();
		IForm[] rootFindModeForms = fm.getVisibleRootFormsInFind();

		if (rootFindModeForms.length > 0)
		{
			HashSet<IFoundSet> processedFounsets = new HashSet<IFoundSet>(); // if you have 2 visible forms in find that use the same foundset (paren + child tab on the same table)
			// if you call performFind() on both of them you might get the "no results, modify last find" dialog twice...
			for (IForm f : rootFindModeForms)
			{
				// check for each form if it is still in find mode (as onSearchCmd on one form might stop find mode on others as well and we do not want to trigger onSearchCmd on the others
				IFoundSet foundSet = f.getFoundSet();
				boolean shouldPerform = f.isInFindMode() && (foundSet == null || !processedFounsets.contains(foundSet));
				if (shouldPerform && f instanceof FormController && ((FormController)f).getForm() != null &&
					((FormController)f).getForm().getOnSearchCmdMethodID() < 0) shouldPerform = false;
				if (shouldPerform)
				{
					processedFounsets.add(foundSet);
					f.performFind(true, true, true);

					// if performFind failed (either because of a problem or because the custom onSearchCmd intentionally
					// wants to continue find) then don't try to perform search any more find mode forms
					if (f.isInFindMode()) break;
				}
			}
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