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
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.specification.property.types.StringPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.PositionComparator;
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
	IFormElementToTemplateJSON<Object, Object>, IFormElementToSabloComponent<Object, Object>, IFormComponentType
{
	public static final String SVY_FORM = "svy_form";

	public static final FormComponentPropertyType INSTANCE = new FormComponentPropertyType();
	public static final String TYPE_NAME = "formcomponent";

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
		return json;
	}

	@Override
	public Object fromJSON(Object newJSONValue, Object previousSabloValue, PropertyDescription propertyDescription, IBrowserConverterContext context,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(Object webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Object webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		Scriptable newObject = Context.getCurrentContext().newObject(startScriptable);
		// TODO return here a NativeScriptable object that understand the full hiearchy?
		WebFormComponent webFormComponent = (WebFormComponent)componentOrService;
		IWebFormUI formUI = webFormComponent.findParent(IWebFormUI.class);
		FlattenedSolution fs = webFormComponent.getDataConverterContext().getSolution();
		Form form = getForm(webComponentValue, fs);
		FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(webFormComponent.getFormElement(), pd, (JSONObject)webComponentValue, form,
			fs);
		for (FormElement fe : cache.getFormComponentElements())
		{
			String name = fe.getPersistIfAvailable() instanceof AbstractBase
				? ((AbstractBase)fe.getPersistIfAvailable()).getRuntimeProperty(FormElementHelper.FORM_COMPONENT_TEMPLATE_NAME) : null;
			if (name != null && !name.startsWith(FormElement.SVY_NAME_PREFIX))
			{
				RuntimeWebComponent webComponent = formUI.getRuntimeWebComponent(fe.getRawName());
				if (webComponent != null)
				{
					newObject.put(name, newObject, webComponent);
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
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		FlattenedSolution fs = formElementContext.getFlattenedSolution();
		Form form = getForm(formElementValue, fs);
		if (form != null)
		{
			// we output here a uuid that is a uuid that must be used to get the compiled template from the $formcomponentCache
			writer.key(key);
			String uuid = FormElementHelper.INSTANCE.getFormComponentCache(formElementContext.getFormElement(), pd, (JSONObject)formElementValue, form,
				fs).getCacheUUID();
			writer.value(uuid);
		}
		return writer;
	}

	/**
	 * @param formElementValue
	 * @param fs
	 * @return
	 */
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

			UUID uuid = Utils.getAsUUID(formId, false);
			if (uuid != null) form = (Form)fs.searchPersist(uuid);
			else form = fs.getForm((String)formId);
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
			IWebFormUI formUI = component.findParent(IWebFormUI.class);
			List<FormElement> elements = FormElementHelper.INSTANCE.getFormComponentCache(formElement, pd, (JSONObject)formElementValue, form,
				dataAdapterList.getApplication().getFlattenedSolution()).getFormComponentElements();
			for (FormElement element : elements)
			{
				WebFormComponent child = ComponentFactory.createComponent(dataAdapterList.getApplication(), dataAdapterList, element, component.getParent(),
					dataAdapterList.getForm().getForm());
				formUI.contributeComponentToElementsScope(element, element.getWebComponentSpec(), child);
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
		PropertyDescription pd = new PropertyDescription(property, FormComponentPropertyType.INSTANCE);
		PropertyDescription formDesc = new PropertyDescription(SVY_FORM, StringPropertyType.INSTANCE);
		pd.putProperty(SVY_FORM, formDesc);
		String formName = currentValue.optString(SVY_FORM);
		Form form = getForm(formName, fs);
		if (form != null)
		{
			List<IFormElement> formelements = form.getFlattenedObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			for (IFormElement element : formelements)
			{
				if (element.getName() != null)
				{
					WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(
						FormTemplateGenerator.getComponentTypeName(element));
					Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
					if (properties.size() > 0)
					{
						PropertyDescription nestedFormComponent = new PropertyDescription(element.getName(), null);
						pd.putProperty(element.getName(), nestedFormComponent);
						for (PropertyDescription nestedFormComponentPD : properties)
						{
							Object object = ((AbstractBase)element).getProperty(nestedFormComponentPD.getName());
							if (object instanceof JSONObject)
							{
								nestedFormComponent.putProperty(nestedFormComponentPD.getName(),
									getPropertyDescription(nestedFormComponentPD.getName(), (JSONObject)object, fs));
							}
						}
					}
					else
					{
						pd.putProperty(element.getName(), spec);
					}
				}
			}
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
				propertyPD = propertyPD != null ? propertyPD.getProperty(split[i]) : null;
				JSONObject tmp = obj.optJSONObject(split[i]);
				if (tmp == null)
				{
					if (!create) break;
					tmp = new JSONObject();
					obj.put(split[i], tmp);
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
}
