/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.extension.dependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.servoy.extension.VersionStringUtils;

/**
 * Node in the resolved dependency tree path (which is a list).<br>
 * It also specifies the operation used that lead to this node being considered (update installed extension/simple dependency resolve).
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ExtensionNode implements Serializable
{

	public final static int SIMPLE_DEPENDENCY_RESOLVE = 1;
	public final static int UPGRADE_RESOLVE = 2;
	public final static int DOWNGRADE_RESOLVE = 3;
	public final static int UNINSTALL_RESOLVE = 4;

	public final int resolveType;
	public final String id;

	/** The already installed version; will be null in case of SIMPLE_DEPENDENCY_RESOLVE. */
	public final String installedVersion;

	/** The new version; will be null in case of UNINSTALL_RESOLVE. */
	public final String version;


	/** 
	 * used for computing the install order; is only filled properly when a new
	 * dependency resolving result was found and is created, afterwards it will keep changing
	 */
	protected transient List<ExtensionNode> depChildren = new ArrayList<ExtensionNode>();
	/** 
	 * used for computing the install order; is only filled properly when a new
	 * dependency resolving result was found and is created, afterwards it will keep changing
	 */
	protected transient List<ExtensionNode> brokenDepChildren = new ArrayList<ExtensionNode>();

	/**
	 * At least one of version and installeVersion must not be null.
	 */
	public ExtensionNode(String id, String version, String installedVersion)
	{
		this.id = id;
		this.version = version;
		this.installedVersion = installedVersion;
		if (installedVersion == null)
		{
			this.resolveType = SIMPLE_DEPENDENCY_RESOLVE;
		}
		else if (version == null)
		{
			this.resolveType = UNINSTALL_RESOLVE;
		}
		else
		{
			this.resolveType = (VersionStringUtils.compareVersions(version, installedVersion) > 0) ? UPGRADE_RESOLVE : DOWNGRADE_RESOLVE;
		}
	}

	public ExtensionNode(String id, String version, int resolveType)
	{
		this.id = id;
		this.version = version;
		this.resolveType = resolveType;
		installedVersion = null;
	}

	@Override
	public String toString()
	{
		String type = String.valueOf(resolveType);
		switch (resolveType)
		{
			case SIMPLE_DEPENDENCY_RESOLVE :
				type = "S";
				break;
			case UPGRADE_RESOLVE :
				type = "U";
				break;
			case DOWNGRADE_RESOLVE :
				type = "D";
				break;
		}
		return "('" + id + "', " + version + ", " + type + ")";
	}

}