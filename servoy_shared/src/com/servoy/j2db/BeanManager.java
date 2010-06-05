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


import com.servoy.j2db.util.JarManager;

/**
 * Manages plugins for the application.
 * 
 * @author jblok
 */
public class BeanManager extends JarManager implements IBeanManager
{
	protected ClassLoader _beansClassLoader;

	/**
	 * Ctor
	 */
	public BeanManager()
	{
		super();
	}

	public Object createInstance(String clazzName) throws Exception
	{
		Class clazz = null;
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try
		{
			if (_beansClassLoader != null)
			{
				Thread.currentThread().setContextClassLoader(_beansClassLoader);
				clazz = _beansClassLoader.loadClass(clazzName);
			}
			else
			{
				clazz = Class.forName(clazzName);
			}
			if (clazz != null)
			{
				return clazz.newInstance();
			}
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(loader);
		}

		return null;
	}

	public ClassLoader getClassLoader()
	{
		if (_beansClassLoader != null)
		{
			return _beansClassLoader;
		}
		if (getClass().getClassLoader() != null)
		{
			return getClass().getClassLoader();
		}
		return ClassLoader.getSystemClassLoader();
	}

	/**
	 * @see com.servoy.j2db.IBeanManager#flushCachedItems()
	 */
	public void flushCachedItems()
	{
	}

	/**
	 * @see com.servoy.j2db.IBeanManager#init()
	 */
	public void init()
	{
	}

}