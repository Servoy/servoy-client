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
 * Dependency information listed by an extension in it's extension.xml file.
 * @author acostescu
 */
@SuppressWarnings("nls")
public class DependencyMetadata
{
	public final String id;
	public final String version;
	public final ServoyDependencyDeclaration servoyDependency;
	public final ExtensionDependencyDeclaration[] extensionDependencies;
	public final LibDependencyDeclaration[] libDependencies;

	public DependencyMetadata(String id, String version, ServoyDependencyDeclaration servoyDependency, ExtensionDependencyDeclaration[] extensionDependencies,
		LibDependencyDeclaration[] libDependencies)
	{
		ExtensionUtils.assertValidId(id);
		VersionStringUtils.assertValidVersion(version);

		this.id = id;
		this.version = version;
		this.servoyDependency = servoyDependency;
		this.extensionDependencies = extensionDependencies;
		this.libDependencies = libDependencies;
	}

	@Override
	public String toString()
	{
		return "('" + id + "', " + version + ")";
	}

}