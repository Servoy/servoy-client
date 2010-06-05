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
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@Deprecated
public class DISPLAYTYPE implements IPrefixedConstantsObject, IDeprecated
{
	public static final int TEXT_FIELD = Field.TEXT_FIELD;
	public static final int TEXT_AREA = Field.TEXT_AREA;
	public static final int COMBOBOX = Field.COMBOBOX;
	public static final int RADIOS = Field.RADIOS;
	public static final int CHECKS = Field.CHECKS;
	public static final int CALENDAR = Field.CALENDAR;
	public static final int PASSWORD = Field.PASSWORD;
	public static final int RTF_AREA = Field.RTF_AREA;
	public static final int HTML_AREA = Field.HTML_AREA;
	public static final int IMAGE_MEDIA = Field.IMAGE_MEDIA;
	public static final int TYPE_AHEAD = Field.TYPE_AHEAD;

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_DISPLAYTYPE"; //$NON-NLS-1$
	}

	@Override
	public String toString()
	{
		return "Display Type Constants"; //$NON-NLS-1$
	}
}
