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

package com.servoy.j2db.util.xmlxport;

/**
 * The <code>VersionInfo</code> class contains the XML version and the repository version information of an export.
 */
public class VersionInfo
{
	/**
	 * The XML version found in the import.
	 */
	public int xmlVersion;

	/**
	 * The repository version found in the import.
	 */
	public int repositoryVersion;

	/**
	 * The Servoy version used for the export.
	 */
	public String servoyVersion;

	/**
	 * The Servoy build number used for the export.
	 */
	public int buildNumber;

	/**
	 * The digest of the version info file.
	 */
	public byte[] digest = null;

	/**
	 * Constructs a new <code>VersionInfo</code> object with the specified XML and repository versions.
	 * 
	 * @param xmlVersion the XML version
	 * @param repositoryVersion the repository version
	 */
	public VersionInfo(int xmlVersion, int repositoryVersion, String servoyVersion, int buildNumber)
	{
		this.xmlVersion = xmlVersion;
		this.repositoryVersion = repositoryVersion;
		this.servoyVersion = servoyVersion;
		this.buildNumber = buildNumber;
	}

	/**
	 * Get the xml version of this version info object.
	 * 
	 * @return the XML version
	 */
	public int getXMLVersion()
	{
		return xmlVersion;
	}

	/**
	 * Get the repository version of this version info object.
	 * 
	 * @return the repository version
	 */
	public int getRepositoryVersion()
	{
		return repositoryVersion;
	}

	/**
	 * Get the Servoy version of this version info object.
	 * 
	 * @return the Servoy version
	 */
	String getServoyVersion()
	{
		return servoyVersion;
	}

	/**
	 * Get the build number of this version info object.
	 * 
	 * @return the build number
	 */
	public int getBuildNumber()
	{
		return buildNumber;
	}

	/**
	 * Get the digest.
	 * 
	 * @return the digest
	 */
	public byte[] getDigest()
	{
		return digest;
	}
}
