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

import com.servoy.extension.LibDependencyDeclaration;

/**
 * Lib dependency declaration that knows which extension declared it. Convenient for use when resolving dependencies.
 * @author acostescu
 */
public class TrackableLibDependencyDeclaration extends LibDependencyDeclaration implements Serializable
{

	public final String declaringExtensionId;
	public final String declaringExtensionVersion;

	/** For an extension that was already installed before the install/replace/uninstall operation this is true. Else false. */
	public final boolean declaringExtensionInstalled;

	/**
	 * See {@link LibDependencyDeclaration#LibDependencyDeclaration(String, String, String, String)}.
	 * @param declaringExtensionId the extension that defines this lib dependency.
	 * @param declaringExtensionVersion the version of the declaring extension.
	 * @param declaringExtensionInstalled For an extension that was already installed before the install/replace/uninstall operation this is true. Else false.
	 */
	public TrackableLibDependencyDeclaration(String id, String version, String minVersion, String maxVersion, String declaringExtensionId,
		String declaringExtensionVersion, boolean declaringExtensionInstalled) throws IllegalArgumentException
	{
		super(id, version, minVersion, maxVersion);

		this.declaringExtensionId = declaringExtensionId;
		this.declaringExtensionVersion = declaringExtensionVersion;
		this.declaringExtensionInstalled = declaringExtensionInstalled;
	}

	/**
	 * See {@link LibDependencyDeclaration#LibDependencyDeclaration(String, String, String, String)}.
	 * @param declaringExtensionId the extension that defines this lib dependency.
	 */
	public TrackableLibDependencyDeclaration(LibDependencyDeclaration lib, String declaringExtensionId, String declaringExtensionVersion,
		boolean declaringExtensionInstalled)
	{
		this(lib.id, lib.version, lib.minVersion, lib.maxVersion, declaringExtensionId, declaringExtensionVersion, declaringExtensionInstalled);
	}

	@Override
	@SuppressWarnings("nls")
	public String toString()
	{
		return super.toString() + " - '" + declaringExtensionId + ", " + declaringExtensionVersion + "'";
	}
}
