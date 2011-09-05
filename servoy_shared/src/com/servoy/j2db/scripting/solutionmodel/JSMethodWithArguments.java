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

/** JSMethod with arguments for assignment to a event.
 * @author rgansevles
 */
package com.servoy.j2db.scripting.solutionmodel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.ScriptMethod;

/**
 * JSMethod extended with arguments to be used in event handler calls.
 * 
 * @author rgansevles
 *
 */

public class JSMethodWithArguments extends JSMethod
{
	protected final Object[] arguments; // used when attached to an event

	/**
	 * JSMethod with arguments.
	 */
	public JSMethodWithArguments(JSMethod jsMethod, Object[] args)
	{
		super(jsMethod.parent, jsMethod.sm, jsMethod.application, jsMethod.isCopy);
		this.arguments = args;
	}

	public JSMethodWithArguments(IApplication application, IJSScriptParent< ? > parent, ScriptMethod sm, boolean isNew, Object[] args)
	{
		super(parent, sm, application, isNew);
		this.arguments = args;
	}

	public Object[] getArguments()
	{
		return arguments;
	}

	@Override
	public Object[] js_getArguments()
	{
		return getArguments();
	}
}
