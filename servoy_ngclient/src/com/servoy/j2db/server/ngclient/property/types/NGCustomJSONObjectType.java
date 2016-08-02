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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ChangeAwareMap;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.WrappingContext;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.scripting.solutionmodel.JSNGWebComponent;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementExtension;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RhinoMapOrArrayWrapper;
import com.servoy.j2db.server.ngclient.property.ComponentTypeFormElementValue;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignDefaultToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.InitialToJSONConverter;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * A JSON array type that is Servoy NG client aware as well.
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
	IDataLinkedType<Map<String, FormElementT>, Map<String, SabloT>>, IRhinoDesignConverter
{

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
					else if (pd.getType().defaultValue(pd) != null)
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
		DataConversion conversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		if (conversionMarkers != null) conversionMarkers.convert(CustomJSONObjectType.TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets

		if (formElementValue != null)
		{
			writer.object().key(CONTENT_VERSION).value(1).key(VALUE).object();
			DataConversion arrayConversionMarkers = new DataConversion();
			for (Entry<String, FormElementT> e : formElementValue.entrySet())
			{
				arrayConversionMarkers.pushNode(e.getKey());
				NGConversions.INSTANCE.convertFormElementToTemplateJSONValue(writer, e.getKey(), e.getValue(),
					getCustomJSONTypeDefinition().getProperty(e.getKey()), arrayConversionMarkers, formElementContext);
				arrayConversionMarkers.popNode();
			}
			writer.endObject();
			if (arrayConversionMarkers.getConversions().size() > 0)
			{
				writer.key("conversions").object();
				JSONUtils.writeConversions(writer, arrayConversionMarkers.getConversions());
				writer.endObject();
			}
			writer.endObject();
		}
		else
		{
			writer.value(JSONObject.NULL);
		}
		return writer;
	}

	@Override
	public JSONWriter initialToJSON(JSONWriter writer, String key, ChangeAwareMap<SabloT, SabloWT> changeAwareMap, PropertyDescription pd,
		DataConversion conversionMarkers, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		return toJSON(writer, key, changeAwareMap, conversionMarkers, true, InitialToJSONConverter.INSTANCE, dataConverterContext);
	}

	@Override
	public Map<String, SabloT> toSabloComponentValue(Map<String, FormElementT> formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dal)
	{
		if (formElementValue != null)
		{
			Map<String, SabloT> map = new HashMap<>(formElementValue.size());
			for (Entry<String, FormElementT> e : formElementValue.entrySet())
			{
				if (e.getValue() instanceof ComponentTypeFormElementValue &&
					!((ComponentTypeFormElementValue)e.getValue()).isSecurityViewable(dal.getApplication().getFlattenedSolution()))
				{
					continue;
				}
				Object v = NGConversions.INSTANCE.convertFormElementToSabloComponentValue(e.getValue(), getCustomJSONTypeDefinition().getProperty(e.getKey()),
					new FormElementExtension(formElement, formElementValue, getCustomJSONTypeDefinition()), component, dal);
				if (v != null) map.put(e.getKey(), (SabloT)v);
			}
			return map;
		}
		return null;
	}

	@Override
	public Map<String, SabloT> toSabloComponentValue(final Object rhinoValue, final Map<String, SabloT> previousComponentValue, PropertyDescription pd,
		final BaseWebObject componentOrService)
	{
		if (rhinoValue == null || rhinoValue == Scriptable.NOT_FOUND) return null;

		final ChangeAwareMap<SabloT, SabloWT> previousSpecialMap = (ChangeAwareMap<SabloT, SabloWT>)previousComponentValue;
		if (rhinoValue instanceof RhinoMapOrArrayWrapper)
		{
			return (Map<String, SabloT>)((RhinoMapOrArrayWrapper)rhinoValue).getWrappedValue();
		}
		else if (previousSpecialMap != null && previousSpecialMap.getBaseMap() instanceof IRhinoNativeProxy &&
			((IRhinoNativeProxy)previousSpecialMap.getBaseMap()).getBaseRhinoScriptable() == rhinoValue)
		{
			return previousComponentValue; // this can get called a lot when a native Rhino wrapper map and proxy are in use; don't create new values each time
			// something is accessed in the wrapper+converter+proxy map cause that messes up references
		}
		else
		{
			// if it's some kind of object, convert it (in depth, iterate over children)

			RhinoNativeObjectWrapperMap<SabloT, SabloWT> rhinoMap = null;

			if (rhinoValue instanceof NativeObject)
			{
				rhinoMap = new RhinoNativeObjectWrapperMap<SabloT, SabloWT>((NativeObject)rhinoValue, getCustomJSONTypeDefinition(), previousComponentValue,
					componentOrService, getChildPropsThatNeedWrapping());
				ChangeAwareMap<SabloT, SabloWT> cam = wrap(rhinoMap, previousSpecialMap, pd, new WrappingContext(componentOrService, pd.getName()));
				cam.markAllChanged();
				return cam;

				// if we really want to remove the extra-conversion map above and convert all to a new map we could do it by executing the code below after a toJSON is called (so after a request finishes,
				// we consider that in the next request the user will only use property reference again taken from service/component, so the new converted map, not anymore the object that was created in JS directly,
				// but this still won't work if the user really holds on to that old/initial reference and changes it...); actually if the initial value is used, it will not be change-aware anyway...
//				for (Entry<String, Object> e : rhinoMap.entrySet())
//				{
//					convertedMap.put(
//						e.getKey(),
//						NGConversions.INSTANCE.convertRhinoToSabloComponentValue(e.getValue(),
//							previousComponentValue != null ? previousComponentValue.get(e.getKey()) : null,
//							getCustomJSONTypeDefinition().getProperty(e.getKey()), componentOrService));
//				}
			}
		}
		return previousComponentValue; // or should we return null or throw exception here? incompatible thing was assigned
	}

	@Override
	public boolean isValueAvailableInRhino(Map<String, SabloT> webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Map<String, SabloT> webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		return webComponentValue == null ? null : new RhinoMapOrArrayWrapper(webComponentValue, componentOrService, pd, startScriptable);
	}

	@Override
	public boolean valueInTemplate(Map<String, FormElementT> object, PropertyDescription pd, FormElementContext formElementContext)
	{
		if (object != null)
		{
			PropertyDescription desc = getCustomJSONTypeDefinition();
			for (Entry<String, PropertyDescription> entry : desc.getProperties().entrySet())
			{
				FormElementT value = object.get(entry.getKey());
				if (value != null && entry.getValue().getType() instanceof ISupportTemplateValue< ? >)
				{
					if (!((ISupportTemplateValue)entry.getValue().getType()).valueInTemplate(value, entry.getValue(), formElementContext))
					{
						return false;
					}
				}
			}
		}
		return true;
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
			// as array element property descriptions can describe multiple property values in the same bean - we won't cache those
			if (entryPD.getType() instanceof IFindModeAwareType)
			{
				boolean b = ((IFindModeAwareType)entryPD.getType()).isFindModeAware(ServoyJSONObject.jsonNullToNull(value), entryPD, flattenedSolution,
					formElement);
				isFindModeAware |= b;
				formElement.getOrCreatePreprocessedPropertyInfoMap(IFindModeAwareType.class).put(entryPD, Boolean.valueOf(b));
			}
		}

		return isFindModeAware;
	}

	@Override
	public TargetDataLinks getDataLinks(Map<String, FormElementT> formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution,
		FormElement formElement)
	{
		if (ServoyJSONObject.isJavascriptNullOrUndefined(formElementValue)) return TargetDataLinks.NOT_LINKED_TO_DATA;

		ArrayList<String> dps = new ArrayList<>();
		boolean recordLinked = false;

		// just to give a chance to nested find mode aware properties to register themselves in FormElement
		for (Entry<String, PropertyDescription> entry : pd.getProperties().entrySet())
		{
			FormElementT value = formElementValue.get(entry.getKey());
			PropertyDescription entryPD = entry.getValue();
			// as array element property descriptions can describe multiple property values in the same bean - we won't cache those
			if (entryPD.getType() instanceof IDataLinkedType)
			{
				TargetDataLinks entryDPs = ((IDataLinkedType)entryPD.getType()).getDataLinks(ServoyJSONObject.jsonNullToNull(value), entryPD, flattenedSolution,
					formElement);
				formElement.getOrCreatePreprocessedPropertyInfoMap(IDataLinkedType.class).put(entryPD, entryDPs);
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
			Context cx = Context.enter();
			Scriptable result = cx.newObject(scope);
			for (String key : obj.keySet())
			{
				if (IChildWebObject.UUID_KEY.equals(key)) continue;
				result.put(key, result, JSNGWebComponent.fromDesignToRhinoValue(obj.get(key), pd.getProperty(key), application, webComponent, key));
			}
			return result;
		}
		return value;
	}

}
