/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

/**
 * @author lvostinar
 *
 */
public class ValuelistConfigTypeSabloValue
{
	private final String filterType;
	private final String filterDestination;
	private final boolean allowNewEntries;

	public static final String STARTS_WITH = "STARTS_WITH";
	public static final String CONTAINS = "CONTAINS";
	public static final String DISPLAY_VALUE = "DISPLAY_VALUE";
	public static final String DISPLAY_AND_REAL_VALUE = "DISPLAY_AND_REAL_VALUE";

	public ValuelistConfigTypeSabloValue(String filterType, String filterDestination, boolean allowNewEntries)
	{
		this.filterType = filterType;
		this.filterDestination = filterDestination;
		this.allowNewEntries = allowNewEntries;
	}

	public boolean useFilterOnRealValues()
	{
		return DISPLAY_AND_REAL_VALUE.equals(filterDestination);
	}

	public boolean useFilterWithContains()
	{
		return CONTAINS.equals(filterType);
	}

	/**
	 * @return the filterDestination
	 */
	public String getFilterDestination()
	{
		return filterDestination;
	}

	/**
	 * @return the filterType
	 */
	public String getFilterType()
	{
		return filterType;
	}

	/**
	 * @return
	 */
	public boolean getAllowNewEntries()
	{
		return allowNewEntries;
	}
}
