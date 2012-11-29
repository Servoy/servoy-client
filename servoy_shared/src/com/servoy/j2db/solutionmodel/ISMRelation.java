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

import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMRelation;


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
	 * @clonedesc com.servoy.j2db.persistence.Relation#getInitialSort()
	 * 
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * relation.initialSort = 'another_child_table_text asc';
	 */
	public String getInitialSort();

	public void setInitialSort(String sort);

}