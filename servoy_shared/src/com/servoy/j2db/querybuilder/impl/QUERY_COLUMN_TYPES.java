/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.querybuilder.impl;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.querybuilder.IQueryBuilderConstants;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * Script constants for Query Builder column types.
 * 
 * @author rgansevles
 * 
 * @since 6.1
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QUERY_COLUMN_TYPES implements IPrefixedConstantsObject, IQueryBuilderConstants
{

	public String getPrefix()
	{
		return "QUERY_COLUMN_TYPES"; //$NON-NLS-1$
	}

	@Override
	public String toString()
	{
		return "Query Builder column types"; //$NON-NLS-1$
	}
}