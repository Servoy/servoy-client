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

package com.servoy.j2db.server.ngclient.property;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.sablo.IWebObjectContext;
import org.sablo.IllegalChangeFromClientException;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ArrayOperation;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IGranularProtectionChecker;
import org.sablo.specification.property.IPropertyCanDependsOn;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.IPushToServerSpecialType;
import org.sablo.specification.property.ISupportsGranularUpdates;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.persistence.IRelation;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Implementation for the complex type "foundset".
 *
 * @author acostescu
 */
public class FoundsetPropertyType extends DefaultPropertyType<FoundsetTypeSabloValue> implements IFormElementToTemplateJSON<JSONObject, FoundsetTypeSabloValue>,
	IFormElementToSabloComponent<JSONObject, FoundsetTypeSabloValue>, IFormElementDefaultValueToSabloComponent<JSONObject, FoundsetTypeSabloValue>,
	IConvertedPropertyType<FoundsetTypeSabloValue>, ISabloComponentToRhino<FoundsetTypeSabloValue>, IRhinoToSabloComponent<FoundsetTypeSabloValue>,
	ISupportsGranularUpdates<FoundsetTypeSabloValue>, IDataLinkedType<JSONObject, FoundsetTypeSabloValue>, IPushToServerSpecialType,
	IGranularProtectionChecker<FoundsetTypeSabloValue>, IPropertyWithClientSideConversions<FoundsetTypeSabloValue>, IPropertyCanDependsOn
{
	public static final FoundsetPropertyType INSTANCE = new FoundsetPropertyType();

	public static final String TYPE_NAME = "foundset";

	public static final String FOUNDSET_SELECTOR = "foundsetSelector";
	public static final String LOAD_ALL_RECORDS_FOR_SEPARATE = "loadAllRecords";

	private static final String DATAPROVIDERS_KEY_FOR_RHINO = "dataproviders";
	private static final String FOUNDSET_KEY_FOR_RHINO = "foundset";
	private static final String RELATIONNAME_KEY_FOR_RHINO = "relationname";
	public static final String DATAPROVIDERS_KEY_FOR_DESIGN = "dataproviders";

	private String[] dependencies;

	public FoundsetPropertyType()
	{
		super();
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, JSONObject formElementValue, PropertyDescription pd,
		FormElementContext formElementContext) throws JSONException
	{
		if (formElementValue == null) return writer;

		// this just dumps an empty/dummy value
		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();
		writer.key(FoundsetTypeSabloValue.SERVER_SIZE).value(0);
		writer.key(FoundsetTypeSabloValue.SELECTED_ROW_INDEXES).array().endArray();
		writer.key(FoundsetTypeSabloValue.MULTI_SELECT).value(false);
		writer.key(FoundsetTypeSabloValue.FIND_MODE).value(false);
		writer.key(FoundsetTypeSabloValue.HAS_MORE_ROWS).value(false);

		// viewPort
		writer.key(FoundsetTypeSabloValue.VIEW_PORT).object().key(FoundsetTypeSabloValue.START_INDEX).value(0).key(FoundsetTypeSabloValue.SIZE).value(0).key(
			FoundsetTypeSabloValue.ROWS).array().endArray().endObject();
		// end viewPort

		writer.endObject();
		return writer;
	}

	@Override
	public FoundsetTypeSabloValue toSabloComponentValue(JSONObject formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dal)
	{
		return formElementValue != null ? new FoundsetTypeSabloValue(formElementValue, pd.getName(), dal, (FoundsetPropertyTypeConfig)pd.getConfig()) : null;
	}

	@Override
	public FoundsetTypeSabloValue toSabloComponentDefaultValue(PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		if (pd.getDefaultValue() instanceof JSONObject)
		{
			return toSabloComponentValue((JSONObject)pd.getDefaultValue(), pd, formElement, component, dataAdapterList);
		}
		return null;
	}

	@Override
	public FoundsetTypeSabloValue fromJSON(Object newJSONValue, FoundsetTypeSabloValue previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (previousSabloValue != null)
		{
			previousSabloValue.browserUpdatesReceived(newJSONValue, pd, dataConverterContext);
		}
		// else there's nothing to do here / this type can't receive browser updates when server has no value for it

		return previousSabloValue;
	}

	@Override
	public JSONWriter changesToJSON(JSONWriter writer, String key, FoundsetTypeSabloValue sabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			sabloValue.changesToJSON(writer, dataConverterContext);
		}
		return writer;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, FoundsetTypeSabloValue sabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			sabloValue.toJSON(writer, dataConverterContext);
		}
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(FoundsetTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return webComponentValue != null;
	}

	@Override
	public Object toRhinoValue(FoundsetTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService,
		Scriptable startScriptable)
	{
		return webComponentValue != null ? new FoundsetTypeSableValueWrapper(startScriptable, webComponentValue, pd) : Scriptable.NOT_FOUND;
	}

	@Override
	public TargetDataLinks getDataLinks(JSONObject formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement)
	{
		return TargetDataLinks.LINKED_TO_ALL; // if you change this you should call this method in FoundsetTypeSabloValue.attach as well when registering as listener to DAL
	}

	/**
	 * @author jcompagner
	 */
	private final class FoundsetTypeSableValueWrapper extends DefaultScope
	{
		private final FoundsetTypeSabloValue webComponentValue;
		private final PropertyDescription pd;

		private FoundsetTypeSableValueWrapper(Scriptable parent, FoundsetTypeSabloValue webComponentValue, PropertyDescription pd)
		{
			super(parent);
			this.webComponentValue = webComponentValue;
			this.pd = pd;
		}

		@Override
		public Object get(String name, Scriptable start)
		{
			switch (name)
			{
				case FOUNDSET_KEY_FOR_RHINO :
				{
					Context cx = Context.enter();
					try
					{
						IFoundSetInternal foundset = webComponentValue.getFoundset();
						if (foundset == null) foundset = webComponentValue.checkForExistingFormFoundsetBeforeFormIsVisibleAndGetIt();
						if (foundset != null) return cx.getWrapFactory().wrap(cx, start, foundset, foundset.getClass());
						return foundset;
					}
					finally
					{
						Context.exit();
					}
				}
				case DATAPROVIDERS_KEY_FOR_RHINO :
				{
					if (webComponentValue.recordDataLinkedPropertyIDToColumnDP.size() > 0)
					{
						return "Foundset bound to a components with dataproviders, can't be set through the foundset property";
					}
					DefaultScope dataproviders = new DefaultScope(start)
					{
						@Override
						public Object get(String nm, Scriptable strt)
						{
							return webComponentValue.dataproviders.get(nm);
						}

						@Override
						public void put(String nm, Scriptable strt, Object value)
						{
							final String dp = webComponentValue.dataproviders.get(nm);
							if ((dp == null && value != null) || (dp != null && !dp.equals(value)))
							{
								webComponentValue.dataproviders.put(nm, (String)value);
								webComponentValue.notifyDataProvidersUpdated();
							}
						}

						@Override
						public Object[] getIds()
						{
							return webComponentValue.dataproviders.keySet().toArray();
						}
					};
					return dataproviders;
				}
				case RELATIONNAME_KEY_FOR_RHINO :
				{
					return webComponentValue.getFoundsetSelector();
				}
			}
			return Scriptable.NOT_FOUND;
		}

		@SuppressWarnings("nls")
		@Override
		public void put(String name, Scriptable start, Object val)
		{
			// can't we already unwrap it all here? Foundset can be done but also the dataprovdiers that test for scriptable itself?
			Object value = val;
			switch (name)
			{
				case FOUNDSET_KEY_FOR_RHINO :
				{
					value = value instanceof Wrapper ? ((Wrapper)value).unwrap() : value;
					if (value instanceof IFoundSetInternal)
					{
						String foundsetSelector = webComponentValue.foundsetSelector;
						if (foundsetSelector != null)
						{
							// if foundsetSelector is a relation, get right datasource
							IRelation r = webComponentValue.getFoundSetManager().getRelation(foundsetSelector);
							if (r != null)
							{
								foundsetSelector = r.getForeignDataSource();
							}
						}

						if (foundsetSelector == null || foundsetSelector.equals(((IFoundSetInternal)value).getDataSource()))
						{
							webComponentValue.updateFoundset((IFoundSetInternal)value);
						}
						else Debug.error("Error Setting foundset value through scripting (servoy scripting or server side api scripting", new RuntimeException(
							"illegal value '" + value + "' to set on the foundset property with foundsetSelector/datasource: " + foundsetSelector + '(' +
								webComponentValue.foundsetSelector + "), foundset (" + ((IFoundSetInternal)value).getDataSource() +
								") is either pinned to form's foundset or to a related foundset " + pd.getName()));
					}
					else Debug.error("Error Setting foundset value through scripting (servoy scripting or server side api scripting",
						new RuntimeException("illegal value '" + value + "' to set on the foundset property " + pd.getName()));
					break;
				}
				case DATAPROVIDERS_KEY_FOR_RHINO :
				{
					if (webComponentValue.recordDataLinkedPropertyIDToColumnDP.size() > 0)
					{
						throw new RuntimeException("Foundset bound to a components with dataproviders, can't be set through the foundset property");
					}
					if (value instanceof Scriptable)
					{
						webComponentValue.dataproviders.clear();
						Object[] ids = ((Scriptable)value).getIds();
						for (Object id : ids)
						{
							webComponentValue.dataproviders.put((String)id, (String)((Scriptable)value).get((String)id, ((Scriptable)value)));
						}
						webComponentValue.notifyDataProvidersUpdated();
					}
					else Debug.error("Error Setting foundset value through scripting (servoy scripting or server side api scripting",
						new RuntimeException("illegal value '" + value + "' to set on the dataprovides property " + pd.getName()));
					break;
				}
				case RELATIONNAME_KEY_FOR_RHINO :
				{
					webComponentValue.updateFoundsetSelector(value != null ? value.toString() : null);
				}
			}
		}

		@Override
		public Object[] getIds()
		{
			return new Object[] { FOUNDSET_KEY_FOR_RHINO, DATAPROVIDERS_KEY_FOR_RHINO, RELATIONNAME_KEY_FOR_RHINO };
		}

	}

	@Override
	public boolean allowPush(Object data, FoundsetTypeSabloValue sabloValue, IllegalChangeFromClientException e)
	{
		if (sabloValue != null)
		{
			return sabloValue.allowPush(data, e);
		}
		return false;
	}


	@Override
	public boolean shouldAlwaysAllowIncommingJSON()
	{
		return true;
	}

	@Override
	public Object parseConfig(JSONObject config)
	{
		dependencies = getDependencies(config, dependencies);
		return new FoundsetPropertyTypeConfig(config);
	}

	@Override
	public FoundsetTypeSabloValue toSabloComponentValue(Object rhinoValue, FoundsetTypeSabloValue previousComponentValue, PropertyDescription pd,
		IWebObjectContext componentOrService)
	{
		FoundsetTypeSabloValue newSabloValue = null;

		IFoundSetInternal newFoundset = null;
		JSONObject dataproviders = null;
		boolean sendDefaultFormats = FoundsetPropertyTypeConfig.DEFAULT_SEND_DEFAULT_FORMATS;
		int initialPreferredViewPortSize = FoundsetPropertyTypeConfig.DEFAULT_INITIALL_PREFERRED_VIEWPORT_SIZE;
		boolean sendSelectionViewportInitially = FoundsetPropertyTypeConfig.DEFAULT_SEND_SELECTION_VIEWPORT_INITIALLY;
		boolean centerInitialViewportOnSelected = FoundsetPropertyTypeConfig.DEFAULT_CENTER_INITIAL_VIEWPORT_ON_SELECTED;
		boolean foundsetDefinitionListener = FoundsetPropertyTypeConfig.DEFAULT_FOUNDSET_DEFINITION_LISTENER;

		if (rhinoValue instanceof Wrapper) rhinoValue = ((Wrapper)rhinoValue).unwrap();

		// rhinoValue can be:
		// 1.                {
		//                       foundset : ...someIFoundSetInternal...,
		//                       dataproviders : { dp1: "userNickname", dp2: "userReviewRating", ... }
		//                       sendDefaultFormats : ...someBoolean...,
		//                       sendSelectionViewportInitially : ...someBoolean...,
		//                       initialPreferredViewPortSize: ..someInteger...
		//                   }
		//    any of the keys above are optional except "foundset"
		// 2. or it can directly be a IFoundSetInternal value in which case dataproviders is considered empty

		if (rhinoValue instanceof FoundsetTypeSableValueWrapper)
		{
			return ((FoundsetTypeSableValueWrapper)rhinoValue).webComponentValue;
		}
		if (rhinoValue instanceof NativeObject)
		{
			NativeObject obj = (NativeObject)rhinoValue;
			Object foundset = obj.get(FOUNDSET_KEY_FOR_RHINO, obj);
			if (foundset instanceof Wrapper) foundset = ((Wrapper)foundset).unwrap();
			if (foundset instanceof IFoundSetInternal)
			{
				newFoundset = (IFoundSetInternal)foundset;
				if (obj.has(FoundsetPropertyTypeConfig.DATAPROVIDERS, obj))
				{
					Object dpsValue = obj.get(FoundsetPropertyTypeConfig.DATAPROVIDERS, obj);
					if (dpsValue instanceof Scriptable)
					{
						dataproviders = new JSONObject();
						Object[] ids = ((Scriptable)dpsValue).getIds();
						for (Object id : ids)
						{
							dataproviders.put((String)id, (String)((Scriptable)dpsValue).get((String)id, ((Scriptable)dpsValue)));
						}
					}
				}
				if (obj.has(FoundsetPropertyTypeConfig.SEND_DEFAULT_FORMATS, obj))
					sendDefaultFormats = Utils.getAsBoolean(obj.get(FoundsetPropertyTypeConfig.SEND_DEFAULT_FORMATS, obj));
				if (obj.has(FoundsetPropertyTypeConfig.INITIAL_PREFERRED_VIEWPORT_SIZE, obj))
					initialPreferredViewPortSize = Utils.getAsInteger(obj.get(FoundsetPropertyTypeConfig.INITIAL_PREFERRED_VIEWPORT_SIZE, obj));
				if (obj.has(FoundsetPropertyTypeConfig.SEND_SELECTION_VIEWPORT_INITIALLY, obj))
					sendSelectionViewportInitially = Utils.getAsBoolean(obj.get(FoundsetPropertyTypeConfig.SEND_SELECTION_VIEWPORT_INITIALLY, obj));
				if (obj.has(FoundsetPropertyTypeConfig.CENTER_INITIAL_VIEWPORT_ON_SELECTED, obj))
					centerInitialViewportOnSelected = Utils.getAsBoolean(obj.get(FoundsetPropertyTypeConfig.CENTER_INITIAL_VIEWPORT_ON_SELECTED, obj));

				Object config = pd.getConfig();
				if (config instanceof FoundsetPropertyTypeConfig)
				{
					foundsetDefinitionListener = ((FoundsetPropertyTypeConfig)config).foundsetDefinitionListener;
				}
				else
				{
					// should not happen, we should always use value from spec
					foundsetDefinitionListener = FoundsetPropertyTypeConfig.DEFAULT_FOUNDSET_DEFINITION_LISTENER;
				}
			}
		}
		else if (rhinoValue instanceof IFoundSetInternal)
		{
			newFoundset = (IFoundSetInternal)rhinoValue;
		}

		if (newFoundset != null)
		{
			if (previousComponentValue != null && previousComponentValue.getFoundset() == newFoundset) newSabloValue = previousComponentValue;
			else
			{
				JSONObject designJSON = new JSONObject();
				designJSON.put(FOUNDSET_SELECTOR, (String)null); // tell the FoundsetTypeSabloValue that it shouldn't try to automatically get a foundset
				if (dataproviders != null) designJSON.put(DATAPROVIDERS_KEY_FOR_DESIGN, dataproviders);


				newSabloValue = new FoundsetTypeSabloValue(designJSON, null, null,
					new FoundsetPropertyTypeConfig(sendDefaultFormats, true, null, sendSelectionViewportInitially, initialPreferredViewPortSize,
						foundsetDefinitionListener, centerInitialViewportOnSelected));
				newSabloValue.updateFoundset(newFoundset);
			}
		}
		else if (rhinoValue != null && !RhinoConversion.isUndefinedOrNotFound(rhinoValue))
		{
			throw new RuntimeException("Cannot set " + rhinoValue +
				" into a foundset property type; for existing foundset properties you can set the myProp.foundset or myProp.dataproviders directly. If you want to set a complete new value you have to give here something like { foundset : ...someFoundset..., dataproviders : { dp1: 'userNickname', dp2: 'userReviewRating', ... } } instead.");
		}

		return newSabloValue;
	}

	public static boolean writeViewportOperationToJSON(ArrayOperation op, ViewportRowDataProvider rowDataProvider, IFoundSetInternal foundset,
		int viewportStartIndex,
		JSONWriter w,
		String keyInParent, Object sabloValueThatRequestedThisDataToBeWritten) throws JSONException
	{
		JSONUtils.addKeyIfPresent(w, keyInParent);

		w.object();

		ViewportClientSideTypes clientSideTypesForViewport = null;
		// write actual data if necessary
		if (op.type != ArrayOperation.DELETE)
		{
			w.key("rows");
			clientSideTypesForViewport = rowDataProvider.writeRowData(viewportStartIndex + op.startIndex, viewportStartIndex + op.endIndex, op.cellNames,
				foundset, w,
				sabloValueThatRequestedThisDataToBeWritten);
		}

		if (clientSideTypesForViewport != null) clientSideTypesForViewport.writeClientSideTypes(w, JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY);

		w.key("startIndex").value(Integer.valueOf(op.startIndex)).key("endIndex").value(Integer.valueOf(op.endIndex)).key("type").value(
			Integer.valueOf(op.type)).endObject();

		return true;
	}

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);
		w.value(TYPE_NAME);

		return true;
	}

	@Override
	public String[] getDependencies()
	{
		return dependencies;
	}

}