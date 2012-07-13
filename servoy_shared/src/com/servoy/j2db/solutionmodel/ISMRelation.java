/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.solutionmodel;

import com.servoy.j2db.query.ISQLJoin;


/**
 * Solution model relation object.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMRelation extends ISMHasUUID
{
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
	public ISMRelationItem[] getRelationItems();

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
	public ISMRelationItem newRelationItem(String dataprovider, String operator, String foreinColumnName);

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
	public void removeRelationItem(String primaryDataProviderID, String operator, String foreignColumnName);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getAllowCreationRelatedRecords()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.allowCreationRelatedRecords = true;
	 */
	public boolean getAllowCreationRelatedRecords();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getAllowParentDeleteWhenHavingRelatedRecords()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.allowParentDeleteWhenHavingRelatedRecords = false;
	 */
	public boolean getAllowParentDeleteWhenHavingRelatedRecords();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getDeleteRelatedRecords()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.deleteRelatedRecords = true;
	 */
	public boolean getDeleteRelatedRecords();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getForeignDataSource()
	 * 
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSRelation#getPrimaryDataSource()
	 */
	public String getForeignDataSource();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getInitialSort()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.initialSort = 'another_child_table_text asc';
	 */
	public String getInitialSort();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getJoinType()
	 * 
	 * @sampleas INNER_JOIN
	 */
	public int getJoinType();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getName()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.name = 'anotherName';
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.relationName = relation.name;
	 */
	public String getName();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Relation#getPrimaryDataSource()
	 * 
	 * @sample
	 * 	var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.primaryDataSource = 'db:/user_data/another_parent_table';
	 * relation.foreignDataSource = 'db:/user_data/another_child_table';
	 */
	public String getPrimaryDataSource();

	public void setAllowCreationRelatedRecords(boolean arg);

	public void setAllowParentDeleteWhenHavingRelatedRecords(boolean arg);

	public void setDeleteRelatedRecords(boolean arg);

	public void setForeignDataSource(String arg);

	public void setInitialSort(String sort);

	public void setJoinType(int joinType);

	public void setName(String name);

	public void setPrimaryDataSource(String arg);
}