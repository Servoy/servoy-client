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
package com.servoy.j2db.debug;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dltk.rhino.dbgp.DBGPDebugFrame;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.debug.DebuggableScript;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;

/**
 * @author jcompagner
 *
 */
public class ServoyDebugFrame extends DBGPDebugFrame implements IDataCallListener
{
	private long startTime;
	private long endTime;
	private Object[] args;
	private String parentSource;
	private final DebuggableScript node;
	private final ServoyDebugger debugger;
	private final ServoyDebugFrame parent;
	private final List<DataCallProfileData> dataCallProfileDatas = new ArrayList<DataCallProfileData>();
	final IServiceProvider client;
	private final Context ct;
	private ServoyDebugFrame servoyDebugSubFrame;
	private String subActionName;

	/**
	 * @param ct
	 * @param node
	 * @param debugger
	 * @param parent
	 */
	public ServoyDebugFrame(Context ct, DebuggableScript node, ServoyDebugger debugger, ServoyDebugFrame parent)
	{
		super(ct, node, debugger);
		this.ct = ct;
		this.node = node;
		this.debugger = debugger;
		this.parent = parent;
		this.client = J2DBGlobals.getServiceProvider();
	}

	public ServoyDebugFrame(Context ct, DebuggableScript node, ServoyDebugger debugger, ServoyDebugFrame parent, String subActionName)
	{
		super(ct, node, debugger);
		this.ct = ct;
		this.node = node;
		this.debugger = debugger;
		this.parent = parent;
		this.subActionName = subActionName;
		this.client = J2DBGlobals.getServiceProvider();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.debug.IDataCallListener#addDataCallProfileData(com.servoy.j2db.debug.DataCallProfileData)
	 */
	public void addDataCallProfileData(DataCallProfileData data)
	{
		dataCallProfileDatas.add(data);
	}

	/**
	 * @see org.eclipse.dltk.rhino.dbgp.DBGPDebugFrame#onEnter(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, org.mozilla.javascript.Scriptable, java.lang.Object[])
	 */
	@Override
	public void onEnter(Context cx, Scriptable activation, Scriptable thisObj, Object[] args)
	{
		this.args = args;
		if (parent != null)
		{
			parentSource = parent.node.getSourceName() + '#' + (parent.getLineNumber());
		}
		super.onEnter(cx, activation, thisObj, args);
		debugger.onenter(this);

		IServiceProvider sp = J2DBGlobals.getServiceProvider();
		if (sp != null && sp.getDataServer() instanceof ProfileDataServer)
		{
			((ProfileDataServer)sp.getDataServer()).addDataCallListener(this);
		}
		startTime = System.currentTimeMillis();
	}

	/**
	 * @see org.eclipse.dltk.rhino.dbgp.DBGPDebugFrame#onExit(org.mozilla.javascript.Context, boolean, java.lang.Object)
	 */
	@Override
	public void onExit(Context cx, boolean byThrow, Object resultOrException)
	{
		endTime = System.currentTimeMillis();
		super.onExit(cx, byThrow, resultOrException);
		debugger.onexit(this);

		IServiceProvider sp = J2DBGlobals.getServiceProvider();
		if (sp != null && sp.getDataServer() instanceof ProfileDataServer)
		{
			((ProfileDataServer)sp.getDataServer()).removeDataCallListener(this);
		}
	}

	public ProfileData getProfileData()
	{
		int[] lineNumbers = null;
		boolean innerFunction = false;
		String name = node.getFunctionName();
		DebuggableScript currentNode = node;
		if (subActionName != null)
		{
			name = subActionName;
		}
		while ((name == null || name.equals("")) && currentNode.getParent() != null)
		{
			name = currentNode.getParent().getFunctionName();
			lineNumbers = currentNode.getLineNumbers();
			currentNode = currentNode.getParent();
			innerFunction = true;
		}

		return new ProfileData(name, (endTime - startTime), args, node.getSourceName(), parentSource, innerFunction, lineNumbers, dataCallProfileDatas);
	}

	@Override
	public Object eval(String value)
	{
		if (client != null && J2DBGlobals.getServiceProvider() == null)
		{
			J2DBGlobals.setServiceProvider(client);
		}
		return super.eval(value);
	}

	/**
	 * @param functionName
	 */
	public void onEnterSubAction(String functionName, Object[] arguments)
	{
		servoyDebugSubFrame = new ServoyDebugFrame(ct, node, debugger, this, functionName);
		servoyDebugSubFrame.onEnter(ct, null, null, arguments);
	}

	/**
	 *
	 */
	public void onExitSubAction()
	{
		if (servoyDebugSubFrame != null) servoyDebugSubFrame.onExit(ct, false, null);
	}

}
