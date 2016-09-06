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

import java.rmi.RemoteException;
import java.util.Iterator;

import javax.servlet.http.HttpSession;

import org.apache.wicket.AbortException;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;
import org.apache.wicket.request.target.coding.HybridUrlCodingStrategy;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IWebClientApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.server.headlessclient.MainPage.ShowUrlInfo;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

public class SolutionLoader extends WebPage
{
	private static final long serialVersionUID = 1L;

	public SolutionLoader(PageParameters pp)
	{
		SolutionMetaData theReq = null;

		try
		{
			if (ApplicationServerRegistry.get().getDataServer().isInGlobalMaintenanceMode() ||
				ApplicationServerRegistry.get().getDataServer().isInServerMaintenanceMode())
			{
				// do this before redirect & register client - where it is usually detected, because when clustered
				// this should result in a valid switch to another server in the cluster by the load balancer; if we wait until
				// after redirect, a page expired will happen on the other server

//			throw new AbortWithHttpStatusException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, false); this works, but doesn't show maintenance error page for non-clustered case

				Session.get().invalidate();
				RequestCycle.get().setRedirect(false);
				throw new RestartResponseException(new ServoyServerInMaintenanceMode());
			}
		}
		catch (RemoteException e)
		{
			// will not happen
			throw new RuntimeException(e);
		}

		FeedbackPanel feedback = new FeedbackPanel("feedback");
		add(feedback);

		StartupArguments argumentsScope = new StartupArguments(pp);

		String solutionName = argumentsScope.getSolutionName();
		String method = argumentsScope.getMethodName();
		String firstArgument = argumentsScope.getFirstArgument();

		try
		{
			IRepository repository = ApplicationServerRegistry.get().getLocalRepository();
			SolutionMetaData smd = (SolutionMetaData)repository.getRootObjectMetaData(solutionName, IRepository.SOLUTIONS);
			if (smd == null ||
					smd.getSolutionType() == SolutionMetaData.SOLUTION ||
					smd.getSolutionType() == SolutionMetaData.WEB_CLIENT_ONLY ||
					((smd.getSolutionType() == SolutionMetaData.MOBILE || smd.getSolutionType() == SolutionMetaData.MODULE) && ApplicationServerRegistry.get().isDeveloperStartup()))
			{
				theReq = smd;
			}
			else
			{
				Debug.log("Not loading solution " + smd.getName() + ", it is not configured for webclient usage");
				theReq = null;
			}

			if (theReq != null)
			{
				Solution sol = (Solution)repository.getActiveRootObject(solutionName, IRepository.SOLUTIONS);
				if (sol.getLoginSolutionName() == null && sol.getLoginFormID() <= 0 && theReq.getMustAuthenticate() &&
					!((WebClientSession)getSession()).isSignedIn())
				{
					String authType = pp.getString("sv_auth_type"); //$NON-NLS-1$
					boolean authorized = false;
					if ((authType != null && authType.equals("basic")) || //$NON-NLS-1$
						(authType == null && Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.webclient.basic.authentication", "false")))) //$NON-NLS-1$ //$NON-NLS-2$
					{
						String authorizationHeader = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getHeader("Authorization"); //$NON-NLS-1$
						if (authorizationHeader != null)
						{
							String authorization = authorizationHeader.substring(6);
							// TODO: which encoding to use? see http://tools.ietf.org/id/draft-reschke-basicauth-enc-05.xml
							authorization = new String(Utils.decodeBASE64(authorization));
							int index = authorization.indexOf(':');
							if (index > 0)
							{
								String username = authorization.substring(0, index);
								String password = authorization.substring(index + 1);
								authorized = ((WebClientSession)getSession()).authenticate(username, password);
							}
						}
						if (!authorized)
						{
							((WebResponse)RequestCycle.get().getResponse()).getHttpServletResponse().setHeader("WWW-Authenticate", "Basic realm=\"webclient\""); //$NON-NLS-1$ //$NON-NLS-2$
							throw new AbortWithWebErrorCodeException(401);
						}
					}

					if (!authorized)
					{
						//signin first
						throw new RestartResponseAtInterceptPageException(SignIn.class);
					}
				}
				WebClientSession session;
				HttpSession httpSession;
				synchronized (sol)
				{
					// create the http session
					httpSession = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getSession();
					Session.unset();
					session = (WebClientSession)getSession();
					session.bind();
					session.getClientInfo();
				}
				synchronized (httpSession)
				{
					IWebClientApplication sc = session.getWebClient();
					if (sc != null && sc.getSolution() != null && sc.getFlattenedSolution().getMainSolutionMetaData().getName().equals(solutionName))
					{
						// make sure it is registered as a start of a request.
						session.getWebClient().onBeginRequest(session);
						FormManager formManager = ((FormManager)sc.getFormManager());
						String currentPageMapName = getPageMap().getName();
						if (currentPageMapName != null && !Utils.equalObjects(sc.getMainPage().getPageMap().getName(), currentPageMapName))
						{
							IMainContainer newContainer = formManager.getOrCreateMainContainer(currentPageMapName);
							formManager.setCurrentContainer(newContainer, currentPageMapName);
						}
						// remove the method/argument from the page parameters, they shouldn't be used to generate a redirect url.
						pp.remove("method"); //$NON-NLS-1$
						pp.remove("m"); //$NON-NLS-1$
						pp.remove("argument"); //$NON-NLS-1$
						pp.remove("a"); //$NON-NLS-1$

						// also remove client method arguments to avoid stackoverflow for deeplinked authenticate solutions (js_login called inside deeplinked method)
						sc.handleArguments(null);
						if (method != null)
						{
							try
							{
								sc.getScriptEngine().getScopesScope().executeGlobalFunction(null, method,
									(argumentsScope.toJSMap().isEmpty() ? null : new Object[] { firstArgument, argumentsScope.toJSMap() }), false, false);
							}
							catch (Exception e1)
							{
								sc.reportError(Messages.getString("servoy.formManager.error.ExecutingOpenSolutionMethod", new Object[] { method }), e1); //$NON-NLS-1$
							}
						}
						if (formManager.getCurrentContainer().getController() == null)
						{
							Iterator<Form> e = sc.getFlattenedSolution().getForms(true);
							// add all forms first, they may be referred to in the login form
							Form first = sc.getFlattenedSolution().getForm(sc.getSolution().getFirstFormID());
							boolean formCanBeInstantiated = sc.getFlattenedSolution().formCanBeInstantiated(first);
							while (!formCanBeInstantiated && e.hasNext())
							{
								Form form = e.next();
								formCanBeInstantiated = sc.getFlattenedSolution().formCanBeInstantiated(form);
								if (formCanBeInstantiated) first = form;
							}
							if (first != null)
							{
								formManager.showFormInCurrentContainer(first.getName());
							}
						}

					}
					else
					{
						sc = session.startSessionClient(theReq, method, argumentsScope);
					}
					if (sc.isValid())
					{
						Page page = sc.getMainPage();
						// do get it from the real wicket session so that a lock is set on this page. (or waited for the lock)
						Page p = session.getPage(page.getPageMapName(), page.getId(), page.getCurrentVersionNumber());
						if (p instanceof MainPage)
						{
							page = p;
							ShowUrlInfo urlScript = ((MainPage)p).getShowUrlInfo();
							if (urlScript != null && "_self".equals(urlScript.getTarget()))
							{
								// a redirect was found to it self, just redirect directly to that one.
								// clear the current main pages show url script first.
								((MainPage)p).getShowUrlScript();
								RequestCycle.get().setRequestTarget(new RedirectRequestTarget(urlScript.getUrl()));
								return;
							}
						}

						HybridUrlCodingStrategy.setInitialPageParameters(page, pp);
						setResponsePage(page);
						setRedirect(true);
						//setRedirect(Utils.getAsBoolean(sc.getSettings().getProperty("servoy.webclient.nice.urls", "false")));
					}
				}
			}
		}
		catch (RestartResponseAtInterceptPageException restart)
		{
			setRedirect(false);
			throw restart;
		}
		catch (AbortException abort)
		{
			setRedirect(true);
			throw abort;
		}
		catch (Exception e)
		{
			Debug.error(e);
			error(e.toString());
		}
	}
}
