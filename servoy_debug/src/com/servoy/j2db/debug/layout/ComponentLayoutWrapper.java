/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.debug.layout;

import com.servoy.base.persistence.IMobileProperties.MobileProperty;
import com.servoy.j2db.scripting.solutionmodel.JSComponent;

/**
 * Layout wrapper for solution model component.
 * 
 * @author rgansevles
 *
 */
public class ComponentLayoutWrapper implements ILayoutWrapper
{
	private final JSComponent< ? > component;

	/**
	 * @param element
	 */
	public ComponentLayoutWrapper(JSComponent< ? > component)
	{
		this.component = component;
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		component.setX(x);
		component.setY(y);
		component.setWidth(width);
		component.setHeight(height);
	}

	@Override
	public int getX()
	{
		return component.getX();
	}

	public int getY()
	{
		return component.getY();
	}

	@Override
	public int getWidth()
	{
		return component.getWidth();
	}

	public int getHeight()
	{
		return component.getHeight();
	}

	@Override
	public int getPreferredHeight()
	{
		return 0;
	}

	@Override
	public MobileFormSection getElementType()
	{
		return MobileFormSection.ContentElement;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getMobileProperty(MobileProperty<T> property)
	{
		return (T)component.getBaseComponent(false).getCustomMobileProperty(property.propertyName);
	}
}
