/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.j2db.server.shared;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Properties;

import com.servoy.j2db.util.Debug;

/**
 * @author emera
 */
public class ServerDeployDate
{
	public static Date WAR_DATE = new Date();

	public static void readDate(Properties properties)
	{
		if (properties != null && properties.getProperty("serverBuildDate") != null)
		{
			try
			{
				Long time = Long.valueOf(properties.getProperty("serverBuildDate"));
				ZoneId zone = ZoneId.of(properties.getProperty("zoneId"));
				OffsetDateTime date = OffsetDateTime.ofInstant(Instant.ofEpochMilli(time.longValue()), zone);
				WAR_DATE = new Date(date.toInstant().toEpochMilli());
			}
			catch (Exception ex)
			{
				Debug.error("Could not read war created date: ", ex);
			}
		}
	}

}
