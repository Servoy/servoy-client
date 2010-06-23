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
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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

import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.IWebClientApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IScriptSupport;
import com.servoy.j2db.scripting.info.WEBCONSTANTS;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * A client which uses the org.apache.wicket framework to render a GUI in a web browser
 * 
 * @author jcompagner
 */
public class WebClient extends SessionClient implements IWebClientApplication
{
	private static final String COOKIE_BASE64_PREFIX = "B64p_";
	private Map<Object, Object> uiProperties;
	private final Map<String, Rectangle> windowBounds = new HashMap<String, Rectangle>();

	protected WebClient(HttpServletRequest req, String name, String pass, String method, Object[] methodArgs, String solution) throws Exception
	{
		super(req, name, pass, method, methodArgs, solution);

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
	public int getClientPlatform()
	{
		if (Session.exists())
		{
			Session webClientSession = Session.get();
			if (webClientSession != null)
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
		}
		return super.getTimeZone();
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.SessionClient#getUserProperty(java.lang.String)
	 */
	@Override
	public String getUserProperty(String name)
	{
		if (name == null) return null;
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
						return decodeCookieValue(element.getValue());
					}
				}
			}
		}
		if (name.startsWith("user.")) //not found, see if present in system properties (as pushed user property) //$NON-NLS-1$
		{
			return getSettings().getProperty(name);
		}
		return getSettings().getProperty("user." + name); //$NON-NLS-1$
	}

	@Override
	public String[] getUserPropertyNames()
	{
		List<String> retval = new ArrayList<String>();
		WebRequestCycle wrc = ((WebRequestCycle)RequestCycle.get());
		if (wrc != null && wrc.getWebRequest() != null)
		{
			Cookie[] cookies = wrc.getWebRequest().getCookies();
			for (Cookie element : cookies)
			{
				retval.add(element.getName());
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

		Cookie[] cookies = ((WebRequestCycle)RequestCycle.get()).getWebRequest().getCookies();
		if (cookies != null)
		{
			for (Cookie element : cookies)
			{
				if (name.equals(element.getName()))
				{
					((WebRequestCycle)RequestCycle.get()).getWebResponse().clearCookie(element);
					break;
				}
			}
		}
		if (value != null)
		{
			Cookie cookie = new Cookie(name, encodeCookieValue(value));
			cookie.setMaxAge(Integer.MAX_VALUE);
			// Add the cookie
			((WebRequestCycle)RequestCycle.get()).getWebResponse().addCookie(cookie);
		}
	}

	/**
	 * Reads a cookie, and decodes it if it was stored in Base64 using {@link #encodeCookieValue(String)}.
	 * @param value the cookie contents.
	 * @return the useful cookie contents.
	 */
	public static String decodeCookieValue(String value)
	{
		String cookieValue = value;
		if (cookieValue != null && cookieValue.startsWith(COOKIE_BASE64_PREFIX))
		{
			try
			{
				cookieValue = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(
					Utils.decodeBASE64(cookieValue.substring(COOKIE_BASE64_PREFIX.length()))), "UTF-8")).readLine();
			}
			catch (UnsupportedEncodingException e)
			{
				Debug.error(e);
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
		}
		return cookieValue;
	}

	/**
	 * Encodes a value into Base64 so that it can be stored in a cookie without special character problems.
	 * @param value the useful value that needs to be encoded.
	 * @return the encoded value that can be decoded using {@link #decodeCookieValue(String)}.
	 */
	public static String encodeCookieValue(String value)
	{
		try
		{
			return COOKIE_BASE64_PREFIX + Utils.encodeBASE64(value.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			Debug.error(e);
			return value;
		}
	}

	@Override
	public boolean setUIProperty(Object name, Object val)
	{
		if (WEBCONSTANTS.WEBCLIENT_TEMPLATES_DIR.equals(name))
		{
			WebClientSession.get().setTemplateDirectoryName((val == null ? null : val.toString()));
			return true;
		}
		else
		{
			if (uiProperties == null)
			{
				uiProperties = new HashMap<Object, Object>();
			}
			uiProperties.put(name, val);
		}
		return super.setUIProperty(name, val);
	}

	@Override
	public Object getUIProperty(Object name)
	{
		if (WEBCONSTANTS.WEBCLIENT_TEMPLATES_DIR.equals(name))
		{
			return WebClientSession.get().getTemplateDirectoryName();
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
				if (adsInfo == null) adsInfo = getAdInfo();
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

	@SuppressWarnings("nls")
	public void executeEvents()
	{
		Runnable[] runnables = null;
		// This can get called during constructor, if an exception is thrown from super(), through shutdown(),
		// so the events array may be not initialized yet.
		if (events != null)
		{
			synchronized (events)
			{
				if (events.size() > 0)
				{
					runnables = new Runnable[events.size()];
					runnables = events.toArray(runnables);
					events.clear();
				}
			}
		}
		if (runnables != null)
		{
			for (Runnable runnable : runnables)
			{
				try
				{
					runnable.run();
				}
				catch (Throwable e)
				{
					Debug.error("error executing event " + runnable, e);
				}
				synchronized (runnable)
				{
					runnable.notifyAll();
				}
			}
			// look if those did add new events in the mean time.
			executeEvents();
		}
		return;
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
			boolean reset = testThreadLocals();
			try
			{
				r.run();
			}
			finally
			{
				if (reset) unsetThreadLocals();
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
	public void invokeLater(Runnable r)
	{
		if (isEventDispatchThread())
		{
			// we have to test for the thread locals because isEventDispatchThread can return true for 2 threads including AWT Thread
			boolean reset = testThreadLocals();
			try
			{
				r.run();
			}
			finally
			{
				if (reset) unsetThreadLocals();
			}
		}
		else
		{
			synchronized (events)
			{
				events.add(r);
			}
		}
	}

	@Override
	public void invokeLater(Runnable r, boolean immediate)
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
			// first just execute all events that are waiting..
			executeEvents();

			super.shutDown(force);
			if (WebClientSession.get() != null) WebClientSession.get().logout(); //valueUnbound will do real shutdown
		}
		finally
		{
			shuttingDown = false;
		}
	}

	//behaviour in webclient is different, we do shutdown the webclient instance ,since we cannot switch solution
	private boolean closing = false;

	public boolean isClosing()
	{
		return closing;
	}

	@Override
	public boolean closeSolution(boolean force, Object[] args)
	{
		if (getSolution() == null || closing) return true;

		try
		{
			closing = true;

			MainPage.ShowUrlInfo showUrlInfo = getMainPage().getShowUrlInfo();
			boolean retval = super.closeSolution(force, args);
			if (retval)
			{
				Collection<Style> userStyles = getFlattenedSolution().flushUserStyles();
				if (userStyles != null)
				{
					for (Style style : userStyles)
					{
						ComponentFactory.flushStyle(style);
					}
				}
				getRuntimeProperties().put(IServiceProvider.RT_VALUELIST_CACHE, null);
				getRuntimeProperties().put(IServiceProvider.RT_OVERRIDESTYLE_CACHE, null);
				RequestCycle rc = RequestCycle.get();
				if (rc != null)
				{
					if (showUrlInfo != null)
					{
						String url = "/"; //$NON-NLS-1$
						if (showUrlInfo.getUrl() != null)
						{
							url = showUrlInfo.getUrl();
						}
						if (rc.getRequestTarget() instanceof AjaxRequestTarget)
						{
							String show = MainPage.getShowUrlScript(showUrlInfo);
							if (show != null)
							{
								((AjaxRequestTarget)rc.getRequestTarget()).appendJavascript(show);
							}
						}
						else
						{
							rc.setRequestTarget(new RedirectRequestTarget(url));
						}
					}
					else
					{
						if (Session.exists())
						{
							if (getPreferedSolutionNameToLoadOnInit() == null)
							{
								getMainPage().setResponsePage(SelectSolution.class);
							}
							else
							{
								Map<String, Object> map = new HashMap<String, Object>();
								map.put("s", getPreferedSolutionNameToLoadOnInit()); //$NON-NLS-1$
								map.put("m", getPreferedSolutionMethodNameToCall()); //$NON-NLS-1$
								if (getPreferedSolutionMethodArguments() != null && getPreferedSolutionMethodArguments().length > 0)
								{
									map.put("a", getPreferedSolutionMethodArguments()[0]); //$NON-NLS-1$
								}
								getMainPage().setResponsePage(SolutionLoader.class, new PageParameters(map));
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

	@Override
	protected IFormManager createFormManager()
	{
		WebFormManager fm = new WebFormManager(this, getMainPage());
		return fm;
	}

	@Override
	public IClientPluginAccess createClientPluginAccess()
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
	public boolean showURL(String url, String target, String target_options, int timeout)
	{
		if (getMainPage() != null)
		{
			getMainPage().setShowURLCMD(url, target, target_options, timeout);
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
		return new Dimension(width, height);
	}

	public void setWindowBounds(String windowName, Rectangle bounds)
	{
		if (windowName == null)
		{
			if (bounds != null)
			{
				ClientProperties properties = ((WebClientInfo)WebClientSession.get().getClientInfo()).getProperties();
				properties.setBrowserWidth(bounds.width);
				properties.setBrowserHeight(bounds.height);
			}
		}
		else
		{
			if (bounds == null)
			{
				windowBounds.remove(windowName);
			}
			else
			{
				windowBounds.put(windowName, bounds);
			}
		}
	}

	public Rectangle getWindowBounds(String windowName)
	{
		Rectangle bounds = windowName == null ? null : windowBounds.get(windowName);
		if (bounds == null)
		{
			// fall back to the browser window size
			ClientProperties properties = ((WebClientInfo)WebClientSession.get().getClientInfo()).getProperties();
			bounds = new Rectangle(properties.getBrowserWidth(), properties.getBrowserHeight());
		}
		return bounds;
	}

	public void onBeginRequest(WebClientSession webClientSession)
	{
		if (getSolution() != null)
		{
			synchronized (webClientSession)
			{
				long solutionLastModifiedTime = webClientSession.getSolutionLastModifiedTime();
				if (solutionLastModifiedTime != -1 && solutionLastModifiedTime != getSolution().getLastModifiedTime())
				{
					webClientSession.setSolutionLastModifiedTime(getSolution().getLastModifiedTime());
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
	}
}
