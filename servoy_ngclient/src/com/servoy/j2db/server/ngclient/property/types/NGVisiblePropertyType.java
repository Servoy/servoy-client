/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IWrappingContext;
import org.sablo.specification.property.types.VisiblePropertyType;
import org.sablo.specification.property.types.VisibleSabloValue;

import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;

/**
 * @author lvostinar
 *
 */
public class NGVisiblePropertyType extends VisiblePropertyType implements ISabloComponentToRhino<Boolean>
{
	public static NGVisiblePropertyType NG_INSTANCE = new NGVisiblePropertyType();

	@Override
	public VisibleSabloValue wrap(Boolean newValue, VisibleSabloValue oldValue, PropertyDescription propertyDescription, IWrappingContext dataConverterContext)
	{
		if (oldValue != null)
		{
			oldValue.setValue(newValue.booleanValue());
		}
		else
		{
			return new NGVisibleSabloValue(newValue.booleanValue(), dataConverterContext);
		}
		return oldValue;
	}

	@Override
	public boolean isValueAvailableInRhino(Boolean webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Boolean webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService, Scriptable startScriptable)
	{
		Object v = componentOrService.getRawPropertyValue(pd.getName());
		if (v != null)
		{
			return ((NGVisibleSabloValue)v).getComponentValue();
		}
		return webComponentValue;
	}
}
