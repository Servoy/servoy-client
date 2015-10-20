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

package com.servoy.base.util;

/**
 * @author gboros
 *
 */
public interface I18NProvider
{
	public final static String MOBILE_KEY_PREFIX = "servoy.mobile."; //$NON-NLS-1$

	/**
	 * get a i18n message for the given key
	 * 
	 * @param i18nKey
	 * @return a String for the given key.
	 */
	public String getI18NMessage(String i18nKey);

	/**
	 * get a i18n message for the given key and array
	 * 
	 * @param i18nKey
	 * @param array
	 * @return a String for the given key.
	 */
	public String getI18NMessage(String i18nKey, Object[] array);

	/**
	 * get a i18n message for the given key if the key is prefixed with i18n. If it is not prefixed the key it self is returned
	 * 
	 * @param i18nKey
	 * @return a String for the given key or the key itself
	 */
	public String getI18NMessageIfPrefixed(String i18nKey);

	/**
	 * set an i18n message client side
	 * 
	 * @param i18nKey
	 * @param value
	 */
	void setI18NMessage(String i18nKey, String value);
}
