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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.swing.SwingUtilities;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.WebSession;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.ClientState;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.server.headlessclient.EmptyRequest;
import com.servoy.j2db.server.headlessclient.SessionClient;
import com.servoy.j2db.server.headlessclient.WebClient;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.server.headlessclient.WebClientsApplication;
import com.servoy.j2db.server.headlessclient.WebCredentials;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IFlattenedSolutionDebugListener;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

public class DebugClientHandler implements IDebugClientHandler, IDesignerCallback
{
	private volatile DebugHeadlessClient debugHeadlessClient;
	private volatile DebugAuthenticator debugAuthenticator;
	private volatile DebugWebClient debugWebClient;
	private volatile DebugJ2DBClient debugJ2DBClient;
	private volatile Solution currentSolution;

	private IDesignerCallback designerCallback;
	private DebugJ2DBClient jsunitJ2DBClient;

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
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null)
		{
			SolutionScope solutionScope = debugHeadlessClient.getScriptEngine().getSolutionScope();
			designerCallback.addScriptObjects(debugHeadlessClient, solutionScope);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IDesignerCallback#addScriptObjects(org.mozilla.javascript.Scriptable)
	 */
	public void addScriptObjects(ClientState client, Scriptable scope)
	{
		if (designerCallback != null) designerCallback.addScriptObjects(client, scope);

	}

	public List<ClientState> getActiveDebugClients()
	{
		ArrayList<ClientState> lst = new ArrayList<ClientState>();
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null) lst.add(debugJ2DBClient);
		if (debugWebClient != null && debugWebClient.getSolution() != null) lst.add(debugWebClient);
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null) lst.add(debugHeadlessClient);
		return lst;
	}


	public void refreshDebugClientsI18N()
	{
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null) debugJ2DBClient.refreshForI18NChange();
		if (jsunitJ2DBClient != null && jsunitJ2DBClient.getSolution() != null) jsunitJ2DBClient.refreshForI18NChange();
		if (debugWebClient != null && debugWebClient.getSolution() != null) debugWebClient.refreshForI18NChange();
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null) debugHeadlessClient.refreshForI18NChange();
	}

	/**
	 * @param changes
	 */
	public void refreshDebugClients(Collection<IPersist> changes)
	{
		if (debugJ2DBClient != null && debugJ2DBClient.getSolution() != null) debugJ2DBClient.refreshPersists(changes);
		if (jsunitJ2DBClient != null && jsunitJ2DBClient.getSolution() != null) jsunitJ2DBClient.refreshPersists(changes);
		if (debugWebClient != null && debugWebClient.getSolution() != null) debugWebClient.refreshPersists(changes);
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null) debugHeadlessClient.refreshPersists(changes);
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
		else if (jsunitJ2DBClient != null && jsunitJ2DBClient.getSolution() != null && RemoteDebugScriptEngine.isConnected(0))
		{
			return jsunitJ2DBClient;
		}
		return null;
	}

	/**
	 * @param persist
	 * @param elementName
	 */
	public void executeMethod(final ISupportChilds persist, final String methodname)
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
						GlobalScope gs = serviceProvider.getScriptEngine().getSolutionScope().getGlobalScope();
						Object function = gs.get(methodname);
						if (function instanceof Function)
						{
							try
							{
								serviceProvider.getScriptEngine().executeFunction((Function)function, gs, gs, null, false, false);
							}
							catch (Exception e)
							{

							}
						}
					}
					else if (persist instanceof Form)
					{
						try
						{
							FormController fp = ((FormManager)serviceProvider.getFormManager()).leaseFormPanel(((Form)persist).getName());
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
						}
					}
				}
			};
			if (serviceProvider == getDebugHeadlessClient() || serviceProvider == getDebugAuthenticator())
			{
				ApplicationServerSingleton.get().getExecutor().execute(new Runnable()
				{
					public void run()
					{
						serviceProvider.invokeLater(run);
					}
				});
			}
			else if (serviceProvider == getDebugWebClient())
			{
				ApplicationServerSingleton.get().getExecutor().execute(new Runnable()
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
		if (jsunitJ2DBClient != null)
		{
			jsunitJ2DBClient.setCurrent(solution);
		}
		if (debugWebClient != null)
		{
			debugWebClient.setCurrent((solution == null) ? null : solution.getSolutionMetaData());
		}
		if (debugHeadlessClient != null)
		{
			debugHeadlessClient.setCurrent((solution == null) ? null : solution.getSolutionMetaData());
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
		if (jsunitJ2DBClient != null && jsunitJ2DBClient.getSolution() != null && jsunitJ2DBClient.getFlattenedSolution() != null)
		{
			try
			{
				jsunitJ2DBClient.loadSecuritySettings(jsunitJ2DBClient.getFlattenedSolution());
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
			J2DBGlobals.setSingletonServiceProvider(debugJ2DBClient);
		}
		return debugJ2DBClient;
	}


	public DebugJ2DBClient getJSUnitJ2DBClient()
	{
		if (jsunitJ2DBClient == null)
		{
			jsunitJ2DBClient = createDebugSmartClient();
			// Do not call J2DBGlobals.setSingletonServiceProvider here now, it will be set temporary when the unit tests are run
			jsunitJ2DBClient.setUnitTestMode(true);
		}
		return jsunitJ2DBClient;
	}

	/**
	 * @return
	 */
	public IDebugWebClient getDebugWebClient()
	{
		return debugWebClient;
	}

	public DebugHeadlessClient getDebugHeadlessClient()
	{
		return debugHeadlessClient;
	}

	public DebugAuthenticator getDebugAuthenticator()
	{
		return debugAuthenticator;
	}

	/**
	 * @param sol
	 */
	public DebugJ2DBClient createDebugSmartClient()
	{
		if (!ApplicationServerSingleton.waitForInstanceStarted())
		{
			return null;
		}
		final DebugJ2DBClient[] client = new DebugJ2DBClient[1];
		try
		{
			Runnable run = new Runnable()
			{
				public void run()
				{
					try
					{
						synchronized (ApplicationServerSingleton.get())
						{
							if (client[0] == null)
							{
								try
								{
									if (Utils.isAppleMacOS())
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

								client[0] = new DebugJ2DBClient(DebugClientHandler.this);
								client[0].setCurrent(currentSolution);
							}
						}
					}
					catch (Exception e)
					{
						Debug.error("Cannot create DebugJ2DBClient", e);
					}
				}
			};
			if (SwingUtilities.isEventDispatchThread())
			{
				run.run();
			}
			else
			{
				SwingUtilities.invokeAndWait(run);
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return client[0];
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

	public synchronized SessionClient createDebugHeadlessClient(ServletRequest req, String userName, String password, String method, Object[] objects)
		throws Exception
	{
		if (debugHeadlessClient != null && debugHeadlessClient.getSolution() != null)
		{
			debugHeadlessClient.shutDown(true);
		}
		debugHeadlessClient = new DebugHeadlessClient(req, userName, password, method, objects, (currentSolution == null) ? null
			: currentSolution.getSolutionMetaData(), designerCallback)
		{
			@Override
			public void shutDown(boolean force)
			{
				super.shutDown(force);
				debugHeadlessClient = null;
			}
		};
		return debugHeadlessClient;
	}

	public synchronized SessionClient createDebugAuthenticator(String authenticatorName, String method, Object[] objects) throws Exception
	{
		if (debugAuthenticator != null && debugAuthenticator.getSolution() != null)
		{
			debugAuthenticator.shutDown(true);
		}
		RootObjectMetaData rootObjectMetaData = ApplicationServerSingleton.get().getLocalRepository().getRootObjectMetaData(authenticatorName,
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
		if (debugHeadlessClient != null) return true;
		return false;
	}

	public void reloadAllStyles()
	{
		// styles were added/removed; refresh all styles
		// TODO really refresh all styles (not just flush cache)
		if (J2DBGlobals.getServiceProvider() != null)
		{
			ComponentFactory.flushCachedItems();
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

}