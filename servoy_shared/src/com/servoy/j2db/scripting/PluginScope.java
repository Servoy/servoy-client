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
package com.servoy.j2db.scripting;


import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 */
public class PluginScope extends DefaultScope
{
	private volatile IApplication application;

	public PluginScope(Scriptable toplevelScope, IApplication application)
	{
		super(toplevelScope);
		this.application = application;
		setLocked(true);
	}

	@Override
	public void destroy()
	{
		super.destroy();
		this.application = null;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		if ("length".equals(name)) //$NON-NLS-1$
		{
			return new Integer(application.getPluginManager().getPlugins(IClientPlugin.class).size());
		}
		if ("allnames".equals(name)) //$NON-NLS-1$
		{
			Context.enter();
			try
			{
				List<IClientPlugin> lst = application.getPluginManager().getPlugins(IClientPlugin.class);
				Object[] array = new String[lst.size()];
				for (int i = 0; i < lst.size(); i++)
				{
					IClientPlugin plugin = lst.get(i);
					array[i] = plugin.getName();
				}
				Arrays.sort(array);
				return new NativeJavaArray(this, array);
			}
			finally
			{
				Context.exit();
			}
		}

		String realName = name;
		if ("it2be_menubar".equals(realName)) realName = "menubar";//we bought, just map, has to backwards compatible anyway //$NON-NLS-1$ //$NON-NLS-2$
		Object o = super.get(realName, start);
		if (o == Scriptable.NOT_FOUND || o == null)
		{
			if (realName.equals("Function"))//performance optimize //$NON-NLS-1$
			{
				return Scriptable.NOT_FOUND;
			}

			IScriptable tocall = null;
			IClientPlugin plugin = application.getPluginManager().getPlugin(IClientPlugin.class, realName);
			if (plugin == null)
			{
				// original name not found, try updated name
				realName = getUpdatedPluginName(name);
				if (!name.equals(realName))
				{
					plugin = application.getPluginManager().getPlugin(IClientPlugin.class, realName);
				}
			}
			if (plugin != null)
			{
				try
				{
					Method method = plugin.getClass().getMethod("getScriptObject", (Class[])null);
					if (method != null)
					{
						tocall = (IScriptable)method.invoke(plugin, (Object[])null);
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
			if (tocall != null)
			{
				// first register the script object itself
				ScriptObjectRegistry.registerScriptObjectForClass(tocall.getClass(), tocall);
				ServoyNativeJavaObject s_tocall = null;
				Context.enter();
				try
				{
					InstanceJavaMembers ijm = new InstanceJavaMembers(this, tocall.getClass());
					s_tocall = new ServoyNativeJavaObject(this, tocall, ijm);
					setLocked(false);
					put(realName, this, s_tocall);//save so we do not all this again
					if (realName != name) put(name, this, s_tocall);
					setLocked(true);
					IExecutingEnviroment scriptEngine = application.getScriptEngine();
					if (scriptEngine != null && tocall instanceof IReturnedTypesProvider)
					{
						scriptEngine.registerScriptObjectReturnTypes((IReturnedTypesProvider)tocall, s_tocall);
					}
				}
				finally
				{
					Context.exit();
				}
				return s_tocall;
			}
			return Scriptable.NOT_FOUND;
		}
		return o;
	}

	/**
	 * Map old plugin names to the current one.
	 */
	@SuppressWarnings("nls")
	public static String getUpdatedPluginName(String name)
	{
		if ("kioskmode".equals(name) || "popupmenu".equals(name) || "menubar".equals(name) || "it2be_menubar".equals(name))
		{
			return "window";
		}
		else if ("jasperPluginRMI".equals(name))
		{
			return "jasperReports";
		}
		return name;
	}

	@Override
	public Object[] getIds()
	{
		if (application.getPluginManager() == null) return new Object[0];
		List<IClientPlugin> lst = application.getPluginManager().getPlugins(IClientPlugin.class);
		Object[] array = new String[lst.size()];
		for (int i = 0; i < lst.size(); i++)
		{
			IClientPlugin plugin = lst.get(i);
			array[i] = plugin.getName();
		}
		Arrays.sort(array);
		return array;
	}
}
