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

import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.DatePropertyType;
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
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.dataprocessing.FindState;
import com.servoy.j2db.dataprocessing.FoundSetEvent;
import com.servoy.j2db.dataprocessing.IFoundSetEventListener;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetDataAdapterList;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;
import com.servoy.j2db.server.ngclient.property.IFindModeAwarePropertyValue;
import com.servoy.j2db.server.ngclient.property.INGWebObjectContext;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Runtime value stored in WebFormComponents for properties of type {@link DataproviderPropertyType}.
 * Handles any needed listeners and deals with to and from browser communications, find mode, ....
 *
 * @author acostescu
 */
public class DataproviderTypeSabloValue implements IDataLinkedPropertyValue, IFindModeAwarePropertyValue
{

	private static final String TAG_TYPE_NAME = "typeName";

	protected final String dataProviderID;

	protected IDataAdapterList dataAdapterList;
	protected final IServoyDataConverterContext servoyDataConverterContext;

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
	private List<IFoundSetInternal> relatedFoundsets = new ArrayList<IFoundSetInternal>();
	private IModificationListener relatedRecordModificationListener;
	private List<IRecordInternal> relatedRecords = new ArrayList<IRecordInternal>();
	private String relationName;
	private IWebObjectContext webObjectContext;
	private String shouldResolveFromValuelistWithName;

	public DataproviderTypeSabloValue(String dataProviderID, IDataAdapterList dataAdapterList, IServoyDataConverterContext servoyDataConverterContext,
		PropertyDescription dpPD)
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
		this.servoyDataConverterContext = servoyDataConverterContext;
		this.dpPD = dpPD;
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
	public void attachToBaseObject(IChangeListener changeNotifier, IWebObjectContext webObjectCntxt)
	{
		this.changeMonitor = changeNotifier;
		this.webObjectContext = webObjectCntxt;
		if (webObjectCntxt instanceof INGWebObjectContext) this.dataAdapterList = ((INGWebObjectContext)webObjectCntxt).getDataAdapterList();
		computeShouldResolveValuelistConfig();
		// register data link and find mode listeners as needed
		dataLinks = ((DataproviderPropertyType)dpPD.getType()).getDataLinks(dataProviderID,
			servoyDataConverterContext.getForm() != null ? servoyDataConverterContext.getForm().getForm() : null);
		dataAdapterList.addDataLinkedProperty(this, dataLinks);

		// they weren't cached in form element; get them again
		boolean isFindModeAware = ((DataproviderPropertyType)dpPD.getType()).isFindModeAware(dataProviderID,
			servoyDataConverterContext.getForm() != null ? servoyDataConverterContext.getForm().getForm() : null);
		if (isFindModeAware) dataAdapterList.addFindModeAwareProperty(this);

		DataproviderConfig config = (DataproviderConfig)dpPD.getConfig();
		String dtpn = config.getDisplayTagsPropertyName();
		Object dtPropVal = null;
		if (dtpn != null)
		{
			dtPropVal = webObjectCntxt.getProperty(dtpn);
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
		relatedFoundsets = Collections.emptyList();

		if (relatedRecordModificationListener != null)
		{
			for (IRecordInternal relatedRecord : relatedRecords)
			{
				relatedRecord.removeModificationListener(relatedRecordModificationListener);
			}
		}
		relatedRecords = Collections.emptyList();

		webObjectContext = null;
		changeMonitor = null;
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
		Collection<PropertyDescription> properties = webObjectContext.getProperties(TypesRegistry.getType(FormatPropertyType.TYPE_NAME));

		for (PropertyDescription formatPd : properties)
		{
			// see whether format if "for" this property (dataprovider)
			Object formatConfig = formatPd.getConfig();
			if (formatConfig instanceof String[] && Arrays.asList((String[])formatConfig).indexOf(dpPD.getName()) != -1)
			{
				INGApplication application = servoyDataConverterContext.getApplication();
				FormatTypeSabloValue formatSabloValue = (FormatTypeSabloValue)webObjectContext.getProperty(formatPd.getName());
				if (formatSabloValue != null && formatSabloValue.getFormatDesignValue() != null)
				{
					fieldFormat = ComponentFormat.getComponentFormat(formatSabloValue.getFormatDesignValue(), dataProviderID, dpLookup, application);
					break;
				}
			}
		}
		if (fieldFormat != null)
		{
			typeOfDP = NGUtils.getDataProviderPropertyDescription(fieldFormat.uiType, getDataProviderConfig().hasParseHtml(),
				fieldFormat.parsedFormat.useLocalDateTime());
			if (record instanceof FindState)
			{
				((FindState)record).setFormat(dataProviderID, fieldFormat.parsedFormat);
			}
		}
		else
		{
			// see type of dataprovider; this is done only once - first time we get a new record
			typeOfDP = NGUtils.getDataProviderPropertyDescription(dataProviderID, servoyDataConverterContext.getApplication(),
				servoyDataConverterContext.getForm().getForm(), record != null ? record.getParentFoundSet().getTable() : null,
				getDataProviderConfig().hasParseHtml(), false);
		}
		if (dpPD.hasTag(TAG_TYPE_NAME))
		{
			IPropertyType< ? > specType = TypesRegistry.getType((String)dpPD.getTag(TAG_TYPE_NAME));
			if (specType != null && (typeOfDP == null || !specType.getClass().isAssignableFrom(typeOfDP.getClass())))
			{
				typeOfDP = new PropertyDescriptionBuilder().withName("Spec type hint").withType(specType).build();
			}
		}

		String dpID = dataProviderID;
		IDataProvider dp = null;
		if (dpLookup != null)
		{
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
		if (globalRelationName != null)
		{
			try
			{
				IFoundSetInternal newRelatedFoundset = servoyDataConverterContext.getApplication().getFoundSetManager().getGlobalRelatedFoundSet(
					globalRelationName);
				if (newRelatedFoundset != globalRelatedFoundset)
				{
					if (globalRelatedFoundsetListener == null)
					{
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
					else if (globalRelatedFoundset != null)
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
					IDataProvider column = dp;
					if (column instanceof ColumnWrapper)
					{
						column = ((ColumnWrapper)column).getColumn();
					}
					boolean isAggregate = (column instanceof IColumn) ? ((IColumn)column).isAggregate() : false;

					if (isAggregate && relatedFoundsets.size() > 0)
					{
						relatedFoundsets.get(relatedFoundsets.size() - 1).removeAggregateModificationListener(relatedRecordModificationListener);
					}
					for (IFoundSetInternal relatedFoundset : relatedFoundsets)
					{
						((ISwingFoundSet)relatedFoundset).getSelectionModel().removeListSelectionListener(relatedFoundsetSelectionListener);
					}

					relatedFoundsets = newRelatedFoundsets;

					for (IFoundSetInternal relatedFoundset : relatedFoundsets)
					{
						((ISwingFoundSet)relatedFoundset).getSelectionModel().addListSelectionListener(relatedFoundsetSelectionListener);
					}

					if (isAggregate && relatedFoundsets.size() > 0)
					{
						relatedFoundsets.get(relatedFoundsets.size() - 1).addAggregateModificationListener(relatedRecordModificationListener);
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


		Object v = com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(record, servoyDataConverterContext.getForm().getFormScope(), dpID);
		if (v == Scriptable.NOT_FOUND) v = null;

		if (fieldFormat != null && !findMode)
		{
			v = ComponentFormat.applyUIConverterToObject(v, dataProviderID, servoyDataConverterContext.getApplication().getFoundSetManager(), fieldFormat);
		}

		v = replaceTagsIfNeeded(v);
		boolean changed = ((v != value) && (v == null || !v.equals(value)));

		value = v;
		if (changed)
		{
			jsonValue = null;
		}
		if (fireChangeEvent && (changed || dataAdapterList instanceof FoundsetDataAdapterList)) // if it is a foundset related DAL then always call valuechanged (the value can be of a previous row)
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
		if (value instanceof DbIdentValue)
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

	private void computeShouldResolveValuelistConfig()
	{
		shouldResolveFromValuelistWithName = null;
		if (webObjectContext != null && getDataProviderConfig() != null && getDataProviderConfig().shouldResolveValuelist())
		{
			Collection<PropertyDescription> properties = webObjectContext.getProperties(ValueListPropertyType.INSTANCE);
			for (PropertyDescription valuelistPD : properties)
			{
				Object config = valuelistPD.getConfig();
				if (config instanceof FoundsetLinkedConfig) config = ((FoundsetLinkedConfig)config).getWrappedConfig();
				if (config instanceof ValueListConfig && Utils.equalObjects(((ValueListConfig)config).getFor(), dpPD.getName()))
				{
					shouldResolveFromValuelistWithName = valuelistPD.getName();
					break;
				}
			}
		}
	}

	protected Object getValueForToJSON(Object dpValue, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		Object jsonValueRepresentation;
		boolean valuelistDisplayValue = false;
		if (shouldResolveFromValuelistWithName != null)
		{
			ValueListTypeSabloValue valuelistSabloValue = (ValueListTypeSabloValue)FoundsetLinkedTypeSabloValue.unwrapIfNeeded(
				webObjectContext.getProperty(shouldResolveFromValuelistWithName));
			if (valuelistSabloValue != null && valuelistSabloValue.getValueList() != null)
			{
				valuelistDisplayValue = true;
				if (valuelistSabloValue.getValueList().realValueIndexOf(dpValue) != -1)
				{
					try
					{
						dpValue = valuelistSabloValue.getValueList().getElementAt(valuelistSabloValue.getValueList().realValueIndexOf(dpValue));
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
				}
				else if (valuelistSabloValue.getValueList() instanceof DBValueList)
				{
					try
					{
						LookupValueList lookup = new LookupValueList(valuelistSabloValue.getValueList().getValueList(), dataAdapterList.getApplication(),
							ComponentFactory.getFallbackValueList(dataAdapterList.getApplication(), null, Types.OTHER, null,
								valuelistSabloValue.getValueList().getValueList()),
							null);
						if (lookup.realValueIndexOf(dpValue) != -1)
						{
							dpValue = lookup.getElementAt(lookup.realValueIndexOf(dpValue));
						}
						lookup.deregister();
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
				}
			}
		}

		if (findMode)
		{
			// in UI show only strings in find mode (just like SC/WC do); if they are something else like real dates/numbers which could happen
			// from scripting, then show string representation
			jsonValueRepresentation = dpValue instanceof String ? dpValue : (dpValue != null ? String.valueOf(dpValue) : "");
		}
		else if (typeOfDP != null && !valuelistDisplayValue)
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
			// if valuelistDisplayValue just use the value from valuelist with no conversion ?
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
			if (typeOfDP.getType() instanceof DatePropertyType && fieldFormat != null && fieldFormat.parsedFormat != null &&
				fieldFormat.parsedFormat.getDisplayFormat() != null && oldValue instanceof Date)
			{
				// if we have format with no hour, skip converting the date from the client and merge it with the old date
				String displayFormat = fieldFormat.parsedFormat.getDisplayFormat();
				boolean displayWithNoHour = displayFormat.indexOf('h') == -1 && displayFormat.indexOf('H') == -1;
				Date newValue = NGDatePropertyType.NG_INSTANCE.fromJSON(newJSONValue, displayWithNoHour || NGDatePropertyType.hasNoDateConversion(typeOfDP));
				if (newValue != null)
				{
					try
					{
						Locale locale = dataAdapterList.getApplication().getLocale();
						if (locale == null) locale = Locale.getDefault(Locale.Category.FORMAT);
						StateFullSimpleDateFormat formatter = new StateFullSimpleDateFormat(fieldFormat.parsedFormat.getDisplayFormat(), null, locale, true);
						formatter.setOriginal((Date)oldValue);
						formatter.parseObject(new SimpleDateFormat(fieldFormat.parsedFormat.getDisplayFormat(), locale).format(newValue));
						value = formatter.getMergedDate();
					}
					catch (Exception ex)
					{
						Debug.error(ex);
						// just set the value to the newValue so we dont loose the input of the user (maybe only the other time part, like minutes)
						value = newValue;
					}
				}
				else value = newValue;
			}
			else if (typeOfDP.getType() instanceof IPropertyConverterForBrowser< ? >)
			{
				value = ((IPropertyConverterForBrowser)typeOfDP.getType()).fromJSON(newJSONValue, value, typeOfDP, dataConverterContext,
					serverSideValueIsNotTheSameAsClient);
			}
			else
			{
				value = newJSONValue;
			}
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

	@Override
	public String toString()
	{
		return "DP(" + dataProviderID + ")";
	}

}
