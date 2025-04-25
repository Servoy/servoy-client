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
public class ColumnType extends BaseColumnType implements IWriteReplace
{
	private static final ConcurrentMap<ColumnType, ColumnType> instances;
	public static final ColumnType UNKNOWN;

	static
	{
		instances = new ConcurrentHashMap<ColumnType, ColumnType>();
		UNKNOWN = getInstance(-1, -1, 0, 0);
	}

	private ColumnType(int sqlType, int length, int scale, int subType)
	{
		super(sqlType, length, scale, subType);
	}

	public static ColumnType getInstance(int sqlType, int length, int scale)
	{
		return getInstance(sqlType, length, scale, 0);
	}

	public static ColumnType getInstance(int sqlType, int length, int scale, int subType)
	{
		ColumnType instance = new ColumnType(sqlType, length, scale, subType);
		ColumnType previous = instances.putIfAbsent(instance, instance);
		if (previous != null)
		{
			instance = previous;
		}
		return instance;
	}

///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new int[] { sqlType, length, scale, subType });
	}

	public ColumnType(ReplacedObject s)
	{
		int[] ints = (int[])s.getObject();
		int index = 0;
		sqlType = ints[index++];
		length = ints[index++];
		scale = ints[index++];
		if (index < ints.length) subType = ints[index++];
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
	public static ColumnType[] getColumnTypes(int... types)
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


	public static ColumnType toColumnType(BaseColumnType baseColumnType)
	{
		if (baseColumnType instanceof ColumnType columnType)
		{
			return columnType;
		}
		return getInstance(baseColumnType.getSqlType(), baseColumnType.getLength(), baseColumnType.getScale(), baseColumnType.getSubType());
	}
}
