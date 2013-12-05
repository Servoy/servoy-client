/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;

/**
 * @author jcompagner
 *
 */
final class ServoyImporterTopLevel extends ImporterTopLevel
{
	private final Object lock = new Object();

	/**
	 * @param cx
	 */
	ServoyImporterTopLevel(Context cx)
	{
		super(cx);
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		synchronized (lock)
		{
			return super.get(index, start);
		}
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		synchronized (lock)
		{
			return super.get(name, start);
		}
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{
		synchronized (lock)
		{
			super.put(index, start, value);
		}
	}

	@Override
	public void put(String name, Scriptable start, Object value)
	{
		synchronized (lock)
		{
			super.put(name, start, value);
		}
	}
}