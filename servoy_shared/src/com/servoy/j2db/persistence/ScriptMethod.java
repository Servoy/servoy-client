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
package com.servoy.j2db.persistence;

import com.servoy.j2db.scripting.ServoyDocumented;
import com.servoy.j2db.util.UUID;


/**
 * A script method
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Method")
public class ScriptMethod extends AbstractScriptProvider implements IPersistCloneable, ICloneable
{
	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private boolean showInMenu;


	/**
	 * Constructor I
	 */
	ScriptMethod(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.METHODS, parent, element_id, uuid);
	}

	public void setShowInMenu(boolean arg)
	{
		checkForChange(showInMenu, arg);
		showInMenu = arg;
	}

	/**
	 * Flag that tells if the method appears or not in the "Methods" menu of Servoy Client.
	 */
	public boolean getShowInMenu()
	{
		return showInMenu;
	}


	@Override
	public String toString()
	{
		return "ScriptMethod[name:" + getName() + ", inmenu:" + showInMenu + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
