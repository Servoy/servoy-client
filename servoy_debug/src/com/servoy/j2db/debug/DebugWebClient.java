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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.swing.SwingUtilities;

import org.apache.wicket.RestartResponseException;
import org.eclipse.dltk.rhino.dbgp.DBGPDebugger;
import org.mozilla.javascript.RhinoException;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.WebClient;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.server.headlessclient.WebFormManager;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * 
 */
@SuppressWarnings("nls")
public class DebugWebClient extends WebClient implements IDebugWebClient
{
	private final HttpSession session;
	private SolutionMetaData solution;
	private final List<Thread> dispatchThreads = new ArrayList<Thread>(3);

	public DebugWebClient(HttpServletRequest req, String name, String pass, String method, Object[] methodArgs, SolutionMetaData solution) throws Exception
	{
		super(req, name, pass, method, methodArgs, solution != null ? solution.getName() : "");
		this.solution = solution;
		this.session = req.getSession();
	}

	protected synchronized void addEventDispatchThread()
	{
		if (!dispatchThreads.contains(Thread.currentThread()))
		{
			dispatchThreads.add(Thread.currentThread());
		}
	}

	protected synchronized void removeEventDispatchThread()
	{
		dispatchThreads.remove(Thread.currentThread());
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.SessionClient#isEventDispatchThread()
	 */
	@Override
	public boolean isEventDispatchThread()
	{
		if (dispatchThreads.size() == 0 || SwingUtilities.isEventDispatchThread())
		{
			return super.isEventDispatchThread();
		}
		return dispatchThreads.contains(Thread.currentThread());
	}

	@Override
	protected IFormManager createFormManager()
	{
		return new DebugHeadlessClient.DebugWebFormManager(this, getMainPage());
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.WebClient#shutDown(boolean)
	 */
	@Override
	public void shutDown(boolean force)
	{
		super.shutDown(force);
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
			super.loadSolution(solution);
		}
	}

	public void setCurrent(SolutionMetaData sol)
	{
		solution = sol;
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

		List<FormController>[] scopesAndFormsToReload = DebugUtils.getScopesAndFormsToReload(this, changes);

		refreshI18NMessages();
		for (FormController controller : scopesAndFormsToReload[0])
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
		return new RemoteDebugScriptEngine(this);
	}

	/**
	 * @see com.servoy.j2db.smart.J2DBClient#output(java.lang.Object)
	 */
	@Override
	public void output(Object msg, int level)
	{
		Object realMsg = msg;
		super.output(realMsg, level);
		if (realMsg == null)
		{
			realMsg = "<null>";
		}

		if (realMsg != null)
		{
			DBGPDebugger debugger = getDebugger();
			if (debugger != null)
			{
				debugger.outputStdOut(realMsg.toString() + '\n');
			}
			else
			{
				Debug.error("No debugger found, for msg report: " + realMsg);
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
					msg += "\n > " + e.toString(); // complete stack? 
				}
				else
				{
					msg += "\n > " + detail.toString(); // complete stack? 
				}
			}
			else if (detail != null)
			{
				msg += "\n" + detail;
			}
			debugger.outputStdErr(msg.toString() + '\n');
		}
		else
		{
			Debug.error("No debugger found, for error report: " + message);
		}
	}


	/**
	 * @param form
	 */
	public void show(Form f)
	{
		this.form = f;
	}

	@Override
	public boolean isInDeveloper()
	{
		return true;
	}

	public boolean checkForChanges()
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
			((FormManager)getFormManager()).showFormInMainPanel(form.getName());
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
			if (checkForChanges())
			{
				MainPage page = (MainPage)((WebFormManager)getFormManager()).getMainContainer(null);
				throw new RestartResponseException(page);
			}
			synchronized (webClientSession)
			{
				executeEvents();
			}
		}
	}

	@Override
	public void onEndRequest(WebClientSession webClientSession)
	{
		removeEventDispatchThread();
	}
}
