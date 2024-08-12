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

package com.servoy.j2db.server.ngclient.property.types;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ChangeAwareList;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.WrappingContext;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.solutionmodel.JSNGWebComponent;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.component.RhinoMapOrArrayWrapper;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.InitialToJSONConverter;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * A JSON array type that is Servoy NG client aware as well.
 * So it adds all conversions from {@link NGConversions}.
 *
 * @author acostescu
 */
public class NGCustomJSONArrayType<SabloT, SabloWT> extends CustomJSONArrayType<SabloT, SabloWT> implements IDesignToFormElement<JSONArray, Object[], Object>,
	IFormElementToTemplateJSON<Object[], Object>, IFormElementToSabloComponent<Object[], Object>, ISabloComponentToRhino<Object>,
	IRhinoToSabloComponent<Object>, ISupportTemplateValue<Object[]>, ITemplateValueUpdaterType<ChangeAwareList<SabloT, SabloWT>>,
	IFindModeAwareType<Object[], Object>, IDataLinkedType<Object[], Object>, IRhinoDesignConverter, IDesignValueConverter<Object>, II18NPropertyType<Object>
{

	public static final String SKIP_NULL_ITEMS_AT_RUNTIME_CONFIG_KEY = "skipNullItemsAtRuntime";

	public NGCustomJSONArrayType(PropertyDescription definition)
	{
		super(definition);
	}

	@Override
	public Object[] toFormElementValue(JSONArray designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		if (designValue != null)
		{
			Object[] formElementValues = new Object[designValue.length()];
			for (int i = designValue.length() - 1; i >= 0; i--)
			{
				try
				{
					propertyPath.add(i);
					formElementValues[i] = NGConversions.INSTANCE.convertDesignToFormElementValue(designValue.opt(i), getCustomJSONTypeDefinition(),
						flattenedSolution, formElement, propertyPath);
				}
				finally
				{
					propertyPath.backOneLevel();
				}
			}
			return formElementValues;
		}
		return null;
	}

	@Override
	public JSONWriter initialToJSON(JSONWriter writer, String key, ChangeAwareList<SabloT, SabloWT> changeAwareList, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		return toJSON(writer, key, changeAwareList, true, InitialToJSONConverter.INSTANCE, dataConverterContext);
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object[] formElementValue, PropertyDescription pd,
		FormElementContext formElementContext) throws JSONException
	{
		// only send template in designer
		if (formElementValue == null || formElementContext == null || formElementContext.getFormElement() == null ||
			!formElementContext.getFormElement().isInDesigner()) return writer;

		JSONUtils.addKeyIfPresent(writer, key);

		writer.object().key(CONTENT_VERSION).value(1).key(VALUE).array();
		for (Object element : formElementValue)
		{
			NGConversions.INSTANCE.convertFormElementToTemplateJSONValue(writer, null, element, getCustomJSONTypeDefinition(), formElementContext);
		}
		writer.endArray().endObject();

		return writer;
	}

	@Override
	public Object toSabloComponentValue(Object[] formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dal)
	{
		if (formElementValue != null)
		{
			List<SabloT> list = new ArrayList<>(formElementValue.length);
			for (Object element : formElementValue)
			{
				Object v = NGConversions.INSTANCE.convertFormElementToSabloComponentValue(element, getCustomJSONTypeDefinition(), formElement, component, dal);
				if (v != null || !shouldSkipNullItemsInFormElementToSabloValueConversion(pd)) list.add((SabloT)v);
			}
			return list;
		}
		return null;
	}

	private boolean shouldSkipNullItemsInFormElementToSabloValueConversion(PropertyDescription pd)
	{
		JSONObject config = ((JSONObject)pd.getConfig());
		return config != null ? config.optBoolean(SKIP_NULL_ITEMS_AT_RUNTIME_CONFIG_KEY, false) : false;
	}

	@Override
	public Object toSabloComponentValue(final Object rhinoValue, Object previousComponentValue, PropertyDescription pd,
		final IWebObjectContext componentOrService)
	{
		if (rhinoValue == null || RhinoConversion.isUndefinedOrNotFound(rhinoValue)) return null;

		final ChangeAwareList<SabloT, SabloWT> previousSpecialArray = (ChangeAwareList<SabloT, SabloWT>)previousComponentValue;
		if (rhinoValue instanceof RhinoMapOrArrayWrapper)
		{
			return ((RhinoMapOrArrayWrapper)rhinoValue).getWrappedValue();
		}
		else
		{
			// if it's some kind of array
			List<SabloT> rhinoArrayCopy = null; // we always make a new copy to simplify code; so previous Rhino reference in js code should no longer be used after this conversion
			PropertyDescription elementPD = getCustomJSONTypeDefinition();

			if (rhinoValue instanceof NativeArray)
			{
				rhinoArrayCopy = new ArrayList<SabloT>();
				NativeArray nativeArray = (NativeArray)rhinoValue;
				for (Object element : nativeArray)
				{
					rhinoArrayCopy.add(
						(SabloT)NGConversions.INSTANCE.convertRhinoToSabloComponentValue(element, null, elementPD, componentOrService));
				}
			}
			else if (rhinoValue instanceof NativeJavaArray)
			{
				rhinoArrayCopy = new ArrayList<SabloT>();
				NativeJavaArray nativeJavaArray = (NativeJavaArray)rhinoValue;
				int length = ((Integer)nativeJavaArray.get("length", nativeJavaArray)).intValue();
				for (int i = 0; i < length; i++)
				{
					rhinoArrayCopy.add((SabloT)NGConversions.INSTANCE.convertRhinoToSabloComponentValue(nativeJavaArray.get(i, nativeJavaArray), null,
						elementPD, componentOrService));
				}
			}
			else if (rhinoValue.getClass().isArray()) // unwrapped already by caller from a NativeJavaArray
			{
				rhinoArrayCopy = new ArrayList<SabloT>();
				int length = Array.getLength(rhinoValue);
				for (int i = 0; i < length; i++)
				{
					rhinoArrayCopy.add((SabloT)NGConversions.INSTANCE.convertRhinoToSabloComponentValue(Array.get(rhinoValue, i), null,
						elementPD, componentOrService));
				}
			}
			else Debug.warn("Cannot convert value assigned from solution scripting into array property type; class: " + rhinoValue.getClass() + //$NON-NLS-1$
				", new value = " + rhinoValue + "; property = " + //$NON-NLS-1$ //$NON-NLS-2$
				pd.getName() + "; component name = " + componentOrService.getUnderlyingWebObject().getName()); //$NON-NLS-1$

			if (rhinoArrayCopy != null)
			{
				return wrap(rhinoArrayCopy, previousSpecialArray, pd, new WrappingContext(componentOrService.getUnderlyingWebObject(), pd.getName()));
			}
		}
		return previousComponentValue; // or should we return null or throw exception here? incompatible thing was assigned
	}

	@Override
	public boolean isValueAvailableInRhino(Object webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Object webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService, Scriptable startScriptable)
	{
		return webComponentValue == null ? null : new RhinoMapOrArrayWrapper(webComponentValue, componentOrService, pd, startScriptable);
	}

	@Override
	public boolean valueInTemplate(Object[] values, PropertyDescription pd, FormElementContext formElementContext)
	{
		if (values != null && values.length > 0 && formElementContext != null && formElementContext.getFormElement() != null &&
			formElementContext.getFormElement().isInDesigner())
		{
			PropertyDescription desc = getCustomJSONTypeDefinition();

			if (desc.getType() instanceof ISupportTemplateValue)
			{
				ISupportTemplateValue<Object> type = (ISupportTemplateValue<Object>)desc.getType();
				for (Object object : values)
				{
					object = (object == IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER) ? null : object;
					if (!type.valueInTemplate(object, desc, formElementContext))
					{
						return false;
					}
				}
			}
			return true;
		}
		// always call tojson to send pushtoserver for real client
		return false;
	}

	@Override
	public boolean isFindModeAware(Object[] formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement)
	{
		if (formElementValue == null) return false;

		boolean isFindModeAware = false;

		PropertyDescription entryPD = getCustomJSONTypeDefinition();
		for (Object value : formElementValue)
		{
			// as array element property descriptions can describe multiple property values in the same bean - we won't cache those
			if (entryPD.getType() instanceof IFindModeAwareType)
			{
				if (((IFindModeAwareType)entryPD.getType()).isFindModeAware(ServoyJSONObject.jsonNullToNull(value), entryPD, flattenedSolution, formElement))
				{
					isFindModeAware = true;
					break;
				}
			}
		}

		return isFindModeAware;
	}

	@Override
	public TargetDataLinks getDataLinks(Object[] formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement)
	{
		if (formElementValue == null) return TargetDataLinks.NOT_LINKED_TO_DATA;

		ArrayList<String> dps = new ArrayList<>();
		boolean recordLinked = false;

		PropertyDescription entryPD = getCustomJSONTypeDefinition();
		for (Object value : formElementValue)
		{
			if (entryPD.getType() instanceof IDataLinkedType)
			{
				TargetDataLinks entryDPs = ((IDataLinkedType)entryPD.getType()).getDataLinks(FormElement.convertToNull(ServoyJSONObject.jsonNullToNull(value)),
					entryPD, flattenedSolution,
					formElement);
				if (entryDPs != null && entryDPs != TargetDataLinks.NOT_LINKED_TO_DATA)
				{
					dps.addAll(Arrays.asList(entryDPs.dataProviderIDs));
					recordLinked |= entryDPs.recordLinked;
					// TODO do also add relations from TargetDataLinks here!! Maybe as part of SVY-18622?
					// or maybe replace this method with a "hasRecordDataLinks(...)" if that is the only info that is needed by the callers
				}
			}
		}

		if (dps.size() == 0 && recordLinked == false) return TargetDataLinks.NOT_LINKED_TO_DATA;
		else return new TargetDataLinks(dps.toArray(new String[dps.size()]), recordLinked);
	}

	@Override
	public Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		Object[] array = (Object[])value;
		JSONArray values = new JSONArray();
		PropertyDescription desc = getCustomJSONTypeDefinition();
		for (Object element : array)
		{
			values.put(JSNGWebComponent.fromRhinoToDesignValue(element, desc, application, webComponent, desc.getName()));
		}
		return values;
	}

	@Override
	public Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		PropertyDescription desc = getCustomJSONTypeDefinition();
		if (value instanceof Object[])
		{
			Object[] obj = (Object[])value;
			Scriptable scope = ScriptableObject.getTopLevelScope(application.getScriptEngine().getSolutionScope());
			Scriptable result = DefaultScope.newArray(scope, 0);
			for (int i = 0; i < obj.length; i++)
			{
				result.put(i, result, JSNGWebComponent.fromDesignToRhinoValue(obj[i], desc, application, webComponent, Integer.toString(i)));
			}
			return result;
		}
		if (value instanceof JSONArray)
		{
			JSONArray arr = (JSONArray)value;
			Scriptable scope = ScriptableObject.getTopLevelScope(application.getScriptEngine().getSolutionScope());
			Scriptable result = DefaultScope.newArray(scope, 0);
			for (int i = 0; i < arr.length(); i++)
			{
				result.put(i, result, JSNGWebComponent.fromDesignToRhinoValue(arr.get(i), desc, application, webComponent, Integer.toString(i)));
			}
			return result;
		}
		return value;
	}

	@Override
	public Object fromDesignValue(Object designValue, PropertyDescription propertyDescription, IPersist persit)
	{
		if (designValue instanceof JSONArray)
		{
			PropertyDescription elementPD = getCustomJSONTypeDefinition();
			JSONArray arr = (JSONArray)designValue;
			Object[] java_arr = new Object[arr.length()];
			for (int i = 0; i < arr.length(); i++)
			{
				java_arr[i] = WebObjectImpl.convertToJavaType(elementPD, arr.opt(i), persit);
			}
			return java_arr;
		}
		return designValue;
	}

	@Override
	public Object toDesignValue(Object value, PropertyDescription pd)
	{
		if (value instanceof Object[])
		{
			PropertyDescription elementPD = getCustomJSONTypeDefinition();
			Object[] arr = (Object[])value;
			JSONArray jsonArray = new ServoyJSONArray();
			for (int i = 0; i < arr.length; i++)
			{
				jsonArray.put(i, WebObjectImpl.convertFromJavaType(elementPD, arr[i]));
			}
			return jsonArray;
		}
		return value;
	}

	@Override
	public Object resetI18nValue(Object property, PropertyDescription pd, WebFormComponent component)
	{
		PropertyDescription arrayElementPD = ((CustomJSONArrayType< ? , ? >)pd.getType()).getCustomJSONTypeDefinition();
		if (arrayElementPD.getType() instanceof II18NPropertyType)
		{
			if (property instanceof List< ? > && ((List< ? >)property).size() > 0)
			{
				List<Object> list = (List<Object>)property;
				for (int i = 0; i < list.size(); i++)
				{
					Object o = list.get(i);
					list.set(i, ((II18NPropertyType<Object>)arrayElementPD.getType()).resetI18nValue(o, arrayElementPD, component));
				}
			}
		}
		return property;
	}
}
