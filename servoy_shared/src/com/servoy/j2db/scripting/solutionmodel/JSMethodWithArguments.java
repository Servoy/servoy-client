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

import java.util.Arrays;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.ScriptMethod;


public class JSMethodWithArguments extends JSMethod
{
	protected final Object[] arguments; // used when attached to an event

	/**
	 * JSMethod with arguments.
	 */
	public JSMethodWithArguments(JSMethod jsMethod, Object[] args)
	{
		super(jsMethod.application, jsMethod.form, jsMethod.sm, jsMethod.isCopy);
		this.arguments = args;
	}

	public JSMethodWithArguments(IApplication application, JSForm form, ScriptMethod sm, boolean isNew, Object[] args)
	{
		super(application, form, sm, isNew);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sm == null) ? 0 : sm.hashCode());
		result = prime * result + Arrays.hashCode(arguments);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSMethodWithArguments other = (JSMethodWithArguments)obj;
		if (!sm.getUUID().equals(other.sm.getUUID())) return false;
		else if (!Arrays.equals(arguments, other.arguments)) return false;
		return true;
	}
}
