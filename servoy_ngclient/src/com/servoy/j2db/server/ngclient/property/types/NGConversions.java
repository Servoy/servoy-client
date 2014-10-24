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

import org.json.JSONException;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.DesignConversion;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.util.Debug;

/**
 * This class does the ng specific conversions for property types.<br>
 * It's all in in this class just for being able to quickly see all these conversions in one place and get to the code that uses each conversion from this starting point.<br><br>
 *
 * All this should be in sync with <a href="https://wiki.servoy.com/display/randd/NGSabloConversions">https://wiki.servoy.com/display/randd/NGSabloConversions</a>
 * @author acostescu
 */
public class NGConversions
{

	public static final NGConversions INSTANCE = new NGConversions();

	/**
	 * Conversion 1 as specified in https://wiki.servoy.com/pages/viewpage.action?pageId=8716797.
	 */
	public static interface IDesignToFormElement<D, F, T> extends IPropertyType<T>
	{

		/**
		 * Value to be used when there is no value set from design for a property, there is no default value in spec file either but
		 * there is a default value specified by the type (but which can't be used directly in FormElement as it's a Sablo (runtime) value).
		 *
		 * The idea behind this marker is that when generating the form template, these Sablo (runtime) values can still be converted to JSON using Sablo
		 * converters so that they do get cached. And this marker can be used at that toJSON stage.
		 */
		public final static Object TYPE_DEFAULT_VALUE_MARKER = new Object()
		{
			@Override
			public String toString()
			{
				return "TYPE_DEFAULT_VALUE_MARKER";
			};
		};

		/**
		 * Converts a design JSON value / primitive to a Java value representing that property in FormElement based on spec. type.<br>
		 * If the spec type doesn't specify a conversion for this stage, it will simply return the give value unchanged.
		 *
		 * @param designValue can be JSONObject, JSONArray, or primitive types
		 * @param pd that value in spec file for this property type.
		 * @param flattenedSolution current flattened solution.
		 * @param formElement the form element this property value will be set on.
		 * @return the corresponding FormElement value based on bean spec.
		 */
		F toFormElementValue(D designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement, PropertyPath propertyPath);

	}

	/**
	 * Conversion 2 as specified in https://wiki.servoy.com/pages/viewpage.action?pageId=8716797.
	 */
	public static interface IFormElementToTemplateJSON<F, T> extends IPropertyType<T>
	{

		/**
		 * Converts from a FormElement value to template JSON form (that can be sent to the browser and cached) and writes to "writer".
		 *
		 * @param writer the JSON writer to write to
		 * @param key if this value will be part of a JSON object, key is non-null and you MUST do writer.key(...) before adding the converted value. This
		 * is useful for cases when you don't want the value written at all in resulting JSON in which case you don't write neither key or value. If
		 * key is null and you want to write the converted value write only the converted value to the writer, ignore the key.
		 * @param formElementValue the value to be converted and written.
		 * @param pd the property description for this property.
		 * @param browserConversionMarkers client conversion markers that can be set and if set will be used client side to interpret the data properly.
		 * @return the JSON writer for easily continuing the write process in the caller.
		 */
		JSONWriter toTemplateJSONValue(JSONWriter writer, String key, F formElementValue, PropertyDescription pd, DataConversion browserConversionMarkers,
			IServoyDataConverterContext servoyDataConverterContext) throws JSONException;

	}

	/**
	 * Conversion 3 as specified in https://wiki.servoy.com/pages/viewpage.action?pageId=8716797.
	 */
	public static interface IFormElementToSabloComponent<F, T> extends IPropertyType<T>
	{

		/**
		 * Converts from a FormElement value to a Sablo value to be set in the web component.
		 * @param formElementValue the FormElement value for this property
		 * @param pd the spec description of the property
		 * @param formElement the form element
		 * @param component the component to which the returned value will be assigned as a property
		 * @return the converted value, ready to be put in the web component property
		 */
		T toSabloComponentValue(F formElementValue, PropertyDescription pd, FormElement formElement, WebFormComponent component);

	}

	/**
	 * Conversion 4.1 as specified in https://wiki.servoy.com/pages/viewpage.action?pageId=8716797.
	 */
	public static interface ISabloComponentToRhino<T> extends IPropertyType<T>
	{

		/**
		 * This call is used for JS property listing purposes. (has/list ids...)
		 * It could be equivalent to "toRhinoValue(...) != Scriptable.NOT_FOUND", but it's meant to be separate
		 * to prevent a full conversion when it might not be needed.
		 *
		 * @param webComponentValue the Sablo value
		 * @param pd the spec description of the property/type
		 * @param componentOrService the component to which the given value belongs to
		 * @return true if this property value is available in Javascript and false otherwise
		 */
		boolean isValueAvailableInRhino(T webComponentValue, PropertyDescription pd, BaseWebObject componentOrService);

		/**
		 * Converts from a Sablo value to a Rhino usable representation of the value.
		 * It does not receive a previous Rhino value as that value is not kept. If needed the type could keep that cached in the sablo component property value.
		 *
		 * @param webComponentValue the Sablo value
		 * @param pd the spec description of the property/type
		 * @param componentOrService the component or service to which the given value belongs to
		 * @param startScriptable the Scriptable in Rhino that this value is requested for
		 * @return the converted value, ready to be used in Rhino; can return Scriptable.NOT_FOUND if the property is not available in scripting.
		 */
		Object toRhinoValue(T webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable);

	}

	/**
	 * Conversion 4.2 as specified in https://wiki.servoy.com/pages/viewpage.action?pageId=8716797.
	 */
	public static interface IRhinoToSabloComponent<T> extends IPropertyType<T>
	{

		/**
		 * Converts from a Rhino to a Sablo value.
		 * @param previousComponentValue the previous (current) value available in in the component for the property if any.
		 * @param rhinoValue the Rhino value to be converted
		 * @param pd the spec description of the property/type
		 * @param componentOrService the component or service to which the given value belongs to
		 * @return the converted value, ready to be set in Sablo component/service
		 */
		T toSabloComponentValue(Object rhinoValue, T previousComponentValue, PropertyDescription pd, BaseWebObject componentOrService);

	}

	/**
	 * Conversion 1 as specified in https://wiki.servoy.com/pages/viewpage.action?pageId=8716797.
	 */
	public Object convertDesignToFormElementValue(Object designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement,
		PropertyPath propertyPath)
	{
		IPropertyType< ? > type = pd.getType();
		if (type instanceof IDesignToFormElement)
		{
			return ((IDesignToFormElement)type).toFormElementValue(designValue, pd, flattenedSolution, formElement, propertyPath);
		}
		return designValue;
	}

	/**
	 * Conversion 2 as specified in https://wiki.servoy.com/pages/viewpage.action?pageId=8716797.
	 */
	public JSONWriter convertFormElementToTemplateJSONValue(JSONWriter writer, String key, Object value, PropertyDescription valueType,
		DataConversion browserConversionMarkers, IServoyDataConverterContext servoyDataConverterContext) throws IllegalArgumentException, JSONException
	{
		return new FormElementToJSON(servoyDataConverterContext).toJSONValue(writer, key, value, valueType, browserConversionMarkers);
	}

	/**
	 * To be used for "Conversion 2".
	 */
	public static class FormElementToJSON implements IToJSONConverter
	{
		private final IServoyDataConverterContext servoyDataConverterContext;

		public FormElementToJSON(IServoyDataConverterContext servoyDataConverterContext)
		{
			this.servoyDataConverterContext = servoyDataConverterContext;
		}

		/**
		 * Converts from FormElement values to template JSON values (sent to browser initially and cached).
		 *
		 * @param key if this value will be part of a JSON object, key is non-null and you MUST do writer.key(...) before adding the converted value. This
		 * is useful for cases when you don't want the value written at all in resulting JSON in which case you don't write neither key or value. If
		 * key is null and you want to write the converted value write only the converted value to the writer, ignore the key.
		 * @see IToJSONConverter#toJSONValue(JSONWriter, Object, PropertyDescription, DataConversion, ConversionLocation)
		 */
		@Override
		public JSONWriter toJSONValue(JSONWriter writer, String key, Object value, PropertyDescription valueType, DataConversion browserConversionMarkers)
			throws JSONException, IllegalArgumentException
		{
			IPropertyType< ? > type = (valueType != null ? valueType.getType() : null);
			if (value != IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER)
			{
				if (type instanceof IFormElementToTemplateJSON)
				{
					writer = ((IFormElementToTemplateJSON)type).toTemplateJSONValue(writer, key, value, valueType, browserConversionMarkers,
						servoyDataConverterContext);
				}
				else if (type instanceof ISupportTemplateValue && !((ISupportTemplateValue)type).valueInTemplate(value))
				{
					return writer;
				}
				else if (!JSONUtils.defaultToJSONValue(this, writer, key, value, valueType, browserConversionMarkers))
				{
					JSONUtils.addKeyIfPresent(writer, key);
					writer.value(value);
				}
			}
			else if (type != null)
			{
				// use conversion 5.1 to convert from default sablo type value to browser JSON in this case
				writer = JSONUtils.FullValueToJSONConverter.INSTANCE.toJSONValue(writer, key, type.defaultValue(), valueType, browserConversionMarkers);
			}
			return writer;
		}
	}

	public static class InitialToJSONConverter extends FullValueToJSONConverter
	{

		public static final InitialToJSONConverter INSTANCE = new InitialToJSONConverter();

		@Override
		public JSONWriter toJSONValue(JSONWriter writer, String key, Object value, PropertyDescription valueType, DataConversion browserConversionMarkers)
			throws JSONException, IllegalArgumentException
		{
			boolean written = false;
			if (value != null && valueType != null)
			{
				IPropertyType< ? > type = valueType.getType();
				if (type instanceof ITemplateValueUpdaterType)
				{
					// good, we now know that this type puts values in template as well and now it only needs to update them to match runtime content
					try
					{
						return ((ITemplateValueUpdaterType)type).initialToJSON(writer, key, value, browserConversionMarkers);
					}
					catch (Exception ex)
					{
						Debug.error("Error while writing template diff changes for value: " + value + " to type: " + type, ex);
						return writer;
					}
				}
			}

			// for most values that don't support template value + updates use full value to JSON
			if (!written) super.toJSONValue(writer, key, value, valueType, browserConversionMarkers);

			return writer;
		}
	}

	// FormElement value of property is only one across all client sessions, so properties that need to have
	// special behavior in cached template form only or those that have a modifyable object in form element and want to make copies of it for
	// each client will implement this conversion
	/**
	 * Conversion 3 as specified in https://wiki.servoy.com/pages/viewpage.action?pageId=8716797.
	 */
	public Object convertFormElementToSabloComponentValue(Object formElementValue, PropertyDescription pd, FormElement formElement, WebFormComponent component)
	{
		IPropertyType< ? > type = pd.getType();
		if (formElementValue != IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER)
		{
			if (type instanceof IFormElementToSabloComponent)
			{
				return ((IFormElementToSabloComponent)type).toSabloComponentValue(formElementValue, pd, formElement, component);
			}
			return formElementValue;
		}
		else
		{
			return type.defaultValue();
		}
	}

	/**
	 * Conversion 4.1 as specified in https://wiki.servoy.com/pages/viewpage.action?pageId=8716797.
	 * @param startScriptable the Scriptable in Rhino that this value is requested for
	 */
	public Object convertSabloComponentToRhinoValue(Object webComponentValue, PropertyDescription pd, BaseWebObject componentOrService,
		Scriptable startScriptable)
	{
		if (pd == null) return webComponentValue;
		if (WebFormComponent.isDesignOnlyProperty(pd)) return Scriptable.NOT_FOUND;

		Object rhinoVal;
		IPropertyType< ? > type = pd.getType();
		if (type instanceof ISabloComponentToRhino< ? >)
		{
			rhinoVal = ((ISabloComponentToRhino)type).toRhinoValue(webComponentValue, pd, componentOrService, startScriptable);
		}
		else
		{
			// TODO this should slowly dissapear as more things are moved to type code

//			String name = pd.getName();

			// hmm this is no longer ok now, the property name could be relative to a nested property, not necessarily to a component
			// we should really implement all types properly as soon as possible
//			Object value = component.getConvertedPropertyWithDefault(name, type instanceof DataproviderPropertyType, true);

			// TODO if we want to support random objects/array we should create a "AnyJSONValueType" that based on value type
			// reuses stuff from CustomJSONArrayType, CustomJSONObject type instead of the commented out code below
//			if (value instanceof Map || value instanceof Object[] || value instanceof List< ? >)
//			{
//				return new RhinoMapOrArrayWrapper(component, name, type instanceof DataproviderPropertyType,
//					component.getFormElement().getWebComponentSpec().getProperty(name), component.getDataConverterContext());
//			}

			// hmm getConvertedPropertyWithDefault() above might (or might not) also have done the design conversion already through DataAdapterList call; so it might get called twice DesignConversion.toStringObject which is wrong
			return DesignConversion.toStringObject(webComponentValue, pd.getType());
		}

		return rhinoVal;
	}

	/**
	 * Conversion 4.2 as specified in https://wiki.servoy.com/pages/viewpage.action?pageId=8716797.
	 */
	public <T> T convertRhinoToSabloComponentValue(Object rhinoValue, T previousComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		T sabloVal;
		IPropertyType< ? > type = pd.getType();
		if (type instanceof IRhinoToSabloComponent< ? >)
		{
			sabloVal = ((IRhinoToSabloComponent<T>)type).toSabloComponentValue(rhinoValue, previousComponentValue, pd, componentOrService);
		}
		else
		{
			// TODO this should slowly dissapear as more things are moved to type code
			IServoyDataConverterContext dataConverterContext = null;
			if (componentOrService instanceof IContextProvider) dataConverterContext = ((IContextProvider)componentOrService).getDataConverterContext();
			sabloVal = (T)RhinoConversion.convert(rhinoValue, previousComponentValue, pd, dataConverterContext);
		}
		return sabloVal;
	}
}
