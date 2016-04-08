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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.swing.SwingUtilities;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.WebSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.DebugClientType;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugClient;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.debug.extensions.IDebugClientPovider;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.server.headlessclient.EmptyRequest;
import com.servoy.j2db.server.headlessclient.SessionClient;
import com.servoy.j2db.server.headlessclient.WebClient;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.server.headlessclient.WebClientsApplication;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IFlattenedSolutionDebugListener;
import com.servoy.j2db.server.shared.WebCredentials;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDeveloperURLStreamHandler;
import com.servoy.j2db.util.Utils;

public class DebugClientHandler implements IDebugClientHandler, IDesignerCallback
{

	public static final String SUPPORTED_DEBUG_CLIENT_TYPE_ID = "debugClientTypeID";
	public static final String DEBUG_CLIENT_PROVIDER_EXTENSION_POINT_ID = "servoy_debug.debugClientProvider";


	//This is needed for mobile client launch with switch to service option .
	// switching to service in developer causes a required refresh in a separate thread triggered by activeSolutionChanged
	// , meanwhile after setActiveSolution is called the mobileClientDelegate opens the browser which causes a get to the service solution which is not fully loaded and debuggable
	public static Lock activeSolutionRefreshLock = new ReentrantLock();

	private volatile DebugHeadlessClient debugHeadlessClient;
	private volatile DebugAuthenticator debugAuthenticator;
	private volatile DebugWebClient debugWebClient;
	@SuppressWarnings("unused")
	private volatile DebugNGClient debugNGClient;
	private volatile DebugJ2DBClient debugJ2DBClient;
	private volatile Solution currentSolution;

	private IDesignerCallback designerCallback;
	private final Map<DebugClientType< ? >, IDebugClient> customDebugClients = new HashMap<DebugClientType< ? >, IDebugClient>(); // for example JSUnit smart debug client

	/**
	 * @param designerCallback the designerCallback to set
	 */
	public void setDesignerCallback(IDesignerCallback designerCallback)
	{
		this.designerCallback = designerCallback;
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null)
		{
			SolutionScope solutionScope = debugJ2DBClient.getScriptEngine().getSolutionScope();
			designerCallback.addScriptObjects(debugJ2DBClient, solutionScope);
		}
		if (debugWebClient != null && debugWebClient.getSolution() != null)
		{
			SolutionScope solutionScope = debugWebClient.getScriptEngine().getSolutionScope();
			designerCallback.addScriptObjects(debugWebClient, solutionScope);
		}
		if (debugNGClient != null && debugNGClient.getSolution() != null)
		{
			SolutionScope solutionScope = debugNGClient.getScriptEngine().getSolutionScope();
			designerCallback.addScriptObjects(debugNGClient, solutionScope);
		}
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null)
		{
			SolutionScope solutionScope = debugHeadlessClient.getScriptEngine().getSolutionScope();
			designerCallback.addScriptObjects(debugHeadlessClient, solutionScope);
		}
		for (IDebugClient c : customDebugClients.values())
		{
			if (c.getSolution() != null)
			{
				SolutionScope solutionScope = c.getScriptEngine().getSolutionScope();
				designerCallback.addScriptObjects(c, solutionScope);
			}
		}
	}

	public void addScriptObjects(IDebugClient client, Scriptable scope)
	{
		if (designerCallback != null) designerCallback.addScriptObjects(client, scope);

	}

	public List<IDebugClient> getActiveDebugClients()
	{
		ArrayList<IDebugClient> lst = new ArrayList<IDebugClient>();
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null) lst.add(debugJ2DBClient);
		if (debugWebClient != null && debugWebClient.getSolution() != null) lst.add(debugWebClient);
		if (debugNGClient != null && debugNGClient.getSolution() != null) lst.add(debugNGClient);
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null) lst.add(debugHeadlessClient);
		for (IDebugClient c : customDebugClients.values())
		{
			if (c.getSolution() != null) lst.add(c);
		}
		return lst;
	}


	public void refreshDebugClientsI18N(boolean recreateForms)
	{
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null) debugJ2DBClient.refreshForI18NChange(recreateForms);
		if (debugWebClient != null && debugWebClient.getSolution() != null) debugWebClient.refreshForI18NChange(recreateForms);
		if (debugNGClient != null && debugNGClient.getSolution() != null) debugNGClient.refreshForI18NChange(recreateForms);
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null) debugHeadlessClient.refreshForI18NChange(recreateForms);
		if (debugAuthenticator != null && debugAuthenticator.getSolution() != null) debugAuthenticator.refreshForI18NChange(recreateForms);
		for (IDebugClient c : customDebugClients.values())
		{
			if (c.getSolution() != null) c.refreshForI18NChange(recreateForms);
		}
	}

	/**
	 * @param changes
	 */
	public void refreshDebugClients(Collection<IPersist> changes)
	{
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null) debugJ2DBClient.refreshPersists(changes);
		if (debugWebClient != null && debugWebClient.getSolution() != null) debugWebClient.refreshPersists(changes);
		if (debugNGClient != null && debugNGClient.getSolution() != null) debugNGClient.refreshPersists(changes);
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null) debugHeadlessClient.refreshPersists(changes);
		if (debugAuthenticator != null && debugAuthenticator.getSolution() != null) debugAuthenticator.refreshPersists(changes);
		for (IDebugClient c : customDebugClients.values())
		{
			if (c.getSolution() != null) c.refreshPersists(changes);
		}
	}

	public void refreshDebugClients(ITable table)
	{
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null)
		{
			String dataSource = debugJ2DBClient.getFoundSetManager().getDataSource(table);
			((FoundSetManager)debugJ2DBClient.getFoundSetManager()).flushSQLSheet(dataSource);
		}
		if (debugWebClient != null && debugWebClient.getSolution() != null)
		{
			String dataSource = debugWebClient.getFoundSetManager().getDataSource(table);
			((FoundSetManager)debugWebClient.getFoundSetManager()).flushSQLSheet(dataSource);
		}
		if (debugNGClient != null && debugNGClient.getSolution() != null)
		{
			String dataSource = debugNGClient.getFoundSetManager().getDataSource(table);
			((FoundSetManager)debugNGClient.getFoundSetManager()).flushSQLSheet(dataSource);
		}
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null)
		{
			String dataSource = debugHeadlessClient.getFoundSetManager().getDataSource(table);
			((FoundSetManager)debugHeadlessClient.getFoundSetManager()).flushSQLSheet(dataSource);
		}
		if (debugAuthenticator != null && debugAuthenticator.getSolution() != null)
		{
			String dataSource = debugAuthenticator.getFoundSetManager().getDataSource(table);
			((FoundSetManager)debugAuthenticator.getFoundSetManager()).flushSQLSheet(dataSource);
		}

		for (IDebugClient c : customDebugClients.values())
		{
			if (c.getSolution() != null)
			{
				String dataSource = c.getFoundSetManager().getDataSource(table);
				((FoundSetManager)c.getFoundSetManager()).flushSQLSheet(dataSource);
			}
		}
	}

	public IApplication getDebugReadyClient()
	{
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null && RemoteDebugScriptEngine.isConnected(0))
		{
			return debugJ2DBClient;
		}
		else if (getDebugWebClient() != null && getDebugWebClient().getSolution() != null && RemoteDebugScriptEngine.isConnected(0))
		{
			return getDebugWebClient();
		}
		else if (getDebugHeadlessClient() != null && getDebugHeadlessClient().getSolution() != null && RemoteDebugScriptEngine.isConnected(0))
		{
			return getDebugHeadlessClient();
		}
		else if (getDebugNGClient() != null && getDebugNGClient().getSolution() != null && RemoteDebugScriptEngine.isConnected(0))
		{
			return getDebugNGClient();
		}
		else
		{
			for (IDebugClient c : customDebugClients.values())
			{
				if (c.getSolution() != null && RemoteDebugScriptEngine.isConnected(0)) return c;
			}
		}
		return null;
	}

	/**
	 * @param persist
	 * @param elementName
	 */
	public void executeMethod(final ISupportChilds persist, final String scopeName, final String methodname)
	{
		final IApplication serviceProvider = getDebugReadyClient();
		if (serviceProvider != null)
		{
			final Runnable run = new Runnable()
			{
				public void run()
				{
					if (persist instanceof Solution)
					{
						try
						{
							serviceProvider.getScriptEngine().getScopesScope().executeGlobalFunction(scopeName, methodname, null, false, false);
						}
						catch (Exception e)
						{
							Debug.log(e);
						}
					}
					else if (persist instanceof Form)
					{
						try
						{
							IFormController fp = serviceProvider.getFormManager().leaseFormPanel(((Form)persist).getName());
							if (fp != null)
							{
								fp.initForJSUsage();
								fp.setView(fp.getView());
								fp.executeOnLoadMethod();
								fp.executeFunction(methodname, null, false, null, false, null);
							}
						}
						catch (Exception e)
						{
							Debug.log(e);
						}
					}
				}
			};
			if (serviceProvider == getDebugHeadlessClient() || serviceProvider == getDebugAuthenticator())
			{
				ApplicationServerRegistry.get().getExecutor().execute(new Runnable()
				{
					public void run()
					{
						serviceProvider.invokeLater(run);
					}
				});
			}
			else if (serviceProvider == getDebugWebClient())
			{
				ApplicationServerRegistry.get().getExecutor().execute(new Runnable()
				{
					public void run()
					{
						RequestCycle rc = null;
						try
						{
							//fake a request as much as possible
							WebClientsApplication fakeApplication = ((WebClient)serviceProvider).getFakeApplication();
							Application.set(fakeApplication);
							rc = new WebRequestCycle(fakeApplication, new EmptyRequest(), new WebResponse());
							serviceProvider.invokeAndWait(run);
						}
						finally
						{
							Application.unset();
							rc.detach();
						}
					}
				});
			}
			else
			{
				serviceProvider.invokeLater(run);
			}
		}
	}

	/**
	 * @param solution
	 */
	public void reloadDebugSolution(Solution solution)
	{
		currentSolution = solution;
		if (debugJ2DBClient != null)
		{
			debugJ2DBClient.setCurrent(solution);
		}
		if (debugWebClient != null)
		{
			debugWebClient.setCurrent(solution);
		}
		if (debugNGClient != null)
		{
			debugNGClient.setCurrent(solution);
		}
		if (debugHeadlessClient != null)
		{
			debugHeadlessClient.setCurrent(solution);
		}
		for (IDebugClient c : customDebugClients.values())
		{
			c.setCurrent(solution);
		}
	}

	public void reloadDebugSolutionSecurity()
	{
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null && debugJ2DBClient.getFlattenedSolution() != null)
		{
			try
			{
				debugJ2DBClient.loadSecuritySettings(debugJ2DBClient.getFlattenedSolution());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		if (debugWebClient != null && debugWebClient.getSolution() != null && debugWebClient.getFlattenedSolution() != null)
		{
			try
			{
				debugWebClient.loadSecuritySettings(debugWebClient.getFlattenedSolution());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		if (debugNGClient != null && debugNGClient.getSolution() != null && debugNGClient.getFlattenedSolution() != null)
		{
			try
			{
				debugNGClient.loadSecuritySettings(debugNGClient.getFlattenedSolution());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null && debugHeadlessClient.getFlattenedSolution() != null)
		{
			try
			{
				debugHeadlessClient.loadSecuritySettings(debugHeadlessClient.getFlattenedSolution());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		for (IDebugClient c : customDebugClients.values())
		{
			if (c.getSolution() != null && c.getFlattenedSolution() != null)
			{
				try
				{
					c.loadSecuritySettings(c.getFlattenedSolution());
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
	}

	/**
	 * @param form
	 */
	public void showInDebugClients(Form form)
	{
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null && debugJ2DBClient.getMainApplicationFrame().isVisible())
		{
			debugJ2DBClient.show(form);
		}
		if (debugWebClient != null && debugWebClient.getSolution() != null)
		{
			debugWebClient.show(form);
		}
		if (debugNGClient != null && debugNGClient.getSolution() != null)
		{
			debugNGClient.show(form);
		}
		// this has no effect for headless client nor any sense for jsunit client
	}

	/**
	 * @return
	 */
	public DebugJ2DBClient getDebugSmartClient()
	{
		if (debugJ2DBClient == null)
		{
			debugJ2DBClient = createDebugSmartClient();
		}
		return debugJ2DBClient;
	}

	protected DebugJ2DBClient createDebugSmartClient()
	{
		return createDebugClient(new IDebugClientPovider<DebugJ2DBClient>()
		{

			@Override
			public DebugJ2DBClient createDebugClient(DebugClientHandler debugClientHandler)
			{
				return new DebugJ2DBClient(true, debugClientHandler);
			}

			@Override
			public boolean isSwingClient()
			{
				return true;
			}
		});
	}

	public <T extends IDebugClient> T getDebugClient(DebugClientType<T> type)
	{
		T client = (T)customDebugClients.get(type);
		if (client == null)
		{
			client = createDebugClient(type);
			if (client != null) customDebugClients.put(type, client);
		}
		return client;
	}

	protected <T extends IDebugClient> T createDebugClient(DebugClientType<T> type)
	{
		// find the correct debug client using the providers extension point
		IDebugClientPovider<T> provider;

		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(DEBUG_CLIENT_PROVIDER_EXTENSION_POINT_ID);
		IExtension[] extensions = ep.getExtensions();

		if (extensions != null && extensions.length > 0)
		{
			for (IExtension extension : extensions)
			{
				IConfigurationElement[] ces = extension.getConfigurationElements();
				if (ces != null)
				{
					for (IConfigurationElement ce : ces)
					{
						if (type.getDebugClientTypeID().equals(ce.getAttribute(SUPPORTED_DEBUG_CLIENT_TYPE_ID)))
						{
							try
							{
								provider = (IDebugClientPovider<T>)ce.createExecutableExtension("class");
								T debugClient = createDebugClient(provider);
								if (debugClient != null) return debugClient;
							}
							catch (CoreException e)
							{
								Debug.error(e);
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * @return
	 */
	public IDebugWebClient getDebugWebClient()
	{
		return debugWebClient;
	}

	@Override
	public IDebugClient getDebugNGClient()
	{
		return debugNGClient;
	}

	public DebugHeadlessClient getDebugHeadlessClient()
	{
		return debugHeadlessClient;
	}

	public DebugAuthenticator getDebugAuthenticator()
	{
		return debugAuthenticator;
	}

	private <T extends IDebugClient> T createDebugClient(final IDebugClientPovider<T> debugClientprovider)
	{
		if (!ApplicationServerRegistry.waitForApplicationServerStarted())
		{
			return null;
		}
		final IDebugClient[] client = new IDebugClient[1];
		try
		{
			final Runnable run = new Runnable()
			{
				public void run()
				{
					try
					{
						synchronized (ApplicationServerRegistry.get())
						{
							if (client[0] == null)
							{
								try
								{
									if (debugClientprovider.isSwingClient() && Utils.isAppleMacOS())
									{
										// added a small sleep time to fix blank screens on macs for the smart debug client.
										// this occurred when starting the smart client triggers another action (like switching
										// perspective) that keeps the SWT thread busy while creating the JFrame for the client
										Thread.sleep(3000);
									}
									// wait for servoy model to be initialised
									// Note: this is needed on the mac! Without it the
									// debug smart client sometimes does not paint in serclipse (The swt main
									// thread must not be busy when the debug smart client is created).
									modelInitialised.await(30, TimeUnit.SECONDS);
								}
								catch (InterruptedException e)
								{
									Debug.log(e);
								}

								// Do not call J2DBGlobals.setSingletonServiceProvider here now, it will be set temporary when the unit tests are run
								client[0] = debugClientprovider.createDebugClient(DebugClientHandler.this);
								client[0].setCurrent(currentSolution);
							}
						}
					}
					catch (Exception e)
					{
						Debug.error("Cannot create a DebugClient.", e);
					}
				}
			};
			if (!debugClientprovider.isSwingClient() || SwingUtilities.isEventDispatchThread())
			{
				run.run();
			}
			else
			{ //https://bugs.eclipse.org/bugs/show_bug.cgi?id=372951#c7
				if (Utils.isAppleMacOS())
				{
					DebugUtils.invokeAndWaitWhileDispatchingOnSWT(run);
				}
				else
				// non OSX
				{
					SwingUtilities.invokeAndWait(run);
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return (T)client[0];

	}

	/**
	 * @param webClientSession
	 * @param req
	 * @param userName
	 * @param password
	 * @param method
	 * @param objects
	 * @return
	 * @throws Exception
	 */
	public synchronized WebClient createDebugWebClient(WebSession webClientSession, HttpServletRequest req, WebCredentials credentials, String method,
		Object[] objects) throws Exception
	{
		if (!(webClientSession instanceof WebClientSession))
		{
			throw new IllegalArgumentException("expecting WebClientSession for debug web client: " + webClientSession.getClass().getName()); // nullpointer when webClientSession is null
		}
		IFlattenedSolutionDebugListener debugListener = null;
		if (debugWebClient != null && debugWebClient.getSolution() != null)
		{
			debugWebClient.shutDown(true);
		}
		if (debugWebClient != null && debugWebClient.getFlattenedSolution() != null)
		{
			debugListener = debugWebClient.getFlattenedSolution().getDebugListener();
		}
		debugWebClient = new DebugWebClient(req, credentials, method, objects, (currentSolution == null) ? null : currentSolution.getSolutionMetaData(),
			designerCallback);
		if (debugListener != null && debugWebClient.getFlattenedSolution() != null) debugWebClient.getFlattenedSolution().registerDebugListener(debugListener);
		return debugWebClient;
	}

	public synchronized IDebugClient createDebugNGClient(Object wsSession) throws Exception
	{
		IFlattenedSolutionDebugListener debugListener = null;
		if (debugNGClient != null && debugNGClient.getFlattenedSolution() != null)
		{
			debugListener = debugNGClient.getFlattenedSolution().getDebugListener();
		}
		if (debugNGClient != null && !debugNGClient.isShutDown())
		{
			debugNGClient.shutDown(true);
		}
		debugNGClient = new DebugNGClient((INGClientWebsocketSession)wsSession, designerCallback);
		if (debugListener != null && debugNGClient.getFlattenedSolution() != null) debugNGClient.getFlattenedSolution().registerDebugListener(debugListener);
		return debugNGClient;
	}

	public synchronized SessionClient createDebugHeadlessClient(ServletRequest req, String userName, String password, String method, Object[] objects,
		String preferedSolution) throws Exception
	{
		if (debugHeadlessClient != null && !debugHeadlessClient.isShutDown())
		{
			debugHeadlessClient.shutDown(true);
		}
		SolutionMetaData solutionMetaData = (currentSolution == null) ? null : currentSolution.getSolutionMetaData();
		if (preferedSolution != null && solutionMetaData != null && !preferedSolution.equals(solutionMetaData.getName()))
		{
			Map<String, Solution> modules = new HashMap<String, Solution>();
			currentSolution.getReferencedModulesRecursive(modules);
			if (modules.containsKey(preferedSolution))
			{
				solutionMetaData = (SolutionMetaData)ApplicationServerRegistry.get().getLocalRepository().getRootObjectMetaData(preferedSolution,
					IRepository.SOLUTIONS);
			}
		}
		debugHeadlessClient = new DebugHeadlessClient(req, userName, password, method, objects, solutionMetaData, designerCallback)
		{
			@Override
			public void shutDown(boolean force)
			{
				super.shutDown(force);
				debugHeadlessClient = null;
			}
		};
		testAndStartDebugger();
		return debugHeadlessClient;
	}

	public synchronized SessionClient createDebugAuthenticator(String authenticatorName, String method, Object[] objects) throws Exception
	{
		if (debugAuthenticator != null && debugAuthenticator.getSolution() != null)
		{
			debugAuthenticator.shutDown(true);
		}
		RootObjectMetaData rootObjectMetaData = ApplicationServerRegistry.get().getLocalRepository().getRootObjectMetaData(authenticatorName,
			IRepository.SOLUTIONS);
		debugAuthenticator = new DebugAuthenticator(method, objects, (SolutionMetaData)rootObjectMetaData)
		{
			@Override
			public void shutDown(boolean force)
			{
				super.shutDown(force);
				debugAuthenticator = null;
			}
		};
		return debugAuthenticator;
	}

	public boolean isClientStarted()
	{
		if (debugJ2DBClient != null && !debugJ2DBClient.isShutDown()) return true;
		if (debugWebClient != null) return true;
		if (debugNGClient != null) return true;
		if (debugHeadlessClient != null) return true;
		return false;
	}

	public void flushValueList(ValueList valueList)
	{
		if (debugJ2DBClient != null) ComponentFactory.flushValueList(debugJ2DBClient, valueList);
		if (debugWebClient != null) ComponentFactory.flushValueList(debugWebClient, valueList);
		if (debugNGClient != null) ComponentFactory.flushValueList(debugNGClient, valueList);
		if (debugHeadlessClient != null) ComponentFactory.flushValueList(debugHeadlessClient, valueList);
		if (debugAuthenticator != null) ComponentFactory.flushValueList(debugAuthenticator, valueList);
		for (IDebugClient c : customDebugClients.values())
		{
			ComponentFactory.flushValueList(c, valueList);
		}
	}

	public void reloadAllStyles()
	{
		// styles were added/removed; refresh all styles
		// TODO really refresh all styles (not just flush cache)
		if (debugJ2DBClient != null) ComponentFactory.flushCachedItems(debugJ2DBClient);
		if (debugWebClient != null) ComponentFactory.flushCachedItems(debugWebClient);
		if (debugNGClient != null) ComponentFactory.flushCachedItems(debugNGClient);
		if (debugHeadlessClient != null) ComponentFactory.flushCachedItems(debugHeadlessClient);
		if (debugAuthenticator != null) ComponentFactory.flushCachedItems(debugAuthenticator);
		for (IDebugClient c : customDebugClients.values())
		{
			ComponentFactory.flushCachedItems(c);
		}
	}

	CountDownLatch modelInitialised = new CountDownLatch(1);

	public void flagModelInitialised()
	{
		modelInitialised.countDown();
	}

	/**
	 * @see com.servoy.j2db.IDesignerCallback#showFormInDesigner(com.servoy.j2db.persistence.Form)
	 */
	public void showFormInDesigner(Form form)
	{
		if (designerCallback != null) designerCallback.showFormInDesigner(form);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IDesignerCallback#testAndStartDebugger()
	 */
	public void testAndStartDebugger()
	{
		if (designerCallback != null) designerCallback.testAndStartDebugger();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IDesignerCallback#addURLStreamHandler(java.lang.String, java.net.URLStreamHandler)
	 */
	@Override
	public void addURLStreamHandler(String protocolName, IDeveloperURLStreamHandler handler)
	{
		if (designerCallback != null) designerCallback.addURLStreamHandler(protocolName, handler);

	}

	public void setSolution(Solution solution)
	{
		currentSolution = solution;
		if (debugJ2DBClient != null)
		{
			debugJ2DBClient.shutDownAndDispose();
			debugJ2DBClient = null;
		}
		if (debugWebClient != null)
		{
			debugWebClient.shutDown(true);
			debugWebClient = null;
		}
		if (debugNGClient != null)
		{
			debugNGClient.shutDown(true);
			debugNGClient = null;
		}
		if (debugHeadlessClient != null)
		{
			debugHeadlessClient.shutDown(true);
			debugHeadlessClient = null;
		}
		for (IDebugClient c : customDebugClients.values())
		{
			c.shutDown(true);
		}
		customDebugClients.clear();
	}
}