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

import static com.servoy.j2db.persistence.IColumnTypes.INTEGER;
import static com.servoy.j2db.persistence.IColumnTypes.NUMBER;
import static com.servoy.j2db.persistence.IColumnTypes.TEXT;

import com.servoy.base.query.BaseColumnType;
import com.servoy.base.query.IBaseQuerySelectValue;
import com.servoy.base.query.TypeInfo;
import com.servoy.j2db.persistence.Column;


/** Interface for selectable values in a select statement.
 * @author rgansevles
 *
 */
public interface IQuerySelectValue extends IBaseQuerySelectValue, IQueryElement
{
	String getAlias();

	default QueryColumn getColumn()
	{
		return null;
	}

	IQuerySelectValue asAlias(String alias);

	default String getColumnName()
	{
		QueryColumn qcol = getColumn();
		if (qcol != null)
		{
			return qcol.getName();
		}

		return null;
	}

	default String getAliasOrName()
	{
		String alias = getAlias();
		if (alias != null)
		{
			return alias;
		}

		String name = getColumnName();
		if (name != null)
		{
			return name;
		}

		return toString();
	}

	default BaseColumnType getColumnType()
	{
		QueryColumn qcol = getColumn();
		if (qcol != null)
		{
			return qcol.getColumnType();
		}
		return null;
	}

	default String getNativeTypename()
	{
		QueryColumn qcol = getColumn();
		if (qcol != null)
		{
			return qcol.getNativeTypename();
		}
		return null;
	}

	default TypeInfo getTypeInfo()
	{
		return new TypeInfo(getColumnType(), getNativeTypename());
	}

	default int getFlags()
	{
		QueryColumn qcol = getColumn();
		if (qcol != null)
		{
			return qcol.getFlags();
		}
		return 0;
	}


	/**
	 * Determine a column type that is compatible with both types
	 */
	public static BaseColumnType determineCompatibleColumnType(BaseColumnType type1, BaseColumnType type2)
	{
		if (type1 == null)
		{
			return type2;
		}
		if (type2 == null)
		{
			return type1;
		}

		int mappedType1 = Column.mapToDefaultType(type1);
		int mappedType2 = Column.mapToDefaultType(type2);

		if (mappedType1 == TEXT)
		{
			// can be converted to other types
			return type2;
		}

		if (mappedType2 == TEXT)
		{
			// can be converted to other types
			return type1;
		}

		if (mappedType1 == INTEGER)
		{
			// can be converted to other non-text types
			return type2;
		}

		if (mappedType2 == INTEGER)
		{
			// can be converted to other non-text types
			return type1;
		}
		if (mappedType1 == NUMBER)
		{
			// can be converted to other non-text types
			return type2;
		}

		if (mappedType2 == NUMBER)
		{
			// can be converted to other non-text types
			return type1;
		}

		// choose one (in a stable way), if they are not compatible the query will fail, we cannot check that here
		return type1.getSqlType() < type2.getSqlType() ? type1 : type2;
	}

}
