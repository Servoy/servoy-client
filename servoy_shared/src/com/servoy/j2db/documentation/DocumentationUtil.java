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

package com.servoy.j2db.documentation;

import com.servoy.j2db.documentation.scripting.docs.Function;

/**
 * Utility class for documentation management.
 * 
 * @author gerzse
 */
public class DocumentationUtil
{

	public static Class< ? > loadClass(String type) throws ClassNotFoundException
	{
		if (type == null) return null;
		if (Integer.TYPE.getName().equals(type)) return Integer.TYPE;
		else if (Character.TYPE.getName().equals(type)) return Character.TYPE;
		else if (Byte.TYPE.getName().equals(type)) return Byte.TYPE;
		else if (Short.TYPE.getName().equals(type)) return Short.TYPE;
		else if (Long.TYPE.getName().equals(type)) return Long.TYPE;
		else if (Float.TYPE.getName().equals(type)) return Float.TYPE;
		else if (Double.TYPE.getName().equals(type)) return Double.TYPE;
		else if (Void.TYPE.getName().equals(type)) return Void.TYPE;
		else if (Boolean.TYPE.getName().equals(type)) return Boolean.TYPE;
		else
		{
			// special case for "function"
			if (type.toLowerCase().trim().equals("function")) return Function.class; //$NON-NLS-1$
			else
			{
				Class< ? > c = Class.forName(type);
				return c;
			}
		}
	}

}
