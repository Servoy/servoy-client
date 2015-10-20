/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import org.json.JSONException;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeJavaObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.util.Debug;

/**
 * @author gboros
 *
 */
public class JSNativeJavaObjectType extends DefaultPropertyType<NativeJavaObject> implements IClassPropertyType<NativeJavaObject>
{

	public static final JSNativeJavaObjectType INSTANCE = new JSNativeJavaObjectType();
	public static final String TYPE_NAME = "jsnativejavaobject"; //$NON-NLS-1$

	/*
	 * @see org.sablo.specification.property.IPropertyType#getName()
	 */
	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public NativeJavaObject fromJSON(Object newJSONValue, NativeJavaObject previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, NativeJavaObject sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			Object o = sabloValue.unwrap();
			if (o != null)
			{
				String className = o.getClass().getName();
				if (className != null)
				{
					try
					{
						IClassPropertyType classPropertyType = TypesRegistry.getType(Class.forName(className));
						if (classPropertyType != null)
						{
							return classPropertyType.toJSON(writer, key, o, pd, clientConversion, dataConverterContext);
						}
					}
					catch (ClassNotFoundException ex)
					{
						Debug.error(ex);
					}
				}
			}
		}
		return null;
	}

	@Override
	public NativeJavaObject defaultValue(PropertyDescription pd)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<NativeJavaObject> getTypeClass()
	{
		return NativeJavaObject.class;
	}

}
