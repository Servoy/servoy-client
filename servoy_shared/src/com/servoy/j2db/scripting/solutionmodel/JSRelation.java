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
package com.servoy.j2db.scripting.solutionmodel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.SQLSheet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSRelation implements IJSParent<Relation>, IConstantsObject
{

	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSRelation.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return null;
			}
		});
	}

	/**
	 * Constant for set/get the joinType of a JSRelation. It is also used in solutionModel.newRelation(...).
	 *
	 * @sample 
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.joinType = JSRelation.LEFT_OUTER_JOIN;
	 */
	public static final int INNER_JOIN = ISQLJoin.INNER_JOIN;

	/**
	 * @sameas INNER_JOIN
	 */
	public static final int LEFT_OUTER_JOIN = ISQLJoin.LEFT_OUTER_JOIN;

	private boolean isCopy = false;

	private Relation relation;
	private final IApplication application;

	/**
	 * @param relation
	 */
	public JSRelation(Relation relation, IApplication application, boolean isNew)
	{
		this.relation = relation;
		this.application = application;
		this.isCopy = isNew;
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getSupportChild()
	 */
	public Relation getSupportChild()
	{
		return relation;
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getJSParent()
	 */
	public IJSParent< ? > getJSParent()
	{
		return null;
	}

	/**
	 * Returns an array of JSRelationItem objects representing the relation criteria defined for this relation.
	 * 
	 * @sample
	 * var criteria = relation.getRelationItems();
	 * for (var i=0; i<criteria.length; i++)
	 * {
	 *	var item = criteria[i];
	 *	application.output('relation item no. ' + i);
	 *	application.output('primary column: ' + item.primaryDataProviderID);
	 *	application.output('operator: ' + item.operator);
	 *	application.output('foreign column: ' + item.foreignColumnName);
	 * }
	 * 
	 * @return An array of JSRelationItem instances representing the relation criteria of this relation.
	 */
	public JSRelationItem[] js_getRelationItems()
	{
		ArrayList<JSRelationItem> al = new ArrayList<JSRelationItem>();
		Iterator<IPersist> allObjects = relation.getAllObjects();
		while (allObjects.hasNext())
		{
			IPersist persist = allObjects.next();
			if (persist instanceof RelationItem)
			{
				al.add(new JSRelationItem((RelationItem)persist, this, isCopy));
			}
		}
		return al.toArray(new JSRelationItem[al.size()]);
	}

	/**
	 * Creates a new relation item for this relation. The primary dataprovider, the foreign data provider 
	 * and one relation operators (like '=' '!=' '>' '<') must be provided.
	 *
	 * @sample 
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.newRelationItem('another_parent_table_id', '=', 'another_child_table_parent_id');
	 *
	 * @param dataprovider The name of the primary dataprovider. 
	 *
	 * @param operator The operator used to relate the primary and the foreign dataproviders. 
	 *
	 * @param foreinColumnName The name of the foreign dataprovider. 
	 * 
	 * @return A JSRelationItem instance representing the newly added relation item.
	 */
	public JSRelationItem js_newRelationItem(String dataprovider, String operator, String foreinColumnName)
	{
		checkModification();
		try
		{
			IDataProvider primaryDataProvider = null;
			if (ScopesUtils.isVariableScope(dataprovider))
			{
				primaryDataProvider = application.getFlattenedSolution().getGlobalDataProvider(dataprovider);
			}
			else
			{
				primaryDataProvider = application.getFlattenedSolution().getDataProviderForTable(
					(Table)application.getFoundSetManager().getTable(relation.getPrimaryDataSource()), dataprovider);
			}
			if (primaryDataProvider == null)
			{
				throw new IllegalArgumentException("cant create relation item primary dataprovider not found: " + dataprovider); //$NON-NLS-1$
			}

			int validOperator = RelationItem.getValidOperator(operator, RelationItem.RELATION_OPERATORS, null);
			if (validOperator == -1)
			{
				throw new IllegalArgumentException("operator " + operator + " is not a valid relation operator"); //$NON-NLS-1$//$NON-NLS-2$
			}

			IDataProvider dp = application.getFlattenedSolution().getDataProviderForTable(
				(Table)application.getFoundSetManager().getTable(relation.getForeignDataSource()), foreinColumnName);
			if (!(dp instanceof Column))
			{
				throw new IllegalArgumentException("Foreign columnname " + foreinColumnName + " is not a valid column"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			RelationItem result = relation.createNewRelationItem(application.getFoundSetManager(), primaryDataProvider, validOperator, (Column)dp);
			if (result != null) return new JSRelationItem(result, this, isCopy);
			else return null;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Removes the desired relation item from the specified relation.
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('myRelation', 'db:/myServer/parentTable', 'db:/myServer/childTable', JSRelation.INNER_JOIN);
	 * relation.newRelationItem('someColumn1', '=', 'someColumn2');
	 * relation.newRelationItem('anotherColumn', '=', 'someOtherColumn');
	 * relation.removeRelationItem('someColumn1', '=', 'someColumn2');
	 * var criteria = relation.getRelationItems();
	 * for (var i = 0; i < criteria.length; i++) {
	 * 	var item = criteria[i];
	 * 	application.output('primary column: ' + item.primaryDataProviderID);
	 * 	application.output('operator: ' + item.operator);
	 * 	application.output('foreign column: ' + item.foreignColumnName);
	 * }
	 * 
	 * @param primaryDataProviderID the primary data provider (column) name
	 * @param operator the operator 
	 * @param foreignColumnName the foreign column name
	 */
	public void js_removeRelationItem(String primaryDataProviderID, String operator, String foreignColumnName)
	{
		checkModification();
		Iterator<IPersist> allObjects = relation.getAllObjects();
		while (allObjects.hasNext())
		{
			IPersist persist = allObjects.next();
			if (persist instanceof RelationItem)
			{
				RelationItem ri = (RelationItem)persist;
				int validOperator = RelationItem.getValidOperator(operator, RelationItem.RELATION_OPERATORS, null);
				if (ri.getPrimaryDataProviderID().equals(primaryDataProviderID) && ri.getOperator() == validOperator &&
					ri.getForeignColumnName().equals(foreignColumnName))
				{
					relation.removeChild(persist);
					relation.setValid(true);
					break;
				}
			}
		}
	}


	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getAllowCreationRelatedRecords()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.allowCreationRelatedRecords = true;
	 */
	public boolean js_getAllowCreationRelatedRecords()
	{
		return relation.getAllowCreationRelatedRecords();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getAllowParentDeleteWhenHavingRelatedRecords()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.allowParentDeleteWhenHavingRelatedRecords = false;
	 */
	public boolean js_getAllowParentDeleteWhenHavingRelatedRecords()
	{
		return relation.getAllowParentDeleteWhenHavingRelatedRecords();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getDeleteRelatedRecords()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.deleteRelatedRecords = true;
	 */
	public boolean js_getDeleteRelatedRecords()
	{
		return relation.getDeleteRelatedRecords();
	}

//	@Deprecated
//	public boolean js_getDuplicateRelatedRecords()
//	{
//		return relation.getDuplicateRelatedRecords();
//	}

	/**
	 * The name of the server where the foreign table is located.
	 * 
	 * @deprecated As of release 5.1, replaced by {@link #getForeignDataSource()}.
	 *  
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSRelation#js_getPrimaryServerName()
	 */
	@Deprecated
	public String js_getForeignServerName()
	{
		return relation.getForeignServerName();
	}

	/**
	 * The name of the foreign table.
	 * 
	 * @deprecated As of release 5.1, replaced by {@link #getForeignDataSource()}.
	 * 
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSRelation#js_getPrimaryServerName()
	 */
	@Deprecated
	public String js_getForeignTableName()
	{
		return relation.getForeignTableName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getForeignDataSource()
	 * 
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSRelation#js_getPrimaryDataSource()
	 */
	public String js_getForeignDataSource()
	{
		return relation.getForeignDataSource();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getInitialSort()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.initialSort = 'another_child_table_text asc';
	 */
	public String js_getInitialSort()
	{
		return relation.getInitialSort();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getJoinType()
	 * 
	 * @sampleas INNER_JOIN
	 */
	public int js_getJoinType()
	{
		return relation.getJoinType();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getName()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.name = 'anotherName';
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.relationName = relation.name;
	 */
	public String js_getName()
	{
		return relation.getName();
	}

	/**
	 * The name of the server where the primary table is located.
	 * 
	 * @deprecated As of release 5.1, replaced by {@link #getPrimaryDataSource()}.
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.primaryTableName = 'another_parent_table';
	 * relation.primaryServerName = 'user_data';
	 * relation.foreignTableName = 'another_child_table';
	 * relation.foreignServerName = 'user_data';
	 */
	@Deprecated
	public String js_getPrimaryServerName()
	{
		return relation.getPrimaryServerName();
	}

	/**
	 * The name of the primary table.
	 * 
	 * @deprecated As of release 5.1, replaced by {@link #getPrimaryDataSource()}.
	 * 
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSRelation#js_getPrimaryServerName()
	 */
	@Deprecated
	public String js_getPrimaryTableName()
	{
		return relation.getPrimaryTableName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getPrimaryDataSource()
	 * 
	 * @sample
	 * 	var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.primaryDataSource = 'db:/user_data/another_parent_table';
	 * relation.foreignDataSource = 'db:/user_data/another_child_table';
	 */
	public String js_getPrimaryDataSource()
	{
		return relation.getPrimaryDataSource();
	}

//	public boolean js_isExactPKRef() throws RepositoryException
//	{
//		return relation.isExactPKRef(application.getFlattenedSolution());
//	}

//	public boolean js_isGlobal()
//	{
//		return relation.isGlobal();
//	}

//	public boolean js_isMultiServer()
//	{
//		return relation.isMultiServer();
//	}

//	public boolean js_isParentRef()
//	{
//		return relation.isParentRef();
//	}

//	public boolean js_isValid()
//	{
//		return relation.isValid();
//	}

	public void js_setAllowCreationRelatedRecords(boolean arg)
	{
		checkModification();
		relation.setAllowCreationRelatedRecords(arg);
	}

	public void js_setAllowParentDeleteWhenHavingRelatedRecords(boolean arg)
	{
		checkModification();
		relation.setAllowParentDeleteWhenHavingRelatedRecords(arg);
	}

	public void js_setDeleteRelatedRecords(boolean arg)
	{
		checkModification();
		relation.setDeleteRelatedRecords(arg);
	}

//	@Deprecated
//	public void js_setDuplicateRelatedRecords(boolean b)
//	{
//		checkModification();
//		relation.setDuplicateRelatedRecords(b);
//	}

	@Deprecated
	public void js_setForeignServerName(String name)
	{
		checkModification();
		relation.setForeignServerName(name);
	}

	@Deprecated
	public void js_setForeignTableName(String name)
	{
		checkModification();
		relation.setForeignTableName(name);
	}

	public void js_setForeignDataSource(String arg)
	{
		// check syntax, do not accept invalid URIs
		if (arg != null)
		{
			try
			{
				new URI(arg);
			}
			catch (URISyntaxException e)
			{
				throw new RuntimeException("Invalid dataSource URI: '" + arg + "' :" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		checkModification();
		relation.setForeignDataSource(arg);
	}

	public void js_setInitialSort(String sort)
	{
		checkModification();
		relation.setInitialSort(sort);
	}

	public void js_setJoinType(int joinType)
	{
		checkModification();
		relation.setJoinType(joinType);
	}

	public void js_setName(String name)
	{
		checkModification();
		try
		{
			relation.updateName(new ScriptNameValidator(application.getFlattenedSolution()), name);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException("Error updating the name from " + relation.getName() + " to " + name, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Deprecated
	public void js_setPrimaryServerName(String name)
	{
		checkModification();
		relation.setPrimaryServerName(name);
	}

	@Deprecated
	public void js_setPrimaryTableName(String name)
	{
		checkModification();
		relation.setPrimaryTableName(name);
	}

	public void js_setPrimaryDataSource(String arg)
	{
		// check syntax, do not accept invalid URIs
		if (arg != null)
		{
			try
			{
				new URI(arg);
			}
			catch (URISyntaxException e)
			{
				throw new RuntimeException("Invalid dataSource URI: '" + arg + "' :" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		checkModification();
		relation.setPrimaryDataSource(arg);
	}

	/**
	 * 
	 */
	public final void checkModification()
	{
		SQLSheet relatedSheet;
		try
		{
			relatedSheet = ((FoundSetManager)application.getFoundSetManager()).getSQLGenerator().getCachedTableSQLSheet(relation.getForeignDataSource());
			if (relation.isRuntimeReadonly() || relatedSheet.getRelatedSQLDescription(relation.getName()) != null) throw new RuntimeException(
				"Cant modify a relation that is already used in the application: " + relation.getName()); //$NON-NLS-1$
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		if (!isCopy)
		{
			relation = application.getFlattenedSolution().createPersistCopy(relation);
			isCopy = true;
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSRelation[name:" + relation.getName() + ",items:" + Arrays.toString(js_getRelationItems()) + ']';
	}

	/**
	 * Returns the UUID of the relation object
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * application.output(relation.getUUID().toString())
	 */
	public UUID js_getUUID()
	{
		return relation.getUUID();
	}
}
