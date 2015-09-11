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

import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;

/**
 * Interface to script executor
 *
 * @author jblok
 */
public interface IExecutingEnviroment
{
	// Keywords for toplevel scope
	public final static String TOPLEVEL_HISTORY = "history"; //$NON-NLS-1$
	public final static String TOPLEVEL_PLUGINS = "plugins"; //$NON-NLS-1$
	public final static String TOPLEVEL_APPLICATION = "application"; //$NON-NLS-1$
	public final static String TOPLEVEL_UTILS = "utils"; //$NON-NLS-1$
	public final static String TOPLEVEL_SECURITY = "security"; //$NON-NLS-1$
	public final static String TOPLEVEL_SOLUTION_MODIFIER = "solutionModel"; //$NON-NLS-1$
	public final static String TOPLEVEL_DATABASE_MANAGER = "databaseManager"; //$NON-NLS-1$
	public final static String TOPLEVEL_DATASOURCES = "datasources"; //$NON-NLS-1$
	public final static String TOPLEVEL_I18N = "i18n"; //$NON-NLS-1$
	public final static String TOPLEVEL_SERVOY_EXCEPTION = "ServoyException"; //$NON-NLS-1$
	public final static String TOPLEVEL_FORMS = "forms"; //$NON-NLS-1$
	public final static String TOPLEVEL_JSUNIT = "jsunit"; //$NON-NLS-1$ // IMPORTANT: if you change this, you MUST change it also in Ident.java
	public final static String TOPLEVEL_SCOPES = ScriptVariable.SCOPES;

	public final static String[] TOPLEVEL_KEYWORDS = { TOPLEVEL_HISTORY, TOPLEVEL_PLUGINS, TOPLEVEL_APPLICATION, TOPLEVEL_UTILS, //
	TOPLEVEL_SECURITY, TOPLEVEL_DATABASE_MANAGER, TOPLEVEL_I18N, TOPLEVEL_SERVOY_EXCEPTION, TOPLEVEL_FORMS, TOPLEVEL_JSUNIT, TOPLEVEL_SCOPES };

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL. Get the top level scope
	 *
	 * @exclude
	 */
	public SolutionScope getSolutionScope();

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL. Get the top level scope
	 *
	 * @exclude
	 */
	public ScopesScope getScopesScope();

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL. Get the table scope for calculations and to set State as prototype
	 *
	 * @exclude
	 */
	public Scriptable getTableScope(ITable table);

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL. Get the table scope for calculations and to set State as prototype
	 *
	 * @exclude
	 */
	public void registerScriptObjectReturnTypes(IReturnedTypesProvider scriptObject);

	public void registerScriptObjectReturnTypes(IReturnedTypesProvider scriptObject, IScriptableAddition scriptableAddition);

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @exclude
	 */
	public Object getSystemConstant(String name);

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @exclude
	 */
	public JSApplication getJSApplication();

	/**
	 * @return the solutionModifier
	 */
	public JSSolutionModel getSolutionModifier();

	/**
	 * Compile a javascript function definition source
	 */
	public Function compileFunction(IScriptProvider sp, Scriptable scope) throws Exception;

	/**
	 * Execute a former compiled a javascript function
	 *
	 * @param focusEvent
	 * @param throwException If true then it will throw the exception that is get from the method instead of calling application.handleException
	 */
	public Object executeFunction(Function f, Scriptable scope, Scriptable thisObject, Object[] args, boolean focusEvent, boolean throwException)
		throws Exception;

	/**
	 * Remove the engine registered scopes from the cache;
	 */
	public void destroy();

	/**
	 * Evaluate the string in a scope.
	 * @return evaluation result
	 */
	public Object eval(Scriptable scope, String eval_string);


}