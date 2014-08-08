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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.DataConverterContext;
import org.sablo.specification.property.IComplexPropertyValue;
import org.sablo.specification.property.IComplexTypeImpl;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.IServerObjToJavaPropertyConverter;
import org.sablo.specification.property.IWrapperType;
import org.sablo.websocket.ConversionLocation;

import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.scripting.WebComponentFunction;
import com.servoy.j2db.util.Debug;

/**
 * @author lvostinar
 *
 */
public class RuntimeWebComponent implements Scriptable
{
	private final WebFormComponent component;
	private Scriptable prototypeScope;
	private final Set<String> specProperties;
	private final Set<String> dataProviderProperties;
	private final Map<String, IServerObjToJavaPropertyConverter< ? , ? >> complexProperties;
	private final Map<String, Function> apiFunctions;

	public RuntimeWebComponent(WebFormComponent component, WebComponentSpecification webComponentSpec)
	{
		this.component = component;
		this.specProperties = new HashSet<String>();
		this.apiFunctions = new HashMap<String, Function>();
		this.dataProviderProperties = new HashSet<>();
		this.complexProperties = new HashMap<>();

		String serverScript = webComponentSpec.getServerScript();
		Scriptable apiObject = null;
		if (serverScript != null)
		{
			Context context = Context.enter();
			try
			{
				Script script = context.compileString(serverScript, webComponentSpec.getName(), 0, null);
				ScriptableObject topLevel = context.initStandardObjects();
				Scriptable scopeObject = context.newObject(topLevel);
				apiObject = context.newObject(topLevel);
				apiObject.setPrototype(this);
				scopeObject.put("api", scopeObject, apiObject);
				scopeObject.put("model", scopeObject, this);
				topLevel.put("$scope", topLevel, scopeObject);
				script.exec(context, topLevel);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
			finally
			{
				Context.exit();
			}
		}
		if (webComponentSpec != null)
		{
			for (WebComponentApiDefinition def : webComponentSpec.getApiFunctions().values())
			{
				Function func = null;
				if (apiObject != null)
				{
					Object serverSideFunction = apiObject.get(def.getName(), apiObject);
					if (serverSideFunction instanceof Function)
					{
						func = (Function)serverSideFunction;
					}
				}
				if (func != null) apiFunctions.put(def.getName(), func);
				else apiFunctions.put(def.getName(), new WebComponentFunction(component, def));
			}
			Map<String, PropertyDescription> specs = webComponentSpec.getProperties();
			for (Entry<String, PropertyDescription> e : specs.entrySet())
			{
				IPropertyType< ? > type = e.getValue().getType();
				if (!component.isDesignOnlyProperty(e.getKey()))
				{
					// design properties cannot be accessed at runtime
					// all handlers are design properties, all api is runtime
					specProperties.add(e.getKey());
				}
				if (type == DataproviderPropertyType.INSTANCE)
				{
					dataProviderProperties.add(e.getKey());
				}
				else if (type instanceof IComplexTypeImpl)
				{
					IServerObjToJavaPropertyConverter< ? , ? > javascriptToPropertyConverter = ((IComplexTypeImpl)type).getServerObjectToJavaPropertyConverter(e.getValue().isArray());
					if (javascriptToPropertyConverter != null)
					{
						// we have a custom property that is able to convert values for javascript
						complexProperties.put(e.getKey(), javascriptToPropertyConverter);
					}
				}
			}
		}
	}

	@Override
	public String getClassName()
	{
		return null;
	}

	@Override
	public Object get(String name, final Scriptable start)
	{
		if (specProperties != null && specProperties.contains(name))
		{
			if (complexProperties.containsKey(name))
			{
				Object val = component.getProperty(name);
				if (val instanceof IComplexPropertyValue)
				{
					Object scriptValue = ((IComplexPropertyValue)val).toServerObj();
					return (scriptValue == IComplexPropertyValue.NOT_AVAILABLE ? Scriptable.NOT_FOUND : scriptValue);
				}
			}
			Object value = component.getConvertedPropertyWithDefault(name, dataProviderProperties.contains(name), true);
			if (value instanceof Map || value instanceof Object[] || value instanceof List< ? >)
			{
				return new MapOrArrayWrapper(component, name, dataProviderProperties.contains(name),
					component.getFormElement().getWebComponentSpec().getProperty(name), component.getDataConverterContext());
			}
			return DesignConversion.toStringObject(value, component.getFormElement().getWebComponentSpec().getProperty(name).getType());
		}
		if (apiFunctions.containsKey(name))
		{
			return apiFunctions.get(name);
		}

		// check if we have a setter/getter for this property
		if (name != null && name.length() > 0)
		{
			String uName = new StringBuffer(name.substring(0, 1).toUpperCase()).append(name.substring(1)).toString();
			if (apiFunctions.containsKey("set" + uName) && apiFunctions.containsKey("get" + uName))
			{
				// call getter
				Function propertyGetter = apiFunctions.get("get" + uName);
				return propertyGetter.call(null, null, null, null);
			}
		}

		if ("markupId".equals(name))
		{
			return ComponentFactory.getMarkupId(component.getFormElement().getForm().getName(), component.getName());
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
		if (specProperties != null && specProperties.contains(name))
		{
			if (complexProperties.containsKey(name))
			{
				return complexProperties.get(name).usesServerObjRepresentation();
			}
			else return true;
		}
		if (apiFunctions.containsKey(name)) return true;

		// check if we have a setter/getter for this property
		if (name != null && name.length() > 0)
		{
			String uName = new StringBuffer(name.substring(0, 1).toUpperCase()).append(name.substring(1)).toString();
			return (apiFunctions.containsKey("set" + uName) && apiFunctions.containsKey("get" + uName));
		}
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
		if (dataProviderProperties.contains(name))
		{
			// cannot set, not supported
			return;
		}
		if (specProperties != null && specProperties.contains(name))
		{
			Object val = RhinoConversion.convert(value, component.getProperty(name), component.getFormElement().getWebComponentSpec().getProperty(name),
				component.getDataConverterContext());
			component.setProperty(name, val, ConversionLocation.SERVER);
		}
		else if (prototypeScope != null)
		{
			if (!apiFunctions.containsKey(name))
			{
				// check if we have a setter for this property
				if (name != null && name.length() > 0)
				{
					String uName = new StringBuffer(name.substring(0, 1).toUpperCase()).append(name.substring(1)).toString();
					if (apiFunctions.containsKey("set" + uName) && apiFunctions.containsKey("get" + uName))
					{
						// call setter
						Function propertySetter = apiFunctions.get("set" + uName);
						propertySetter.call(null, null, null, new Object[] { value });
						return;
					}
				}
				prototypeScope.put(name, start, value);
			}
		}
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
		return null;
	}

	@Override
	public void setParentScope(Scriptable parent)
	{
	}

	@Override
	public Object[] getIds()
	{
		ArrayList<String> al = new ArrayList<>();
		al.addAll(specProperties);
		al.addAll(apiFunctions.keySet());
		for (Entry<String, IServerObjToJavaPropertyConverter< ? , ? >> cp : complexProperties.entrySet())
		{
			if (!cp.getValue().usesServerObjRepresentation()) al.remove(cp.getKey());
		}
		return al.toArray();
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

	/**
	 * @author jcompagner
	 */
	private final static class MapOrArrayWrapper implements Scriptable
	{
		private final Object parentValue;
		private final String property;
		private final boolean design;
		private final int indexProperty;
		private final PropertyDescription propertyDescription;
		private final IServoyDataConverterContext converterContext;
		private Scriptable prototype;
		private Scriptable parent;

		public MapOrArrayWrapper(WebFormComponent parentValue, String property, boolean design, PropertyDescription propertyDescription,
			IServoyDataConverterContext converterContext)
		{
			this.parentValue = parentValue;
			this.property = property;
			this.design = design;
			this.propertyDescription = propertyDescription;
			this.converterContext = converterContext;
			this.indexProperty = -1;
		}

		public MapOrArrayWrapper(MapOrArrayWrapper parentValue, String property, PropertyDescription propertyDescription,
			IServoyDataConverterContext converterContext)
		{
			this.parentValue = parentValue;
			this.property = property;
			this.propertyDescription = propertyDescription;
			this.converterContext = converterContext;
			this.design = false;
			this.indexProperty = -1;
		}

		public MapOrArrayWrapper(MapOrArrayWrapper parentValue, int indexProperty, PropertyDescription propertyDescription,
			IServoyDataConverterContext converterContext)
		{
			this.parentValue = parentValue;
			this.indexProperty = indexProperty;
			this.propertyDescription = propertyDescription;
			this.converterContext = converterContext;
			this.property = null;
			this.design = false;
		}

		private WebFormComponent getWebFormComponent()
		{
			if (parentValue instanceof WebFormComponent) return (WebFormComponent)parentValue;
			return ((MapOrArrayWrapper)parentValue).getWebFormComponent();
		}

		private Object getValue()
		{
			if (parentValue instanceof WebFormComponent)
			{
				return ((WebFormComponent)parentValue).getConvertedPropertyWithDefault(property, design, true);
			}
			if (indexProperty != -1)
			{
				return ((MapOrArrayWrapper)parentValue).get(indexProperty);
			}
			return ((MapOrArrayWrapper)parentValue).get(property);
		}

		@Override
		public String getClassName()
		{
			return getValue() instanceof Map ? "Object" : "Array";
		}

		public Object get(String name)
		{
			Object value = getValue();
			if (value instanceof Map)
			{
				return ((Map<String, Object>)value).get(name);
			}
			else if (value instanceof Object[] && name.equals("length"))
			{
				return Integer.valueOf(((Object[])value).length);
			}
			else if (value instanceof List && name.equals("length"))
			{
				return Integer.valueOf(((List< ? >)value).size());
			}
			return null;
		}

		@Override
		public Object get(String name, Scriptable start)
		{
			Object value = get(name);

			PropertyDescription propDesc = propertyDescription.getProperty(name);
			if (value instanceof Map || value instanceof Object[] || value instanceof List< ? >)
			{
				return new MapOrArrayWrapper(this, name, propDesc, converterContext);
			}
			return propDesc != null ? DesignConversion.toStringObject(value, propDesc.getType()) : value;
		}

		public Object get(int index)
		{
			Object value = getValue();
			if (value instanceof Object[])
			{
				if (((Object[])value).length > index)
				{
					return ((Object[])value)[index];
				}
			}
			else if (value instanceof List< ? >)
			{
				if (((List< ? >)value).size() > index)
				{
					return ((List< ? >)value).get(index);
				}
			}
			return null;
		}

		@Override
		public Object get(int index, Scriptable start)
		{
			Object value = get(index);
			if (value instanceof Map || value instanceof Object[] || value instanceof List< ? >)
			{
				return new MapOrArrayWrapper(this, index, propertyDescription.asArrayElement(), converterContext);
			}
			return DesignConversion.toStringObject(value, propertyDescription.getType());
		}

		@Override
		public boolean has(String name, Scriptable start)
		{
			Object value = getValue();
			if (value instanceof Map)
			{
				return ((Map)value).containsKey(name);
			}
			return false;
		}

		@Override
		public boolean has(int index, Scriptable start)
		{
			Object value = getValue();
			if (value instanceof Object[])
			{
				return ((Object[])value).length > index;
			}
			else if (value instanceof List< ? >)
			{
				return ((List< ? >)value).size() > index;
			}
			else if (value instanceof MapOrArrayWrapper)
			{
				return ((MapOrArrayWrapper)value).has(index, start);
			}
			return false;
		}

		@Override
		public void put(String name, Scriptable start, Object value)
		{
			Object mapValue = getValue();
			if (mapValue instanceof Map)
			{
				Object convertedValue = convertValue(value, propertyDescription.getProperty(name), ((Map)mapValue).get(name));
				((Map)mapValue).put(name, convertedValue);
				markAsChanged();
			}
		}

		/**
		 * @param value
		 * @param propertyDesc
		 * @param oldValue
		 * @return
		 */
		private Object convertValue(Object value, PropertyDescription propertyDesc, Object oldValue)
		{
			// first convert the rhino value to a java value.
			Object convertedValue = RhinoConversion.convert(value, oldValue, propertyDesc, converterContext);
			// now convert it to an internal WebObject value (so it must be wrapped)
			// TODO should this be done in BaseWebObject itself? So set the property with dots?
			if (propertyDesc != null && propertyDesc.getType() instanceof IWrapperType< ? , ? >)
			{
				IWrapperType type = (IWrapperType)propertyDesc.getType();
				convertedValue = type.wrap(convertedValue, oldValue, new DataConverterContext(propertyDesc, getWebFormComponent()));
			}
			return convertedValue;
		}

		private void markAsChanged()
		{
			if (parentValue instanceof WebFormComponent)
			{
				((WebFormComponent)parentValue).flagPropertyChanged(property);
			}
			else if (parentValue instanceof MapOrArrayWrapper)
			{
				((MapOrArrayWrapper)parentValue).markAsChanged();
			}

		}

		@Override
		public void put(int index, Scriptable start, Object value)
		{
			Object arrayValue = getValue();
			if (arrayValue instanceof Object[])
			{
				Object val = convertValue(value, propertyDescription.asArrayElement(), ((Object[])arrayValue).length > index ? ((Object[])arrayValue)[index]
					: null);
				if (((Object[])arrayValue).length > index)
				{
					((Object[])arrayValue)[index] = val;
					markAsChanged();
				}
				else
				{
					// array has to grow bigger.
					Object[] newArray = new Object[index + 1];
					newArray[index] = val;
					System.arraycopy(arrayValue, 0, newArray, 0, ((Object[])arrayValue).length);
					// store the new array in the parent.
					if (parentValue instanceof Scriptable)
					{
						if (indexProperty != -1)
						{
							((Scriptable)parentValue).put(indexProperty, (Scriptable)parentValue, newArray);
						}
						else
						{
							((Scriptable)parentValue).put(property, (Scriptable)parentValue, newArray);
						}
						markAsChanged();
					}
					else
					{

						((WebFormComponent)parentValue).setProperty(property, newArray, ConversionLocation.SERVER);
					}
				}
			}
			else if (arrayValue instanceof List< ? >)
			{
				List<Object> lst = (List<Object>)arrayValue;
				Object val = convertValue(value, propertyDescription.asArrayElement(), lst.size() > index ? lst.get(index) : null);
				while (lst.size() <= index)
				{
					lst.add(null);
				}
				lst.set(index, val);
				markAsChanged();
			}
		}

		@Override
		public void delete(String name)
		{
			Object value = getValue();
			if (value instanceof Map)
			{
				((Map)value).remove(name);
				markAsChanged();
			}
		}

		@Override
		public void delete(int index)
		{
			Object value = getValue();
			if (value instanceof Object[])
			{
				((Object[])value)[index] = null;
				markAsChanged();
			}
			else if (value instanceof List< ? >)
			{
				((List< ? >)value).set(index, null);
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
			Object value = getValue();
			if (value instanceof Map)
			{
				return ((Map)value).keySet().toArray(new Object[0]);
			}
			else if (value instanceof Object[] || value instanceof List< ? >)
			{
				int length = 0;
				if (value instanceof Object[]) length = ((Object[])value).length;
				else length = ((List< ? >)value).size();
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
			Object value = getValue();
			if (value != null) return value.toString();
			return null;
		}

		@Override
		public boolean hasInstance(Scriptable instance)
		{
			return false;
		}
	}

}
