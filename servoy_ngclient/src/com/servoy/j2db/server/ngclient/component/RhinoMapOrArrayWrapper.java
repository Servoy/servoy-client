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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;

/**
 * @author jcompagner
 */
public final class RhinoMapOrArrayWrapper implements Scriptable
{
	private final Object wrappedValue;
	private final String property;
	private final int indexProperty;
	private final PropertyDescription propertyDescription;
	private final IServoyDataConverterContext converterContext;
	private Scriptable prototype;
	private Scriptable parent;
	private final WebFormComponent baseWebObject;

	public RhinoMapOrArrayWrapper(Object wrappedValue, WebFormComponent baseWebObject, String property, PropertyDescription propertyDescription,
		IServoyDataConverterContext converterContext)
	{
		this.baseWebObject = baseWebObject;
		this.wrappedValue = wrappedValue;
		this.property = property;
		this.propertyDescription = propertyDescription;
		this.converterContext = converterContext;
		this.indexProperty = -1;
	}

	protected RhinoMapOrArrayWrapper(Object wrappedValue, WebFormComponent baseWebObject, int indexProperty, PropertyDescription propertyDescription,
		IServoyDataConverterContext converterContext)
	{
		this.baseWebObject = baseWebObject;
		this.wrappedValue = wrappedValue;
		this.indexProperty = indexProperty;
		this.propertyDescription = propertyDescription;
		this.converterContext = converterContext;
		this.property = null;
	}

	public Object getWrappedValue()
	{
		return wrappedValue;
	}

	@Override
	public String getClassName()
	{
		return wrappedValue instanceof Map ? "Object" : "Array";
	}

	protected Object getAsSabloValue(String name)
	{
		if (wrappedValue instanceof Map)
		{
			return ((Map<String, Object>)wrappedValue).get(name);
		}
		else if (wrappedValue instanceof List && name.equals("length"))
		{
			return Integer.valueOf(((List< ? >)wrappedValue).size());
		}
		return null;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		Object value = getAsSabloValue(name);
		if (wrappedValue instanceof List && name.equals("length")) return value;

		PropertyDescription propDesc = propertyDescription.getProperty(name);
		return NGConversions.INSTANCE.convertSabloComponentToRhinoValue(value, propDesc, baseWebObject);
	}

	protected Object getSabloValueForIndex(int index)
	{
		if (wrappedValue instanceof List< ? >)
		{
			if (((List< ? >)wrappedValue).size() > index)
			{
				return ((List< ? >)wrappedValue).get(index);
			}
		}
		return null;
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		Object value = getSabloValueForIndex(index);
		return NGConversions.INSTANCE.convertSabloComponentToRhinoValue(value, getArrayElementDescription(), baseWebObject);
	}

	protected PropertyDescription getArrayElementDescription()
	{
		PropertyDescription elementType = propertyDescription;
		if (propertyDescription.getType() instanceof CustomJSONArrayType< ? , ? >) elementType = ((CustomJSONArrayType)propertyDescription.getType()).getCustomJSONTypeDefinition();
		return elementType;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		if (wrappedValue instanceof Map)
		{
			if (((Map)wrappedValue).containsKey(name))
			{
				PropertyDescription pd = propertyDescription.getProperty(name);
				IPropertyType< ? > type = pd.getType();
				// it is available by default, so if it doesn't have conversion, or if it has conversion and is explicitly available
				return !(type instanceof ISabloComponentToRhino< ? >) ||
					((ISabloComponentToRhino)type).isValueAvailableInRhino(getAsSabloValue(name), pd, baseWebObject);
			}
		}
		return false;
	}

	@Override
	public boolean has(int index, Scriptable start)
	{
		if (wrappedValue instanceof List< ? >)
		{
			return ((List< ? >)wrappedValue).size() > index;
		}
		else if (wrappedValue instanceof RhinoMapOrArrayWrapper)
		{
			return ((RhinoMapOrArrayWrapper)wrappedValue).has(index, start);
		}
		return false;
	}

	@Override
	public void put(String name, Scriptable start, Object value)
	{
		if (wrappedValue instanceof Map)
		{
			Object convertedValue = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, getAsSabloValue(name),
				propertyDescription.getProperty(name), baseWebObject);
			((Map)wrappedValue).put(name, convertedValue);
		}
		else if ("length".equals(name))
		{
			int length = ((Number)value).intValue();
			if (wrappedValue instanceof List)
			{
				List lst = (List)wrappedValue;
				if (length == 0) lst.clear();
				else
				{
					while (lst.size() != length)
					{
						lst.remove(lst.size() - 1);
					}
				}
			}
		}
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{
		if (wrappedValue instanceof List< ? >)
		{
			List<Object> lst = (List<Object>)wrappedValue;
			Object val = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, getSabloValueForIndex(index), getArrayElementDescription(),
				baseWebObject);
			while (lst.size() <= index)
			{
				lst.add(null);
			}
			lst.set(index, val);
		}
	}

	@Override
	public void delete(String name)
	{
		if (wrappedValue instanceof Map)
		{
			((Map)wrappedValue).remove(name);
		}
	}

	@Override
	public void delete(int index)
	{
		if (wrappedValue instanceof List< ? >)
		{
			((List< ? >)wrappedValue).set(index, null);
		}
	}

	@Override
	public Scriptable getPrototype()
	{
		return prototype;
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{
		this.prototype = prototype;
	}

	@Override
	public Scriptable getParentScope()
	{
		return parent;
	}

	@Override
	public void setParentScope(Scriptable parent)
	{
		this.parent = parent;
	}

	@Override
	public Object[] getIds()
	{
		if (wrappedValue instanceof Map)
		{
			Set<Entry<String, Object>> tmp = ((Map<String, Object>)wrappedValue).entrySet();
			List<Object> result = new ArrayList<Object>();
			for (Entry<String, Object> entry : tmp)
			{
				PropertyDescription pd = propertyDescription.getProperty(entry.getKey());
				IPropertyType< ? > type = pd.getType();
				// it is available by default, so if it doesn't have conversion, or if it has conversion and is explicitly available
				if (!(type instanceof ISabloComponentToRhino< ? >) ||
					((ISabloComponentToRhino)type).isValueAvailableInRhino(entry.getValue(), pd, baseWebObject))
				{
					result.add(entry.getKey());
				}
			}
			return result.toArray();
		}
		else if (wrappedValue instanceof List< ? >)
		{
			int length = ((List< ? >)wrappedValue).size();
			Object[] result = new Object[length];
			int i = result.length;
			while (--i >= 0)
				result[i] = Integer.valueOf(i);
			return result;
		}
		return new Object[0];
	}

	@Override
	public Object getDefaultValue(Class< ? > hint)
	{
		if (wrappedValue != null) return wrappedValue.toString();
		return null;
	}

	@Override
	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}
}