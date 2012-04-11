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
 * Extension provider that is able to find extensions from multiple sources.
 * For example you might want to combine a local folder extension provider with a Marketplace extension provider. Or you might want to combine two local folders extension providers.
 * @author acostescu
 */
public class ComposedExtensionProvider implements IExtensionProvider
{

	protected final IExtensionProvider provider1;
	protected final IExtensionProvider provider2;

	/**
	 * Creates a new composed extension provider. This will get extension related data from both specified sources.<br>
	 * It will try to find what it needs in provider1 and then in provider2. In case of duplicate entries only one will be returned - the
	 * one from provider1.
	 * @param provider1 the higher priority extension provider.
	 * @param provider2 the lower priority extension provider.
	 */
	public ComposedExtensionProvider(IExtensionProvider provider1, IExtensionProvider provider2)
	{
		this.provider1 = provider1;
		this.provider2 = provider2;
	}

	public DependencyMetadata[] getDependencyMetadata(ExtensionDependencyDeclaration extensionDependency)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
