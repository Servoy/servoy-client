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

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.dltk.rhino.dbgp.DBGPDebugger;
import org.mozilla.javascript.RhinoException;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.server.headlessclient.DummyMainContainer;
import com.servoy.j2db.server.headlessclient.SessionClient;
import com.servoy.j2db.server.headlessclient.WebFormManager;
import com.servoy.j2db.server.shared.IDebugHeadlessClient;
import com.servoy.j2db.util.Debug;

/**
 * Headless client when running from developer.
 * @author acostescu
 */
public class DebugHeadlessClient extends SessionClient implements IDebugHeadlessClient
{

	public static class DebugWebFormManager extends WebFormManager implements DebugUtils.DebugUpdateFormSupport
	{

		public DebugWebFormManager(IApplication app, IMainContainer mainp)
		{
			super(app, mainp);
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
				List<String> lst = new ArrayList<String>();
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
	public IClientPluginAccess createClientPluginAccess()
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
	protected IFormManager createFormManager()
	{
		DebugWebFormManager fm = new DebugWebFormManager(this, new DummyMainContainer());
		return fm;
	}

	@Override
	protected void loadSolution(SolutionMetaData solutionMeta) throws RepositoryException
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

	public void setCurrent(SolutionMetaData solutionMeta)
	{
		solution = solutionMeta;
		closeSolution(true, null);
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

		List[] scopesAndFormsToReload = DebugUtils.getScopesAndFormsToReload(this, changes);

		refreshI18NMessages();
		for (FormController controller : (List<FormController>)scopesAndFormsToReload[0])
		{
			if (controller.getForm() instanceof FlattenedForm)
			{
				FlattenedForm ff = (FlattenedForm)controller.getForm();
				ff.reload();
			}
			controller.getFormScope().reload();
		}

		if (scopesAndFormsToReload[1].size() > 0) ((WebFormManager)getFormManager()).reload(((List<FormController>)scopesAndFormsToReload[1]).toArray(new FormController[0]));
	}

	public void refreshForI18NChange()
	{
		List<FormController> cachedFormControllers = ((FormManager)getFormManager()).getCachedFormControllers();
		ArrayList<IPersist> formsToReload = new ArrayList<IPersist>();
		for (FormController fc : cachedFormControllers)
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
		if (msg == null)
		{
			msg = "<null>";
		}
		if (msg != null)
		{
			DBGPDebugger debugger = getDebugger();
			if (debugger != null)
			{
				debugger.outputStdOut(msg.toString() + "\n");
			}
			else
			{
				Debug.error("No debugger found, for msg report: " + msg);
			}
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
	 * @see com.servoy.j2db.smart.J2DBClient#reportError(java.awt.Component, java.lang.String, java.lang.Object)
	 */
	@Override
	public void reportError(Component parentComponent, String message, Object detail)
	{
		errorToDebugger(message, detail);
		super.reportError(parentComponent, message, detail);
	}

	/**
	 * @see com.servoy.j2db.ClientState#reportError(java.lang.String, java.lang.Object)
	 */
	@Override
	public void reportError(String msg, Object detail)
	{
		reportError(null, msg, detail);
	}

	/**
	 * @param message
	 * @param detail
	 */
	private void errorToDebugger(String message, Object detail)
	{
		DBGPDebugger debugger = getDebugger();
		if (debugger != null)
		{
			String msg = message;
			if (detail instanceof RhinoException)
			{
				RhinoException re = (RhinoException)detail;
				if (msg == null)
				{
					msg = re.getCause().getLocalizedMessage();
				}
				msg += "\n > " + re.getScriptStackTrace(); //$NON-NLS-1$
			}
			else if (detail instanceof Exception)
			{
				Object e = ((Exception)detail).getCause();
				if (e != null)
				{
					detail = e;
				}
				msg += "\n > " + detail.toString(); // complete stack?
			}
			else if (detail != null)
			{
				msg += "\n" + detail;
			}
			debugger.outputStdErr(msg.toString() + "\n"); //$NON-NLS-1$
		}
		else
		{
			Debug.error("No debugger found, for error report: " + message);
		}
	}


	/**
	 * @param form
	 */
	public void show(Form form)
	{
		this.form = form;
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
			((FormManager)getFormManager()).showFormInMainPanel(form.getName());
			form = null;
			changed = true;
		}
		return changed;
	}

	@Override
	public synchronized boolean setMainForm(String formName)
	{
		invokeAndWait(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.setMainForm(formName);
	}

	@Override
	public synchronized Object getDataProviderValue(String contextName, String dataprovider)
	{
		invokeAndWait(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.getDataProviderValue(contextName, dataprovider);
	}

	@Override
	public synchronized Object setDataProviderValue(String contextName, String dataprovider, Object value)
	{
		invokeAndWait(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.setDataProviderValue(contextName, dataprovider, value);
	}

	@Override
	public synchronized int setDataProviderValues(String contextName, HttpServletRequest request_data)
	{
		invokeAndWait(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.setDataProviderValues(contextName, request_data);
	}

	@Override
	public synchronized void saveData()
	{
		invokeAndWait(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		super.saveData();
	}

	@Override
	public synchronized Object executeMethod(String visibleFormName, String methodName, Object[] arguments) throws Exception
	{
		invokeAndWait(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.executeMethod(visibleFormName, methodName, arguments);
	}

	@Override
	public synchronized String getI18NMessage(String key, Object[] args)
	{
		invokeAndWait(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.getI18NMessage(key, args);
	}

	@Override
	public synchronized void setLocale(Locale l)
	{
		invokeAndWait(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		super.setLocale(l);
	}

	@Override
	public synchronized IDataSet getValueListItems(String contextName, String valuelistName)
	{
		invokeAndWait(checkForChangesRunnable); // this is an ISessionBean interface method that can be called from JSP - update things before proceeding
		return super.getValueListItems(contextName, valuelistName);
	}

}