/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.UUID;

/**
 * A design-time menu object (a list of menu items), that can be used in various components and services (so you can re-use the same object in multiple places).
 *
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, typeCode = IRepository.MENUS)
@ServoyClientSupport(mc = false, wc = false, sc = false, ng = true)
public class Menu extends AbstractBase implements ISupportUpdateableName, ISupportEncapsulation, ICloneable, ISupportChilds
{
	private static final long serialVersionUID = 1L;


	/**
	 * Constructor I
	 */
	Menu(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.MENUS, parent, element_id, uuid);
	}

	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		validator.checkName(arg, getID(), new ValidatorSearchContext(IRepository.MENUS), false);
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	/**
	 * Set the name
	 *
	 * @param arg the name
	 */
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	/**
	 * The name/identifier of the menu.
	 */
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	@Override
	public void setEncapsulation(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ENCAPSULATION, arg);
	}

	/**
	 * The encapsulation mode of this Menu. The following can be used:
	 *
	 * - Public (available in both scripting and designer from any module)
	 * - Module Scope - available in both scripting and designer but only in the same module.
	 *
	 * @return the encapsulation mode/level of the persist.
	 */
	@Override
	public int getEncapsulation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ENCAPSULATION).intValue();
	}

	public String getStyleClass()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS);
	}

	/**
	 * The css classes added in browser.
	 */
	public void setStyleClass(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS, arg);
	}

	public MenuItem createNewMenuItem(IValidateName validator, String menuItemName) throws RepositoryException
	{
		String name = menuItemName == null ? "untitled" : menuItemName; //$NON-NLS-1$

		//check if name is in use
		validator.checkName(name, 0, new ValidatorSearchContext(IRepository.MENU_ITEMS), false);

		MenuItem obj = (MenuItem)getRootObject().getChangeHandler().createNewObject(this, IRepository.MENU_ITEMS);
		//set all the required properties

		obj.setName(name);

		addChild(obj);
		return obj;
	}

	@Override
	public String toString()
	{
		String name = getName();
		return name == null ? super.toString() : name;
	}
}
