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
	static final int USER_ROWID_COLUMN = 2;
	static final int UUID_COLUMN = 4;
	static final int EXCLUDED_COLUMN = 8;

	static final int IDENT_COLUMNS = PK_COLUMN + USER_ROWID_COLUMN;
	static final int NON_IDENT_COLUMNS = ~IDENT_COLUMNS;

	static final int[] allDefinedRowIdents = new int[] { NORMAL_COLUMN, PK_COLUMN, USER_ROWID_COLUMN };
	static final int[] allDefinedOtherFlags = new int[] { UUID_COLUMN, EXCLUDED_COLUMN };

	int getDataProviderType();

	int getLength();

	int getFlags();
}
