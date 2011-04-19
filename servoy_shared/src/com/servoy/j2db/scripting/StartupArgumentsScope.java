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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.servoy.j2db.server.annotations.TerracottaInstrumentedClass;

/**
 *  This class represents a object containing all the startup arguments
 * 
 * @author gboros
 */
@TerracottaInstrumentedClass
public class StartupArgumentsScope extends HashMap<String, Object>
{
	private static final long serialVersionUID = 1L;

	public static final String PARAM_KEY_SOLUTION = "solution"; //$NON-NLS-1$
	public static final String PARAM_KEY_METHOD = "method"; //$NON-NLS-1$
	public static final String PARAM_KEY_ARGUMENT = "argument"; //$NON-NLS-1$
	public static final String PARAM_KEY_CLIENT_IDENTIFIER = "CI"; //$NON-NLS-1$
	private static final String PARAM_KEY_SHORT_SOLUTION = "s"; //$NON-NLS-1$
	private static final String PARAM_KEY_SHORT_METHOD = "m"; //$NON-NLS-1$
	private static final String PARAM_KEY_SHORT_ARGUMENT = "a"; //$NON-NLS-1$

	public static final char PARAM_KEY_VALUE_SEPARATOR = ':';

	public StartupArgumentsScope()
	{
		super();
	}

	/**
	 * @param arguments the map of arguments where the key is the argName and the value is the argValue
	 */
	public StartupArgumentsScope(Map<String, Object> arguments)
	{
		super();
		fillArguments(arguments);
	}

	/**
	 * @param sArguments the array of arguments, where each element is of type argName:argValue
	 */
	public StartupArgumentsScope(String[] sArguments)
	{
		super();
		if (sArguments != null)
		{
			String key, value;
			int sepIdx;
			for (String element : sArguments)
			{
				if (element != null && (sepIdx = element.indexOf(PARAM_KEY_VALUE_SEPARATOR)) > 0)
				{
					key = element.substring(0, sepIdx);
					value = sepIdx < element.length() - 1 ? element.substring(sepIdx + 1) : ""; //$NON-NLS-1$

					put(key, value);
				}
			}
		}
	}

	private void fillArguments(Map<String, Object> arguments)
	{
		Iterator<String> paramsIte = arguments.keySet().iterator();
		String paramKey;
		Object paramValue;
		while (paramsIte.hasNext())
		{
			paramKey = paramsIte.next();
			paramValue = arguments.get(paramKey);
			if (paramValue instanceof String)
			{
				put(paramKey, paramValue);
			}
			else if (paramValue instanceof String[] && ((String[])paramValue).length > 0)
			{
				String[] sParamValue = (String[])paramValue;
				if (sParamValue.length > 1)
				{
					JSMap array = new JSMap("Array");
					for (int i = 0; i < sParamValue.length; i++)
						array.put(new Integer(i), sParamValue[i]);
					put(paramKey, array);
				}
				else put(paramKey, sParamValue[0]);
			}
		}
	}

	public void setSolutionName(String solutionName)
	{
		if (solutionName == null)
		{
			remove(StartupArgumentsScope.PARAM_KEY_SOLUTION);
			remove(StartupArgumentsScope.PARAM_KEY_SHORT_SOLUTION);
		}
		else
		{
			put(StartupArgumentsScope.PARAM_KEY_SOLUTION, solutionName);
			put(StartupArgumentsScope.PARAM_KEY_SHORT_SOLUTION, solutionName);
		}
	}

	public String getSolutionName()
	{
		Object solutionName = get(StartupArgumentsScope.PARAM_KEY_SOLUTION);
		if (solutionName == null) solutionName = get(StartupArgumentsScope.PARAM_KEY_SHORT_SOLUTION);
		return solutionName == null ? null : solutionName.toString();
	}

	public void setMethodName(String methodName)
	{
		if (methodName == null)
		{
			remove(StartupArgumentsScope.PARAM_KEY_METHOD);
			remove(StartupArgumentsScope.PARAM_KEY_SHORT_METHOD);
		}
		else
		{
			put(StartupArgumentsScope.PARAM_KEY_METHOD, methodName);
			put(StartupArgumentsScope.PARAM_KEY_SHORT_METHOD, methodName);
		}
	}

	public String getMethodName()
	{
		Object methodName = get(StartupArgumentsScope.PARAM_KEY_METHOD);
		if (methodName == null) methodName = get(StartupArgumentsScope.PARAM_KEY_SHORT_METHOD);
		return methodName == null ? null : methodName.toString();
	}

	public void setFirstArgument(String firstArgument)
	{
		if (firstArgument == null)
		{
			remove(StartupArgumentsScope.PARAM_KEY_ARGUMENT);
			remove(StartupArgumentsScope.PARAM_KEY_SHORT_ARGUMENT);
		}
		else
		{
			put(StartupArgumentsScope.PARAM_KEY_ARGUMENT, firstArgument);
			put(StartupArgumentsScope.PARAM_KEY_SHORT_ARGUMENT, firstArgument);
		}
	}

	public String getFirstArgument()
	{
		Object firstArgument = get(StartupArgumentsScope.PARAM_KEY_ARGUMENT);
		if (firstArgument == null) firstArgument = get(StartupArgumentsScope.PARAM_KEY_SHORT_ARGUMENT);
		if (firstArgument instanceof JSMap) firstArgument = ((JSMap)firstArgument).get(Integer.valueOf(0));
		return firstArgument == null ? null : firstArgument.toString();
	}


	public void setClientIdentifier(String clientIdentifier)
	{
		if (clientIdentifier == null)
		{
			remove(StartupArgumentsScope.PARAM_KEY_CLIENT_IDENTIFIER);
		}
		else
		{
			put(StartupArgumentsScope.PARAM_KEY_CLIENT_IDENTIFIER, clientIdentifier);
		}
	}

	public String getClientIdentifier()
	{
		Object clientIdentifier = get(StartupArgumentsScope.PARAM_KEY_CLIENT_IDENTIFIER);
		return clientIdentifier == null ? null : clientIdentifier.toString();
	}

	private JSMap jsMap;

	public JSMap toJSMap()
	{
		if (jsMap == null)
		{
			jsMap = new JSMap();
			Object key;
			for (Map.Entry<String, Object> e : entrySet())
			{
				key = e.getKey();
				if (StartupArgumentsScope.PARAM_KEY_SOLUTION.equals(key) || StartupArgumentsScope.PARAM_KEY_METHOD.equals(key) ||
					StartupArgumentsScope.PARAM_KEY_CLIENT_IDENTIFIER.equals(key) || StartupArgumentsScope.PARAM_KEY_SHORT_SOLUTION.equals(key) ||
					StartupArgumentsScope.PARAM_KEY_SHORT_METHOD.equals(key)) continue;

				jsMap.put(StartupArgumentsScope.PARAM_KEY_SHORT_ARGUMENT.equals(key) ? StartupArgumentsScope.PARAM_KEY_ARGUMENT : e.getKey(), e.getValue());
			}
		}

		return jsMap;
	}
}
