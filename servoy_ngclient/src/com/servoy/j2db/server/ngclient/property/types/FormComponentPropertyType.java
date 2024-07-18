/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.servoy.j2db.server.ngclient.property.types;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.IPushToServerSpecialType;
import org.sablo.specification.property.ISupportsGranularUpdates;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.specification.property.types.StringPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSNGWebComponent;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.server.ngclient.property.ComponentTypeConfig;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IFormComponentRhinoConverter;
import com.servoy.j2db.util.IFormComponentType;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public class FormComponentPropertyType extends DefaultPropertyType<Object> implements IConvertedPropertyType<Object>, ISabloComponentToRhino<Object>,
	IFormElementToTemplateJSON<Object, Object>, IFormElementToSabloComponent<Object, Object>, IFormComponentType, IPushToServerSpecialType,
	ISupportsGranularUpdates<Object>, IPropertyWithClientSideConversions<Object>
{
	public static final String SVY_FORM = "svy_form"; //$NON-NLS-1$

	public static final FormComponentPropertyType INSTANCE = new FormComponentPropertyType();
	public static final String TYPE_NAME = "formcomponent"; //$NON-NLS-1$

	protected FormComponentPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		if (json == null) return null;

		String forFoundset = json.optString("forFoundset"); //$NON-NLS-1$
		// we return here a ComponentTypeConfig because this is used in the ComponentPropertyType
		return forFoundset == null || forFoundset.length() == 0 ? null : new ComponentTypeConfig(forFoundset);
	}

	@Override
	public Object fromJSON(Object newJSONValue, Object previousSabloValue, PropertyDescription propertyDescription, IBrowserConverterContext context,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newJSONValue instanceof JSONArray && previousSabloValue instanceof FormComponentSabloValue)
		{
			((FormComponentSabloValue)previousSabloValue).browserUpdatesReceived((JSONArray)newJSONValue);
		}
		return previousSabloValue;
	}


	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object sabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		if (sabloValue instanceof FormComponentSabloValue)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			((FormComponentSabloValue)sabloValue).fullToJSON(writer, this, dataConverterContext);
		} // else - if it's not from a repeater (foundset linked) - we don't send anything at all to client for this property in the form component component - all form component subcomponents are already handled though form code directly
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(Object webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Object webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService, Scriptable startScriptable)
	{
		Scriptable newObject = DefaultScope.newObject(startScriptable);
		WebFormComponent webFormComponent = (WebFormComponent)componentOrService;
		FlattenedSolution fs = webFormComponent.getDataConverterContext().getSolution();
		FormComponentCache cache = null;
		if (webComponentValue instanceof FormComponentSabloValue)
		{
			cache = ((FormComponentSabloValue)webComponentValue).getCache();
		}
		else
		{
			Form form = getForm(webComponentValue, fs);
			if (form == null) return null;
			// TODO return here a NativeScriptable object that understand the full hiearchy?
			cache = FormElementHelper.INSTANCE.getFormComponentCache(webFormComponent.getFormElement(), pd, (JSONObject)webComponentValue, form, fs);
		}
		IWebFormUI formUI = webFormComponent.findParent(IWebFormUI.class);
		String prefix = FormElementHelper.getStartElementName(webFormComponent.getFormElement(), pd);
		for (FormElement fe : cache.getFormComponentElements())
		{
			String name = fe.getName();
			if (name != null && !name.startsWith(FormElement.SVY_NAME_PREFIX))
			{
				RuntimeWebComponent webComponent = formUI.getRuntimeWebComponent(fe.getRawName());
				if (webComponent != null)
				{
					newObject.put(name.substring(prefix.length()), newObject, webComponent);
				}
			}
		}
		return newObject;

	}

	@Override
	public Object defaultValue(PropertyDescription pd)
	{
		return null;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object formElementValue, PropertyDescription pd, FormElementContext formElementContext)
		throws JSONException
	{
		if (formElementValue == null) return writer;

		FlattenedSolution fs = formElementContext.getFlattenedSolution();
		Form form = getForm(formElementValue, fs);
		if (form != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			// we output here a uuid that is a uuid that must be used to get the compiled template from the $formcomponentCache
			String uuid = FormElementHelper.INSTANCE.getFormComponentCache(formElementContext.getFormElement(), pd, (JSONObject)formElementValue, form,
				fs).getHtmlTemplateUUIDForAngular();
			writer.object();
			writer.key("uuid");
			writer.value(uuid);
			writer.key("formHeight");
			writer.value(form.getSize().height);
			writer.key("formWidth");
			writer.value(form.getSize().width);
			writer.key("absoluteLayout");
			writer.value(!form.isResponsiveLayout());
			writer.endObject();
		}
		return writer;
	}

	public Form getForm(Object formElementValue, FlattenedSolution fs)
	{
		Object formId = formElementValue;
		if (formId instanceof JSONObject)
		{
			formId = ((JSONObject)formId).optString("svy_form");
		}
		Form form = null;
		if (formId instanceof Integer)
		{
			form = fs.getForm(((Integer)formId).intValue());
		}
		else if (formId instanceof String || formId instanceof UUID)
		{
			// try first by name or uuid (FS caches by both)
			form = fs.getForm(formId.toString());
			if (form == null)
			{
				form = (Form)fs.searchPersist(formId.toString());
			}
		}
		else if (formId instanceof JSForm)
		{
			return ((JSForm)formId).getSupportChild();
		}
		return form;
	}


	@Override
	public Object toSabloComponentValue(Object formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		Form form = getForm(formElementValue, dataAdapterList.getApplication().getFlattenedSolution());
		if (form != null)
		{
			if (pd.getConfig() instanceof ComponentTypeConfig && ((ComponentTypeConfig)pd.getConfig()).forFoundset != null)
			{
				return new FormComponentSabloValue(formElement, (JSONObject)formElementValue, pd, dataAdapterList, component, form);
			}
			else
			{
				FormComponentCache formComponentCache = FormElementHelper.INSTANCE.getFormComponentCache(formElement, pd, (JSONObject)formElementValue, form,
					dataAdapterList.getApplication().getFlattenedSolution());
				List<FormElement> elements = formComponentCache.getFormComponentElements();

				IWebFormUI formUI = component.findParent(IWebFormUI.class);
				for (FormElement element : elements)
				{
					WebFormComponent child = ComponentFactory.createComponent(dataAdapterList.getApplication(), dataAdapterList, element, component.getParent(),
						dataAdapterList.getForm().getForm());
					formUI.contributeComponentToElementsScope(element, element.getWebComponentSpec(), child);
				}
			}
		}
		return formElementValue;
	}

	@Override
	public IFormComponentRhinoConverter getFormComponentRhinoConverter(String property, final Object currentValue, final IApplication application,
		JSWebComponent webComponnent)
	{
		return new FormComponentValue(property, (JSONObject)currentValue, application, webComponnent);
	}

	public PropertyDescription getPropertyDescription(String property, JSONObject currentValue, FlattenedSolution fs)
	{
		return getFCPropertyDescription(property, currentValue, fs, new HashSet<String>(), new HashMap<String, PropertyDescription>());
	}

	/**
	 * @param property
	 * @param currentValue
	 * @param fs
	 * @param forms
	 * @return
	 */
	private PropertyDescription getFCPropertyDescription(String property, JSONObject currentValue, FlattenedSolution fs, HashSet<String> forms,
		Map<String, PropertyDescription> cached)
	{
		PropertyDescription pd;
		if (currentValue != null)
		{
			String formName = currentValue.optString(SVY_FORM);
			PropertyDescription propertyDescription = cached.get(formName);
			if (propertyDescription != null)
			{
				return propertyDescription;
			}
			PropertyDescriptionBuilder pdBuilder = new PropertyDescriptionBuilder().withName(property).withType(FormComponentPropertyType.INSTANCE);
			PropertyDescription formDesc = new PropertyDescriptionBuilder().withName(SVY_FORM).withType(StringPropertyType.INSTANCE).build();
			pdBuilder.withProperty(SVY_FORM, formDesc);
			Form form = getForm(formName, fs);
			if (form == null)
			{
				Debug.error("Can't find the form component " + formName + " for property " + property + " in solution " + fs.getName(), new RuntimeException());
				return pdBuilder.build();
			}
			String testFormName = form.getName();
			boolean nested = false;
			if (form != null && !forms.add(testFormName))
			{
				Debug.error("recursive reference found between (List)FormComponents: " + forms); //$NON-NLS-1$
				nested = true;
			}
			while (form != null)
			{
				List<IFormElement> formelements = form.getFlattenedObjects(null);
				for (IFormElement element : formelements)
				{
					if (element.getName() != null)
					{
						WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
							FormTemplateGenerator.getComponentTypeName(element));
						Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
						if (properties.size() > 0 && !nested)
						{
							PropertyDescriptionBuilder nestedFormComponentBuilder = new PropertyDescriptionBuilder().withName(element.getName());
							for (PropertyDescription nestedFormComponentPD : properties)
							{
								Object object = ((AbstractBase)element).getProperty(nestedFormComponentPD.getName());
								if (object instanceof JSONObject)
								{
									nestedFormComponentBuilder.withProperty(nestedFormComponentPD.getName(),
										getFCPropertyDescription(nestedFormComponentPD.getName(), (JSONObject)object, fs, forms, cached));
								}
							}
							pdBuilder.withProperty(element.getName(), nestedFormComponentBuilder.build());
						}
						else
						{
							pdBuilder.withProperty(element.getName(), spec);
						}
					}
				}
				form = form.getExtendsForm();
			}
			forms.remove(testFormName);
			pd = pdBuilder.build();
			cached.put(formName, pd);
		}
		else
		{
			PropertyDescriptionBuilder pdBuilder = new PropertyDescriptionBuilder().withName(property).withType(FormComponentPropertyType.INSTANCE);
			PropertyDescription formDesc = new PropertyDescriptionBuilder().withName(SVY_FORM).withType(StringPropertyType.INSTANCE).build();
			pdBuilder.withProperty(SVY_FORM, formDesc);
			pd = pdBuilder.build();
		}
		return pd;
	}

	private class FormComponentValue implements IFormComponentRhinoConverter
	{
		private final PropertyDescription pd;
		private final IApplication application;
		private final JSONObject currentValue;
		private final JSWebComponent webComponent;

		public FormComponentValue(String property, JSONObject currentValue, IApplication application, JSWebComponent webComponent)
		{
			this.webComponent = webComponent;
			this.currentValue = currentValue == null ? new JSONObject() : currentValue;
			this.application = application;
			this.pd = getPropertyDescription(property, currentValue, application.getFlattenedSolution());
		}

		public JSONObject setRhinoToDesignValue(String property, Object value)
		{
			if ("".equals(property))
			{
				// special case, this is just the form itself or it is a get of the complete jsonobject (current value)
				if (value == currentValue) return (JSONObject)value;
				Form form = getForm(value, application.getFlattenedSolution());
				if (form == null)
				{
					// form is reset just remove all the properties
					for (String key : Utils.iterate(currentValue.keys()))
					{
						currentValue.remove(key);
					}
				}
				else
				{
					currentValue.put(SVY_FORM, form.getUUID().toString());
				}
			}
			else
			{
				String[] split = property.split("\\.");
				Pair<PropertyDescription, JSONObject> context = getContext(split, true);
				PropertyDescription propertyPD = context.getLeft();
				Pair<PropertyDescription, String> lastDescription = getLastDescription(propertyPD, split[split.length - 1]);
				if (value == Context.getUndefinedValue())
				{
					context.getRight().remove(lastDescription.getRight());
				}
				else
				{
					PropertyDescription last = lastDescription.getLeft();
					if (last != null)
					{
						context.getRight().put(lastDescription.getRight(),
							JSNGWebComponent.fromRhinoToDesignValue(value, last, application, webComponent, last.getName()));
					}
					else
					{
						Debug.log("Setting a property " + property + "  a value " + value + " on " + webComponent + " that has no spec");
					}
				}
			}
			return currentValue;
		}

		@Override
		public Object getDesignToRhinoValue(String property)
		{
			if ("".equals(property))
			{
				Form form = getForm(currentValue, application.getFlattenedSolution());
				return form != null ? application.getScriptEngine().getSolutionModifier().getForm(form.getName()) : null;
			}
			else
			{
				String[] split = property.split("\\.");
				Pair<PropertyDescription, JSONObject> context = getContext(split, false);
				PropertyDescription propertyPD = context.getLeft();

				Pair<PropertyDescription, String> lastDescription = getLastDescription(propertyPD, split[split.length - 1]);

				PropertyDescription last = lastDescription.getLeft();
				if (last != null)
				{
					Object value = context.getRight().opt(lastDescription.getRight());
					return JSNGWebComponent.fromDesignToRhinoValue(value, last, application, webComponent, property);
				}
				else
				{
					Debug.log("getting a property " + property + "  a value " + context.getRight().opt(lastDescription.getRight()) + " on " + webComponent +
						" that has no spec");
				}
				return null;
			}
		}

		private Pair<PropertyDescription, JSONObject> getContext(String[] split, boolean create)
		{
			JSONObject obj = currentValue;
			PropertyDescription propertyPD = pd;
			for (int i = 0; i < split.length - 1; i++)
			{
				JSONObject tmp = null;
				if (split[i].endsWith("]"))
				{
					String propertyID = split[i].substring(0, split[i].indexOf("["));
					propertyPD = propertyPD != null ? propertyPD.getProperty(propertyID) : null;
					if (propertyPD != null && propertyPD.getType() instanceof ICustomType)
					{
						propertyPD = ((ICustomType)propertyPD.getType()).getCustomJSONTypeDefinition();
					}
					JSONArray array = obj.optJSONArray(propertyID);
					if (array == null)
					{
						if (!create) break;
						array = new JSONArray();
						obj.put(propertyID, array);
					}
					int index = Utils.getAsInteger(split[i].substring(split[i].indexOf("[") + 1, split[i].indexOf("]")));
					if (index >= 0)
					{
						for (int j = array.length(); j <= index; j++)
						{
							array.put(new JSONObject());
						}
						tmp = (JSONObject)array.get(index);
					}
				}
				else
				{
					propertyPD = propertyPD != null ? propertyPD.getProperty(split[i]) : null;
					tmp = obj.optJSONObject(split[i]);
					if (tmp == null)
					{
						if (!create) break;
						tmp = new JSONObject();
						obj.put(split[i], tmp);
					}
				}
				obj = tmp;
			}
			return new Pair<PropertyDescription, JSONObject>(propertyPD, obj);
		}

		private Pair<PropertyDescription, String> getLastDescription(PropertyDescription propertyPD, String property)
		{
			String propname = property;
			PropertyDescription last = null;
			if (propertyPD != null)
			{
				last = propertyPD.getProperty(propname);
				if (last == null)
				{
					last = propertyPD.getProperty(propname + "ID"); // legacy
					if (last != null)
					{
						propname = propname + "ID";
					}
				}
				if (last == null && propertyPD instanceof WebObjectSpecification)
				{
					// its a handler
					WebObjectFunctionDefinition handler = ((WebObjectSpecification)propertyPD).getHandler(propname);
					if (handler == null)
					{
						handler = ((WebObjectSpecification)propertyPD).getHandler(propname + "MethodID"); // legacy
						if (handler != null) propname = propname + "MethodID";
					}
					if (handler != null) last = handler.getAsPropertyDescription();
				}
			}
			return new Pair<PropertyDescription, String>(last, propname);
		}
	}

	@Override
	public boolean shouldAlwaysAllowIncommingJSON()
	{
		return true;
	}

	@Override
	public JSONWriter changesToJSON(JSONWriter writer, String key, Object sabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue instanceof FormComponentSabloValue)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			((FormComponentSabloValue)sabloValue).changesToJSON(writer, this);
		}
		return writer;
	}

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		if (pd.getConfig() instanceof ComponentTypeConfig && ((ComponentTypeConfig)pd.getConfig()).forFoundset != null)
		{
			// this is a list form component's (or similar) property - linked to foundset
			JSONUtils.addKeyIfPresent(w, keyToAddTo);
			w.value(TYPE_NAME);
			return true;
		}
		else
		{
			// this is a simple form component property (not in a repeated component) - that is part of a form directly - some things will happen directly through
			// form code (form already has the subcomponents in it)
			// this property will not even send any value client-side; we don't need to send anything here then either
			return false;
		}
	}

}
