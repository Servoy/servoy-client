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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.swing.SwingUtilities;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IFormManagerInternal;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.ValidatingDelegateDataServer;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.server.headlessclient.WebClient;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.server.headlessclient.WebFormManager;
import com.servoy.j2db.server.headlessclient.eventthread.IEventDispatcher;
import com.servoy.j2db.server.headlessclient.eventthread.WicketEventDispatcher;
import com.servoy.j2db.server.shared.WebCredentials;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ILogLevel;

/**
 * @author jcompagner
 *
 */
public class DebugWebClient extends WebClient implements IDebugWebClient
{
	private SolutionMetaData solution;
	private final List<Thread> dispatchThreads = new ArrayList<Thread>(3);
	private final IDesignerCallback designerCallBack;

	public DebugWebClient(HttpServletRequest req, WebCredentials credentials, String method, Object[] methodArgs, SolutionMetaData solution,
		IDesignerCallback designerCallBack) throws Exception
	{
		super(req, credentials, method, methodArgs, solution != null ? solution.getName() : "");
		this.solution = solution;
		this.designerCallBack = designerCallBack;
	}

	public synchronized void addEventDispatchThread()
	{
		if (!dispatchThreads.contains(Thread.currentThread()))
		{
			dispatchThreads.add(Thread.currentThread());
		}
	}

	public synchronized void removeEventDispatchThread()
	{
		dispatchThreads.remove(Thread.currentThread());
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.SessionClient#isEventDispatchThread()
	 */
	@Override
	public boolean isEventDispatchThread()
	{
		// this has to be in synch with invokelater
		if (dispatchThreads.size() == 0 || SwingUtilities.isEventDispatchThread() || isShutDown())
		{
			return super.isEventDispatchThread();
		}
		return dispatchThreads.contains(Thread.currentThread());
	}

	@Override
	protected IFormManagerInternal createFormManager()
	{
		return new DebugHeadlessClient.DebugWebFormManager(this, getMainPage());
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.WebClient#shutDown(boolean)
	 */
	@Override
	public void shutDown(boolean force)
	{
		boolean sessionExists = Session.exists() && (RequestCycle.get() != null);
		if (sessionExists) Session.unset(); // avoid session invalidating in super.shutDown - as the current session might be needed when DebugWC is restarted (by next DWC)
		super.shutDown(force);
		if (sessionExists) Session.get();

		// null pointers fix when switching between browsers in developer.
		if (force && session != null)
		{
			try
			{
				session.invalidate();
			}
			catch (Exception e)
			{
				// ignore
			}
		}
	}

	@Override
	protected void loadSolution(SolutionMetaData solutionMeta) throws RepositoryException
	{// set the dispatch thread to this one if not already set.
		addEventDispatchThread();
		// ignore given always load the active.
		if (getSolution() != null)
		{
			closeSolution(true, null);
		}
		if (solution != null)
		{
			// reset the preferred solution always to this solution.
			// debug client can't load another.
			preferredSolutionNameToLoadOnInit = solution.getName();
			super.loadSolution(solution);
		}
	}

	public void setCurrent(Solution sol)
	{
		solution = (sol == null) ? null : sol.getSolutionMetaData();
	}

	private Form form;

	private final List<List<IPersist>> changesQueue = Collections.synchronizedList(new ArrayList<List<IPersist>>());
	private final List<List<IFormController>> recreateUISet = Collections.synchronizedList(new ArrayList<List<IFormController>>());

	private boolean performRefresh()
	{
		boolean changed = changesQueue.size() > 0;
		while (changesQueue.size() > 0)
		{
			performRefresh(changesQueue.remove(0));
		}
		if (!changed) changed = recreateUISet.size() > 0;
		while (recreateUISet.size() > 0)
		{
			List<IFormController> lst = recreateUISet.remove(0);
			for (IFormController fc : lst)
			{
				fc.recreateUI();
			}
		}
		return changed;
	}

	private void performRefresh(List<IPersist> changes)
	{
		Set<IFormController>[] scopesAndFormsToReload = DebugUtils.getScopesAndFormsToReload(this, changes);

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
		refreshI18NMessages();

		if (recreateForms)
		{
			List<IFormController> cachedFormControllers = getFormManager().getCachedFormControllers();
			recreateUISet.add(cachedFormControllers);
		}
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.WebClient#createDispatcher()
	 */
	@Override
	protected IEventDispatcher createDispatcher()
	{
		return new WicketEventDispatcher(this)
		{
			@Override
			public void run()
			{
				// just add this thread to the dispatchers.
				addEventDispatchThread();
				super.run();
			}
		};
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
			DebugUtils.errorToDebugger(getScriptEngine(), msg.toString(), null);
		}
		else
		{
			DebugUtils.stdoutToDebugger(getScriptEngine(), msg);
		}
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#reportJSError(java.lang.String, java.lang.Object)
	 */
	@Override
	public void reportJSError(String message, Object detail)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), message, detail);
		super.reportJSError(message, detail);
	}

	/**
	 * @see com.servoy.j2db.ClientState#reportError(java.lang.String, java.lang.Object)
	 */
	@Override
	public void reportError(String message, Object detail)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), message, detail);
		super.reportError(message, detail);
	}

	@Override
	public void reportJSWarning(String s)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), s, null);
		super.reportJSWarning(s);
	}

	@Override
	public void reportJSWarning(String s, Throwable t)
	{
		errorToDebugger(s, t);
		super.reportJSWarning(s, t);
	}

	@Override
	public void reportJSInfo(String s)
	{
		DebugUtils.stdoutToDebugger(getScriptEngine(), "INFO: " + s);
		super.reportJSInfo(s);
	}

	public void show(Form f)
	{
		this.form = f;
	}

	public synchronized boolean checkForChanges()
	{
		if (getClientInfo() == null) return false;
		boolean changed = false;
		SolutionMetaData mainSolutionMetaData = getFlattenedSolution().getMainSolutionMetaData();
		if ((mainSolutionMetaData == null && solution != null) || (mainSolutionMetaData != null && !mainSolutionMetaData.getName().equals(solution.getName())))
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
	public void onBeginRequest(WebClientSession webClientSession)
	{
		if (getSolution() != null)
		{
			addEventDispatchThread();
			checkForChanges();
			synchronized (onBeginRequestLock)
			{
				executeEvents();
			}
		}
	}

	@Override
	public void onEndRequest(WebClientSession webClientSession)
	{
		super.onEndRequest(webClientSession);
		removeEventDispatchThread();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ClientState#createDataServer()
	 */
	@Override
	protected IDataServer createDataServer()
	{
		IDataServer dataServer = super.createDataServer();
		if (dataServer != null)
		{
			dataServer = new ProfileDataServer(new ValidatingDelegateDataServer(dataServer, this));
		}
		return dataServer;
	}

	private HashMap<Object, Object> changedProperties;
	private boolean wasLoginSolution;

	@Override
	public boolean putClientProperty(Object name, Object val)
	{
		if (name != null && changedProperties != null && !changedProperties.containsKey(name))
		{
			changedProperties.put(name, getClientProperty(name));
			if (getSolution() != null && getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION)
			{
				wasLoginSolution = true;
			}
		}

		return super.putClientProperty(name, val);
	}

	public void onSolutionOpen()
	{
		if (changedProperties == null)
		{
			changedProperties = new HashMap<Object, Object>();
		}
		else
		{
			if (!wasLoginSolution)
			{
				Iterator<Map.Entry<Object, Object>> changedPropertiesIte = changedProperties.entrySet().iterator();
				Map.Entry<Object, Object> changedEntry;
				while (changedPropertiesIte.hasNext())
				{
					changedEntry = changedPropertiesIte.next();
					super.putClientProperty(changedEntry.getKey(), changedEntry.getValue());
				}
				changedProperties.clear();
			}
			else
			{
				wasLoginSolution = false;
			}
		}
	}

	@Override
	protected int getSolutionTypeFilter()
	{
		return super.getSolutionTypeFilter();
	}

	/*
	 * @see com.servoy.j2db.IDebugClient#errorToDebugger(java.lang.String, java.lang.String)
	 */
	@Override
	public void errorToDebugger(String message, Object detail)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), message, detail);
	}
}
