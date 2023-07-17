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

import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.util.IDelegate;

/**
 * Scriptable for recording access to delegate scriptable. Used to determine dependencies for calculations.
 *
 * @author rgansevles
 *
 */
public class RecordingScriptable extends AbstractRecordingScriptable implements Wrapper
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
		if (scriptable instanceof NativeDate || scriptable instanceof RecordingScriptable) return scriptable;
		return new RecordingScriptable(scriptableName, scriptable);
	}

}
