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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.IBasicFormManager.History;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.plugins.IPlugin;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.util.Utils;

/**
 * An abstract manager of JSWindows. Creates windows, keeps track of current window and windows that are in use.
 * It is meant to be extended for each type of user interface.
 * @author acostescu
 * @since 6.0
 */
public abstract class RuntimeWindowManager
{
	protected static final Logger log = LoggerFactory.getLogger("RuntimeWindowManager");
	protected static final String MAIN_APPLICATION_WINDOW_NAME = "mainApplicationWindow";

	protected final HashMap<String, RuntimeWindow> windows = new HashMap<String, RuntimeWindow>();
	private String currentWindowName = null; // this should be null when the main application window is the current window
	protected IApplication application;

	public RuntimeWindowManager(IApplication application)
	{
		this.application = application;
		windows.put(MAIN_APPLICATION_WINDOW_NAME, getMainApplicationWindow());
	}

	protected abstract RuntimeWindow getMainApplicationWindow();

	protected abstract RuntimeWindow createWindowInternal(String windowName, int type, RuntimeWindow parent);

	/**
	 * @param string the name of the window. If this is null, returns a wrapper for the main application frame.
	 * @return the user window with the specified name or a wrapper for the main application frame. if name is null.
	 */
	public RuntimeWindow getWindow(String windowName)
	{
		if (windowName == null) windowName = MAIN_APPLICATION_WINDOW_NAME;
		return windows.get(windowName);
	}

	/**
	 * Create a new user window. If a window with this name already exists, it will be closed/destroyed first.
	 * @param windowName the name of the window. Must not be null.
	 * @param parent
	 * @param i
	 * @return the newly created window.
	 */
	public RuntimeWindow createWindow(String windowName, int type, JSWindow parent)
	{
		RuntimeWindow win = windows.get(windowName);
		if (win != null)
		{
			win.destroy();
		}

//		RuntimeWindow currentWindow = null;
//		if (type == JSWindow.DIALOG || type == JSWindow.MODAL_DIALOG)
//		{
//			RuntimeWindow pw = application.getRuntimeWindowManager().getCurrentWindow();
//			if (pw != null)
//			{
//				currentWindow = pw.getJSWindow().getImpl();
//			}
//		}

		win = createWindowInternal(windowName, type, parent != null ? parent.getImpl() : null); // was : currentWindow, but why use fixed parent if it was not specified in JS? better decide each time the window/dialog is shown, so null should be better
		if (win != null) windows.put(windowName, win);
		return win;
	}

	public void removeWindow(String windowName)
	{
		windows.remove(windowName);
	}

	/**
	 * Get the name of the current user window/dialog (null for the main window).
	 */
	public String getCurrentWindowName()
	{
		return currentWindowName;
	}

	/**
	 * Set the name of the current user window/dialog (null for the main window).
	 */
	public void setCurrentWindowName(String currentWindowName)
	{
		String oldName = this.currentWindowName;
		this.currentWindowName = currentWindowName;
		J2DBGlobals.firePropertyChange(application, IPlugin.PROPERTY_CURRENT_WINDOW, oldName, currentWindowName);
	}

	public RuntimeWindow getCurrentWindow()
	{
		RuntimeWindow window = getWindow(currentWindowName);
		if (window == null)
		{
			if (log.isInfoEnabled()) log.info("No current window for '" + currentWindowName + "' creating one if name is not null");
			if (currentWindowName != null)
			{
				// the current window shouldn't be null, this could be a new tab in the web, create it
				window = createWindowInternal(currentWindowName, JSWindow.WINDOW, null);
				windows.put(currentWindowName, window);
			}
		}
		return window;
	}

	/**
	 * Returns a list of window names that represents the order in which these windows should be closed so as to avoid unexpected results.<br>
	 * When windows sit on top of other windows, the top-most windows should probably be closed first. (for example if you have many modal windows on top of each-other and
	 * you need to close all the windows, you might want to keep closing the top-most window until all are closed)
	 */
	protected abstract List<String> getOrderedContainers();

	protected boolean doCloseFormInWindow(IBasicMainContainer container)
	{
		boolean ok = true;
		IFormController fp = container.getController();
		if (fp != null)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			ok = fp.notifyVisible(false, invokeLaterRunnables, true);
			Utils.invokeLater(application, invokeLaterRunnables);
			IApplication app = fp.getApplication();
			if (app.getFormManager() != null)
			{
				String form_uuid = fp.getForm().getNavigatorID();
				if (form_uuid != null && !Form.NAVIGATOR_IGNORE.equals(form_uuid) && !Form.NAVIGATOR_NONE.equals(form_uuid))
				{
					IFormController navigator = (IFormController)app.getFormManager().getForm(app.getFlattenedSolution().getForm(form_uuid).getName())
						.getFormUI()
						.getController();
					List<Runnable> invokeLaterRunnables2 = new ArrayList<Runnable>();
					if (navigator != null) navigator.notifyVisible(false, invokeLaterRunnables2, true);
				}
			}
		}
		if (ok)
		{
			hideContainer(container);
		}
		return ok;
	}

	/**
	 * @param container
	 */
	protected void hideContainer(IBasicMainContainer container)
	{
		RuntimeWindow w = getWindow(container.getContainerName());
		if (w != null) w.hideUI();
	}

	public boolean closeFormInWindow(String windowName, boolean closeAll)
	{
		IBasicFormManager fm = application.getFormManager();
		IBasicMainContainer container = null;

		if ((windowName == null || windowName.replace(" ", "").length() == 0) && closeAll == true) //$NON-NLS-1$ //$NON-NLS-2$
		{
			boolean result = true;

			List<String> oc = getOrderedContainers();
			boolean ret = true;
			for (String key : oc)
			{
				if (key != null)
				{
					IBasicMainContainer mContainer = fm.getMainContainer(key);
					if (fm.getMainContainer(null) != mContainer && mContainer != null)
					{
						ret = doCloseFormInWindow(mContainer);
						if (ret)
						{
							// If dialog is closed then we must clear the complete history
							History hist = fm.getHistory(mContainer);
							hist.clear();
						}
					}

					result = result && ret;
				}
			}

			return result;

		}
		else if (windowName == null || windowName.replace(" ", "").length() == 0) //$NON-NLS-1$ //$NON-NLS-2$
		{
			container = fm.getCurrentContainer();
		}
		else
		{
			container = fm.getMainContainer(windowName);
		}

		if (container != null)
		{
			//don't do anything if it is the main container
			if (container == fm.getMainContainer(null)) return false;

			if (!closeAll)
			{
				// legacy V3 behavior, when multiple forms are shown in same form, go back 1 in history
				History hist = fm.getHistory(container);
				if (hist.getLength() > 1 && hist.getIndex() > 0)
				{
					storeWindowBounds(container);
					boolean b = hist.removeIndex(hist.getIndex());
					if (b)
					{
						restoreWindowBounds(container);
					}
					return b;
				}
			}

			boolean ret = doCloseFormInWindow(container);
			if (ret)
			{
				// If dialog is closed then we must clear the complete history
				History hist = fm.getHistory(container);
				hist.clear();
			}
			return ret;
		}
		return true;
	}

	protected boolean restoreWindowBounds(IBasicMainContainer container)
	{
		// dummy; subclasses can override if needed
		return false;
	}

	protected void storeWindowBounds(IBasicMainContainer container)
	{
		// dummy; subclasses can override if needed
	}

	public Object getCurrentWindowWrappedObject()
	{
		RuntimeWindow w = getCurrentWindow();
		return w != null ? w.getWrappedObject() : null;
	}

	public Object getWindowWrappedObject(String windowName)
	{
		RuntimeWindow w = getWindow(windowName);
		return w != null ? w.getWrappedObject() : null;
	}
}
