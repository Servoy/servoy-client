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

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.persistence.LiteralDataprovider;

/**
 * Solution relation item.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMRelationItem extends ISMHasUUID
{

	/** 
	 * Constant for using literals in solution model in relations.
	 * Strings must be passed as quoted value to make a distinction between string '5' and number 5.
	 * 
	 * @sample
	 * relation.newRelationItem(JSRelationItem.LITERAL_PREFIX + "'hello'",'=', 'mytextfield');
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final String LITERAL_PREFIX = LiteralDataprovider.LITERAL_PREFIX;


	/**
	 * The name of the column from the destination table
	 * that this relation item is based on.
	 * 
	 * @sample
	 * 	var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * var criteria = relation.newRelationItem('parent_table_id', '=', 'child_table_parent_id');
	 * criteria.primaryDataProviderID = 'parent_table_text';
	 * criteria.foreignColumnName = 'child_table_text';
	 * criteria.operator = '<';
	 */
	public String getForeignColumnName();

	/**
	 * The operator that defines the relationship between the primary dataprovider
	 * and the foreign column.
	 * 
	 * @sampleas getForeignColumnName()
	 * @see #getForeignColumnName()
	 */
	public String getOperator();

	/**
	 * The name of the column from the source table 
	 * that this relation item is based on.
	 * 
	 * @sampleas getForeignColumnName()
	 * @see #getForeignColumnName()
	 */
	public String getPrimaryDataProviderID();

	public void setOperator(String operator);

	public void setForeignColumnName(String arg);

	public void setPrimaryDataProviderID(String arg);

}