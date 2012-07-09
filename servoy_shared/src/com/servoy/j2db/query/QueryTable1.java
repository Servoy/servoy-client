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

import com.servoy.j2db.util.serialize.ReplacedObject;

/**
 * Legacy QueryTable class used for deserialisation of xml from before adding of required field dataSource.
 * 
 * @author rgansevles
 * 
 */
public final class QueryTable1 extends QueryTable
{
	private final String v1name;
	private final String v1alias;
	private final boolean v1needsQuoting;
	private transient String v1catalogName;
	private transient String v1schemaName;
	private final transient boolean v1generatedAlias;
	private transient boolean v1isComplete;

	/** 
	 * Create a full QueryTable from current data and dataSource.
	 */
	public QueryTable addDataSource(String dataSource)
	{
		return new QueryTable(v1name, dataSource, v1alias, v1needsQuoting, v1catalogName, v1schemaName, v1generatedAlias, v1isComplete);
	}

	@Override
	public String getName()
	{
		return v1name;
	}

	@Override
	public String getAlias()
	{
		return v1alias;
	}

	@Override
	public String getCatalogName()
	{
		return v1catalogName;
	}

	@Override
	public String getSchemaName()
	{
		return v1schemaName;
	}

	///////// serialization ////////////////

	@Override
	public Object writeReplace()
	{
		throw new RuntimeException("Legacy QueryTable1 should have been replaced with QueryTable before serialization");
	}

	public QueryTable1(ReplacedObject s)
	{
		super(); // initialise all super fields with null
		// following code is QueryTable v1 deserialisation code, without required field dataSource

		// catalogName and schemaName will be regenerated on the server
		v1isComplete = false;
		v1catalogName = null;
		v1schemaName = null;

		Object o = s.getObject();
		if (o instanceof Object[])
		{
			Object[] members = (Object[])o;
			int i = 0;
			v1name = (String)members[i++];
			if (members.length == 1)
			{
				// [name], needsQuoting = false
				v1needsQuoting = false;
				v1alias = QueryTable.generateAlias(v1name);
				v1generatedAlias = true;
			}
			else if (members.length == 3)
			{
				// [name, alias, needsQuoting]
				v1alias = (String)members[i++];
				v1needsQuoting = ((Boolean)members[i++]).booleanValue();
				v1generatedAlias = false;
			}
			else if (members.length == 4)
			{
				// [name, catalog, schema, needsQuoting]
				v1catalogName = (String)members[i++];
				v1schemaName = (String)members[i++];
				v1isComplete = true;
				v1needsQuoting = ((Boolean)members[i++]).booleanValue();
				v1alias = QueryTable.generateAlias(v1name);
				v1generatedAlias = true;
			}
			else if (members.length == 5)
			{
				// [name, catalog, schema, alias, needsQuoting]
				v1catalogName = (String)members[i++];
				v1schemaName = (String)members[i++];
				v1isComplete = true;
				v1alias = (String)members[i++];
				v1needsQuoting = ((Boolean)members[i++]).booleanValue();
				v1generatedAlias = false;
			}
			else
			{
				// [name, alias, needsQuoting]
				v1alias = (String)members[i++];
				v1needsQuoting = ((Boolean)members[i++]).booleanValue();
				v1generatedAlias = false;
			}
		}
		else
		{ // name
			v1name = (String)o;
			v1needsQuoting = true;
			v1alias = QueryTable.generateAlias(v1name);
			v1generatedAlias = true;
		}
	}
}
