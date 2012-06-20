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


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.ExitScriptException;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.DataException;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.solutionmodel.JSMethodWithArguments;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.ui.DataRendererOnRenderWrapper;
import com.servoy.j2db.ui.IScriptAccordionPanelMethods;
import com.servoy.j2db.ui.IScriptDataLabelMethods;
import com.servoy.j2db.ui.IScriptPortalComponentMethods;
import com.servoy.j2db.ui.IScriptScriptLabelMethods;
import com.servoy.j2db.ui.IScriptSplitPaneMethods;
import com.servoy.j2db.ui.IScriptTabPanelMethods;
import com.servoy.j2db.ui.RenderableWrapper;
import com.servoy.j2db.ui.runtime.IRuntimeButton;
import com.servoy.j2db.ui.runtime.IRuntimeCalendar;
import com.servoy.j2db.ui.runtime.IRuntimeCheck;
import com.servoy.j2db.ui.runtime.IRuntimeChecks;
import com.servoy.j2db.ui.runtime.IRuntimeCombobox;
import com.servoy.j2db.ui.runtime.IRuntimeDataButton;
import com.servoy.j2db.ui.runtime.IRuntimeHtmlArea;
import com.servoy.j2db.ui.runtime.IRuntimeImageMedia;
import com.servoy.j2db.ui.runtime.IRuntimeListBox;
import com.servoy.j2db.ui.runtime.IRuntimePassword;
import com.servoy.j2db.ui.runtime.IRuntimeRadio;
import com.servoy.j2db.ui.runtime.IRuntimeRadios;
import com.servoy.j2db.ui.runtime.IRuntimeRtfArea;
import com.servoy.j2db.ui.runtime.IRuntimeTextArea;
import com.servoy.j2db.ui.runtime.IRuntimeTextField;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Instances of this clas should only exist as long as the duration of a solution!
 * 
 * @author jblok, jcompagner
 */
public class ScriptEngine implements IScriptSupport
{
	private final static Pattern docStripper = Pattern.compile("\\A\\s*(?:/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/)?\\s*function\\s*(?:/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/)*\\s*([\\w\\$]+)"); //$NON-NLS-1$


	private final static ContextFactory.Listener contextListener = new ContextFactory.Listener()
	{
		@SuppressWarnings("nls")
		public void contextCreated(Context cx)
		{
			IServiceProvider sp = J2DBGlobals.getServiceProvider();
			if (sp instanceof IApplication)
			{
				IApplication application = (IApplication)sp;
				cx.setApplicationClassLoader(application.getBeanManager().getClassLoader());
				cx.setWrapFactory(new ServoyWrapFactory(application));
			}
		}

		public void contextReleased(Context cx)
		{
			cx.setApplicationClassLoader(null);
		}
	};
	static
	{
		ContextFactory.getGlobal().addListener(contextListener);
	}

	protected IApplication application;
	private HashMap<ITable, Scriptable> tableScopes;

	//scopes
	protected ServoyTopLevelScope toplevelScope;
	protected SolutionScope solutionScope;
	protected ScopesScope scopesScope;
	private PluginScope pluginScope;
	private CreationalPrototype creator;

	private final JSApplication jsApplication;
	private final JSUtils jsUtils;
	private final JSSecurity jssec;
	private final JSDatabaseManager jsdbm;
	private final JSI18N i18n;
	private final HistoryProvider historyProvider;
	private final JSSolutionModel solutionModifier;


	@SuppressWarnings("nls")
	public ScriptEngine(IApplication app)
	{
		application = app;

		jsApplication = new JSApplication(application);
		jsUtils = new JSUtils(application);
		jssec = new JSSecurity(application);
		jsdbm = new JSDatabaseManager(application);
		i18n = new JSI18N(application);
		historyProvider = new HistoryProvider(application);
		solutionModifier = new JSSolutionModel(application);

		Context cx = Context.enter();

		try
		{
			toplevelScope = new ServoyTopLevelScope(cx);

			ScriptObjectRegistry.getJavaMembers(UUID.class, toplevelScope);
			ScriptObjectRegistry.getJavaMembers(JSMethodWithArguments.class, toplevelScope);
			ScriptObjectRegistry.getJavaMembers(DbIdentValue.class, toplevelScope);

			toplevelScope.put(Record.JS_RECORD, toplevelScope, new InstanceOfScope(Record.JS_RECORD, Record.class));
			toplevelScope.put(FoundSet.JS_FOUNDSET, toplevelScope, new InstanceOfScope(FoundSet.JS_FOUNDSET, FoundSet.class));
			toplevelScope.put("JSFoundset", toplevelScope, new InstanceOfScope("JSFoundset", FoundSet.class));
			toplevelScope.put("JSDataSet", toplevelScope, new InstanceOfScope("JSDataSet", JSDataSet.class));
			toplevelScope.put("Form", toplevelScope, new InstanceOfScope("Form", FormScope.class));

			toplevelScope.put("RuntimeForm", toplevelScope, new InstanceOfScope("RuntimeForm", FormScope.class));
			toplevelScope.put("RuntimeButton", toplevelScope, new InstanceOfScope("RuntimeButton", IRuntimeButton.class));
			toplevelScope.put("RuntimeDataButton", toplevelScope, new InstanceOfScope("RuntimeDataButton", IRuntimeDataButton.class));
			toplevelScope.put("RuntimeLabel", toplevelScope, new InstanceOfScope("RuntimeLabel", IScriptScriptLabelMethods.class));
			toplevelScope.put("RuntimeDataLabel", toplevelScope, new InstanceOfScope("RuntimeDataLabel", IScriptDataLabelMethods.class));
			toplevelScope.put("RuntimePassword", toplevelScope, new InstanceOfScope("RuntimePassword", IRuntimePassword.class));
			toplevelScope.put("RuntimeHtmlArea", toplevelScope, new InstanceOfScope("RuntimeHtmlArea", IRuntimeHtmlArea.class));
			toplevelScope.put("RuntimeRtfArea", toplevelScope, new InstanceOfScope("RuntimeRtfArea", IRuntimeRtfArea.class));
			toplevelScope.put("RuntimeTextArea", toplevelScope, new InstanceOfScope("RuntimeTextArea", IRuntimeTextArea.class));
			toplevelScope.put("RuntimeChecks", toplevelScope, new InstanceOfScope("RuntimeChecks", IRuntimeChecks.class));
			toplevelScope.put("RuntimeCheck", toplevelScope, new InstanceOfScope("RuntimeCheck", IRuntimeCheck.class));
			toplevelScope.put("RuntimeRadios", toplevelScope, new InstanceOfScope("RuntimeRadios", IRuntimeRadios.class));
			toplevelScope.put("RuntimeRadio", toplevelScope, new InstanceOfScope("RuntimeRadio", IRuntimeRadio.class));
			toplevelScope.put("RuntimeComboBox", toplevelScope, new InstanceOfScope("RuntimeComboBox", IRuntimeCombobox.class));
			toplevelScope.put("RuntimeCalendar", toplevelScope, new InstanceOfScope("RuntimeCalendar", IRuntimeCalendar.class));
			toplevelScope.put("RuntimeMediaField", toplevelScope, new InstanceOfScope("RuntimeMediaField", IRuntimeImageMedia.class));
			toplevelScope.put("RuntimeTextField", toplevelScope, new InstanceOfScope("RuntimeTextField", IRuntimeTextField.class));
			toplevelScope.put("RuntimeTabPanel", toplevelScope, new InstanceOfScope("RuntimeTabPanel", IScriptTabPanelMethods.class));
			toplevelScope.put("RuntimeSplitPane", toplevelScope, new InstanceOfScope("RuntimeSplitPane", IScriptSplitPaneMethods.class));
			toplevelScope.put("RuntimePortal", toplevelScope, new InstanceOfScope("RuntimePortal", IScriptPortalComponentMethods.class));
			toplevelScope.put("RuntimeListBox", toplevelScope, new InstanceOfScope("RuntimeListBox", IRuntimeListBox.class));
			toplevelScope.put("RuntimeAccordionPanel", toplevelScope, new InstanceOfScope("RuntimeAccordionPanel", IScriptAccordionPanelMethods.class));

			ScriptObjectRegistry.getJavaMembers(FormController.JSForm.class, toplevelScope);
			toplevelScope.setPrototype(null);

			SolutionScope tmpSolutionScope = new SolutionScope(toplevelScope);


			InstanceJavaMembers ijm = new InstanceJavaMembers(toplevelScope, historyProvider.getClass());
			Scriptable history = new NativeJavaObject(toplevelScope, historyProvider, ijm);
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_HISTORY, toplevelScope, history);

			pluginScope = new PluginScope(toplevelScope, application);
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_PLUGINS, toplevelScope, pluginScope);

			//add application variable to toplevel scope
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_APPLICATION, toplevelScope, new NativeJavaObject(toplevelScope, jsApplication,
				new InstanceJavaMembers(toplevelScope, JSApplication.class)));
			registerScriptObjectReturnTypes(jsApplication);

			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_UTILS, toplevelScope, new NativeJavaObject(toplevelScope, jsUtils, new InstanceJavaMembers(
				toplevelScope, JSUtils.class)));

			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_SECURITY, toplevelScope, new NativeJavaObject(toplevelScope, jssec, new InstanceJavaMembers(
				toplevelScope, JSSecurity.class)));
			registerScriptObjectReturnTypes(jssec);
			toplevelScope.put(JSSecurity.class.getSimpleName(), toplevelScope, new NativeJavaClass(toplevelScope, JSSecurity.class));

			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER, toplevelScope, new NativeJavaObject(toplevelScope, solutionModifier,
				new InstanceJavaMembers(toplevelScope, JSSolutionModel.class)));
			registerScriptObjectClass(JSSolutionModel.class);
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER, toplevelScope, new NativeJavaObject(toplevelScope, jsdbm,
				new InstanceJavaMembers(toplevelScope, JSDatabaseManager.class)));
			registerScriptObjectClass(JSDatabaseManager.class);

			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_I18N, toplevelScope, new NativeJavaObject(toplevelScope, i18n, new InstanceJavaMembers(
				toplevelScope, JSI18N.class)));

			ScriptObjectRegistry.getJavaMembers(RepositoryException.class, toplevelScope);
			ScriptObjectRegistry.getJavaMembers(ApplicationException.class, toplevelScope);
			ScriptObjectRegistry.getJavaMembers(ServoyException.class, toplevelScope);
			ScriptObjectRegistry.getJavaMembers(DataException.class, toplevelScope);
			toplevelScope.put(IExecutingEnviroment.TOPLEVEL_SERVOY_EXCEPTION, toplevelScope, new NativeJavaClass(toplevelScope, ServoyException.class));
			registerScriptObjectClass(ServoyException.class);

			ScriptObjectRegistry.getJavaMembers(DataRendererOnRenderWrapper.class, toplevelScope);
			ScriptObjectRegistry.getJavaMembers(RenderableWrapper.class, toplevelScope);

			creator = new CreationalPrototype(tmpSolutionScope, application);
			creator.setPrototype(null);

			tmpSolutionScope.put(IExecutingEnviroment.TOPLEVEL_FORMS, tmpSolutionScope, creator);

			scopesScope = new ScopesScope(tmpSolutionScope, this, app);
			tmpSolutionScope.setScopesScope(scopesScope);

			solutionScope = tmpSolutionScope;
			toplevelScope.setSealReadOnly(true);
		}
		catch (Exception ex)
		{
			Debug.error("ScriptEngine init not completly successfully ", ex); //$NON-NLS-1$
		}
		finally
		{
			Context.exit();
		}
	}

	@SuppressWarnings("deprecation")
	private void registerScriptObjectClass(Class< ? > cls)
	{
		IScriptObject scriptObjectForClass = ScriptObjectRegistry.getScriptObjectForClass(cls);
		if (scriptObjectForClass != null)
		{
			registerScriptObjectReturnTypes(scriptObjectForClass);
		}
		else
		{
			if (IReturnedTypesProvider.class.isAssignableFrom(cls))
			{
				try
				{
					registerScriptObjectReturnTypes((IReturnedTypesProvider)cls.newInstance());
				}
				catch (Exception e)
				{
					// ignore
				}
			}
		}
	}

	/**
	 * @param scriptObject
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public void registerScriptObjectReturnTypes(IReturnedTypesProvider scriptObject, IScriptableAddition scriptableAddition)
	{
		if (scriptObject == null) return;
		Class< ? >[] allReturnedTypes = scriptObject.getAllReturnedTypes();
		if (allReturnedTypes == null) return;
		try
		{
			toplevelScope.setSealReadOnly(false);
			for (Class< ? > element : allReturnedTypes)
			{
				if (!(Scriptable.class.isAssignableFrom(element)))
				{
					ScriptObjectRegistry.getJavaMembers(element, toplevelScope);
				}
				if (IPrefixedConstantsObject.class.isAssignableFrom(element))
				{
					try
					{
						IPrefixedConstantsObject constants = (IPrefixedConstantsObject)element.newInstance();
						toplevelScope.put(constants.getPrefix(), toplevelScope, new NativeJavaClass(toplevelScope, element));
						if (scriptableAddition != null)
						{
							scriptableAddition.addVar(constants.getPrefix(), new NativeJavaClass(scriptableAddition, element));
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
				else if (IJavaScriptType.class.isAssignableFrom(element)) // constants or javascript types
				{
					try
					{
						toplevelScope.put(element.getSimpleName(), toplevelScope, new NativeJavaClass(toplevelScope, element));
						if (scriptableAddition != null)
						{
							scriptableAddition.addVar(element.getSimpleName(), new NativeJavaClass(scriptableAddition, element));
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
			}
		}
		finally
		{
			toplevelScope.setSealReadOnly(true);
		}
	}

	public void registerScriptObjectReturnTypes(IReturnedTypesProvider scriptObject)
	{
		registerScriptObjectReturnTypes(scriptObject, null);
	}

	public void destroy()
	{
		ScriptObjectRegistry.removeEntryFromCache(toplevelScope);

		jsApplication.destroy();
		scopesScope.destroy();
		jsdbm.destroy();
		jssec.destroy();
		jsUtils.destroy();
		i18n.destroy();
		historyProvider.destroy();
		pluginScope.destroy();
		creator.destroy();
		solutionModifier.destroy();

	}

	public Object getSOMObject(String name)
	{
		Object obj = toplevelScope.get(name, toplevelScope);
		if (obj instanceof Wrapper)
		{
			obj = ((Wrapper)obj).unwrap();
		}
		return obj;
	}

	public Object getSystemConstant(String name)
	{
		if ("serverURL".equals(name)) //$NON-NLS-1$
		{
			return application.getServerURL().toString();
		}
		return null;
	}

	public ScopesScope getScopesScope()
	{
		return scopesScope;
	}

	/**
	 * @return the toplevelScope
	 */
	public Scriptable getToplevelScope()
	{
		return toplevelScope;
	}

	public SolutionScope getSolutionScope()
	{
		return solutionScope;
	}

	/**
	 * @return the solutionModifier
	 */
	public JSSolutionModel getSolutionModifier()
	{
		return solutionModifier;
	}

	protected final Scriptable getExistingTableScrope(ITable table)
	{
		if (tableScopes != null)
		{
			return tableScopes.get(table);
		}
		return null;
	}

	public Scriptable getTableScope(final ITable table)
	{
		if (tableScopes == null) tableScopes = new HashMap<ITable, Scriptable>();
		Scriptable tableScope = null;
		synchronized (table)
		{
			tableScope = tableScopes.get(table);
			if (tableScope == null)
			{
				Context.enter();
				try
				{
					tableScope = new TableScope(solutionScope, this, table, application.getFlattenedSolution(), new ISupportScriptProviders()
					{
						public Iterator< ? extends IScriptProvider> getScriptMethods(boolean sort)
						{
							return application.getFlattenedSolution().getScriptCalculations(table, false);
						}

						public Iterator<ScriptVariable> getScriptVariables(boolean b)
						{
							return Collections.<ScriptVariable> emptyList().iterator();
						}

						public ScriptMethod getScriptMethod(int methodId)
						{
							return null; // is not used for calculations 
						}
					});
					tableScope.setPrototype(null);

					tableScopes.put(table, tableScope);
				}
				finally
				{
					Context.exit();
				}
			}
		}
		return tableScope;
	}

	@SuppressWarnings("nls")
	public Function compileFunction(IScriptProvider sp, Scriptable scope) throws Exception
	{
		Context cx = Context.enter();
		int iOp = cx.getOptimizationLevel();
		try
		{
			if (Utils.getAsBoolean(System.getProperty("servoy.disableScriptCompile", "false"))) //flag should only be used in rich client
			{
				cx.setOptimizationLevel(-1);
			}
			else
			{
				cx.setOptimizationLevel(9);
			}
			cx.setGeneratingSource(Boolean.getBoolean("servoy.generateJavascriptSource"));
			return compileScriptProvider(sp, scope, cx, sp.getDataProviderID());
		}
		catch (Exception e)
		{
			Debug.error("Compilation failed for method: " + sp.getDataProviderID());
			throw e;
		}
		finally
		{
			cx.setOptimizationLevel(iOp);
			Context.exit();
		}
	}

	/**
	 * @param sp
	 * @param scope
	 * @param cx
	 * @return
	 */
	@SuppressWarnings("nls")
	protected Function compileScriptProvider(IScriptProvider sp, Scriptable scope, Context cx, String sourceName)
	{
		String declaration = sp.getDeclaration();
		// for script calcs we change the name a bit so that references to itself 
		// dont return the calc function itself but still the value.
		if (sp instanceof ScriptCalculation)
		{
			declaration = docStripper.matcher(declaration).replaceFirst("function $1_");
		}
		else
		{
			declaration = docStripper.matcher(declaration).replaceFirst("function $1");
		}

		Function f = cx.compileFunction(scope, declaration, sourceName, sp.getLineNumberOffset(), null);
		if (!(sp instanceof ScriptCalculation))
		{
			if (sp.getScopeName() != null)
			{
				f.put("_scopename_", f, sp.getScopeName()); //$NON-NLS-1$				
			}
			f.put("_methodname_", f, sp.getDataProviderID()); //$NON-NLS-1$
			f.put(
				"_AllowToRunInFind_", f, Boolean.valueOf(sp.getDeclaration().indexOf("@AllowToRunInFind") != -1 || declaration.indexOf(".search") != -1 || declaration.indexOf("controller.loadAllRecords") != -1)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return f;
	}

	public Object executeFunction(Function f, Scriptable scope, Scriptable thisObject, Object[] args, boolean focusEvent, boolean throwException)
		throws Exception
	{
		if (!application.isSolutionLoaded())
		{
			return null;
		}

		//find function
		Object retValue = null;
		if (f != null)
		{
			String userUidBefore = null;
			if (Context.getCurrentContext() == null)
			{
				userUidBefore = application.getClientInfo().getUserUid();
			}
			Context cx = Context.enter();
			try
			{
				if (application instanceof ISmartClientApplication)
				{
					((ISmartClientApplication)application).setPaintTableImmediately(false);
				}
				// wrap objects when needed (to support typeof(arguments[0]) )
				Object[] wrappedArgs;
				if (args == null)
				{
					wrappedArgs = new Object[0];
				}
				else
				{
					wrappedArgs = new Object[args.length];
					for (int i = 0; i < args.length; i++)
					{
						if (args[i] != null)
						{
							wrappedArgs[i] = cx.getWrapFactory().wrap(cx, scope, args[i], args[i].getClass());
						}
					}
				}

				//run
				retValue = f.call(cx, scope, thisObject, wrappedArgs);
				if (retValue instanceof Wrapper)
				{
					retValue = ((Wrapper)retValue).unwrap();
				}
			}
			catch (Exception ex)
			{
				if (scope instanceof TableScope || throwException)
				{
					throw ex;//is calc report back via other way
				}
				else if (application.getSolution() != null)
				{
					application.handleException(null, ex);
				}
				else
				{
					Debug.trace("Solution already closed, ignoring exception", ex); //$NON-NLS-1$
				}
			}
			finally
			{
				if (application instanceof ISmartClientApplication)
				{
					((ISmartClientApplication)application).setPaintTableImmediately(true);
				}
				Context.exit();
			}
			if (Context.getCurrentContext() == null && !application.isShutDown())
			{
				if (application.isEventDispatchThread() && !(scope instanceof TableScope))
				{
					application.getFlattenedSolution().checkStateForms(application);
				}
				String userUidAfter = application.getClientInfo().getUserUid();
				if (!Utils.stringSafeEquals(userUidBefore, userUidAfter))
				{
					// user was authenticated or logged out
					application.handleClientUserUidChanged(userUidBefore, userUidAfter);
				}
			}
		}
		return retValue;
	}

	public Object eval(Scriptable scope, String eval_string)
	{
		Context cx = Context.enter();
		try
		{
			Object o = null;
			Function compileFunction = cx.compileFunction(scope, "function evalFunction(){}", "evalFunction", 0, null); //$NON-NLS-1$ //$NON-NLS-2$ 
			if (compileFunction instanceof NativeFunction)
			{
				o = cx.evaluateString(ScriptRuntime.createFunctionActivation((NativeFunction)compileFunction, scope, null), eval_string,
					"internal_anon", 1, null); //$NON-NLS-1$
			}
			else
			{
				o = cx.evaluateString(scope, eval_string, "internal_anon", 1, null); //$NON-NLS-1$
			}
			if (o instanceof Wrapper)
			{
				o = ((Wrapper)o).unwrap();
			}
			if (o == Scriptable.NOT_FOUND || o == Undefined.instance)
			{
				o = null;
			}
			return o;
		}
		catch (Exception ex)
		{
			if (ex instanceof ExitScriptException)
			{
				throw (ExitScriptException)ex;
			}
			else if (ex.getCause() instanceof ExitScriptException)
			{
				throw (ExitScriptException)ex.getCause();
			}
			else if (ex instanceof JavaScriptException && ((JavaScriptException)ex).getValue() instanceof ExitScriptException)
			{
				throw (ExitScriptException)((JavaScriptException)ex).getValue();
			}
			else if (application.getSolution() != null)
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.evalString") + eval_string + "'", ex); //$NON-NLS-1$//$NON-NLS-2$
			}
			else
			{
				Debug.trace("Solution already closed, ignoring exception", ex); //$NON-NLS-1$
			}
		}
		finally
		{
			Context.exit();
		}
		return null;
	}

	public void stopExecutingCurrentFunction()
	{
		//TODO:see if this works?
//		Context cx = Context.enter();
//		try
//		{
//			DebuggableEngine de = cx.getDebuggableEngine();
//			if(de != null) de.setBreakNextLine(true);
//		}
//		finally
//		{
//			Context.exit();
//		}
	}

	public void flushCachedScopes()
	{
		reload();
	}

	public void setLastKeyModifiers(int m)
	{
		((JSApplication)getSOMObject("application")).setLastKeyModifiers(m); //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.scripting.IExecutingEnviroment#reload()
	 */
	public void reload()
	{
		scopesScope = new ScopesScope(solutionScope, this, application);
		solutionScope.setScopesScope(scopesScope);
		scopesScope.createScopes();
	}

	public boolean isAWTSuspendedRunningScript()
	{
		return false;
	}

	public JSApplication getJSApplication()
	{
		return jsApplication;
	}

	/**
	 * @author jcompagner
	 *
	 */
	private final class ServoyTopLevelScope extends ImporterTopLevel
	{
		private final Set<String> readonlyProperties = new HashSet<String>();
		// if false then the properties are recorded as readonly
		// if true then the properties are checked as readonly
		// default it starts in recording mode
		private boolean sealedReadOnly = false;

		/**
		 * @param cx
		 */
		private ServoyTopLevelScope(Context cx)
		{
			super(cx);
		}

		public void setSealReadOnly(boolean sealed)
		{
			this.sealedReadOnly = sealed;
		}

		@SuppressWarnings("nls")
		@Override
		public void put(String name, Scriptable start, Object value)
		{
			// must test for null, because put is already called by the super class.
			if (readonlyProperties != null)
			{
				if (!sealedReadOnly)
				{
					readonlyProperties.add(name);
				}
				else if (readonlyProperties.contains(name))
				{
					throw new RuntimeException("Property " + name + " is readonly");
				}
			}
			super.put(name, start, value);
		}
	}

}
