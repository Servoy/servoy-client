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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.solutionmodel.ISMFontStyle;

/**
 * The <code>FONTSTYLE</code> and <code>ISMFontStyle</code> components provide constants for defining font styles in Servoy,
 * such as <code>PLAIN</code>, <code>BOLD</code>, <code>ITALIC</code>, and <code>BOLD_ITALIC</code>. These constants enable
 * developers to customize text appearance in forms and components, streamlining font style management in scripting.
 *
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class FONTSTYLE implements ISMFontStyle, IPrefixedConstantsObject
{

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_FONTSTYLE"; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Font Style Constants"; //$NON-NLS-1$
	}
}
