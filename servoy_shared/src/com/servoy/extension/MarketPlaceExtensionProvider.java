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

import java.io.File;

/**
 * TODO
 * @author acostescu
 */
public class MarketPlaceExtensionProvider extends CachingExtensionProvider
{

	public String[] getAvailableVersions(String extensionID)
	{
		// TODO
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.extension.IExtensionProvider#getEXPFile(java.lang.String, java.lang.String)
	 */
	public File getEXPFile(String extensionId, String version)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.extension.CachingExtensionProvider#getDependencyMetadataImpl(com.servoy.extension.ExtensionDependencyDeclaration)
	 */
	@Override
	protected DependencyMetadata[] getDependencyMetadataImpl(ExtensionDependencyDeclaration extensionDependency)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void dispose()
	{
		// TODO delete any temporary files/release any other used resources 
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.extension.IExtensionProvider#getMessages()
	 */
	public Message[] getMessages()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.extension.IExtensionProvider#clearMessages()
	 */
	public void clearMessages()
	{
		// TODO Auto-generated method stub

	}

}
