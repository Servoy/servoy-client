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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import com.servoy.base.persistence.IMobileProperties.MobileProperty;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.scripting.solutionmodel.JSForm;

/**
 * Layout wrapper for solution model gouped elements.
 *
 * @author rgansevles
 *
 */
public class GroupLayoutWrapper implements ILayoutWrapper
{
	private final FormElementGroup group;
	private final JSForm jsform;

	/**
	 * @param element
	 * @param jsform
	 * @param debugWebClient
	 */
	public GroupLayoutWrapper(FormElementGroup group, JSForm jsform)
	{
		this.group = group;
		this.jsform = jsform;
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		setLocation(x, y);
		MobileFormLayout.layoutGroup(x, y, width, height, getLayoutElements());
	}

	public void setLocation(int x, int y)
	{
		Point oldLocation = group.getLocation();
		int dx = x - oldLocation.x;
		int dy = y - oldLocation.y;
		if (dx == 0 && dy == 0) return;

		for (ILayoutWrapper element : getLayoutElements())
		{
			element.setBounds(element.getX() + dx, element.getY() + dy, element.getWidth(), element.getHeight());
		}
	}

	@Override
	public int getPreferredHeight()
	{
		return MobileFormLayout.calculateGroupHeight(getLayoutElements());
	}

	private List<ILayoutWrapper> getLayoutElements()
	{
		List<ILayoutWrapper> elements = new ArrayList<ILayoutWrapper>();
		for (ISupportFormElement element : MobileFormLayout.getGroupElements(group))
		{
			ILayoutWrapper wrapper = MobileFormLayout.createLayoutWrapper(element, jsform);
			if (wrapper != null)
			{
				elements.add(wrapper);
			}
		}
		return elements;
	}

	@Override
	public MobileFormSection getElementType()
	{
		return MobileFormSection.ContentElement;
	}

	@Override
	public int getX()
	{
		return group.getBounds().x;
	}

	@Override
	public int getY()
	{
		return group.getBounds().y;
	}

	@Override
	public int getWidth()
	{
		return group.getBounds().width;
	}

	@Override
	public int getHeight()
	{
		return group.getBounds().height;
	}

	@Override
	public <T> T getMobileProperty(MobileProperty<T> property)
	{
		return null;
	}
}
