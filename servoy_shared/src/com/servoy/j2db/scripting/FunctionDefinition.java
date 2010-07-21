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

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;

/**
 * Function definition, use to retrieve form and method name from a javascript Function object
 * 
 * This class should be used to hold on to function objects in plugins that are later called by using the method
 * {@link #executeSync(IClientPluginAccess, Object[])} which is a short cut to {@link IClientPluginAccess#executeMethod(String, String, Object[], boolean)}
 * 
 */
public class FunctionDefinition
{
	private String formName;
	private String methodName;

	public FunctionDefinition() // for bean xml serialization
	{
	}

	/**
	 * Create FunctionDefinition from [formname|globals].method string.
	 * @param methodString
	 * @since 5.0
	 */
	public FunctionDefinition(String methodString)
	{
		if (methodString != null)
		{
			String[] split = methodString.split("\\."); //$NON-NLS-1$
			if (split.length == 1)
			{
				// global method
				methodName = split[0];
			}
			else
			{
				if (!"globals".equals(split[0])) //$NON-NLS-1$
				{
					// form method
					formName = split[0];
				}
				methodName = split[1];
			}
		}
	}

	public FunctionDefinition(String formName, String methodName)
	{
		this.formName = formName;
		this.methodName = methodName;
	}

	public FunctionDefinition(Function f)
	{
		if (f == null)
		{
			throw new IllegalArgumentException("Method/Function is null"); //$NON-NLS-1$
		}
		Object formNameObj = f.getParentScope().get("_formname_", f.getParentScope()); //$NON-NLS-1$
		this.formName = (formNameObj == null || formNameObj.equals(Scriptable.NOT_FOUND)) ? null : formNameObj.toString();

		Object methodNameObj = f.get("_methodname_", f); //$NON-NLS-1$
		this.methodName = (methodNameObj == null || methodNameObj.equals(Scriptable.NOT_FOUND)) ? null : methodNameObj.toString();

		if (this.methodName == null)
		{
			throw new IllegalArgumentException("Method/Function is not a servoy method"); //$NON-NLS-1$
		}
	}

	public String getFormName()
	{
		return formName;
	}

	public void setFormName(String formName)
	{
		this.formName = formName;
	}

	public String getMethodName()
	{
		return methodName;
	}

	public void setMethodName(String methodName)
	{
		this.methodName = methodName;
	}

	public String toMethodString()
	{
		if (formName == null)
		{
			return "globals." + methodName; //$NON-NLS-1$
		}
		return formName + '.' + methodName;
	}

	public enum Exist
	{
		NO_SOLUTION, METHOD_NOT_FOUND, METHOD_FOUND, FORM_NOT_FOUND;
	}

	/**
	* Test if the given methodName or formName do exist. Will return one of the {@link Exist} enums.
	*/
	public Exist exists(IClientPluginAccess access)
	{
		if (access instanceof ClientPluginAccessProvider)
		{
			final Exist[] retVal = new Exist[] { Exist.METHOD_NOT_FOUND };
			final IApplication application = ((ClientPluginAccessProvider)access).getApplication();
			Runnable run = new Runnable()
			{
				public void run()
				{
					if (application.getSolution() != null)
					{
						if (formName == null)
						{
							GlobalScope gs = application.getScriptEngine().getSolutionScope().getGlobalScope();
							if (gs.get(methodName) instanceof Function)
							{
								retVal[0] = Exist.METHOD_FOUND;
							}
						}
						else
						{
							FormController fp = ((FormManager)application.getFormManager()).leaseFormPanel(formName);
							if (fp == null)
							{
								retVal[0] = Exist.FORM_NOT_FOUND;
							}
							if (fp.getFormScope().get(methodName, fp.getFormScope()) instanceof Function)
							{
								retVal[0] = Exist.METHOD_FOUND;
							}
						}
					}
					else
					{
						retVal[0] = Exist.NO_SOLUTION;
					}
				}
			};

			application.invokeAndWait(run);
			return retVal[0];
		}
		return Exist.METHOD_NOT_FOUND;
	}

	/**
	 * @deprecated see {@link #executeAsync(IClientPluginAccess, Object[])}and {@link #executeSync(IClientPluginAccess, Object[])
	 */
	@Deprecated
	public Object execute(IClientPluginAccess access, Object[] arguments, boolean async)
	{
		return exec(access, arguments, async);
	}

	/**
	 * Helper method that calls the {@link IClientPluginAccess#executeMethod(String, String, Object[], boolean)} for you with the right formname/context and
	 * method name.
	 * <b>The method is called asynchronously in the user-interface thread.
	 * 
	 * @param access The IClientPluginAccess object to call
	 * @param arguments The arguments to give to the method calls
	 */
	public void executeAsync(IClientPluginAccess access, Object[] arguments)
	{
		exec(access, arguments, true);
	}

	/**
	 * Helper method that calls the {@link IClientPluginAccess#executeMethod(String, String, Object[], boolean)} for you with the right formname/context and
	 * method name.
	 * 
	 * @param access The IClientPluginAccess object to call
	 * @param arguments The arguments to give to the method calls
	 * 
	 * @return The object that the method returns.
	 */
	public Object executeSync(IClientPluginAccess access, Object[] arguments)
	{
		return exec(access, arguments, false);
	}

	@SuppressWarnings("nls")
	private Object exec(IClientPluginAccess access, Object[] arguments, boolean async)
	{
		try
		{
			return access.executeMethod(formName, methodName, arguments, async);
		}
		catch (Exception e)
		{
			access.handleException(
				"Failed to execute the method of context " + formName + " and name " + methodName + " on the solution " + access.getSolutionName(), e);
		}
		return null;
	}


	@Override
	public String toString()
	{
		return "Function(" + toMethodString() + ')'; //$NON-NLS-1$
	}
}
