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

import java.io.Serializable;
import java.sql.Types;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.servoy.base.query.BaseColumnType;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.serialize.ReplacedObject;


/** Container for column types describing type, length and scale (for numerical columns).
 * @author rgansevles
 *
 */
public class ColumnType extends BaseColumnType implements Serializable, IWriteReplace
{
	private static final ConcurrentMap<ColumnType, ColumnType> instances;
	public static final ColumnType DUMMY;

	static
	{
		instances = new ConcurrentHashMap<ColumnType, ColumnType>();
		DUMMY = getInstance(-1, -1, 0);
	}

	private ColumnType(int sqlType, int length, int scale)
	{
		super(sqlType, length, scale);
	}

	public static ColumnType getInstance(int sqlType, int length, int scale)
	{
		ColumnType instance = new ColumnType(sqlType, length, scale);
		ColumnType previous = instances.putIfAbsent(instance, instance);
		if (previous != null)
		{
			instance = previous;
		}
		return instance;
	}

	public ColumnType intern()
	{
		return getInstance(getSqlType(), getLength(), getScale());
	}

///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new int[] { sqlType, length, scale });
	}

	public ColumnType(ReplacedObject s)
	{
		int[] ints = (int[])s.getObject();
		sqlType = ints[0];
		length = ints[1];
		scale = ints[2];
	}


	public static void clearInstances()
	{
		instances.clear();
	}

	/** Convert {@link java.sql.Types} to {@link ColumnType}
	 *
	 * @param types
	 * @return
	 */
	public static ColumnType[] getColumnTypes(int[] types)
	{
		if (types == null)
		{
			return null;
		}

		ColumnType[] columnTypes = new ColumnType[types.length];
		for (int i = 0; i < types.length; i++)
		{
			columnTypes[i] = getColumnType(types[i]);
		}

		return columnTypes;
	}

	/** Convert {@link java.sql.Types} to {@link ColumnType}
	 *
	 * @param type
	 * @return
	 */
	public static ColumnType getColumnType(int type)
	{
		switch (type)
		{
			case Types.CHAR :
				return getInstance(type, 255, 0);
			case Types.DECIMAL :
				return getInstance(type, 19, 2);
		}
		switch (Column.mapToDefaultType(type))
		{
			case IColumnTypes.MEDIA :
			case IColumnTypes.TEXT :
				return getInstance(type, Integer.MAX_VALUE, 0);
		}
		return getInstance(type, 0, 0);
	}
}
