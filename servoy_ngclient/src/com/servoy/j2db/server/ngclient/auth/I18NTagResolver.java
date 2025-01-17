/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.server.ngclient.auth;

import java.util.Locale;

import com.servoy.base.util.I18NProvider;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.AngularIndexPageWriter;

/**
 * @author jcompagner
 *
 */
public final class I18NTagResolver implements I18NProvider
{
	private final Locale locale;
	private final Solution sol;

	/**
	 * @param request
	 * @param sol
	 */
	public I18NTagResolver(Locale locale, Solution sol)
	{
		this.locale = locale;
		this.sol = sol;
	}

	@Override
	public String getI18NMessage(String i18nKey)
	{
		return AngularIndexPageWriter.getSolutionDefaultMessage(sol, locale, i18nKey);
	}

	@Override
	public String getI18NMessage(String i18nKey, String language, String country)
	{
		return getI18NMessage(i18nKey);
	}

	@Override
	public String getI18NMessage(String i18nKey, Object[] array)
	{
		return getI18NMessage(i18nKey);
	}

	@Override
	public String getI18NMessage(String i18nKey, Object[] array, String language, String country)
	{
		return getI18NMessage(i18nKey);
	}

	@Override
	public String getI18NMessageIfPrefixed(String key)
	{
		if (key != null && key.startsWith("i18n:")) //$NON-NLS-1$
		{
			return getI18NMessage(key.substring(5), null);
		}
		return key;
	}

	@Override
	public void setI18NMessage(String i18nKey, String value)
	{
	}
}