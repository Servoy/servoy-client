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

package com.servoy.extension;


/**
 * Library dependency declaration (as defined in extension.xml)
 * @author acostescu
 */
@SuppressWarnings("nls")
public class LibDependencyDeclaration
{

	/** the library's id */
	public final String id;
	/** the version of the library that is included with the extension package */
	public final String version;
	/** minimum version. Can be {@link VersionStringUtils#UNBOUNDED} */
	public final String minVersion;
	/** maximum version. Can be {@link VersionStringUtils#UNBOUNDED} */
	public final String maxVersion;

	/**
	 * Creates a new library dependency declaration (as defined in extension.xml).
	 * @param id the library's id.
	 * @param version the version of the library that is included with the extension package.
	 * @param minVersion minimum version. Can be {@link VersionStringUtils#UNBOUNDED}.
	 * @param maxVersion maximum version. Can be {@link VersionStringUtils#UNBOUNDED}.
	 * @throws IllegalArgumentException if any of the arguments or their combination is invalid.
	 */
	public LibDependencyDeclaration(String id, String version, String minVersion, String maxVersion) throws IllegalArgumentException
	{
		ExtensionUtils.assertValidId(id);
		if (!VersionStringUtils.belongsToInterval(version, minVersion, maxVersion))
		{
			throw new IllegalArgumentException("Version of lib being delivered with this extension must be within accepted minVersion, maxVersion range. " +
				this);
		}
		else if (minVersion == VersionStringUtils.UNBOUNDED && maxVersion == VersionStringUtils.UNBOUNDED)
		{
			throw new IllegalArgumentException("Illegal combination of lib version, maxVersion, minVersion. 'Any version' dependency is not allowed. " + this);
		}
		VersionStringUtils.assertValidVersion(version);
		VersionStringUtils.assertValidMinMaxVersion(minVersion);
		VersionStringUtils.assertValidMinMaxVersion(maxVersion);

		this.id = id;
		this.version = version;
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
	}

	@Override
	public String toString()
	{
		return "('" + id + "', " + version + ", " + minVersion + ", " + maxVersion + ")";
	}

}