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


import java.awt.Point;
import java.awt.Polygon;
import java.util.StringTokenizer;

import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;

/**
 * a painted shape
 * 
 * @author jblok
 */
public class Shape extends BaseComponent
{
	public static final int LINE = 0;
	public static final int POLYGON = 1;
	public static final int SPLINE = 2;
	public static final int INK = 3;

/*
 * _____________________________________________________________ Declaration of attributes
 */

	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private int lineSize;
	private int shapeType;

	/**
	 * Constructor I
	 */
	Shape(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.SHAPES, parent, element_id, uuid);
	}

	/**
	 * Returns the lineSize.
	 * 
	 * @return int
	 */
	public int getLineSize()
	{
		return lineSize;
	}

	/**
	 * Returns the shapeType.
	 * 
	 * @return int
	 */
	public int getShapeType()
	{
		return shapeType;
	}

	/**
	 * Sets the lineSize.
	 * 
	 * @param lineSize The lineSize to set
	 */
	public void setLineSize(int arg)
	{
		checkForChange(lineSize, arg);
		lineSize = arg;
	}

	/**
	 * Sets the shapeType.
	 * 
	 * @param shapeType The shapeType to set
	 */
	public void setShapeType(int arg)
	{
		checkForChange(shapeType, arg);
		shapeType = arg;
	}

	/**
	 * Sets the points.
	 * 
	 * @param points The points to set
	 */
	public void setPoints(String points)
	{
		checkForChange(getPoints(), points);
		if (points != null)
		{
			poly = new Polygon();
			StringTokenizer tk = new StringTokenizer(points, ";"); //$NON-NLS-1$
			while (tk.hasMoreTokens())
			{
				String point = tk.nextToken();
				Point p = PersistHelper.createPoint(point);
				if (p != null) poly.addPoint(p.x, p.y);
			}
		}
	}

	/**
	 * Returns the points.
	 * 
	 * @return String
	 */
	public String getPoints()
	{
		if (poly != null && poly.npoints != 0)
		{
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < poly.npoints; i++)
			{
				sb.append(poly.xpoints[i]);
				sb.append(","); //$NON-NLS-1$
				sb.append(poly.ypoints[i]);
				if (i < poly.npoints - 1)
				{
					sb.append(";"); //$NON-NLS-1$
				}
			}
			return sb.toString();
		}
		return null;
	}

	//for runtime
	private Polygon poly = new Polygon();

	public Polygon getPolygon()
	{
		return poly;
	}

	public void setPoly(Polygon p)
	{
		String oldPoints = getPoints();
		poly = p;
		checkForChange(oldPoints, getPoints());//polygon does not have proper equals impl
	}
}
