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
package com.servoy.j2db.scripting;

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Tagging interface that plugin can return as a map that will then be converted to a NativeObject in javascript.
 * 
 * They keys of this map can only be Integers or Strings, an Integer will used as the index on {@link Scriptable#put(int, Scriptable, Object)}
 * and the String will be used as the name in the call to {@link Scriptable#put(String, Scriptable, Object)}
 * 
 * @author jcompagner
 *
 */
public interface JSConvertedMap<K, V> extends Map<K, V>
{
	/**
	 * returns the constructor name like "Array" that should be created. 
	 * Used in the call to {@link Context#newObject(Scriptable, String)}
	 * if it returns null then it defaults to 'Object' with a call to {@link Context#newObject(Scriptable)}
	 * 
	 * @return String the constructor name, null if default NativeObject should be created.
	 */
	public String getConstructorName();
}
