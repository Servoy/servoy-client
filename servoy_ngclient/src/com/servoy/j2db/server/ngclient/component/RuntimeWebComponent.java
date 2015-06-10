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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.VisiblePropertyType;

import com.servoy.j2db.FormController;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.scripting.IInstanceOf;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.LabelForPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.scripting.WebComponentFunction;
import com.servoy.j2db.server.ngclient.scripting.WebServiceScriptable;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class RuntimeWebComponent implements Scriptable, IInstanceOf
{
	private final WebFormComponent component;
	private Scriptable prototypeScope;
	private final Set<String> specProperties;
	private final Map<String, Function> apiFunctions;
	private final WebComponentSpecification webComponentSpec;
	private Scriptable parentScope;

	public RuntimeWebComponent(WebFormComponent component, WebComponentSpecification webComponentSpec)
	{
		this.component = component;
		setParentScope(component.getDataConverterContext().getApplication().getScriptEngine().getSolutionScope());
		this.specProperties = new HashSet<String>();
		this.apiFunctions = new HashMap<String, Function>();
		this.webComponentSpec = webComponentSpec;

		URL serverScript = webComponentSpec.getServerScript();
		Scriptable apiObject = null;
		if (serverScript != null)
		{
			apiObject = WebServiceScriptable.compileServerScript(serverScript, this, component.getDataConverterContext().getApplication());
		}
		if (webComponentSpec != null)
		{
			for (WebComponentApiDefinition def : webComponentSpec.getApiFunctions().values())
			{
				if (isApiFunctionEnabled(def.getName()))
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
			}
			Map<String, PropertyDescription> specs = webComponentSpec.getProperties();
			for (String propName : specs.keySet())
			{
				if (!component.isDesignOnlyProperty(propName))
				{
					// design properties and private properties cannot be accessed at runtime
					// all handlers are design properties, all api is runtime
					specProperties.add(propName);
				}
			}
		}
	}

	protected boolean isApiFunctionEnabled(String functionName)
	{
		FormElement fe = component.getFormElement();
		if (fe.isLegacy() && fe.getPersistIfAvailable() instanceof ISupportAnchors)
		{
			int anchor = ((ISupportAnchors)fe.getPersistIfAvailable()).getAnchors();
			if ((anchor == 0 || anchor == (IAnchorConstants.NORTH + IAnchorConstants.WEST) || (fe.getForm().getView() == FormController.TABLE_VIEW || fe.getForm().getView() == FormController.LOCKED_TABLE_VIEW)) &&
				(("getLocationX").equals(functionName) || ("getLocationY").equals(functionName) || ("getWidth").equals(functionName) || ("getHeight").equals(functionName)))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isInstance(String name)
	{
		if ("RuntimeComponent".equals(name)) return true;
		if (getPrototype() instanceof IInstanceOf)
		{
			return ((IInstanceOf)getPrototype()).isInstance(name);
		}
		return false;
	}

	@Override
	public String getClassName()
	{
		return "RuntimeComponent";
	}

	@Override
	public Object get(String name, final Scriptable start)
	{
		if (specProperties != null && specProperties.contains(name))
		{
			PropertyDescription pd = webComponentSpec.getProperties().get(name);
			return NGConversions.INSTANCE.convertSabloComponentToRhinoValue(component.getProperty(name), pd, component, start);
		}
		Function func = apiFunctions.get(name);
		if (func != null)
		{
			return func;
		}
		// check if we have a setter/getter for this property
		if (name != null && name.length() > 0)
		{
			String uName = new StringBuffer(name.substring(0, 1).toUpperCase()).append(name.substring(1)).toString();
			if (apiFunctions.containsKey("set" + uName) && apiFunctions.containsKey("get" + uName))
			{
				// call getter
				Function propertyGetter = apiFunctions.get("get" + uName);
				return propertyGetter.call(Context.getCurrentContext(), start, start, new Object[] { });
			}
		}

		if ("svyMarkupId".equals(name))
		{
			return ComponentFactory.getMarkupId(component.getFormElement().getForm().getName(), component.getName());
		}
		if ("getFormName".equals(name))
		{
			return new Callable()
			{

				@Override
				public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
				{
					Container parent = component.getParent();
					while (!(parent instanceof IWebFormUI))
					{
						parent = parent.getParent();
					}
					return ((IWebFormUI)parent).getController().getName();
				}
			};
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
			PropertyDescription pd = webComponentSpec.getProperty(name);
			IPropertyType< ? > type = pd.getType();
			// it is available by default, so if it doesn't have conversion, or if it has conversion and is explicitly available
			return !(type instanceof ISabloComponentToRhino< ? >) ||
				((ISabloComponentToRhino)type).isValueAvailableInRhino(component.getProperty(name), pd, component);
		}

		if (apiFunctions.containsKey(name)) return true;

		// check if we have a setter/getter for this property
		if (name != null && name.length() > 0)
		{
			String uName = new StringBuffer(name.substring(0, 1).toUpperCase()).append(name.substring(1)).toString();
			return (apiFunctions.containsKey("set" + uName) && apiFunctions.containsKey("get" + uName));
		}
		if ("getFormName".equals(name)) //$NON-NLS-1$
		{
			return true;
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
		if (specProperties != null && specProperties.contains(name))
		{
			Object previousVal = component.getProperty(name);
			PropertyDescription pd = webComponentSpec.getProperties().get(name);
			Object val = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, previousVal, pd, component);

			if (val != previousVal) component.setProperty(name, val);

			if (pd != null && pd.getType() instanceof VisiblePropertyType)
			{
				// search all labelfor elements
				for (WebComponent siblingComponent : component.getParent().getComponents())
				{
					Collection<PropertyDescription> labelFors = siblingComponent.getSpecification().getProperties(LabelForPropertyType.INSTANCE);
					if (labelFors != null)
					{
						for (PropertyDescription labelForProperty : labelFors)
						{
							if (Utils.equalObjects(component.getName(), siblingComponent.getProperty(labelForProperty.getName())))
							{
								// sibling component is labelfor, so set value to all its visible properties
								Collection<PropertyDescription> visibleProperties = siblingComponent.getSpecification().getProperties(
									VisiblePropertyType.INSTANCE);
								if (visibleProperties != null)
								{
									for (PropertyDescription visibleProperty : visibleProperties)
									{
										previousVal = siblingComponent.getProperty(visibleProperty.getName());
										val = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, previousVal, visibleProperty, siblingComponent);

										if (val != previousVal) siblingComponent.setProperty(name, val);
									}
								}
								break;
							}
						}
					}
				}
			}
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
						propertySetter.call(Context.getCurrentContext(), start, start, new Object[] { value });
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
		ArrayList<String> al = new ArrayList<>();
		for (String name : specProperties)
		{
			PropertyDescription pd = webComponentSpec.getProperty(name);
			IPropertyType< ? > type = pd.getType();
			if (!(type instanceof ISabloComponentToRhino< ? >) ||
				((ISabloComponentToRhino)type).isValueAvailableInRhino(component.getProperty(name), pd, component))
			{
				al.add(name);
			}
		}
		al.addAll(apiFunctions.keySet());
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

	public WebFormComponent getComponent()
	{
		return component;
	}

}