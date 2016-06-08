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
 * Base class and generic implementation for a type of DB server's connection attributes.
 * It is meant to be extended per DB type to split URL into multiple simpler fields and be able to suggest driver download page/other details.
 *
 * @author gboros
 */
public class ServerTemplateDefinition
{
	public static final String JDBC_URL_DESCRIPTION = "URL of the JDBC connection to the DB.\nPlease see the documentation of the specific JDBC driver used to connect to your database (advanced -> driver).";

	protected ServerConfig template;

	public ServerTemplateDefinition(ServerConfig template)
	{
		this.template = template;
	}

	public ServerConfig getTemplate()
	{
		return template;
	}

	public String[] getUrlKeys()
	{
		return new String[] { "URL" };
	}

	public String[] getUrlKeyDescriptions()
	{
		return new String[] { JDBC_URL_DESCRIPTION };
	}

	public String[] getUrlValues(String url)
	{
		return new String[] { url };
	}

	public String getUrlForValues(String[] values)
	{
		return values != null && values.length == 1 ? values[0] : null;
	}

	public String getDriverDownloadURL()
	{
		return null;
	}

}
