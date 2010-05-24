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
package com.servoy.j2db.persistence;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Map.Entry;

import com.servoy.j2db.util.Debug;

public class ClientMethodTemplatesLoader
{
	public static final String JSEVENT_ARG_LOCATION_PREFIX = "method.template.jseventarg.location.";
	private static boolean loaded = false;
	private static final Object lock = new Object();

	public static Object getLock()
	{
		return lock;
	}

	public static void setLoaded()
	{
		loaded = true;
	}

	public static synchronized void loadClientMethodTemplatesIfNeeded()
	{
		if (!loaded)
		{
			loaded = true;
			URL url = ClientMethodTemplatesLoader.class.getResource("doc/methodtemplates.properties"); //$NON-NLS-1$
			try
			{
				InputStream is = url.openStream();
				Properties properties = new Properties();
				properties.load(is);
				is.close();

				int counter = 0;
				for (Entry<Object, Object> entry : properties.entrySet())
				{
					if (((String)entry.getKey()).startsWith(JSEVENT_ARG_LOCATION_PREFIX))
					{
						String name = ((String)entry.getKey()).substring(JSEVENT_ARG_LOCATION_PREFIX.length());
						if (MethodTemplate.COMMON_TEMPLATES.get(name) == null) // make sure we do not overwrite dev. loaded templates - as they contain more info - although it should never be != null because of the loaded flag
						{
							int argIndex = Integer.parseInt((String)entry.getValue());

							MethodArgument[] args = new MethodArgument[argIndex + 1];
							for (int i = 0; i < argIndex; i++)
							{
								args[i] = new MethodArgument(null, MethodArgument.ArgumentType.Other, null);
							}
							args[argIndex] = new MethodArgument(null, MethodArgument.ArgumentType.JSEvent, null);

							MethodTemplate mt = new MethodTemplate(null, null, args, null, false);
							MethodTemplate.COMMON_TEMPLATES.put(name, mt);
							counter++;
						}
					}
				}

				Debug.log("Loaded " + counter + " client method templates.");
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

}
