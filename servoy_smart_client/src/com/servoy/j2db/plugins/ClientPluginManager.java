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
package com.servoy.j2db.plugins;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.preference.PreferencePanel;
import com.servoy.j2db.smart.preference.ApplicationPreferences;
import com.servoy.j2db.util.Debug;


public class ClientPluginManager extends PluginManager
{
	public ClientPluginManager(IApplication application)
	{
		super(application);
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
					PreferencePanel[] panels = plugin.getPreferencePanels();
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


}
