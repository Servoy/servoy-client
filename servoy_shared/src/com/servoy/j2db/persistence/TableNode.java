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
import java.util.Iterator;
import java.util.Map;

import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 * This class is a repository node for storing scriptcalculations under, because jaleman said that there easily are 300-1000 calcs in a solution
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Table", typeCode = IRepository.TABLENODES)
public class TableNode extends AbstractBase implements ISupportChilds
{

	private static final long serialVersionUID = 1L;

	private transient ITable table;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	TableNode(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.TABLENODES, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods for ScriptCalculation handling
	 */

	public Iterator<ScriptCalculation> getScriptCalculations()
	{
		return getObjects(IRepository.SCRIPTCALCULATIONS);
	}

	public ScriptCalculation createNewScriptCalculation(IValidateName validator, String calcName, String userTemplate, ITable table) throws RepositoryException
	{
		String name = calcName == null ? "untitled" : calcName; //$NON-NLS-1$

		//check if name is in use
		ValidatorSearchContext ft = new ValidatorSearchContext(table, IRepository.SCRIPTCALCULATIONS);
		validator.checkName(name, 0, ft, false);

		ScriptCalculation obj = (ScriptCalculation)getRootObject().getChangeHandler().createNewObject(this, IRepository.SCRIPTCALCULATIONS);
		//set all the required properties

		obj.setName(name);
		MethodTemplate template = MethodTemplate.getTemplate(ScriptCalculation.class, null);
		obj.setDeclaration(template.getMethodDeclaration(name, "\treturn 1;", userTemplate)); //$NON-NLS-1$
		addChild(obj);
		return obj;
	}

	public ScriptMethod createNewFoundsetMethod(IValidateName validator, String methodName, String userTemplate) throws RepositoryException
	{
		String name = methodName == null ? "untitled" : methodName; //$NON-NLS-1$

		//check if name is in use
		ValidatorSearchContext ft = new ValidatorSearchContext(this, IRepository.METHODS);
		validator.checkName(name, 0, ft, false);

		ScriptMethod obj = (ScriptMethod)getRootObject().getChangeHandler().createNewObject(this, IRepository.METHODS);
		//set all the required properties

		obj.setName(name);
		MethodTemplate template = MethodTemplate.getTemplate(ScriptMethod.class, null);
		obj.setDeclaration(template.getMethodDeclaration(name, null, userTemplate));
		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for AggregateVariable handling
	 */

	public Iterator<AggregateVariable> getAggregateVariables()
	{
		return getObjects(IRepository.AGGREGATEVARIABLES);
	}

	AggregateVariable createNewAggregateVariable(IValidateName validator, String calcName, int atype, String dataProviderIDToAggregate, ITable table)
		throws RepositoryException
	{
		String name = calcName == null ? "untitled" : calcName; //$NON-NLS-1$

		//check if name is in use
		ValidatorSearchContext ft = new ValidatorSearchContext(table, IRepository.AGGREGATEVARIABLES);
		validator.checkName(name, 0, ft, true);

		AggregateVariable obj = (AggregateVariable)getRootObject().getChangeHandler().createNewObject(this, IRepository.AGGREGATEVARIABLES);
		//set all the required properties

		obj.setName(name);
		obj.setType(atype);
		obj.setDataProviderIDToAggregate(dataProviderIDToAggregate);
		addChild(obj);
		return obj;
	}

	public Iterator<ScriptMethod> getFoundsetMethods(boolean sort)
	{
		return Solution.getScriptMethods(getAllObjectsAsList(), null, sort);
	}

	public ScriptMethod getFoundsetMethod(int methodId)
	{
		return AbstractBase.selectById(getFoundsetMethods(false), methodId);
	}

/*
 * _____________________________________________________________ The methods below belong to this class
 */
	public void setDataSource(String arg)
	{
		Object old = getProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE.getPropertyName());
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE, arg);
	}

	/**
	 * The datasource of this table.
	 */
	public String getDataSource()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE);
	}

	public void setTableName(String name)
	{
		setDataSource(DataSourceUtils.createDBTableDataSource(getServerName(), name));
	}

	public String getTableName()
	{
		String[] stn = DataSourceUtilsBase.getDBServernameTablename(getDataSource());
		if (stn == null) stn = DataSourceUtils.getMemServernameTablename(getDataSource());

		return stn == null ? null : stn[1];
	}


	public void setServerName(String arg)
	{
		setDataSource(DataSourceUtils.createDBTableDataSource(arg, getTableName()));
	}

	public String getServerName()
	{
		String[] stn = DataSourceUtilsBase.getDBServernameTablename(getDataSource());
		if (stn == null) stn = DataSourceUtils.getMemServernameTablename(getDataSource());
		return stn == null ? null : stn[0];
	}

	public void setTable(ITable table)
	{
		this.table = table;
	}

	ITable getTable() throws RepositoryException
	{
		// can we exact this out of this class?
		// aggregates and script calculations are depending on this (IColumn.getTable())
		if (table != null) return table;
		String dataSource = getDataSource();
		String[] dbServernameTablename = DataSourceUtilsBase.getDBServernameTablename(dataSource);
		try
		{
			// does it have a sql server/table
			if (dbServernameTablename != null)
			{
				IServer server = getRootObject().getServer(dbServernameTablename[0]);
				return server != null ? server.getTable(dbServernameTablename[1]) : null;
			}
			// or is it a in memory datasource
			String inmemDataSourceName = DataSourceUtils.getInmemDataSourceName(dataSource);
			if (inmemDataSourceName != null)
			{
				if (J2DBGlobals.getServiceProvider() != null)
				{
					return J2DBGlobals.getServiceProvider().getFoundSetManager().getTable(dataSource);
				}
				else
				{
					Debug.warn("Cannot retrieve in memory datasource: " + dataSource + ", there is no application available.");
				}
			}
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
		return null;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("TableNode{"); //$NON-NLS-1$
		sb.append(getDataSource());
		sb.append("}\n"); //$NON-NLS-1$
		for (IPersist obj : getAllObjectsAsList())
		{
			sb.append(obj);
			sb.append('\n');
		}
		return sb.toString();
	}

	/**
	 * A method that is executed before an insert operation. The method can block the insert operation by returning false.
	 *
	 * @templatedescription
	 * Record pre-insert trigger
	 * Validate the record to be inserted.
	 * When false is returned the record will not be inserted in the database.
	 * When an exception is thrown the record will also not be inserted in the database but it will be added to databaseManager.getFailedRecords(),
	 * the thrown exception can be retrieved via record.exception.getValue().
	 * @templatename onRecordInsert
	 * @templatetype Boolean
	 * @templateparam JSRecord<${dataSource}> record record that will be inserted
	 * @templateaddtodo
	 * @templatecode
	 *
	 * var not_valid = false;
	 * // test if it is valid.
	 *
	 * // throw exception to pass info to handler, will be returned in record.exception.getValue() when record.exception is a DataException
	 * if (not_valid) throw 'cannot insert'
	 *
	 * // return boolean to indicate success
	 * return true
	 */
	public int getOnInsertMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONINSERTMETHODID).intValue();
	}

	public void setOnInsertMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONINSERTMETHODID, arg);
	}

	/**
	 * A method that is executed before an update operation. A method can block the update by returning false.
	 *
	 * @templatedescription
	 * Record pre-update trigger
	 * Validate the record to be updated.
	 * When false is returned the record will not be updated in the database.
	 * When an exception is thrown the record will also not be updated in the database but it will be added to databaseManager.getFailedRecords(),
	 * the thrown exception can be retrieved via record.exception.getValue().
	 * @templatename onRecordUpdate
	 * @templatetype Boolean
	 * @templateparam JSRecord<${dataSource}> record record that will be updated
	 * @templateaddtodo
	 * @templatecode
	 *
	 * var not_valid = false;
	 * // test if it is valid.
	 *
	 * // throw exception to pass info to handler, will be returned in record.exception.getValue() when record.exception is a DataException
	 * if (not_valid) throw 'cannot update'
	 *
	 * // return boolean to indicate success
	 * return true
	 */
	public int getOnUpdateMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONUPDATEMETHODID).intValue();
	}

	public void setOnUpdateMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONUPDATEMETHODID, arg);
	}

	/**
	 * A method that is executed before a delete operation. The method can block the delete operation by returning false.
	 *
	 * @templatedescription
	 * Record pre-delete trigger
	 * Validate the record to be deleted.
	 * When false is returned the record will not be deleted in the database.
	 * When an exception is thrown the record will also not be deleted in the database but it will be added to databaseManager.getFailedRecords(),
	 * the thrown exception can be retrieved via record.exception.getValue().
	 * @templatename onRecordDelete
	 * @templatetype Boolean
	 * @templateparam JSRecord<${dataSource}> record record that will be deleted
	 * @templateaddtodo
	 * @templatecode
	 *
	 * var not_valid = false;
	 * // test if it is valid.
	 *
	 * // throw exception to pass info to handler, will be returned in record.exception.getValue() when record.exception is a DataException
	 * if (not_valid) throw 'cannot delete'
	 *
	 * // return boolean to indicate success
	 * return true
	 */
	public int getOnDeleteMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDELETEMETHODID).intValue();
	}

	public void setOnDeleteMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDELETEMETHODID, arg);
	}

	/**
	 * A method that is executed after an insert operation.
	 *
	 * @templatedescription Record after-insert trigger
	 * @templatename afterRecordInsert
	 * @templateparam JSRecord<${dataSource}> record record that is inserted
	 * @templateaddtodo
	 */
	public int getOnAfterInsertMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERINSERTMETHODID).intValue();
	}

	public void setOnAfterInsertMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERINSERTMETHODID, arg);
	}

	/**
	 * A method that is executed after an update operation.
	 *
	 * @templatedescription Record after-update trigger
	 * @templatename afterRecordUpdate
	 * @templateparam JSRecord<${dataSource}> record record that is updated
	 * @templateaddtodo
	 */
	public int getOnAfterUpdateMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERUPDATEMETHODID).intValue();
	}

	public void setOnAfterUpdateMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERUPDATEMETHODID, arg);
	}

	/**
	 * A method that is executed after a delete operation.
	 *
	 * @templatedescription Record after-delete trigger
	 * @templatename afterRecordDelete
	 * @templateparam JSRecord<${dataSource}> record record that is deleted
	 * @templateaddtodo
	 */
	public int getOnAfterDeleteMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERDELETEMETHODID).intValue();
	}

	public void setOnAfterDeleteMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERDELETEMETHODID, arg);
	}

	/**
	 * A method that is executed before a record is created. The method can block the creation by returning false.
	 *
	 * @templatedescription
	 * Record pre-create trigger
	 * When false is returned the record will not be created in the foundset.
	 * @templatename onFoundSetRecordCreate
	 * @templatetype Boolean
	 * @templateaddtodo
	 * @templatecode
	 *
	 * // return true so that the record can be created
	 * return true
	 */
	public int getOnCreateMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONCREATEMETHODID).intValue();
	}

	public void setOnCreateMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONCREATEMETHODID, arg);
	}

	/**
	 * A method that is executed before a foundset is going into find mode. The method can block the mode change.
	 *
	 * @templatedescription
	 * Foundset pre-find trigger
	 * When false is returned the foundset will not go into find mode.
	 * @templatename onFoundSetFind
	 * @templatetype Boolean
	 * @templateaddtodo
	 * @templatecode
	 *
	 * // return true so that it will go into find mode.
	 * return true
	 */
	public int getOnFindMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONFINDMETHODID).intValue();
	}

	public void setOnFindMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONFINDMETHODID, arg);
	}

	/**
	 * A method that is executed after a foundset has switched to find mode.
	 *
	 * @templatedescription
	 * Foundset post-find trigger
	 * @templatename afterFoundSetFind
	 * @templateaddtodo
	 * @templatecode
	 */
	public int getOnAfterFindMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERFINDMETHODID).intValue();
	}

	public void setOnAfterFindMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERFINDMETHODID, arg);
	}

	/**
	 * A method that is executed before search() is called on a foundset in find mode. The method can block the search (foundset will stay in find mode).
	 *
	 * @templatedescription
	 * Foundset pre-search trigger
	 * When false is returned the search will not be executed and the foundset will stay in find mode.
	 * @templatename onFoundSetSearch
	 * @templateparam Boolean clearLastResults
	 * @templateparam Boolean reduceSearch
	 * @templatetype Boolean
	 * @templateaddtodo
	 * @templatecode
	 *
	 * // return true so that the search will go on.
	 * return true
	 */
	public int getOnSearchMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONSEARCHMETHODID).intValue();
	}

	public void setOnSearchMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONSEARCHMETHODID, arg);
	}

	/**
	 * A method that is executed after a search is executed for a foundset.
	 * @templatedescription
	 * Foundset post-search trigger
	 * @templatename afterFoundSetSearch
	 * @templateaddtodo
	 * @templatecode
	 */
	public int getOnAfterSearchMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERSEARCHMETHODID).intValue();
	}

	public void setOnAfterSearchMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERSEARCHMETHODID, arg);
	}

	/**
	 * A method that is executed after a new record is created.
	 *
	 * @templatedescription Record after-create trigger
	 * @templatename afterFoundSetRecordCreate
	 * @templateparam JSRecord<${dataSource}> record record that is created
	 * @templateaddtodo
	 */
	public int getOnAfterCreateMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERCREATEMETHODID).intValue();
	}

	public void setOnAfterCreateMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERCREATEMETHODID, arg);
	}

	/**
	 * A method that is executed when an in memory table is first touched (by ui or scripting)
	 *
	 * @templatedescription Foundset load trigger, make sure a JSDataSet.createDataSource(inMemName) is called.
	 * @templatename onFoundSetLoad
	 * @templateparam String inMemName The in memory table name that is touched.
	 * @templateaddtodo
	 */
	public int getOnFoundSetLoadMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOUNDSETLOADMETHODID).intValue();
	}

	public void setOnFoundSetLoadMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOUNDSETLOADMETHODID, arg);
	}

	public boolean isEmpty()
	{
		// Table node is empty if it has no method/var and no property set (except for dataSource)
		if (getAllObjects().hasNext())
		{
			return false;
		}

		Map<String, Object> props = getPropertiesMap();
		return props.isEmpty() || (props.size() == 1 && props.containsKey(StaticContentSpecLoader.PROPERTY_DATASOURCE.getPropertyName()));
	}

	public ScriptCalculation getScriptCalculation(String name)
	{
		if (name == null) return null;
		return AbstractBase.selectByName(new TypeIterator<ScriptCalculation>(getAllObjects(), IRepository.SCRIPTCALCULATIONS), name);
	}

	public AggregateVariable getAggregateVariable(String name)
	{
		if (name == null) return null;
		return AbstractBase.selectByName(new TypeIterator<AggregateVariable>(getAllObjects(), IRepository.AGGREGATEVARIABLES), name);
	}

	public ScriptMethod getFoundsetMethod(String name)
	{
		if (name == null) return null;
		return AbstractBase.selectByName(new TypeIterator<ScriptMethod>(getAllObjects(), IRepository.METHODS), name);
	}

	/**
	 * @param contents
	 */
	public void setColumns(ServoyJSONObject contents)
	{
		this.setTypedProperty(StaticContentSpecLoader.PROPERTY_COLUMNS, contents);
	}

	public ServoyJSONObject getColumns()
	{
		return this.getTypedProperty(StaticContentSpecLoader.PROPERTY_COLUMNS);
	}
}
