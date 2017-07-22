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
package com.servoy.j2db.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.dltk.rhino.dbgp.DBGPDebugger;
import org.mozilla.javascript.RhinoException;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IFormManagerInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.server.headlessclient.DummyMainContainer;
import com.servoy.j2db.server.headlessclient.HeadlessClient;
import com.servoy.j2db.server.headlessclient.WebFormManager;
import com.servoy.j2db.server.shared.IDebugHeadlessClient;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.ServoyException;

/**
 * Headless client when running from developer.
 * @author acostescu
 */
public class DebugHeadlessClient extends HeadlessClient implements IDebugHeadlessClient
{
	//This is needed for mobile client launch with switch to service option .
	// switching to service in developer causes a required refresh in a separate thread triggered by activeSolutionChanged
	// , meanwhile after setActiveSolution is called the mobileClientDelegate opens the browser which causes a get to the service solution which is not fully loaded and debuggable
	public static Object activeSolutionRefreshLock = new Object();

	public static class DebugWebFormManager extends WebFormManager implements DebugUtils.DebugUpdateFormSupport
	{

		public DebugWebFormManager(IApplication app, IMainContainer mainp)
		{
			super(app, mainp);
		}

		@Override
		protected void makeSolutionSettings(Solution s)
		{
			IApplication app = getApplication();
			if (app instanceof IDebugWebClient) ((IDebugWebClient)app).onSolutionOpen();
			super.makeSolutionSettings(s);
		}

		public void updateForm(Form form)
		{
			boolean isNew = !possibleForms.containsValue(form);
			boolean isDeleted = false;
			if (!isNew)
			{
				isDeleted = !((AbstractBase)form.getParent()).getAllObjectsAsList().contains(form);
			}
			updateForm(form, isNew, isDeleted);
		}

		/**
		 * @param form
		 * @param isNew
		 * @param isDeleted
		 */
		private void updateForm(Form form, boolean isNew, boolean isDeleted)
		{
			if (isNew)
			{
				addForm(form, false);
			}
			else if (isDeleted)
			{
				Iterator<Entry<String, Form>> iterator = possibleForms.entrySet().iterator();
				while (iterator.hasNext())
				{
					Map.Entry<String, Form> entry = iterator.next();
					if (entry.getValue().equals(form))
					{
						iterator.remove();
					}
				}
			}
			else
			{
				// just changed
				if (possibleForms.get(form.getName()) == null)
				{
					// name change, first remove the form
					updateForm(form, false, true);
					// then add it back in
					updateForm(form, true, false);
				}
			}
		}
	}

	private final Runnable checkForChangesRunnable = new Runnable()
	{
		public void run()
		{
			checkForChanges();
		}
	};
	private SolutionMetaData solution;
	private final IDesignerCallback designerCallBack;

	public DebugHeadlessClient(ServletRequest req, String name, String pass, String method, Object[] methodArgs, SolutionMetaData solution,
		IDesignerCallback designerCallBack) throws Exception
	{
		super(req, name, pass, method, methodArgs, solution == null ? null : solution.getName());
		this.solution = solution;
		this.designerCallBack = designerCallBack;
	}


	@Override
	protected IClientPluginAccess createClientPluginAccess()
	{
		return new ClientPluginAccessProvider(this)
		{
			@Override
			public Object executeMethod(String context, String methodname, Object[] arguments, boolean async) throws Exception
			{
				checkForChanges();
				return super.executeMethod(context, methodname, arguments, async);
			}
		};
	}

	@Override
	protected IFormManagerInternal createFormManager()
	{
		DebugWebFormManager fm = new DebugWebFormManager(this, new DummyMainContainer(this));
		return fm;
	}

	@Override
	protected void loadSolution(SolutionMetaData solutionMeta) throws RepositoryException
	{
		synchronized (activeSolutionRefreshLock)
		{
			if (!isShutDown())
			{
				// ignore given always load the active.
				if (getSolution() != null)
				{
					closeSolution(true, null);
				}
				if (solution != null)
				{
					super.loadSolution(solution);
				}
			}
		}
	}

	@Override
	public void shutDown(boolean force)
	{
		synchronized (activeSolutionRefreshLock)
		{
			super.shutDown(force);
		}
	}

	@Override
	public boolean closeSolution(boolean force, Object[] args)
	{
		synchronized (activeSolutionRefreshLock)
		{
			return super.closeSolution(force, args);
		}
	}

	public void setCurrent(Solution s)
	{
		SolutionMetaData solutionMeta = (s == null) ? null : s.getSolutionMetaData();

		synchronized (activeSolutionRefreshLock)
		{
			solution = solutionMeta;
			if (isSolutionLoaded() && (solutionMeta != null && !getSolution().getName().equals(solutionMeta.getName()) || solutionMeta == null))
			{
				closeSolution(true, null);
			}
		}
	}

	private Form form;

	private final List<List<IPersist>> changesQueue = new ArrayList<List<IPersist>>();

	private boolean performRefresh()
	{
		boolean changed = changesQueue.size() > 0;
		while (changesQueue.size() > 0)
		{
			performRefresh(changesQueue.remove(0));
		}
		return changed;
	}

	private void performRefresh(List<IPersist> changes)
	{

		Set<IFormController>[] scopesAndFormsToReload = DebugUtils.getScopesAndFormsToReload(this, changes);

		refreshI18NMessages();
		for (IFormController controller : scopesAndFormsToReload[0])
		{
			if (controller.getForm() instanceof FlattenedForm)
			{
				FlattenedForm ff = (FlattenedForm)controller.getForm();
				ff.reload();
			}
			controller.getFormScope().reload();
		}

		if (scopesAndFormsToReload[1].size() > 0) ((WebFormManager)getFormManager()).reload((scopesAndFormsToReload[1]).toArray(new FormController[0]));
	}

	public void refreshForI18NChange(boolean recreateForms)
	{
		List<IFormController> cachedFormControllers = getFormManager().getCachedFormControllers();
		ArrayList<IPersist> formsToReload = new ArrayList<IPersist>();
		for (IFormController fc : cachedFormControllers)
			formsToReload.add(fc.getForm());
		refreshPersists(formsToReload);
	}

	/**
	 * @param changes
	 */
	public void refreshPersists(Collection<IPersist> changes)
	{
		changesQueue.add(new ArrayList<IPersist>(changes));
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.SessionClient#createScriptEngine()
	 */
	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		RemoteDebugScriptEngine engine = new RemoteDebugScriptEngine(this);

		if (designerCallBack != null)
		{
			designerCallBack.addScriptObjects(this, engine.getSolutionScope());
		}
		return engine;
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#output(java.lang.Object)
	 */
	@Override
	public void output(Object msg, int level)
	{
		super.output(msg, level);
		if (level == ILogLevel.WARNING || level == ILogLevel.ERROR)
		{
			errorToDebugger(msg.toString(), null);
		}
		else
		{
			stdoutToDebugger(msg);
		}
	}

	protected void stdoutToDebugger(Object message)
	{
		DBGPDebugger debugger = getDebugger();
		if (debugger != null)
		{
			debugger.outputStdOut((message == null ? "<null>" : message.toString()) + '\n');
		}
		else
		{
			Debug.error("No debugger found, for msg report: " + message);
		}
	}

	private DBGPDebugger getDebugger()
	{
		RemoteDebugScriptEngine rdse = (RemoteDebugScriptEngine)getScriptEngine();
		if (rdse == null) return null;
		return rdse.getDebugger();
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#reportJSError(java.lang.String, java.lang.Object)
	 */
	@Override
	public void reportJSError(String message, Object detail)
	{
		errorToDebugger(message, detail);
		super.reportJSError(message, detail);
	}

	/**
	 * @see com.servoy.j2db.ClientState#reportError(java.lang.String, java.lang.Object)
	 */
	@Override
	public void reportError(String message, Object detail)
	{
		errorToDebugger(message, detail);
		super.reportError(message, detail);
	}

	/**
	 * @param message
	 * @param detail
	 */
	public void errorToDebugger(String message, Object errorDetail)
	{
		Object detail = errorDetail;
		RemoteDebugScriptEngine engine = (RemoteDebugScriptEngine)getScriptEngine();
		if (engine != null)
		{
			DBGPDebugger debugger = engine.getDebugger();
			if (debugger != null)
			{
				RhinoException rhinoException = null;
				if (detail instanceof Exception)
				{
					Throwable exception = (Exception)detail;
					while (exception != null)
					{
						if (exception instanceof RhinoException)
						{
							rhinoException = (RhinoException)exception;
							break;
						}
						exception = exception.getCause();
					}
				}
				String msg = message;
				if (rhinoException != null)
				{
					if (msg == null)
					{
						msg = rhinoException.getLocalizedMessage();
					}
					else msg += '\n' + rhinoException.getLocalizedMessage();
					msg += '\n' + rhinoException.getScriptStackTrace();
				}
				else if (detail instanceof Exception)
				{
					Object e = ((Exception)detail).getCause();
					if (e != null)
					{
						detail = e;
					}
					msg += "\n > " + detail.toString(); // complete stack?
					if (detail instanceof ServoyException && ((ServoyException)detail).getScriptStackTrace() != null)
					{
						msg += '\n' + ((ServoyException)detail).getScriptStackTrace();
					}
				}
				else if (detail != null)
				{
					msg += "\n" + detail;
				}
				debugger.outputStdErr(msg.toString() + '\n');
			}
		}
	}

	/**
	 * @param f
	 */
	public void show(Form f)
	{
		this.form = f;
	}

	/**
	 * @param object
	 */
	public boolean checkForChanges()
	{
		boolean changed = false;
		if (getSolution() == null && solution != null)
		{
			try
			{
				loadSolution(solution);
				changed = true;
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		if (!changed)
		{
			changed = performRefresh();
		}
		if (getSolution() != null && form != null)
		{
			getFormManager().showFormInMainPanel(form.getName());
			form = null;
			changed = true;
		}
		return changed;
	}

	@Override
	public synchronized boolean setMainForm(String formName)
	{
		invokeAndWaitIfExecutionNotLocked(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.setMainForm(formName);
	}

	@Override
	public synchronized Object getDataProviderValue(String contextName, String dataprovider)
	{
		invokeAndWaitIfExecutionNotLocked(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.getDataProviderValue(contextName, dataprovider);
	}

	@Override
	public synchronized Object setDataProviderValue(String contextName, String dataprovider, Object value)
	{
		invokeAndWaitIfExecutionNotLocked(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.setDataProviderValue(contextName, dataprovider, value);
	}

	@Override
	public synchronized int setDataProviderValues(String contextName, HttpServletRequest request_data)
	{
		invokeAndWaitIfExecutionNotLocked(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.setDataProviderValues(contextName, request_data);
	}

	@Override
	public synchronized void saveData()
	{
		invokeAndWaitIfExecutionNotLocked(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		super.saveData();
	}

	@Override
	public synchronized Object executeMethod(String visibleFormName, String methodName, Object[] arguments) throws Exception
	{
		invokeAndWaitIfExecutionNotLocked(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.executeMethod(visibleFormName, methodName, arguments);
	}

	@Override
	public synchronized String getI18NMessage(String key, Object[] args)
	{
		invokeAndWaitIfExecutionNotLocked(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.getI18NMessage(key, args);
	}

	@Override
	public synchronized void setLocale(Locale l)
	{
		invokeAndWaitIfExecutionNotLocked(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		super.setLocale(l);
	}

	@Override
	public synchronized IDataSet getValueListItems(String contextName, String valuelistName)
	{
		invokeAndWaitIfExecutionNotLocked(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.getValueListItems(contextName, valuelistName);
	}

	private void invokeAndWaitIfExecutionNotLocked(Runnable r)
	{
		if (!isExecutionLocked()) super.invokeAndWait(r);
	}
}