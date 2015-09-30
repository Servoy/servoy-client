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

package com.servoy.j2db.server.ngclient;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sablo.eventthread.IEventDispatcher;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.IWindow;
import org.sablo.websocket.WebsocketSessionManager;
import org.sablo.websocket.utils.ObjectReference;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.server.ngclient.eventthread.NGEventDispatcher;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;


/**
 * Handles a websocket session based on a NGClient.
 *
 * @author rgansevles
 *
 */
public class NGClientWebsocketSession extends BaseWebsocketSession implements INGClientWebsocketSession
{
	private NGClient client;

	public NGClientWebsocketSession(String uuid)
	{
		super(uuid);
	}

	@Override
	public void init() throws Exception
	{
		if (client == null)
		{
			setClient(new NGClient(this));
		}
	}

	public void setClient(NGClient client)
	{
		this.client = client;
	}

	public NGClient getClient()
	{
		return client;
	}

	@Override
	public INGClientWindow createWindow(String windowName)
	{
		return new NGClientWindow(this, windowName);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<INGClientWindow> getWindows()
	{
		return (Collection<INGClientWindow>)super.getWindows();
	}

	@Override
	public Collection<ObjectReference< ? extends IWindow>> getWindowsRefs()
	{
		return super.getWindowsRefs();
	}

	@Override
	public boolean isValid()
	{
		return client != null && !client.isShutDown();
	}

	@Override
	protected IEventDispatcher createEventDispatcher()
	{
		return new NGEventDispatcher(client);
	}

	@Override
	public void onOpen(final Map<String, List<String>> requestParams)
	{
		super.onOpen(requestParams);

		if (requestParams == null)
		{
			CurrentWindow.get().cancelSession("Solution name is required");
			return;
		}

		final StartupArguments args = new StartupArguments(requestParams);
		final String solutionName = args.getSolutionName();

		if (Utils.stringIsEmpty(solutionName))
		{
			CurrentWindow.get().cancelSession("Invalid solution name");
			return;
		}

		if (!client.isEventDispatchThread()) J2DBGlobals.setServiceProvider(client);
		try
		{
			FlattenedSolution solution = client.getFlattenedSolution();
			if (solution != null)
			{
				// test for the main solution meta data else a login solution will constantly be closed even if it is for the right main solution.
				if (solution.getSolution() != null && !solutionName.equals(solution.getMainSolutionMetaData().getName()))
				{
					client.closeSolution(true, null);
				}
				else
				{
					if (solution.isMainSolutionLoaded())
					{
						String method = args.getMethodName();
						String firstArgument = args.getFirstArgument();
						if (method != null)
						{
							try
							{
								client.getScriptEngine().getScopesScope().executeGlobalFunction(null, method,
									(firstArgument == null ? null : new Object[] { firstArgument, args.toJSMap() }), false, false);
							}
							catch (Exception e1)
							{
								client.reportError(Messages.getString("servoy.formManager.error.ExecutingOpenSolutionMethod", new Object[] { method }), e1); //$NON-NLS-1$
							}
						}
					}

					client.getRuntimeWindowManager().setCurrentWindowName(CurrentWindow.get().getUuid());
					IWebFormController currentForm = client.getFormManager().getCurrentForm();
					if (currentForm != null)
					{
						// we have to call setcontroller again so that switchForm is called and the form is loaded into the reloaded/new window.
						startHandlingEvent();
						try
						{
							client.getRuntimeWindowManager().getCurrentWindow().setController(currentForm);
							sendSolutionCSSURL(solution.getSolution());
						}
						finally
						{
							stopHandlingEvent();
						}
						return;
					}
				}
			}

			getEventDispatcher().addEvent(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						// the solution was not loaded or another was loaded, now create a main window and load the solution.
						client.getRuntimeWindowManager().createMainWindow(CurrentWindow.get().getUuid());

						List<String> arguments = new ArrayList<String>();

						if (args.getSolutionName() != null) arguments.add(StartupArguments.PARAM_KEY_SOLUTION + StartupArguments.PARAM_KEY_VALUE_SEPARATOR +
							args.getSolutionName());
						if (args.getFirstArgument() != null) arguments.add(StartupArguments.PARAM_KEY_ARGUMENT + StartupArguments.PARAM_KEY_VALUE_SEPARATOR +
							args.getFirstArgument());
						if (args.getMethodName() != null) arguments.add(StartupArguments.PARAM_KEY_METHOD + StartupArguments.PARAM_KEY_VALUE_SEPARATOR +
							args.getMethodName());
						client.handleArguments(arguments.toArray(new String[arguments.size()]), args);

						client.loadSolution(solutionName);
					}
					catch (RepositoryException e)
					{
						Debug.error("Failed to load the solution: " + solutionName, e);
						sendInternalError(e);
					}
				}


			});
		}
		catch (Exception e)
		{
			Debug.error(e);
			sendInternalError(e);
		}
		finally
		{
			if (!client.isEventDispatchThread()) J2DBGlobals.setServiceProvider(null);
		}
	}

	@Override
	protected IServerService createFormService()
	{
		return new NGFormServiceHandler(this);
	}

	@Override
	public void solutionLoaded(Solution solution)
	{
		sendSolutionCSSURL(solution);
	}

	protected void sendSolutionCSSURL(Solution solution)
	{
		int styleSheetID = solution.getStyleSheetID();
		if (styleSheetID > 0)
		{
			Media styleSheetMedia = client.getFlattenedSolution().getMedia(styleSheetID);
			if (styleSheetMedia != null)
			{
				String path = "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + solution.getName() + "/" + styleSheetMedia.getName();
				getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setStyleSheet", new Object[] { path });
			}
			else
			{
				Debug.error("Cannot find solution styleSheet in media lib.");
			}
		}
		else
		{
			getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setStyleSheet", new Object[] { });
		}
	}

	@Override
	public void valueChanged()
	{
		if (client != null)
		{
			super.valueChanged();
		}
	}

	public void sendRedirect(final String redirectUrl)
	{
		IWindow curr = CurrentWindow.safeGet();
		CurrentWindow.runForWindow(curr != null && redirectUrl != null ? curr : new NGClientWebsocketSessionWindows(client.getWebsocketSession()),
			new Runnable()
			{
				@Override
				public void run()
				{
					Map<String, Object> detail = new HashMap<>();
					String htmlfilePath = Settings.getInstance().getProperty("servoy.webclient.pageexpired.page");
					if (htmlfilePath != null) detail.put("viewUrl", htmlfilePath);
					if (redirectUrl != null) detail.put("redirectUrl", redirectUrl);
					getClientService("$sessionService").executeAsyncServiceCall("expireSession", new Object[] { detail });
				}
			});
	}

	@Override
	protected IClientService createClientService(String name)
	{
		WebComponentSpecification spec = WebServiceSpecProvider.getInstance().getWebServiceSpecification(name);
		if (spec == null) spec = new WebComponentSpecification(name, "", name, null, null, null, "", null);
		return new ServoyClientService(name, spec, this);
	}

	/*
	 * All windows are now closed. We shutdown the client in order to free up the license/resources for the next NGClient instantiation.
	 *
	 * @see org.sablo.websocket.BaseWebsocketSession#sessionExpired()
	 */
	@Override
	public void sessionExpired()
	{
		getClient().shutDown(true);
		super.sessionExpired();
	}


	/**
	 * Sets an internalServerError object on the client side which shows the internal server error page.
	 * If it is run from the developer it also adds the stack trace
	 * @param e
	 */
	public static void sendInternalError(Exception e)
	{
		Map<String, Object> internalError = new HashMap<>();
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String stackTrace = sw.toString();
		if (ApplicationServerRegistry.get().isDeveloperStartup()) internalError.put("stack", stackTrace);
		String htmlView = Settings.getInstance().getProperty("servoy.webclient.error.page");
		if (htmlView != null) internalError.put("viewUrl", htmlView);
		CurrentWindow.get().getSession().getClientService("$sessionService").executeAsyncServiceCall("setInternalServerError", new Object[] { internalError });
	}

	@Override
	public void invalidateWindow(IWindow window)
	{
		super.invalidateWindow(window);

		// check for window activity each time a window is closed, after the timeout period
		ApplicationServerRegistry.get().getExecutor().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				WebsocketSessionManager.closeInactiveSessions();
			}
		}, getWindowTimeout() + 10, TimeUnit.MILLISECONDS);
	}

	@Override
	public Locale getLocale()
	{
		return client.getLocale();
	}

	@Override
	public INGClientWindow getWindowWithForm(String formName)
	{
		INGClientWindow currentWindow = NGClientWindow.getCurrentWindow();
		if (currentWindow != null && currentWindow.hasForm(formName)) return currentWindow;

		for (INGClientWindow w : getWindows())
		{
			if (w != currentWindow && w.hasForm(formName)) return w;
		}

		return null;
	}
}
