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

import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@Deprecated
public class VIEW implements IPrefixedConstantsObject, IDeprecated
{
	public static final int RECORD_VIEW = IForm.RECORD_VIEW;
	public static final int LIST_VIEW = IForm.LIST_VIEW;
//	public static final int TABLE_VIEW = FormController.TABLE_VIEW;
	public static final int LOCKED_TABLE_VIEW = FormController.LOCKED_TABLE_VIEW;
	public static final int LOCKED_LIST_VIEW = FormController.LOCKED_LIST_VIEW;
	public static final int LOCKED_RECORD_VIEW = FormController.LOCKED_RECORD_VIEW;

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_VIEW"; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "View Constants"; //$NON-NLS-1$
	}
}
