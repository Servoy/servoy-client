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


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.annotations.ServoyDocumented;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * A value list
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class ValueList extends AbstractBase implements ISupportUpdateableName, ISupportContentEquals, ICloneable
{
	public static final int CUSTOM_VALUES = 0;
	public static final int DATABASE_VALUES = 1;

	//type of database Values
	public static final int TABLE_VALUES = 2;
	public static final int RELATED_VALUES = 3;

	public static final int GLOBAL_METHOD_VALUES = 4;

	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private String name = null;
	private int valueListType; // CUSTOM_VALUES or DATABASE_VALUES
	private String customValues = null;

	private String dataSource = null;//TABLE_VALUES

	private String relationName;//RELATED_VALUES

	private String dataProviderID1 = null;//1
	private String dataProviderID2 = null;//2
	private String dataProviderID3 = null;//4
	private int showDataProvider;
	private int returnDataProviders;

	private int fallbackValueListID;

	private String separator = null;
	private String sortOptions = null;

	public static final int EMPTY_VALUE_ALWAYS = 0;
	public static final int EMPTY_VALUE_NEVER = 1;
	public static final int EMPTY_VALUE_ONCREATION_ONLY = 2; //TODO:not impl yet
	private int addEmptyValue;
	private boolean useTableFilter; //only used for db valuelist

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
				getValueListType() == other.getValueListType() && getValueListType() == CUSTOM_VALUES && ("" + getCustomValues()).equals(other.getCustomValues()) //$NON-NLS-1$
			);
		}
		return false;
	}


	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		validator.checkName(arg, getID(), new ValidatorSearchContext(IRepository.VALUELISTS), false);
		checkForNameChange(name, arg);
		name = arg;
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	/**
	 * Set the name
	 * 
	 * @param arg the name
	 */
	public void setName(String arg)
	{
		if (name != null) throw new UnsupportedOperationException("Can't set name 2x, use updateName"); //$NON-NLS-1$
		name = arg;
	}

	/**
	 * The name of the value list.
	 * 
	 * It is relevant when the "useTableFilter" property is set.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the valueListType
	 * 
	 * @param arg the valueListType
	 */
	public void setValueListType(int arg)
	{
		checkForChange(valueListType, arg);
		valueListType = arg;
	}

	/**
	 * The type of the valuelist. Can be either custom values or database values.
	 */
	public int getValueListType()
	{
		return valueListType;
	}

	public int getDatabaseValuesType()
	{
		return (relationName == null ? TABLE_VALUES : RELATED_VALUES);
	}


	/**
	 * Sets the relationName.
	 * 
	 * @param relationName The relationName to set
	 */
	public void setRelationName(String relName)
	{
		String arg = relName;
		if (arg != null && relationName != null && relationName.startsWith(".")) //$NON-NLS-1$
		{
			arg = arg + relationName; // relationName was used to store deprecated value of nmrelation
		}
		checkForChange(relationName, arg);
		relationName = arg;
	}

	/**
	 * The name of the relation that is used for loading data from the database.
	 */
	public String getRelationName()
	{
		return relationName;
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
	 * A string with the elements in the valuelist. The elements 
	 * can be separated by linefeeds (custom1
	 * custom2), optional with realvalues ((custom1|1
	 * custom2|2)).
	 */
	public String getCustomValues()
	{
		return customValues;
	}

	/**
	 * Sets the customValues.
	 * 
	 * @param customValues The customValues to set
	 */
	public void setCustomValues(String arg)
	{
		checkForChange(customValues, arg);
		customValues = arg;
	}

	/**
	 * Compact representation of the names of the server and table that 
	 * are used for loading the data from the database.
	 */
	public String getDataSource()
	{
		return dataSource;
	}

	/**
	 * Set the data source
	 * 
	 * @param arg the data source uri
	 */
	public void setDataSource(String arg)
	{
		checkForChange(dataSource, arg);
		dataSource = arg == null ? null : (arg.intern());
	}

	/**
	 * The name of the database server that is used for loading the values when 
	 * the value list has the type set to database values.
	 */
	public String getServerName()
	{
		if (dataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		if (stn != null)
		{
			return stn[0];
		}

		// data source is not a server/table combi
		ITable table = null;
		try
		{
			table = getTable();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
		return table == null ? null : table.getServerName();
	}

	/**
	 * Sets the serverName.
	 * 
	 * @param serverName The serverName to set
	 */
	public void setServerName(String arg)
	{
		String uri = DataSourceUtils.createDBTableDataSource(arg, getTableName());
		checkForChange(dataSource, uri);
		dataSource = uri == null ? null : (uri.intern());
	}

	/**
	 * The name of the database table that is used for loading the values when 
	 * the value list has the type set to database values.
	 */
	public String getTableName()
	{
		if (dataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		if (stn != null)
		{
			return stn[1];
		}

		// data source is not a server/table combi
		ITable table = null;
		try
		{
			table = getTable();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
		return table == null ? null : table.getName();
	}

	public ITable getTable() throws RepositoryException, RemoteException
	{
		if (dataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		if (stn != null)
		{
			IServer server = getRootObject().getServer(stn[0]);
			if (server != null)
			{
				return server.getTable(stn[1]);
			}
			return null;
		}

		// not a server/table combi, ask the current clients foundset manager
		if (J2DBGlobals.getServiceProvider() != null)
		{
			return J2DBGlobals.getServiceProvider().getFoundSetManager().getTable(dataSource);
		}

		// developer
		return null;
	}


	/**
	 * Sets the tableName.
	 * 
	 * @param tableName The tableName to set
	 */
	public void setTableName(String arg)
	{
		String uri = DataSourceUtils.createDBTableDataSource(getServerName(), arg);
		checkForChange(dataSource, uri);
		dataSource = uri == null ? null : (uri.intern());
	}

	/**
	 * Gets the dataProviderID1.
	 * 
	 * @return Returns a String
	 */
	public String getDataProviderID1()
	{
		return dataProviderID1;
	}

	/**
	 * Sets the dataProviderID1.
	 * 
	 * @param dataProviderID1 The dataProviderID1 to set
	 */
	public void setDataProviderID1(String arg)
	{
		checkForChange(dataProviderID1, arg);
		dataProviderID1 = arg;
		dataProviderIDs = null;
	}

	/**
	 * Gets the dataProviderID2.
	 * 
	 * @return Returns a String
	 */
	public String getDataProviderID2()
	{
		return dataProviderID2;
	}

	/**
	 * Sets the dataProviderID2.
	 * 
	 * @param dataProviderID2 The dataProviderID2 to set
	 */
	public void setDataProviderID2(String arg)
	{
		checkForChange(dataProviderID2, arg);
		dataProviderID2 = arg;
		dataProviderIDs = null;
	}

	/**
	 * Gets the dataProviderID3.
	 * 
	 * @return Returns a String
	 */
	public String getDataProviderID3()
	{
		return dataProviderID3;
	}

	/**
	 * Sets the dataProviderID3.
	 * 
	 * @param dataProviderID3 The dataProviderID3 to set
	 */
	public void setDataProviderID3(String arg)
	{
		checkForChange(dataProviderID3, arg);
		dataProviderID3 = arg;
		dataProviderIDs = null;
	}

	/**
	 * A String representing the separator that should be used when multiple
	 * display dataproviders are set, when the value list has the type set to
	 * database values.
	 */
	public String getSeparator()
	{
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
		checkForChange(separator, arg);
		separator = arg;
	}

	/**
	 * Sort options that are applied when the valuelist loads its data
	 * from the database.
	 */
	public String getSortOptions()
	{
		return sortOptions;
	}

	/**
	 * Sets the sortOptions.
	 * 
	 * @param sortOptions The sortOptions to set
	 */
	public void setSortOptions(String arg)
	{
		checkForChange(sortOptions, arg);
		sortOptions = arg;
	}

	/**
	 * Gets the showDataProvider.
	 * 
	 * @return Returns a int
	 */
	public int getShowDataProviders()
	{
		return showDataProvider;
	}

	/**
	 * Sets the showDataProvider.
	 * 
	 * @param showDataProvider The showDataProvider to set
	 */
	public void setShowDataProviders(int arg)
	{
		checkForChange(showDataProvider, arg);
		showDataProvider = arg;
	}

	/**
	 * Gets the returnDataProviders.
	 * 
	 * @return Returns a int
	 */
	public int getReturnDataProviders()
	{
		return returnDataProviders;
	}

	/**
	 * Sets the returnDataProviders.
	 * 
	 * @param returnDataProviders The returnDataProviders to set
	 */
	public void setReturnDataProviders(int arg)
	{
		checkForChange(returnDataProviders, arg);
		returnDataProviders = arg;
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
			int total = (showDataProvider | returnDataProviders);
			if ((total & 1) != 0)
			{
				all.add(dataProviderID1);
			}
			if ((total & 2) != 0)
			{
				all.add(dataProviderID2);
			}
			if ((total & 4) != 0)
			{
				all.add(dataProviderID3);
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
		return addEmptyValue;
	}

	/**
	 * Sets the addEmptyValue.
	 * 
	 * @param addEmptyValue The addEmptyValue to set
	 */
	public void setAddEmptyValue(int arg)
	{
		checkForChange(addEmptyValue, arg);
		addEmptyValue = arg;
	}


	/*
	 * _____________________________________________________________ Runtime property
	 */

	private transient int displayValueType = IColumnTypes.TEXT;//type of Column.allDefinedTypes

	public int getDisplayValueType()
	{
		return displayValueType;
	}

	public void setDisplayValueType(int i)
	{
		displayValueType = i;
	}

	/**
	 * Flag that tells if the name of the valuelist should be applied as a filter on the
	 * 'valuelist_name' column when retrieving the data from the database.
	 */
	public boolean getUseTableFilter()
	{
		return useTableFilter;
	}

	public void setUseTableFilter(boolean arg)
	{
		checkForChange(useTableFilter, arg);
		useTableFilter = arg;
	}

	public int getFallbackValueListID()
	{
		return fallbackValueListID;
	}

	public void setFallbackValueListID(int id)
	{
		checkForChange(fallbackValueListID, id);
		this.fallbackValueListID = id;
	}
}
