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
public class MSSQLTemplate extends ServerTemplateDefinition
{
	private final String urlPattern = "jdbc:sqlserver://<host_name>:1433;DatabaseName=<database_name>;SelectMethod=direct;sendTimeAsDateTime=false;encrypt=true;trustServerCertificate=true";

	public MSSQLTemplate()
	{
		super(new ServerConfig.Builder()
			.setServerName("new_mssql")
			.setUserName("sa")
			.setPassword("")
			.setServerUrl(
				"jdbc:sqlserver://localhost:1433;DatabaseName=<database_name>;SelectMethod=direct;sendTimeAsDateTime=false;encrypt=true;trustServerCertificate=true")
			.setDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver")
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
		return new String[] { "The host name where your MS SQL DB server can be found.", "MS SQL database name to connect to." };
	}

	@Override
	public String[] getUrlValues(String url)
	{
		String server = "";
		String db = "";
		if (url != null)
		{
			int startIdx = "jdbc:sqlserver://".length();
			if (url.startsWith("jdbc:sqlserver://") && url.length() > startIdx)
			{
				int endIdx = url.indexOf(":", startIdx);
				if (endIdx < 0)
				{
					//no port
					endIdx = url.indexOf(";", startIdx);
				}
				if (endIdx > 0)
				{
					server = url.substring(startIdx, endIdx);

					startIdx = url.indexOf("DatabaseName=");
					if (startIdx != -1)
					{
						startIdx += "DatabaseName=".length();
						if (startIdx < url.length() - 2)
						{
							endIdx = url.indexOf(";", startIdx);
							if (endIdx != -1)
							{
								db = url.substring(startIdx, endIdx);
							}
						}
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
		if (oldUrl != null && !oldUrl.contains(":1433;"))
		{
			int startIndex = oldUrl.indexOf(":", "jdbc:sqlserver://".length());
			int endIndex = oldUrl.indexOf(";");
			if (startIndex > 0 && endIndex > 0 && endIndex < url.length() - 1)
			{
				int port = Utils.getAsInteger(oldUrl.substring(startIndex + 1, endIndex));
				if (port > 0)
				{
					url = url.replace("1433", String.valueOf(port));
				}
			}
		}
		return url;
	}

	@Override
	public String getDriverDownloadURL()
	{
		return "https://msdn.microsoft.com/en-us/library/ms378526%28v=sql.110%29.aspx";
	}
}
