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

package com.servoy.j2db.server.ngclient;

import java.util.List;

import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class NGRuntimeWindowManager extends RuntimeWindowManager implements IServerService
{
	public static final String WINDOW_SERVICE = "$windowService";


	/**
	 * @param application
	 */
	public NGRuntimeWindowManager(INGApplication application)
	{
		super(application);
		application.getWebsocketSession().registerServerService(WINDOW_SERVICE, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.IService#executeMethod(java.lang.String, java.util.Map)
	 */
	@Override
	public Object executeMethod(String methodName, JSONObject args)
	{
		switch (methodName)
		{
			case "windowClosing" :
			{
				NGRuntimeWindow window = getWindow(args.optString("window"));
				if (window != null)
				{
					return Boolean.valueOf(window.hide());
				}
				break;
			}
			case "touchForm" :
			{
				String formName = args.optString("name");
				if (formName != null)
				{
					Form form = application.getFormManager().getPossibleForm(formName);
					if (form == null)
					{
						form = application.getFlattenedSolution().getForm(formName);
						if (form != null)
						{
							// new form, add it in form manager
							application.getFormManager().addForm(form, false);
						}
					}
					if (form != null)
					{
						((INGApplication)application).getWebsocketSession().touchForm(application.getFlattenedSolution().getFlattenedForm(form), formName, true);
					}
				}
				break;
			}
			case "resize" :
			{
				String windowName = args.optString("name");
				JSONObject size = args.optJSONObject("size");
				if (windowName != null && size != null)
				{
					NGRuntimeWindow window = getWindow(windowName);
					window.updateSize(size.optInt("width"), size.optInt("height"));
				}
				break;
			}
			case "move" :
			{
				String windowName = args.optString("name");
				JSONObject location = args.optJSONObject("location");
				if (windowName != null && location != null)
				{
					NGRuntimeWindow window = getWindow(windowName);
					window.updateLocation(location.optInt("x"), location.optInt("y"));
				}
				break;
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
		return new NGRuntimeWindow((INGApplication)application, null, JSWindow.WINDOW, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.RuntimeWindowManager#getWindow(java.lang.String)
	 */
	@Override
	public NGRuntimeWindow getWindow(String windowName)
	{
		return (NGRuntimeWindow)super.getWindow(windowName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.RuntimeWindowManager#getCurrentWindow()
	 */
	@Override
	public NGRuntimeWindow getCurrentWindow()
	{
		return (NGRuntimeWindow)super.getCurrentWindow();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.RuntimeWindowManager#createWindowInternal(java.lang.String, int, com.servoy.j2db.scripting.RuntimeWindow)
	 */
	@Override
	protected RuntimeWindow createWindowInternal(String windowName, int type, RuntimeWindow parent)
	{
		((INGApplication)application).getWebsocketSession().getService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("create",
			new Object[] { windowName, String.valueOf(type) });
		return new NGRuntimeWindow((INGApplication)application, windowName, type, parent);
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
