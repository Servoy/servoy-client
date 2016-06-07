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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.IChangeListener;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithConversions;
import org.sablo.websocket.utils.JSONUtils.JSONStringWithConversions;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.FormAndTableDataProviderLookup;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.FoundSetEvent;
import com.servoy.j2db.dataprocessing.IFoundSetEventListener;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.Relation;
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
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;
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
	private IFoundSetEventListener globalRelatedFoundsetListener;
	private IFoundSetInternal globalRelatedFoundset;
	private String globalRelationName;

	private ListSelectionListener relatedFoundsetSelectionListener;
	private ArrayList<IFoundSetInternal> relatedFoundsets = new ArrayList<IFoundSetInternal>();
	private IModificationListener relatedRecordModificationListener;
	private ArrayList<IRecordInternal> relatedRecords = new ArrayList<IRecordInternal>();
	private String relationName;

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
		if (dataProviderID.indexOf('.') != -1 && !ScopesUtils.isVariableScope(dataProviderID))
		{
			Relation relation = dataAdapterList.getApplication().getFlattenedSolution().getRelation(dataProviderID.substring(0, dataProviderID.indexOf('.')));
			if (relation != null && relation.isGlobal())
			{

				globalRelationName = relation.getName();
				globalRelatedFoundsetListener = new IFoundSetEventListener()
				{
					@Override
					public void foundSetChanged(FoundSetEvent e)
					{
						if (e.getType() == FoundSetEvent.CONTENTS_CHANGED)
						{
							dataProviderOrRecordChanged(DataproviderTypeSabloValue.this.dataAdapterList.getRecord(), null, false, false, true);
						}
					}
				};
			}
			relationName = dataProviderID.substring(0, dataProviderID.lastIndexOf('.'));
			relatedFoundsetSelectionListener = new ListSelectionListener()
			{
				public void valueChanged(ListSelectionEvent e)
				{
					dataProviderOrRecordChanged(DataproviderTypeSabloValue.this.dataAdapterList.getRecord(), null, false, false, true);
				}
			};
			relatedRecordModificationListener = new IModificationListener()
			{
				@Override
				public void valueChanged(ModificationEvent e)
				{
					dataProviderOrRecordChanged(DataproviderTypeSabloValue.this.dataAdapterList.getRecord(), null, false, false, true);
				}
			};
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
//		dataLinks = (TargetDataLinks)formElement.getPreprocessedPropertyInfo(IDataLinkedType.class, dpPD);
//		if (dataLinks == null)
//		{
//			// they weren't cached in form element; get them again
//			dataLinks = ((DataproviderPropertyType)dpPD.getType()).getDataLinks(dataProviderID, dpPD, servoyDataConverterContext.getSolution(), formElement);
//		}
		dataLinks = ((DataproviderPropertyType)dpPD.getType()).getDataLinks(dataProviderID, dpPD, servoyDataConverterContext.getSolution(), formElement);
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
		if (globalRelatedFoundset != null)
		{
			globalRelatedFoundset.removeFoundSetEventListener(globalRelatedFoundsetListener);
		}
		globalRelatedFoundset = null;
		globalRelatedFoundsetListener = null;

		for (IFoundSetInternal relatedFoundset : relatedFoundsets)
		{
			((ISwingFoundSet)relatedFoundset).getSelectionModel().removeListSelectionListener(relatedFoundsetSelectionListener);
		}
		relatedFoundsetSelectionListener = null;
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
	public void dataProviderOrRecordChanged(final IRecordInternal record, final String dataProvider, final boolean isFormDP, final boolean isGlobalDP,
		boolean fireChangeEvent)
	{
		// TODO can type or fieldFormat change, for example in scripting the format is reset (but type shouldn't really change)
		IDataProviderLookup dpLookup = new FormAndTableDataProviderLookup(servoyDataConverterContext.getApplication().getFlattenedSolution(),
			servoyDataConverterContext.getForm().getForm(), record != null ? record.getParentFoundSet().getTable() : null);
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
					fieldFormat = ComponentFormat.getComponentFormat(format, dataProviderID, dpLookup, application);
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
		if (dpPD.hasTag("typeName"))
		{
			IPropertyType< ? > specType = TypesRegistry.getType((String)dpPD.getTag("typeName"));
			if (specType != null && (typeOfDP == null || !specType.getClass().isAssignableFrom(typeOfDP.getClass())))
			{
				typeOfDP = new PropertyDescription("Spec type hint", specType);
			}
		}
		if (globalRelationName != null)
		{
			try
			{
				IFoundSetInternal newRelatedFoundset = servoyDataConverterContext.getApplication().getFoundSetManager().getGlobalRelatedFoundSet(
					globalRelationName);
				if (newRelatedFoundset != globalRelatedFoundset)
				{
					if (globalRelatedFoundset != null)
					{
						globalRelatedFoundset.removeFoundSetEventListener(globalRelatedFoundsetListener);
					}
					globalRelatedFoundset = newRelatedFoundset;
					globalRelatedFoundset.addFoundSetEventListener(globalRelatedFoundsetListener);
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		if (relatedFoundsetSelectionListener != null)
		{
			try
			{
				ArrayList<IFoundSetInternal> newRelatedFoundsets = getRelatedFoundsets(record, relationName);

				if (!newRelatedFoundsets.equals(relatedFoundsets))
				{
					for (IFoundSetInternal relatedFoundset : relatedFoundsets)
					{
						((ISwingFoundSet)relatedFoundset).getSelectionModel().removeListSelectionListener(relatedFoundsetSelectionListener);
					}

					relatedFoundsets = newRelatedFoundsets;

					for (IFoundSetInternal relatedFoundset : relatedFoundsets)
					{
						((ISwingFoundSet)relatedFoundset).getSelectionModel().addListSelectionListener(relatedFoundsetSelectionListener);
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		if (relatedRecordModificationListener != null)
		{
			try
			{
				ArrayList<IRecordInternal> newRelatedRecords = getRelatedRecords(record, relationName);

				if (!newRelatedRecords.equals(relatedRecords))
				{
					for (IRecordInternal relatedRecord : relatedRecords)
					{
						relatedRecord.removeModificationListener(relatedRecordModificationListener);
					}

					relatedRecords = newRelatedRecords;

					for (IRecordInternal relatedRecord : relatedRecords)
					{
						relatedRecord.addModificationListener(relatedRecordModificationListener);
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}


		String dpID = dataProviderID;
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

	private ArrayList<IFoundSetInternal> getRelatedFoundsets(IRecordInternal record, String relName)
	{
		ArrayList<IFoundSetInternal> returnRelatedFoundsets = new ArrayList<IFoundSetInternal>();
		if (record != null)
		{
			StringTokenizer st = new StringTokenizer(relName, "."); //$NON-NLS-1$
			String r = null;
			while (st.hasMoreTokens())
			{
				if (r == null) r = st.nextToken();
				else r = r + "." + st.nextToken(); //$NON-NLS-1$
				IFoundSetInternal fs = record.getRelatedFoundSet(r);
				if (fs != null)
				{
					returnRelatedFoundsets.add(fs);
				}
			}
		}

		return returnRelatedFoundsets;
	}

	private ArrayList<IRecordInternal> getRelatedRecords(IRecordInternal record, String relName)
	{
		// similar code as the loop below is also in class DisplaysAdapter - just in case future fixes need to apply to both places
		ArrayList<IRecordInternal> returnRelatedRecords = new ArrayList<IRecordInternal>();
		if (record != null)
		{
			// get the new records were are depending on
			IRecordInternal currRecord = record;
			String[] parts = relName.split("\\."); //$NON-NLS-1$

			for (int i = 0; currRecord != null && i < parts.length; i++)
			{
				Object v = currRecord.getValue(parts[i]);
				if (v instanceof ISwingFoundSet)
				{
					currRecord = ((ISwingFoundSet)v).getRecord(((ISwingFoundSet)v).getSelectedIndex());
					if (currRecord == null) currRecord = ((ISwingFoundSet)v).getPrototypeState();
					returnRelatedRecords.add(currRecord);
				}
				else
				{
					currRecord = null;
				}
			}
		}

		return returnRelatedRecords;
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
		String result = val;
		if (val.contains("%%") || val.startsWith("i18n:"))
		{
			final Set<String> dataProviders = new HashSet<>();
			final boolean recordDP[] = new boolean[1];

			result = Text.processTags(val, new ITagResolver()
			{
				@Override
				public String getStringValue(String name)
				{
					String dp = name;
					if (dp.startsWith(ScriptVariable.GLOBALS_DOT_PREFIX))
					{
						dp = ScriptVariable.SCOPES_DOT_PREFIX + dp;
					}

					dataProviders.add(dp);
					// TODO Can't it be something special like record count or current record which are special cases and could still not depend on record...?
					recordDP[0] = recordDP[0] || (!ScopesUtils.isVariableScope(dp) && dataAdapterList.getForm().getForm().getScriptVariable(dp) == null);

					return dataAdapterList.getStringValue(dp);
				}
			});

			if (result.startsWith("i18n:")) result = dataAdapterList.getApplication().getI18NMessage(result.substring(5));

			if (tagsDataProviders == null || tagsDataProviders.size() != dataProviders.size() || !tagsDataProviders.containsAll(dataProviders))
			{
				dataAdapterList.removeDataLinkedProperty(this);
				dataAdapterList.addDataLinkedProperty(this, dataLinks.concatDataLinks(dataProviders.toArray(new String[dataProviders.size()]), recordDP[0]));
				tagsDataProviders = dataProviders;
			}
		}
		else if (tagsDataProviders != null)
		{
			//remove links if the dataprovider value doesn't contain tags anymore
			dataAdapterList.removeDataLinkedProperty(this);
			dataAdapterList.addDataLinkedProperty(this, dataLinks);
			tagsDataProviders = null;
		}

		return result;
	}

	public void toJSON(JSONWriter writer, String key, DataConversion clientConversion, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		// TODO UUIDs are now just seen as strings
		if (value instanceof UUID)
		{
			value = value.toString();
		}
		else if (value instanceof DbIdentValue)
		{
			value = ((DbIdentValue)value).getPkValue();
		}

		JSONUtils.addKeyIfPresent(writer, key);
		if (jsonValue == null)
		{
			jsonValue = getValueForToJSON(value, dataConverterContext);
		}

		writer.value(jsonValue);
		if (jsonValue instanceof IJSONStringWithConversions) clientConversion.convert(((IJSONStringWithConversions)jsonValue).getDataConversions());
	}

	protected Object getValueForToJSON(Object dpValue, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		Object jsonValueRepresentation;
		if (findMode)
		{
			// in UI show only strings in find mode (just like SC/WC do); if they are something else like real dates/numbers which could happen
			// from scripting, then show string representation
			jsonValueRepresentation = dpValue instanceof String ? dpValue : (dpValue != null ? String.valueOf(dpValue) : "");
		}
		else if (typeOfDP != null)
		{
			EmbeddableJSONWriter ejw = new EmbeddableJSONWriter(true); // that 'true' is a workaround for allowing directly a value instead of object or array
			DataConversion jsonDataConversion = new DataConversion();
			FullValueToJSONConverter.INSTANCE.toJSONValue(ejw, null, dpValue, typeOfDP, jsonDataConversion, dataConverterContext);
			if (jsonDataConversion.getConversions().size() == 0) jsonDataConversion = null;
			String str = ejw.toJSONString();
			if (str == null || str.trim().length() == 0)
			{
				Debug.error("A dataprovider that is not able to send itself to client... (" + typeOfDP + ", " + dpValue + ")");
				str = "null";
			}
			jsonValueRepresentation = new JSONStringWithConversions(str, jsonDataConversion);
		}
		else
		{
			jsonValueRepresentation = dpValue;
		}
		return jsonValueRepresentation;
	}

	public void browserUpdateReceived(Object newJSONValue, IBrowserConverterContext dataConverterContext)
	{
		Object oldValue = value;

		ValueReference<Boolean> serverSideValueIsNotTheSameAsClient = new ValueReference<>(Boolean.FALSE);
		if (!findMode && typeOfDP != null)
		{
			if (typeOfDP.getType() instanceof IPropertyConverterForBrowser< ? >)
			{
				value = ((IPropertyConverterForBrowser)typeOfDP.getType()).fromJSON(newJSONValue, value, typeOfDP, dataConverterContext,
					serverSideValueIsNotTheSameAsClient);
			}
			else value = newJSONValue;
		}
		else value = newJSONValue;

		if (oldValue != value && (oldValue == null || !oldValue.equals(value)))
		{
			jsonValue = null;
		}

		if (serverSideValueIsNotTheSameAsClient.value.booleanValue())
		{
			// if we detect that the new server value (it's representation on client) is no longer what the client has showing, we must update the client's value
			jsonValue = null;

			changeMonitor.valueChanged(); // value changed from client so why do we need this one might ask (client already has the value)?
			// because for example in a field an INTEGER dataprovider might be shown with format ##0.00 and if the user enters non-int value client side
			// the server will trunc/round to an INTEGER and then the client shows double value while the server DP has the int value (which are not the same)
		}
	}
}
