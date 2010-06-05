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
import java.util.List;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.JSWindowManager;
import com.servoy.j2db.scripting.JSWindowImpl;

public class WebJSWindowManager extends JSWindowManager
{

	public WebJSWindowManager(IApplication application)
	{
		super(application);
	}

	@Override
	protected JSWindowImpl createWindowInternal(String windowName, int type, JSWindowImpl parent)
	{
		return new WebJSWindowImpl(application, windowName, type, parent);
	}

	@Override
	protected JSWindowImpl getMainApplicationWindow()
	{
		return new MainApplicationWebJSFrame(application);
	}


	@Override
	protected List<String> getOrderedContainers()
	{
		FormManager fm = ((FormManager)application.getFormManager());
		List<String> all = fm.getCreatedMainContainerKeys();
		int size = all.size();
		ArrayList<IMainContainer> al = new ArrayList<IMainContainer>(size);
		ArrayList<String> result = new ArrayList<String>(size);
		for (String key : all)
		{
			((MainPage)fm.getMainContainer(key)).addCallingContainers(al);
		}
		// now we have in al first the root containers and then children, according to hierarchy; this list in reveresed order is what we want

		for (int i = size - 1; i >= 0; i--)
		{
			result.add(al.get(i).getContainerName());
		}
		return result;
	}

	private static class MainApplicationWebJSFrame extends WebJSWindowImpl
	{

		public MainApplicationWebJSFrame(IApplication application)
		{
			super(application, null, JSWindow.WINDOW, null);
		}

		@Override
		protected void doShow(String formName)
		{
			if (formName != null)
			{
				((FormManager)application.getFormManager()).showFormInMainPanel(formName);
			}
		}

		@Override
		public void destroy()
		{
			// you cannot destroy main app. window
		}

		@Override
		public void closeUI()
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