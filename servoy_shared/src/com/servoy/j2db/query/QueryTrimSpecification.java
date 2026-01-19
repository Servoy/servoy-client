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

import static com.servoy.j2db.query.QueryTrimSpecification.TrimSpecification.BOTH;
import static com.servoy.j2db.query.QueryTrimSpecification.TrimSpecification.LEADING;
import static com.servoy.j2db.query.QueryTrimSpecification.TrimSpecification.TRAILING;

import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;


/**
 * RAGTEST doc
 *
 * @author rgansevles
 *
 */
public final class QueryTrimSpecification implements IQuerySelectValue
{
	public enum TrimSpecification
	{
		LEADING,
		TRAILING,
		BOTH,
	}

	private final TrimSpecification trimSpecification;

	public QueryTrimSpecification(TrimSpecification trimSpecification)
	{
		this.trimSpecification = trimSpecification;
	}

	public static QueryTrimSpecification fromSpecification(String specification)
	{
		if (specification != null)
		{
			switch (specification.toUpperCase())
			{
				case "LEADING" :
					return new QueryTrimSpecification(LEADING);
				case "TRAILING" :
					return new QueryTrimSpecification(TRAILING);
				case "BOTH" :
					return new QueryTrimSpecification(BOTH);

				default :
			}
		}

		throw new IllegalArgumentException("Unexpected trim specification: " + specification);
	}


	/**
	 * @return the trimSpecification
	 */
	public TrimSpecification getTrimSpecification()
	{
		return trimSpecification;
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return this;
	}


	@Override
	public void acceptVisitor(IVisitor visitor)
	{
	}

	@Override
	public String getAlias()
	{
		return null;
	}

	@Override
	public IQuerySelectValue asAlias(String alias)
	{
		return this;
	}

	@Override
	public String toString()
	{
		return trimSpecification.name();
	}

	///////// serialization ////////////////
	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), trimSpecification);
	}

	public QueryTrimSpecification(ReplacedObject s)
	{
		trimSpecification = (TrimSpecification)s.getObject();
	}
}
