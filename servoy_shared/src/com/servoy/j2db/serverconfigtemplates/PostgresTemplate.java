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
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 *
 */
public class PostgresTemplate extends ServerTemplateDefinition
{
	private final String urlPattern = "jdbc:postgresql://<host_name>:5432/<database_name>";

	public PostgresTemplate()
	{
		super(new ServerConfig.Builder()
			.setServerName("new_postgresql")
			.setUserName("DBA")
			.setPassword("")
			.setServerUrl("jdbc:postgresql://localhost:5432/<database_name>")
			.setDriver("org.postgresql.Driver")
			.build());
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
	public String getUrlForValues(String[] values, String oldUrl)
	{
		String server = "";
		String db = "";
		if (values != null && values.length > 1)
		{
			if (values[0] != null) server = values[0];
			if (values[1] != null) db = values[1];
		}

		String url = urlPattern.replace("<host_name>", server).replace("<database_name>", db);
		if (oldUrl != null && !oldUrl.contains(":5432/"))
		{
			int startIndex = oldUrl.indexOf(":", "jdbc:postgresql://".length());
			int endIndex = oldUrl.lastIndexOf("/");
			if (startIndex > 0 && endIndex > 0 && endIndex < url.length() - 1)
			{
				int port = Utils.getAsInteger(oldUrl.substring(startIndex + 1, endIndex));
				if (port > 0)
				{
					url = url.replace("5432", String.valueOf(port));
				}
			}
		}
		return url;
	}

}
