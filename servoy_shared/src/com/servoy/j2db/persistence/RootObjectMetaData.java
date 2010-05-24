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


import java.io.Serializable;

import com.servoy.j2db.util.UUID;

public class RootObjectMetaData extends MetaData implements ISupportName, Serializable
{
	private static final long serialVersionUID = 1L;

	private int rootObjectId;
	private final UUID rootObjectUuid;
	private String name;
	private final int objectTypeId;
	private int activeRelease;
	private int latestRelease;
	private boolean isChanged;

	public RootObjectMetaData(int rootObjectId, UUID rootObjectUuid, String name, int objectTypeId, int activeRelease, int latestRelease)
	{
		this.rootObjectId = rootObjectId;
		this.rootObjectUuid = rootObjectUuid;
		this.name = name;
		this.objectTypeId = objectTypeId;
		this.activeRelease = activeRelease;
		this.latestRelease = latestRelease;
		this.isChanged = false;
	}

	public String getName()
	{
		return name;
	}

	public UUID getRootObjectUuid()
	{
		return rootObjectUuid;
	}

	public int getRootObjectId()
	{
		return rootObjectId;
	}

	public int getObjectTypeId()
	{
		return objectTypeId;
	}

	public int getLatestRelease()
	{
		return latestRelease;
	}

	public int getActiveRelease()
	{
		return activeRelease;
	}

	// need this to set the correct id when a solution is commited from outside
	public void setRootObjectId(int rootObjectId)
	{
		this.rootObjectId = rootObjectId;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setActiveRelease(int activeRelease)
	{
		this.activeRelease = activeRelease;
	}

	public void setLatestRelease(int latestRelease)
	{
		this.latestRelease = latestRelease;
	}

	@Override
	public String toString()
	{
		return name;
	}

	public boolean isChanged()
	{
		return isChanged;
	}

	public void flagChanged()
	{
		isChanged = true;
	}

	public void clearChanged()
	{
		isChanged = false;
	}

	protected void checkForChange(boolean oldValue, boolean newValue)
	{
		if (oldValue != newValue)
		{
			isChanged = true;
		}
	}

	protected void checkForChange(int oldValue, int newValue)
	{
		if (oldValue != newValue)
		{
			isChanged = true;
		}
	}
}
