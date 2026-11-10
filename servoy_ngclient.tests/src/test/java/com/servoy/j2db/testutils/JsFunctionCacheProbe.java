/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.j2db.testutils;

import java.lang.reflect.Field;

/**
 * Test helper (SVY-21334): reads the (private, lazily created) <code>jsFunctionCache</code> field that js function owners keep.
 * It looks through the class hierarchy, so it works for {@link com.servoy.j2db.scripting.DefaultJavaScope} subclasses as well as for
 * the hand written owners like foundset, record or dataset.
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
public final class JsFunctionCacheProbe
{
	private static final String CACHE_FIELD = "jsFunctionCache";

	private JsFunctionCacheProbe()
	{
	}

	/**
	 * @return the current value of the owner's function cache field, <code>null</code> if no function was accessed yet.
	 */
	public static Object readCache(Object owner) throws Exception
	{
		return findField(owner.getClass()).get(owner);
	}

	/**
	 * @return the accessible function cache field of the given class or of its nearest super class that declares it.
	 */
	public static Field findField(Class< ? > clazz) throws NoSuchFieldException
	{
		for (Class< ? > c = clazz; c != null; c = c.getSuperclass())
		{
			try
			{
				Field field = c.getDeclaredField(CACHE_FIELD);
				field.setAccessible(true);
				return field;
			}
			catch (NoSuchFieldException e)
			{
				// try super class
			}
		}
		throw new NoSuchFieldException(CACHE_FIELD + " in " + clazz);
	}
}
