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
package com.servoy.j2db.persistence;

import java.util.HashSet;
import java.util.Set;

public class DataSourceCollectorVisitor implements IPersistVisitor
{
	private final Set<String> dataSources = new HashSet<String>();

	public Set<String> getDataSources()
	{
		dataSources.remove(null);
		return dataSources;
	}

	/**
	 * Visits the IPersist, but without 'checking' the correct references in the repository. Just simply add to the map, the IPersist, server and table
	 * reference;
	 * 
	 * @param child
	 */
	public Object visit(IPersist child)
	{
		// Get the tables used by this object and save them in the export
		// info object, in a map of sets indexed by server name.
		if (child instanceof Form)
		{
			// The object is a form, and a form has a table.
			dataSources.add(((Form)child).getDataSource());
		}
		else if (child instanceof Relation)
		{
			// The object is a relation, and a relation has a primary and
			// foreign table.
			Relation relation = ((Relation)child);
			dataSources.add(relation.getPrimaryDataSource());
			dataSources.add(relation.getForeignDataSource());
		}
		else if (child instanceof TableNode)
		{
			// The object is a table node (script calculation or aggregate
			// variable) and has a table.
			dataSources.add(((TableNode)child).getDataSource());
		}
		else if (child instanceof ValueList)
		{
			// The object is valuelist
			dataSources.add(((ValueList)child).getDataSource());
		}
		return CONTINUE_TRAVERSAL;
	}

}