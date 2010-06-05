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
import java.util.List;

import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;

/**
 * This class is a repository node for storing scriptcalculations under, because jaleman said that there easily are 300-1000 calcs in a solution
 * 
 * @author jblok
 */
public class TableNode extends AbstractBase implements ISupportChilds
{
	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private String dataSource = null;
	private int onInsertMethodID;
	private int onUpdateMethodID;
	private int onDeleteMethodID;
	private int onAfterInsertMethodID;
	private int onAfterUpdateMethodID;
	private int onAfterDeleteMethodID;

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

	public List<ScriptCalculation> getScriptCalculations()
	{
		return SortedTypeIterator.createFilteredList(getAllObjectsAsList(), IRepository.SCRIPTCALCULATIONS);
	}

	ScriptCalculation createNewScriptCalculation(IValidateName validator, String name) throws RemoteException, RepositoryException
	{
		if (name == null) name = "untitled"; //$NON-NLS-1$

		//check if name is in use
		ValidatorSearchContext ft = new ValidatorSearchContext(getRootObject().getServer(getServerName()).getTable(getTableName()),
			IRepository.SCRIPTCALCULATIONS);
		validator.checkName(name, 0, ft, false);

		ScriptCalculation obj = (ScriptCalculation)getRootObject().getChangeHandler().createNewObject(this, IRepository.SCRIPTCALCULATIONS);
		//set all the required properties

		obj.setName(name);
		MethodTemplate template = MethodTemplate.getTemplate(ScriptCalculation.class, null);
		obj.setDeclaration(template.getMethodDeclaration(name, "\treturn 1;")); //$NON-NLS-1$
		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for AggregateVariable handling
	 */

	public List<AggregateVariable> getAggregateVariables()
	{
		return SortedTypeIterator.createFilteredList(getAllObjectsAsList(), IRepository.AGGREGATEVARIABLES);
	}

	AggregateVariable createNewAggregateVariable(IValidateName validator, String name, int atype, String dataProviderIDToAggregate) throws RemoteException,
		RepositoryException
	{
		if (name == null) name = "untitled"; //$NON-NLS-1$

		//check if name is in use
		ValidatorSearchContext ft = new ValidatorSearchContext(getRootObject().getServer(getServerName()).getTable(getTableName()),
			IRepository.AGGREGATEVARIABLES);
		validator.checkName(name, 0, ft, true);

		AggregateVariable obj = (AggregateVariable)getRootObject().getChangeHandler().createNewObject(this, IRepository.AGGREGATEVARIABLES);
		//set all the required properties

		obj.setName(name);
		obj.setType(atype);
		obj.setDataProviderIDToAggregate(dataProviderIDToAggregate);
		addChild(obj);
		return obj;
	}

/*
 * _____________________________________________________________ The methods below belong to this class
 */
	public void setDataSource(String arg)
	{
		checkForChange(dataSource, arg);
		dataSource = (arg == null) ? null : arg.intern();
	}

	public String getDataSource()
	{
		return dataSource;
	}

	public void setTableName(String name)
	{
		setDataSource(DataSourceUtils.createDBTableDataSource(getServerName(), name));
	}

	public String getTableName()
	{
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		return stn == null ? null : stn[1];
	}

	public void setServerName(String arg)
	{
		setDataSource(DataSourceUtils.createDBTableDataSource(arg, getTableName()));
	}

	public String getServerName()
	{
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		return stn == null ? null : stn[0];
	}

	private transient Table table = null;

	public Table getTable() throws RepositoryException
	{
		if (table == null)
		{
			try
			{
				IServer server = getRootObject().getServer(getServerName());
				if (server == null) throw new RepositoryException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { getServerName() });
				table = (Table)server.getTable(getTableName());
			}
			catch (RemoteException e)
			{
				throw new RepositoryException(e);
			}
		}
		return table;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		List<IPersist> allobjects = getAllObjectsAsList();
		if (allobjects != null)
		{
			for (int i = 0; i < allobjects.size(); i++)
			{
				sb.append(allobjects.get(i));
				sb.append("\n"); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

	/**
	 * @templatedescription 
	 * Record pre-insert trigger
	 * Validate the record to be inserted.
	 * When false is returned the record will not be inserted in the database.
	 * When an exception is thrown the record will also not be inserted in the database but it will be added to databaseManager.getFailedRecords(),
	 * the thrown exception can be retrieved via record.exception.getValue().
	 * @templatename onRecordInsert
	 * @templatetype Boolean
	 * @templateparam JSRecord record record that will be inserted
	 * @templateaddtodo
	 * @templatecode
	 * 
	 * // throw exception to pass info to handler, will be returned in record.exception.getValue() when record.exception is a DataException
	 * if (not_valid) throw 'cannot insert'
	 * 
	 * // return boolean to indicate success
	 * return true
	 */
	public int getOnInsertMethodID()
	{
		return onInsertMethodID;
	}

	public void setOnInsertMethodID(int arg)
	{
		checkForChange(onInsertMethodID, arg);
		onInsertMethodID = arg;
	}

	/**
	 * @templatedescription 
	 * Record pre-update trigger
	 * Validate the record to be updated.
	 * When false is returned the record will not be updated in the database.
	 * When an exception is thrown the record will also not be updated in the database but it will be added to databaseManager.getFailedRecords(),
	 * the thrown exception can be retrieved via record.exception.getValue().
	 * @templatename onRecordUpdate
	 * @templatetype Boolean
	 * @templateparam JSRecord record record that will be updated
	 * @templateaddtodo
	 * @templatecode
	 * 
	 * // throw exception to pass info to handler, will be returned in record.exception.getValue() when record.exception is a DataException
	 * if (not_valid) throw 'cannot update'
	 * 
	 * // return boolean to indicate success
	 * return true
	 */
	public int getOnUpdateMethodID()
	{
		return onUpdateMethodID;
	}

	public void setOnUpdateMethodID(int arg)
	{
		checkForChange(onUpdateMethodID, arg);
		onUpdateMethodID = arg;
	}

	/**
	 * @templatedescription 
	 * Record pre-delete trigger
	 * Validate the record to be deleted.
	 * When false is returned the record will not be deleted in the database.
	 * When an exception is thrown the record will also not be deleted in the database but it will be added to databaseManager.getFailedRecords(),
	 * the thrown exception can be retrieved via record.exception.getValue().
	 * @templatename onRecordDelete
	 * @templatetype Boolean
	 * @templateparam JSRecord record record that will be deleted
	 * @templateaddtodo
	 * @templatecode
	 * 
	 * // throw exception to pass info to handler, will be returned in record.exception.getValue() when record.exception is a DataException
	 * if (not_valid) throw 'cannot delete'
	 * 
	 * // return boolean to indicate success
	 * return true
	 */
	public int getOnDeleteMethodID()
	{
		return onDeleteMethodID;
	}

	public void setOnDeleteMethodID(int arg)
	{
		checkForChange(onDeleteMethodID, arg);
		onDeleteMethodID = arg;
	}

	/**
	 * @templatedescription Record after-insert trigger
	 * @templatename afterInsertRecord
	 * @templateparam JSRecord record record that is inserted
	 * @templateaddtodo
	 */
	public int getOnAfterInsertMethodID()
	{
		return onAfterInsertMethodID;
	}

	public void setOnAfterInsertMethodID(int arg)
	{
		checkForChange(onAfterInsertMethodID, arg);
		onAfterInsertMethodID = arg;
	}

	/**
	 * @templatedescription Record after-update trigger
	 * @templatename afterUpdateRecord
	 * @templateparam JSRecord record record that is updated
	 * @templateaddtodo
	 */
	public int getOnAfterUpdateMethodID()
	{
		return onAfterUpdateMethodID;
	}

	public void setOnAfterUpdateMethodID(int arg)
	{
		checkForChange(onAfterUpdateMethodID, arg);
		onAfterUpdateMethodID = arg;
	}

	/**
	 * @templatedescription Record after-delete trigger
	 * @templatename afterDeleteRecord
	 * @templateparam JSRecord record record that is deleted
	 * @templateaddtodo
	 */
	public int getOnAfterDeleteMethodID()
	{
		return onAfterDeleteMethodID;
	}

	public void setOnAfterDeleteMethodID(int arg)
	{
		checkForChange(onAfterDeleteMethodID, arg);
		onAfterDeleteMethodID = arg;
	}

	public boolean isEmpty()
	{
		return (!getAllObjects().hasNext() && getOnInsertMethodID() == 0 && getOnUpdateMethodID() == 0 && getOnDeleteMethodID() == 0 &&
			getOnAfterInsertMethodID() == 0 && getOnAfterUpdateMethodID() == 0 && getOnAfterDeleteMethodID() == 0);
	}
}
