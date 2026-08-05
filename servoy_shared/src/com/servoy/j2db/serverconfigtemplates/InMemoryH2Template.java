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

public class InMemoryH2Template extends ServerTemplateDefinition
{
	public InMemoryH2Template()
	{
		super(new ServerConfig.Builder()
			.setServerName("new_inmem_h2")
			.setUserName("sa")
			.setPassword("")
			.setServerUrl("jdbc:h2:mem:.;DB_CLOSE_DELAY=-1")
			.setDriver("org.h2.Driver")
			.build());
	}
}
