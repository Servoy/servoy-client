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
package com.servoy.j2db.server.headlessclient;

import java.awt.Dimension;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.swing.SwingUtilities;

import org.apache.wicket.AbortException;
import org.apache.wicket.Application;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IFormManagerInternal;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.IWebClientApplication;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IScriptSupport;
import com.servoy.j2db.scripting.info.WEBCONSTANTS;
import com.servoy.j2db.server.headlessclient.MainPage.ShowUrlInfo;
import com.servoy.j2db.server.headlessclient.ServoyBrowserInfoPage.ServoyWebClientInfo;
import com.servoy.j2db.server.headlessclient.eventthread.IEventDispatcher;
import com.servoy.j2db.server.headlessclient.eventthread.WicketEvent;
import com.servoy.j2db.server.headlessclient.eventthread.WicketEventDispatcher;
import com.servoy.j2db.server.shared.WebCredentials;
import com.servoy.j2db.util.Ad;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * A client which uses the org.apache.wicket framework to render a GUI in a web browser
 *
 * @author jcompagner
 */
public class WebClient extends SessionClient implements IWebClientApplication
{
	private Map<Object, Object> uiProperties;

	protected WebClient(HttpServletRequest req, WebCredentials credentials, String method, Object[] methodArgs, String solution) throws Exception
	{
		super(req, credentials, method, methodArgs, solution);

		//set the remote info, since localhost from server is useless
		ClientInfo ci = getClientInfo();
		String ipaddr = req.getHeader("X-Forwarded-For");//incase there is a forwarding proxy //$NON-NLS-1$
		if (ipaddr == null)
		{
			ipaddr = req.getRemoteAddr();
		}
		ci.setHostAddress(ipaddr);
		ci.setHostName(req.getRemoteHost());
		ci.setTimeZone(getTimeZone());
		// getTimeZone() must always be called here to cache the time-zone in the beginning, because otherwise scheduler tasks may fail (when performing find for example) - if executed before the first
		// call to getTimeZone() from within a browser request
	}

	@Override
	public int getApplicationType()
	{
		return WEB_CLIENT;
	}

	@Override
	protected RuntimeWindowManager createJSWindowManager()
	{
		return new WebRuntimeWindowManager(this);
	}

	@SuppressWarnings("nls")
	@Override
	public URL getServerURL()
	{
		if (Session.exists())
		{
			Session webClientSession = Session.get();
			if (webClientSession != null)
			{
				WebClientInfo clientInfo = (WebClientInfo)webClientSession.getClientInfo();
				if (clientInfo != null && clientInfo.getProperties() != null && clientInfo.getProperties().getHostname() != null)
				{
					String hostname = clientInfo.getProperties().getHostname();
					try
					{
						if (hostname.startsWith("http"))
						{
							// first try to find the wicket servlet
							int index = hostname.indexOf("/servoy-webclient", 8); //8 is to skip http:// or https://
							// if that fails then just try to find the first /
							if (index == -1) index = hostname.indexOf('/', 8); //8 is to skip http:// or https://
							if (index == -1)
							{
								return new URL(hostname);
							}
							else
							{
								return new URL(hostname.substring(0, index));
							}
						}
						else
						{
							return new URL("http", hostname, "");
						}
					}
					catch (MalformedURLException e)
					{
						Debug.error(e);
					}
				}
			}
		}
		return super.getServerURL();
	}

	@Override
	@SuppressWarnings("nls")
	public String getClientOSName()
	{
		if (Session.exists())
		{
			Session webClientSession = Session.get();
			if (webClientSession != null)
			{
				WebClientInfo clientInfo = (WebClientInfo)webClientSession.getClientInfo();
				if (clientInfo != null && clientInfo.getProperties() != null)
				{
					String userAgent = clientInfo.getUserAgent();
					if (userAgent != null)
					{
						if (userAgent.indexOf("NT 6.1") != -1) return "Windows 7";
						if (userAgent.indexOf("NT 6.0") != -1) return "Windows Vista";
						if (userAgent.indexOf("NT 5.1") != -1 || userAgent.indexOf("Windows XP") != -1) return "Windows XP";
						if (userAgent.indexOf("Linux") != -1) return "Linux";
						if (userAgent.indexOf("Mac") != -1) return "Mac OS";
					}
					return clientInfo.getProperties().getNavigatorPlatform();
				}
			}
		}
		return System.getProperty("os.name");
	}

	@Override
	public int getClientPlatform()
	{
		if (Session.exists())
		{
			Session webClientSession = Session.get();
			if (webClientSession != null)
			{
				try
				{
					WebClientInfo clientInfo = (WebClientInfo)webClientSession.getClientInfo();
					if (clientInfo != null)
					{
						ClientProperties properties = clientInfo.getProperties();
						if (properties != null)
						{
							return Utils.getPlatform(properties.getNavigatorPlatform());
						}
					}
				}
				catch (Exception e)
				{
					Debug.trace("trying to get the client platform of a session, when destroying the client in a none request thread", e);
				}
			}
		}
		return super.getClientPlatform();
	}

	@Override
	public TimeZone getTimeZone()
	{
		if (timeZone == null && Session.exists())
		{
			WebClientSession webClientSession = ((WebClientSession)Session.get());
			timeZone = ((WebClientInfo)webClientSession.getClientInfo()).getProperties().getTimeZone();
			// if the timezone is really just the default of the server just use that one.
			TimeZone dftZone = TimeZone.getDefault();
			if (timeZone != null && (timeZone.getRawOffset() == dftZone.getRawOffset() && timeZone.getDSTSavings() == dftZone.getDSTSavings()))
			{
				timeZone = dftZone;
			}
		}
		return super.getTimeZone();
	}

	private final Map<String, String> userRequestProperties = new HashMap<String, String>();

	/**
	 * @see com.servoy.j2db.server.headlessclient.SessionClient#getUserProperty(java.lang.String)
	 */
	@Override
	public String getUserProperty(String name)
	{
		if (name == null) return null;
		// first look if there is a property that was set in the same request.
		String value = userRequestProperties.get(name);
		if (value != null) return value;

		String defaultUserProperty = getDefaultUserProperties().get(name);
		if (defaultUserProperty != null) return defaultUserProperty;
		WebRequestCycle wrc = ((WebRequestCycle)RequestCycle.get());
		if (wrc != null && wrc.getWebRequest() != null)
		{
			Cookie[] cookies = wrc.getWebRequest().getCookies();
			if (cookies != null)
			{
				for (Cookie element : cookies)
				{
					if (name.equals(element.getName()))
					{
						return Utils.decodeCookieValue(element.getValue());
					}
				}
			}
		}

		return null;
	}

	@Override
	public String[] getUserPropertyNames()
	{
		List<String> retval = new ArrayList<String>();
		WebRequestCycle wrc = ((WebRequestCycle)RequestCycle.get());
		if (wrc != null && wrc.getWebRequest() != null)
		{
			Cookie[] cookies = wrc.getWebRequest().getCookies();
			if (cookies != null)
			{
				for (Cookie element : cookies)
				{
					retval.add(element.getName());
				}
			}
		}

		for (String defaultUserPropertyKey : getDefaultUserProperties().keySet())
		{
			if (retval.indexOf(defaultUserPropertyKey) == -1)
			{
				retval.add(defaultUserPropertyKey);
			}
		}

		return retval.toArray(new String[retval.size()]);
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.SessionClient#setUserProperty(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("nls")
	@Override
	public void setUserProperty(String name, String value)
	{
		if (name == null) return;
		if (RequestCycle.get() == null)
		{
			Debug.log("Set user property called when there is no request (onload of a from with a session timeout?), property " + name + ", value " + value +
				" not saved");
			return;
		}
		getDefaultUserProperties().remove(name);

		// first set in the the special request properties map
		if (value == null)
		{
			userRequestProperties.remove(name);
		}
		else
		{
			userRequestProperties.put(name, value);
		}

		WebRequest webRequest = ((WebRequestCycle)RequestCycle.get()).getWebRequest();

		// calculate the base path (servlet path)
		// it can be /path/ or /context/path/ or even just / if it is virtual hosted so try to get it from the url.
		String url = webRequest.getHttpServletRequest().getRequestURL().toString();
		// first try to get to the first / of the root path, strip off http://domain:port
		int index = url.indexOf("//");
		if (index > 0)
		{
			url = url.substring(index + 2);
		}
		index = url.indexOf('/');
		if (index > 0)
		{
			url = url.substring(index);
		}
		else
		{
			url = "/";
		}
		String path = webRequest.getPath();
		if (path.length() == 0) path = "?";
		index = url.indexOf(path);
		if (index > 0)
		{
			path = url.substring(0, index);
		}
		else
		{
			path = url;
		}
		index = path.indexOf(";jsessionid");
		if (index > 0)
		{
			path = path.substring(0, index);
		}
		if (!path.startsWith("/")) path = "/" + path;


		Cookie[] cookies = webRequest.getCookies();
		if (cookies != null)
		{
			for (Cookie element : cookies)
			{
				if (name.equals(element.getName()))
				{
					element.setPath(path);
					((WebRequestCycle)RequestCycle.get()).getWebResponse().clearCookie(element);
					break;
				}
			}
		}

		if (value != null)
		{
			Cookie cookie = new Cookie(name, Utils.encodeCookieValue(value));
			cookie.setMaxAge(Integer.MAX_VALUE);
			cookie.setPath(path);
			// when in secure request, browser does not send cookie over insecure request
			cookie.setSecure(Boolean.parseBoolean(Settings.getInstance().getProperty("servoy.webclient.enforceSecureCookies", "false")) ||
				((WebRequestCycle)RequestCycle.get()).getWebRequest().getHttpServletRequest().isSecure());
			// Add the cookie
			((WebRequestCycle)RequestCycle.get()).getWebResponse().addCookie(cookie);
		}
	}

	@Override
	public boolean putClientProperty(Object name, Object val)
	{
		if (WEBCONSTANTS.WEBCLIENT_TEMPLATES_DIR.equals(name))
		{
			WebClientSession webClientSession = WebClientSession.get();
			if (webClientSession != null) webClientSession.setTemplateDirectoryName((val == null ? null : val.toString()));
		}
		else
		{
			if (uiProperties == null)
			{
				uiProperties = new HashMap<Object, Object>();
			}
			uiProperties.put(name, val);
		}
		return true;
	}

	@Override
	public Object getClientProperty(Object name)
	{
		if (WEBCONSTANTS.WEBCLIENT_TEMPLATES_DIR.equals(name))
		{
			WebClientSession webClientSession = WebClientSession.get();
			return webClientSession == null ? "" : webClientSession.getTemplateDirectoryName();
		}
		else
		{
			return (uiProperties == null) ? null : uiProperties.get(name);
		}
	}

	private transient Object[] adsInfo = null;//chache to expensive to get each time

	@Override
	protected boolean registerClient(IUserClient uc) throws Exception
	{
		boolean registered = false;
		try
		{
			registered = super.registerClient(uc);
			if (!registered)
			{
				if (adsInfo == null) adsInfo = Ad.getAdInfo();
				final int w = Utils.getAsInteger(adsInfo[1]);
				final int h = Utils.getAsInteger(adsInfo[2]);
				if (w > 50 && h > 50)
				{
					URL url = (URL)adsInfo[0];
					int t = Utils.getAsInteger(adsInfo[3]);
					getMainPage().getPageContributor().addDynamicJavaScript(
						"showInfoPanel('" + url + "'," + w + ',' + h + ',' + t + ",'" + getI18NMessage("servoy.button.close") + "');"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				}
			}
		}
		catch (final ApplicationException e)
		{
			if (e.getErrorCode() == ServoyException.NO_LICENSE)
			{
				throw new RestartResponseException(ServoyServerToBusyPage.class);
			}
			else if (e.getErrorCode() == ServoyException.MAINTENANCE_MODE)
			{
				throw new RestartResponseException(ServoyServerInMaintenanceMode.class);
			}
		}
		return registered;
	}

	private boolean shuttingDown = false;

	/**
	 * @see com.servoy.j2db.server.headlessclient.SessionClient#isEventDispatchThread()
	 */
	@Override
	public boolean isEventDispatchThread()
	{
		// just execute everything immediately if client is shutting down
		if (isShutDown()) return true;
		// We test here for printing, WebForm.processFppInAWTEventQueue(..) will call SwingUtilities.invokeAndWait() to print in awt thread.
		return RequestCycle.get() != null || SwingUtilities.isEventDispatchThread();
	}

	private final List<Runnable> events = new ArrayList<Runnable>();
	private final static ThreadLocal<List<Runnable>> requestEvents = new ThreadLocal<List<Runnable>>()
	{
		@Override
		protected java.util.List<Runnable> initialValue()
		{
			return new ArrayList<Runnable>();
		}
	};

	private boolean blockEventExecution;

	boolean blockEventExecution(boolean block)
	{
		boolean prev = this.blockEventExecution;
		this.blockEventExecution = block;
		return prev;
	}

	@SuppressWarnings("nls")
	public void executeEvents()
	{
		if (blockEventExecution) return;
		List<Runnable> runnables = null;
		// This can get called during constructor, if an exception is thrown from super(), through shutdown(),
		// so the events array may be not initialized yet.
		if (events != null)
		{
			synchronized (events)
			{
				if (events.size() > 0)
				{
					runnables = new ArrayList<Runnable>(events);
					events.clear();
				}
			}

			List<Runnable> list = requestEvents.get();
			if (list.size() > 0)
			{
				if (runnables == null)
				{
					runnables = new ArrayList<Runnable>(list);
				}
				else runnables.addAll(list);
				list.clear();
			}
		}
		if (runnables != null)
		{
			if (getEventDispatcher() != null)
			{
				getEventDispatcher().addEvent(new WicketEvent(this, new EventsRunnable(runnables)));
			}
			else
			{
				new EventsRunnable(this, runnables).run();
			}
		}
		return;
	}


	/**
	 * returns the request/thread local events, clears them when there are events waiting
	 * So the caller must execute them.
	 * @return
	 */
	public List<Runnable> getRequestEvents()
	{
		List<Runnable> lst = requestEvents.get();
		if (lst.size() > 0)
		{
			ArrayList<Runnable> copy = new ArrayList<Runnable>(lst);
			lst.clear();
			return copy;
		}
		return lst;
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.SessionClient#invokeAndWait(java.lang.Runnable)
	 */
	@Override
	public void invokeAndWait(Runnable r)
	{
		if (isEventDispatchThread())
		{
			// we have to test for the thread locals because isEventDispatchThread can return true for 2 threads including AWT Thread
			IServiceProvider prev = testThreadLocals();
			try
			{
				r.run();
			}
			finally
			{
				unsetThreadLocals(prev);
			}
		}
		else
		{
			synchronized (events)
			{
				events.add(r);
			}
			synchronized (r)
			{
				try
				{
					r.wait();
				}
				catch (InterruptedException e)
				{
					Debug.error(e);
				}
			}
		}
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.SessionClient#invokeLater(java.lang.Runnable)
	 */
	@Override
	protected void doInvokeLater(Runnable r)
	{
		// When shutting down call runnable immediately, otherwise it may never happen.
		// When printing (SwingUtilities.isEventDispatchThread()) runnable also needs to be called directly.
		// In all other cases the events will be called from executeEvents(), see WebClientsApplication.processEvents(). or on begin of next request.
		if (isShutDown() || SwingUtilities.isEventDispatchThread())
		{
			// thread locals may not have been set when in AWT Thread
			IServiceProvider prev = testThreadLocals();
			try
			{
				r.run();
			}
			finally
			{
				unsetThreadLocals(prev);
			}
		}
		else
		{
			if (RequestCycle.get() != null)
			{
				requestEvents.get().add(r);
			}
			else synchronized (events)
			{
				events.add(r);
			}
		}
	}

	@Override
	protected void doInvokeLater(Runnable r, boolean immediate)
	{
		if (!immediate) invokeLater(r);
		else getScheduledExecutor().execute(r);
	}

	@Override
	public void shutDown(boolean force)
	{
		if (shuttingDown) return;
		shuttingDown = true;
		try
		{
			// first just execute all events that are waiting, but only when we are in request cycle
			if (RequestCycle.get() != null) executeEvents();

			super.shutDown(force);

			if (executor != null) executor.destroy();

			if (RequestCycle.get() != null && WebClientSession.get() != null) WebClientSession.get().logout(); //valueUnbound will do real shutdown
			else if (session != null)
			{
				try
				{
					session.invalidate();
				}
				catch (Exception e)
				{
				}
			}
		}
		finally
		{
			shuttingDown = false;
		}
	}

	@SuppressWarnings("nls")
	@Override
	public void showDefaultLogin() throws ServoyException
	{
		// if no credentials are set then redirect to the solution loader page.
		if (credentials.getUserName() == null || credentials.getPassword() == null)
		{
			String solutionName = solutionRoot.getSolution() != null ? solutionRoot.getSolution().getName() : null;
			// close the solution, webclient can't handle a "half" open solution.
			solutionRoot.close(getActiveSolutionHandler());
			Map<String, Object> map = new HashMap<String, Object>();
			if (getPreferedSolutionNameToLoadOnInit() != null)
			{
				map.put("s", getPreferedSolutionNameToLoadOnInit());
				if (getPreferedSolutionMethodNameToCall() != null) map.put("m", getPreferedSolutionMethodNameToCall());
				if (getPreferedSolutionMethodArguments() != null && getPreferedSolutionMethodArguments().length > 0 &&
					getPreferedSolutionMethodArguments()[0] != null)
				{
					map.put("a", getPreferedSolutionMethodArguments()[0]);
				}
			}
			else
			{
				map.put("s", solutionName);
			}
			getMainPage().setResponsePage(SolutionLoader.class, new PageParameters(map));
			return;
		}
		super.showDefaultLogin();
	}

	@Override
	public void logout(final Object[] solution_to_open_args)
	{
		if (getClientInfo().getUserUid() != null)
		{
			if (getSolution() != null)
			{
				// close solution first
				invokeLater(new Runnable()
				{
					public void run()
					{
						boolean doLogOut = getClientInfo().getUserUid() != null;
						if (getSolution() != null)
						{
							doLogOut = closeSolution(false, solution_to_open_args);
						}
						if (doLogOut && getSolution() == null)
						{
							credentials.clear();
							getClientInfo().clearUserInfo();
						}
						//remove cookies
						//TODO: make cookies remove through signIn form
						if (RequestCycle.get() != null)
						{
							WebRequest webRequest = ((WebRequestCycle)RequestCycle.get()).getWebRequest();
							WebResponse webResponse = ((WebRequestCycle)RequestCycle.get()).getWebResponse();

							Cookie password = webRequest.getCookie("signInForm.password");
							if (password != null)
							{
								password.setMaxAge(0);
								password.setPath("/");
								webResponse.addCookie(password);
							}
						}
					}
				});
			}
			else
			{
				credentials.clear();
				getClientInfo().clearUserInfo();
			}
		}
	}

	//behaviour in webclient is different, we do shutdown the webclient instance ,since we cannot switch solution
	private boolean closing = false;

	public boolean isClosing()
	{
		return closing;
	}

	@SuppressWarnings("nls")
	@Override
	public boolean closeSolution(boolean force, Object[] args)
	{
		if (getSolution() == null || closing) return true;

		try
		{
			RequestCycle rc = RequestCycle.get();
			closing = true;

			MainPage mp = MainPage.getRequestMainPage();
			if (mp == null)
			{
				mp = getMainPage();
			}

			// generate requests on all other reachable browser tabs/browser windows that are open in this client;
			// so that they can show the "page expired" page (even if AJAX timer is not enabled)
			List<String> triggerReqScripts = getTriggerReqOnOtherPagesJS(rc, mp);

			MainPage.ShowUrlInfo showUrlInfo = mp.getShowUrlInfo();
			boolean shownInDialog = mp.isShowingInDialog() || mp.isClosingAsDivPopup(); // if this page is showing in a div dialog (or is about to be closed as it was in one), the page redirect needs to happen inside root page, not in iframe
			boolean retval = super.closeSolution(force, args);
			if (retval)
			{
				// reset path to templates such as servoy_webclient_default.css in case session/client are reused for another solution
				if (rc != null) putClientProperty(WEBCONSTANTS.WEBCLIENT_TEMPLATES_DIR, null);
				else
				{
					// for example when being closed from admin page
					invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							putClientProperty(WEBCONSTANTS.WEBCLIENT_TEMPLATES_DIR, null);
						}
					});
				}

				if (rc != null && rc.getRequestTarget() instanceof AjaxRequestTarget)
				{
					// the idea of this line is to block all possible showurl calls generated by any RedirectAjaxRequestTargets arriving (after the solution is closed) on the page,
					// page that might want to actually show some (possibly external and slow) other page instead of page expired
					((AjaxRequestTarget)rc.getRequestTarget()).appendJavascript("getRootServoyFrame().Servoy.redirectingOnSolutionClose = true;");

					if (triggerReqScripts != null)
					{
						for (String js : triggerReqScripts)
						{
							((AjaxRequestTarget)rc.getRequestTarget()).appendJavascript(js);
						}
					}
				}

				// close all windows
				getRuntimeWindowManager().closeFormInWindow(null, true);

				Collection<Style> userStyles = getFlattenedSolution().flushUserStyles();
				if (userStyles != null)
				{
					for (Style style : userStyles)
					{
						ComponentFactory.flushStyle(this, style);
					}
				}
				getRuntimeProperties().put(IServiceProvider.RT_VALUELIST_CACHE, null);
				getRuntimeProperties().put(IServiceProvider.RT_OVERRIDESTYLE_CACHE, null);

				// what page should be shown next in browser?
				if (rc != null)
				{
					boolean showDefault = true;
					boolean urlShown = false;
					if (showUrlInfo != null)
					{
						showDefault = !"_self".equals(showUrlInfo.getTarget()) && !"_top".equals(showUrlInfo.getTarget());
						String url = "/";
						if (showUrlInfo.getUrl() != null)
						{
							url = showUrlInfo.getUrl();
						}
						if (rc.getRequestTarget() instanceof AjaxRequestTarget)
						{
							showUrlInfo.setOnRootFrame(true);
							showUrlInfo.setUseIFrame(false);
							String show = MainPage.getShowUrlScript(showUrlInfo);
							if (show != null)
							{
								urlShown = true;
								((AjaxRequestTarget)rc.getRequestTarget()).appendJavascript(show);
								// extra call to make sure that it is removed for the next time.
								mp.getShowUrlScript();
							}
						}
						else
						{
							rc.setRequestTarget(new RedirectRequestTarget(url));
						}
					}
					if (showDefault)
					{
						if (Session.exists() && RequestCycle.get() != null)
						{
							if (getPreferedSolutionNameToLoadOnInit() == null)
							{
								if ((urlShown || shownInDialog) && rc.getRequestTarget() instanceof AjaxRequestTarget)
								{
									// if this page is shown in a dialog then try to get the parent so that the page map is not included in the url
									MainPage page = mp;
									while ((page.isShowingInDialog() || page.isClosingAsDivPopup()) && page.getCallingContainer() != null)
									{
										page = page.getCallingContainer();
									}
									CharSequence urlFor = page.urlFor(SelectSolution.class, null);
									((AjaxRequestTarget)rc.getRequestTarget()).appendJavascript(
										MainPage.getShowUrlScript(new ShowUrlInfo(urlFor.toString(), "_self", null, 0, true, false)));
								}
								else
								{
									mp.setResponsePage(SelectSolution.class);
								}
							}
							else
							{
								// if solution browsing is false, make sure that the credentials are kept
								if (!Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.allowSolutionBrowsing", "true")))
								{
									WebClientSession.get().keepCredentials(getPreferedSolutionNameToLoadOnInit());
								}
								Map<String, Object> map = new HashMap<String, Object>();
								map.put("s", getPreferedSolutionNameToLoadOnInit());
								map.put("m", getPreferedSolutionMethodNameToCall());
								if (getPreferedSolutionMethodArguments() != null && getPreferedSolutionMethodArguments().length > 0)
								{
									map.put("a", getPreferedSolutionMethodArguments()[0]);
								}
								if ((urlShown || shownInDialog) && rc.getRequestTarget() instanceof AjaxRequestTarget)
								{
									CharSequence urlFor = mp.urlFor(SolutionLoader.class, new PageParameters(map));
									((AjaxRequestTarget)rc.getRequestTarget()).appendJavascript(
										MainPage.getShowUrlScript(new ShowUrlInfo(urlFor.toString(), "_self", null, 0, true, false)));
								}
								else
								{
									rc.setResponsePage(SolutionLoader.class, new PageParameters(map), null);
								}
							}
						}
					}
				}
			}
			return retval;
		}
		finally
		{
			closing = false;
		}
	}

	// generate JS requests on all other reachable browser tabs/browser windows that are open in this client
	private List<String> getTriggerReqOnOtherPagesJS(RequestCycle rc, MainPage currentPage)
	{
		List<String> triggerJSs = null;
		if (rc != null && rc.getRequestTarget() instanceof AjaxRequestTarget)
		{
			FormManager fm = (FormManager)getFormManager();
			if (fm != null)
			{
				List<String> all = fm.getCreatedMainContainerKeys();
				triggerJSs = new ArrayList<String>(all.size());
				for (String key : all)
				{
					MainPage page = (MainPage)fm.getMainContainer(key);
					if (page != null && page != currentPage) // should always be != null
					{
						String tmp = page.getTriggerBrowserRequestJS();
						if (tmp != null) triggerJSs.add(tmp);
					}
				}
				return triggerJSs;
			}
		}
		return triggerJSs;
	}

	@Override
	protected IFormManagerInternal createFormManager()
	{
		return new WebFormManager(this, getMainPage());
	}

	@Override
	protected IClientPluginAccess createClientPluginAccess()
	{
		return new WebClientPluginAccessProvider(this);
	}

	public MainPage getMainPage()
	{
		if (getFormManager() == null)
		{
			// this happens the first time. Then the main container is created.
			return createMainPage();
		}
		else
		{
			// after that the form manger has the current page
			return (MainPage)((FormManager)getFormManager()).getCurrentContainer();
		}
	}

	private MainPage createMainPage()
	{
		return new MainPage(this);
	}

	@Override
	public void setTitle(String title)
	{
		if (!isShutDown()) getMainPage().setTitle(title);
	}

	@Override
	public boolean isShutDown()
	{
		return shuttingDown || super.isShutDown();
	}

	@Override
	public void setStatusText(String text, String tooltip)
	{
		getMainPage().setStatusText(text);
	}

	@Override
	public boolean showURL(String url, String target, String target_options, int timeout, boolean onRootFrame)
	{
		MainPage mp = MainPage.getRequestMainPage();
		if (mp == null) mp = getMainPage();
		if (mp != null)
		{
			mp.setShowURLCMD(url, target, target_options, timeout, onRootFrame);
			return true;
		}
		return false;
	}

	@Override
	public void reportInfo(String message)
	{
		adminInfoQueue.add(message);
	}

	private final List<String> adminInfoQueue = Collections.synchronizedList(new ArrayList<String>());

	public String getAdminInfo()
	{
		if (adminInfoQueue.size() > 0) return adminInfoQueue.remove(0);
		else return null;
	}

	@Override
	public Dimension getScreenSize()
	{
		int width = ((WebClientInfo)WebClientSession.get().getClientInfo()).getProperties().getScreenWidth();
		int height = ((WebClientInfo)WebClientSession.get().getClientInfo()).getProperties().getScreenHeight();
		int orientation = getMainPage().getOrientation();
		if (orientation == -1)
		{
			orientation = ((ServoyWebClientInfo)WebClientSession.get().getClientInfo()).getOrientation();
		}
		if (orientation == 90 || orientation == -90)
		{
			return new Dimension(height, width);
		}
		return new Dimension(width, height);
	}

	protected final Object onBeginRequestLock = new Object();

	public void onBeginRequest(WebClientSession webClientSession)
	{
		Solution solution = getSolution();
		if (solution != null)
		{
			synchronized (onBeginRequestLock)
			{
				long solutionLastModifiedTime = webClientSession.getSolutionLastModifiedTime(solution);
				if (solutionLastModifiedTime != -1 && solutionLastModifiedTime != solution.getLastModifiedTime())
				{
					if (isClosing() || isShutDown())
					{
						if (((WebRequest)RequestCycle.get().getRequest()).isAjax()) throw new AbortException();
						else throw new RestartResponseException(Application.get().getHomePage());
					}
					refreshI18NMessages();
					((IScriptSupport)getScriptEngine()).reload();
					((WebFormManager)getFormManager()).reload();
					MainPage page = (MainPage)((WebFormManager)getFormManager()).getMainContainer(null);
					throw new RestartResponseException(page);
				}
				executeEvents();
			}
		}
	}

	public void onEndRequest(@SuppressWarnings("unused") WebClientSession webClientSession)
	{
		userRequestProperties.clear();
		// just to make sure that on the end of the request there are really no more events waiting.
		// if that is the case then copy them to the events for the next time (no much sense to do them now, everything is detached)
		List<Runnable> list = requestEvents.get();
		if (list.size() > 0)
		{
			synchronized (events)
			{
				events.addAll(list);
			}
		}
		requestEvents.remove();
	}

	private void writeObject(ObjectOutputStream stream) throws IOException
	{
		//serialize is not implemented
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
	{
		//serialize is not implemented
	}

	@SuppressWarnings("nls")
	public static boolean isMobile()
	{
		boolean isMobile = false;
		if (Session.exists())
		{
			org.apache.wicket.request.ClientInfo info = Session.get().getClientInfo();
			if (info instanceof WebClientInfo)
			{
				String userAgent = ((WebClientInfo)info).getProperties().getNavigatorUserAgent();
				if (userAgent != null)
				{
					userAgent = userAgent.toLowerCase();
					isMobile = userAgent.contains("android") || userAgent.contains("iphone") || userAgent.contains("ipad");
				}
			}
		}

		return isMobile;
	}


	private IEventDispatcher<WicketEvent> executor;

	/**
	 *
	 */
	@SuppressWarnings("nls")
	public final synchronized IEventDispatcher<WicketEvent> getEventDispatcher()
	{
		if (executor == null && Boolean.parseBoolean(Settings.getInstance().getProperty("servoy.webclient.startscriptthread", "false")))
		{
			executor = createDispatcher();
			Thread thread = new Thread(executor, "Executor,clientid:" + getClientID());
			thread.setDaemon(true);
			thread.start();
		}
		return executor;
	}

	/**
	 * Method to create the {@link IEventDispatcher} runnable
	 */
	protected IEventDispatcher<WicketEvent> createDispatcher()
	{
		return new WicketEventDispatcher(this);
	}

	@Override
	protected void reinitializeDefaultProperties()
	{
	}


}
