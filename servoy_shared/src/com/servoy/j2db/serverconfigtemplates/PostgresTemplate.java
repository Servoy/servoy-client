/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.j2db.serverconfigtemplates;

import com.servoy.j2db.persistence.ServerConfig;

/**
 * @author gboros
 *
 */
public class PostgresTemplate extends ServerTemplateDefinition
{
	private final String urlPattern = "jdbc:postgresql://<host_name>:5432/<database_name>";

	public PostgresTemplate()
	{
		super(new ServerConfig("new_postgresql", "DBA", "", "jdbc:postgresql://localhost:5432/<database_name>", null, "org.postgresql.Driver", null, null, true,
			false, null));
	}

	@Override
	public String[] getUrlKeys()
	{
		return new String[] { "Host name", "Database name" };
	}

	@Override
	public String[] getUrlKeyDescriptions()
	{
		return new String[] { "The host name where your Postgres server can be found.", "Postgres database name to connect to." };
	}

	@Override
	public String[] getUrlValues(String url)
	{
		String server = "";
		String db = "";
		if (url != null)
		{
			int startIdx = "jdbc:postgresql://".length();
			if (url.startsWith("jdbc:postgresql://") && url.length() > startIdx)
			{
				int endIdx = url.indexOf(":", startIdx);
				if (endIdx > 0)
				{
					server = url.substring(startIdx, endIdx);
					endIdx = url.lastIndexOf("/");
					if (endIdx > 0 && endIdx < url.length() - 1)
					{
						db = url.substring(endIdx + 1);
					}
				}
			}
		}
		return new String[] { server, db };
	}

	@Override
	public String getUrlForValues(String[] values)
	{
		String server = "";
		String db = "";
		if (values != null && values.length > 1)
		{
			if (values[0] != null) server = values[0];
			if (values[1] != null) db = values[1];
		}

		return urlPattern.replace("<host_name>", server).replace("<database_name>", db);
	}

}
