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
import org.apache.wicket.markup.html.list.ListItem;

import com.servoy.j2db.server.headlessclient.dataui.WebCellBasedView.CellContainer;

/**
 * Utility class for determining the markup ID of a given component. Uses the servoy id of that component and adds indexes in case of table/list view.
 * <p>
 * Called by overriden methods of {@link Component#getMarkupId()}
 * </p>
 * @author acostescu
 * @since 5.0
 */
public class WebComponentSpecialIdMaker
{

	/**
	 * Determines the markup ID of a given component. Uses the servoy id of that component and adds indexes in case of table/list view in order to make id's
	 * unique.
	 * 
	 * @param component the component who's markup id is computed and returned.
	 * @return the markup id for the given component.
	 */
	public static String getSpecialIdIfAppropriate(Component component)
	{
		if (component.getParent() instanceof ListItem)
		{
			return component.getParent().getId() + Component.PATH_SEPARATOR + component.getId();
		}
		else if (component.getParent() instanceof CellContainer)
		{
			ListItem li = (ListItem)component.getParent().getParent();
			if (li != null)
			{
				return li.getId() + Component.PATH_SEPARATOR + component.getId();
			}
		}
		return component.getId();
	}

	public static String getSpecialId(int selectedIndex, Component component)
	{
		return (new Integer(selectedIndex).toString() + Component.PATH_SEPARATOR + component.getId());
	}
}
