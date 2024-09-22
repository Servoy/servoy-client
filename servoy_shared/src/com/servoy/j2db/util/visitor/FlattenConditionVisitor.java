/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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
package com.servoy.j2db.util.visitor;

import static com.servoy.j2db.query.AbstractBaseQuery.acceptVisitor;

import com.servoy.j2db.query.AndOrCondition;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.Placeholder;

/**
 * Visitor class for flattening a condition as much as possible.
 *
 * And/Or conditions with a single element are replaced with the element.
 *
 * Placeholders are replaced with their values.
 *
 * @see IVisitable
 * @author rgansevles
 *
 */
public class FlattenConditionVisitor implements IVisitor
{
	private static final FlattenConditionVisitor INSTANCE = new FlattenConditionVisitor();

	private FlattenConditionVisitor()
	{
	}

	public Object visit(Object o)
	{
		if (o instanceof AndOrCondition andOrCondition)
		{
			return new VisitorResult(andOrCondition.flatten(), true);
		}
		if (o instanceof Placeholder placeHolder && placeHolder.isSet())
		{
			return placeHolder.getValue();
		}
		return o;

	}

	public static ISQLCondition flattenCondition(ISQLCondition condition)
	{
		return acceptVisitor(condition, INSTANCE);
	}
}
