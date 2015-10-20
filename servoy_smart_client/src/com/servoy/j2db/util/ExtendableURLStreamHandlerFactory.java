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
package com.servoy.j2db.util;


import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jblok
 */
public class ExtendableURLStreamHandlerFactory implements URLStreamHandlerFactory
{
	private final Map handlers = new HashMap();

	public void addStreamHandler(String protocol, Class urlStreamHandlerClass)
	{
		if (!handlers.containsKey(protocol)) handlers.put(protocol, urlStreamHandlerClass);
	}

	public void addStreamHandler(String protocol, URLStreamHandler urlStreamHandlerInstance)
	{
		if (!handlers.containsKey(protocol)) handlers.put(protocol, urlStreamHandlerInstance);
	}

	public URLStreamHandler createURLStreamHandler(String protocol)
	{
		Object obj = handlers.get(protocol);
		if (obj != null)
		{
			if (obj instanceof Class)
			{
				Class clazz = (Class)obj;
				try
				{
					return (URLStreamHandler)clazz.newInstance();
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
			else if (obj instanceof URLStreamHandler)
			{
				return (URLStreamHandler)obj;
			}
		}
		return null;
	}
}
