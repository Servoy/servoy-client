/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.persistence;


import java.util.ArrayList;
import java.util.List;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * A <b>value list</b> is an object from the data layer that is used by the UI (components) to provide selection lists and translations of
 * internal values to display values.<br/>
 * Value lists can be static, meaning their contents do not change, or dynamic, where they can be updated or modified over time.<br/><br/>
 *
 * They can be used for example in components such as dropdown menus, radio button groups or checkbox lists.
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, typeCode = IRepository.VALUELISTS)
@ServoyClientSupport(mc = true, wc = true, sc = true)
public class ValueList extends AbstractBase
	implements IValueListConstants, ISupportUpdateableName, ISupportContentEquals, ISupportEncapsulation, ICloneable, ISupportDeprecated
{

	private static final long serialVersionUID = 1L;


	/**
	 * Constructor I
	 */
	ValueList(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.VALUELISTS, parent, element_id, uuid);
	}

/*
 * _____________________________________________________________ Methods from this class
 */

//the repository element id can differ!
	public boolean contentEquals(Object obj)
	{
		if (obj instanceof ValueList)
		{
			ValueList other = (ValueList)obj;
			return (("" + getName()).equals(other.getName()) && //$NON-NLS-1$
				getValueListType() == other.getValueListType() && getValueListType() == CUSTOM_VALUES &&
				("" + getCustomValues()).equals(other.getCustomValues()) //$NON-NLS-1$
			);
		}
		return false;
	}


	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		validator.checkName(arg, getID(), new ValidatorSearchContext(IRepository.VALUELISTS), false);
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	/**
	 * Set the name
	 *
	 * @param arg the name
	 */
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMValueList#getName()
	 */
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * Set the valueListType
	 *
	 * @param arg the valueListType
	 */
	public void setValueListType(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_VALUELISTTYPE, arg);
	}

	/**
	 * The type of the valuelist.<br/>
	 * Can be one of:
	 * <ul>
	 *   <li>custom values</li>
	 *   <li>global method</li>
	 *   <li>database values</li>
	 *     <ul>
	 *       <li>table based</li>
	 *       <li>relation based</li>
	 *     </ul>
	 * </ul>
	 */
	public int getValueListType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_VALUELISTTYPE).intValue();
	}

	public int getDatabaseValuesType()
	{
		return (getRelationName() == null ? TABLE_VALUES : RELATED_VALUES);
	}


	/**
	 * Sets the relationName.
	 *
	 * @param relationName The relationName to set
	 */
	public void setRelationName(String relName)
	{
		String arg = relName;
		String relationName = getRelationName();
		if (arg != null && relationName != null && relationName.startsWith(".")) //$NON-NLS-1$
		{
			arg = arg + relationName; // relationName was used to store deprecated value of nmrelation
		}
		setTypedProperty(StaticContentSpecLoader.PROPERTY_RELATIONNAME, arg);
	}

	/**
	 * The name of the relation that is used for loading data from the database.
	 *
	 * @sample "orders_to_order_details"
	 */
	public String getRelationName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_RELATIONNAME);
	}

	/**
	 * Sets the relationNMName.
	 *
	 * @deprecated relationName supports multiple levels relations
	 *
	 * @param relationName The relationName to set
	 */
	@Deprecated
	public void setRelationNMName(String arg)
	{
		if (arg != null)
		{
			String relationName = getRelationName();
			if (relationName == null)
			{
				setRelationName('.' + arg);
			}
			else
			{
				int dot = relationName.lastIndexOf('.');
				if (dot > 0)
				{
					setRelationName(relationName.substring(0, dot + 1) + arg);
				}
				else
				{
					setRelationName(relationName + '.' + arg);
				}
			}
		}
	}

	/**
	 * Get the relationNMName
	 *
	 * @deprecated relationName supports multiple levels relations
	 */
	@Deprecated
	public String getRelationNMName()
	{
		String relationName = getRelationName();
		if (relationName != null)
		{
			int dot = relationName.lastIndexOf('.');
			if (dot > 0)
			{
				return relationName.substring(dot + 1);
			}
		}
		return null;
	}

	/**
	 * A string with the elements in the valuelist. The elements can be separated by line feeds:<br/>
	 * custom1<br/>
	 * custom2<br/><br/>
	 *
	 * Elements can optionally have real values:<br/>
	 * custom1|1<br/>
	 * custom2|2<br/><br/>
	 *
	 * The (custom) values can be given via a global scope method instead, if the valuelist type is of global method type...
	 *
	 * @sample
	 * Yes|1
	 * No|2
	 * Don't know|3
	 */
	public String getCustomValues()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_CUSTOMVALUES);
	}

	/**
	 * Sets the customValues.
	 *
	 * @param customValues The customValues to set
	 */
	public void setCustomValues(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CUSTOMVALUES, arg);
	}

	/**
	 * Compact representation of the names of the server and table that
	 * are used for loading the data from the database.
	 *
	 * @sample "example_data.customers"
	 */
	public String getDataSource()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE);
	}

	/**
	 * Set the data source
	 *
	 * @param arg the data source uri
	 */
	public void setDataSource(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE, arg);
	}

	/**
	 * The name of the database server that is used for loading the values when
	 * the value list has the type set to database/table values.
	 */
	public String getServerName()
	{
		String dataSource = getDataSource();
		if (dataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		if (stn != null)
		{
			return stn[0];
		}

		if (DataSourceUtils.getInmemDataSourceName(dataSource) != null)
		{
			return IServer.INMEM_SERVER;
		}
		// can return null if it is not a DB server/table combi
		return null;
	}

	/**
	 * Sets the serverName.
	 *
	 * @param serverName The serverName to set
	 */
	public void setServerName(String arg)
	{
		setDataSource(DataSourceUtils.createDBTableDataSource(arg, getTableName()));
	}

	/**
	 * The name of the database table that is used for loading the values when
	 * the value list has the type set to database/table values.
	 */
	public String getTableName()
	{
		String dataSource = getDataSource();
		if (dataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		if (stn != null)
		{
			return stn[1];
		}

		String inmemDataSourceName = DataSourceUtils.getInmemDataSourceName(dataSource);
		if (inmemDataSourceName != null)
		{
			return inmemDataSourceName;
		}
		// can return null when data source is not a server/table combi
		return null;
	}

	/**
	 * Table name property othat is set when a table valuelist is created
	 *
	 * @param tableName The tableName to set
	 */
	public void setTableName(String arg)
	{
		setDataSource(DataSourceUtils.createDBTableDataSource(getServerName(), arg));
	}

	/**
	 * This is the dataprovider selected in the first list of dataproviders, when a table or related valuelist is selected.<br/>
	 * This dataprovider can be marked as a display and/or a return value.
	 *
	 * @sample "companyname"
	 * @return Returns a String
	 */
	public String getDataProviderID1()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID1);
	}

	/**
	 * This is the dataprovider selected in the first list of dataproviders, when a table or related valuelist is selected.<br/>
	 * This dataprovider can be marked as a display and/or a return value.
	 *
	 * @param dataProviderID1 The dataProviderID1 to set
	 */
	public void setDataProviderID1(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID1, arg);
		dataProviderIDs = null;
	}

	/**
	 * This is the dataprovider selected in the second list of dataproviders, when a table or related valuelist is selected.<br/>
	 * This dataprovider can be marked as a display and/or a return value.
	 *
	 * @sample "customerid"
	 * @return Returns a String
	 */
	public String getDataProviderID2()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID2);
	}

	/**
	 * This is the dataprovider selected in the second list of dataproviders, when a table or related valuelist is selected.<br/>
	 * This dataprovider can be marked as a display and/or a return value.
	 *
	 * @param dataProviderID2 The dataProviderID2 to set
	 */
	public void setDataProviderID2(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID2, arg);
		dataProviderIDs = null;
	}

	/**
	 * This is the dataprovider selected in the third list of dataproviders, when a table or related valuelist is selected.<br/>
	 * This dataprovider can be marked as a display and/or a return value.
	 *
	 * @sample "contactname"
	 * @return Returns a String
	 */
	public String getDataProviderID3()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID3);
	}

	/**
	 * This is the dataprovider selected in the third list of dataproviders, when a table or related valuelist is selected.<br/>
	 * This dataprovider can be marked as a display and/or a return value.
	 *
	 * @param dataProviderID3 The dataProviderID3 to set
	 */
	public void setDataProviderID3(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID3, arg);
		dataProviderIDs = null;
	}

	/**
	 * A String representing the separator that should be used when multiple
	 * display dataproviders are set, when the value list has the type set to
	 * database values.
	 *
	 * @sample " / "
	 */
	public String getSeparator()
	{
		String separator = getTypedProperty(StaticContentSpecLoader.PROPERTY_SEPARATOR);
		if (separator == null) separator = " "; //$NON-NLS-1$
		return separator;

	}

	/**
	 * Sets the separator.
	 *
	 * @param separator The separator to set
	 */
	public void setSeparator(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SEPARATOR, arg);
	}

	/**
	 * Sort options that are applied when the valuelist loads its data
	 * from the database.
	 *
	 * @sample "companyname asc"
	 */
	public String getSortOptions()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SORTOPTIONS);
	}

	/**
	 * Sets the sortOptions.
	 *
	 * @param sortOptions The sortOptions to set
	 */
	public void setSortOptions(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SORTOPTIONS, arg);
	}

	/**
	 * Gets the showDataProvider.
	 *
	 * @return Returns a int
	 */
	public int getShowDataProviders()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWDATAPROVIDERS).intValue();
	}

	/**
	 * Sets the showDataProvider.
	 *
	 * @param showDataProvider The showDataProvider to set
	 */
	public void setShowDataProviders(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWDATAPROVIDERS, arg);
	}

	/**
	 * Which of the dataprovers should be used as the return dataprovider, so set back into the records dataprovider.
	 *
	 * @return Returns a int
	 */
	public int getReturnDataProviders()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_RETURNDATAPROVIDERS).intValue();
	}

	/**
	 * Sets the returnDataProviders.
	 *
	 * @param returnDataProviders The returnDataProviders to set
	 */
	public void setReturnDataProviders(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_RETURNDATAPROVIDERS, arg);
	}

	@Override
	public String toString()
	{
		return getName();
	}

	private transient String[] dataProviderIDs;

	public String[] getDataProviderIDs()
	{
		if (dataProviderIDs == null)
		{
			List<String> all = new ArrayList<String>();
			int total = (getShowDataProviders() | getReturnDataProviders());
			if ((total & 1) != 0)
			{
				all.add(getDataProviderID1());
			}
			if ((total & 2) != 0)
			{
				all.add(getDataProviderID2());
			}
			if ((total & 4) != 0)
			{
				all.add(getDataProviderID3());
			}
			dataProviderIDs = new String[all.size()];
			all.toArray(dataProviderIDs);
		}
		return dataProviderIDs;
	}

	/**
	 * Property that tells if an empty value must be shown next to the items in the value list.
	 */
	public int getAddEmptyValue()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ADDEMPTYVALUE).intValue();
	}

	/**
	 * Sets the addEmptyValue.
	 *
	 * @param addEmptyValue The addEmptyValue to set
	 */
	public void setAddEmptyValue(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ADDEMPTYVALUE, arg);
	}

	/**
	 * Flag that tells if the name of the valuelist should be applied as a filter on the
	 * 'valuelist_name' column when retrieving the data from the database.
	 */
	public boolean getUseTableFilter()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_USETABLEFILTER).booleanValue();
	}

	public void setUseTableFilter(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_USETABLEFILTER, arg);
	}

	/**
	 * If the valuelist only displays the "active" values that a user can choose, but you still need to be able to show also old values, then a fallback valuelist should be set.<br/>
	 * It works as a backup list for getting a display value for a real value that is not in this normal/main valuelist anymore.
	 *
	 * @sample "customers_and_company_name"
	 * @return the valueback valuelist id.
	 */
	public int getFallbackValueListID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FALLBACKVALUELISTID).intValue();
	}

	public void setFallbackValueListID(int id)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FALLBACKVALUELISTID, id);
	}

	@Override
	public void setEncapsulation(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ENCAPSULATION, arg);
	}

	/**
	 * The encapsulation mode of this Valuelist. The following can be used:<br/>
	 * <ul>
	 *   <li>Public (available in both scripting and designer from any module)</li>
	 *   <li>Module Scope - available in both scripting and designer but only in the same module.</li>
	 * </ul>
	 *
	 * @sample "Public"
	 * @return the encapsulation mode/level of the persist.
	 */
	@Override
	public int getEncapsulation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ENCAPSULATION).intValue();
	}

	/*
	 * @see com.servoy.j2db.persistence.ISupportDeprecated#getDeprecated()
	 */
	@Override
	public String getDeprecated()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DEPRECATED);
	}

	/*
	 * @see com.servoy.j2db.persistence.ISupportDeprecated#setDeprecated(String)
	 */
	@Override
	public void setDeprecated(String deprecatedInfo)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DEPRECATED, "".equals(deprecatedInfo) ? null : deprecatedInfo);
	}

	/**
	 * Set the displayValue type
	 *
	 * @param arg the displayValue type
	 */
	public void setDisplayValueType(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_VALUELIST_DISPLAYVALUE_TYPE, arg);
	}

	/**
	 * The type of the display value if this is a global or custom valuelist, servoy needs to know this for formatting purposes.
	 *
	 * @sample "TEXT"
	 */
	public int getDisplayValueType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_VALUELIST_DISPLAYVALUE_TYPE).intValue();
	}

	/**
	 * Set the realValue type
	 *
	 * @param arg the realValue type
	 */
	public void setRealValueType(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_VALUELIST_REALVALUE_TYPE, arg);
	}

	/**
	 * The type of the real value if this is a global or custom valuelist, servoy needs to know this so it can check if the types match on the record it is binded to.
	 *
	 * @sample "TEXT"
	 */
	public int getRealValueType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_VALUELIST_REALVALUE_TYPE).intValue();
	}

	/**
	 * A property, specific for NG & Titanium clients with GlobalValuelist - it helps to query/call the global valuelist method only when it is needed.<br/><br/>
	 * This flag has to be set both on the valuelist and in component specification (.spec file), on the valuelist property, in order for it to work correctly (so the
	 * developer decides if a valuelist provides this option, and the component decides if it uses this feature or not).<br/><br/>
	 *
	 * IMPORTANT: Usage of real & display values is not fully supported with lazy loading. Don't set lazy load if your method returns both real and display values.
	 * This limitation is in place because, very likely in that case, we do need directly the display value for the current component's data (real value) in order to display it in UI...
	 */
	@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
	public boolean getLazyLoading()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_LAZY_LOADING });
		return Utils.getAsBoolean(customProperty);
	}

	/**
	 * Sets the lazyLoading.
	 *
	 * @param lazyLoading The lazyLoading to set
	 */
	@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
	public void setLazyLoading(boolean arg)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_LAZY_LOADING }, Boolean.valueOf(arg));
	}

}
