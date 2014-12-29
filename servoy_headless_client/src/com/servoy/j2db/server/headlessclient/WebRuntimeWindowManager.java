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
package com.servoy.j2db.server.headlessclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IWebClientApplication;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.server.headlessclient.PageJSActionBuffer.PageAction;
import com.servoy.j2db.util.Pair;

/**
 * Swing implementation of the JSWindowManager. It works with WebJSWindowImpl windows.
 * @author acostescu
 * @since 6.0
 */
public class WebRuntimeWindowManager extends RuntimeWindowManager
{
	private final Stack<Pair<PageJSActionBuffer, Integer>> closeDialogStack = new Stack<Pair<PageJSActionBuffer, Integer>>();

	public WebRuntimeWindowManager(IApplication application)
	{
		super(application);
	}

	@Override
	protected RuntimeWindow createWindowInternal(String windowName, int type, RuntimeWindow parent)
	{
		return new WebRuntimeWindow((IWebClientApplication)application, windowName, type, parent);
	}

	@Override
	protected RuntimeWindow getMainApplicationWindow()
	{
		return new MainApplicationWebJSFrame(application);
	}

	@Override
	protected boolean doCloseFormInWindow(IMainContainer container)
	{
		// get the current action buffer of the container (or a parent)
		PageJSActionBuffer pageActionBuffer = ((MainPage)container).getPageActionBuffer();
		// and record the current size
		Pair<PageJSActionBuffer, Integer> pair = new Pair<PageJSActionBuffer, Integer>(pageActionBuffer, Integer.valueOf(pageActionBuffer.getBuffer().size()));
		closeDialogStack.push(pair);
		try
		{
			return super.doCloseFormInWindow(container);
		}
		finally
		{
			closeDialogStack.pop();
		}
	}

	@Override
	protected void hideContainer(IMainContainer container)
	{
		int currentSize = -1;
		if (closeDialogStack.size() > 0)
		{
			// if there is one on the stack, look what the current size is
			// the added actions between the stored size and this size are the once that need to be last. (show of a next dialog)
			currentSize = closeDialogStack.peek().getLeft().getBuffer().size();
		}
		super.hideContainer(container);
		if (currentSize > 0)
		{
			// if the size before the hide is bigger then 0 then move from there until the end of the list to the
			// position stored before onhide is called.
			List<PageAction> buffer = closeDialogStack.peek().getLeft().getBuffer();
			if (currentSize < buffer.size())
			{
				List<PageAction> subList = buffer.subList(currentSize, buffer.size());
				ArrayList<PageAction> copy = new ArrayList<PageJSActionBuffer.PageAction>(subList);
				subList.clear();
				buffer.addAll(closeDialogStack.peek().getRight().intValue(), copy);
			}
		}
	}


	@Override
	protected List<String> getOrderedContainers()
	{
		FormManager fm = ((FormManager)application.getFormManager());
		List<String> all = fm.getCreatedMainContainerKeys();
		int size = all.size();
		ArrayList<String> al = new ArrayList<String>(size);
		HashSet<String> visited = new HashSet<String>();
		ArrayList<String> result = new ArrayList<String>(size);
		for (String key : all)
		{
			((MainPage)fm.getMainContainer(key)).getMainPageReversedCloseSeq(al, visited);
		}
		// now we have in al first the root containers and then children, according to hierarchy; this list in reveresed order is what we want

		for (int i = al.size() - 1; i >= 0; i--)
		{
			result.add(al.get(i));
		}
		return result;
	}

	private static class MainApplicationWebJSFrame extends WebRuntimeWindow
	{

		public MainApplicationWebJSFrame(IApplication application)
		{
			super((IWebClientApplication)application, null, JSWindow.WINDOW, null);
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

		@Override
		public boolean isVisible()
		{
			return true;
		}

	}
}
