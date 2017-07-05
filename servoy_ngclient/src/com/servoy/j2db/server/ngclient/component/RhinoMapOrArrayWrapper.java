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

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.server.ngclient.property.ComponentTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.IRhinoPrototypeProvider;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public final class RhinoMapOrArrayWrapper implements Scriptable
{
	private final Object wrappedValue;
	private final PropertyDescription propertyDescription;
	private Scriptable prototype;
	private Scriptable parent;
	private final IWebObjectContext webObjectContext;

	public RhinoMapOrArrayWrapper(Object wrappedValue, IWebObjectContext webObjectContext, PropertyDescription propertyDescription, Scriptable startScriptable)
	{
		this.webObjectContext = webObjectContext;
		this.wrappedValue = wrappedValue;
		this.propertyDescription = propertyDescription;

		if (wrappedValue instanceof List)
		{
			// allow it to use native JS array methods
			NativeArray proto = new NativeArray(0);
			if (startScriptable != null) proto.setPrototype(ScriptableObject.getArrayPrototype(startScriptable));
			setPrototype(proto); // new instance so that JS can use usual put/set even for non-defined things in PropertyDescription by forwarding to prototype
		}
		else if (wrappedValue instanceof Map)
		{
			// allow it to use native JS array methods
			Scriptable proto;

			if (wrappedValue instanceof IRhinoPrototypeProvider) proto = ((IRhinoPrototypeProvider)wrappedValue).getRhinoPrototype(); // for example window_server.js popup menus create objects with prototypes; and while we do change the object ('instrument' it) we want to keep the prototype the same so that all the functions defined in it still work
			else
			{
				// standard object
				proto = new NativeObject();
				if (startScriptable != null) proto.setPrototype(ScriptableObject.getObjectPrototype(startScriptable));
			}
			setPrototype(proto); // new instance so that JS can use usual put/set even for non-defined things in PropertyDescription by forwarding to prototype
		}
		if (startScriptable != null) parent = ScriptableObject.getTopLevelScope(startScriptable);
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
		if (wrappedValue instanceof List)
		{
			if (name.equals("length")) return value;
			List list = (List)wrappedValue;
			if (list.size() > 0)
			{
				for (Object element : list)
				{
					if (element instanceof ComponentTypeSabloValue && Utils.equalObjects(name, ((ComponentTypeSabloValue)element).getName()))
					{
						return NGConversions.INSTANCE.convertSabloComponentToRhinoValue(element,
							((ComponentTypeSabloValue)element).getComponentPropertyDescription(), webObjectContext, start);
					}
				}
			}
			return Scriptable.NOT_FOUND; // then it will be searched for in prototype that is a native array prototype
		}

		PropertyDescription propDesc = propertyDescription.getProperty(name);
		return propDesc != null ? NGConversions.INSTANCE.convertSabloComponentToRhinoValue(value, propDesc, webObjectContext, start) : Scriptable.NOT_FOUND;
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
		return Scriptable.NOT_FOUND;
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		Object value = getSabloValueForIndex(index);
		return value == Scriptable.NOT_FOUND ? Scriptable.NOT_FOUND
			: NGConversions.INSTANCE.convertSabloComponentToRhinoValue(value, getArrayElementDescription(), webObjectContext, start);
	}

	protected PropertyDescription getArrayElementDescription()
	{
		PropertyDescription elementType = propertyDescription;
		if (propertyDescription.getType() instanceof CustomJSONArrayType< ? , ? >)
			elementType = ((CustomJSONArrayType)propertyDescription.getType()).getCustomJSONTypeDefinition();
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
					((ISabloComponentToRhino)type).isValueAvailableInRhino(getAsSabloValue(name), pd, webObjectContext);
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
			PropertyDescription pd = propertyDescription.getProperty(name);
			if (pd != null)
			{
				Object convertedValue = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, getAsSabloValue(name), pd, webObjectContext);
				((Map)wrappedValue).put(name, convertedValue);
			}
			else
			{
				// so that JS can use usual put/set even for non-defined things in PropertyDescription by forwarding to prototype
				getPrototype().put(name, getPrototype(), value);
			}
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
		else
		{
			// so that JS can use usual put/set even for non-defined things in PropertyDescription by forwarding to prototype
			getPrototype().put(name, getPrototype(), value);
		}
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{
		if (wrappedValue instanceof List< ? >)
		{
			List<Object> lst = (List<Object>)wrappedValue;
			Object prev = getSabloValueForIndex(index);
			Object val = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, prev == Scriptable.NOT_FOUND ? null : prev,
				getArrayElementDescription(), webObjectContext);
			while (lst.size() <= index)
			{
				lst.add(null);
			}
			lst.set(index, val);
		}
		else
		{
			// so that JS can use usual put/set even for non-defined things in PropertyDescription by forwarding to prototype
			getPrototype().put(index, getPrototype(), value);
		}
	}

	@Override
	public void delete(String name)
	{
		if (wrappedValue instanceof Map)
		{
			if (((Map)wrappedValue).containsKey(name))
			{
				((Map)wrappedValue).remove(name);
			}
			else
			{
				// so that JS can use usual put/set even for non-defined things in PropertyDescription by forwarding to prototype
				getPrototype().delete(name);
			}
		}
		else
		{
			// so that JS can use usual put/set even for non-defined things in PropertyDescription by forwarding to prototype
			getPrototype().delete(name);
		}
	}

	@Override
	public void delete(int index)
	{
		if (wrappedValue instanceof List< ? >)
		{
			((List< ? >)wrappedValue).set(index, null);
		}
		else
		{
			// so that JS can use usual put/set even for non-defined things in PropertyDescription by forwarding to prototype
			getPrototype().delete(index);
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
					((ISabloComponentToRhino)type).isValueAvailableInRhino(entry.getValue(), pd, webObjectContext))
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

	@Override
	public String toString()
	{
		return "RhinoMapOrArrayWrapper: " + wrappedValue;
	}

}