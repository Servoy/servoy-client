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
package com.servoy.j2db;

import org.mozilla.javascript.Function;

import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.FunctionDefinition;
import com.servoy.j2db.scripting.JSEvent;

/**
 * @author jcompagner
 * 
 */
public class DesignModeCallbacks
{
	private final FunctionDefinition ondrag;
	private final FunctionDefinition ondrop;
	private final FunctionDefinition onselect;
	private final FunctionDefinition onresize;
	private final IApplication application;

	/**
	 * @param args
	 */
	public DesignModeCallbacks(Object[] args, IApplication application)
	{
		this.application = application;
		if (args.length > 0 && args[0] instanceof Function)
		{
			ondrag = new FunctionDefinition((Function)args[0]);
		}
		else
		{
			ondrag = null;
		}
		if (args.length > 1 && args[1] instanceof Function)
		{
			ondrop = new FunctionDefinition((Function)args[1]);
		}
		else
		{
			ondrop = null;
		}
		if (args.length > 2 && args[2] instanceof Function)
		{
			onselect = new FunctionDefinition((Function)args[2]);
		}
		else
		{
			onselect = null;
		}
		if (args.length > 3 && args[3] instanceof Function)
		{
			onresize = new FunctionDefinition((Function)args[3]);
		}
		else
		{
			onresize = null;
		}
	}

	public Object executeOnDrag(JSEvent event)
	{
		if (ondrag != null)
		{
			return ondrag.executeSync((IClientPluginAccess)application.getPluginAccess(), new Object[] { event });
		}
		return null;
	}

	public Object executeOnDrop(JSEvent event)
	{
		if (ondrop != null)
		{
			return ondrop.executeSync((IClientPluginAccess)application.getPluginAccess(), new Object[] { event });
		}
		return null;
	}

	public Object executeOnSelect(JSEvent event)
	{
		if (onselect != null)
		{
			return onselect.executeSync((IClientPluginAccess)application.getPluginAccess(), new Object[] { event });
		}
		return null;
	}

	public Object executeOnResize(JSEvent event)
	{
		if (onresize != null)
		{
			return onresize.executeSync((IClientPluginAccess)application.getPluginAccess(), new Object[] { event });
		}
		return null;
	}

}
