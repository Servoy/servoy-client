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
package com.servoy.j2db.server.headlessclient.dataui;

import org.apache.wicket.Component;

/**
 * Utility class for determining the markup ID of a given component. Uses the servoy id of that component and adds indexes in case of table/list view.
 * <p>
 * Called by overriden methods of {@link Component#getMarkupId()}
 * </p>
 * @deprecated as of version 6.0 components should just fallback to default wicket markup id
 * @author acostescu
 * @since 5.0
 */
@Deprecated
public class WebComponentSpecialIdMaker
{
	/**
	 * Determines the markup ID of a given component. Uses the servoy id of that component and adds indexes in case of table/list view in order to make id's
	 * unique.
	 * 
	 * @deprecated as of version 6.0 components should just fallback to default wicket markup id
	 * @param component the component who's markup id is computed and returned.
	 * @return the markup id for the given component.
	 */
	@Deprecated
	public static String getSpecialIdIfAppropriate(Component component)
	{
		return component.getMarkupId(true);
	}

	/**
	 * @deprecated as of version 6.0 components should just fallback to default wicket markup id
	 */
	@Deprecated
	public static String getSpecialId(int selectedIndex, Component component)
	{
		return component.getMarkupId(true);
	}
}
