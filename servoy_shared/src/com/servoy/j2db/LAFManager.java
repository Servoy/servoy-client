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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.metal.MetalTheme;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ExtendableURLClassLoader;
import com.servoy.j2db.util.JarManager;

/**
 * Manages lafs for the application.
 * 
 * @author jblok
 */
public class LAFManager extends JarManager implements ILAFManager
{
	protected ExtendableURLClassLoader _lafClassLoader;

	protected List<LookAndFeelInfo> lafInfos;
	protected Map<String, String> lafThemes;

	/**
	 * Ctor.
	 */
	public LAFManager()
	{
		lafInfos = new ArrayList<LookAndFeelInfo>();
		lafThemes = new HashMap<String, String>();
	}

	/**
	 * @see com.servoy.j2db.IBeanManager#flushCachedItems()
	 */
	public void flushCachedItems()
	{
		lafInfos = new ArrayList<LookAndFeelInfo>();
		lafThemes = new HashMap<String, String>();
	}

	/**
	 * @see com.servoy.j2db.IBeanManager#init()
	 */
	public void init()
	{
		loadDefaultLAFs();
	}

	private void loadDefaultLAFs()
	{
		//load defaults
		LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();

		for (LookAndFeelInfo info : lafs)
		{
			if (!lafInfos.contains(info))
			{
				Debug.trace(info);
				lafInfos.add(info);
			}
		}
	}

	public LookAndFeel createInstance(String clazzName) throws Exception
	{
		Class< ? > clazz = null;
		if (_lafClassLoader != null)
		{
			clazz = Class.forName(clazzName, true, _lafClassLoader);
		}
		else
		{
			clazz = Class.forName(clazzName);
		}
		if (clazz != null)
		{
			return (LookAndFeel)clazz.newInstance();
		}
		return null;
	}

	public MetalTheme createThemeInstance(String clazzName) throws Exception
	{
		Class< ? > clazz = null;
		if (_lafClassLoader != null)
		{
			clazz = Class.forName(clazzName, true, _lafClassLoader);
		}
		else
		{
			clazz = Class.forName(clazzName);
		}
		if (clazz != null)
		{
			return (MetalTheme)clazz.newInstance();
		}
		return null;
	}

	private boolean isLafsLoadedFromProps = false;

	public List<LookAndFeelInfo> getLAFInfos(IApplication _app)//client loading
	{
		if (_app.getApplicationType() == IApplication.CLIENT && !isLafsLoadedFromProps)
		{
			Iterator<LookAndFeelInfo> it = lafInfos.iterator();
			HashSet<String> hsNames = new HashSet<String>();
			while (it.hasNext())
			{
				hsNames.add(it.next().getClassName());
			}

			int count = 0;
			String sCount = _app.getSettings().getProperty("com.servoy.j2db.LAFManager.LAFCount"); //$NON-NLS-1$
			if (sCount != null)
			{
				count = new Integer(sCount).intValue();
			}
			Debug.trace("laf count " + count); //$NON-NLS-1$
			for (int x = 0; x < count; x++)
			{
				String clazzName = _app.getSettings().getProperty("com.servoy.j2db.LAFManager.LAF." + x + ".className"); //$NON-NLS-1$ //$NON-NLS-2$
				if (!hsNames.contains(clazzName))
				{
					String name = _app.getSettings().getProperty("com.servoy.j2db.LAFManager.LAF." + x + ".name"); //$NON-NLS-1$ //$NON-NLS-2$
					LookAndFeelInfo info = new LookAndFeelInfo(name, clazzName);
					UIManager.installLookAndFeel(info);
					lafInfos.add(info);
					hsNames.add(name);
				}
			}
			isLafsLoadedFromProps = true;
		}
		return lafInfos;
	}

	private boolean isThemesLoadedFromProps = false;

	public Map<String, String> getLoadedThemes(IApplication _app)//client loading
	{
		if (_app.getApplicationType() == IApplication.CLIENT && !isThemesLoadedFromProps)
		{
			int count = 0;
			String sCount = _app.getSettings().getProperty("com.servoy.j2db.LAFManager.ThemeCount"); //$NON-NLS-1$
			if (sCount != null)
			{
				count = new Integer(sCount).intValue();
			}
			Debug.trace("theme count " + count); //$NON-NLS-1$
			for (int x = 0; x < count; x++)
			{
				String clazzName = _app.getSettings().getProperty("com.servoy.j2db.LAFManager.Theme." + x + ".className"); //$NON-NLS-1$ //$NON-NLS-2$
				String name = _app.getSettings().getProperty("com.servoy.j2db.LAFManager.Theme." + x + ".name"); //$NON-NLS-1$ //$NON-NLS-2$
				lafThemes.put(name, clazzName);
			}
			isThemesLoadedFromProps = true;
		}
		return lafThemes;
	}

	public static Boolean mac;

	public static boolean isUsingAppleLAF()
	{
		if (mac == null)
		{
			mac = new Boolean(UIManager.getSystemLookAndFeelClassName().indexOf("apple") > -1); //$NON-NLS-1$
		}
		return mac.booleanValue();
	}
}
