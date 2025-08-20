/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 * @since 2022.09
 *
 */
public class CSSPositionLayoutContainer extends LayoutContainer implements ISupportCSSPosition, ISupportFormElement
{

	/**
	 * @param parent
	 * @param element_id
	 * @param uuid
	 */
	protected CSSPositionLayoutContainer(ISupportChilds parent, UUID uuid)
	{
		super(IRepository.CSSPOS_LAYOUTCONTAINERS, parent, uuid);
	}


	/**
	 * The css position of the component.
	 */
	public CSSPosition getCssPosition()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_CSS_POSITION);
	}

	public void setCssPosition(CSSPosition arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CSS_POSITION, arg);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ISupportFormIndex#setFormIndex(int)
	 */
	@Override
	public void setFormIndex(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FORMINDEX, arg);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ISupportFormIndex#getFormIndex()
	 */
	@Override
	public int getFormIndex()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FORMINDEX).intValue();
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ISupportFormIndexAndGroupID#getGroupID()
	 */
	@Override
	public String getGroupID()
	{
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ISupportFormIndexAndGroupID#setGroupID(java.lang.String)
	 */
	@Override
	public void setGroupID(String arg)
	{
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ISupportFormElement#getVisible()
	 */
	@Override
	public boolean getVisible()
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ISupportFormElement#getEnabled()
	 */
	@Override
	public boolean getEnabled()
	{
		// TODO Auto-generated method stub
		return false;
	}
}
