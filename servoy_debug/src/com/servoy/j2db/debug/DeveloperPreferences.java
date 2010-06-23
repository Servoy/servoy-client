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
package com.servoy.j2db.debug;

import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Preferences holder for developer settings.
 * 
 * @author rob
 * 
 */
public class DeveloperPreferences
{
	public static final String DUMMY_AUTHENTICATION_SETTING = "developer.useDummyAuth"; //$NON-NLS-1$
	public static final boolean DUMMY_AUTHENTICATION_DEFAULT = true; // note that the pref is hidden now

	private final Settings settings;

	public DeveloperPreferences(Settings settings)
	{
		this.settings = settings;
	}

	public boolean getEnhancedSecurity()
	{
		return Utils.getAsBoolean(settings.getProperty(Settings.ENHANCED_SECURITY_SETTING, String.valueOf(Settings.ENHANCED_SECURITY_DEFAULT)));
	}

	public void setEnhancedSecurity(boolean enhancedSecurity)
	{
		settings.setProperty(Settings.ENHANCED_SECURITY_SETTING, String.valueOf(enhancedSecurity));
	}

	public boolean getUseDummyAuth()
	{
		return Utils.getAsBoolean(settings.getProperty(DUMMY_AUTHENTICATION_SETTING, String.valueOf(DUMMY_AUTHENTICATION_DEFAULT)));
	}

	public void setUseDummyAuth(boolean useDummyAuth)
	{
		settings.setProperty(DUMMY_AUTHENTICATION_SETTING, String.valueOf(useDummyAuth));
	}
}
