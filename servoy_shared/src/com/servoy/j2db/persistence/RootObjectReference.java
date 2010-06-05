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

/**
 * A reference to a specific release of a specific solution.
 * 
 * Either the name or the UUID should be not null if the solution
 * was unresolvable; otherwise all fields should be initialized.
 * A release number of -1 indicates the active release.
 * 
 * @author sebster
 *
 */
public class RootObjectReference implements Serializable
{

	private String name;
	
	private UUID uuid;
	
	private RootObjectMetaData metaData;
	
	private int releaseNumber;
	
	public RootObjectReference(RootObjectMetaData metaData, int releaseNumber)
	{
		this.name = metaData.getName();
		this.uuid = metaData.getRootObjectUuid();
		this.metaData = metaData;
		this.releaseNumber = releaseNumber;
	}
	
	public RootObjectReference(String name, UUID uuid, RootObjectMetaData metaData, int releaseNumber)
	{
		this.name = name;
		this.uuid = uuid;
		this.metaData = metaData;
		this.releaseNumber = releaseNumber;
	}

	public UUID getUuid()
	{
		return this.uuid;
	}
	
	public String getName() 
	{
		return this.name;
	}

	public RootObjectMetaData getMetaData()
	{
		return this.metaData;
	}

	public int getReleaseNumber()
	{
		return this.releaseNumber;
	}
		
	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return uuid.hashCode();
	}

	public String toString()
	{
		if (metaData != null)
		{
			return metaData.getName();
		}
		if (name != null)
		{
			return "<" + name + ">";
		}
		if (uuid != null)
		{
			return "<" + uuid + ">";
		}
		return "<invalid solution reference>";
	}

}
