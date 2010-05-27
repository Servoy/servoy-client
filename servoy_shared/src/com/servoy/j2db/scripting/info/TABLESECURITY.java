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
package com.servoy.j2db.scripting.info;

import com.servoy.j2db.annotations.ServoyDocumented;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@Deprecated
public class TABLESECURITY implements IPrefixedConstantsObject, IDeprecated
{
	public static final int READ = IRepository.READ;
	public static final int INSERT = IRepository.INSERT;
	public static final int UPDATE = IRepository.UPDATE;
	public static final int DELETE = IRepository.DELETE;
	public static final int TRACKING = IRepository.TRACKING;

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_TABLESECURITY"; //$NON-NLS-1$
	}

	@Override
	public String toString()
	{
		return "Security Manager Constants"; //$NON-NLS-1$
	}
}
