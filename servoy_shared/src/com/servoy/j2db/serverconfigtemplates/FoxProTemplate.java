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
public class FoxProTemplate extends ServerTemplateDefinition
{
	public FoxProTemplate()
	{
		super(new ServerConfig("new_dbf", "", "", "jdbc:DBF:/C:/TEMP?lockType=VFP&versionNumber=DB2K&delayedClose=0", null, "com.hxtt.sql.dbf.DBFDriver", null,
			null, ServerConfig.MAX_ACTIVE_DEFAULT, ServerConfig.MAX_IDLE_DEFAULT, 0 /* disable PS pool */, ServerConfig.VALIDATION_TYPE_DEFAULT, null, null,
			true, false, false, -1, null));
	}
}
