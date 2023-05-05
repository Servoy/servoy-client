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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.base.query.BaseAndOrCondition;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Base condition class for AndCondition and OrCondition.
 *
 * @author rgansevles
 *
 */
public abstract class AndOrCondition extends BaseAndOrCondition<ISQLCondition> implements ISQLCondition
{
	public AndOrCondition()
	{
	}

	public AndOrCondition(List<ISQLCondition> conditions)
	{
		super(conditions);
	}

	public AndOrCondition(Map<String, List<ISQLCondition>> conditions)
	{
		super(conditions);
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		conditions = validateConditions(AbstractBaseQuery.acceptVisitor(conditions, visitor));
	}

	///////// serialization ////////////////


	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), conditions == null ? "" : conditions);
	}

	public AndOrCondition(ReplacedObject s)
	{
		// conditions used to be a list, now it is a HashMap
		Object o = s.getObject();
		if (o instanceof List)
		{
			HashMap<String, List<ISQLCondition>> map = new HashMap<>();
			map.put(null, (List<ISQLCondition>)o);
			conditions = map;
		}
		else
		{
			// null not allowed as replaced object, we use an empty string in that case
			conditions = "".equals(o) ? null : (HashMap<String, List<ISQLCondition>>)o;
		}
	}
}