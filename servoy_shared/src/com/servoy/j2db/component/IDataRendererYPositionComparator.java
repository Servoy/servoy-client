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
package com.servoy.j2db.component;

import java.util.Comparator;

import com.servoy.j2db.ui.IDataRenderer;

public class IDataRendererYPositionComparator implements Comparator<IDataRenderer>
{
	public static final Comparator<IDataRenderer> INSTANCE = new IDataRendererYPositionComparator();

	public int compare(IDataRenderer dr1, IDataRenderer dr2)
	{
		if (dr1 == null)
		{
			if (dr2 == null) return 0;
			else return 1;
		}
		else
		{
			if (dr2 == null) return -1;
			else
			{
				int y1 = dr1.getLocation() == null ? -1 : dr1.getLocation().y;
				int y2 = dr2.getLocation() == null ? -1 : dr2.getLocation().y;
				return new Integer(y1).compareTo(new Integer(y2));
			}
		}
	}
}
