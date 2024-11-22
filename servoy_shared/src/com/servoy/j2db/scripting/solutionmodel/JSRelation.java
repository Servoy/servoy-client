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

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.solutionmodel.ISMRelation;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;

/**
 * <code>JSRelation</code> provides a model for defining and managing relations between primary and foreign data sources,
 * supporting operations such as joins, sorting, and criteria management.
 * It includes constants, properties, and methods to facilitate the creation, modification, and use of relational data structures.
 *
 * <h2>Functionality</h2>
 * <p>JSRelation offers constants to define join types (<code>FULL_JOIN</code>, <code>INNER_JOIN</code>, <code>LEFT_OUTER_JOIN</code>, and <code>RIGHT_OUTER_JOIN</code>)
 * used in database relations. These constants influence how tables are joined during operations like sorting and querying.
 * Properties such as <code>allowCreationRelatedRecords</code> and <code>deleteRelatedRecords</code> provide control over cascading actions
 * like record creation or deletion across related data sets.
 * Other attributes like <code>foreignDataSource</code>, <code>primaryDataSource</code>, and <code>initialSort</code>
 * allow fine-tuning of the relational setup, enabling flexibility across multiple databases or data views.</p>
 *
 * <p>The methods provided, such as <code>getRelationItems</code> and <code>getUUID</code>, allow querying and identifying relations,
 * while methods like <code>newRelationItem</code> and <code>removeRelationItem</code> enable dynamic modification of relation criteria.
 * For example, relations can be defined between specific columns or modified to add or remove criteria dynamically during runtime.</p>
 *
 * <p>JSRelation supports a robust configuration system with detailed examples, showcasing the integration of parent-child relations and control over operations
 * such as cascading deletions, dynamic key assignments, and data source management.</p>
 *
 * For details please refer to the <a href="../../object-model/solution/relation.md">Relation</a> section of this documentation</p>
 *
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSRelation implements IJSParent<Relation>, IConstantsObject, ISMRelation
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

	private boolean isCopy = false;
	private Relation relation;
	private final IApplication application;

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
	@JSFunction
	public JSRelationItem[] getRelationItems()
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
	 * // for literals use a prefix
	 * relation.newRelationItem(JSRelationItem.LITERAL_PREFIX + "'hello'",'=', 'mytextfield');
	 *
	 * @param dataprovider The name of the primary dataprovider.
	 *
	 * @param operator The operator used to relate the primary and the foreign dataproviders.
	 *
	 * @param foreinColumnName The name of the foreign dataprovider.
	 *
	 * @return A JSRelationItem instance representing the newly added relation item.
	 */
	@JSFunction
	public JSRelationItem newRelationItem(String dataprovider, String operator, String foreinColumnName)
	{
		if (dataprovider == null)
		{
			throw new IllegalArgumentException("dataprovider cannot be null"); //$NON-NLS-1$
		}
		if (foreinColumnName == null)
		{
			throw new IllegalArgumentException("foreinColumnName cannot be null"); //$NON-NLS-1$
		}

		int validOperator = RelationItem.getValidOperator(operator, RelationItem.RELATION_OPERATORS, null);
		if (validOperator == -1)
		{
			throw new IllegalArgumentException("operator " + operator + " is not a valid relation operator"); //$NON-NLS-1$//$NON-NLS-2$
		}

		checkModification();
		try
		{
			IDataProvider primaryDataProvider = null;
			if (ScopesUtils.isVariableScope(dataprovider))
			{
				primaryDataProvider = application.getFlattenedSolution().getGlobalDataProvider(dataprovider);
			}
			else if (dataprovider.startsWith(LiteralDataprovider.LITERAL_PREFIX))
			{
				primaryDataProvider = new LiteralDataprovider(dataprovider);
				if (((LiteralDataprovider)primaryDataProvider).getValue() == null)
				{
					throw new IllegalArgumentException("primary data provider " + dataprovider + " is not a valid relation primary data provider"); //$NON-NLS-1$
				}
			}
			else
			{
				primaryDataProvider = application.getFlattenedSolution().getDataProviderForTable(
					application.getFoundSetManager().getTable(relation.getPrimaryDataSource()), dataprovider);
			}
			if (primaryDataProvider == null)
			{
				throw new IllegalArgumentException("cant create relation item primary dataprovider not found: " + dataprovider); //$NON-NLS-1$
			}

			IDataProvider dp = application.getFlattenedSolution().getDataProviderForTable(
				application.getFoundSetManager().getTable(relation.getForeignDataSource()), foreinColumnName);
			if (!(dp instanceof Column))
			{
				throw new IllegalArgumentException("Foreign columnname " + foreinColumnName + " is not a valid column"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			RelationItem result = relation.createNewRelationItem(application.getFoundSetManager(), primaryDataProvider, validOperator, (Column)dp);
			if (result != null) return new JSRelationItem(result, this, isCopy);
			return null;
		}
		catch (RepositoryException e)
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
	@JSFunction
	public void removeRelationItem(String primaryDataProviderID, String operator, String foreignColumnName)
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
	@JSGetter
	public boolean getAllowCreationRelatedRecords()
	{
		return relation.getAllowCreationRelatedRecords();
	}

	@JSSetter
	public void setAllowCreationRelatedRecords(boolean arg)
	{
		checkModification();
		relation.setAllowCreationRelatedRecords(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getAllowParentDeleteWhenHavingRelatedRecords()
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.allowParentDeleteWhenHavingRelatedRecords = false;
	 */
	@JSGetter
	public boolean getAllowParentDeleteWhenHavingRelatedRecords()
	{
		return relation.getAllowParentDeleteWhenHavingRelatedRecords();
	}

	@JSSetter
	public void setAllowParentDeleteWhenHavingRelatedRecords(boolean arg)
	{
		checkModification();
		relation.setAllowParentDeleteWhenHavingRelatedRecords(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getDeleteRelatedRecords()
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.deleteRelatedRecords = true;
	 */
	@JSGetter
	public boolean getDeleteRelatedRecords()
	{
		return relation.getDeleteRelatedRecords();
	}

	@JSSetter
	public void setDeleteRelatedRecords(boolean arg)
	{
		checkModification();
		relation.setDeleteRelatedRecords(arg);
	}

	/**
	 * The name of the server where the foreign table is located.
	 *
	 * @deprecated As of release 5.1, replaced by foreignDataSource property.
	 */
	@Deprecated
	public String js_getForeignServerName()
	{
		return relation.getForeignServerName();
	}

	/**
	 * The name of the foreign table.
	 *
	 * @deprecated As of release 5.1, replaced by foreignDataSource property.
	 */
	@Deprecated
	public String js_getForeignTableName()
	{
		return relation.getForeignTableName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getForeignDataSource()
	 *
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSRelation#getPrimaryDataSource()
	 */
	@JSGetter
	public String getForeignDataSource()
	{
		return relation.getForeignDataSource();
	}

	@JSSetter
	public void setForeignDataSource(String arg)
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

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getInitialSort()
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.initialSort = 'another_child_table_text asc';
	 */
	@JSGetter
	public String getInitialSort()
	{
		return relation.getInitialSort();
	}

	@JSSetter
	public void setInitialSort(String sort)
	{
		checkModification();
		relation.setInitialSort(sort);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getJoinType()
	 *
	 * @sampleas com.servoy.j2db.solutionmodel.ISMRelation#INNER_JOIN
	 */
	@JSGetter
	public int getJoinType()
	{
		return relation.getJoinType();
	}

	@JSSetter
	public void setJoinType(int joinType)
	{
		checkModification();
		relation.setJoinType(joinType);
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
	@JSGetter
	public String getName()
	{
		return relation.getName();
	}

	@JSSetter
	public void setName(String name)
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

	/**
	 * The name of the server where the primary table is located.
	 *
	 * @deprecated As of release 5.1, replaced by primaryDataSource property.
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
	 * @deprecated As of release 5.1, replaced by primaryDataSource property.
	 */
	@Deprecated
	public String js_getPrimaryTableName()
	{
		return relation.getPrimaryTableName();
	}

	/**
	 * @clonedesc com.servoy.j2db.solutionmodel.ISMRelation#getPrimaryDataSource()
	 *
	 * @sampleas com.servoy.j2db.solutionmodel.ISMRelation#getForeignDataSource()
	 */
	@JSGetter
	public String getPrimaryDataSource()
	{
		return relation.getPrimaryDataSource();
	}

	@JSSetter
	public void setPrimaryDataSource(String arg)
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

	/**
	 * Returns the UUID of the relation object
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * application.output(relation.getUUID().toString())
	 */
	@JSFunction
	public UUID getUUID()
	{
		return relation.getUUID();
	}

	/**
	 *
	 */
	public final void checkModification()
	{
		try
		{
			if (application.getFoundSetManager().getSQLGenerator().getCachedTableSQLSheet(relation.getForeignDataSource()).getRelatedSQLDescription(
				relation.getName()) != null)
			{
				throw new RuntimeException("Cant modify a relation that is already used in the application: " + relation.getName()); //$NON-NLS-1$
			}
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
		relation.flushCashedItems();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSRelation[name:" + relation.getName() + ",items:" + Arrays.toString(getRelationItems()) + ']';
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((relation == null) ? 0 : relation.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSRelation other = (JSRelation)obj;
		if (relation == null)
		{
			if (other.relation != null) return false;
		}
		else if (!relation.getUUID().equals(other.relation.getUUID())) return false;
		return true;
	}
}
