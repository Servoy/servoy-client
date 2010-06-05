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


/**
 * 
 * @author asisu
 * 
 */
public class ReplaceTableVisitor implements IPersistVisitor
{
	private final String sourceDataSource;
	private final String targetDataSource;


	public ReplaceTableVisitor(String sourceDataSource, String targetDataSource)
	{
		this.sourceDataSource = sourceDataSource;
		this.targetDataSource = targetDataSource;
	}

	public Object visit(IPersist object)
	{
		if (sourceDataSource != null && targetDataSource != null)
		{
			if (object instanceof Form)
			{
				// The object is a form, and a form has a table.
				Form form = ((Form)object);
				if (sourceDataSource.equals(form.getDataSource()))
				{
					form.setDataSource(targetDataSource);
				}
			}
			else if (object instanceof Relation)
			{
				// The object is a relation, and a relation has a primary and foreign table.
				Relation relation = ((Relation)object);
				if (sourceDataSource.equals(relation.getPrimaryDataSource()))
				{
					relation.setPrimaryDataSource(targetDataSource);
				}
				if (sourceDataSource.equals(relation.getForeignDataSource()))
				{
					relation.setForeignDataSource(targetDataSource);
				}
			}
			else if (object instanceof TableNode)
			{
				// The object is a table node (script calculation or aggregate variable) and has a table.
				// do not replace
			}
			else if (object instanceof ValueList)
			{
				// The object is valuelist
				ValueList vl = ((ValueList)object);
				if (sourceDataSource.equals(vl.getDataSource()))
				{
					vl.setDataSource(targetDataSource);
				}
			}
		}
		return IPersistVisitor.CONTINUE_TRAVERSAL;
	}
}
