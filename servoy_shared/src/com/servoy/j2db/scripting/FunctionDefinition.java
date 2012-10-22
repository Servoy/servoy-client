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
import com.servoy.j2db.persistence.ScriptVariable;
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
	private String contextName; // form name or sopes.scopeName
	private String methodName;

	public FunctionDefinition() // for bean xml serialization
	{
	}

	/**
	 * Create FunctionDefinition from method string.
	 * @param methodString formname.methodname or globals.methodname or scopes.scopename.methodname
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
				contextName = ScriptVariable.SCOPES_DOT_PREFIX + ScriptVariable.GLOBAL_SCOPE;
			}
			else if (split.length <= 3)
			{
				if (split.length == 2 && ScriptVariable.GLOBAL_SCOPE.equals(split[0]))
				{
					contextName = ScriptVariable.SCOPES_DOT_PREFIX + ScriptVariable.GLOBAL_SCOPE;
				}
				else if (split.length == 3 && ScriptVariable.SCOPES.equals(split[0]))
				{
					contextName = ScriptVariable.SCOPES_DOT_PREFIX + split[1];
				}
				else
				{
					// form method
					if (split.length == 3)
					{
						contextName = split[1];
					}
					else
					{
						contextName = split[0];
					}
				}
				methodName = split[split.length - 1];
			}
		}
	}

	/**
	 * Create FunctionDefinition from context and method name.
	 * @param contextName form name or scopes.scopename
	 * @param methodName
	 * @since 6.1
	 */
	public FunctionDefinition(String contextName, String methodName)
	{
		this.contextName = contextName == null ? ScriptVariable.SCOPES_DOT_PREFIX + ScriptVariable.GLOBAL_SCOPE : contextName;
		this.methodName = methodName;
	}

	public FunctionDefinition(Function f)
	{
		if (f == null)
		{
			throw new IllegalArgumentException("Method/Function is null"); //$NON-NLS-1$
		}

		Object scopeNameObj = f.get("_scopename_", f); //$NON-NLS-1$
		if (scopeNameObj == null || scopeNameObj.equals(Scriptable.NOT_FOUND))
		{
			Object formNameObj = f.getParentScope().get("_formname_", f.getParentScope()); //$NON-NLS-1$
			if (formNameObj == null || formNameObj.equals(Scriptable.NOT_FOUND))
			{
				this.contextName = ScriptVariable.SCOPES_DOT_PREFIX + ScriptVariable.GLOBAL_SCOPE;
			}
			else
			{
				this.contextName = formNameObj.toString();
			}
		}
		else
		{
			this.contextName = ScriptVariable.SCOPES_DOT_PREFIX + scopeNameObj.toString();
		}

		Object methodNameObj = f.get("_methodname_", f); //$NON-NLS-1$
		this.methodName = (methodNameObj == null || methodNameObj.equals(Scriptable.NOT_FOUND)) ? null : methodNameObj.toString();

		if (this.methodName == null)
		{
			throw new IllegalArgumentException("Method/Function is not a servoy method"); //$NON-NLS-1$
		}
	}

	/**
	 * Get the scope name of this FunctionDefinition when the context is defined for a global method.
	 * Otherwise returns null.
	 * @since 6.1
	 */
	public String getScopeName()
	{
		return contextName.startsWith(ScriptVariable.SCOPES_DOT_PREFIX) ? contextName.substring(ScriptVariable.SCOPES_DOT_PREFIX.length()) : null;
	}

	/**
	 * Get the context of this function definition.
	 * <p>The context name is either the form name or scopes.scopeName.
	 * @since 6.1
	 */
	public String getContextName()
	{
		return contextName;
	}

	/**
	 * Set the context of this function definition.
	 * <p>The context name is either the form name or scopes.scopeName.
	 * @since 6.1
	 */
	public void setContextName(String contextName)
	{
		this.contextName = contextName;
	}

	/**
	 * @deprecated see {@link #getContextName()}.
	 */
	@Deprecated
	public String getFormName()
	{
		return contextName.startsWith(ScriptVariable.SCOPES_DOT_PREFIX) ? null : contextName;
	}

	/**
	 * @deprecated see {@link #setContextName(String)}.
	 */
	@Deprecated
	public void setFormName(String formName)
	{
		this.contextName = formName == null ? ScriptVariable.SCOPES_DOT_PREFIX + ScriptVariable.GLOBAL_SCOPE : formName;
	}

	/**
	 * Get the method name of this function definition.
	 */
	public String getMethodName()
	{
		return methodName;
	}

	/**
	 * Set the method name of this function definition.
	 */
	public void setMethodName(String methodName)
	{
		this.methodName = methodName;
	}

	/**
	 * Get the method name plus scope.
	 */
	public String toMethodString()
	{
		return contextName + '.' + methodName;
	}

	public enum Exist
	{
		NO_SOLUTION, METHOD_NOT_FOUND, METHOD_FOUND, FORM_NOT_FOUND;
	}

	/**
	* Test if the given methodName or formName do exist. Will return one of the {@link Exist} enums.
	* @since 5.2
	*/
	public Exist exists(IClientPluginAccess access)
	{
		final Exist[] retVal = new Exist[] { Exist.METHOD_NOT_FOUND };
		if (access instanceof ClientPluginAccessProvider)
		{
			final IApplication application = ((ClientPluginAccessProvider)access).getApplication();
			application.invokeAndWait(new Runnable()
			{
				public void run()
				{
					if (application.getSolution() != null)
					{
						if (contextName.startsWith(ScriptVariable.SCOPES_DOT_PREFIX))
						{
							GlobalScope gs = application.getScriptEngine().getScopesScope().getGlobalScope(
								contextName.substring(ScriptVariable.SCOPES_DOT_PREFIX.length()));
							if (gs != null && gs.get(methodName) instanceof Function)
							{
								retVal[0] = Exist.METHOD_FOUND;
							}
						}
						else
						{
							FormController fp = ((FormManager)application.getFormManager()).leaseFormPanel(contextName);
							if (fp == null)
							{
								retVal[0] = Exist.FORM_NOT_FOUND;
							}
							else if (fp.getFormScope().get(methodName, fp.getFormScope()) instanceof Function)
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
			});
		}
		return retVal[0];
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
	 * method name. And exception that {@link IClientPluginAccess#executeMethod(String, String, Object[], boolean)} will throw will be catched and {@link IClientPluginAccess#handleException(String, Exception)}
	 * will be called with the Exception object. If you want the exception object call {@link IClientPluginAccess#executeMethod(String, String, Object[], boolean)} directly.
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
			return access.executeMethod(contextName, methodName, arguments, async);
		}
		catch (Exception e)
		{
			access.handleException(
				"Failed to execute the method of context " + contextName + " and name " + methodName + " on the solution " + access.getSolutionName(), e);
		}
		return null;
	}


	@Override
	public String toString()
	{
		return "Function(" + toMethodString() + ')'; //$NON-NLS-1$
	}
}
