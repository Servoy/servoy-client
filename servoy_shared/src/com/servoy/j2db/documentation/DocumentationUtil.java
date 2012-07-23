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

package com.servoy.j2db.documentation;

import java.lang.reflect.Array;


/**
 * Utility class for documentation management.
 * 
 * @author gerzse
 */
public class DocumentationUtil
{

	private static JavaToDocumentedJSTypeTranslator javaToJSTypeTranslator;

	private static Class< ? > loadClassEx(ClassLoader loader, String type) throws ClassNotFoundException
	{
		Class< ? > c;
		if (loader != null)
		{
			try
			{
				c = loader.loadClass(type);
			}
			catch (ClassNotFoundException e)
			{
				c = Class.forName(type);
			}
		}
		else
		{
			c = Class.forName(type);
		}
		return c;
	}

	public static Class< ? > loadClass(ClassLoader loader, String type) throws ClassNotFoundException
	{
		if (type == null) return null;
		else if (Byte.TYPE.getName().equals(type)) return Byte.TYPE;
		else if (Short.TYPE.getName().equals(type)) return Short.TYPE;
		else if (Integer.TYPE.getName().equals(type)) return Integer.TYPE;
		else if (Long.TYPE.getName().equals(type)) return Long.TYPE;
		else if (Float.TYPE.getName().equals(type)) return Float.TYPE;
		else if (Double.TYPE.getName().equals(type)) return Double.TYPE;
		else if (Character.TYPE.getName().equals(type)) return Character.TYPE;
		else if (Boolean.TYPE.getName().equals(type)) return Boolean.TYPE;
		else if (Void.TYPE.getName().equals(type)) return Void.TYPE;
		else
		{
			int dim = 0, i = 0;
			while (type.charAt(i) == '[')
			{
				dim++;
				i++;
			}
			if (dim > 0)
			{
				// If we have at least a '[' followed by an 'L', then it's a non-primitive type.
				if (type.charAt(i) == 'L')
				{
					i++;
					String baseType = type.substring(i, type.length() - 1);
					Class< ? > clz = loadClassEx(loader, baseType);
					for (i = 0; i < dim; i++)
					{
						clz = Array.newInstance(clz, 0).getClass();
					}
					return clz;
				}
				// If there is no 'L' after the '['s, then it's a primitive type.
				else
				{
					return loadClassEx(loader, type);
				}
			}
			else
			{
				return loadClassEx(loader, type);
			}
		}
	}

	/**
	 * Gives a translator object capable of translating Java classes either to another java class that is ServoyDocumented or scriptable, or directly to
	 * a javascript type name.
	 */
	public static JavaToDocumentedJSTypeTranslator getJavaToJSTypeTranslator()
	{
		if (javaToJSTypeTranslator == null)
		{
			synchronized (DocumentationUtil.class)
			{
				if (javaToJSTypeTranslator == null)
				{
					javaToJSTypeTranslator = new JavaToDocumentedJSTypeTranslator();
				}
			}
		}
		return javaToJSTypeTranslator;
	}

}
