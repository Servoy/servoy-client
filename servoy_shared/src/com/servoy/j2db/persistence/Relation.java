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


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.query.ISQLTableJoin;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;

/**
 * A relation (between 2 tables on one or more key column pairs)
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, typeCode = IRepository.RELATIONS)
@ServoyClientSupport(mc = true, wc = true, sc = true)
public class Relation extends AbstractBase implements ISupportChilds, ISupportUpdateableName, ISupportHTMLToolTipText, ISupportContentEquals,
	ISupportEncapsulation, ICloneable, IRelation, ISupportDeprecated
{

	private static final long serialVersionUID = 1L;

	public static final String INTERNAL_PREFIX = "-int-";

	public static RuntimeProperty<ISQLTableJoin> RELATION_JOIN = new RuntimeProperty<ISQLTableJoin>()
	{
	};


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
				obj.setForeignColumnName(foreignColumns[i].getDataProviderID());
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

		flushCashedItems();
		primary = primaryDataProvider; //faster
		foreign = foreignColumns; //faster
		operators = ops; //faster
	}


	public RelationItem createNewRelationItem(IFoundSetManagerInternal foundSetManager, IDataProvider primaryDataProvider, int ops, Column foreignColumn)
		throws RepositoryException
	{
		if (!foreignColumn.getTable().equals(foundSetManager.getTable(getForeignDataSource())))
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

		flushCashedItems();

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
				return (getName().equals(other.getName()) && getDeleteRelatedRecords() == other.getDeleteRelatedRecords() &&
					getAllowCreationRelatedRecords() == other.getAllowCreationRelatedRecords());
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
		return getName();
	}

	public void updateName(IValidateName validator, String name) throws RepositoryException
	{
		validator.checkName(name, getID(), new ValidatorSearchContext(IRepository.RELATIONS), true);
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, name);
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
	 * The name of the relation.
	 */
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * Set the primary data source
	 */
	public void setPrimaryDataSource(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PRIMARYDATASOURCE, arg);
	}

	/**
	 * Qualified name of the primary data source. Contains both the name of the primary server
	 * and the name of the primary table.
	 */
	public String getPrimaryDataSource()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_PRIMARYDATASOURCE);
	}

	/**
	 * Set the foreign data source
	 */
	public void setForeignDataSource(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FOREIGNDATASOURCE, arg);
	}

	/**
	 * Qualified name of the foreign data source. Contains both the name of the foreign
	 * server and the name of the foreign table.
	 */
	public String getForeignDataSource()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FOREIGNDATASOURCE);
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
		String primaryDataSource = getPrimaryDataSource();
		if (primaryDataSource == null)
		{
			return null;
		}
		String[] snt = DataSourceUtilsBase.getDBServernameTablename(primaryDataSource);
		if (snt != null)
		{
			return snt[0];
		}
		if (DataSourceUtils.getInmemDataSourceName(primaryDataSource) != null)
		{
			return IServer.INMEM_SERVER;
		}
		// can return null if it is not a db server/table combi
		return null;
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
		String foreignDataSource = getForeignDataSource();
		if (foreignDataSource == null)
		{
			return null;
		}
		String[] stn = DataSourceUtilsBase.getDBServernameTablename(foreignDataSource);
		if (stn != null)
		{
			return stn[0];
		}
		if (DataSourceUtils.getInmemDataSourceName(foreignDataSource) != null)
		{
			return IServer.INMEM_SERVER;
		}
		// can return null if it is not a DB server/table combi
		return null;
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
		String primaryDataSource = getPrimaryDataSource();
		if (primaryDataSource == null)
		{
			return null;
		}
		String tableName = DataSourceUtils.getDataSourceTableName(primaryDataSource);
		if (tableName != null)
		{
			return tableName;
		}
		String inmemDataSourceName = DataSourceUtils.getInmemDataSourceName(primaryDataSource);
		if (inmemDataSourceName != null)
		{
			return inmemDataSourceName;
		}
		// will return null when data source is not a server/table combi
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
		String foreignDataSource = getForeignDataSource();
		if (foreignDataSource == null)
		{
			return null;
		}
		String tableName = DataSourceUtils.getDataSourceTableName(foreignDataSource);
		if (tableName != null)
		{
			return tableName;
		}
		String inmemDataSourceName = DataSourceUtils.getInmemDataSourceName(foreignDataSource);
		if (inmemDataSourceName != null)
		{
			return inmemDataSourceName;
		}
		// can return null when data source is not a server/table combi
		return null;
	}

	/**
	 * A String which specified a set of sort options for the initial sorting of data
	 * retrieved through this relation.
	 *
	 * Has the form "column_name asc, another_column_name desc, ...".
	 */
	public String getInitialSort()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_INITIALSORT);
	}

	/**
	 * Sets the sortOptions.
	 *
	 * @param initialSort The sortOptions to set
	 */
	public void setInitialSort(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_INITIALSORT, arg);
	}


	/**
	 * Gets the duplicateRelatedRecords.
	 *
	 * @return Returns a boolean
	 */
	public boolean getDuplicateRelatedRecords()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DUPLICATERELATEDRECORDS).booleanValue();
	}

	/**
	 * Sets the duplicateRelatedRecords.
	 *
	 * @param duplicateRelatedRecords The options to set
	 */
	public void setDuplicateRelatedRecords(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DUPLICATERELATEDRECORDS, arg);
	}

	/**
	 * Set the deleteRelatedRecords
	 *
	 * @param arg the deleteRelatedRecords
	 */
	public void setDeleteRelatedRecords(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DELETERELATEDRECORDS, arg);
	}

	/**
	 * Flag that tells if related records should be deleted or not when a parent record is deleted.
	 *
	 * The default value of this flag is "false".
	 */
	public boolean getDeleteRelatedRecords()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DELETERELATEDRECORDS).booleanValue();
	}

	/**
	 * Set the existsInDB
	 *
	 * @param arg the existsInDB
	 */
	public void setExistsInDB(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_EXISTSINDB, arg);
	}

	/**
	 * Get the existsInDB
	 *
	 * @return the existsInDB
	 */
	public boolean getExistsInDB()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_EXISTSINDB).booleanValue();
	}

	public void setAllowCreationRelatedRecords(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ALLOWCREATIONRELATEDRECORDS, arg);
	}

	/**
	 * Flag that tells if related records can be created through this relation.
	 *
	 * The default value of this flag is "false".
	 */
	public boolean getAllowCreationRelatedRecords()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ALLOWCREATIONRELATEDRECORDS).booleanValue();
	}

	public void setAllowParentDeleteWhenHavingRelatedRecords(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ALLOWPARENTDELETEWHENHAVINGRELATEDRECORDS, arg);
	}

	/**
	 * Flag that tells if the parent record can be deleted while it has related records.
	 *
	 * The default value of this flag is "true".
	 */
	public boolean getAllowParentDeleteWhenHavingRelatedRecords()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ALLOWPARENTDELETEWHENHAVINGRELATEDRECORDS).booleanValue();
	}

	public int getItemCount()
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

	public Column[] getForeignColumns(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		if (foreign == null)
		{
			makeForeignColumns(dataProviderHandler);
		}
		return foreign;
	}

	public boolean isUsableInSort()
	{
		if (!isUsableInSearch())
		{
			return false;
		}
		if (getJoinType() != ISQLJoin.INNER_JOIN)
		{ // outer joins icw or-null modifiers do not work (oracle) or looses outer join (ansi)
			for (int operator : getOperators())
			{
				if ((operator & IBaseSQLCondition.ORNULL_MODIFIER) != 0)
				{
					return false;
				}
			}
		}
		return true;
	}

	public boolean isUsableInSearch()
	{
		return isValid() && !isMultiServer() && !isGlobal();
	}

	public boolean isMultiServer()
	{
		return !DataSourceUtils.isSameServer(getPrimaryDataSource(), getForeignDataSource());
	}

	//creates real object relations also does some checks
	private void makePrimaryDataProviders(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		if (primary != null) return;

		List<IPersist> allobjects = getAllObjectsAsList();
		IDataProvider[] p = new IDataProvider[allobjects.size()];
		ITable pt = null;
		RepositoryException exception = null;

		for (int pos = 0; pos < allobjects.size(); pos++)
		{
			RelationItem ri = (RelationItem)allobjects.get(pos);
			String pdp = ri.getPrimaryDataProviderID();

			if (ScopesUtils.isVariableScope(pdp))
			{
				IDataProvider pc = dataProviderHandler.getGlobalDataProvider(pdp);
				if (pc != null)
				{
					p[pos] = pc;
				}
				else if (exception == null)
				{
					exception = new RepositoryException(Messages.getString("servoy.relation.error.dataproviderDoesntExist", //$NON-NLS-1$
						new Object[] { ri.getPrimaryDataProviderID(), ri.getForeignColumnName(), getName() }));
				}
			}
			else if (pdp != null && pdp.startsWith(LiteralDataprovider.LITERAL_PREFIX))
			{
				p[pos] = new LiteralDataprovider(pdp);
			}
			else
			{
				if (pt == null)
				{
					pt = dataProviderHandler.getTable(getPrimaryDataSource());
				}

				if (pt != null)
				{
					IDataProvider pc = dataProviderHandler.getDataProviderForTable(pt, pdp);
					if (pc != null)
					{
						p[pos] = pc;
					}
					else if (exception == null)
					{
						exception = new RepositoryException(Messages.getString("servoy.relation.error.dataproviderDoesntExist", //$NON-NLS-1$
							new Object[] { ri.getPrimaryDataProviderID(), ri.getForeignColumnName(), getName() }));
					}
				}
				else if (exception == null)
				{
					exception = new RepositoryException(
						Messages.getString("servoy.relation.error.tableDoesntExist", new Object[] { getPrimaryTableName(), getForeignTableName(), getName() })); //$NON-NLS-1$
				}
			}
		}
		primary = p;

		if (exception != null)
		{
			valid = Boolean.FALSE;
			throw exception;
		}

	}

	private void makeForeignColumns(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		if (foreign != null) return;

		List<IPersist> allobjects = getAllObjectsAsList();
		Column[] f = new Column[allobjects.size()];
		ITable ft = null;
		RepositoryException exception = null;

		for (int pos = 0; pos < allobjects.size(); pos++)
		{
			RelationItem ri = (RelationItem)allobjects.get(pos);
			if (ft == null)
			{
				ft = dataProviderHandler.getTable(getForeignDataSource());
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
					if (exception == null) exception = new RepositoryException(Messages.getString("servoy.relation.error.dataproviderDoesntExist", //$NON-NLS-1$
						new Object[] { ri.getPrimaryDataProviderID(), ri.getForeignColumnName(), getName() }));
				}
			}
			else
			{
				if (exception == null) exception = new RepositoryException(Messages.getString("servoy.relation.error.tableDoesntExist", //$NON-NLS-1$
					new Object[] { getPrimaryTableName(), getForeignTableName(), getName() }));
			}
		}
		foreign = f;

		if (exception != null)
		{
			valid = Boolean.FALSE;
			throw exception;
		}
	}

	public Boolean valid = null;

	public void setValid(boolean b)
	{
		if (b)//clear so they are checked again
		{
			flushCashedItems();
		}
		valid = Boolean.valueOf(b);
	}

	private boolean isValid()
	{
		if (valid != null) return valid.booleanValue();
		// default true
		return true;
	}

	public void flushCashedItems()
	{
		primary = null;
		foreign = null;
		operators = null;
		isGlobal = null;
		usedScopes = null;
		isLiteral = null;
		valid = null;
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
		String primaryDataSource = getPrimaryDataSource();
		return primaryDataSource != null && primaryDataSource.equals(getForeignDataSource()) && getAllObjectsAsList().size() == 0;
	}

	/**
	 * Does the relation always relate to the same record?
	 *
	 * @return true if the relation is a FK->PK relation on the same data source.
	 * @throws RepositoryException
	 */
	public boolean isExactPKRef(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		String primaryDataSource = getPrimaryDataSource();
		return primaryDataSource != null && primaryDataSource.equals(getForeignDataSource()) // same data source
		&& isFKPKRef(dataProviderHandler) // FK to itself
		&& Arrays.equals(getPrimaryDataProviders(dataProviderHandler), getForeignColumns(dataProviderHandler));
	}

	/**
	 * Does the relation define a FK->PK relation?
	 *
	 * @throws RepositoryException
	 */
	public boolean isFKPKRef(IDataProviderHandler dataProviderHandler) throws RepositoryException
	{
		getForeignColumns(dataProviderHandler);
		if (foreign == null || foreign.length == 0)
		{
			return false;
		}

		getOperators();
		for (int element : operators)
		{
			if (element != IBaseSQLCondition.EQUALS_OPERATOR)
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
			getForeignColumns(dataProviderHandler);
		}

		if (primary != null && foreign != null)
		{
			for (int i = 0; i < primary.length; i++)
			{
				if (primary[i] == null || foreign[i] == null)
				{
					return Messages.getString("servoy.relation.error"); //$NON-NLS-1$
				}
				if (primary[i] instanceof LiteralDataprovider)
				{
					Object value = ((LiteralDataprovider)primary[i]).getValue(foreign[i].getDataProviderType());
					try
					{
						if (value != null)
						{
							Column.getAsRightType(foreign[i].getDataProviderType(), foreign[i].getFlags(), value, foreign[i].getLength(), true);
							continue;
						}
					}
					catch (Exception e)
					{
					}
					return Messages.getString("servoy.relation.error.literalInvalidForColumn", //$NON-NLS-1$
						new Object[] { ((LiteralDataprovider)primary[i]).getLiteral(), foreign[i].getDataProviderID() });
				}

				int primaryType = Column.mapToDefaultType(primary[i].getDataProviderType());
				int foreignType = Column.mapToDefaultType(foreign[i].getDataProviderType());
				if (primaryType == IColumnTypes.INTEGER && foreignType == IColumnTypes.NUMBER)
				{
					continue; //allow integer to number mappings
				}
				if (primaryType == IColumnTypes.NUMBER && foreignType == IColumnTypes.INTEGER)
				{
					continue; //allow number to integer mappings
				}
				if (foreignType == IColumnTypes.INTEGER && primary[i] instanceof AbstractBase &&
					"Boolean".equals(((AbstractBase)primary[i]).getSerializableRuntimeProperty(IScriptProvider.TYPE))) //$NON-NLS-1$
				{
					continue; //allow boolean var to number mappings
				}
				if (primaryType != foreignType)
				{
					return Messages.getString("servoy.relation.error.typeDoesntMatch", //$NON-NLS-1$
						new Object[] { primary[i].getDataProviderID(), foreign[i].getDataProviderID() });
				}
			}
		}
		return null;
	}

	private transient Boolean isGlobal;
	private transient Boolean isLiteral;

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
		if (!isValid()) return false;//don't know

		List<IPersist> allobjects = getAllObjectsAsList();
		if (allobjects.size() == 0) return false;
		for (IPersist ri : allobjects)
		{
			String primaryDataProviderID = ((RelationItem)ri).getPrimaryDataProviderID();
			if (primaryDataProviderID != null)
			{
				if (!ScopesUtils.isVariableScope(primaryDataProviderID) && !primaryDataProviderID.startsWith(LiteralDataprovider.LITERAL_PREFIX))
				{
					return false;
				}
			}
			else
			{
				Debug.error("Relation '" + getName() + "' has NULL primary dataprovider in a relation item. Solution: " + getRootObject()); //$NON-NLS-1$//$NON-NLS-2$
				throw new NullPointerException();
			}
		}
		return true;
	}

	public boolean containsGlobal()
	{
		if (isValid())
		{
			List<IPersist> allobjects = getAllObjectsAsList();
			if (allobjects.size() == 0) return false;
			for (IPersist ri : allobjects)
			{
				String primaryDataProviderID = ((RelationItem)ri).getPrimaryDataProviderID();
				if (primaryDataProviderID != null)
				{
					if (ScopesUtils.isVariableScope(primaryDataProviderID) || primaryDataProviderID.startsWith(LiteralDataprovider.LITERAL_PREFIX))
					{
						return true;
					}
				}
				else
				{
					Debug.error("Relation '" + getName() + "' has NULL primary dataprovider in a relation item. Solution: " + getRootObject()); //$NON-NLS-1$//$NON-NLS-2$
					throw new NullPointerException();
				}
			}
		}
		return false;
	}

	public boolean isLiteral()
	{
		if (isLiteral == null)
		{
			isLiteral = Boolean.valueOf(isLiteralEx());
		}
		return isLiteral.booleanValue();
	}

	private boolean isLiteralEx()
	{
		if (!isValid()) return false;//don't know

		List<IPersist> allobjects = getAllObjectsAsList();
		if (allobjects.size() == 0) return false;
		for (IPersist ri : allobjects)
		{
			String primaryDataProviderID = ((RelationItem)ri).getPrimaryDataProviderID();
			if (!primaryDataProviderID.startsWith(LiteralDataprovider.LITERAL_PREFIX))
			{
				return false;
			}
		}
		return true;
	}

	public boolean isInternal()
	{
		String name = getName();
		return name != null && name.startsWith(INTERNAL_PREFIX);
	}

	private transient Set<String> usedScopes;

	public boolean usesScope(String scopeName)
	{
		if (!isGlobal())
		{
			// scopes are only for global relations
			return false;
		}

		Set<String> tmp = usedScopes;
		if (tmp == null)
		{
			List<IPersist> allobjects = getAllObjectsAsList();
			if (allobjects.size() == 0) return false;
			tmp = new HashSet<String>();
			for (IPersist ri : allobjects)
			{
				Pair<String, String> scope = ScopesUtils.getVariableScope(((RelationItem)ri).getPrimaryDataProviderID());
				if (scope.getLeft() != null)
				{
					tmp.add(scope.getLeft());
				}
			}
			usedScopes = tmp;
		}
		return tmp.contains(scopeName);
	}

	public String toHTML()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html>Name(solution): <b>"); //$NON-NLS-1$
		sb.append(getName());
		sb.append(" ("); //$NON-NLS-1$
		sb.append(getRootObject().getName());
		sb.append(')');
		if (isGlobal())
		{
			sb.append("</b><br>Global relation <b>"); //$NON-NLS-1$
		}
		sb.append("</b><br><br>From: <b>"); //$NON-NLS-1$
		sb.append(getPrimaryServerName());
		sb.append(" - "); //$NON-NLS-1$
		sb.append(getPrimaryTableName());
		sb.append("</b><br>To: <b>"); //$NON-NLS-1$
		sb.append(getForeignServerName());
		sb.append(" - "); //$NON-NLS-1$
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
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_JOINTYPE).intValue();
	}

	public void setJoinType(int JoinType)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_JOINTYPE, JoinType);
	}

	@Override
	public void setEncapsulation(int arg)
	{
		int newAccess = arg;
		int access = getEncapsulation();
		if ((newAccess & PersistEncapsulation.MODULE_SCOPE) == PersistEncapsulation.MODULE_SCOPE &&
			(newAccess & PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE) == PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE)
		{
			if ((access & PersistEncapsulation.MODULE_SCOPE) == PersistEncapsulation.MODULE_SCOPE) newAccess = newAccess ^ PersistEncapsulation.MODULE_SCOPE;
			else newAccess = newAccess ^ PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE;
		}
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ENCAPSULATION, newAccess);
	}

	/**
	 * The encapsulation mode of this Relation. The following can be used/checked:
	 *
	 * - Public (not a separate option - if none of the below options are selected)
	 * - Hide in scripting; Module Scope - not available in scripting from any other context except the form itself. Available in designer for the same module.
	 * - Module Scope - available in both scripting and designer but only in the same module.
	 *
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
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DEPRECATED, deprecatedInfo);
	}

	public static boolean isValid(Relation relation, IDataProviderHandler handler)
	{
		if (relation == null) return false;
		// TODO can this valid really be stored in the relation.
		// can we have the same relation that is valid in 1 client but not in the other?
		if (relation.valid == null && relation.getForeignDataSource() != null)
		{
			try
			{
				IServer server = handler.getServer(relation.getForeignDataSource());
				ITable table = handler.getTable(relation.getForeignDataSource());
				relation.valid = Boolean.valueOf(server != null && server.isValid() && table != null);
			}
			catch (Exception e)
			{
				relation.valid = Boolean.FALSE;
			}
		}
		// default to true
		return relation.valid == null || relation.valid.booleanValue();
	}
}
