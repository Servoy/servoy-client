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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.DynamicWebResource;
import org.apache.wicket.markup.html.DynamicWebResource.ResourceState;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.util.crypt.ICrypt;

import com.servoy.j2db.IWebClientApplication;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.server.headlessclient.dnd.DNDSessionInfo;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.WebCredentials;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * A session which holds the actual started client inside
 *
 * @author jblok
 */
public class WebClientSession extends WebSession
{
	private static final long serialVersionUID = 1L;

	private long solutionLastModifiedTime = -1;
	private Solution previousSolution = null;

	private HttpSession httpSession;

	private final WebCredentials credentials = new WebCredentials();

	private Object[] serveInfo;

	private transient final DNDSessionInfo dndSessionInfo = new DNDSessionInfo();

	private String keepCredentialsSolutionName;

	private final boolean blockInput;

	private final boolean hideLoadingIndicator;

	private transient ICrypt crypt;

	public static WebClientSession get()
	{
		if (exists())
		{
			return (WebClientSession)Session.get();
		}
		return null;
	}

	public WebClientSession(Request request)
	{
		super(request);
		setTemplateDirectoryName("default"); //$NON-NLS-1$
		blockInput = Boolean.valueOf(Settings.getInstance().getProperty("servoy.webclient.blockinputonrequest", "false")).booleanValue(); //$NON-NLS-1$ //$NON-NLS-2$
		hideLoadingIndicator = Boolean.valueOf(Settings.getInstance().getProperty("servoy.webclient.hideloadingindicator", "false")).booleanValue(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@SuppressWarnings("nls")
	public IWebClientApplication startSessionClient(RootObjectMetaData sd, String method, StartupArguments argumentsScope) throws Exception
	{
		String firstArgument = argumentsScope.getFirstArgument();
		IWebClientApplication webClient = getWebClient();
		if (webClient != null)
		{
			boolean solutionLoaded = webClient.getSolution() != null;
			if (solutionLoaded && !webClient.closeSolution(false, null))
			{
				return webClient; // not allowed to close solution?
			}

			if (solutionLoaded && isSignedIn() && !Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.allowSolutionBrowsing", "true")) &&
				!sd.getName().equals(keepCredentialsSolutionName))
			{
				webClient.logout(null);
			}
			if (!isSignedIn())
			{
				SolutionMetaData smd = (SolutionMetaData)sd;
				IRepository repository = ApplicationServerRegistry.get().getLocalRepository();
				Solution sol = (Solution)repository.getActiveRootObject(smd.getName(), IRepository.SOLUTIONS);
				if (sol.getLoginSolutionName() == null && sol.getLoginFormID() <= 0 && smd.getMustAuthenticate())
				{
					//signin first
					throw new RestartResponseAtInterceptPageException(SignIn.class);
				}
			}
			keepCredentialsSolutionName = null;
		}
		if (webClient == null || webClient.isShutDown())
		{
			HttpServletRequest req = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest();
			httpSession = req.getSession();
			webClient = createWebClient(req, credentials, method, firstArgument == null ? null : new Object[] { firstArgument, argumentsScope.toJSMap() },
				sd.getName());
			webClient.handleArguments(new String[] { sd.getName() }, argumentsScope);
			if (RequestCycle.get() != null)
			{
				// if this is inside a request cycle set the service provider.
				// will be reset by the detach of the RequestCycle.
				J2DBGlobals.setServiceProvider(webClient);
			}
			setAttribute("servoy_webclient", webClient);
		}
		else
		{
			webClient.handleArguments(firstArgument != null ? new String[] { sd.getName(), method, firstArgument } : new String[] { sd.getName(), method },
				argumentsScope);
		}

		webClient.handleClientUserUidChanged(null, ""); // fake first load
		if (webClient.getSolution() != null) getSolutionLastModifiedTime(webClient.getSolution());
		else
		{
			if (webClient.getPreferedSolutionNameToLoadOnInit() != null)
			{
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("s", webClient.getPreferedSolutionNameToLoadOnInit());
				map.put("m", webClient.getPreferedSolutionMethodNameToCall());
				if (webClient.getPreferedSolutionMethodArguments() != null && webClient.getPreferedSolutionMethodArguments().length > 0)
				{
					map.put("a", webClient.getPreferedSolutionMethodArguments()[0]);
				}
				throw new RestartResponseException(SolutionLoader.class, new PageParameters(map));
			}
		}
		return webClient;
	}

	protected IWebClientApplication createWebClient(HttpServletRequest req, WebCredentials credentials, String method, Object[] methodArgs, String solution)
		throws Exception
	{
		return new WebClient(req, credentials, method, methodArgs, solution);
	}

	public WebClient getWebClient()
	{
		return (WebClient)getAttribute("servoy_webclient"); //$NON-NLS-1$
	}

	public boolean authenticate(String u, String p)
	{
		if (ApplicationServerRegistry.get().checkDefaultServoyAuthorisation(u, p) != null)
		{
			credentials.setUserName(u);
			credentials.setPassword(p);
			return true;
		}
		return false;
	}

	/**
	 * Logout is being called by:
	 *
	 * 1> 2 javascript methods: js_logout and js_exit
	 *
	 * 2> 2 UserClient methods: closeSolution and shutDown
	 *
	 * 3> 1 ValueUnbound of the session a> Session time out b> Remove attribute when loading new Solution
	 *
	 * With 1 and 2 the session can be invalidated. With 3a the session is already invalidating.
	 *
	 * 3b the session shouldn't be invalidated.
	 *
	 * If logout calls invalidate then the value unbound will be called again that will call logout again.
	 */
	public void logout()
	{
		credentials.clear();

		RequestCycle rc = RequestCycle.get();
		if (rc != null)
		{
			rc.setRedirect(false);
			invalidate();
		}
		else if (httpSession != null)
		{
			try
			{
				httpSession.invalidate();
			}
			catch (RuntimeException ex)
			{
				// ignore can be that it is already (being) invalidated.
			}
		}
		httpSession = null;
	}

	public boolean isSignedIn()
	{
		return credentials.getUserName() != null && credentials.getPassword() != null;
	}

	public void setTemplateDirectoryName(String dirName)
	{
		String name = dirName;
		if (name == null || name.toString().length() == 0)
		{
			name = "default$";// for $ meaning see ServoyResourceStreamLocator //$NON-NLS-1$
		}
		else
		{
			name = name + "$";// for $ meaning see ServoyResourceStreamLocator //$NON-NLS-1$
		}
		setStyle(name);
	}

	public String getTemplateDirectoryName()
	{
		String retval = getStyle();
		return retval.substring(0, retval.length() - 1); // trim tailing $
	}

	/**
	 * @return
	 */
	public long getSolutionLastModifiedTime(Solution solution)
	{
		if (previousSolution == null || !solution.equals(previousSolution))
		{
			solutionLastModifiedTime = solution.getLastModifiedTime();
			previousSolution = solution;
		}
		return solutionLastModifiedTime;
	}

	public void serveResource(String fname, byte[] bs, String mimetype, String contentDisposition)
	{
		serveInfo = new Object[] { fname, bs, mimetype, contentDisposition == null ? "attachment" : contentDisposition }; //$NON-NLS-1$
	}

	public boolean isServedResourceAttachment()
	{
		return serveInfo != null && serveInfo.length >= 4 && "attachment".equals(serveInfo[3]);
	}

	/**
	 * @see wicket.IResourceListener#onResourceRequested()
	 */
	public DynamicWebResource.ResourceState getResourceState()
	{
		DynamicWebResource.ResourceState resourceState = null;
		if (serveInfo != null && serveInfo[0] != null && serveInfo[1] != null)
		{
			((WebResponse)RequestCycle.get().getResponse()).setHeader("Content-disposition", serveInfo[3] + "; filename=\"" + serveInfo[0] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			resourceState = new DynamicWebResource.ResourceState()
			{
				private final byte[] data = (byte[])serveInfo[1];
				private final String mime = (String)serveInfo[2];

				@Override
				public int getLength()
				{
					return data.length;
				}

				@Override
				public byte[] getData()
				{
					return data;
				}

				@Override
				public String getContentType()
				{
					return mime;
				}
			};
		}
		else
		{
			resourceState = new ResourceState()
			{
				@Override
				public byte[] getData()
				{
					return new byte[0];
				}

				@Override
				public String getContentType()
				{
					return null;
				}
			};
		}

		class ServeClearer implements Runnable
		{
			private Object[] toClear;

			ServeClearer(Object[] ref)
			{
				toClear = ref;
			}

			public void run()
			{
				toClear[0] = null;//clear
				toClear[1] = null;//clear
				toClear[2] = null;//clear
				toClear = null;//clear
			}
		}

		if (getWebClient() != null && serveInfo != null)
		{
			// clear after one minute
			getWebClient().getScheduledExecutor().schedule(new ServeClearer(serveInfo), 60, TimeUnit.SECONDS);
		}

		return resourceState;
	}

	public DNDSessionInfo getDNDSessionInfo()
	{
		return dndSessionInfo;
	}

	public boolean useAjax()
	{
		WebClient webClient = getWebClient();
		return webClient != null && Utils.getAsBoolean(webClient.getRuntimeProperties().get("useAJAX"));
	}

	public void keepCredentials(String solutionName)
	{
		this.keepCredentialsSolutionName = solutionName;
	}

	@Override
	public void invalidateNow()
	{
		if (Debug.tracing())
		{
			Debug.trace("Session invalidated for sessionid: " + getId());
		}
		super.invalidateNow();
	}


	/**
	 * @return
	 */
	public boolean blockRequest()
	{
		return blockInput;
	}

	public boolean hideLoadingIndicator()
	{
		return hideLoadingIndicator;
	}

	private final HashMap<Page, List<Page>> lockedPages = new HashMap<Page, List<Page>>();
	private final ThreadLocal<List<Page>> toRelease = new ThreadLocal<List<Page>>()
	{
		@Override
		protected java.util.List<Page> initialValue()
		{
			return new ArrayList<Page>();
		};
	};

	public List<Page> getPagesToRelease()
	{
		return toRelease.get();
	}

	/**
	 * @param touchedPages
	 * @param mainPage
	 */
	public void wantsToLock(List<Page> touchedPages, MainPage mainPage)
	{
		List<Page> ownLockedPages = toRelease.get();
		if (ownLockedPages.contains(mainPage)) return;
		synchronized (lockedPages)
		{
			boolean found = touchedPages.size() > 0;
			boolean released = false;
			while (found)
			{
				for (Page touched : touchedPages)
				{
					// if the current touched pages are in the locked pages map and its not on our own locked pages list.
					// something is waiting for that page to be released.
					found = !ownLockedPages.contains(touched) && lockedPages.containsKey(touched);
					if (found)
					{
						// release first all current pages so that the other thread can go on.
						releaseLocks();
						released = true;
						try
						{
							// wait for the other to release the locked pages.
							lockedPages.wait();
						}
						catch (InterruptedException e)
						{
							Debug.error(e);
						}
						break;
					}
				}
			}
			// if we did release our own pages, we have to lock them now again.
			if (released)
			{
				for (Page touched : touchedPages)
				{
					getPage(touched.getPageMapName(), touched.getPath(), Page.LATEST_VERSION);
				}
			}
			// add them to the threadlocal so that we know what to release for this thread in ondetach.
			ownLockedPages.add(mainPage);
			// mark the page as locked by the current touched pages.
			lockedPages.put(mainPage, touchedPages);
		}
	}

	@Override
	protected void detach()
	{
		super.detach();
		synchronized (lockedPages)
		{
			// remove all locks this thread has
			for (Page page : toRelease.get())
			{
				lockedPages.remove(page);
			}
			toRelease.remove();
			// notify other threads that are waiting because they released there own used page lock.
			lockedPages.notifyAll();
		}
	}

	/**
	 * @return
	 */
	public ICrypt getCrypt()
	{
		return crypt;
	}

	/**
	 * @param crypt the crypt to set
	 */
	public void setCrypt(ICrypt crypt)
	{
		this.crypt = crypt;
	}
}
