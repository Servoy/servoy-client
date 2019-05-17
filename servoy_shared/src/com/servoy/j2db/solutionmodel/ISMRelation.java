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

import com.servoy.base.query.IJoinConstants;
import com.servoy.base.solutionmodel.IBaseSMRelation;


/**
 * Solution model relation object.
 *
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMRelation extends IBaseSMRelation, ISMHasUUID
{

	/**
	 * Constant for the joinType of a JSRelation. It is also used in solutionModel.newRelation(...).
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.joinType = JSRelation.LEFT_OUTER_JOIN;
	 */
	public static final int INNER_JOIN = IJoinConstants.INNER_JOIN;

	/**
	 * @sameas INNER_JOIN
	 * @see #INNER_JOIN
	 */
	public static final int LEFT_OUTER_JOIN = IJoinConstants.LEFT_OUTER_JOIN;

	/**
	 * @sameas INNER_JOIN
	 * @see #INNER_JOIN
	 */
	public static final int RIGHT_OUTER_JOIN = IJoinConstants.RIGHT_OUTER_JOIN;

	/**
	 * @sameas INNER_JOIN
	 * @see #INNER_JOIN
	 */
	public static final int FULL_JOIN = IJoinConstants.FULL_JOIN;

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
	 * Flag that tells if related records can be created through this relation.
	 *
	 * The default value of this flag is "false".
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.allowCreationRelatedRecords = true;
	 */
	public boolean getAllowCreationRelatedRecords();

	/**
	 * Flag that tells if the parent record can be deleted while it has related records.
	 *
	 * The default value of this flag is "true".
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.allowParentDeleteWhenHavingRelatedRecords = false;
	 */
	public boolean getAllowParentDeleteWhenHavingRelatedRecords();

	/**
	 * Flag that tells if related records should be deleted or not when a parent record is deleted.
	 *
	 * The default value of this flag is "false".
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.deleteRelatedRecords = true;
	 */
	public boolean getDeleteRelatedRecords();

	/**
	 * Qualified name of the foreign data source. Contains both the name of the foreign
	 * server and the name of the foreign table.
	 *
	 * @sample
	 * 	var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.primaryDataSource = 'db:/user_data/another_parent_table';
	 * relation.foreignDataSource = 'db:/user_data/another_child_table';
	 */
	public String getForeignDataSource();

	/**
	 * The join type that is performed between the primary table and the foreign table.
	 * Can be "inner join" or "left outer join".
	 *
	 * @sampleas INNER_JOIN
	 * @see #INNER_JOIN
	 */
	public int getJoinType();

	/**
	 * Qualified name of the primary data source. Contains both the name of the primary server
	 * and the name of the primary table.
	 *
	 * @sampleas getForeignDataSource()
	 * @see #getForeignDataSource()
	 */
	public String getPrimaryDataSource();

	/**
	 * The name of the relation.
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.name = 'anotherName';
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.relationName = relation.name;
	 */
	public String getName();

	public void setAllowCreationRelatedRecords(boolean arg);

	public void setAllowParentDeleteWhenHavingRelatedRecords(boolean arg);

	public void setDeleteRelatedRecords(boolean arg);

	public void setForeignDataSource(String arg);

	public void setJoinType(int joinType);

	public void setName(String name);

	public void setPrimaryDataSource(String arg);

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
	 * A String which specified a set of sort options for the initial sorting of data
	 * retrieved through this relation.
	 *
	 * Has the form "column_name asc, another_column_name desc, ...".
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.initialSort = 'another_child_table_text asc';
	 */
	public String getInitialSort();

	public void setInitialSort(String sort);

}