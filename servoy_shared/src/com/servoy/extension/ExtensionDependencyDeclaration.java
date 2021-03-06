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
 * Extension dependency declaration (as defined in package.xml)
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ExtensionDependencyDeclaration
{
	/** the extension's id */
	public final String id;
	/** minimum version (inclusive). Can be {@link VersionStringUtils#UNBOUNDED} */
	public final String minVersion;
	/** maximum version (inclusive). Can be {@link VersionStringUtils#UNBOUNDED} */
	public final String maxVersion;

	/**
	 * Creates a new extension dependency declaration (as defined in package.xml).
	 * @param id the extension's id.
	 * @param minVersion minimum version (inclusive). Can be {@link VersionStringUtils#UNBOUNDED}.
	 * @param maxVersion maximum version (inclusive). Can be {@link VersionStringUtils#UNBOUNDED}.
	 */
	public ExtensionDependencyDeclaration(String id, String minVersion, String maxVersion)
	{
		ExtensionUtils.assertValidId(id);
		VersionStringUtils.assertValidMinMaxVersion(minVersion);
		VersionStringUtils.assertValidMinMaxVersion(maxVersion);

		this.id = id;
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
	}

	@Override
	public String toString()
	{
		return "('" + id + "', " + minVersion + ", " + maxVersion + ")";
	}
}