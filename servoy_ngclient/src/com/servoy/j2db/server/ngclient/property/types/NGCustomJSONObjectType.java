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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.CustomObjectContext;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ChangeAwareMap;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IWrappingContext;
import org.sablo.specification.property.WrappingContext;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.solutionmodel.JSNGWebComponent;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementExtension;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.component.RhinoMapOrArrayWrapper;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignDefaultToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.InitialToJSONConverter;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * A custom JSON object type that is Servoy NG client aware as well.
 * So it adds all conversions from {@link NGConversions}.
 *
 * @author acostescu
 */
//TODO these SabloT, SabloWT and FormElementT are improper - as for object type they can represent multiple types (a different set for each child key), but they help to avoid some bugs at compile-time
public class NGCustomJSONObjectType<SabloT, SabloWT, FormElementT> extends CustomJSONObjectType<SabloT, SabloWT>
	implements IDesignToFormElement<JSONObject, Map<String, FormElementT>, Map<String, SabloT>>,
	IFormElementToTemplateJSON<Map<String, FormElementT>, Map<String, SabloT>>, IFormElementToSabloComponent<Map<String, FormElementT>, Map<String, SabloT>>,
	ISabloComponentToRhino<Map<String, SabloT>>, IRhinoToSabloComponent<Map<String, SabloT>>, ISupportTemplateValue<Map<String, FormElementT>>,
	ITemplateValueUpdaterType<ChangeAwareMap<SabloT, SabloWT>>, IFindModeAwareType<Map<String, FormElementT>, Map<String, SabloT>>,
	IDataLinkedType<Map<String, FormElementT>, Map<String, SabloT>>, IRhinoDesignConverter, IDesignValueConverter<Object>,
	II18NPropertyType<Map<String, SabloT>>
{

	public static final String SET_SABLO_VALUE_TO_NULL_IF_ANY_OF_THESE_KEYS_ARE_NULL = "setToNullAtRuntimeIfAnyOfTheseKeysAreNull";

	public NGCustomJSONObjectType(String typeName, PropertyDescription definition)
	{
		super(typeName, definition);
	}

	@Override
	public Map<String, FormElementT> toFormElementValue(JSONObject designValue, PropertyDescription mainProperty, FlattenedSolution flattenedSolution,
		INGFormElement formElement, PropertyPath propertyPath)
	{
		if (designValue != null)
		{
			Map<String, FormElementT> formElementValues = new HashMap<>(designValue.length());
			Iterator<String> keys = designValue.keys();
			while (keys.hasNext())
			{
				String key = keys.next();
				try
				{
					propertyPath.add(key);
					PropertyDescription property = getCustomJSONTypeDefinition().getProperty(key);
					if (property != null) formElementValues.put(key, (FormElementT)NGConversions.INSTANCE.convertDesignToFormElementValue(designValue.opt(key),
						property, flattenedSolution, formElement, propertyPath));
				}
				finally
				{
					propertyPath.backOneLevel();
				}
			}
			for (PropertyDescription pd : getCustomJSONTypeDefinition().getProperties().values())
			{
				if (!formElementValues.containsKey(pd.getName()))
				{
					if (pd.hasDefault())
					{
						propertyPath.add(pd.getName());
						formElementValues.put(pd.getName(), (FormElementT)NGConversions.INSTANCE.convertDesignToFormElementValue(pd.getDefaultValue(), pd,
							flattenedSolution, formElement, propertyPath));
						propertyPath.backOneLevel();
					}
					else if (pd.getType() instanceof IDesignDefaultToFormElement< ? , ? , ? >)
					{
						propertyPath.add(pd.getName());
						formElementValues.put(pd.getName(), (FormElementT)((IDesignDefaultToFormElement< ? , ? , ? >)pd.getType()).toDefaultFormElementValue(pd,
							flattenedSolution, formElement, propertyPath));
						propertyPath.backOneLevel();
					}
					else if (pd.getType().defaultValue(pd) != null || pd.getType() instanceof IFormElementDefaultValueToSabloComponent)
					{
						propertyPath.add(pd.getName());
						formElementValues.put(pd.getName(), (FormElementT)NGConversions.IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER);
						propertyPath.backOneLevel();
					}
				}
			}
			return formElementValues;
		}
		return null;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Map<String, FormElementT> formElementValue, PropertyDescription pd,
		FormElementContext formElementContext) throws JSONException
	{
		// only send template in designer
		if (formElementValue == null || formElementContext == null || formElementContext.getFormElement() == null ||
			!formElementContext.getFormElement().isInDesigner()) return writer;

		JSONUtils.addKeyIfPresent(writer, key);

		writer.object().key(CONTENT_VERSION).value(1).key(VALUE).object();
		for (Entry<String, FormElementT> e : formElementValue.entrySet())
		{
			NGConversions.INSTANCE.convertFormElementToTemplateJSONValue(writer, e.getKey(), e.getValue(),
				getCustomJSONTypeDefinition().getProperty(e.getKey()), formElementContext);
		}
		writer.endObject().endObject();
		return writer;
	}

	@Override
	public JSONWriter initialToJSON(JSONWriter writer, String key, ChangeAwareMap<SabloT, SabloWT> changeAwareMap, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		return toJSON(writer, key, changeAwareMap, true, InitialToJSONConverter.INSTANCE, dataConverterContext);
	}

	@Override
	public Map<String, SabloT> toSabloComponentValue(Map<String, FormElementT> formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dal)
	{
		if (formElementValue != null)
		{
			Map<String, SabloT> map = new HashMap<>(formElementValue.size());
			Set<String> nullifyWholeValueIfTheValueForAnyOfTheseIsNull = null;
			for (Entry<String, FormElementT> e : formElementValue.entrySet())
			{
				PropertyDescription subpropPD = getCustomJSONTypeDefinition().getProperty(e.getKey());

				if (subpropPD == null) log.error(
					"subprop. PD is null... subProp: '" + e.getKey() + "', value: " + e.getValue() + ", customType: " + getCustomJSONTypeDefinition()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				Object v = NGConversions.INSTANCE.convertFormElementToSabloComponentValue(e.getValue(), subpropPD,
					new FormElementExtension(formElement, formElementValue, getCustomJSONTypeDefinition()), component, dal);
				if (v != null) map.put(e.getKey(), (SabloT)v);
				else
				{
					if (nullifyWholeValueIfTheValueForAnyOfTheseIsNull == null)
						nullifyWholeValueIfTheValueForAnyOfTheseIsNull = shouldSetFullValueToNullInFormElementToSabloValueIfTheseKeysHaveNullValues(pd);
					if (nullifyWholeValueIfTheValueForAnyOfTheseIsNull.contains(e.getKey())) return null;
				}
			}
			return map;
		}
		return null;
	}

	private Set<String> shouldSetFullValueToNullInFormElementToSabloValueIfTheseKeysHaveNullValues(PropertyDescription pd)
	{
		// useful if for example you have columns[] in a custom component where each column is an object of properties and where if a dataprovider or component key in that is null you want that column to be completely ignored (be fully null)
		// then you can combine array's type SKIP_NULL_ITEMS_AT_RUNTIME_CONFIG_KEY with this option (SET_SABLO_VALUE_TO_NULL_IF_ANY_OF_THESE_KEYS_ARE_NULL) in .spec to get the desired result
		JSONObject config = ((JSONObject)pd.getConfig());
		JSONArray v = config != null ? config.optJSONArray(SET_SABLO_VALUE_TO_NULL_IF_ANY_OF_THESE_KEYS_ARE_NULL) : null;
		Set<String> rv = new HashSet<>();
		if (v != null) v.forEach((item) -> {
			if (item instanceof String) rv.add((String)item);
			else log.error(SET_SABLO_VALUE_TO_NULL_IF_ANY_OF_THESE_KEYS_ARE_NULL +
				" configuration option from spec file should be an array of strings but it contains | " + item + " | instead. PD: " + pd);
		});

		return rv;
	}

	@Override
	public Map<String, SabloT> toSabloComponentValue(final Object rhinoValue, final Map<String, SabloT> previousComponentValue, PropertyDescription pd,
		final IWebObjectContext webObjectContext)
	{
		if (rhinoValue == null || RhinoConversion.isUndefinedOrNotFound(rhinoValue)) return null;

		if (rhinoValue instanceof RhinoMapOrArrayWrapper)
		{
			return (Map<String, SabloT>)((RhinoMapOrArrayWrapper)rhinoValue).getWrappedValue();
		}
		else
		{
			final ChangeAwareMap<SabloT, SabloWT> previousSpecialMap = (ChangeAwareMap<SabloT, SabloWT>)previousComponentValue;

			// if it's some kind of object, convert it (in depth, iterate over children)
			if (rhinoValue instanceof NativeObject)
			{
				Map<String, SabloT> rhinoObjectCopy = new HashMap<>();
				NativeObject rhinoNativeObject = (NativeObject)rhinoValue;

				CustomObjectContext<SabloT, SabloWT> customObjectContext = createComponentOrServiceExtension(webObjectContext);

				Object[] keys = rhinoNativeObject.getIds();
				Object value;
				String keyAsString;

				// perform the rhino-to-sablo conversions
				for (Object key : keys)
				{
					if (key instanceof String)
					{
						keyAsString = (String)key;
						value = rhinoNativeObject.get(keyAsString, rhinoNativeObject);
					}
					else if (key instanceof Number)
					{
						keyAsString = String.valueOf(((Number)key).intValue());
						value = rhinoNativeObject.get(((Number)key).intValue(), rhinoNativeObject);
					}
					else throw new RuntimeException("JS Object key must be either String or Number.");

					rhinoObjectCopy.put(keyAsString, (SabloT)NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, null,
						getCustomJSONTypeDefinition().getProperty(keyAsString), customObjectContext));
				}

				// create the new change-aware-map based on the converted sub-properties
				ChangeAwareMap<SabloT, SabloWT> retVal = wrapAndKeepRhinoPrototype(rhinoObjectCopy,
					rhinoNativeObject /*
										 * keep initial native obj. AS a prototype in the future RhinoMapOrArrayWrapper that will be created for Rhino
										 * access/change detection; it can be used there to store/get properties that are not defined in the .spec file and
										 * might be used as server-side impl. details - not to be sent to client
										 */,
					previousSpecialMap, pd, new WrappingContext(webObjectContext.getUnderlyingWebObject(), pd.getName()), customObjectContext);

				// after it is returned it and it's sub-properties will at some point get "attached" (ISmartPropertyValue)
				return retVal;
			}
			else Debug.warn("Cannot convert value assigned from solution scripting into custom object property type; new value = " + rhinoValue +
				"; property = " + pd.getName() + "; component name = " + webObjectContext.getUnderlyingWebObject().getName());
		}
		return previousComponentValue; // or should we return null or throw exception here? incompatible thing was assigned
	}

	protected ChangeAwareMap<SabloT, SabloWT> wrapAndKeepRhinoPrototype(Map<String, SabloT> value, Scriptable prototype,
		ChangeAwareMap<SabloT, SabloWT> previousValue, PropertyDescription propertyDescription, IWrappingContext dataConverterContext,
		CustomObjectContext<SabloT, SabloWT> initialComponentOrServiceExtension)
	{
		Map<String, SabloT> wrappedMap = wrapMap(value, propertyDescription, dataConverterContext);
		if (wrappedMap != null)
		{
			// ok now we have the map or wrap map (depending on if child types are IWrapperType or not)
			// wrap this further into a change-aware map; this is used to be able to track changes and perform server to browser full or granular updates
			return new ChangeAwareMapWithPrototype<SabloT, SabloWT>(wrappedMap, prototype,
				previousValue != null ? previousValue.getListContentVersion() + 1 : 1, initialComponentOrServiceExtension, getCustomJSONTypeDefinition());
		}
		return null;
	}

	protected CustomObjectContext<SabloT, SabloWT> createComponentOrServiceExtension(final IWebObjectContext webObjectContext)
	{
		return new CustomObjectContext<SabloT, SabloWT>(getCustomJSONTypeDefinition(), webObjectContext);
	}

	@Override
	public boolean isValueAvailableInRhino(Map<String, SabloT> webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Map<String, SabloT> webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService, Scriptable startScriptable)
	{
		if (webComponentValue != null)
		{
			CustomObjectContext<SabloT, SabloWT> ext = ((ChangeAwareMap<SabloT, SabloWT>)webComponentValue).getOrCreateComponentOrServiceExtension();
			RhinoMapOrArrayWrapper rhinoValue = new RhinoMapOrArrayWrapper(webComponentValue, ext, pd, startScriptable);
			return rhinoValue;
		}
		return null;
	}

	@Override
	public boolean valueInTemplate(Map<String, FormElementT> object, PropertyDescription pd, FormElementContext formElementContext)
	{
		if (object != null && formElementContext != null && formElementContext.getFormElement() != null && formElementContext.getFormElement().isInDesigner())
		{
			PropertyDescription desc = getCustomJSONTypeDefinition();
			for (Entry<String, PropertyDescription> entry : desc.getProperties().entrySet())
			{
				FormElementT value = object.get(entry.getKey());
				value = (value == IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER) ? null : value;
				if (value != null && entry.getValue().getType() instanceof ISupportTemplateValue< ? >)
				{
					if (!((ISupportTemplateValue)entry.getValue().getType()).valueInTemplate(value, entry.getValue(), formElementContext))
					{
						return false;
					}
				}
			}
			return true;
		}
		// always call tojson to send pushtoserver in real client
		return false;
	}

	@Override
	public boolean isFindModeAware(Map<String, FormElementT> formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution,
		FormElement formElement)
	{
		if (formElementValue == null) return false;

		boolean isFindModeAware = false;

		// just to give a chance to nested find mode aware properties to register themselves in FormElement
		for (Entry<String, PropertyDescription> entry : pd.getProperties().entrySet())
		{
			FormElementT value = formElementValue.get(entry.getKey());
			PropertyDescription entryPD = entry.getValue();
			if (entryPD.getType() instanceof IFindModeAwareType)
			{
				boolean b = ((IFindModeAwareType)entryPD.getType()).isFindModeAware(ServoyJSONObject.jsonNullToNull(value), entryPD, flattenedSolution,
					formElement);
				isFindModeAware |= b;
			}
		}

		return isFindModeAware;
	}

	@Override
	public TargetDataLinks getDataLinks(Map<String, FormElementT> formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution,
		INGFormElement formElement)
	{
		if (ServoyJSONObject.isJavascriptNullOrUndefined(formElementValue)) return TargetDataLinks.NOT_LINKED_TO_DATA;

		ArrayList<String> dps = new ArrayList<>();
		boolean recordLinked = false;

		// just to give a chance to nested find mode aware properties to register themselves in FormElement
		for (Entry<String, PropertyDescription> entry : pd.getProperties().entrySet())
		{
			FormElementT value = formElementValue.get(entry.getKey());
			PropertyDescription entryPD = entry.getValue();

			if (entryPD.getType() instanceof IDataLinkedType)
			{
				TargetDataLinks entryDPs = ((IDataLinkedType)entryPD.getType()).getDataLinks(FormElement.convertToNull(ServoyJSONObject.jsonNullToNull(value)),
					entryPD, flattenedSolution,
					formElement);
				if (entryDPs != null && entryDPs != TargetDataLinks.NOT_LINKED_TO_DATA)
				{
					dps.addAll(Arrays.asList(entryDPs.dataProviderIDs));
					recordLinked |= entryDPs.recordLinked;
				}
			}
		}

		if (dps.size() == 0 && recordLinked == false) return TargetDataLinks.NOT_LINKED_TO_DATA;
		else return new TargetDataLinks(dps.toArray(new String[dps.size()]), recordLinked);
	}

	@Override
	public Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		if (value instanceof NativeObject)
		{
			NativeObject obj = (NativeObject)value;
			JSONObject result = new JSONObject();
			for (Object key : obj.keySet())
			{
				result.put((String)key,
					JSNGWebComponent.fromRhinoToDesignValue(obj.get(key), pd.getProperty((String)key), application, webComponent, (String)key));
			}
			return result;
		}
		return value;
	}

	@Override
	public Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		if (value instanceof JSONObject)
		{
			JSONObject obj = (JSONObject)value;
			Scriptable scope = ScriptableObject.getTopLevelScope(application.getScriptEngine().getSolutionScope());
			Scriptable result = DefaultScope.newObject(scope);
			for (String key : obj.keySet())
			{
				if (IChildWebObject.UUID_KEY.equals(key)) continue;
				result.put(key, result, JSNGWebComponent.fromDesignToRhinoValue(obj.get(key), pd.getProperty(key), application, webComponent, key));
			}
			return result;
		}
		return value;
	}

	@Override
	public Map<String, SabloT> resetI18nValue(Map<String, SabloT> property, PropertyDescription pd, WebFormComponent component)
	{
		if (property != null)
		{
			PropertyDescription customPd = ((CustomJSONObjectType< ? , ? >)pd.getType()).getCustomJSONTypeDefinition();
			for (String prop : property.keySet())
			{
				if (customPd != null && customPd.getProperty(prop) != null && customPd.getProperty(prop).getType() instanceof II18NPropertyType)
				{
					property.put(prop, (SabloT)((II18NPropertyType)customPd.getProperty(prop).getType()).resetI18nValue(property.get(prop),
						customPd.getProperty(prop), component));
				}
			}
		}
		return property;
	}

	@Override
	public Object fromDesignValue(Object designValue, PropertyDescription propertyDescription, IPersist persit)
	{
		return designValue;
	}

	@Override
	public Object toDesignValue(Object javaValue, PropertyDescription pd)
	{
		return javaValue;
	}

}
