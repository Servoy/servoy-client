/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.dltk.rhino.dbgp.DBGPDebugFrame;
import org.eclipse.dltk.rhino.dbgp.DBGPDebugger;
import org.sablo.eventthread.WebsocketSessionWindows;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IWindow;

import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IDebugNGClient;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.ValidatingDelegateDataServer;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.PluginScope;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.INGFormManager;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.NGFormManager;
import com.servoy.j2db.server.ngclient.NGRuntimeWindowManager;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.component.WebFormController;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.server.ngclient.scripting.WebServiceScriptable;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class DebugNGClient extends NGClient implements IDebugNGClient
{
	private final IDesignerCallback designerCallback;
	private Solution current;

	final class DebugNGFormMananger extends NGFormManager implements DebugUtils.DebugUpdateFormSupport
	{
		private DebugNGFormMananger(DebugNGClient app)
		{
			super(app);
		}

		public void updateForm(Form form)
		{
			boolean isNew = !possibleForms.containsKey(form.getName());
			boolean isDeleted = false;
			if (!isNew)
			{
				isDeleted = !((AbstractBase)form.getParent()).getAllObjectsAsList().contains(form);
			}
			updateForm(form, isDeleted);
		}

		private void updateForm(Form form, boolean isDeleted)
		{
			if (isDeleted)
			{
				IFormController tmp = getCachedFormController(form.getName());
				if (tmp != null) removeFormController((BasicFormController)tmp); // form was deleted in designer; remove it's controller from cached/already used forms
				possibleForms.remove(form.getName());
			}
		}
	}

	/**
	 * @param webSocketClientEndpoint
	 * @param designerCallback
	 */
	public DebugNGClient(INGClientWebsocketSession wsSession, IDesignerCallback designerCallback) throws Exception
	{
		super(wsSession);
		this.designerCallback = designerCallback;
		getWebsocketSession().registerServerService("developerService", new DeveloperServiceHandler(this));
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

	@Override
	public synchronized void shutDown(boolean force)
	{
		// shutdown can be called for an older client when opening a new debug client.
		// then nothing should be done for the current window which is already for the new client instance
		IWindow currentWindow = null;
		if (CurrentWindow.exists() && !CurrentWindow.get().getSession().getUuid().equals(getWebsocketSession().getUuid()))
		{
			currentWindow = CurrentWindow.set(null);
		}
		try
		{
			super.shutDown(force);
		}
		finally
		{
			CurrentWindow.set(currentWindow);
		}
	}

	@Override
	protected IBasicFormManager createFormManager()
	{
		return new DebugNGFormMananger(this);
	}

	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		RemoteDebugScriptEngine engine = new RemoteDebugScriptEngine(this);
		WebObjectSpecification[] serviceSpecifications = WebServiceSpecProvider.getSpecProviderState().getAllWebComponentSpecifications();
		PluginScope scope = (PluginScope)engine.getSolutionScope().get("plugins", engine.getSolutionScope());
		scope.setLocked(false);
		for (WebObjectSpecification serviceSpecification : serviceSpecifications)
		{
			if (serviceSpecification.getApiFunctions().size() != 0 || serviceSpecification.getAllPropertiesNames().size() != 0)
			{
				scope.put(serviceSpecification.getScriptingName(), scope, new WebServiceScriptable(this, serviceSpecification, engine.getSolutionScope()));
			}
		}
		scope.setLocked(true);
		if (designerCallback != null)
		{
			designerCallback.addScriptObjects(this, engine.getSolutionScope());
		}

		return engine;
	}

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

	@Override
	public void reportJSError(String message, Object detail)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), message, detail);
		super.reportJSError(message, detail);
	}

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
		DebugUtils.errorToDebugger(getScriptEngine(), s, t);
		super.reportJSWarning(s, t);
	}

	@Override
	public void reportJSInfo(String s)
	{
		DebugUtils.stdoutToDebugger(getScriptEngine(), "INFO: " + s);
		super.reportJSInfo(s);
	}

	@Override
	public void setCurrent(Solution current)
	{
		this.current = current;
		closeSolution(true, null);
		getWebsocketSession().sendRedirect("/solutions/" + current.getName() + "/index.html");
	}

	@Override
	public void loadSolution(String solutionName) throws RepositoryException
	{
		if (current == null || current.getName().equals(solutionName))
		{
			super.loadSolution(solutionName);
		}
		else if (getWebsocketSession() != null)
		{
			getWebsocketSession().sendRedirect(current != null ? "/solutions/" + current.getName() + "/index.html" : null);
		}
	}

	@Override
	public void refreshForI18NChange(boolean recreateForms)
	{
		if (isShutDown()) return;

		refreshI18NMessages();
		if (recreateForms)
		{
			recreateForms();
		}
	}

	@Override
	public void recreateForms()
	{
		INGFormManager fm = getFormManager();
		if (fm != null)
		{
			List<IFormController> cachedFormControllers = fm.getCachedFormControllers();
			refreshForms(cachedFormControllers, false);
		}
	}

	private void refreshForms(Collection<IFormController> forms, boolean forcePageReload)
	{
		boolean reload = forcePageReload;
		if (forms != null && forms.size() > 0)
		{
			reload = true;
			FormElementHelper.INSTANCE.reload();
			List<IFormController> cachedFormControllers = getFormManager().getCachedFormControllers();
			for (IFormController formController : cachedFormControllers)
			{
				if (formController.getFormUI() instanceof WebFormUI)
				{
					((WebFormUI)formController.getFormUI()).clearCachedFormElements();
				}
			}
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>(); // should we also use these?
			for (IFormController controller : forms)
			{
				boolean isVisible = controller.isFormVisible();
				if (isVisible) controller.notifyVisible(false, invokeLaterRunnables);
				if (controller.getFormModel() != null && !Utils.stringSafeEquals(controller.getDataSource(), controller.getFormModel().getDataSource()))
				{
					// for now we just destroy the form and recreate it with the other datasource;
					// TODO we just load the shared foundset for that datasource - can we improve this somehow so that the loaded foundset is closer to the current runtime situation of the form? (related tabs etc.)
					String name = controller.getName();
					controller.destroy();
					controller = getFormManager().leaseFormPanel(name);
					FoundSet foundset;
					try
					{
						foundset = (FoundSet)getFoundSetManager().getSharedFoundSet(controller.getDataSource());
						foundset.loadAllRecords();
						controller.loadRecords(foundset);
					}
					catch (ServoyException e)
					{
						Debug.error(e);
					}
				}
				else
				{
					((WebFormController)controller).initFormUI();
				}
				if (isVisible) controller.notifyVisible(true, invokeLaterRunnables);
			}
		}
		if (reload)
		{
			WebsocketSessionWindows allendpoints = new NGClientWebsocketSessionWindows(getWebsocketSession());
			allendpoints.executeAsyncServiceCall(getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE), "reload", null, null);
			try
			{
				allendpoints.flush();
			}
			catch (IOException e)
			{
				reportError("error sending changes to the client", e);
			}
		}
	}

	@Override
	public void refreshPersists(Collection<IPersist> changes)
	{
		if (isShutDown()) return;

		Set<IFormController>[] scopesAndFormsToReload = DebugUtils.getScopesAndFormsToReload(this, changes);

		for (IFormController controller : scopesAndFormsToReload[1])
		{
			if (controller.getForm() instanceof FlattenedForm)
			{
				FlattenedForm ff = (FlattenedForm)controller.getForm();
				ff.reload();
			}
		}

		boolean forcePageReload = false;
		if (scopesAndFormsToReload[1] == null || scopesAndFormsToReload[1].size() < 1)
		{
			for (IPersist persist : changes)
			{
				if (persist instanceof Media && PersistHelper.getOrderedStyleSheets(getFlattenedSolution()).contains(((Media)persist).getName()))
				{
					forcePageReload = true;
					break;
				}
			}
		}

		refreshForms(scopesAndFormsToReload[1], forcePageReload);

		for (IFormController controller : scopesAndFormsToReload[0])
		{
			controller.getFormScope().reload();
		}


	}

	/**
	 * @param form
	 */
	public void show(Form form)
	{
		// TODO Auto-generated method stub
	}

	@Override
	protected void showInfoPanel()
	{
		//ignore
	}

	IDesignerCallback getDesignerCallback()
	{
		return designerCallback;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.NGClient#onStartSubAction(java.lang.String, java.lang.String, org.sablo.specification.WebObjectApiDefinition)
	 */
	@Override
	public Pair<UUID, UUID> onStartSubAction(String serviceName, String functionName, WebObjectFunctionDefinition apiFunction, Object[] args)
	{

		Pair<UUID, UUID> result = super.onStartSubAction(serviceName, functionName, apiFunction, args);
		IExecutingEnviroment engine = getScriptEngine();
		if (engine instanceof RemoteDebugScriptEngine && ((RemoteDebugScriptEngine)engine).getDebugger() != null)
		{
			DBGPDebugger debugger = ((RemoteDebugScriptEngine)engine).getDebugger();
			DBGPDebugFrame stackFrame = debugger.getStackManager().getStackFrame(0);
			if (stackFrame instanceof ServoyDebugFrame)
			{
				ServoyDebugFrame servoyDebugFrame = (ServoyDebugFrame)stackFrame;
				servoyDebugFrame.onEnterSubAction(serviceName + "." + functionName, args);
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.NGClient#onStopSubAction(com.servoy.j2db.util.Pair)
	 */
	@Override
	public void onStopSubAction(Pair<UUID, UUID> perfId)
	{
		super.onStopSubAction(perfId);
		IExecutingEnviroment engine = getScriptEngine();
		if (engine instanceof RemoteDebugScriptEngine && ((RemoteDebugScriptEngine)engine).getDebugger() != null)
		{
			DBGPDebugger debugger = ((RemoteDebugScriptEngine)engine).getDebugger();
			DBGPDebugFrame stackFrame = debugger.getStackManager().getStackFrame(0);
			if (stackFrame instanceof ServoyDebugFrame)
			{
				ServoyDebugFrame servoyDebugFrame = (ServoyDebugFrame)stackFrame;
				servoyDebugFrame.onExitSubAction();
			}
		}
	}

	@Override
	protected int getSolutionTypeFilter()
	{
		return super.getSolutionTypeFilter() | SolutionMetaData.MODULE;
	}

	@Override
	public void errorToDebugger(String message, Object detail)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), message, detail);
	}
}
