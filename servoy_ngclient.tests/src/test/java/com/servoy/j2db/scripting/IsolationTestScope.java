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

package com.servoy.j2db.scripting;

import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * Minimal {@link DefaultJavaScope} used by the SVY-21334 tests: one `js_` function, an overloaded `@JSFunction` and a read-only property.
 * It is public (and lives in its own file) so that Rhino can reflectively invoke its methods.
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
public class IsolationTestScope extends DefaultJavaScope
{
	public static final Map<String, JsMethod> FUNCTIONS = DefaultJavaScope.getJsFunctions(IsolationTestScope.class);

	private final String id;

	public IsolationTestScope(String id, Scriptable parent)
	{
		super(parent, FUNCTIONS);
		this.id = id;
	}

	@Override
	public String getClassName()
	{
		return "IsolationTestScope";
	}

	public String js_hello()
	{
		return "hello from " + id;
	}

	@JSFunction
	public String describe(String value)
	{
		return "string:" + value;
	}

	@JSFunction
	public String describe(int value)
	{
		return "int:" + value;
	}

	@JSFunction
	public String describe(String first, String second)
	{
		return "pair:" + first + "," + second;
	}

	@JSReadonlyProperty(property = "ownerId")
	public String getOwnerId()
	{
		return id;
	}
}
