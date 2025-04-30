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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.json.JSONException;
import org.mozilla.javascript.Scriptable;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.IllegalChangeFromClientException;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.DatePropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithClientSideType;
import org.sablo.websocket.utils.JSONUtils.JSONStringWithClientSideType;

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
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetDataAdapterList;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;
import com.servoy.j2db.server.ngclient.property.IFindModeAwarePropertyValue;
import com.servoy.j2db.server.ngclient.property.IHasUnderlyingState;
import com.servoy.j2db.server.ngclient.property.NGComponentDALContext;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Runtime value stored in WebFormComponents for properties of type {@link DataproviderPropertyType}.
 * Handles any needed listeners and deals with to and from browser communications, find mode, ....
 *
 * @author acostescu
 */
public class DataproviderTypeSabloValue implements IDataLinkedPropertyValue, IFindModeAwarePropertyValue, IHasUnderlyingState
{

	private static final String TAG_TYPE_NAME = "typeName";
	private static final String TAG_MULTIVALUES = "multivalues";

	protected final String dataProviderID;

	protected IDataAdapterList dataAdapterList;
	protected final IServoyDataConverterContext servoyDataConverterContext;

	protected Object uiValue; // if this DP prop. uses an UI converter, this value is the one from UI (so the one that would result after converting it from record/scope value to UI value)

	protected IJSONStringWithClientSideType jsonValue;
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
	private String formatPdName;
	protected List<IChangeListener> underlyingValueChangeListeners = new ArrayList<>();

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
	 * Returns the value.
	 * In case this DP type uses an UIConverter it will return the non-UI value (so the one in the record)
	 */
	public Object getValue()
	{
		if (!findMode && fieldFormat != null)
		{
			// in case it has an UI converter, convert it from UI value into the record/scope value
			return ComponentFormat.applyUIConverterFromObject(uiValue, dataProviderID, servoyDataConverterContext.getApplication().getFoundSetManager(),
				fieldFormat);
		}
		else if (findMode && uiValue instanceof String && dpPD.getTag(TAG_MULTIVALUES) == Boolean.TRUE)
		{
			return Utils.getFindModeValueForMultipleValues(Arrays.asList(((String)uiValue).split("\\n")));
		}
		return uiValue; // ui value == record/scope value (no converter)
	}

	@Override
	public void attachToBaseObject(IChangeListener changeNotifier, IWebObjectContext webObjectCntxt)
	{
		this.changeMonitor = changeNotifier;
		this.webObjectContext = webObjectCntxt;

		IDataAdapterList newDal = NGComponentDALContext.getDataAdapterList(webObjectContext);
		if (newDal != null) this.dataAdapterList = newDal; // it will probably never be null

		computeShouldResolveValuelistConfig();
		if (webObjectCntxt != null) computeFormatPropertiesForThisDP();
		// register data link and find mode listeners as needed
		dataLinks = ((DataproviderPropertyType)dpPD.getType()).getDataLinks(dataProviderID,
			servoyDataConverterContext.getForm() != null ? servoyDataConverterContext.getForm().getForm() : null, servoyDataConverterContext.getSolution());
		dataAdapterList.addDataLinkedProperty(this, dataLinks);

		// they weren't cached in form element; get them again
		boolean isFindModeAware = ((DataproviderPropertyType)dpPD.getType()).isFindModeAware(dataProviderID,
			servoyDataConverterContext.getForm() != null ? servoyDataConverterContext.getForm().getForm() : null, servoyDataConverterContext.getSolution());
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
		if (webObjectContext == null) return; // it is already detached

		// unregister listeners
		dataAdapterList.removeDataLinkedProperty(this);
		dataAdapterList.removeFindModeAwareProperty(this);
		if (globalRelatedFoundset != null)
		{
			globalRelatedFoundset.removeFoundSetEventListener(globalRelatedFoundsetListener);
		}
		globalRelatedFoundset = null;
		globalRelatedFoundsetListener = null;

		if (relatedFoundsets.size() > 0 && relatedRecordModificationListener != null)
		{
			// just remove it, no need to test this because a remove will be just a NOP when it was not registered anyway.
			relatedFoundsets.get(relatedFoundsets.size() - 1).removeAggregateModificationListener(relatedRecordModificationListener);
		}
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
		shouldResolveFromValuelistWithName = null;
		formatPdName = null;

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

		FormatTypeSabloValue formatSabloValue = null;
		if (formatPdName != null)
		{
			formatSabloValue = (FormatTypeSabloValue)webObjectContext.getProperty(formatPdName);
			if (formatSabloValue != null)
			{
				fieldFormat = formatSabloValue.getComponentFormat();
			}
		}

		// if UI converter is set on the format, use it to get the dp type
		if (fieldFormat != null && fieldFormat.parsedFormat != null && fieldFormat.parsedFormat.getUIConverterName() != null)
		{
			typeOfDP = NGUtils.getDataProviderPropertyDescription(fieldFormat.uiType, getDataProviderConfig().hasParseHtml(),
				fieldFormat.parsedFormat.useLocalDateTime());
		}
		else
		{
			// see type of dataprovider; this is done only once - first time we get a new record
			typeOfDP = NGUtils.getDataProviderPropertyDescription(dataProviderID, servoyDataConverterContext.getApplication(),
				servoyDataConverterContext.getForm().getForm(), record != null ? record.getParentFoundSet().getTable() : null,
				getDataProviderConfig().hasParseHtml(),
				(formatSabloValue != null && formatSabloValue.getComponentFormat() != null)
					? formatSabloValue.getComponentFormat().parsedFormat.useLocalDateTime() : false);
		}
		if (fieldFormat != null && record instanceof FindState)
		{
			((FindState)record).setFormat(dataProviderID, fieldFormat.parsedFormat);
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

				boolean equals = testByReference(newRelatedFoundsets, this.relatedFoundsets);
				if (!equals)
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
				boolean equals = testByReference(newRelatedRecords, this.relatedRecords);
				if (!equals)
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
			// if it has an UI converter, transform it from the record/scope value into the UI value
			v = ComponentFormat.applyUIConverterToObject(v, dataProviderID, servoyDataConverterContext.getApplication().getFoundSetManager(), fieldFormat);
		}

		v = replaceTagsIfNeeded(v);
		// this is Utils.equalsObject() because it could be that the Row has a timestamp but the uivalue is just a date (representing the same date)
		boolean changed = ((v != uiValue) && (v == null || !Utils.equalObjects(uiValue, v)));

		uiValue = v;
		if (changed)
		{
			jsonValue = null;
		}
		if (fireChangeEvent && (changed || dataAdapterList instanceof FoundsetDataAdapterList)) // if it is a foundset related DAL then always call valuechanged (the value can be of a previous row)
		{
			changeMonitor.valueChanged();
		}
		if (changed)
		{
			fireUnderlyingPropertyChangeListeners();
		}
	}

	private boolean testByReference(List< ? > listA, List< ? > listB)
	{
		if (listA == null && listB != null) return false;
		if (listA != null && listB == null) return false;
		if (listA.size() != listB.size()) return false;

		for (int i = 0; i < listA.size(); i++)
		{
			if (listA.get(i) != listB.get(i)) return false;
		}
		return true;

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
			// remove links if the dataprovider value doesn't contain tags anymore
			dataAdapterList.removeDataLinkedProperty(this);
			dataAdapterList.addDataLinkedProperty(this, dataLinks);
			tagsDataProviders = null;
		}

		return result;
	}

	public IJSONStringWithClientSideType toJSON(IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (jsonValue == null)
		{
			jsonValue = getValueForToJSON(dataConverterContext);
		}
		return jsonValue;
	}

	private void computeFormatPropertiesForThisDP()
	{
		Collection<PropertyDescription> properties = webObjectContext.getProperties(TypesRegistry.getType(FormatPropertyType.TYPE_NAME));

		for (PropertyDescription formatPd : properties)
		{
			// see whether format if "for" this property (dataprovider)
			Object formatConfig = formatPd.getConfig();
			if (formatConfig instanceof String[] && Arrays.asList((String[])formatConfig).indexOf(dpPD.getName()) != -1)
			{
				formatPdName = formatPd.getName();
				break;
			}
		}
	}

	private void checkIfModifiable()
	{
		if (webObjectContext == null) return;
		Collection<PropertyDescription> properties = webObjectContext.getProperties(TypesRegistry.getType(ModifiablePropertyType.TYPE_NAME));

		for (PropertyDescription modifiable : properties)
		{
			// see whether format if "for" this property (dataprovider)
			Object config = modifiable.getConfig();
			if (dpPD.getName().equals(config))
			{
				// it is for our property
				Object property = webObjectContext.getProperty(modifiable.getName());
				if (property == null)
				{
					throw new IllegalChangeFromClientException(modifiable.getName(),
						"Property '" + dpPD.getName() + "' is blocked because of the modifiable property '" + modifiable.getName() +
							"' that blocks it because it has no value",
						webObjectContext.getUnderlyingWebObject().getName(), dpPD.getName(), true);
				}
			}
		}
		return;
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

	protected IJSONStringWithClientSideType getValueForToJSON(IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (uiValue instanceof DbIdentValue)
		{
			uiValue = ((DbIdentValue)uiValue).getPkValue();
		}

		IJSONStringWithClientSideType jsonValueRepresentation;
		boolean valuelistDisplayValue = false;
		int valuelistDisplayType = 0;
		if (shouldResolveFromValuelistWithName != null)
		{
			ValueListTypeSabloValue valuelistSabloValue = (ValueListTypeSabloValue)FoundsetLinkedTypeSabloValue.unwrapIfNeeded(
				webObjectContext.getProperty(shouldResolveFromValuelistWithName));
			if (valuelistSabloValue != null && valuelistSabloValue.getValueList() != null)
			{
				valuelistDisplayValue = true;
				valuelistDisplayType = valuelistSabloValue.getValueList().getValueList().getDisplayValueType();
				if (valuelistSabloValue.getValueList().realValueIndexOf(uiValue) != -1)
				{
					try
					{
						// TODO don't we have to apply the UI converter's toObject here as well in the unlikely case of a valuelist + UI converter? and also
						// when searching we should then use the fromObject(uiValue) rather then uiValue directly I think
						uiValue = valuelistSabloValue.getValueList().getElementAt(valuelistSabloValue.getValueList().realValueIndexOf(uiValue));
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
							null, dataAdapterList.getRecord());
						if (lookup.realValueIndexOf(uiValue) != -1)
						{
							// TODO don't we have to apply the UI converter's toObject here as well in the unlikely case of a valuelist + UI converter? and also
							// when searching we should then use the fromObject(uiValue) rather then uiValue directly I think
							uiValue = lookup.getElementAt(lookup.realValueIndexOf(uiValue));
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
			EmbeddableJSONWriter ejw = new EmbeddableJSONWriter(true); // that 'true' is a workaround for allowing directly a value instead of object or array
			ejw.value(uiValue instanceof String ? uiValue : (uiValue != null ? String.valueOf(uiValue) : ""));
			jsonValueRepresentation = new JSONStringWithClientSideType(ejw.toJSONString(), null);
		}
		else if (typeOfDP != null && !valuelistDisplayValue)
		{
			Object value = uiValue;
			// if the value to display is null, but it represents a count/avg/sum aggregate DP then
			// set it to 0, as it means that the foundset has no records, so count/avg/sum should show as 0;
			// merged this change from SC, DisplaysAdapter
			if (value == null && com.servoy.j2db.dataprocessing.DataAdapterList.isCountOrAvgOrSumAggregateDataProvider(dataProviderID,
				new FormAndTableDataProviderLookup(servoyDataConverterContext.getApplication().getFlattenedSolution(),
					servoyDataConverterContext.getForm().getForm(),
					dataAdapterList.getRecord() != null ? dataAdapterList.getRecord().getParentFoundSet().getTable() : null)))
				value = Integer.valueOf(0);

			jsonValueRepresentation = JSONUtils.FullValueToJSONConverter.INSTANCE.getConvertedValueWithClientType(value, typeOfDP, dataConverterContext,
				false);

			if (jsonValueRepresentation == null || jsonValueRepresentation.toJSONString() == null ||
				jsonValueRepresentation.toJSONString().trim().length() == 0)
			{
				Debug.error("A dataprovider that is not able to send itself to client... (" + typeOfDP + ", " + uiValue + ")");
				jsonValueRepresentation = new JSONStringWithClientSideType("null", null);
			}
		}
		else if (valuelistDisplayValue && (valuelistDisplayType == IColumnTypes.DATETIME ||
			valuelistDisplayType == IColumnTypes.INTEGER ||
			valuelistDisplayType == IColumnTypes.NUMBER ||
			valuelistDisplayType == IColumnTypes.MEDIA) && !(uiValue instanceof String)) // I think the !(uiValue instanceof String) is needed because sometimes (table based valuelist with table level column format on display DP) a format is already applied to the display value; TODO use always an unformatted display value and give via the format property the correct format (either column level for display DP or a specific one set on the property)
		{
			PropertyDescription pdToUse = null;


			// FIXME: don't we have to check more here when choosing the correct PropertyDescription for converting the display value?
			// for example what if this dataprovider has a format which says "use local date"? shouldn't we then choose NGUtils.LOCAL_DATE_DATAPROVIDER_CACHED_PD instead?
			// why don't we do here instead of all the ifs just
			// NGUtils.getDataProviderPropertyDescription(valuelistDisplayType, getDataProviderConfig().hasParseHtml(),
			//                                                fieldFormat != null ? fieldFormat.parsedFormat.useLocalDateTime() : false);
			if (valuelistDisplayType == IColumnTypes.DATETIME)
			{
				pdToUse = NGUtils.DATE_DATAPROVIDER_CACHED_PD;
			}
			else if (valuelistDisplayType == IColumnTypes.INTEGER)
			{
				pdToUse = NGUtils.INTEGER_DATAPROVIDER_CACHED_PD;
			}
			else if (valuelistDisplayType == IColumnTypes.NUMBER)
			{
				pdToUse = NGUtils.NUMBER_DATAPROVIDER_CACHED_PD;
			}
			else if (valuelistDisplayType == IColumnTypes.MEDIA)
			{
				pdToUse = NGUtils.MEDIA_PERMISIVE_DATAPROVIDER_NO_PARSE_HTML_CACHED_PD;
			}

			if (pdToUse == null)
			{
				Debug
					.error("A dataprovider with resolveValuelist that is not able to send itself to client... (" + valuelistDisplayType + ", " + uiValue + ")");
				jsonValueRepresentation = new JSONStringWithClientSideType("null");
			}
			else
			{
				jsonValueRepresentation = JSONUtils.FullValueToJSONConverter.INSTANCE.getConvertedValueWithClientType(uiValue, pdToUse, dataConverterContext,
					false);

				if (jsonValueRepresentation == null || jsonValueRepresentation.toJSONString() == null ||
					jsonValueRepresentation.toJSONString().trim().length() == 0)
				{
					Debug.error("A dataprovider (vl. display) that is not able to send itself to client... (" + typeOfDP + ", " + uiValue + ")");
					jsonValueRepresentation = new JSONStringWithClientSideType("null", null);
				}
			}
		}
		else
		{
			EmbeddableJSONWriter ejw = new EmbeddableJSONWriter(true); // that 'true' is a workaround for allowing directly a value instead of object or array
			ejw.value(uiValue);
			// if valuelistDisplayValue just use the value from valuelist with no conversion ?
			jsonValueRepresentation = new JSONStringWithClientSideType(ejw.toJSONString(), null);
		}
		return jsonValueRepresentation;
	}

	public void browserUpdateReceived(Object newJSONValue, IBrowserConverterContext dataConverterContext)
	{
		checkIfModifiable();
		Object oldUIValue = uiValue;

		ValueReference<Boolean> serverSideValueIsNotTheSameAsClient = new ValueReference<>(Boolean.FALSE);
		if (!findMode && typeOfDP != null)
		{
			if (typeOfDP.getType() instanceof DatePropertyType && fieldFormat != null && fieldFormat.parsedFormat != null &&
				newJSONValue != null && fieldFormat.parsedFormat.getDisplayFormat() != null && (oldUIValue instanceof Date || oldUIValue == null))
			{
				boolean hasNoDateConversion = NGDatePropertyType.hasNoDateConversion(typeOfDP);
				Date newValue = NGDatePropertyType.NG_INSTANCE.fromJSON(newJSONValue, hasNoDateConversion);
				if (oldUIValue != null)
				{
					String format = fieldFormat.parsedFormat.hasEditFormat() ? fieldFormat.parsedFormat.getEditFormat()
						: fieldFormat.parsedFormat.getDisplayFormat();
					SimpleDateFormat sdf = new SimpleDateFormat(format);
					if (!hasNoDateConversion) sdf.setTimeZone(dataAdapterList.getApplication().getTimeZone());

					try
					{
						String oldFormatted = sdf.format(oldUIValue);
						String newFormatted = sdf.format(newValue);
						// need to go back to the default time zone so it doesn't make it sudden 2 jan 1970 because of the
						// time zone difference between the default here and where it needs to go to.
						sdf.setTimeZone(TimeZone.getDefault());
						Date oldValueParsed = sdf.parse(oldFormatted);
						Date newValueParsed = sdf.parse(newFormatted);
						uiValue = new Date(((Date)oldUIValue).getTime() + (newValueParsed.getTime() - oldValueParsed.getTime()));
					}
					catch (ParseException e)
					{
						uiValue = newValue;
					}
				}
				else
				{
					uiValue = newValue;
				}
			}
			else if (typeOfDP.getType() instanceof IPropertyConverterForBrowser< ? >)
			{
				try
				{
					uiValue = ((IPropertyConverterForBrowser<Object>)typeOfDP.getType()).fromJSON(newJSONValue, uiValue, typeOfDP, dataConverterContext,
						serverSideValueIsNotTheSameAsClient);
				}
				catch (ClassCastException e)
				{
					// this can hapen if a find mode uiVaue keeps hanging
					uiValue = ((IPropertyConverterForBrowser<Object>)typeOfDP.getType()).fromJSON(newJSONValue, null, typeOfDP, dataConverterContext,
						serverSideValueIsNotTheSameAsClient);
				}
			}
			else
			{
				uiValue = newJSONValue;
			}
		}
		else uiValue = newJSONValue;

		if (oldUIValue != uiValue && (oldUIValue == null || !oldUIValue.equals(uiValue)))
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
			fireUnderlyingPropertyChangeListeners();
		}
	}

	public void checkValueForChanges(final IRecordInternal record)
	{
		Object v = com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(record, servoyDataConverterContext.getForm().getFormScope(), dataProviderID);
		if (v == Scriptable.NOT_FOUND) v = null;
		// check if ui converter would change the value (even if input would stay the same)
		if (fieldFormat != null && fieldFormat.parsedFormat != null && fieldFormat.parsedFormat.getUIConverterName() != null)
		{
			v = ComponentFormat.applyUIConverterToObject(v, dataProviderID, servoyDataConverterContext.getApplication().getFoundSetManager(), fieldFormat);
		}
		if (v != uiValue && (v == null || !Utils.equalObjects(v, uiValue)))
		{
			uiValue = v;
			// if we detect that the new server value (it's representation on client) is no longer what the client has showing, we must update the client's value
			jsonValue = null;

			changeMonitor.valueChanged(); // value changed from client so why do we need this one might ask (client already has the value)?
			// because for example in a field an INTEGER dataprovider might be shown with format ##0.00 and if the user enters non-int value client side
			// the server will trunc/round to an INTEGER and then the client shows double value while the server DP has the int value (which are not the same)
		}
	}

	@Override
	public void addStateChangeListener(IChangeListener valueChangeListener)
	{
		if (!underlyingValueChangeListeners.contains(valueChangeListener)) underlyingValueChangeListeners.add(valueChangeListener);
	}

	@Override
	public void removeStateChangeListener(IChangeListener valueChangeListener)
	{
		underlyingValueChangeListeners.remove(valueChangeListener);
	}

	protected void fireUnderlyingPropertyChangeListeners()
	{
		if (underlyingValueChangeListeners.size() > 0)
		{
			// just in case any listeners will end up trying to alter underlyingValueChangeListeners - avoid a ConcurrentModificationException
			IChangeListener[] copyOfListeners = underlyingValueChangeListeners.toArray(new IChangeListener[underlyingValueChangeListeners.size()]);
			for (IChangeListener l : copyOfListeners)
			{
				l.valueChanged();
			}
		}
	}

	@Override
	public String toString()
	{
		return "DP(" + dataProviderID + ")";
	}

}