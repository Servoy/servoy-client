/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ISmartPropertyValue;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.FormAndTableDataProviderLookup;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.GlobalMethodValueList;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.IHasUnderlyingState;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.FormatPropertyType.FormatPropertyDependencies;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * As a format property can have for: valueslistProperty, dataproviderProperty, it needs to watch those properties for changes.<br/><br/>
 *
 * When setting the format property from Rhino by setting an entire custom object property that contains it as subproperty (for example a 'column' in a table that contains format, dp and vl subproperties)
 * then we have no guarantee that the valuelist and dataprovider properties are already set before FormatProperty type during Rhino-to-Sablo conversion (they will be converted and set 1-by-1). So we have
 * to use {@link ISmartPropertyValue#attachToBaseObject(IChangeListener, IWebObjectContext)} and listeners to properly initialize the format when the "for" properties are already processed or change.
 *
 * @author acostescu
 */
public class FormatTypeSabloValue implements ISmartPropertyValue, IHasUnderlyingState, PropertyChangeListener, IChangeListener
{

	protected List<IChangeListener> underlyingValueChangeListeners = new ArrayList<>();

	private boolean initialized;

	// values that we have from the start/even before initialization (if it is not initialized from the start)
	private final String formatDesignValue;
	private final FormatPropertyDependencies propertyDependencies;

	// values available after the ValueListTypeSabloValue initialization was completed
	private IChangeListener changeMonitor;
	private IWebObjectContext webObjectContext;
	private String dataproviderID;
	private Object valuelistID;
	private String foundsetID;

	private ComponentFormat componentFormat;

	/**
	 * Creates a new FormatTypeSabloValue that is aware of any changes to it's "for" properties (if it is for a dataprovider or valuelist property type in .spec).<br/>
	 * It will be initialized (compute it's ComponentFormat) once it has everything it needs.
	 *
	 * @param formatDesignValue the format string value as specified at design-time or set from Rhino.
	 * @param propertyDependencies if the spec declares a for: dataproviderProperty or for: [valuelistProperty, dataproviderPropety]
	 */
	public FormatTypeSabloValue(String formatDesignValue, FormatPropertyDependencies propertyDependencies)
	{
		this.formatDesignValue = formatDesignValue;
		this.propertyDependencies = propertyDependencies;

		initialized = false;
	}

	/**
	 * Creates a new already-initialized format sablo value; it was initialized probably based on form element values right away in form-element-to-sablo conversion, that is why we
	 * don't have to wait for {@link #attachToBaseObject(IChangeListener, IWebObjectContext)} to initialize it. FormElement had all he necessary data in it.
	 */
	public FormatTypeSabloValue(String formatDesignValue, FormatPropertyDependencies propertyDependencies, String dataproviderID, Object valuelistID,
		String foundsetID, IWebObjectContext webObjectContext)
	{
		this.formatDesignValue = formatDesignValue;
		this.propertyDependencies = propertyDependencies;
		this.webObjectContext = webObjectContext;

		this.initialized = true;
		this.dataproviderID = dataproviderID;
		this.valuelistID = valuelistID;
		this.foundsetID = foundsetID;

		this.componentFormat = getSabloValue(formatDesignValue, dataproviderID, valuelistID, foundsetID, webObjectContext);

		initialized = true;
	}

	public ComponentFormat getComponentFormat()
	{
		return componentFormat;
	}

	public String getFormatDesignValue()
	{
		return formatDesignValue;
	}

	@Override
	public void attachToBaseObject(IChangeListener monitor, IWebObjectContext webObjectCntxt)
	{
		this.changeMonitor = monitor;
		this.webObjectContext = webObjectCntxt;

		if (propertyDependencies.dataproviderPropertyName != null)
			webObjectContext.addPropertyChangeListener(propertyDependencies.dataproviderPropertyName, this);
		if (propertyDependencies.valueListPropertyName != null) webObjectContext.addPropertyChangeListener(propertyDependencies.valueListPropertyName, this);
		if (propertyDependencies.foundsetPropertyName != null) webObjectContext.addPropertyChangeListener(propertyDependencies.foundsetPropertyName, this); // this I think is not really needed... can foundsetID ever change? even from R

		initializeIfPossibleAndNeeded(); // adds more listeners if needed (for example for underlying sablo value of a foundset linked value)
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		// a sablo value that we are depending upon in changed
		initializeIfPossibleAndNeeded();
	}

	@Override
	public void valueChanged()
	{
		// the underlying sablo value of the foundset linked dataprovider value that we are interested in changed
		initializeIfPossibleAndNeeded();
	}

	private void initializeIfPossibleAndNeeded()
	{
		// some dependent property has changed

		// get the new values
		String newDataproviderID = null;
		Object newValuelistID = null;
		String newFoundsetID = null;

		if (propertyDependencies.valueListPropertyName != null)
		{
			ValueListTypeSabloValue valuelistSabloValue = (ValueListTypeSabloValue)webObjectContext.getProperty(propertyDependencies.valueListPropertyName);
			if (valuelistSabloValue != null) newValuelistID = valuelistSabloValue.getValuelistIdentifier();
		}
		if (propertyDependencies.dataproviderPropertyName != null)
		{
			Object dataproviderValue = webObjectContext.getProperty(propertyDependencies.dataproviderPropertyName);
			if (dataproviderValue instanceof IHasUnderlyingState)
			{
				((IHasUnderlyingState)dataproviderValue).addStateChangeListener(this); // this won't add it twice if it's already added (see javadoc of this call)
			}

			newDataproviderID = DataAdapterList.getDataProviderID(dataproviderValue); // this will only return non-null if dataproviderValue != null && it is initialized (so foundset is already operational)
		}

		if (propertyDependencies.foundsetPropertyName != null)
		{
			FoundsetTypeSabloValue runtimeValOfFoundset = (FoundsetTypeSabloValue)webObjectContext.getProperty(propertyDependencies.foundsetPropertyName);
			if (runtimeValOfFoundset != null)
			{
				newFoundsetID = runtimeValOfFoundset.getFoundsetSelector();
				if (newFoundsetID == null && runtimeValOfFoundset.getFoundset() != null)
				{
					// set from Rhino (then foundsetSelector/ID is null); take the datasource from the foundset itself; datasources are valid "foundsetIDs/foundsetSelectors"
					newFoundsetID = runtimeValOfFoundset.getFoundset().getDataSource();
				}
			}
		}

		// see if anything we are interested in changed +
		// format value always has what it needs the first time attachToBaseObject is called; so if componentFormat is null we must initialize
		// it even if dataProvider or valueList are null (maybe they are really not set or not present)
		if (!Utils.stringSafeEquals(newDataproviderID, dataproviderID) || !Utils.safeEquals(valuelistID, newValuelistID) ||
			!Utils.stringSafeEquals(newFoundsetID, foundsetID) || componentFormat == null)
		{
			// so something did change or we should do first initialization
			dataproviderID = newDataproviderID;
			valuelistID = newValuelistID;
			foundsetID = newFoundsetID;

			componentFormat = getSabloValue(formatDesignValue, dataproviderID, valuelistID, foundsetID, webObjectContext);

			if (changeMonitor != null) changeMonitor.valueChanged();
		}

		// a detach/attach (for ex. a table column that has a format was removed then re-added) could result in everything staying the same but initialize == false;
		// then on attach we just need to mark it as 'initialized' again
		if (componentFormat != null) initialized = true;
	}

	public boolean isInitialized()
	{
		return initialized;
	}

	public void detach()
	{
		if (propertyDependencies.dataproviderPropertyName != null)
		{
			webObjectContext.removePropertyChangeListener(propertyDependencies.dataproviderPropertyName, this);

			Object dataproviderValue = webObjectContext.getProperty(propertyDependencies.dataproviderPropertyName);
			if (dataproviderValue instanceof IHasUnderlyingState) ((IHasUnderlyingState)dataproviderValue).removeStateChangeListener(this);
		}
		if (propertyDependencies.valueListPropertyName != null) webObjectContext.removePropertyChangeListener(propertyDependencies.valueListPropertyName, this);

		this.changeMonitor = null;
		webObjectContext = null;

		initialized = false;
	}

	protected ComponentFormat getSabloValue(String formatValue, String dataproviderId, Object valuelistId, String foundsetId, IWebObjectContext webObjectCntxt)
	{
		INGApplication application = ((WebFormComponent)webObjectCntxt.getUnderlyingWebObject()).getDataConverterContext().getApplication();
		IDataProviderLookup dataProviderLookup = null;

		// IMPORTANT: here we iterate over the for: configs
		//
		// if you have for: [valuelist, dataprovider] then 2 things can happen:
		// - valuelist if it has both real and display values - forces the type; it is either TEXT (custom vl., global method vl.) or the 'display' column type in case it's a DB valuelist
		// - valuelist if not real/display but only one kind of values: here it is required in docs in the spec file that the valuelist property also defines "for": dataprovider if format
		//   defines both "for" valuelist and dataprovider => valuelist doesn't force the type and then the dataprovider will decide the type
		//
		// if you have just for: dataprovider the the dataprovider property determines the type
		// if you have just for: valuelist (TODO) - this is currently not properly supported - as here we should get the type always from the VL (for both display and real values) - as we don't have a dataprovider to fall back on

		if (valuelistId != null)
		{
			// if we have a "for" valuelist, see if this valuelist forces the format type due to display values (when they are separate from real values)
			// otherwise it will do nothing and loop/fallback to the other if clause below which checks the "for" dataprovider
			ValueList valuelistPersist = ValueListTypeSabloValue.getValuelistPersist(valuelistId, application);

			if (valuelistPersist != null)
			{
				IDataProvider dataProvider = null;
				ITable table;
				try
				{
					if (valuelistPersist.getRelationName() != null)
					{
						Relation[] relations = application.getFlattenedSolution().getRelationSequence(valuelistPersist.getRelationName());
						table = application.getFlattenedSolution().getTable(relations[relations.length - 1].getForeignDataSource());
					}
					else
					{
						table = application.getFlattenedSolution().getTable(valuelistPersist.getDataSource());
					}

					if (table != null)
					{
						// if the format is for a table valuelist - the type to be used is the one of the dp chosen as 'display' in the valuelist
						String dp = null;
						int showDataProviders = valuelistPersist.getShowDataProviders(); // if show == real then we can use show anyway cause there is only one value for both real and display; if show != real we care about show

						if ((showDataProviders & 1) != 0)
						{
							dp = valuelistPersist.getDataProviderID1();
						}

						if ((showDataProviders & 2) != 0)
						{
							if (dp != null) return ComponentFormat.getComponentFormat(formatValue, IColumnTypes.TEXT, application); // display value is a concat of multiple columns, so a string; not even sure if format property makes sense, but it is for a String then
							dp = valuelistPersist.getDataProviderID2();
						}

						if ((showDataProviders & 4) != 0)
						{
							if (dp != null) return ComponentFormat.getComponentFormat(formatValue, IColumnTypes.TEXT, application); // display value is a concat of multiple columns, so a string; not even sure if format property makes sense, but it is for a String then
							dp = valuelistPersist.getDataProviderID3();
						}

						if (dp != null)
						{
							dataProvider = application.getFlattenedSolution().getDataProviderForTable(table, dp);
						}
						return ComponentFormat.getComponentFormat(formatValue, dataProvider, application, true);
					}
					else if (valuelistPersist.getValueListType() == IValueListConstants.CUSTOM_VALUES)
					{
						IValueList realValuelist = com.servoy.j2db.component.ComponentFactory.getRealValueList(application, valuelistPersist, true, Types.OTHER,
							null, null, true);
						if (realValuelist.hasRealValues())
						{
							// if custom vl has both real and display values, the display values are TEXT (format is for those)
							return ComponentFormat.getComponentFormat(formatValue, IColumnTypes.TEXT, application);
						}
					}
					else if (valuelistPersist.getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES)
					{
						PropertyDescription vlPD = webObjectCntxt.getPropertyDescription(propertyDependencies.valueListPropertyName);
						boolean lazyLoad = valuelistPersist.getLazyLoading() && vlPD.getConfig() instanceof ValueListConfig &&
							((ValueListConfig)vlPD.getConfig()).getLazyLoading();
						if (!lazyLoad)
						{
							IValueList realValuelist = com.servoy.j2db.component.ComponentFactory.getRealValueList(application, valuelistPersist, true,
								Types.OTHER, null, null, true);
							if (realValuelist instanceof GlobalMethodValueList)
							{
								((GlobalMethodValueList)realValuelist).fill(null, "", null);
								if (realValuelist.hasRealValues())
								{
									// if global method vl has both real and display values, it seems that the display values are always TEXT (format is for those)
									return ComponentFormat.getComponentFormat(formatValue, IColumnTypes.TEXT, application);
								}
							}
						}
					}
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
			// here - we want to fall back to the dataprovider if available in for: [ ..., dataprovider] if valuelist didn't force a certain display type on the format
		}

		if (dataproviderId != null && foundsetId != null)
		{
			Form form = ((IContextProvider)webObjectCntxt.getUnderlyingWebObject()).getDataConverterContext().getForm().getForm();
			ITable table = FoundsetTypeSabloValue.getTableBasedOfFoundsetPropertyFromFoundsetIdentifier(foundsetId, application, form);

			if (table != null)
			{
				dataProviderLookup = new FormAndTableDataProviderLookup(application.getFlattenedSolution(), form, table);
			} // else it will be searched for in form's context and table as below
		} // else there is no "for DP or it's just a normal DP, not foundset-linked; see below - it will search for it's type using the form's dataproviderLookup

		if (dataProviderLookup == null && application != null)
			dataProviderLookup = application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(),
				((IContextProvider)webObjectCntxt.getUnderlyingWebObject()).getDataConverterContext().getForm().getForm());

		ComponentFormat format = ComponentFormat.getComponentFormat(formatValue, dataproviderId, dataProviderLookup, application, true);

		return format;
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

	protected void resetI18nValue()
	{
		// the current language of the client probably changed; make sure to refresh i18n key formats
		if (formatDesignValue != null && formatDesignValue.startsWith("i18n:"))
		{
			componentFormat = null; // dump old format
			initializeIfPossibleAndNeeded(); // reinitialize if everything is available
		}
	}

	@Override
	public String toString()
	{
		return "FormatTypeSabloValue: " +
			(componentFormat != null && componentFormat.parsedFormat != null ? componentFormat.parsedFormat.toFormatProperty() : componentFormat);
	}

}
