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
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@Deprecated
public class PARTS implements IPrefixedConstantsObject, IDeprecated
{
	public static final int BODY = Part.BODY;
	public static final int FOOTER = Part.FOOTER;
	public static final int HEADER = Part.HEADER;
	public static final int LEADING_GRAND_SUMMARY = Part.LEADING_GRAND_SUMMARY;
	public static final int LEADING_SUBSUMMARY = Part.LEADING_SUBSUMMARY;
	public static final int TITLE_FOOTER = Part.TITLE_FOOTER;
	public static final int TITLE_HEADER = Part.TITLE_HEADER;
	public static final int TRAILING_GRAND_SUMMARY = Part.TRAILING_GRAND_SUMMARY;
	public static final int TRAILING_SUBSUMMARY = Part.TRAILING_SUBSUMMARY;

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_PARTS"; //$NON-NLS-1$
	}

	@Override
	public String toString()
	{
		return "Part Constants"; //$NON-NLS-1$
	}
}
