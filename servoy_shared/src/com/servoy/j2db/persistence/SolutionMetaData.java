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

import com.servoy.j2db.server.ApplicationServerSingleton;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author sebster
 * 
 */
public class SolutionMetaData extends RootObjectMetaData
{
	public static final String AFTER_IMPORT_PREFIX = "after_import"; //$NON-NLS-1$
	public static final String BEFORE_IMPORT_PREFIX = "before_import"; //$NON-NLS-1$

	public static final int SOLUTION = 1;
	public static final int MODULE = 2;
	public static final int WEB_CLIENT_ONLY = 4;
	public static final int SMART_CLIENT_ONLY = 8;
	public static final int LOGIN_SOLUTION = 16;
	public static final int AUTHENTICATOR = 32;

	public static final String[] solutionTypeNames = { "Normal", "Module", "Web client only", "Smart client only", "Login", "Authenticator" };//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	public static final int[] solutionTypes = { SOLUTION, MODULE, WEB_CLIENT_ONLY, SMART_CLIENT_ONLY, LOGIN_SOLUTION, AUTHENTICATOR };


	private int solutionType;

	private String protectionPassword;

	private boolean mustAuthenticate;

	private transient int fileVersion = AbstractRepository.repository_version;

	SolutionMetaData(int rootObjectId, UUID rootObjectUuid, String name, int objectTypeId, int activeRelease, int latestRelease)
	{
		super(rootObjectId, rootObjectUuid, name, objectTypeId, activeRelease, latestRelease);
		solutionType = SolutionMetaData.SOLUTION;
		protectionPassword = ApplicationServerSingleton.get().calculateProtectionPassword(this, null);
	}

	public boolean getMustAuthenticate()
	{
		return this.mustAuthenticate;
	}

	public void setMustAuthenticate(boolean arg)
	{
		checkForChange(mustAuthenticate, arg);
		mustAuthenticate = arg;
	}

	public String getProtectionPassword()
	{
		return this.protectionPassword;
	}

	public void setProtectionPassword(String protectionPassword)
	{
		if (!Utils.stringSafeEquals(this.protectionPassword, protectionPassword)) flagChanged();
		this.protectionPassword = protectionPassword;
	}

	public boolean isProtected()
	{
		return ApplicationServerSingleton.get().isSolutionProtected(this);
	}

	public int getSolutionType()
	{
		return this.solutionType;
	}

	public void setSolutionType(int arg)
	{
		checkForChange(solutionType, arg);
		solutionType = arg;
	}

	public int getFileVersion()
	{
		return fileVersion;
	}

	public void setFileVersion(int arg)
	{
		checkForChange(fileVersion, arg);
		fileVersion = arg;
	}

	public static boolean isPreImportHook(String name)
	{
		return name.toLowerCase().startsWith(BEFORE_IMPORT_PREFIX);
	}

	public static boolean isPostImportHook(String name)
	{
		return name.toLowerCase().startsWith(AFTER_IMPORT_PREFIX);
	}

	public static boolean isImportHook(String name)
	{
		return isPreImportHook(name) || isPostImportHook(name);
	}
}