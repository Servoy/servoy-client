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

import com.servoy.j2db.annotations.ServoyDocumented;
import com.servoy.j2db.util.UUID;


/**
 * a rectangular drawing
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Rectangle")
public class RectShape extends BaseComponent
{
	public static final int BORDER_PANEL = 0;
	public static final int RECTANGLE = 1;
	public static final int ROUNDED_RECTANGLE = 2;
	public static final int OVAL = 3;

/*
 * _____________________________________________________________ Declaration of attributes
 */

	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private int lineSize;
	private int roundedRadius;
	private int containsFormID;
	private int shapeType;

	/**
	 * Constructor I
	 */
	RectShape(ISupportChilds parent, int element_id, UUID uuid)
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
		checkForChange(roundedRadius, arg);
		roundedRadius = arg;
	}

	/**
	 * The rounding radius in pixels.
	 */
	public int getRoundedRadius()
	{
		return roundedRadius;
	}

	/**
	 * Set the lineSize
	 * 
	 * @param arg the lineSize
	 */
	public void setLineSize(int arg)
	{
		checkForChange(lineSize, arg);
		lineSize = arg;
	}

	/**
	 * The width of the line used for drawing the rectangle.
	 */
	public int getLineSize()
	{
		return lineSize;
	}


	/**
	 * Set the containsFormID
	 * 
	 * @param arg the containsFormID
	 */
	public void setContainsFormID(int arg)
	{
		checkForChange(containsFormID, arg);
		containsFormID = arg;
	}

	/**
	 * The form that is displayed inside the shape.
	 * 
	 */
	public int getContainsFormID()
	{
		return containsFormID;
	}

	/**
	 * The type of the shape. The type can be BORDER_PANEL, RECTANGLE, ROUNDED_RECTANGLE or OVAL.
	 */
	public int getShapeType()
	{
		return shapeType;
	}

	/**
	 * Sets the type.
	 * 
	 * @param type The type to set
	 */
	public void setShapeType(int type)
	{
		checkForChange(shapeType, type);
		shapeType = type;
	}
}
