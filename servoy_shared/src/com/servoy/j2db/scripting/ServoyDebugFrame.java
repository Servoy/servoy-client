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

import org.eclipse.dltk.rhino.dbgp.DBGPDebugFrame;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.debug.DebuggableScript;

/**
 * @author jcompagner
 *
 */
public class ServoyDebugFrame extends DBGPDebugFrame
{
	private long startTime;
	private long endTime;
	private Object[] args;
	private String parentSource;
	private final DebuggableScript node;
	private final ServoyDebugger debugger;
	private final ServoyDebugFrame parent;

	/**
	 * @param ct
	 * @param node
	 * @param debugger
	 * @param parent 
	 */
	public ServoyDebugFrame(Context ct, DebuggableScript node, ServoyDebugger debugger, ServoyDebugFrame parent)
	{
		super(ct, node, debugger);
		this.node = node;
		this.debugger = debugger;
		this.parent = parent;
	}

	/**
	 * @see org.eclipse.dltk.rhino.dbgp.DBGPDebugFrame#onEnter(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, org.mozilla.javascript.Scriptable, java.lang.Object[])
	 */
	@Override
	public void onEnter(Context cx, Scriptable activation, Scriptable thisObj, Object[] args)
	{
		startTime = System.currentTimeMillis();
		this.args = args;
		if (parent != null)
		{
			parentSource = parent.node.getSourceName() + '#' + (parent.getLineNumber());
		}
		super.onEnter(cx, activation, thisObj, args);
	}

	/**
	 * @see org.eclipse.dltk.rhino.dbgp.DBGPDebugFrame#onExit(org.mozilla.javascript.Context, boolean, java.lang.Object)
	 */
	@Override
	public void onExit(Context cx, boolean byThrow, Object resultOrException)
	{
		super.onExit(cx, byThrow, resultOrException);

		endTime = System.currentTimeMillis();
		debugger.onexit(this);
	}

	public ProfileData getProfileData()
	{
		return new ProfileData(node.getFunctionName(), (endTime - startTime), args, node.getSourceName(), parentSource);
	}
}
