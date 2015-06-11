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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.IChangeListener;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.DataConverterContext;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.IPropertyConverter;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithConversions;
import org.sablo.websocket.utils.JSONUtils.JSONStringWithConversions;

import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;
import com.servoy.j2db.server.ngclient.property.IFindModeAwarePropertyValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

/**
 * Runtime value stored in WebFormComponents for properties of type {@link DataproviderPropertyType}.
 * Handles any needed listeners and deals with to and from browser communications, find mode, ....
 *
 * @author acostescu
 */
public class DataproviderTypeSabloValue implements IDataLinkedPropertyValue, IFindModeAwarePropertyValue
{
	protected final String dataProviderID;
	protected final DataAdapterList dataAdapterList;
	protected final IServoyDataConverterContext servoyDataConverterContext;
	private final FormElement formElement;

	protected Object value;
	protected Object jsonValue;
	protected IChangeListener changeMonitor;
	protected PropertyDescription typeOfDP;
	protected ComponentFormat fieldFormat;
	protected boolean findMode = false;
	protected final PropertyDescription dpPD;
	private TargetDataLinks dataLinks;
	private Set<String> tagsDataProviders;
	private boolean displaysTags;

	public DataproviderTypeSabloValue(String dataProviderID, DataAdapterList dataAdapterList, WebFormComponent component, PropertyDescription dpPD)
	{
		if (dataProviderID.startsWith(ScriptVariable.GLOBALS_DOT_PREFIX))
		{
			this.dataProviderID = ScriptVariable.SCOPES_DOT_PREFIX + dataProviderID;
		}
		else
		{
			this.dataProviderID = dataProviderID;
		}

		this.dataAdapterList = dataAdapterList;
		this.servoyDataConverterContext = component.getDataConverterContext();
		this.dpPD = dpPD;
		this.formElement = component.getFormElement();
	}

	protected DataproviderConfig getDataProviderConfig()
	{
		return (DataproviderConfig)dpPD.getConfig();
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	/**
	 * Returns the actual value (that is already full converted by an ui converter) that this dataProvider has.
	 */
	public Object getValue()
	{
		if (!findMode && fieldFormat != null)
		{
			return ComponentFormat.applyUIConverterFromObject(value, dataProviderID, servoyDataConverterContext.getApplication().getFoundSetManager(),
				fieldFormat);
		}
		return value;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeNotifier, BaseWebObject component)
	{
		FormElement formElement = ((WebFormComponent)component).getFormElement();

		this.changeMonitor = changeNotifier;

		// register data link and find mode listeners as needed
		dataLinks = (TargetDataLinks)formElement.getPreprocessedPropertyInfo(IDataLinkedType.class, dpPD);
		if (dataLinks == null)
		{
			// they weren't cached in form element; get them again
			dataLinks = ((DataproviderPropertyType)dpPD.getType()).getDataLinks(dataProviderID, dpPD, servoyDataConverterContext.getSolution(), formElement);
		}
		dataAdapterList.addDataLinkedProperty(this, dataLinks);

		Boolean isFindModeAware = (Boolean)formElement.getPreprocessedPropertyInfo(IFindModeAwareType.class, dpPD);
		if (isFindModeAware == null)
		{
			// they weren't cached in form element; get them again
			isFindModeAware = ((DataproviderPropertyType)dpPD.getType()).isFindModeAware(dataProviderID, dpPD, servoyDataConverterContext.getSolution(),
				formElement);
		}
		if (isFindModeAware != null && isFindModeAware.booleanValue() == true) dataAdapterList.addFindModeAwareProperty(this);

		DataproviderConfig config = (DataproviderConfig)dpPD.getConfig();
		String dtpn = config.getDisplayTagsPropertyName();
		Object dtPropVal = null;
		if (dtpn != null)
		{
			dtPropVal = formElement.getPropertyValue(dtpn);
			if (dtPropVal == null) dtPropVal = Boolean.FALSE;
		}
		displaysTags = dtpn != null && ((Boolean)dtPropVal).booleanValue() == true || (dtpn == null && config.shouldDisplayTags());

		dataProviderOrRecordChanged(dataAdapterList.getRecord(), null, false, false, false);
	}

	@Override
	public void detach()
	{
		// unregister listeners
		dataAdapterList.removeDataLinkedProperty(this);
		dataAdapterList.removeFindModeAwareProperty(this);
	}

	@Override
	public void findModeChanged(boolean newFindMode)
	{
		// this normally only gets called for foundset based dataproviders (so not for global/form variables); DataproviderPropertyType.isFindModeAware(...)
		if (findMode != newFindMode)
		{
			findMode = newFindMode;
			changeMonitor.valueChanged();
		}
	}

	@Override
	public void dataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent)
	{
		// TODO can type or fieldFormat change, for example in scripting the format is reset (but type shouldn't really change)
		if (typeOfDP == null)
		{
			Collection<PropertyDescription> properties = formElement.getWebComponentSpec().getProperties(TypesRegistry.getType("format"));
			for (PropertyDescription formatPd : properties)
			{
				// compare whether format and valuelist property are for same property (dataprovider) or if format is used for valuelist property itself
				Object formatConfig = formatPd.getConfig();
				if (formatConfig instanceof String[] && Arrays.asList((String[])formatConfig).indexOf(dpPD.getName()) != -1)
				{
					INGApplication application = servoyDataConverterContext.getApplication();
					String format = (String)formElement.getPropertyValue(formatPd.getName());
					if (format != null)
					{
						fieldFormat = ComponentFormat.getComponentFormat(
							format,
							dataProviderID,
							application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(),
								servoyDataConverterContext.getForm().getForm()), application);
						break;
					}
				}
			}
			if (fieldFormat != null)
			{
				typeOfDP = NGUtils.getDataProviderPropertyDescription(fieldFormat.uiType, getDataProviderConfig().hasParseHtml());
			}
			else
			{
				// see type of dataprovider; this is done only once - first time we get a new record
				typeOfDP = NGUtils.getDataProviderPropertyDescription(dataProviderID, servoyDataConverterContext.getApplication().getFlattenedSolution(),
					servoyDataConverterContext.getForm().getForm(), record != null ? record.getParentFoundSet().getTable() : null,
					getDataProviderConfig().hasParseHtml());
			}
		}

		String dpID = dataProviderID;
		IDataProviderLookup dpLookup = servoyDataConverterContext.getApplication().getFlattenedSolution().getDataproviderLookup(
			servoyDataConverterContext.getApplication().getFoundSetManager(), servoyDataConverterContext.getForm().getForm());
		if (dpLookup != null)
		{
			IDataProvider dp;
			try
			{
				dp = dpLookup.getDataProvider(dataProviderID);
				if (dp != null)
				{
					dpID = dp.getDataProviderID();
				}
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
		Object v = com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(record, servoyDataConverterContext.getForm().getFormScope(), dpID);
		if (v == Scriptable.NOT_FOUND) v = null;

		if (fieldFormat != null && !findMode)
		{
			v = ComponentFormat.applyUIConverterToObject(v, dataProviderID, servoyDataConverterContext.getApplication().getFoundSetManager(), fieldFormat);
		}

		v = replaceTagsIfNeeded(v);
		if (v instanceof UUID) v = v.toString();
		boolean changed = ((v != value) && (v == null || !v.equals(value)));

		value = v;
		if (changed)
		{
			jsonValue = null;
		}
		if (fireChangeEvent && changed) // TODO I don't get here why changeMonitor.valueChanged() shouldn't be done if fireChangeEvent is false; but kept it as it was before refactor...
		{
			changeMonitor.valueChanged();
		}
	}

	/**
	 * Replaces tagstrings if displaysTags is true.
	 * Also updates the datalinks for this property.
	 * @param v the value of the dataprovider
	 * @return
	 */
	private Object replaceTagsIfNeeded(Object v)
	{
		if (!displaysTags || !(v instanceof String)) return v;

		String val = (String)v;
		if (val.contains("%%") || val.startsWith("i18n:"))
		{
			Pair<String, TargetDataLinks> replaced = TagStringPropertyType.processTags(val, dataAdapterList, servoyDataConverterContext);

			if (tagsDataProviders == null || tagsDataProviders.size() != replaced.getRight().dataProviderIDs.length ||
				!tagsDataProviders.containsAll(new HashSet<String>(Arrays.asList(replaced.getRight().dataProviderIDs))))
			{
				dataAdapterList.removeDataLinkedProperty(this);
				dataAdapterList.addDataLinkedProperty(this, dataLinks.concatDataLinks(replaced.getRight()));
				tagsDataProviders = new HashSet<String>(Arrays.asList(replaced.getRight().dataProviderIDs));
			}
			val = replaced.getLeft();
		}
		else if (tagsDataProviders != null)
		{
			//remove links if the dataprovider value doesn't contain tags anymore
			dataAdapterList.removeDataLinkedProperty(this);
			dataAdapterList.addDataLinkedProperty(this, dataLinks);
			tagsDataProviders = null;
		}

		return val;
	}

	public void toJSON(JSONWriter writer, String key, DataConversion clientConversion, IDataConverterContext dataConverterContext) throws JSONException
	{
		// TODO UUIDs are now just seen as strings
		if (value instanceof UUID)
		{
			value = value.toString();
		}
		JSONUtils.addKeyIfPresent(writer, key);
		if (jsonValue == null)
		{
			if (findMode)
			{
				// in UI show only strings in find mode (just like SC/WC do); if they are something else like real dates/numbers which could happen
				// from scripting, then show string representation
				jsonValue = value instanceof String ? value : (value != null ? String.valueOf(value) : "");
			}
			else if (typeOfDP != null)
			{
				EmbeddableJSONWriter ejw = new EmbeddableJSONWriter(true); // that 'true' is a workaround for allowing directly a value instead of object or array
				DataConversion jsonDataConversion = new DataConversion();
				FullValueToJSONConverter.INSTANCE.toJSONValue(ejw, null, value, typeOfDP, jsonDataConversion, dataConverterContext.getWebObject());
				if (jsonDataConversion.getConversions().size() == 0) jsonDataConversion = null;
				String str = ejw.toJSONString();
				if (str == null || str.trim().length() == 0)
				{
					Debug.error("A dataprovider that is not able to send itself to client... (" + typeOfDP + ", " + value + ")");
					str = "null";
				}
				jsonValue = new JSONStringWithConversions(str, jsonDataConversion);
			}
			else
			{
				jsonValue = value;
			}
		}

		writer.value(jsonValue);
		if (jsonValue instanceof IJSONStringWithConversions) clientConversion.convert(((IJSONStringWithConversions)jsonValue).getDataConversions());
	}

	public void browserUpdateReceived(Object newJSONValue, IDataConverterContext dataConverterContext)
	{
		Object oldValue = value;

		if (!findMode && typeOfDP != null)
		{
			if (typeOfDP.getType() instanceof IPropertyConverter< ? >)
			{
				value = ((IPropertyConverter)typeOfDP.getType()).fromJSON(newJSONValue, value,
					new DataConverterContext(typeOfDP, dataConverterContext.getWebObject()));
			}
			else value = newJSONValue;
		}
		else value = newJSONValue;

		if (oldValue != value && (oldValue == null || !oldValue.equals(value)))
		{
			jsonValue = null;
		}
	}

}