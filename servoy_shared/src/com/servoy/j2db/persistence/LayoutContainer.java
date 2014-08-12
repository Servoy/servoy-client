/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.awt.Point;

import com.servoy.j2db.util.UUID;

/**
 * @author lvostinar
 *
 */
public class LayoutContainer extends AbstractContainer implements ISupportBounds
{
	protected LayoutContainer(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.LAYOUTCONTAINERS, parent, element_id, uuid);
	}

	/**
	 * Set the location
	 * 
	 * @param arg the location
	 */
	public void setLocation(java.awt.Point arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION, arg);
	}

	public java.awt.Point getLocation()
	{
		java.awt.Point point = getTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION);
		if (point == null)
		{
			point = new Point(10, 10);
		}
		return point;
	}

	public void setTagType(String tagType)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TAGTYPE, tagType);
	}

	public String getTagType()
	{
		String tag = getTypedProperty(StaticContentSpecLoader.PROPERTY_TAGTYPE);
		if (tag == null)
		{
			return "div";
		}
		return tag;
	}

	public void setElementId(String id)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ELEMENTID, id);
	}

	public String getElementId()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ELEMENTID);
	}

	public void setCSSClasses(String cls)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CSSCLASS, cls);
	}

	public String getCSSClasses()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_CSSCLASS);
	}

	public void setStyle(String style)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_STYLE, style);
	}

	public String getStyle()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_STYLE);
	}
}
