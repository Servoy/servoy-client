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
package com.servoy.j2db.scripting.solutionmodel;

import com.servoy.j2db.annotations.ServoyDocumented;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class SCROLLBAR implements IPrefixedConstantsObject
{
	/**
	 * Used to set the horizontal and vertical scroll bar policy so that both scrollbars are displayed
	 * only when needed.
	 * 
	 * @sample
	 * var neededScrollbars = form.newField('my_table_text', JSField.TEXT_AREA, 120, 10, 100, 100);
	 * // This is the default option, but if you really want you can set it explicitly.
	 * neededScrollbars.scrollbars = SM_SCROLLBAR.SCROLLBARS_WHEN_NEEDED;
	 */
	public static final int SCROLLBARS_WHEN_NEEDED = ISupportScrollbars.SCROLLBARS_WHEN_NEEDED;

	/**
	 * Used to set the vertical scroll bar policy so that vertical scrollbars are displayed only when needed.
	 * 
	 * @sample
	 * var neededScrollbars = form.newField('my_table_text', JSField.TEXT_AREA, 120, 10, 100, 100);
	 * neededScrollbars.scrollbars = SM_SCROLLBAR.HORIZONTAL_SCROLLBAR_AS_NEEDED | SM_SCROLLBAR.VERTICAL_SCROLLBAR_AS_NEEDED;
	 */
	public static final int VERTICAL_SCROLLBAR_AS_NEEDED = ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED;

	/**
	 * Used to set the vertical scroll bar policy so that vertical scrollbars are always displayed.
	 * 
	 * @sample
	 * var alwaysScrollbars = form.newField('my_table_text', JSField.TEXT_AREA, 230, 10, 100, 100);
	 * alwaysScrollbars.scrollbars = SM_SCROLLBAR.HORIZONTAL_SCROLLBAR_ALWAYS | SM_SCROLLBAR.VERTICAL_SCROLLBAR_ALWAYS;
	 */
	public static final int VERTICAL_SCROLLBAR_ALWAYS = ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS;

	/**
	 * Used to set the vertical scroll bar policy so that vertical scrollbars are never displayed.
	 * 
	 * @sample
	 * var noScrollbars = form.newField('my_table_text', JSField.TEXT_AREA, 10, 10, 100, 100);
	 * noScrollbars.scrollbars = SM_SCROLLBAR.HORIZONTAL_SCROLLBAR_NEVER | SM_SCROLLBAR.VERTICAL_SCROLLBAR_NEVER;
	 */
	public static final int VERTICAL_SCROLLBAR_NEVER = ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER;

	/**
	 * Used to set the horizontal scroll bar policy so that horizontal scrollbars are displayed only when needed.
	 * 
	 * @sample
	 * var neededScrollbars = form.newField('my_table_text', JSField.TEXT_AREA, 120, 10, 100, 100);
	 * neededScrollbars.scrollbars = SM_SCROLLBAR.HORIZONTAL_SCROLLBAR_AS_NEEDED | SM_SCROLLBAR.VERTICAL_SCROLLBAR_AS_NEEDED;
	 */
	public static final int HORIZONTAL_SCROLLBAR_AS_NEEDED = ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED;

	/**
	 * Used to set the horizontal scroll bar policy so that horizontal scrollbars are always displayed.
	 * 
	 * @sample
	 * var alwaysScrollbars = form.newField('my_table_text', JSField.TEXT_AREA, 230, 10, 100, 100);
	 * alwaysScrollbars.scrollbars = SM_SCROLLBAR.HORIZONTAL_SCROLLBAR_ALWAYS | SM_SCROLLBAR.VERTICAL_SCROLLBAR_ALWAYS;
	 */
	public static final int HORIZONTAL_SCROLLBAR_ALWAYS = ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS;

	/**
	 * Used to set the horizontal scroll bar policy so that horizontal scrollbars are never displayed.
	 * 
	 * @sample
	 * var noScrollbars = form.newField('my_table_text', JSField.TEXT_AREA, 10, 10, 100, 100);
	 * noScrollbars.scrollbars = SM_SCROLLBAR.HORIZONTAL_SCROLLBAR_NEVER | SM_SCROLLBAR.VERTICAL_SCROLLBAR_NEVER;
	 */
	public static final int HORIZONTAL_SCROLLBAR_NEVER = ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER;

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_SCROLLBAR"; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Scrollbar Constants"; //$NON-NLS-1$
	}

}
