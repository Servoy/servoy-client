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

import static com.servoy.j2db.util.Utils.arrayMap;

import java.util.Collections;
import java.util.Iterator;

import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.UUID;

/**
 * Scriptable for recording access to delegate scriptable. Used to determine dependencies for calculations.
 *
 * @author rgansevles
 *
 */
public class RecordingScriptable extends AbstractRecordingScriptable implements Wrapper, Iterable
{
	public RecordingScriptable(Scriptable scriptable)
	{
		this(null, scriptable);
	}

	RecordingScriptable(String scriptableName, Scriptable scriptable)
	{
		super(scriptableName, scriptable);
	}


	@Override
	public Object unwrap()
	{
		// the scriptable is the tablescope for the "this" in a calculation then it should unwrap to the Record (whichs is the prototype)
		// a related record is also wrapped in a RecordingScriptable but in that case the scriptable is directly the Record
		if (scriptable instanceof TableScope)
		{
			return scriptable.getPrototype();
		}
		return scriptable instanceof Wrapper ? ((Wrapper)scriptable).unwrap() : scriptable;
	}

	public Iterator iterator()
	{
		if (scriptable instanceof Iterable)
		{
			Iterator scriptingIterator = ((Iterable)scriptable).iterator();
			return new Iterator()
			{
				@Override
				public boolean hasNext()
				{
					return scriptingIterator.hasNext();
				}

				@Override
				public Object next()
				{
					Object next = scriptingIterator.next();
					if (next instanceof Scriptable)
					{
						next = wrapScriptableIfNeeded(null, (Scriptable)next);
					}
					return next;
				}

			};
		}
		return Collections.emptyIterator();
	}

	static Object unwrapScriptable(Object obj)
	{
		if (obj instanceof IDelegate< ? >)
		{
			Object delegate = ((IDelegate< ? >)obj).getDelegate();
			if (delegate instanceof Scriptable)
			{
				return delegate;
			}
		}
		return obj;
	}

	static Object[] unwrapScriptable(Object[] array)
	{
		return arrayMap(array, RecordingScriptable::unwrapScriptable);
	}

	static Scriptable wrapScriptableIfNeeded(String scriptableName, Scriptable scriptable)
	{
		if (scriptable == null) return null;
		if (scriptable instanceof NativeDate || scriptable instanceof RecordingScriptable ||
			(scriptable instanceof NativeJavaObject njo && njo.unwrap() instanceof UUID)) return scriptable;
		return new RecordingScriptable(scriptableName, scriptable);
	}

}
