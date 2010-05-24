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


/**
 * @author Jan Blok
 */
public interface ISupportScrollbars
{
	/**
	 * Scrollbar options for the vertical and horizontal scrollbars. Each of the
	 * vertical and horizontal scrollbars can be configured to display all the time,
	 * to display only when needed or to never display.
	 */
	public int getScrollbars();

	int SCROLLBARS_WHEN_NEEDED = 0;

	/**
	 * Used to set the vertical scroll bar policy so that 
	 * vertical scrollbars are displayed only when needed.
	 */
	int VERTICAL_SCROLLBAR_AS_NEEDED = 1;

	/**
	 * Used to set the vertical scroll bar policy so that 
	 * vertical scrollbars are always displayed.
	 */
	int VERTICAL_SCROLLBAR_ALWAYS = 2;
	int VERTICAL_SCROLLBAR_NEVER = 4;

	/**
	 * Used to set the horizontal scroll bar policy so that 
	 * horizontal scrollbars are displayed only when needed.
	 */
	int HORIZONTAL_SCROLLBAR_AS_NEEDED = 8;

	/**
	 * Used to set the horizontal scroll bar policy so that 
	 * horizontal scrollbars are always displayed.
	 */
	int HORIZONTAL_SCROLLBAR_ALWAYS = 16;
	int HORIZONTAL_SCROLLBAR_NEVER = 32;

}
