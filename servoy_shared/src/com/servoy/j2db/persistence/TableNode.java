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
 * <p>The <code>Table</code> object supports defining table-level events such as <code>onCreate</code>,
 * <code>onUpdate</code>, and <code>onDelete</code>. These methods can control operations by allowing or
 * preventing actions based on custom logic. For instance, <code>onValidate</code> can validate a record
 * before insertion, while <code>onAfterInsert</code> is triggered post-insertion for additional operations.</p>
 *
 * <p>Additionally, events like <code>onSearch</code> and <code>onFind</code> enable customization of
 * foundset searches. Events such as <code>onFoundSetLoad</code> and <code>onFoundsetNextChunk</code> are
 * useful for managing in-memory or view-based datasets.</p>
 *
 * <p>The <code>columns</code> property allows for detailed configuration and interaction with the database
 * schema.</p>
 *
 * <p>For a broader understanding of database-level features and capabilities, refer to the
 * <a href="https://docs.servoy.com/reference/servoy-developer/solution-explorer/resources/database-servers/database-server">Database Server</a> documentation.</p>
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
	TableNode(ISupportChilds parent, UUID uuid)
	{
		super(IRepository.TABLENODES, parent, uuid);
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
		validator.checkName(name, null, ft, false);

		ScriptCalculation obj = (ScriptCalculation)getRootObject().getChangeHandler().createNewObject(this, IRepository.SCRIPTCALCULATIONS);
		//set all the required properties

		obj.setName(name);
		MethodTemplate template = MethodTemplate.getTemplate(ScriptCalculation.class, null);
		obj.setDeclaration(template.getMethodDeclaration(name, "\treturn '';", userTemplate)); //$NON-NLS-1$
		addChild(obj);
		return obj;
	}

	public ScriptMethod createNewFoundsetMethod(IValidateName validator, String methodName, String userTemplate) throws RepositoryException
	{
		String name = methodName == null ? "untitled" : methodName; //$NON-NLS-1$

		//check if name is in use
		ValidatorSearchContext ft = new ValidatorSearchContext(this, IRepository.METHODS);
		validator.checkName(name, null, ft, false);

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

	AggregateVariable createNewAggregateVariable(IValidateName validator, String aggName, int atype, String dataProviderIDToAggregate, ITable table)
		throws RepositoryException
	{
		String name = aggName == null ? "untitled" : aggName; //$NON-NLS-1$

		//check if name is in use
		ValidatorSearchContext ft = new ValidatorSearchContext(table, IRepository.AGGREGATEVARIABLES);
		validator.checkName(name, null, ft, true);

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
		if (stn == null) stn = DataSourceUtils.getViewServernameTablename(getDataSource());

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
		if (stn == null) stn = DataSourceUtils.getViewServernameTablename(getDataSource());
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
	 * Record validation method, will be called by databaseManager.validateRecord() and when databaseManager.saveData() is called.
	 * Validate changes or state of the record.
	 * All errors need toe be reported in the recordMarkers that is then returned by databaseManager.validateRecord() and is also placed
	 * on the record itself (record.recordMarkers)
	 *
	 * @templatename onValidate
	 * @templateparam JSRecord<${dataSource}> record record that must be validated
	 * @templateparam JSRecordMarkers recordMarkers the object where all the problems can be reported against.
	 * @templateparam Object stateObject an object that a user can give to validateRecord for extra state (optional, can be null).
	 * @templateaddtodo
	 * @templatecode
	 *
	 * if (record.mynumber < 10) recordMarkers.report("mynumber must be greater then 10", "mynumber", LOGGINGLEVEL.ERROR);
	 *
	 */
	public String getOnValidateMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONVALIDATEMETHODID);
	}

	public void setOnValidateMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONVALIDATEMETHODID, uuid);
	}

	/**
	 * A method that can be used to load extra data in an in memory datasource.
	 * <p>
	 * This method id called when the inMem datasource is fully read.
	 *
	 * @templatedescription Return the next chunk of data for an inmemory datasource, when there is no more data, return nothing or an empty dataset
	 * @templatetype JSDataSet
	 * @templatename onFoundsetNextChunk
	 * @templateparam String inmemDataSourceName name of the inmemory datasource.
	 * @templateparam Number sizeHint preferred number of records to be retrieved.
	 * @templateaddtodo
	 * @templatecode
	 * return databaseManager.createEmptyDataSet();
	 */
	public String getOnFoundsetNextChunkMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOUNDSETNEXTCHUNKMETHODID);
	}

	public void setOnFoundsetNextChunkMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOUNDSETNEXTCHUNKMETHODID, uuid);
	}

	/**
	 * A method that is executed before an insert operation. The method can block the insert operation by returning false.
	 *
	 * @templatedescription
	 * Record pre-insert trigger
	 * Validate the record to be inserted.
	 * When false is returned or a validation error is added to the recordMarkers the record will not be inserted in the database.
	 * When an exception is thrown the record will also not be inserted in the database but it will be added to databaseManager.getFailedRecords(),
	 * the thrown exception can be retrieved via record.exception.getValue().
	 * @templatename onRecordInsert
	 * @templatetype Boolean
	 * @templateparam JSRecord<${dataSource}> record record that will be inserted
	 * @templateparam JSRecordMarkers recordMarkers the object where all the problems can be reported against
	 * @templateparam Object stateObject an object that a user can give to validateRecord for extra state (optional, can be null).
	 * @templateaddtodo
	 * @templatecode
	 *
	 * var valid = true;
	 * if (record.mynumber > 10) {
	 *   recordMarkers.report("mynumber must be greater then 10", "mynumber",LOGGINGLEVEL.ERROR);
	 *   valid = true; // keep the valid on true if you just report through the recordMarkers and want to also execute other oninsert methods.;
	 * }
	 *
	 * // return boolean to indicate success
	 * return valid;
	 */
	public String getOnInsertMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONINSERTMETHODID);
	}

	public void setOnInsertMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONINSERTMETHODID, uuid);
	}

	/**
	 * A method that is executed before an update operation. A method can block the update by returning false.
	 *
	 * @templatedescription
	 * Record pre-update trigger
	 * Validate the record to be updated.
	 * When false is returned or a validation error is added to the recordMarkers the record will not be updated in the database.
	 * When an exception is thrown the record will also not be updated in the database but it will be added to databaseManager.getFailedRecords(),
	 * the thrown exception can be retrieved via record.exception.getValue().
	 * @templatename onRecordUpdate
	 * @templatetype Boolean
	 * @templateparam JSRecord<${dataSource}> record record that will be updated
	 * @templateparam JSRecordMarkers recordMarkers the object where all the problems can be reported against
	 * @templateparam Object stateObject an object that a user can give to validateRecord for extra state (optional, can be null).
	 * @templateaddtodo
	 * @templatecode
	 *
	 * var valid = true;
	 * if (record.mynumber > 10) {
	 *   recordMarkers.report("mynumber must be greater then 10", "mynumber", LOGGINGLEVEL.ERROR);
	 *   valid = true; // keep the valid on true if you just report through the recordMarkers and want to also execute other oninsert methods.
	 * }
	 *
	 * // return boolean to indicate success
	 * return valid;
	 */
	public String getOnUpdateMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONUPDATEMETHODID);
	}

	public void setOnUpdateMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONUPDATEMETHODID, uuid);
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
	 * var valid = true;
	 * // test if it is valid.
	 *
	 * // throw exception to pass info to handler, will be returned in record.exception.getValue() when record.exception is a DataException
	 * if (!valid) throw 'cannot delete'
	 *
	 * // return boolean to indicate success
	 * return true
	 */
	public String getOnDeleteMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDELETEMETHODID);
	}

	public void setOnDeleteMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDELETEMETHODID, uuid);
	}

	/**
	 * A method that is executed before a foundset selection change operation. The method can cancel the selection change operation by returning false.
	 *
	 * @templatedescription
	 * Foundset pre-selection change trigger
	 * Can be used to validate the record to be not selected.
	 * When false is returned the selection will not bechanged.
	 * @templatename onFoundSetSelectionChange
	 * @templatetype Boolean
	 * @templateparam JSRecord<${dataSource}>|Array<JSRecord<${dataSource}>> oldSelection array with selected records
	 * @templateparam JSRecord<${dataSource}>|Array<JSRecord<${dataSource}>> newSelection array with records that will become selected
	 * @templateaddtodo
	 * @templatecode
	 *
	 * var valid = true;
	 * // test if it is valid.
	 * return valid;
	 */
	public String getOnFoundSetBeforeSelectionChangeMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOUNDSETBEFORESELECTIONCHANGEMETHODID);
	}

	public void setOnFoundSetBeforeSelectionChangeMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOUNDSETBEFORESELECTIONCHANGEMETHODID, uuid);
	}

	/**
	 * A method that is executed after an insert operation.
	 *
	 * @templatedescription Record after-insert trigger
	 * @templatename afterRecordInsert
	 * @templateparam JSRecord<${dataSource}> record record that is inserted
	 * @templateaddtodo
	 */
	public String getOnAfterInsertMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERINSERTMETHODID);
	}

	public void setOnAfterInsertMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERINSERTMETHODID, uuid);
	}

	/**
	 * A method that is executed after an update operation.
	 *
	 * @templatedescription Record after-update trigger
	 * @templatename afterRecordUpdate
	 * @templateparam JSRecord<${dataSource}> record record that is updated
	 * @templateaddtodo
	 */
	public String getOnAfterUpdateMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERUPDATEMETHODID);
	}

	public void setOnAfterUpdateMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERUPDATEMETHODID, uuid);
	}

	/**
	 * A method that is executed after a delete operation.
	 *
	 * @templatedescription Record after-delete trigger
	 * @templatename afterRecordDelete
	 * @templateparam JSRecord<${dataSource}> record record that is deleted
	 * @templateaddtodo
	 */
	public String getOnAfterDeleteMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERDELETEMETHODID);
	}

	public void setOnAfterDeleteMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERDELETEMETHODID, uuid);
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
	public String getOnCreateMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONCREATEMETHODID);
	}

	public void setOnCreateMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONCREATEMETHODID, uuid);
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
	public String getOnFindMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONFINDMETHODID);
	}

	public void setOnFindMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONFINDMETHODID, uuid);
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
	public String getOnAfterFindMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERFINDMETHODID);
	}

	public void setOnAfterFindMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERFINDMETHODID, uuid);
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
	public String getOnSearchMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONSEARCHMETHODID);
	}

	public void setOnSearchMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONSEARCHMETHODID, uuid);
	}

	/**
	 * A method that is executed after a search is executed for a foundset.
	 * @templatedescription
	 * Foundset post-search trigger
	 * @templatename afterFoundSetSearch
	 * @templateaddtodo
	 * @templatecode
	 */
	public String getOnAfterSearchMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERSEARCHMETHODID);
	}

	public void setOnAfterSearchMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERSEARCHMETHODID, uuid);
	}

	/**
	 * A method that is executed after a new record is created.
	 *
	 * @templatedescription Record after-create trigger
	 * @templatename afterFoundSetRecordCreate
	 * @templateparam JSRecord<${dataSource}> record record that is created
	 * @templateaddtodo
	 */
	public String getOnAfterCreateMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERCREATEMETHODID);
	}

	public void setOnAfterCreateMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONAFTERCREATEMETHODID, uuid);
	}

	/**
	 * A method that is executed when an in memory or viewfoundset table is first touched (by ui or scripting)
	 *
	 * @templatedescription Foundset load trigger, make sure a for inmem JSDataSet.createDataSource(inMemName) is called or that a ViewFoundSet is registered (datasources.view.xxx.getViewFoundset(select)
	 * @templatename onFoundSetLoad
	 * @templateparam String memOrViewName The in memory or view foundset table name that is touched.
	 * @templateaddtodo
	 */
	public String getOnFoundSetLoadMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOUNDSETLOADMETHODID);
	}

	public void setOnFoundSetLoadMethodID(String uuid)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOUNDSETLOADMETHODID, uuid);
	}

	public boolean getImplicitSecurityNoRights()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_IMPLICIT_SECURITY_NO_RIGHTS });
		if (customProperty instanceof Boolean) return ((Boolean)customProperty).booleanValue();
		return false;
	}

	public void setImplicitSecurityNoRights(boolean implicitSecurity)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_IMPLICIT_SECURITY_NO_RIGHTS }, Boolean.valueOf(implicitSecurity));
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
