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
package com.servoy.j2db.query;

import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Base condition class for AndCondition and OrCondition.
 * 
 * @author rgansevles
 * 
 */
public abstract class AndOrCondition implements ISQLCondition
{
	protected List<ISQLCondition> conditions;


	public AndOrCondition()
	{
		this.conditions = new ArrayList<ISQLCondition>();
	}

	public AndOrCondition(List<ISQLCondition> conditions)
	{
		this.conditions = conditions;
	}


	public void addCondition(ISQLCondition condition)
	{
		if (condition == null)
		{
			return;
		}
		if (getClass() == condition.getClass())
		{
			conditions.addAll(((AndOrCondition)condition).getConditions());
		}
		else
		{
			conditions.add(condition);
		}
	}

	public List<ISQLCondition> getConditions()
	{
		return conditions;
	}

	public abstract ISQLCondition negate();


	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		conditions = AbstractBaseQuery.acceptVisitor(conditions, visitor);
	}

	public abstract String getInfix();

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.conditions == null) ? 0 : this.conditions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final AndOrCondition other = (AndOrCondition)obj;
		if (this.conditions == null)
		{
			if (other.conditions != null) return false;
		}
		else if (!this.conditions.equals(other.conditions)) return false;
		return true;
	}


	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append('(');
		for (int i = 0; i < conditions.size(); i++)
		{
			if (i > 0)
			{
				sb.append(' ').append(getInfix().toUpperCase()).append(' ');
			}
			sb.append(conditions.get(i).toString());
		}
		return sb.append(')').toString();
	}


	///////// serialization ////////////////


	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), conditions);
	}

	public AndOrCondition(ReplacedObject s)
	{
		conditions = (List)s.getObject();
	}
}