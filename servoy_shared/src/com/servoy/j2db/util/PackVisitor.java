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
package com.servoy.j2db.util;

import java.util.HashMap;
import java.util.Map;

/** Visitor class to for replacing duplicate objects with the same instance in a IVisitable object.
 * @see IVisitable
 * @author rob
 *
 */
public class PackVisitor implements IVisitor
{
	Map map = new HashMap();

	public Object visit(Object o)
	{
		Object o2 = map.get(o);
		if (o2 == null)
		{
			map.put(o, o);
			o2 = o;
		}
		return o2;
	}

}
