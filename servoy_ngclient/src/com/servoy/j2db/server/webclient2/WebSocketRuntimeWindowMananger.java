/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.webclient2;

import java.util.List;

import org.json.JSONObject;

import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class WebSocketRuntimeWindowMananger extends RuntimeWindowManager implements IService
{

	/**
	 * @param application
	 */
	public WebSocketRuntimeWindowMananger(IWebSocketApplication application)
	{
		super(application);
		application.registerService("dialogService", this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.webclient2.IService#executeMethod(java.lang.String, java.util.Map)
	 */
	@Override
	public Object executeMethod(String methodName, JSONObject args)
	{
		switch (methodName)
		{
			case "windowClosing" :
			{
				WebSocketRuntimeWindow window = getWindow(args.optString("window"));
				if (window != null)
				{
					return Boolean.valueOf(window.hide());
				}
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.RuntimeWindowManager#getMainApplicationWindow()
	 */
	@Override
	protected RuntimeWindow getMainApplicationWindow()
	{
		return new WebSocketRuntimeWindow((IWebSocketApplication)application, null, JSWindow.WINDOW, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.RuntimeWindowManager#getWindow(java.lang.String)
	 */
	@Override
	public WebSocketRuntimeWindow getWindow(String windowName)
	{
		return (WebSocketRuntimeWindow)super.getWindow(windowName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.RuntimeWindowManager#getCurrentWindow()
	 */
	@Override
	public WebSocketRuntimeWindow getCurrentWindow()
	{
		return (WebSocketRuntimeWindow)super.getCurrentWindow();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.RuntimeWindowManager#createWindowInternal(java.lang.String, int, com.servoy.j2db.scripting.RuntimeWindow)
	 */
	@Override
	protected RuntimeWindow createWindowInternal(String windowName, int type, RuntimeWindow parent)
	{
		return new WebSocketRuntimeWindow((IWebSocketApplication)application, windowName, type, parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.RuntimeWindowManager#getOrderedContainers()
	 */
	@Override
	protected List<String> getOrderedContainers()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 
	 */
	public String createMainWindow()
	{
		String windowsUUID = UUID.randomUUID().toString();
		createWindow(windowsUUID, JSWindow.WINDOW, null);
		setCurrentWindowName(windowsUUID);
		return windowsUUID;
	}

}
