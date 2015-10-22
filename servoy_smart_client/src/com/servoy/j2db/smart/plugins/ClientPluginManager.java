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
package com.servoy.j2db.smart.plugins;

import java.lang.reflect.Method;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.PluginManager;
import com.servoy.j2db.preference.ApplicationPreferences;
import com.servoy.j2db.preference.PreferencePanel;
import com.servoy.j2db.util.Debug;

/**
 * Plugin manager running in smart client.
 * 
 * @author rgansevles
 */
@SuppressWarnings("nls")
public class ClientPluginManager extends PluginManager
{
	public ClientPluginManager(IApplication application)
	{
		super(application, null);
	}

	public void addPreferenceTabs(ApplicationPreferences ap)
	{
		synchronized (initLock)
		{
			Object[] list = loadedClientPlugins.values().toArray();
			for (Object element : list)
			{
				IClientPlugin plugin = (IClientPlugin)element;
				try
				{
					PreferencePanel[] panels = getPreferencePanels(plugin);
					if (panels != null)
					{
						for (PreferencePanel element2 : panels)
						{
							ap.addPreferenceTab(element2);
						}
					}
				}
				catch (Throwable e)
				{
					Debug.error(e);
				}
			}

//			if (loadedServerPlugins != null)
//			{
//				for (int j = 0; j < loadedServerPlugins.size(); j++)
//				{
//					IServerPlugin plugin = (IServerPlugin)loadedServerPlugins.get(j);
//					try
//					{
//						PreferencePanel[] panels = plugin.getPreferencePanels();
//						if (panels != null)
//						{
//							for (PreferencePanel element : panels)
//							{
//								ap.addPreferenceTab(element);
//							}
//						}
//					}
//					catch (Throwable e)
//					{
//						Debug.error(e);
//					}
//				}
//			}
		}
	}

	/*
	 * using reflection here since we moved this method downwards and not all plugin will implement ISmartClientPlugin yet
	 * 
	 * @see ISmartClientPlugin
	 */
	private PreferencePanel[] getPreferencePanels(IClientPlugin plugin)
	{
		try
		{
			Method methodToInvoke = null;
			Method[] methods = plugin.getClass().getMethods();
			for (Method method : methods)
			{
				if (method.getName().equalsIgnoreCase("getPreferencePanels"))
				{
					methodToInvoke = method;
					break;
				}
			}
			if (methodToInvoke != null)
			{
				return (PreferencePanel[])methodToInvoke.invoke(plugin, new Object[0]);
			}
		}
		catch (Exception e)
		{
			Debug.error("Failed to get the preference panels for client plugin: " + plugin.getName(), e);
		}
		return null;
	}
}
