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

import com.servoy.base.query.IJoinConstants;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@Deprecated
public class JOINTYPE implements IPrefixedConstantsObject, IDeprecated
{
	/**
	 * @deprecated replaced by JSRelation.INNER_JOIN
	 */
	@Deprecated
	public static final int INNER_JOIN = IJoinConstants.INNER_JOIN;

	/**
	 * @deprecated replaced by JSRelation.LEFT_OUTER_JOIN
	 */
	@Deprecated
	public static final int LEFT_OUTER_JOIN = IJoinConstants.LEFT_OUTER_JOIN;

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_JOINTYPE"; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Join type Constants"; //$NON-NLS-1$
	}
}
