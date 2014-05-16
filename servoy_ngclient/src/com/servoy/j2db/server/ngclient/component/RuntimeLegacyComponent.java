/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.awt.Dimension;
import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.NGClientForJsonConverter.ConversionLocation;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.util.Utils;

/**
 * Runtime component for legacy scripting methods from default Servoy components.
 * 
 * @author lvostinar
 *	
 */
@SuppressWarnings("nls")
public class RuntimeLegacyComponent implements Scriptable
{
	private final WebFormComponent component;
	private final PutPropertyCallable putCallable;
	private final GetPropertyCallable getCallable;
	private static Map<String, String> ScriptNameToSpecName;
	private Map<Object, Object> clientProperties;
	static
	{
		ScriptNameToSpecName = new HashMap<String, String>();
		ScriptNameToSpecName.put("bgcolor", StaticContentSpecLoader.PROPERTY_BACKGROUND.getPropertyName());
		ScriptNameToSpecName.put("fgcolor", StaticContentSpecLoader.PROPERTY_FOREGROUND.getPropertyName());
		ScriptNameToSpecName.put("width", StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
		ScriptNameToSpecName.put("height", StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
		ScriptNameToSpecName.put("locationX", StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
		ScriptNameToSpecName.put("locationY", StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
		ScriptNameToSpecName.put("border", StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName());
		ScriptNameToSpecName.put("font", StaticContentSpecLoader.PROPERTY_FONTTYPE.getPropertyName());
	}

	public RuntimeLegacyComponent(WebFormComponent component)
	{
		this.component = component;
		putCallable = new PutPropertyCallable(this);
		getCallable = new GetPropertyCallable(this);
	}

	@Override
	public String getClassName()
	{
		return null;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		if ("putClientProperty".equals(name))
		{
			putCallable.setProperty("clientProperty");
			return putCallable;
		}
		if (name.startsWith("get") || name.startsWith("is") || name.startsWith("set"))
		{
			String newName = name.substring(name.startsWith("is") ? 2 : 3);

			// Make the bean property name.
			char ch0 = newName.charAt(0);
			if (Character.isUpperCase(ch0))
			{
				if (newName.length() == 1)
				{
					newName = newName.toLowerCase();
				}
				else
				{
					char ch1 = newName.charAt(1);
					if (!Character.isUpperCase(ch1))
					{
						newName = Character.toLowerCase(ch0) + newName.substring(1);
					}
				}
			}
			if (name.startsWith("set"))
			{
				putCallable.setProperty(newName);
				return putCallable;
			}
			else
			{
				getCallable.setProperty(newName);
				return getCallable;
			}
		}
		boolean isReadonly = false;
		if (name.equals("readOnly"))
		{
			isReadonly = true;
			name = StaticContentSpecLoader.PROPERTY_EDITABLE.getPropertyName();
		}

		if (!isLegacyProperty(name)) return Scriptable.NOT_FOUND;

		Object value = convertValue(name, component.getConvertedPropertyWithDefault(convertName(name),
			StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName().equals(name), !needsValueConversion(name)));

		if (isReadonly && value instanceof Boolean)
		{
			return !((Boolean)value).booleanValue();
		}

		return value;
	}

	private boolean isLegacyProperty(String name)
	{
		boolean validProperty = false; // to avoid new properties for legacy portals for example (relatedFoundset, childElements)...
		Iterator<Element> it = StaticContentSpecLoader.getContentSpec().getPropertiesForObjectType(
			component.getFormElement().getLegacyPersistIfAvailable().getTypeID());
		while (it.hasNext())
		{
			if (convertName(name).equals(it.next().getName()))
			{
				validProperty = true;
				break;
			}
		}
		return validProperty;
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		return null;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		return false;
	}

	@Override
	public boolean has(int index, Scriptable start)
	{
		return false;
	}

	@Override
	public void put(String name, Scriptable start, Object value)
	{
		if (StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName().equals(name))
		{
			//cannot set
			return;
		}
		if (name.equals("readOnly"))
		{
			name = StaticContentSpecLoader.PROPERTY_EDITABLE.getPropertyName();
			if (value instanceof Boolean)
			{
				value = !((Boolean)value).booleanValue();
			}
		}
		component.putProperty(convertName(name), value, ConversionLocation.SERVER);
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{

	}

	@Override
	public void delete(String name)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(int index)
	{

	}

	@Override
	public Scriptable getPrototype()
	{
		return null;
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{

	}

	@Override
	public Scriptable getParentScope()
	{
		return null;
	}

	@Override
	public void setParentScope(Scriptable parent)
	{

	}

	@Override
	public Object[] getIds()
	{
		return new Object[0];
	}

	@Override
	public Object getDefaultValue(Class< ? > hint)
	{
		return null;
	}

	@Override
	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}

	private String convertName(String name)
	{
		if (ScriptNameToSpecName.containsKey(name))
		{
			return ScriptNameToSpecName.get(name);
		}
		return name;
	}

	private Object convertValue(String name, Object value)
	{
		if ("width".equals(name) && value instanceof Dimension)
		{
			return Integer.valueOf(((Dimension)value).width);
		}
		if ("height".equals(name) && value instanceof Dimension)
		{
			return Integer.valueOf(((Dimension)value).height);
		}
		if ("locationX".equals(name) && value instanceof Point)
		{
			return Integer.valueOf(((Point)value).x);
		}
		if ("locationY".equals(name) && value instanceof Point)
		{
			return Integer.valueOf(((Point)value).y);
		}
		return value;
	}

	private boolean needsValueConversion(String name)
	{
		if ("width".equals(name) || "height".equals(name) || "locationX".equals(name) || "locationY".equals(name))
		{
			return true;
		}
		return false;
	}

	public void putClientProperty(Object key, Object value)
	{
		if (clientProperties == null)
		{
			clientProperties = new HashMap<Object, Object>();
		}
		clientProperties.put(key, value);
	}

	public Object getClientProperty(Object key)
	{
		if (clientProperties == null) return null;
		return clientProperties.get(key);
	}

	private abstract class PropertyCallable implements Callable
	{
		protected final Scriptable scriptable;
		protected String propertyName;

		public PropertyCallable(Scriptable scriptable)
		{
			this.scriptable = scriptable;
		}

		public void setProperty(String propertyName)
		{
			this.propertyName = propertyName;
		}
	}

	private class PutPropertyCallable extends PropertyCallable
	{
		public PutPropertyCallable(Scriptable scriptable)
		{
			super(scriptable);
		}

		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
		{
			Object value = args;
			if (args != null && args.length == 1)
			{
				value = args[0];
			}
			if ("clientProperty".equals(propertyName) && args != null && args.length >= 2)
			{
				putClientProperty(args[0], args[1]);
			}
			scriptable.put(propertyName, null, value);
			return null;
		}
	}

	private class GetPropertyCallable extends PropertyCallable
	{
		public GetPropertyCallable(Scriptable scriptable)
		{
			super(scriptable);
		}

		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
		{
			if (propertyName.equals("absoluteFormLocationY") && component.getFormElement().getLegacyPersistIfAvailable() instanceof IFormElement) //$NON-NLS-1$
			{
				return Integer.valueOf(((IFormElement)component.getFormElement().getLegacyPersistIfAvailable()).getLocation().y);
			}

			if ("clientProperty".equals(propertyName) && args != null && args.length > 0)
			{
				return getClientProperty(args[0]);
			}

			if (propertyName.equals("designTimeProperty") && args != null && args.length > 0 &&
				component.getFormElement().getLegacyPersistIfAvailable() instanceof AbstractBase)
			{
				return Utils.parseJSExpression(((AbstractBase)component.getFormElement().getLegacyPersistIfAvailable()).getCustomDesignTimeProperty((String)args[0]));
			}

			return scriptable.get(propertyName, null);
		}
	}
}
