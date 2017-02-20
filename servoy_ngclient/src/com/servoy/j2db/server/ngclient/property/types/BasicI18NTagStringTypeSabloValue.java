/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import com.servoy.j2db.server.ngclient.DataAdapterList;

/**
 * @author jcompagner
 *
 */
public class BasicI18NTagStringTypeSabloValue extends BasicTagStringTypeSabloValue implements II18NValue
{

	private final String i18nKey;

	/**
	 * @param designValue
	 * @param dataAdapterList
	 */
	public BasicI18NTagStringTypeSabloValue(String designValue, DataAdapterList dataAdapterList, String i18nKey)
	{
		super(designValue, dataAdapterList);
		this.i18nKey = i18nKey;
	}

	@Override
	public String getI18NKey()
	{
		return i18nKey;
	}

	@Override
	public DataAdapterList getDataAdapterList()
	{
		return super.getDataAdapterList();
	}

}
