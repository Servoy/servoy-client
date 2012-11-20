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

package com.servoy.j2db.scripting;

import java.util.ArrayList;
import java.util.Collection;


/**
 * HashMap extension to implement JSConvertedList.
 * This instructs the Servoy engine how to convert data to scriptable objects before returning it to the scripting engine.
 * 
 * @author rgansevles
 *
 */
public class JSList<E> extends ArrayList<E> implements JSConvertedList<E>
{
	public JSList()
	{
	}

	public JSList(int initialCapacity)
	{
		super(initialCapacity);
	}

	public JSList(Collection< ? extends E> c)
	{
		super(c);
	}

}
