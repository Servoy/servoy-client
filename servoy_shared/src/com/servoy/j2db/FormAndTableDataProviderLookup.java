/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;

public class FormAndTableDataProviderLookup implements IDataProviderLookup
{
	private final FlattenedSolution flattenedSolution;

	private Map<String, IDataProvider> formProviders = null; //cache for recurring lookup

	private final ITable tableDisplay; // may be null

	private final Form form;

	public FormAndTableDataProviderLookup(FlattenedSolution flattenedSolution, Form form, ITable table)
	{
		this.flattenedSolution = flattenedSolution;
		this.form = form;
		tableDisplay = table;
		formProviders = Collections.synchronizedMap(new HashMap<String, IDataProvider>());
	}

	public IDataProvider getDataProvider(String id) throws RepositoryException
	{
		if (id == null) return null;
		IDataProvider retval = this.flattenedSolution.getGlobalDataProvider(id); // this getGlobalDataProvider handles related DPs as well like a.b.c
		if (retval == null)
		{
			retval = formProviders.get(id);
			if (retval != null)
			{
				return retval;
			}

			retval = this.flattenedSolution.getDataProviderForTable(getTable(), id);
			if (retval != null)
			{
				formProviders.put(id, retval);
			}
			else
			{
				retval = form.getScriptVariable(id);
				if (retval != null)
				{
					formProviders.put(id, retval);
				}
			}
		}
		return retval;
	}

	/**
	 * get the table (may be null)
	 */
	public ITable getTable() throws RepositoryException
	{
		return tableDisplay;
	}
}