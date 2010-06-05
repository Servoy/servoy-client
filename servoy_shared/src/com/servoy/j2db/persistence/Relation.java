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
import java.util.Arrays;
import java.util.List;

import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * A relation (between 2 tables on one or more key column pairs)
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class Relation extends AbstractBase implements ISupportChilds, ISupportUpdateableName, ISupportHTMLToolTipText, ISupportContentEquals, ICloneable
{
	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private String name = null;
	private String primaryDataSource = null;
	private String foreignDataSource = null;
	private boolean allowCreationRelatedRecords;
	private boolean deleteRelatedRecords;
	private String initialSort;
	private boolean duplicateRelatedRecords;
	private boolean allowParentDeleteWhenHavingRelatedRecords;
	private boolean existsInDB;
	private int joinType = ISQLJoin.INNER_JOIN; // 0 default in repository

	/*
	 * All 1-n providers for this class
	 */
	private transient IDataProvider[] primary;
	private transient Column[] foreign;
	private transient int[] operators;

	/**
	 * Constructor I
	 */
	Relation(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.RELATIONS, parent, element_id, uuid);
	}

	public boolean isRuntimeReadonly()
	{
		return primary != null || foreign != null || operators != null;
	}

	public void setChanged(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		isChanged = true;
		IDataProvider[] dataproviders = getPrimaryDataProviders(dataProviderHandler);
		int[] operators = getOperators();
		Column[] columns = getForeignColumns();
		createNewRelationItems(dataproviders, operators, columns);

	}

	/*
	 * _____________________________________________________________ Methods from IPersist
	 */

	/*
	 * _____________________________________________________________ Methods for relation column handling
	 */

	public boolean checkIfRelationItemsValid(IDataProvider[] primaryDataProvider, Column[] foreignColumns) throws RepositoryException
	{
		if (primaryDataProvider == null || primaryDataProvider.length == 0 || foreignColumns == null || foreignColumns.length == 0)
		{
			throw new RepositoryException("one of the arguments is null or is an empty array"); //$NON-NLS-1$
		}
		for (Column column : foreignColumns)
		{
			if (!column.getTable().getName().equalsIgnoreCase(getForeignTableName()))
			{
				throw new RepositoryException("one of the arguments has another tablename than the ones defined on creation of the relations"); //$NON-NLS-1$
			}
		}
		return true;
	}

	public void createNewRelationItems(IDataProvider[] primaryDataProvider, int[] ops, Column[] foreignColumns) throws RepositoryException
	{
		if (!isParentRef()) checkIfRelationItemsValid(primaryDataProvider, foreignColumns);
		List<IPersist> allobjects = getAllObjectsAsList();

		int i = 0;
		if (primaryDataProvider != null)
		{
			for (; i < primaryDataProvider.length; i++)
			{
				RelationItem obj = null;
				if (i < allobjects.size())
				{
					obj = (RelationItem)allobjects.get(i);
				}
				else
				{
					obj = (RelationItem)getRootObject().getChangeHandler().createNewObject(this, IRepository.RELATION_ITEMS);
					addChild(obj);
				}

				//set all the required properties
				obj.setPrimaryDataProviderID(primaryDataProvider[i].getDataProviderID());
				obj.setOperator(ops[i]);
				obj.setForeignColumnName(foreignColumns[i].getName());
			}
		}

		//delete the once which are not used anymore
		if (i < allobjects.size())
		{
			IPersist[] remainder = allobjects.subList(i, allobjects.size()).toArray(new IPersist[allobjects.size() - i]);
			for (IPersist p : remainder)
			{
				((IDeveloperRepository)p.getRootObject().getRepository()).deleteObject(p);
			}
		}


		//if (relation_items.size() != 0) makeColumns(relation_items); //slow
		primary = primaryDataProvider; //faster
		foreign = foreignColumns; //faster
		operators = ops; //faster
		isGlobal = null;
		valid = true;
	}


	public RelationItem createNewRelationItem(IFoundSetManagerInternal foundSetManager, IDataProvider primaryDataProvider, int ops, Column foreignColumn)
		throws RepositoryException
	{
		if (!foreignColumn.getTable().equals(foundSetManager.getTable(foreignDataSource)))
		{
			throw new RepositoryException("one of the arguments has another tablename than the ones defined on creation of the relations"); //$NON-NLS-1$
		}
		RelationItem obj = null;
		if (primaryDataProvider != null)
		{
			obj = (RelationItem)getRootObject().getChangeHandler().createNewObject(this, IRepository.RELATION_ITEMS);
			//set all the required properties
			obj.setPrimaryDataProviderID(primaryDataProvider.getDataProviderID());
			obj.setOperator(ops);
			obj.setForeignColumnName(foreignColumn.getName());
			addChild(obj);
		}

		primary = null;
		foreign = null;
		operators = null;
		isGlobal = null;
		valid = true;

		return obj;
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

	//the repository element id can differ!
	public boolean contentEquals(Object obj)
	{
		if (obj instanceof Relation)
		{
			Relation other = (Relation)obj;
			try
			{
				List<IPersist> allobjects = getAllObjectsAsList();
				if (other.getAllObjectsAsList().size() != allobjects.size()) return false;
				for (int pos = 0; pos < allobjects.size(); pos++)
				{
					RelationItem ri = (RelationItem)allobjects.get(pos);
					RelationItem ori = (RelationItem)other.getAllObjectsAsList().get(pos);
					if (!ri.contentEquals(ori))
					{
						return false;
					}
				}

				if (!isGlobal() && (!getPrimaryTableName().equals(other.getPrimaryTableName()) || !getForeignTableName().equals(other.getForeignTableName())))
				{
					return false;
				}
				return (getName().equals(other.getName()) && getDeleteRelatedRecords() == other.getDeleteRelatedRecords() && getAllowCreationRelatedRecords() == other.getAllowCreationRelatedRecords());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return false;
	}

	@Override
	public String toString()
	{
		return name;
	}

	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		if (arg != null) arg = Utils.toEnglishLocaleLowerCase(arg);
		validator.checkName(arg, getID(), new ValidatorSearchContext(IRepository.RELATIONS), true);
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
		if (arg != null) arg = arg.toLowerCase();
		if (name != null) throw new UnsupportedOperationException("Can't set name 2x, use updateName"); //$NON-NLS-1$
		name = arg;
	}

	/**
	 * The name of the relation. 
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the primary data source
	 */
	public void setPrimaryDataSource(String arg)
	{
		checkForChange(primaryDataSource, arg);
		if (arg != null)
		{
			primaryDataSource = arg.intern();
		}
		else
		{
			primaryDataSource = null;
		}
	}

	/**
	 * Qualified name of the primary data source. Contains both the name of the primary server
	 * and the name of the primary table.
	 */
	public String getPrimaryDataSource()
	{
		return primaryDataSource;
	}

	/**
	 * Set the foreign data source
	 */
	public void setForeignDataSource(String arg)
	{
		checkForChange(foreignDataSource, arg);
		if (arg != null)
		{
			foreignDataSource = arg.intern();
		}
		else
		{
			foreignDataSource = null;
		}
	}

	/**
	 * Qualified name of the foreign data source. Contains both the name of the foreign
	 * server and the name of the foreign table.
	 */
	public String getForeignDataSource()
	{
		return foreignDataSource;
	}

	/**
	 * Set the serverName1
	 * 
	 * @param arg the serverName1
	 */
	public void setPrimaryServerName(String arg)
	{
		setPrimaryDataSource(DataSourceUtils.createDBTableDataSource(arg, getPrimaryTableName()));
	}

	public String getPrimaryServerName()
	{
		if (primaryDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(primaryDataSource);
		if (stn != null)
		{
			return stn[0];
		}

		// data source is not a server/table combi
		Table primaryTable = null;
		try
		{
			primaryTable = getPrimaryTable();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return primaryTable == null ? null : primaryTable.getServerName();
	}

	/**
	 * Set the foreignServerName
	 * 
	 * @param arg the foreignServerName
	 */
	public void setForeignServerName(String arg)
	{
		setForeignDataSource(DataSourceUtils.createDBTableDataSource(arg, getForeignTableName()));
	}

	public String getForeignServerName()
	{
		if (foreignDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(foreignDataSource);
		if (stn != null)
		{
			return stn[0];
		}

		// data source is not a server/table combi
		Table foreignTable = null;
		try
		{
			foreignTable = getForeignTable();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return foreignTable == null ? null : foreignTable.getServerName();
	}

	/**
	 * Set the tableName1
	 * 
	 * @param arg the tableName1
	 */
	public void setPrimaryTableName(String arg)
	{
		setPrimaryDataSource(DataSourceUtils.createDBTableDataSource(getPrimaryServerName(), arg));
	}

	public String getPrimaryTableName()
	{
		if (primaryDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(primaryDataSource);
		if (stn != null)
		{
			return stn[1];
		}

		// data source is not a server/table combi
		Table primaryTable = null;
		try
		{
			primaryTable = getPrimaryTable();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return primaryTable == null ? null : primaryTable.getName();
	}

	public Table getPrimaryTable() throws RepositoryException
	{
		return getTable(primaryDataSource);
	}

	public IServer getPrimaryServer() throws RepositoryException, RemoteException
	{
		if (primaryDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(primaryDataSource);
		if (stn != null)
		{
			return getRootObject().getServer(stn[0]);
		}

		ITable primaryTable = getPrimaryTable();
		if (primaryTable != null)
		{
			return getRootObject().getServer(primaryTable.getServerName());
		}
		return null;
	}

	/**
	 * Set the foreignTableName
	 * 
	 * @param arg the foreignTableName
	 */
	public void setForeignTableName(String arg)
	{
		setForeignDataSource(DataSourceUtils.createDBTableDataSource(getForeignServerName(), arg));
	}

	public String getForeignTableName()
	{
		if (foreignDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(foreignDataSource);
		if (stn != null)
		{
			return stn[1];
		}

		// data source is not a server/table combi
		Table foreignTable = null;
		try
		{
			foreignTable = getForeignTable();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return foreignTable == null ? null : foreignTable.getName();
	}

	/**
	 * A String which specified a set of sort options for the initial sorting of data
	 * retrieved through this relation.
	 * 
	 * Has the form "column_name asc, another_column_name desc, ...".
	 */
	public String getInitialSort()
	{
		return initialSort;
	}

	/**
	 * Sets the sortOptions.
	 * 
	 * @param initialSort The sortOptions to set
	 */
	public void setInitialSort(String arg)
	{
		checkForChange(initialSort, arg);
		initialSort = arg;
	}


	/**
	 * Gets the duplicateRelatedRecords.
	 * 
	 * @return Returns a boolean
	 */
	public boolean getDuplicateRelatedRecords()
	{
		return duplicateRelatedRecords;
	}

	/**
	 * Sets the duplicateRelatedRecords.
	 * 
	 * @param duplicateRelatedRecords The options to set
	 */
	public void setDuplicateRelatedRecords(boolean arg)
	{
		checkForChange(duplicateRelatedRecords, arg);
		duplicateRelatedRecords = arg;
	}

	/**
	 * Set the deleteRelatedRecords
	 * 
	 * @param arg the deleteRelatedRecords
	 */
	public void setDeleteRelatedRecords(boolean arg)
	{
		checkForChange(deleteRelatedRecords, arg);
		deleteRelatedRecords = arg;
	}

	/**
	 * Flag that tells if related records should be deleted or not when a parent record is deleted.
	 * 
	 * The default value of this flag is "false".
	 */
	public boolean getDeleteRelatedRecords()
	{
		return deleteRelatedRecords;
	}

	/**
	 * Set the existsInDB
	 * 
	 * @param arg the existsInDB
	 */
	public void setExistsInDB(boolean arg)
	{
		checkForChange(existsInDB, arg);
		existsInDB = arg;
	}

	/**
	 * Get the existsInDB
	 * 
	 * @return the existsInDB
	 */
	public boolean getExistsInDB()
	{
		return existsInDB;
	}

	public void setAllowCreationRelatedRecords(boolean arg)
	{
		checkForChange(allowCreationRelatedRecords, arg);
		allowCreationRelatedRecords = arg;
	}

	/**
	 * Flag that tells if related records can be created through this relation.
	 * 
	 * The default value of this flag is "false".
	 */
	public boolean getAllowCreationRelatedRecords()
	{
		return allowCreationRelatedRecords;
	}

	public void setAllowParentDeleteWhenHavingRelatedRecords(boolean arg)
	{
		checkForChange(allowParentDeleteWhenHavingRelatedRecords, arg);
		allowParentDeleteWhenHavingRelatedRecords = arg;
	}

	/**
	 * Flag that tells if the parent record can be deleted while it has related records.
	 * 
	 * The default value of this flag is "true".
	 */
	public boolean getAllowParentDeleteWhenHavingRelatedRecords()
	{
		return allowParentDeleteWhenHavingRelatedRecords;
	}

	public int getSize()
	{
		return getAllObjectsAsList().size();
	}

	public IDataProvider[] getPrimaryDataProviders(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		if (primary == null)
		{
			makePrimaryDataProviders(dataProviderHandler);
		}
		return primary;
	}

	public Column[] getForeignColumns() throws RepositoryException
	{
		if (foreign == null)
		{
			makeForeignColumns();
		}
		return foreign;
	}

	private Table getTable(String dataSource) throws RepositoryException
	{
		if (dataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		if (stn != null)
		{
			try
			{
				IServer server = getRootObject().getServer(stn[0]);
				if (server == null)
				{
					valid = false;
					throw new RepositoryException(Messages.getString("servoy.exception.serverNotFound", new Object[] { stn[0] })); //$NON-NLS-1$
				}
				return (Table)server.getTable(stn[1]);
			}
			catch (RemoteException e)
			{
				Debug.error(e);
				return null;
			}
		}

		// not a server/table combi, ask the current clients foundset manager
		if (J2DBGlobals.getServiceProvider() != null)
		{
			return (Table)J2DBGlobals.getServiceProvider().getFoundSetManager().getTable(dataSource);
		}

		// developer
		return null;
	}

	public Table getForeignTable() throws RepositoryException
	{
		return getTable(foreignDataSource);
	}

	public IServer getForeignServer() throws RepositoryException, RemoteException
	{
		if (foreignDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtils.getDBServernameTablename(foreignDataSource);
		if (stn != null)
		{
			return getRootObject().getServer(stn[0]);
		}

		ITable foreignTable = getForeignTable();
		if (foreignTable != null)
		{
			return getRootObject().getServer(foreignTable.getServerName());
		}
		return null;
	}

	public boolean isUsableInSort()
	{
		if (!isUsableInSearch())
		{
			return false;
		}
		if (joinType != ISQLJoin.INNER_JOIN)
		{ // outer joins icw or-null modifiers do not work (oracle) or looses outer join (ansi)
			for (int operator : getOperators())
			{
				if ((operator & ISQLCondition.ORNULL_MODIFIER) != 0)
				{
					return false;
				}
			}
		}
		return true;
	}

	public boolean isUsableInSearch()
	{
		return valid && !isMultiServer() && !isGlobal();
	}

	public boolean isMultiServer()
	{
		String primaryServerName = getPrimaryServerName();
		return (primaryServerName == null || !primaryServerName.equals(getForeignServerName()));
	}

	//creates real object relations also does some checks
	private void makePrimaryDataProviders(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		if (primary != null) return;

		List<IPersist> allobjects = getAllObjectsAsList();
		IDataProvider[] p = new IDataProvider[allobjects.size()];
		Table pt = null;
		RepositoryException exception = null;

		for (int pos = 0; pos < allobjects.size(); pos++)
		{
			RelationItem ri = (RelationItem)allobjects.get(pos);
			String pdp = ri.getPrimaryDataProviderID();
			IDataProvider pc = null;

			if (pdp.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				pc = dataProviderHandler.getGlobalDataProvider(pdp);
				if (pc != null)
				{
					p[pos] = pc;
				}
				else
				{
					if (exception == null) exception = new RepositoryException(Messages.getString(
						"servoy.relation.error.dataproviderDoesntExist", new Object[] { ri.getPrimaryDataProviderID(), ri.getForeignColumnName(), getName() })); //$NON-NLS-1$
				}
			}
			else
			{
				if (pt == null)
				{
					pt = getPrimaryTable();
				}

				if (pt != null)
				{
					pc = dataProviderHandler.getDataProviderForTable(pt, pdp);
					if (pc != null)
					{
						p[pos] = pc;
					}
					else
					{
						if (exception == null) exception = new RepositoryException(
							Messages.getString(
								"servoy.relation.error.dataproviderDoesntExist", new Object[] { ri.getPrimaryDataProviderID(), ri.getForeignColumnName(), getName() })); //$NON-NLS-1$
					}
				}
				else
				{
					if (exception == null) exception = new RepositoryException(Messages.getString(
						"servoy.relation.error.tableDoesntExist", new Object[] { getPrimaryTableName(), getForeignTableName(), getName() })); //$NON-NLS-1$
				}
			}
		}
		primary = p;

		if (exception != null)
		{
			valid = false;
			throw exception;
		}

	}

	private void makeForeignColumns() throws RepositoryException
	{
		if (foreign != null) return;

		List<IPersist> allobjects = getAllObjectsAsList();
		Column[] f = new Column[allobjects.size()];
		Table ft = null;
		RepositoryException exception = null;

		for (int pos = 0; pos < allobjects.size(); pos++)
		{
			RelationItem ri = (RelationItem)allobjects.get(pos);
			if (ft == null)
			{
				ft = getTable(foreignDataSource);
			}

			if (ft != null)
			{
				Column fc = ft.getColumn(ri.getForeignColumnName());
				if (fc != null)
				{
					f[pos] = fc;
				}
				else
				{
					if (exception == null) exception = new RepositoryException(Messages.getString("servoy.relation.error.dataproviderDoesntExist",
						new Object[] { ri.getPrimaryDataProviderID(), ri.getForeignColumnName(), getName() }));
				}
			}
			else
			{
				if (exception == null) exception = new RepositoryException(Messages.getString("servoy.relation.error.tableDoesntExist",
					new Object[] { getPrimaryTableName(), getForeignTableName(), getName() }));
			}
		}
		foreign = f;

		if (exception != null)
		{
			valid = false;
			throw exception;
		}
	}

	public boolean isValid()
	{
		return valid;
	}

	public boolean valid = true;

	public void setValid(boolean b)
	{
		valid = b;
		if (b)//clear so they are checked again
		{
			primary = null;
			foreign = null;
			operators = null;
			isGlobal = null;
		}
	}

	public int[] getOperators()
	{
		if (operators == null)
		{
			List<IPersist> allobjects = getAllObjectsAsList();
			int size = 0;
			if (primary != null)
			{
				size = primary.length;
			}
			else
			{
				size = allobjects.size();
			}
			operators = new int[size];
			if (allobjects != null)
			{
				for (int pos = 0; pos < allobjects.size(); pos++)
				{
					RelationItem ri = (RelationItem)allobjects.get(pos);
					operators[pos] = ri.getOperator();
				}
			}
		}
		return operators;
	}

	public boolean isParentRef()
	{
		return primaryDataSource != null && primaryDataSource.equals(foreignDataSource) && getAllObjectsAsList().size() == 0;
	}

	/**
	 * Does the relation always relate to the same record?
	 * 
	 * @return true if the relation is a FK->PK relation on the same data source.
	 * @throws RepositoryException
	 */
	public boolean isExactPKRef(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		return primaryDataSource != null && primaryDataSource.equals(foreignDataSource) // same data source
			&& isFKPKRef() // FK to itself
			&& Arrays.equals(getPrimaryDataProviders(dataProviderHandler), getForeignColumns());
	}

	/**
	 * Does the relation define a FK->PK relation?
	 * 
	 * @throws RepositoryException
	 */
	public boolean isFKPKRef() throws RepositoryException
	{
		getForeignColumns();
		if (foreign == null || foreign.length == 0)
		{
			return false;
		}

		getOperators();
		for (int element : operators)
		{
			if (element != ISQLCondition.EQUALS_OPERATOR)
			{
				return false;
			}
		}

		return Arrays.equals(foreign[0].getTable().getRowIdentColumns().toArray(), foreign);
	}

	public String checkKeyTypes(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		if (primary == null)
		{
			//make sure they are loaded
			getPrimaryDataProviders(dataProviderHandler);
			getForeignColumns();
		}

		if (primary != null && foreign != null)
		{
			for (int i = 0; i < primary.length; i++)
			{
				if (primary[i] == null || foreign[i] == null)
				{
					return Messages.getString("servoy.relation.error"); //$NON-NLS-1$
				}

				if (Column.mapToDefaultType(primary[i].getDataProviderType()) == IColumnTypes.INTEGER &&
					Column.mapToDefaultType(foreign[i].getDataProviderType()) == IColumnTypes.NUMBER)
				{
					continue;//allow integer to number mappings
				}
				if (Column.mapToDefaultType(primary[i].getDataProviderType()) == IColumnTypes.NUMBER &&
					Column.mapToDefaultType(foreign[i].getDataProviderType()) == IColumnTypes.INTEGER)
				{
					continue;//allow number to integer mappings
				}
				else if (Column.mapToDefaultType(primary[i].getDataProviderType()) != Column.mapToDefaultType(foreign[i].getDataProviderType()))
				{
					return Messages.getString(
						"servoy.relation.error.typeDoesntMatch", new Object[] { primary[i].getDataProviderID(), foreign[i].getDataProviderID() }); //$NON-NLS-1$
				}
			}
		}
		return null;
	}

	private transient Boolean isGlobal;

	public boolean isGlobal()
	{
		if (isGlobal == null)
		{
			isGlobal = Boolean.valueOf(isGlobalEx());
		}
		return isGlobal.booleanValue();
	}

	/**
	 * @return true if entirely global
	 */
	private boolean isGlobalEx()
	{
		if (!valid) return false;//don't know

		List<IPersist> allobjects = getAllObjectsAsList();
		if (allobjects.size() == 0) return false;
		for (int pos = 0; pos < allobjects.size(); pos++)
		{
			RelationItem ri = (RelationItem)allobjects.get(pos);
			String pdp = ri.getPrimaryDataProviderID();
			if (!pdp.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				return false;
			}
		}
		return true;
	}

	public String toHTML()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("<html>Name(solution): <b>"); //$NON-NLS-1$
		sb.append(getName());
		sb.append(" ("); //$NON-NLS-1$
		sb.append(getRootObject().getName());
		sb.append(")");
		if (isGlobal())
		{
			sb.append("</b><br>Global relation <b>"); //$NON-NLS-1$
		}
		sb.append("</b><br><br>From: <b>"); //$NON-NLS-1$
		sb.append(getPrimaryServerName());
		sb.append(" - ");
		sb.append(getPrimaryTableName());
		sb.append("</b><br>To: <b>"); //$NON-NLS-1$
		sb.append(getForeignServerName());
		sb.append(" - ");
		sb.append(getForeignTableName());
		sb.append("</b><br>"); //$NON-NLS-1$
		sb.append("<br>"); //$NON-NLS-1$
		List<IPersist> allobjects = getAllObjectsAsList();
		int size = allobjects.size();
		for (int i = 0; i < size; i++)
		{
			RelationItem ri = (RelationItem)allobjects.get(i);
			sb.append(ri.getPrimaryDataProviderID());
			sb.append(" <font color=\"red\">"); //$NON-NLS-1$
			sb.append(RelationItem.getOperatorAsString(ri.getOperator()).replaceAll("<", "&lt;")); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("</font> "); //$NON-NLS-1$
			sb.append(ri.getForeignColumnName());
			if (i < size - 1) sb.append("<br>"); //$NON-NLS-1$
		}
		sb.append("</html>"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * The join type that is performed between the primary table and the foreign table.
	 * Can be "inner join" or "left outer join".
	 */
	public int getJoinType()
	{
		return this.joinType;
	}

	public void setJoinType(int JoinType)
	{
		checkForChange(this.joinType, JoinType);
		this.joinType = JoinType;
	}
}
