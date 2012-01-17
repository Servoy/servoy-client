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
package com.servoy.j2db.ui;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.ui.runtime.IRuntimeSplitPane;

/**
 * Scripting interface for RuntimeSplitPane.
 * Combines (deprecated) script methods and java api ({@link IRuntimeSplitPane});
 * 
 * @author gboros
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "RuntimeSplitPane", extendsComponent = "RuntimeComponent")
public interface IScriptSplitPaneMethods extends IRuntimeSplitPane
{

	/**
	 * Set a relationless or related form as left panel.
	 * 
	 * @sample %%prefix%%%%elementName%%.setLeftForm(forms.orders);
	 *
	 * @param form the specified form or form name you wish to add as left panel
	 * 
	 * @return a boolean value indicating if tab was successfully added
	 */
	public boolean js_setLeftForm(Object form);

	/**
	 * Set a relationless or related form as left panel.
	 * 
	 * @sample %%prefix%%%%elementName%%.setLeftForm(forms.orders,'orders_to_order_details');
	 *
	 * @param form the specified form or form name you wish to add as left panel
	 * @param relation the relation name or a related foundset or null for relationless
	 * 
	 * @return a boolean value indicating if tab was successfully added
	 */
	public boolean js_setLeftForm(Object form, Object relation);


	/**
	 * Returns the left form of the split pane.
	 * 
	 * @sample var leftForm = %%prefix%%%%elementName%%.getLeftForm();
	 * 
	 * @return left form of the split pane
	 */
	public FormScope js_getLeftForm();

	/**
	 * Set a relationless or related form as right panel.
	 * 
	 * @sample %%prefix%%%%elementName%%.setRightForm(forms.orders);
	 *
	 * @param form the specified form or form name you wish to add as right panel
	 * 
	 * @return a boolean value indicating if tab was successfully added
	 */
	public boolean js_setRightForm(Object form);

	/**
	 * Set a relationless or related form as right panel.
	 * 
	 * @sample %%prefix%%%%elementName%%.setRightForm(forms.orders,'orders_to_order_details');
	 *
	 * @param form the specified form or form name you wish to add as right panel
	 * @param relation the relation name or a related foundset or null for relationless
	 * 
	 * @return a boolean value indicating if tab was successfully added
	 */
	public boolean js_setRightForm(Object form, Object relation);

	/**
	 * Returns the right form of the split pane.
	 * 
	 * @sample var rightForm = %%prefix%%%%elementName%%.getRightForm();
	 * 
	 * @return right form of the split pane
	 */
	public FormScope js_getRightForm();
}
