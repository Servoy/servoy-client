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

package com.servoy.j2db.server.ngclient.component;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.ReadonlyPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ReadonlySabloValue;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.Utils;

/**
 * Makes it possible to get/set properties of a group of elements at runtime.
 * @author emera
 */
@SuppressWarnings("nls")
public class RuntimeWebGroup implements Scriptable
{
	private final List<RuntimeWebComponent> runtimeWebComponents = new ArrayList<RuntimeWebComponent>();
	private Scriptable parentScope;
	private Scriptable prototypeScope;
	private final PutPropertyCallable putCallable;
	private final GetPropertyCallable getCallable;
	private final String groupName;
	private static Map<String, String> properties = new HashMap<>();
	private static Set<String> api = new HashSet<>();
	private static final Rectangle NO_BOUNDS = new Rectangle(0, 0, 0, 0);

	static
	{
		properties.put("bgcolor", StaticContentSpecLoader.PROPERTY_BACKGROUND.getPropertyName());
		properties.put("fgcolor", StaticContentSpecLoader.PROPERTY_FOREGROUND.getPropertyName());
		properties.put("width", StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
		properties.put("height", StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
		properties.put("locationx", StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
		properties.put("locationy", StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
		properties.put("border", StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName());
		properties.put("font", StaticContentSpecLoader.PROPERTY_FONTTYPE.getPropertyName());
		properties.put("enabled", StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName());
		properties.put("toolTipText", StaticContentSpecLoader.PROPERTY_TOOLTIPTEXT.getPropertyName());
		properties.put("transparent", StaticContentSpecLoader.PROPERTY_TRANSPARENT.getPropertyName());
		properties.put("visible", StaticContentSpecLoader.PROPERTY_VISIBLE.getPropertyName());
		properties.put("readOnly", WebFormUI.READONLY);

		api.add("putClientProperty");
		api.add("getAbsoluteFormLocationY");
		api.add("getClientProperty");
		api.add("getDesignTimeProperty");
		api.add("getElementType");
		api.add("getFormName");
		api.add("getName");
		api.add("getLocationX");
		api.add("getLocationY");
		api.add("getWidth");
		api.add("getHeight");
		api.add("setSize");
		api.add("setLocation");
	}

	public RuntimeWebGroup(String groupName)
	{
		this.groupName = groupName;
		putCallable = new PutPropertyCallable(this);
		getCallable = new GetPropertyCallable(this);
	}

	public void add(RuntimeWebComponent runtimeComponent)
	{
		runtimeWebComponents.add(runtimeComponent);
	}

	public void remove(WebFormComponent formComponent)
	{
		Iterator<RuntimeWebComponent> it = runtimeWebComponents.iterator();
		while (it.hasNext())
		{
			RuntimeWebComponent runtimeComponent = it.next();
			if (runtimeComponent.getComponent() == formComponent)
			{
				it.remove();
				break;
			}
		}
	}

	public int getComponentCount()
	{
		return runtimeWebComponents.size();
	}

	@Override
	public String getClassName()
	{
		return "RuntimeGroup";
	}

	@SuppressWarnings({ "nls", "boxing" })
	@Override
	public Object get(String rawName, Scriptable start)
	{
		if (properties.keySet().contains(rawName) || api.contains(rawName))
		{
			String name = rawName;

			if (name.startsWith("get"))
			{
				if ("getFormName".equals(name))
				{
					return runtimeWebComponents.get(0).get("getFormName", start);
				}
				getCallable.setProperty(rawName.substring(3).toLowerCase());
				return getCallable;
			}
			else if (name.startsWith("set") || name.startsWith("put"))
			{
				putCallable.setProperty(rawName.substring(3).toLowerCase());
				return putCallable;
			}

			switch (name)
			{
				case "visible" :
				case "enabled" :
				case "transparent" :
					for (RuntimeWebComponent component : runtimeWebComponents)
					{
						Object value = component.get(properties.get(name), start);
						if (value != null && (boolean)value) return Boolean.TRUE;
					}
					return Boolean.FALSE;

				case "bgcolor" :
				case "border" :
				case "fgcolor" :
				case "font" :
				case "toolTipText" :
					for (RuntimeWebComponent component : runtimeWebComponents)
					{
						Object value = component.get(properties.get(name), start);
						if (value != Scriptable.NOT_FOUND && value != null) return value;
					}
					return null;

				case "readOnly" :
					for (RuntimeWebComponent component : runtimeWebComponents)
					{
						if (component.has(WebFormUI.READONLY, start) && !((boolean)component.get(WebFormUI.READONLY, start)))
						{
							return Boolean.FALSE;
						}
					}
					return Boolean.TRUE;
				case "width" :
					return getBounds().width;
				case "height" :
					return getBounds().height;
				case "locationx" :
					return getBounds().x;
				case "locationy" :
					return getBounds().y;

			}
		}
		return Scriptable.NOT_FOUND;
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		return null;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		return properties.keySet().contains(name) || api.contains(name);
	}

	@Override
	public boolean has(int index, Scriptable start)
	{
		return false;
	}

	@Override
	public void put(String name, Scriptable start, Object value)
	{
		if (!properties.containsKey(name)) return;
		String propertyName = properties.get(name);
		for (RuntimeWebComponent obj : runtimeWebComponents)
		{
			putProperty(propertyName, value, obj.getComponent());
		}
	}

	private void putProperty(String propertyName, Object value, WebFormComponent component)
	{
		if (propertyName == null) return;
		Object previousVal = component.getProperty(propertyName);
		Object val = null;
		if (propertyName.equals(WebFormUI.READONLY))
		{
			val = Boolean.valueOf((boolean)value);
			Object readonlyproperty = component.getProperty(WebFormUI.READONLY);
			if (readonlyproperty instanceof ReadonlySabloValue)
			{
				ReadonlySabloValue oldValue = (ReadonlySabloValue)readonlyproperty;
				//use the rhino conversion to convert from boolean to ReadOnlySabloValue
				PropertyDescription pd = component.getFormElement().getWebComponentSpec().getProperty(WebFormUI.READONLY);
				if (pd != null) val = ReadonlyPropertyType.INSTANCE.toSabloComponentValue(val, oldValue, pd, component);
			}
		}
		else
		{
			val = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, previousVal, component.getSpecification().getProperties().get(propertyName),
				component);
		}

		if (val != previousVal) component.setProperty(propertyName, val);
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{
	}

	@Override
	public void delete(String name)
	{
	}

	@Override
	public void delete(int index)
	{
	}

	@Override
	public Scriptable getPrototype()
	{
		return prototypeScope;
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{
		this.prototypeScope = prototype;
	}

	@Override
	public Scriptable getParentScope()
	{
		return parentScope;
	}

	@Override
	public void setParentScope(Scriptable parent)
	{
		parentScope = parent;
	}

	@Override
	public Object[] getIds()
	{
		ArrayList<String> ids = new ArrayList<>();
		ids.addAll(properties.keySet());
		ids.addAll(api);
		return ids.toArray();
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

	private class PutPropertyCallable extends BaseFunction
	{
		private final Scriptable scriptable;
		private String property;

		public PutPropertyCallable(Scriptable scriptable)
		{
			this.scriptable = scriptable;
		}

		public void setProperty(String name)
		{
			this.property = name;
		}

		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
		{
			Object value = args;
			if (args != null)
			{
				if (args.length == 1)
				{
					value = args[0];
				}
				else if (args.length == 2)
				{
					if (property.equals("size")) setSize(Utils.getAsInteger(args[0]), Utils.getAsInteger(args[1]));
					if (property.equals("location")) setLocation(Utils.getAsInteger(args[0]), Utils.getAsInteger(args[1]));
					if (property.equals("clientproperty")) putClientProperty(cx, scope, thisObj, args);
				}
			}

			scriptable.put(property, null, value);
			return null;
		}
	}


	private class GetPropertyCallable extends BaseFunction
	{
		private final Scriptable scriptable;
		private String property;

		public GetPropertyCallable(Scriptable scriptable)
		{
			this.scriptable = scriptable;
		}

		public void setProperty(String name)
		{
			this.property = name;
		}

		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
		{
			switch (property)
			{
				case "name" :
					return groupName;
				case "absoluteformlocationy" :
					return getAbsoluteFormLocationY(cx, scope, thisObj, args);
				case "elementtype" :
					return IRuntimeComponent.GROUP;
				case "clientproperty" :
					return getClientProperty(cx, scope, thisObj, args);
				case "designtimeproperty" :
					return getDesignTimeProperty(cx, scope, thisObj, args);
			}
			return scriptable.get(property, null);
		}
	}

	protected Rectangle getBounds()
	{
		Rectangle bounds = null;
		for (RuntimeWebComponent obj : runtimeWebComponents)
		{
			Dimension size = (Dimension)obj.getComponent().getProperty("size");
			Point location = (Point)obj.getComponent().getProperty("location");
			Rectangle rect = new Rectangle(location.x, location.y, size.width, size.height);
			if (bounds == null)
			{
				bounds = rect;
			}
			else
			{
				bounds = bounds.union(rect);
			}
		}
		return bounds == null ? NO_BOUNDS : bounds;
	}

	private void setSize(int width, int height)
	{
		Rectangle bounds = getBounds();
		float scalew = ((float)width) / bounds.width;
		float scaleh = ((float)height) / bounds.height;

		for (RuntimeWebComponent obj : runtimeWebComponents)
		{
			WebFormComponent component = obj.getComponent();
			Point location = (Point)component.getProperty("location");
			putProperty("location",
				new Object[] { bounds.x + (int)Math.floor(scalew * (location.x - bounds.x)), bounds.y + (int)Math.floor(scaleh * (location.y - bounds.y)) },
				component);

			Dimension size = (Dimension)component.getProperty("size");
			putProperty("size", new Object[] { (int)Math.floor(scalew * size.width), (int)Math.floor(scaleh * size.height) }, component);
		}
	}

	private void setLocation(int x, int y)
	{
		Rectangle bounds = getBounds();
		int dx = x - bounds.x;
		int dy = y - bounds.y;

		for (RuntimeWebComponent obj : runtimeWebComponents)
		{
			WebFormComponent component = obj.getComponent();
			Point location = (Point)component.getProperty("location");
			putProperty("location", new Object[] { location.x + dx, location.y + dy }, component);
		}
	}

	private Object getDesignTimeProperty(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		for (RuntimeWebComponent component : runtimeWebComponents)
		{
			Object f = component.get("getDesignTimeProperty", scope);
			if (f != null && f != Scriptable.NOT_FOUND)
			{
				Object value = ((Function)f).call(cx, scope, thisObj, args);
				if (value != null) return value;
			}
		}
		return null;
	}

	private Object getAbsoluteFormLocationY(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		int y = -1;
		for (RuntimeWebComponent component : runtimeWebComponents)
		{
			Object f = component.get("getAbsoluteFormLocationY", scope);
			if (f != null && f != Scriptable.NOT_FOUND)
			{
				y = Math.min(y == -1 ? Integer.MAX_VALUE : y, (int)((Function)f).call(cx, scope, thisObj, args));
			}
		}
		return y;
	}

	private void putClientProperty(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		for (RuntimeWebComponent component : runtimeWebComponents)
		{
			Object f = component.get("putClientProperty", scope);
			if (f != null && f != Scriptable.NOT_FOUND) ((Function)f).call(cx, scope, thisObj, args);
		}

	}

	private Object getClientProperty(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		for (RuntimeWebComponent component : runtimeWebComponents)
		{
			Object f = component.get("getClientProperty", scope);
			if (f != null && f != Scriptable.NOT_FOUND)
			{
				Object value = ((Function)f).call(cx, scope, thisObj, args);
				if (value != null) return value;
			}
		}
		return null;
	}
}
