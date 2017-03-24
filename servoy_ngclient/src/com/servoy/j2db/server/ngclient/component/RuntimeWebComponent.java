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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.VisiblePropertyType;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IWindow;

import com.servoy.j2db.FormController;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.scripting.IInstanceOf;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.INGClientWindow;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.property.types.LabelForPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.scripting.WebComponentFunction;
import com.servoy.j2db.server.ngclient.scripting.WebServiceScriptable;
import com.servoy.j2db.util.Pair;
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
	private final WebObjectSpecification webComponentSpec;
	private Scriptable parentScope;
	private Scriptable scopeObject;

	public RuntimeWebComponent(WebFormComponent component, WebObjectSpecification webComponentSpec)
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
			scopeObject = WebServiceScriptable.compileServerScript(serverScript, this, component.getDataConverterContext().getApplication());
			apiObject = (Scriptable)scopeObject.get("api", scopeObject);
		}
		if (webComponentSpec != null)
		{
			for (WebObjectFunctionDefinition def : webComponentSpec.getApiFunctions().values())
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

	public Object executeScopeFunction(WebObjectFunctionDefinition functionSpec, Object[] arrayOfSabloJavaMethodArgs)
	{
		if (functionSpec != null)
		{
			Object object = scopeObject.get(functionSpec.getName(), scopeObject);
			if (object instanceof Function)
			{
				Context context = Context.enter();
				try
				{
					// find spec for method
					List<PropertyDescription> argumentPDs = (functionSpec != null ? functionSpec.getParameters() : null);

					// convert arguments to Rhino
					Object[] array = new Object[arrayOfSabloJavaMethodArgs.length];
					for (int i = 0; i < arrayOfSabloJavaMethodArgs.length; i++)
					{
						array[i] = NGConversions.INSTANCE.convertSabloComponentToRhinoValue(arrayOfSabloJavaMethodArgs[i],
							(argumentPDs != null && argumentPDs.size() > i) ? argumentPDs.get(i) : null, component, this);
					}
					Object retValue = ((Function)object).call(context, scopeObject, scopeObject, array);

					PropertyDescription retValuePD = (functionSpec != null ? functionSpec.getReturnType() : null);
					return NGConversions.INSTANCE.convertRhinoToSabloComponentValue(retValue, null, retValuePD, component);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
					return null;
				}
				finally
				{
					Context.exit();
				}
			}
			else
			{
				throw new RuntimeException("trying to call a function '" + functionSpec.getName() + "' that does not exists on a component '" +
					component.getName() + " with spec: " + webComponentSpec.getName());
			}
		}
		return null;
	}

	protected boolean isApiFunctionEnabled(String functionName)
	{
		boolean isLocationOrSize = "getLocationX".equals(functionName) || "getLocationY".equals(functionName) || "getWidth".equals(functionName) ||
			"getHeight".equals(functionName);

		if (isLocationOrSize)
		{
			// if parent form not visible, don't call api, let it get from the model on the server
			Container parent = component.getParent();
			while (parent != null)
			{
				if (parent instanceof WebFormUI)
				{
					boolean isFormVisible = false;
					IWindow currentWindow = CurrentWindow.safeGet();
					if (currentWindow instanceof INGClientWindow)
					{
						isFormVisible = ((INGClientWindow)currentWindow).hasForm(parent.getName());
					}
					if (isFormVisible)
					{
						// it is reported to be still on the client
						// but do check also if it is not about to be hidden, by having
						// formVisible already set to false in the controller
						isFormVisible = ((WebFormUI)parent).getController().isFormVisible();
					}

					if (!isFormVisible)
					{
						return false;
					}
					break;
				}
				parent = parent.getParent();
			}

			// if it is not table view (it can have columns moved/resize) and not anchored, no need to call api, let it get from the model on the server
			FormElement fe = component.getFormElement();
			if (fe.isLegacy() && fe.getPersistIfAvailable() instanceof ISupportAnchors &&
				(fe.getForm().getView() != FormController.TABLE_VIEW && fe.getForm().getView() != FormController.LOCKED_TABLE_VIEW))
			{
				int anchor = Utils.getAsInteger(component.getProperty(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName()));//((ISupportAnchors)fe.getPersistIfAvailable()).getAnchors();
				if ((anchor == 0 || anchor == (IAnchorConstants.NORTH + IAnchorConstants.WEST)))
				{
					return false;
				}
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
	public Object get(final String name, final Scriptable start)
	{
		if (specProperties != null && specProperties.contains(name))
		{
			PropertyDescription pd = webComponentSpec.getProperties().get(name);
			if (WebFormComponent.isDesignOnlyProperty(pd)) return Scriptable.NOT_FOUND;
			return NGConversions.INSTANCE.convertSabloComponentToRhinoValue(component.getProperty(name), pd, component, start);
		}
		if ("getFormName".equals(name))
		{
			return new Callable()
			{
				@Override
				public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
				{
					IWebFormUI parent = component.findParent(IWebFormUI.class);
					if (parent != null)
					{
						return parent.getController().getName();
					}
					return null;
				}
			};
		}
		final Function func = apiFunctions.get(name);
		if (func != null && isApiFunctionEnabled(name))
		{
			final List<Pair<String, String>> oldVisibleForms = getVisibleForms();
			return new BaseFunction()
			{

				@Override
				public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
				{
					Object retValue = func.call(cx, scope, thisObj, args);
					if (!(func instanceof WebComponentFunction))
					{
						WebObjectFunctionDefinition def = webComponentSpec.getApiFunctions().get(name);
						retValue = NGConversions.INSTANCE.convertServerSideRhinoToRhinoValue(retValue, def.getReturnType(), component, null);
					}
					updateVisibleContainers(oldVisibleForms);
					return retValue;
				}
			};
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

		if (prototypeScope != null)
		{
			return prototypeScope.get(name, start);
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
		if (isInvalidValue(name, value)) return;
		List<Pair<String, String>> oldVisibleForms = getVisibleForms();
		if (specProperties != null && specProperties.contains(name))
		{
			Object previousVal = null;
			PropertyDescription pd = webComponentSpec.getProperties().get(name);
			if (pd.getType() instanceof ISabloComponentToRhino && !(pd.getType() instanceof IRhinoToSabloComponent))
			{
				// the it has sablo to rhino conversion but not the other way around then we should just use the
				// value from the conversion so call get(String,Scriptable)
				previousVal = get(name, start);
			}
			else previousVal = component.getProperty(name);
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
					}
					else
					{
						prototypeScope.put(name, start, value);
					}
				}
			}
		}
		updateVisibleContainers(oldVisibleForms);
	}

	private boolean isInvalidValue(String name, Object value)
	{
		if (component.getFormElement() != null && component.getFormElement().getPersistIfAvailable() instanceof TabPanel &&
			Utils.equalObjects(name, "tabIndex"))
		{
			Object tabs = component.getProperty("tabs");
			if (tabs instanceof List && value instanceof Number)
			{
				int index = ((Number)value).intValue() - 1;
				if (index < 0 || index >= ((List)tabs).size())
				{
					return true;
				}
			}
		}
		return false;
	}

	private List<Pair<String, String>> getVisibleForms()
	{
		List<Pair<String, String>> visibleContainedForms = new ArrayList<Pair<String, String>>();
		// legacy for now, should we do it more general, from the spec
		if (component.getFormElement() != null && component.getFormElement().getPersistIfAvailable() instanceof TabPanel)
		{
			Object tabIndex = component.getProperty("tabIndex");
			Object tabs = component.getProperty("tabs");
			if (tabs instanceof List && ((List)tabs).size() > 0)
			{
				List tabsList = (List)tabs;
				TabPanel tabpanel = (TabPanel)component.getFormElement().getPersistIfAvailable();
				if (tabpanel.getTabOrientation() == TabPanel.SPLIT_HORIZONTAL || tabpanel.getTabOrientation() == TabPanel.SPLIT_VERTICAL)
				{
					for (int i = 0; i < tabsList.size(); i++)
					{
						Map<String, Object> tab = (Map<String, Object>)tabsList.get(i);
						if (tab != null)
						{
							String relationName = tab.get("relationName") != null ? tab.get("relationName").toString() : null;
							Object form = tab.get("containsFormId");
							if (form != null)
							{
								visibleContainedForms.add(new Pair<String, String>(form.toString(), relationName));
							}
						}
					}
				}
				else
				{
					Map<String, Object> visibleTab = null;
					if (tabIndex instanceof Number && tabsList.size() > 0 && ((Number)tabIndex).intValue() <= tabsList.size())
					{
						int index = ((Number)tabIndex).intValue() - 1;
						if (index < 0)
						{
							index = 0;
						}
						visibleTab = (Map<String, Object>)(tabsList.get(index));
					}
					else if (tabIndex instanceof String || tabIndex instanceof CharSequence)
					{
						for (int i = 0; i < tabsList.size(); i++)
						{
							Map<String, Object> tab = (Map<String, Object>)tabsList.get(i);
							if (Utils.equalObjects(tabIndex, tab.get("name")))
							{
								visibleTab = tab;
								break;
							}
						}
					}
					if (visibleTab != null)
					{
						String relationName = visibleTab.get("relationName") != null ? visibleTab.get("relationName").toString() : null;
						Object form = visibleTab.get("containsFormId");
						if (form != null)
						{
							visibleContainedForms.add(new Pair<String, String>(form.toString(), relationName));
						}
					}
				}
			}
		}
		return visibleContainedForms;
	}

	private void updateVisibleContainers(List<Pair<String, String>> oldForms)
	{
		((DataAdapterList)component.getDataConverterContext().getForm().getFormUI().getDataAdapterList()).updateRelatedVisibleForms(oldForms,
			getVisibleForms());
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
			if (WebFormComponent.isDesignOnlyProperty(pd)) continue;
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

	@Override
	public String toString()
	{
		return "Component: " + component;
	}

}