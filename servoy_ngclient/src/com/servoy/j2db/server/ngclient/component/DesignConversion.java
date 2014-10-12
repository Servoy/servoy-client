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

package com.servoy.j2db.server.ngclient.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;

import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author jcompagner
 */
public class DesignConversion
{

	public static Object toStringObject(Object propertyValue, IPropertyType propertyType)
	{
		if (propertyValue != null && propertyType != null)
		{
			switch (propertyType.getName())
			{
				case "dimension" :
					if (propertyValue instanceof Dimension)
					{
						return PersistHelper.createDimensionString((Dimension)propertyValue);
					}
					break;

				case "point" :
					if (propertyValue instanceof Point)
					{
						return PersistHelper.createPointString((Point)propertyValue);
					}
					break;

				case "color" :
					if (propertyValue instanceof Color)
					{
						return PersistHelper.createColorString((Color)propertyValue);
					}
					break;

				case "form" :
					if (propertyValue instanceof Form)
					{
						return ((Form)propertyValue).getName();
					}
					break;

				default :
			}
		}

		return propertyValue;
	}
}
