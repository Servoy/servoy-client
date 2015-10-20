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
package com.servoy.j2db.smart;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicMainContainer;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.gui.FormDialog;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;

/**
 * Swing implementation of the JSWindowManager. It works with SwingJSWindowImpl windows.
 * @author acostescu
 * @since 6.0
 */
public class SwingRuntimeWindowManager extends RuntimeWindowManager
{

	public SwingRuntimeWindowManager(IApplication application)
	{
		super(application);
	}

	@Override
	protected RuntimeWindow createWindowInternal(String windowName, int type, RuntimeWindow parent)
	{
		return new SwingRuntimeWindow((ISmartClientApplication)application, windowName, type, parent);
	}

	@Override
	protected RuntimeWindow getMainApplicationWindow()
	{
		return new MainApplicationSwingJSWindow((ISmartClientApplication)application);
	}

	@Override
	protected List<String> getOrderedContainers()
	{
		// here we have to close in reverse order of the opening
		FormManager fm = ((FormManager)application.getFormManager());
		List<String> orderedDialogs = new ArrayList<String>();
		Map<FormDialog, String> dialogs = new HashMap<FormDialog, String>();
		List<String> all = fm.getCreatedMainContainerKeys();
		for (String key : all)
		{
			if (key != null)
			{
				IMainContainer mContainer = fm.getMainContainer(key);
				if (fm.getMainContainer(null) != mContainer)
				{
					Container parent = ((Component)mContainer).getParent();
					while (parent != null && !(parent instanceof FormDialog))
					{
						parent = parent.getParent();
					}
					if (parent instanceof FormDialog)
					{
						dialogs.put((FormDialog)parent, key);
						continue;
					}
				}
			}
		}
		for (FormDialog dialog : dialogs.keySet())
		{
			addDialogsInOrder(dialog, dialogs, orderedDialogs);
		}
		if (orderedDialogs.size() < all.size())
		{
			for (String key : all)
			{
				if (!orderedDialogs.contains(key))
				{
					orderedDialogs.add(key);
				}
			}
		}
		return orderedDialogs;
	}

	private void addDialogsInOrder(FormDialog dialog, Map<FormDialog, String> dialogs, List<String> orderedDialogs)
	{
		if (dialog.getOwnedWindows() != null)
		{
			for (Window window : dialog.getOwnedWindows())
			{
				if (window instanceof FormDialog)
				{
					addDialogsInOrder((FormDialog)window, dialogs, orderedDialogs);
				}
			}
		}
		if (!orderedDialogs.contains(dialogs.get(dialog)))
		{
			orderedDialogs.add(dialogs.get(dialog));
		}
	}

	@Override
	public boolean doCloseFormInWindow(IBasicMainContainer container)
	{
		((SwingFormManager)application.getFormManager()).removePreview((IMainContainer)container);
		return super.doCloseFormInWindow(container);
	}

	@Override
	protected void storeWindowBounds(IBasicMainContainer container)
	{
		SwingRuntimeWindow w = (SwingRuntimeWindow)getWindow(container.getContainerName());
		if (w != null)
		{
			w.storeBounds();
		}
	}

	@Override
	protected boolean restoreWindowBounds(IBasicMainContainer container)
	{
		SwingRuntimeWindow w = (SwingRuntimeWindow)getWindow(container.getContainerName());
		return w != null ? w.restoreWindowBounds() : false;
	}

	private static class MainApplicationSwingJSWindow extends SwingRuntimeWindow
	{

		public MainApplicationSwingJSWindow(ISmartClientApplication application)
		{
			super(application, null, JSWindow.WINDOW, null);
			wrappedWindow = application.getMainApplicationFrame();
		}

		@Override
		protected void doShow(String formName)
		{
			if (formName != null)
			{
				((FormManager)getApplication().getFormManager()).showFormInMainPanel(formName);
			}
		}

		@Override
		public TextToolbar getTextToolbar()
		{
			if (textToolbar == null)
			{
				textToolbar = (TextToolbar)getApplication().getToolbarPanel().getToolBar("text"); //$NON-NLS-1$
			}
			return super.getTextToolbar();
		}

		@Override
		public void showTextToolbar(boolean value)
		{
			// does not affect main app. window
		}

		@Override
		public void destroy()
		{
			// you cannot destroy main app. window
		}

		@Override
		public void hideUI()
		{
			// should never get called, but to be on the safe side
		}

		@Override
		protected void doOldShow(String formName, boolean closeAll, boolean legacyV3Behavior)
		{
			// should never get called, but to be on the safe side
		}

	}

}