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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IEventDispatchAwareServerService;
import org.sablo.websocket.IWebsocketEndpoint;

import com.servoy.j2db.IBasicMainContainer;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class NGRuntimeWindowManager extends RuntimeWindowManager implements IEventDispatchAwareServerService
{
	public static final String WINDOW_SERVICE = "$windowService";

	private String lastCurrentWindow = null;

	private String modalWindow;

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
						NGClientWindow.getCurrentWindow().touchForm(application.getFlattenedSolution().getFlattenedForm(form), formName, true, false);
					}
				}
				break;
			}
			case "resize" :
			{
				String windowName = args.has("name") ? args.optString("name") : null;
				JSONObject size = args.optJSONObject("size");
				if (size != null)
				{
					NGRuntimeWindow window = windowName == null ? (NGRuntimeWindow)getMainApplicationWindow() : getWindow(windowName);
					if (window != null)
					{
						if (window.updateSize(size.optInt("width"), size.optInt("height")) && window.getController() != null)
						{
							window.getController().notifyResized();
						}
					}
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
					if (window != null) window.updateLocation(location.optInt("x"), location.optInt("y"));
				}
				break;
			}
		}
		return null;
	}

	@Override
	protected RuntimeWindow getMainApplicationWindow()
	{
		RuntimeWindow runtimeWindow = this.windows.get(MAIN_APPLICATION_WINDOW_NAME);
		if (runtimeWindow == null)
		{
			int windowNr = CurrentWindow.exists() ? CurrentWindow.get().getNr() : -1;
			if (windowNr != -1)
			{
				String name = String.valueOf(windowNr);
				runtimeWindow = getWindow(name);
				if (runtimeWindow == null)
				{
					runtimeWindow = createWindowInternal(name, JSWindow.WINDOW, null);
					windows.put(name, runtimeWindow);
				}
			}
		}
		return runtimeWindow;
	}

	@Override
	public NGRuntimeWindow getWindow(String windowName)
	{
		return windowName == null ? (NGRuntimeWindow)getMainApplicationWindow() : (NGRuntimeWindow)super.getWindow(windowName);
	}

	@Override
	public NGRuntimeWindow getCurrentWindow()
	{
		// This will return a the last used if no  current window is set
		// so that code that is run through a scheduler or databroadcast will have a current container in scripting
		// getCurrentWindowName will return null so that the system can still know the difference. (NGEvent that resets the current window)
		NGRuntimeWindow currentWindow = (NGRuntimeWindow)super.getCurrentWindow();
		if (currentWindow == null)
		{
			if (log.isInfoEnabled())
				log.info("Couldn't get the current window getting it by the last know window name " + lastCurrentWindow + ", all windows: " + this.windows);
			currentWindow = getWindow(lastCurrentWindow);
			if (currentWindow == null)
			{
				if (log.isInfoEnabled())
					log.info("Last know window name " + lastCurrentWindow + " didn't result in a window returning the main application window");
				currentWindow = (NGRuntimeWindow)getMainApplicationWindow();
			}
		}
		else if (modalWindow != null)
		{
			// if there is a modal window registered look if the current window about is not a parent of this one
			// if it is return the modal window.
			NGRuntimeWindow modal = getWindow(modalWindow);
			if (modal != null)
			{
				RuntimeWindow parent = modal.getRuntimeParent();
				while (parent != null)
				{
					if (parent == currentWindow) return modal;
					parent = parent.getRuntimeParent();
				}
			}
		}
		return currentWindow;
	}

	@Override
	public String getCurrentWindowName()
	{
		// this will return null if there is no current active window set.
		// getCurrentWindow above does behave a bit different it will always return the last used window
		// if there is no current window set, this way Scheduler jobs or other runnables that can run (on data broadcast)
		// will just use the last used window as the current one
		return super.getCurrentWindowName();
	}

	@Override
	public void setCurrentWindowName(String currentWindowName)
	{
		if (currentWindowName != null) lastCurrentWindow = currentWindowName;
		super.setCurrentWindowName(currentWindowName);
	}

	@Override
	protected RuntimeWindow createWindowInternal(String windowName, int type, RuntimeWindow parent)
	{
		((INGApplication)application).getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("create",
			new Object[] { windowName, Integer.valueOf(type) });
		if (parent == null && CurrentWindow.exists())
		{
			IBasicMainContainer currentContainer = null;
			// try to always just set the current parent if it is null
			// this way we can later on use this to set the window back.
			// first check if the current window name is set (else this could be a the startup)
			if (this.getCurrentWindowName() != null)
			{
				currentContainer = getWindow(this.getCurrentWindowName());
			}
			if (currentContainer instanceof RuntimeWindow rw)
			{
				parent = rw;
			}
			else
			{
				parent = getWindow(String.valueOf(CurrentWindow.get().getNr()));
			}
		}
		return new NGRuntimeWindow((INGApplication)application, windowName, type, parent);
	}

	@Override
	protected List<String> getOrderedContainers()
	{
		// return them unordered for now, we do not have the order info in ngclient yet
		return new ArrayList<String>(windows.keySet());
	}

	public void createMainWindow(int windowNr)
	{
		String windowName = String.valueOf(windowNr);
		if (getWindow(windowName) == null)
		{
			RuntimeWindow mainApplicationWindow = createWindow(windowName, JSWindow.WINDOW, null);
			mainApplicationWindow.setLocation(0, 0); //default values, that never change
			setCurrentWindowName(windowName);
		}
	}

	@Override
	public boolean closeFormInWindow(String windowName, boolean closeAll)
	{
		NGRuntimeWindow window = null;
		if (windowName != null && (window = getWindow(windowName)) != null)
		{
			for (int i = window.children.size(); --i >= 0;)
			{
				NGRuntimeWindow childWindow = window.children.get(i);
				if (!closeFormInWindow(childWindow.getName(), closeAll))
				{
					return false;
				}
			}
		}
		return super.closeFormInWindow(windowName, closeAll);
	}

	public void destroy(boolean keepMainApplicationWindow)
	{
		List<Entry<String, RuntimeWindow>> copy = new ArrayList<>(windows.entrySet());
		for (Entry<String, RuntimeWindow> we : copy)
		{
			RuntimeWindow rw = we.getValue();
			if (rw != null)
			{
				if (rw instanceof NGRuntimeWindow)
				{
					((NGRuntimeWindow)rw).setController(null);
				}
				if (keepMainApplicationWindow && rw.equals(getMainApplicationWindow()))
				{
					continue;
				}
				if (rw instanceof NGRuntimeWindow)
				{
					rw.destroy();
				}
			}
			windows.remove(we.getKey());
		}
	}

	@Override
	public int getMethodEventThreadLevel(String methodName, JSONObject arguments, int dontCareLevel)
	{
		// sync api calls to client might need to touch forms... allow that even if event thread is currently blocked on that API call
		// but only if that form is not yet loaded yet or it is not in a changing state (DAL.setRecord())
		if ("touchForm".equals(methodName))
		{
			String formName = arguments.optString("name");
			IWebFormController cachedFormController = ((INGApplication)application).getFormManager().getCachedFormController(formName);
			if (cachedFormController == null || !cachedFormController.getFormUI().isChanging())
			{
				return IWebsocketEndpoint.EVENT_LEVEL_SYNC_API_CALL;
			}
		}
		return dontCareLevel;
	}

	/**
	 * @param name
	 * @return
	 */
	public String setModalWindowName(String modalWindow)
	{
		String prev = this.modalWindow;
		this.modalWindow = modalWindow;
		return prev;
	}

}
