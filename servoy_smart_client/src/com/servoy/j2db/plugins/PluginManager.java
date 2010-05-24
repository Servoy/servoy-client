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


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.IColumnConverter;
import com.servoy.j2db.dataprocessing.IColumnConverterManager;
import com.servoy.j2db.dataprocessing.IColumnValidator;
import com.servoy.j2db.dataprocessing.IColumnValidatorManager;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ExtendableURLClassLoader;
import com.servoy.j2db.util.Ident;
import com.servoy.j2db.util.JarManager;
import com.servoy.j2db.util.Settings;

/**
 * Manages plugins for the application.
 * 
 * @author jcompagner, jblok
 */
public class PluginManager extends JarManager implements IPluginManagerInternal, PropertyChangeListener
{
	private final File pluginDir;
	private static ExtendableURLClassLoader _pluginsClassLoader;
	private static Map supportLibUrls = new HashMap();
	private static Map pluginUrls = new HashMap();

	private static Extension[] clientPluginInfo;

	// ---instance vars
	protected final Object[] initLock = new Object[1];//when filled with an Object the init is completed

	// ---plugin instances
	protected Map loadedClientPlugins; //contains all instances of client plugins, (name -> instance)
	private List loadedServerPlugins;//contains all instances of server plugins

	/**
	 * Loads plugins from the plugins directory.
	 */
	public PluginManager(Object prop_change_source)
	{
		this();
		J2DBGlobals.addPropertyChangeListener(prop_change_source, this);
	}

	public PluginManager()
	{
		super();
		pluginDir = new File(Settings.getInstance().getProperty(J2DBGlobals.SERVOY_APPLICATION_SERVER_DIRECTORY_KEY) + File.separator + "plugins"); //$NON-NLS-1$ 
		if (pluginUrls.size() == 0 && pluginDir.isDirectory())
		{
			readDir(pluginDir, pluginUrls, supportLibUrls, null, false);
		}
	}

	public PluginManager(String pluginDirAsString)
	{
		super();
		pluginDir = new File(pluginDirAsString);
		if (pluginUrls.size() == 0 && this.pluginDir.isDirectory())
		{
			readDir(pluginDir, pluginUrls, supportLibUrls, null, false);
		}
	}

	public File getPluginDir()
	{
		return pluginDir;
	}

	/**
	 * Unload all plugins.
	 */
	public synchronized void flushCachedItems()
	{
		if (loadedClientPlugins != null)
		{
			Object[] list = loadedClientPlugins.values().toArray();
			for (Object element : list)
			{
				IClientPlugin plugin = (IClientPlugin)element;
				try
				{
					plugin.unload();
				}
				catch (Throwable th)
				{
					Debug.error("Error occured unloading client plugin: " + plugin.getName() + "  error: " + th.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			loadedClientPlugins = null;
		}

		if (loadedServerPlugins != null)
		{
			for (int j = 0; j < loadedServerPlugins.size(); j++)
			{
				IPlugin plugin = (IPlugin)loadedServerPlugins.get(j);
				try
				{
					plugin.unload();
				}
				catch (Throwable th)
				{
					Debug.error("Error ocured unloading server plugin: " + plugin.getClass().getName() + "  error: " + th.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			loadedServerPlugins = null;
		}
	}

	public Extension[] loadClientPluginDefs() throws IOException
	{
		if (clientPluginInfo == null)
		{
			clientPluginInfo = getExtensions((ExtendableURLClassLoader)getClassLoader(), IClientPlugin.class, pluginUrls);
		}
		return clientPluginInfo;
	}

	protected void checkIfInitialized()
	{
		synchronized (initLock)
		{
			if (initLock[0] == null)
			{
				try
				{
					initLock.wait(5 * 1000);//max 5 sec, if not copleted then something else is wrong
				}
				catch (InterruptedException e)
				{
					Debug.error(e);
				}
			}
		}
	}

	private void flagInitialized()
	{
		initLock[0] = new Object();//done
		initLock.notifyAll();
	}

	/**
	 * Note: load clients first
	 * 
	 * @return
	 */
	private Class[] loadServerPluginDefs()
	{
		try
		{
			Map all = new HashMap();
			all.putAll(pluginUrls);
//			all.putAll(supportLibUrls);
			Class[] classes = getAssignableClasses((ExtendableURLClassLoader)getClassLoader(), IServerPlugin.class, all);
			return classes;
		}
		catch (Throwable th)
		{
			Debug.error("Error occured retrieving server plugins, error: " + th.getMessage()); //$NON-NLS-1$
			return null;
		}
	}

	public void init()
	{
		//ignore 
	}

	//should only be called by app server
	public void initServerPlugins(IServerAccess app)
	{
		synchronized (initLock)
		{
			if (loadedServerPlugins == null)
			{
				loadedServerPlugins = new ArrayList();

				Class[] classes = loadServerPluginDefs();
				for (Class element : classes)
				{
					try
					{
						IServerPlugin plugin = loadServerPlugin(element);
						if (plugin != null)
						{
							long now = System.currentTimeMillis();
							plugin.initialize(app);
							Debug.trace("Plugin " + element.getName() + " initialised in " + (System.currentTimeMillis() - now) + " ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							loadedServerPlugins.add(plugin);
						}
					}
					catch (Throwable th)
					{
						Debug.error("Error occured loading server plugin class " + element.getName() + ", error: " + th.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}

				flagInitialized();
			}
		}
	}

	/**
	 * Initialize plugins.
	 */
	public void initClientPlugins(IApplication application, IClientPluginAccess app)
	{
		synchronized (initLock)
		{
			loadClientPlugins(application);
			initClientPluginsEx(app);
		}
	}

	/**
	 * Only load client plugins; do not initialize them.
	 */
	public void loadClientPlugins(IApplication application)
	{
		synchronized (initLock)
		{
			loadedClientPlugins = new HashMap();
			if (application == null || !application.isRunningRemote()) //Servoy developer loading
			{
				try
				{
					Extension[] exts = loadClientPluginDefs();
					if (exts != null)
					{
						for (Extension element : exts)
						{
							try
							{
								loadClientPlugin(element.instanceClass);
							}
							catch (Throwable th)
							{
								Debug.error("Error occured loading client plugin class " + element.instanceClass.getName() + ", error: " + th); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
					}
				}
				catch (IOException th)
				{
					Debug.error("Error occured loading client plugin classes, error: " + th); //$NON-NLS-1$
				}
			}
			else
			//Servoy client loading
			{
				int count = 0;
				Settings settings = Settings.getInstance();
				String sCount = settings.getProperty("com.servoy.j2db.plugins.PluginManager.PluginCount"); //$NON-NLS-1$
				if (sCount != null)
				{
					count = new Integer(sCount).intValue();
				}
				Debug.trace("plugin count " + count); //$NON-NLS-1$

				for (int x = 0; x < count; x++)
				{
					String clazzName = settings.getProperty("com.servoy.j2db.plugins.PluginManager.Plugin." + x + ".className"); //$NON-NLS-1$ //$NON-NLS-2$
					if (clazzName != null && clazzName.length() != 0)
					{
						try
						{
							loadClientPlugin(Class.forName(clazzName.trim()));
						}
						catch (Throwable th)
						{
							Debug.error("Error occured loading client plugin class " + clazzName + ", error: " + th); //$NON-NLS-1$ //$NON-NLS-2$
							Debug.error(th);
						}
					}
				}
			}

			columnConverterManager = new ColumnConverterManager();
			columnValidatorManager = new ColumnValidatorManager();
			checkForConvertersAndValidators(columnConverterManager, columnValidatorManager);
		}
	}

	private IColumnConverterManager columnConverterManager;
	private IColumnValidatorManager columnValidatorManager;

	public static class ColumnConverterManager implements IColumnConverterManager
	{
		private final Map converters = new HashMap();//name -> converter

		public IColumnConverter getConverter(String name)
		{
			return (IColumnConverter)converters.get(name);
		}

		public void registerConvertor(IColumnConverter converter)
		{
			Object obj = converters.put(converter.getName(), converter);
			if (obj != null) Debug.log("Duplicate converter found: " + converter.getName()); //$NON-NLS-1$
		}

		public Map getConverters()
		{
			return converters;
		}

	}
	public static class ColumnValidatorManager implements IColumnValidatorManager
	{
		private final Map validators = new HashMap();//name -> validator

		public void registerValidator(IColumnValidator validator)
		{
			Object obj = validators.put(validator.getName(), validator);
			if (obj != null) Debug.log("Duplicate validator found: " + validator.getName()); //$NON-NLS-1$
		}

		public Map<String, IColumnValidator> getValidators()
		{
			return validators;
		}


		public IColumnValidator getValidator(String name)
		{
			return (IColumnValidator)validators.get(name);
		}

	}

	public IColumnConverterManager getColumnConverterManager()
	{
		return columnConverterManager;
	}

	public IColumnValidatorManager getColumnValidatorManager()
	{
		return columnValidatorManager;
	}

	private void initClientPluginsEx(IClientPluginAccess app)
	{
		synchronized (initLock)
		{
			// we assume that the plugins are already loaded
			Object[] list = loadedClientPlugins.values().toArray();
			for (Object element : list)
			{
				IClientPlugin plugin = (IClientPlugin)element;
				try
				{
					long now = System.currentTimeMillis();
					plugin.initialize(app);
//					if (plugin instanceof IAgent)
//					{
//						IApplication base = ((ClientPluginAccessProvider)app).getApplication();
//						if (base instanceof J2DBClient)
//						{
//							((J2DBClient)base).setAgent((IAgent)plugin);
//						}
//					}
					Debug.trace("Plugin " + plugin.getName() + " initialised in " + (System.currentTimeMillis() - now) + " ms."); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
				catch (Throwable th)
				{
					Debug.error("Error ocured initializing plugin: " + plugin.getName() + " , error: " + th.getMessage()); //$NON-NLS-1$//$NON-NLS-2$
					Debug.error(th);
				}
			}

			flagInitialized();
		}
	}

	private void checkForConvertersAndValidators(IColumnConverterManager converterManager, IColumnValidatorManager validatorManager)
	{
		synchronized (initLock)
		{
			// we assume that the plugins are already loaded
			Object[] list = loadedClientPlugins.values().toArray();
			for (Object element : list)
			{
				IClientPlugin plugin = (IClientPlugin)element;
				try
				{
					long now = System.currentTimeMillis();
					if (plugin instanceof IColumnConverterProvider && converterManager != null)
					{
						IColumnConverter[] cons = ((IColumnConverterProvider)plugin).getColumnConverters();
						if (cons != null)
						{
							for (IColumnConverter element2 : cons)
							{
								if (element2 == null) continue;
								converterManager.registerConvertor(element2);
							}
						}
					}
					if (plugin instanceof IColumnValidatorProvider && validatorManager != null)
					{
						IColumnValidator[] vals = ((IColumnValidatorProvider)plugin).getColumnValidators();
						if (vals != null)
						{
							for (IColumnValidator element2 : vals)
							{
								if (element2 == null) continue;
								validatorManager.registerValidator(element2);
							}
						}
					}
					Debug.trace("Plugin " + plugin.getName() + " checked for converter/validator in " + (System.currentTimeMillis() - now) + " ms."); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
				catch (Throwable th)
				{
					Debug.error("Error ocured checking plugin: " + plugin.getName() + " , error: " + th.getMessage()); //$NON-NLS-1$//$NON-NLS-2$
					Debug.error(th);
				}
			}
		}
	}

	private IClientPlugin loadClientPlugin(Class pluginClass)
	{
		try
		{
			long now = System.currentTimeMillis();
			IClientPlugin plugin = (IClientPlugin)pluginClass.newInstance();
			if (validatePlugin(plugin))
			{
				plugin.load();
				String name = plugin.getName();
				if (!Ident.checkIfKeyword(name))
				{
					loadedClientPlugins.put(name, plugin);
					Debug.trace("Plugin " + name + " loaded in " + (System.currentTimeMillis() - now) + " ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					return plugin;
				}
				else
				{
					Debug.error("Error occured loading client plugin, name is reserved word " + name); //$NON-NLS-1$
				}
			}
		}
		catch (Throwable th)
		{
			Debug.error("Error occured loading client class " + pluginClass.getName() + " from plugin.", th); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	private IServerPlugin loadServerPlugin(Class pluginClass)
	{
		try
		{
			long now = System.currentTimeMillis();
			IServerPlugin plugin = (IServerPlugin)pluginClass.newInstance();
			plugin.load();
			Debug.trace("Plugin " + pluginClass.getName() + " loaded in " + (System.currentTimeMillis() - now) + " ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return plugin;
		}
		catch (Throwable th)
		{
			Debug.error("Error occured loading server class " + pluginClass.getName() + " from plugin, error: " + th); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	public IClientPlugin getClientPlugin(String name)
	{
		checkIfInitialized();
		synchronized (initLock)
		{
			return (IClientPlugin)loadedClientPlugins.get(name);
		}
	}

	private List getClientPlugins()
	{
		checkIfInitialized();
		synchronized (initLock)
		{
			Object[] list = loadedClientPlugins.values().toArray();
			Arrays.sort(list, NameComparator.INSTANCE);
			return Arrays.asList(list);
		}
	}

	public List getServerPlugins()
	{
		checkIfInitialized();
		synchronized (initLock)
		{
			return loadedServerPlugins;
		}
	}

	private boolean validatePlugin(IClientPlugin plugin)
	{
		String name = plugin.getName();
		if (name == null || name.trim().length() == 0)
		{
			Debug.error("Plugin " + plugin.getClass().getName() + " doesn't return a valid getName()"); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}

		if (loadedClientPlugins.get(name) != null)
		{
			Debug.error("A Plugin with the internal name " + name + " has already been loaded"); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		synchronized (initLock)
		{
			if (IPlugin.PROPERTY_SOLUTION.equals(evt.getPropertyName()) || IPlugin.PROPERTY_LOCALE.equals(evt.getPropertyName()) ||
				IPlugin.PROPERTY_CURRENT_WINDOW.equals(evt.getPropertyName()))
			{
				if (loadedClientPlugins != null)
				{
					Object[] list = loadedClientPlugins.values().toArray();
					for (Object element : list)
					{
						IClientPlugin plugin = (IClientPlugin)element;
						try
						{
							plugin.propertyChange(evt);
						}
						catch (Throwable e)//incase method is missing in old plugin or the plugin designer did something stupid
						{
							Debug.error("Error occured informing client plugin " + plugin.getName() + ", error: " + e); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			}
		}
	}

	/**
	 * Get the classloader (normally system classloader).
	 * 
	 * @return ClassLoader
	 */
	public ClassLoader getClassLoader()
	{
		return getPluginClassLoader();
	}

	/**
	 * Load plugins. Load all plugin jars into class loader.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public synchronized ClassLoader getPluginClassLoader()
	{
		if (_pluginsClassLoader == null)
		{
			try
			{
				if (pluginUrls.size() == 0 && pluginDir.isDirectory())
				{
					readDir(pluginDir, pluginUrls, supportLibUrls, null, false);
				}

				List allUrls = new ArrayList(supportLibUrls.size() + pluginUrls.size());
				allUrls.addAll(supportLibUrls.keySet());
				allUrls.addAll(pluginUrls.keySet());
				URL[] urls = (URL[])allUrls.toArray(new URL[allUrls.size()]);
				_pluginsClassLoader = new ExtendableURLClassLoader(urls, getClass().getClassLoader());
			}
			catch (Throwable th)
			{
				Debug.error("Error occured retrieving plugins. No plugins have been loaded, error: " + th.getMessage()); //$NON-NLS-1$
				return null;
			}
		}
		return _pluginsClassLoader;
	}

	/**
	 * @return
	 */
	public PluginManager createEfficientCopy(Object prop_change_source)
	{
		PluginManager retval = new PluginManager(prop_change_source);
		return retval;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.plugins.IPluginManager#getPlugin(java.lang.Class, java.lang.String)
	 */
	public IPlugin getPlugin(Class pluginSubType, String name)
	{
		if (IClientPlugin.class.equals(pluginSubType))
		{
			return getClientPlugin(name);
		}
		if (IServerPlugin.class.equals(pluginSubType))
		{
			return null;//nyi
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.plugins.IPluginManager#getPlugins(java.lang.Class)
	 */
	public List getPlugins(Class pluginSubType)
	{
		if (IClientPlugin.class.equals(pluginSubType))
		{
			return getClientPlugins();
		}
		if (IServerPlugin.class.equals(pluginSubType))
		{
			return getServerPlugins();
		}
		return null;
	}
}