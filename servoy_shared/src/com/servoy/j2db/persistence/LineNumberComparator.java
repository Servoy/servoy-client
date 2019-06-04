/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import java.util.Comparator;

/**
 * @author acostache
 *
 */
public class LineNumberComparator implements Comparator<Object>
{
	public static final Comparator<Object> INSTANCE = new LineNumberComparator();

	private LineNumberComparator()
	{
	}

	public int compare(Object o1, Object o2)
	{
		if (o1 instanceof ScriptVariable && o2 instanceof ScriptVariable)
		{
			ScriptVariable sv1 = (ScriptVariable)o1;
			ScriptVariable sv2 = (ScriptVariable)o2;
			if (!sv1.getParent().equals(sv2.getParent()) && sv1.getParent() instanceof Form)
			{
				Form sv1Form = (Form)sv1.getParent();
				Form sv2Form = (Form)sv2.getParent();
				Form extendsForm = sv1Form.getExtendsForm();
				while (extendsForm != null)
				{
					if (extendsForm.equals(sv2Form))
					{
						return 1;
					}
					extendsForm = extendsForm.getExtendsForm();
				}
				return -1;
			}
			if (sv1.getLineNumberOffset() > sv2.getLineNumberOffset()) return 1;
			else if (sv1.getLineNumberOffset() < sv2.getLineNumberOffset()) return -1;
			else if (sv1.equals(sv2))
			{
				return 0;
			}
			else return sv1.getUUID().compareTo(sv2.getUUID());

		}
		else if (o1 instanceof ScriptVariable && !(o2 instanceof ScriptVariable))
		{
			return 1;
		}
		else if (!(o1 instanceof ScriptVariable) && o2 instanceof ScriptVariable)
		{
			return -1;
		}
		else if (o1 == null && o2 == null)
		{
			return 0;
		}
		else if (o1 == null)
		{
			return -1;
		}
		else
		{
			return 1; // o2 == null
		}
	}
}
