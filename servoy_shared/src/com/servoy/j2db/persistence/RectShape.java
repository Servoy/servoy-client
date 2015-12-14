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

import com.servoy.j2db.util.UUID;


/**
 * a rectangular drawing
 *
 * @author jblok
 */
public class RectShape extends BaseComponent
{

	private static final long serialVersionUID = 1L;

	public static final int BORDER_PANEL = 0;
	public static final int RECTANGLE = 1;
	public static final int ROUNDED_RECTANGLE = 2;
	public static final int OVAL = 3;

/*
 * _____________________________________________________________ Declaration of attributes
 */

	/**
	 * Constructor I
	 */
	protected RectShape(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.RECTSHAPES, parent, element_id, uuid);
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */


/*
 * _____________________________________________________________ The methods below belong to this class
 */
	/**
	 * Set the roundedRadius
	 *
	 * @param arg the roundedRadius
	 */
	public void setRoundedRadius(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ROUNDEDRADIUS, arg);
	}

	/**
	 * The rounding radius in pixels.
	 */
	public int getRoundedRadius()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ROUNDEDRADIUS).intValue();
	}

	/**
	 * Set the lineSize
	 *
	 * @param arg the lineSize
	 */
	public void setLineSize(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LINESIZE, arg);
	}

	/**
	 * The width of the line used for drawing the rectangle.
	 */
	public int getLineSize()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_LINESIZE).intValue();
	}


	/**
	 * Set the containsFormID
	 *
	 * @param arg the containsFormID
	 */
	public void setContainsFormID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CONTAINSFORMID, arg);
	}

	/**
	 * The form that is displayed inside the shape.
	 *
	 */
	public int getContainsFormID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_CONTAINSFORMID).intValue();
	}

	/**
	 * The type of the shape. The type can be BORDER_PANEL, RECTANGLE, ROUNDED_RECTANGLE or OVAL.
	 */
	public int getShapeType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHAPETYPE).intValue();
	}

	/**
	 * Sets the type.
	 *
	 * @param type The type to set
	 */
	public void setShapeType(int type)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SHAPETYPE, type);
	}
}
