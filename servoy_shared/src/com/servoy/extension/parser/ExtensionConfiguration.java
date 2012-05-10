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

package com.servoy.extension.parser;

/**
 * An object built from & representing the package.xml of an .exp file.
 * @author acostescu
 */
public class ExtensionConfiguration
{

	protected FullDependencyMetadata dependencyInfo;
	protected Content content;
	protected Info info;
	public final boolean requiresRestart;

	public ExtensionConfiguration(FullDependencyMetadata dependencyInfo, Content content, Info info, boolean requiresRestart)
	{
		this.dependencyInfo = dependencyInfo;
		this.content = content;
		this.info = info;
		this.requiresRestart = requiresRestart;
		// TODO add members for the rest of the xml
	}

	public FullDependencyMetadata getDependencyInfo()
	{
		return dependencyInfo;
	}

	public Content getContent()
	{
		return content;
	}

	public Info getInfo()
	{
		return info;
	}

	/**
	 * Convenience method.
	 */
	public String getExtensionId()
	{
		return dependencyInfo.id;
	}

	/**
	 * Convenience method.
	 */
	public String getExtensionVersion()
	{
		return dependencyInfo.version;
	}

	/**
	 * Convenience method.
	 */
	public String getExtensionName()
	{
		return dependencyInfo.extensionName;
	}

}
