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

	private static final long serialVersionUID = 1L;

	public static final int LINE = 0;
	public static final int POLYGON = 1;
	public static final int SPLINE = 2;
	public static final int INK = 3;

/*
 * _____________________________________________________________ Declaration of attributes
 */

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
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_LINESIZE).intValue();
	}

	/**
	 * Returns the shapeType.
	 *
	 * @return int
	 */
	public int getShapeType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHAPETYPE).intValue();
	}

	/**
	 * Sets the lineSize.
	 *
	 * @param lineSize The lineSize to set
	 */
	public void setLineSize(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LINESIZE, arg);
	}

	/**
	 * Sets the shapeType.
	 *
	 * @param shapeType The shapeType to set
	 */
	public void setShapeType(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SHAPETYPE, arg);
	}

	/**
	 * Sets the points.
	 *
	 * @param points The points to set
	 */
	public void setPoints(String points)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_POINTS, points);
		poly = null;
	}

	/**
	 * Returns the points.
	 *
	 * @return String
	 */
	public String getPoints()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_POINTS);
	}

	//for runtime
	private Polygon poly = new Polygon();

	public Polygon getPolygon()
	{
		if (poly == null)
		{
			String points = getPoints();
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

		return poly;
	}
}
