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

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.solutionmodel.ISMRelationItem;
import com.servoy.j2db.util.Utils;

/**
 * <code>JSRelationItem</code> represents a single criterion within a relation in the solution model.
 * It defines how columns from the primary and foreign data sources are connected, enabling precise relationship definitions for queries and data operations.
 *
 * <h2>Functionality</h2>
 * <p>JSRelationItem provides constants such as <code>LITERAL_PREFIX</code> to enable the use of literals in relation definitions.
 * Properties like <code>foreignColumnName</code>, <code>primaryDataProviderID</code>, and <code>operator</code> allow customization of the relationship criteria.
 * Supported operators include <code>=</code>, <code>!=</code>, <code><</code>, <code>></code>, <code>like</code>, and more, enabling a wide range of comparisons and queries between data sources.</p>
 *
 * <p>Methods like <code>getComment</code> and <code>getUUID</code> assist in retrieving metadata about the relation item,
 * such as a unique identifier or comments. These utilities enhance the traceability and documentation of relation definitions in complex solution models.</p>
 *
 * <h3>Example</h3>
 * <p>A relation item can be configured to specify relationships like:</p>
 * <ul>
 *   <li>Linking a column from the primary table to a foreign table.</li>
 *   <li>Using literals or specific operators to define advanced criteria.</li>
 * </ul>
 *
 * <p>For more details, please refer to the
 * <a href="https://docs.servoy.com/reference/servoycore/object-model/solution/relation/relationitem">Relation item</a> section of this documentation.</p>
 *
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSRelationItem extends JSBase<RelationItem> implements IJavaScriptType, IConstantsObject, ISMRelationItem
{

	public JSRelationItem(RelationItem relationItem, JSRelation parent, boolean isNew)
	{
		super(parent, relationItem, isNew);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.RelationItem#getForeignColumnName()
	 *
	 * @sample
	 * 	var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * var criteria = relation.newRelationItem('parent_table_id', '=', 'child_table_parent_id');
	 * criteria.primaryDataProviderID = 'parent_table_text';
	 * criteria.foreignColumnName = 'child_table_text';
	 * criteria.operator = '<';
	 */
	@JSGetter
	public String getForeignColumnName()
	{
		return getBaseComponent(false).getForeignColumnName();
	}

	@JSSetter
	public void setForeignColumnName(String arg)
	{
		checkModification();
		getBaseComponent(true).setForeignColumnName(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.RelationItem#getOperator()
	 *
	 * @sampleas getForeignColumnName()
	 */
	@JSGetter
	public String getOperator()
	{
		return RelationItem.getOperatorAsString(getBaseComponent(false).getOperator());
	}

	@JSSetter
	public void setOperator(String operator)
	{
		checkModification();
		int validOperator = RelationItem.getValidOperator(operator, RelationItem.RELATION_OPERATORS, null);
		if (validOperator == -1) throw new IllegalArgumentException("Operator " + operator + " is not a valid relation operator"); //$NON-NLS-1$ //$NON-NLS-2$
		getBaseComponent(true).setOperator(validOperator);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.RelationItem#getPrimaryDataProviderID()
	 *
	 * @sampleas getForeignColumnName()
	 */
	@JSGetter
	public String getPrimaryDataProviderID()
	{
		return getBaseComponent(false).getPrimaryDataProviderID();
	}

	@JSSetter
	public void setPrimaryDataProviderID(String arg)
	{
		checkModification();
		getBaseComponent(true).setPrimaryDataProviderID(arg);
	}

	/**
	 * Get the literal.
	 *
	 * @sample
	 * var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * var criteria = relation.newRelationItem(JSRelationItem.LITERAL_PREFIX + "'hello'",'=', 'myTextField');
	 * criteria.primaryLiteral = 'literal_text';
	 * //criteria.primaryLiteral = number;
	 * var primaryLiteral = criteria.primaryLiteral;
	 *
	 */
	@JSGetter
	public Object getPrimaryLiteral()
	{
		String primaryLiteral = getPrimaryDataProviderID();
		if (primaryLiteral.startsWith(LiteralDataprovider.LITERAL_PREFIX))
		{
			return Utils.parseJSExpression(primaryLiteral.substring(LiteralDataprovider.LITERAL_PREFIX.length()));
		}
		return null;
	}

	@JSSetter
	public void setPrimaryLiteral(Object arg)
	{
		checkModification();
		getBaseComponent(true).setPrimaryDataProviderID(LiteralDataprovider.LITERAL_PREFIX + Utils.makeJSExpression(arg));
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "JSRelationItem[" + getBaseComponent(false).getPrimaryDataProviderID() + getOperator() + getBaseComponent(false).getForeignColumnName() + ']'; //$NON-NLS-1$
	}
}
