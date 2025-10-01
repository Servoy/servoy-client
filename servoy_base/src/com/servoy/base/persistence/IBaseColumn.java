/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.persistence;

/**
 * Generic interface for columns
 *
 * @author rgansevles
 *
 */
public interface IBaseColumn
{
	// column flags
	static final int NORMAL_COLUMN = 0;
	static final int PK_COLUMN = 1;
	static final int USER_ROWID_COLUMN = 1 << 1;
	static final int UUID_COLUMN = 1 << 2;
	static final int EXCLUDED_COLUMN = 1 << 3;
	static final int TENANT_COLUMN = 1 << 4;
	static final int NATIVE_COLUMN = 1 << 5;
	static final int SORT_IGNORECASE = 1 << 6;
	static final int SORT_ASC_NULLS_FIRST = 1 << 7;
	static final int SORT_ASC_NULLS_LAST = 1 << 8;
	static final int NO_DATA_LOG_COLUMN = 1 << 9;

	static final int IDENT_COLUMNS = PK_COLUMN + USER_ROWID_COLUMN;
	static final int NON_IDENT_COLUMNS = ~IDENT_COLUMNS;

	static final int[] allDefinedRowIdents = new int[] { NORMAL_COLUMN, PK_COLUMN, USER_ROWID_COLUMN };

	int getDataProviderType();

	int getLength();

	int getFlags();
}
