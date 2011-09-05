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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.UUID;


/**
 * A script method
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Method")
@SuppressWarnings("nls")
public class ScriptMethod extends AbstractScriptProvider implements IPersistCloneable, ICloneable
{
	/**
	 * Constructor I
	 */
	ScriptMethod(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.METHODS, parent, element_id, uuid);
	}

	public void setShowInMenu(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWINMENU, arg);
	}

	/**
	 * Flag that tells if the method appears or not in the "Methods" menu of Servoy Client.
	 */
	public boolean getShowInMenu()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWINMENU).booleanValue();
	}


	public String getPrefixedName()
	{
		if (getParent() instanceof Solution)
		{
			return ScriptVariable.GLOBAL_DOT_PREFIX + getName();
		}
		return getName();
	}

	/**
	 * @return
	 */
	public boolean isPrivate()
	{
		String declaration = getDeclaration();
		if (declaration == null) return false;
		int index = declaration.indexOf("*/");
		return index != -1 && declaration.lastIndexOf("@private", index) != -1;
	}

	public boolean isProtected()
	{
		String declaration = getDeclaration();
		if (declaration == null) return false;
		int index = declaration.indexOf("*/");
		return index != -1 && declaration.lastIndexOf("@protected", index) != -1;
	}

	@Override
	public String toString()
	{
		return "ScriptMethod[name:" + getName() + ", inmenu:" + getShowInMenu() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
