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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.server.annotations.TerracottaInstrumentedClass;

/**
 *  This class represents a javascript object containing all the startup arguments
 * 
 * @author gboros
 */
@TerracottaInstrumentedClass
public class StartupArgumentsScope extends DefaultScope implements Externalizable
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
		super(null);
	}

	/**
	 * @param arguments the map of arguments where the key is the argName and the value is the argValue
	 */
	public StartupArgumentsScope(Map<String, Object> arguments)
	{
		super(null);
		fillArguments(arguments);
	}

	/**
	 * @param sArguments the array of arguments, where each element is of type argName:argValue
	 */
	public StartupArgumentsScope(String[] sArguments)
	{
		super(null);
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

					put(key, this, value);
				}
			}
		}
	}

	/**
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		this.allIndex = (HashMap<Integer, Object>)in.readObject();
		this.allVars = (HashMap<String, Object>)in.readObject();

	}

	/**
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeObject(this.allIndex);
		out.writeObject(this.allVars);
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
				put(paramKey, this, paramValue);
			}
			else if (paramValue instanceof String[] && ((String[])paramValue).length > 0)
			{

				put(paramKey, this, ((String[])paramValue).length > 1 ? new NativeArray((String[])paramValue) : ((String[])paramValue)[0]);
			}
		}
	}

	public void setSolutionName(String solutionName)
	{
		if (solutionName == null)
		{
			delete(StartupArgumentsScope.PARAM_KEY_SOLUTION);
			delete(StartupArgumentsScope.PARAM_KEY_SHORT_SOLUTION);
		}
		else
		{
			put(StartupArgumentsScope.PARAM_KEY_SOLUTION, this, solutionName);
			put(StartupArgumentsScope.PARAM_KEY_SHORT_SOLUTION, this, solutionName);
		}
	}

	public String getSolutionName()
	{
		Object solutionName = get(StartupArgumentsScope.PARAM_KEY_SOLUTION, this);
		if (solutionName == Scriptable.NOT_FOUND) solutionName = get(StartupArgumentsScope.PARAM_KEY_SHORT_SOLUTION, this);
		return solutionName == Scriptable.NOT_FOUND ? null : solutionName.toString();
	}

	public void setMethodName(String methodName)
	{
		if (methodName == null)
		{
			delete(StartupArgumentsScope.PARAM_KEY_METHOD);
			delete(StartupArgumentsScope.PARAM_KEY_SHORT_METHOD);
		}
		else
		{
			put(StartupArgumentsScope.PARAM_KEY_METHOD, this, methodName);
			put(StartupArgumentsScope.PARAM_KEY_SHORT_METHOD, this, methodName);
		}
	}

	public String getMethodName()
	{
		Object methodName = get(StartupArgumentsScope.PARAM_KEY_METHOD, this);
		if (methodName == Scriptable.NOT_FOUND) methodName = get(StartupArgumentsScope.PARAM_KEY_SHORT_METHOD, this);
		return methodName == Scriptable.NOT_FOUND ? null : methodName.toString();
	}

	public void setFirstArgument(String firstArgument)
	{
		if (firstArgument == null)
		{
			delete(StartupArgumentsScope.PARAM_KEY_ARGUMENT);
			delete(StartupArgumentsScope.PARAM_KEY_SHORT_ARGUMENT);
		}
		else
		{
			put(StartupArgumentsScope.PARAM_KEY_ARGUMENT, this, firstArgument);
			put(StartupArgumentsScope.PARAM_KEY_SHORT_ARGUMENT, this, firstArgument);
		}
	}

	public String getFirstArgument()
	{
		Object firstArgument = get(StartupArgumentsScope.PARAM_KEY_ARGUMENT, this);
		if (firstArgument == Scriptable.NOT_FOUND) firstArgument = get(StartupArgumentsScope.PARAM_KEY_SHORT_ARGUMENT, this);
		return firstArgument == Scriptable.NOT_FOUND ? null : firstArgument.toString();
	}


	public void setClientIdentifier(String clientIdentifier)
	{
		if (clientIdentifier == null)
		{
			delete(StartupArgumentsScope.PARAM_KEY_CLIENT_IDENTIFIER);
		}
		else
		{
			put(StartupArgumentsScope.PARAM_KEY_CLIENT_IDENTIFIER, this, clientIdentifier);
		}
	}

	public String getClientIdentifier()
	{
		Object clientIdentifier = get(StartupArgumentsScope.PARAM_KEY_CLIENT_IDENTIFIER, this);
		return clientIdentifier == Scriptable.NOT_FOUND ? null : clientIdentifier.toString();
	}

	public Map<String, Object> getArguments()
	{
		return allVars;
	}

	@Override
	public String getClassName()
	{
		return "ArgumentsScope"; //$NON-NLS-1$
	}

	@Override
	public String toString()
	{
		return "ArgumentsScope: " + allVars.toString(); //$NON-NLS-1$
	}
}
