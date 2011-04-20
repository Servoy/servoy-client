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


import java.util.Enumeration;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.apache.wicket.AbortException;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.application.IComponentOnBeforeRenderListener;
import org.apache.wicket.markup.html.PackageResource;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.HttpSessionStore;
import org.apache.wicket.protocol.http.MockServletContext;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.http.request.CryptedUrlWebRequestCodingStrategy;
import org.apache.wicket.protocol.http.request.InvalidUrlException;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.protocol.http.request.urlcompressing.UrlCompressingWebCodingStrategy;
import org.apache.wicket.protocol.http.request.urlcompressing.UrlCompressingWebRequestProcessor;
import org.apache.wicket.request.ClientInfo;
import org.apache.wicket.request.IRequestCodingStrategy;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.request.RequestParameters;
import org.apache.wicket.request.target.basic.EmptyAjaxRequestTarget;
import org.apache.wicket.request.target.basic.EmptyRequestTarget;
import org.apache.wicket.request.target.coding.BookmarkablePageRequestTargetUrlCodingStrategy;
import org.apache.wicket.request.target.coding.HybridUrlCodingStrategy;
import org.apache.wicket.request.target.component.listener.BehaviorRequestTarget;
import org.apache.wicket.request.target.resource.SharedResourceRequestTarget;
import org.apache.wicket.session.ISessionStore;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.time.Duration;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.component.ServoyBeanState;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.headlessclient.dataui.RecordItemModel;
import com.servoy.j2db.server.headlessclient.mask.MaskBehavior;
import com.servoy.j2db.server.headlessclient.yui.YUILoader;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IWebClientSessionFactory;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Class to create (special)sessions and is a container which can hold a reference to ApplicationServer (actually application server should have bin the
 * org.apache.wicket application but it requires to be a subclass from webapp)
 * 
 * @author jblok
 */
public class WebClientsApplication extends WebApplication
{
	/**
	 * @author jcompagner
	 * 
	 */
	private final class ServoyCryptedUrlWebRequestCodingStrategy extends CryptedUrlWebRequestCodingStrategy
	{
		private final IRequestCodingStrategy defaultStrategy;

		/**
		 * @param defaultStrategy
		 */
		private ServoyCryptedUrlWebRequestCodingStrategy(IRequestCodingStrategy defaultStrategy)
		{
			super(defaultStrategy);
			this.defaultStrategy = defaultStrategy;
		}

		@Override
		protected String decodeURL(String url)
		{
			try
			{
				return super.decodeURL(url);
			}
			catch (Exception ex)
			{
				// just ignore this more gracefully and redirect once to main page.
				WebClientSession webClientSession = (WebClientSession)Session.get();
				WebClient webClient = webClientSession.getWebClient();
				if (webClient != null)
				{
					if (webClient.isInDeveloper())
					{
						throw new RestartResponseException(getApplicationSettings().getPageExpiredErrorPage());
					}
					else
					{
						throw new RestartResponseException(webClient.getMainPage());
					}
				}
				else
				{
//					if (((WebRequest)RequestCycle.get().getRequest()).isAjax())
//					{
//						throw new AbortException();
//					}
//					else
//					{
					throw new RestartResponseException(getApplicationSettings().getPageExpiredErrorPage());
//					}
				}
			}
		}

		@Override
		protected CharSequence encodeURL(CharSequence url)
		{
			if (Session.get() != null && !Session.get().isSessionInvalidated()) return super.encodeURL(url);
			return url;
		}

		/**
		 * @see org.apache.wicket.protocol.http.request.CryptedUrlWebRequestCodingStrategy#encode(org.apache.wicket.RequestCycle,
		 *      org.apache.wicket.IRequestTarget)
		 */
		@Override
		public CharSequence encode(RequestCycle requestCycle, IRequestTarget requestTarget)
		{
			if (requestTarget instanceof SharedResourceRequestTarget)
			{
				return defaultStrategy.encode(requestCycle, requestTarget);
			}
			return super.encode(requestCycle, requestTarget);
		}
	}

	private SharedMediaResource sharedMediaResource;

	/**
	 * Constructor
	 */
	public WebClientsApplication()
	{
		super();
	}

	public WebClientsApplication fakeInit()
	{
		setWicketFilter(new WicketFilter()
		{
			private static final long serialVersionUID = 1L;

			/**
			 * @see wicket.protocol.http.WicketFilter#getFilterConfig()
			 */
			@Override
			public FilterConfig getFilterConfig()
			{
				return new FilterConfig()
				{
					public String getFilterName()
					{
						return "fakeservlet"; //$NON-NLS-1$
					}

					public String getInitParameter(String arg0)
					{
						return Application.DEPLOYMENT;
					}

					public Enumeration<String> getInitParameterNames()
					{
						return new Enumeration<String>()
						{
							int i = 0;

							/**
							 * @see java.util.Enumeration#hasMoreElements()
							 */
							public boolean hasMoreElements()
							{
								return i == 0;
							}

							/**
							 * @see java.util.Enumeration#nextElement()
							 */
							public String nextElement()
							{
								i++;
								return Application.CONFIGURATION;
							}
						};
					}

					public ServletContext getServletContext()
					{
						return new MockServletContext(WebClientsApplication.this, null);
					}
				};
			}
		});
		internalInit();
		return this;
	}

	/**
	 * @see wicket.protocol.http.WebApplication#init()
	 */
	@Override
	protected void init()
	{
		getResourceSettings().setResourceWatcher(new ServoyModificationWatcher(Duration.seconds(5)));
//		getResourceSettings().setResourcePollFrequency(Duration.seconds(5));
		getResourceSettings().setAddLastModifiedTimeToResourceReferenceUrl(true);
		getResourceSettings().setDefaultCacheDuration((int)Duration.days(365).seconds());
		getMarkupSettings().setCompressWhitespace(true);
		getMarkupSettings().setMarkupCache(new ServoyMarkupCache(this));
		// getMarkupSettings().setStripWicketTags(true);
		getResourceSettings().setResourceStreamLocator(new ServoyResourceStreamLocator(this));
		// getResourceSettings().setResourceFinder(createResourceFinder());
		getResourceSettings().setThrowExceptionOnMissingResource(false);
		getApplicationSettings().setPageExpiredErrorPage(ServoyExpiredPage.class);
		getApplicationSettings().setClassResolver(new ServoyClassResolver());
		getSessionSettings().setMaxPageMaps(15);
//		getRequestCycleSettings().setGatherExtendedBrowserInfo(true);

		Settings settings = Settings.getInstance();
		if (Utils.getAsBoolean(settings.getProperty("servoy.webclient.nice.urls", "false"))) //$NON-NLS-1$ //$NON-NLS-2$
		{
			mount(new HybridUrlCodingStrategy("/solutions", SolutionLoader.class)); //$NON-NLS-1$
			mount(new HybridUrlCodingStrategy("/application", MainPage.class)); //$NON-NLS-1$
			mount(new HybridUrlCodingStrategy("/ss", SolutionLoader.class) //$NON-NLS-1$
			{
				/**
				 * @see wicket.request.target.coding.BookmarkablePageRequestTargetUrlCodingStrategy#matches(wicket.IRequestTarget)
				 */
				@Override
				public boolean matches(IRequestTarget requestTarget)
				{
					return false;
				}
			});
		}
		else
		{
			mountBookmarkablePage("/solutions", SolutionLoader.class); //$NON-NLS-1$
			mount(new BookmarkablePageRequestTargetUrlCodingStrategy("/ss", SolutionLoader.class, null) //$NON-NLS-1$
			{
				/**
				 * @see wicket.request.target.coding.BookmarkablePageRequestTargetUrlCodingStrategy#matches(wicket.IRequestTarget)
				 */
				@Override
				public boolean matches(IRequestTarget requestTarget)
				{
					return false;
				}
			});
		}

		long maxSize = Utils.getAsLong(settings.getProperty("servoy.webclient.maxuploadsize", "0"), false); //$NON-NLS-1$ //$NON-NLS-2$
		if (maxSize > 0)
		{
			getApplicationSettings().setDefaultMaximumUploadSize(Bytes.kilobytes(maxSize));
		}


		getSharedResources().putClassAlias(IApplication.class, "application"); //$NON-NLS-1$
		getSharedResources().putClassAlias(PageContributor.class, "pc"); //$NON-NLS-1$
		getSharedResources().putClassAlias(MaskBehavior.class, "mask"); //$NON-NLS-1$
		getSharedResources().putClassAlias(Application.class, "servoy"); //$NON-NLS-1$
		getSharedResources().putClassAlias(org.wicketstuff.calendar.markup.html.form.DatePicker.class, "datepicker"); //$NON-NLS-1$
		getSharedResources().putClassAlias(YUILoader.class, "yui"); //$NON-NLS-1$
		getSharedResources().putClassAlias(org.apache.wicket.markup.html.WicketEventReference.class, "wicketevent"); //$NON-NLS-1$
		getSharedResources().putClassAlias(org.apache.wicket.ajax.WicketAjaxReference.class, "wicketajax"); //$NON-NLS-1$
		getSharedResources().putClassAlias(MainPage.class, "servoyjs"); //$NON-NLS-1$
		getSharedResources().putClassAlias(org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.class, "modalwindow"); //$NON-NLS-1$

		PackageResource.bind(this, IApplication.class, "images/open_project.gif"); //$NON-NLS-1$
		PackageResource.bind(this, IApplication.class, "images/save.gif"); //$NON-NLS-1$

		mountSharedResource("/formcss", "servoy/formcss"); //$NON-NLS-1$//$NON-NLS-2$

		sharedMediaResource = new SharedMediaResource();
		getSharedResources().add("media", sharedMediaResource); //$NON-NLS-1$

		getSharedResources().add("resources", new ServeResources()); //$NON-NLS-1$

		getSharedResources().add("formcss", new FormCssResource(this)); //$NON-NLS-1$

		if (settings.getProperty("servoy.webclient.error.page", null) != null) //$NON-NLS-1$
		{
			getApplicationSettings().setInternalErrorPage(ServoyErrorPage.class);
		}
		if (settings.getProperty("servoy.webclient.pageexpired.page", null) != null) //$NON-NLS-1$
		{
			getApplicationSettings().setPageExpiredErrorPage(ServoyPageExpiredPage.class);
		}

		addPreComponentOnBeforeRenderListener(new IComponentOnBeforeRenderListener()
		{
			public void onBeforeRender(Component component)
			{
				if (component instanceof IServoyAwareBean)
				{
					IModel model = component.getInnermostModel();
					WebForm webForm = component.findParent(WebForm.class);
					if (model instanceof RecordItemModel && webForm != null)
					{
						IRecord record = (IRecord)((RecordItemModel)model).getObject();
						FormScope fs = webForm.getController().getFormScope();

						if (record != null && fs != null)
						{
							((IServoyAwareBean)component).setSelectedRecord(new ServoyBeanState(record, fs));
						}
					}
				}
			}
		});
	}

	public SharedMediaResource getSharedMediaResource()
	{
		return sharedMediaResource;
	}

	/**
	 * @see wicket.Application#getHomePage()
	 */
	@Override
	public Class<SelectSolution> getHomePage()
	{
		return SelectSolution.class;
	}

	@SuppressWarnings("nls")
	@Override
	public synchronized Session newSession(Request request, Response response)
	{
		ISessionStore sessionStore = getSessionStore();
		Session session = sessionStore.lookup(request);
		if (session == null)
		{
			IWebClientSessionFactory webClientSessionFactory = ApplicationServerSingleton.get().getWebClientSessionFactory();
			if (webClientSessionFactory == null)
			{
				throw new IllegalStateException("Server was not started for web client usage");
			}
			session = webClientSessionFactory.newSession(request, response);
			session.bind();
		}
		return session;
	}

	@Override
	protected IRequestCycleProcessor newRequestCycleProcessor()
	{
		return new UrlCompressingWebRequestProcessor()
		{
			@Override
			public void processEvents(RequestCycle requestCycle)
			{
				super.processEvents(requestCycle);

				// execute events from WebClient.invokeLater() before the respond (render) is started
				Session session = Session.get();
				if (session instanceof WebClientSession && ((WebClientSession)session).getWebClient() != null)
				{
					  ((WebClientSession)session).getWebClient().executeEvents();
				}
			}

			/**
			 * @see wicket.protocol.http.WebRequestCycleProcessor#newRequestCodingStrategy()
			 */
			@Override
			protected IRequestCodingStrategy newRequestCodingStrategy()
			{
				Settings settings = Settings.getInstance();
				if (Utils.getAsBoolean(settings.getProperty("servoy.webclient.crypt-urls", "true"))) //$NON-NLS-1$ //$NON-NLS-2$
				{
					return new ServoyCryptedUrlWebRequestCodingStrategy(new UrlCompressingWebCodingStrategy());
				}
				else
				{
					return new UrlCompressingWebCodingStrategy();
				}
			}

			@Override
			protected IRequestTarget resolveListenerInterfaceTarget(RequestCycle requestCycle, Page page, String componentPath, String interfaceName,
				RequestParameters requestParameters)
			{
				try
				{
					IRequestTarget requestTarget = super.resolveListenerInterfaceTarget(requestCycle, page, componentPath, interfaceName, requestParameters);
					if (requestTarget instanceof BehaviorRequestTarget)
					{
						Component target = ((BehaviorRequestTarget)requestTarget).getTarget();
						try
						{
							target.getPage(); // test if it has a page.
						}
						catch (Exception e)
						{
							Debug.log("couldnt resolve the page of the component, component already gone from page? returning empty"); //$NON-NLS-1$
							return EmptyRequestTarget.getInstance();
						}
					}
					return requestTarget;
				}
				catch (Exception e)
				{
					Debug.log("couldnt resolve interface, component page already gone from page? returning empty"); //$NON-NLS-1$
				}
				return EmptyRequestTarget.getInstance();
			}

			@Override
			public IRequestTarget resolve(final RequestCycle requestCycle, final RequestParameters requestParameters)
			{
				try
				{
					return super.resolve(requestCycle, requestParameters);
				}
				catch (PageExpiredException e)
				{
					// if there is a page expired exception
					// then ignore it if there is a current form.
					Debug.trace("Page expired, checking for a current form"); //$NON-NLS-1$
					Request request = requestCycle.getRequest();
					if (request instanceof WebRequest && ((WebRequest)request).isAjax() && requestParameters.isOnlyProcessIfPathActive())
					{
						Debug.trace("Page expired, it is an ajan/process only if active request"); //$NON-NLS-1$
						Session session = Session.get();
						if (session instanceof WebClientSession && ((WebClientSession)session).getWebClient() != null)
						{
							WebClient webClient = ((WebClientSession)session).getWebClient();
							if (webClient.getFormManager().getCurrentForm() != null)
							{
								Debug.trace("Page expired, there is a current form, ignore this ajax request"); //$NON-NLS-1$
								return EmptyAjaxRequestTarget.getInstance();
							}
						}
					}
					throw e;
				}
			}
		};
	}

	/**
	 * @see wicket.protocol.http.WebApplication#newSessionStore()
	 */
	@Override
	protected ISessionStore newSessionStore()
	{
		return new HttpSessionStore(this);
	}

	/**
	 * @see org.apache.wicket.protocol.http.WebApplication#newRequestCycle(org.apache.wicket.Request, org.apache.wicket.Response)
	 */
	@Override
	public RequestCycle newRequestCycle(Request request, Response response)
	{
		// Respond to request
		return new WebRequestCycle(this, (WebRequest)request, (WebResponse)response)
		{
			/**
			 * @see wicket.RequestCycle#onBeginRequest()
			 */
			@Override
			protected void onBeginRequest()
			{
				WebClientSession webClientSession = (WebClientSession)getSession();
				WebClient webClient = webClientSession.getWebClient();
				if (webClient != null)
				{
					J2DBGlobals.setServiceProvider(webClient);
					webClient.onBeginRequest(webClientSession);
				}
			}

			/**
			 * @see wicket.RequestCycle#onEndRequest()
			 */
			@Override
			protected void onEndRequest()
			{
				J2DBGlobals.setServiceProvider(null);
				WebClientSession webClientSession = (WebClientSession)getSession();
				WebClient webClient = webClientSession.getWebClient();
				if (webClient != null)
				{
					webClient.onEndRequest(webClientSession);
				}
			}

			/**
			 * @see org.apache.wicket.protocol.http.WebRequestCycle#newClientInfo()
			 */
			@Override
			protected ClientInfo newClientInfo()
			{
				// We will always do a redirect here. The servoy browser info has to make one.
				WebClientInfo webClientInfo = new WebClientInfo(this);
				ClientProperties cp = webClientInfo.getProperties();
				if (cp.isBrowserInternetExplorer() || cp.isBrowserMozilla() || cp.isBrowserKonqueror() || cp.isBrowserOpera() || cp.isBrowserSafari() ||
					cp.isBrowserChrome())
				{
					if (cp.isBrowserInternetExplorer() && cp.getBrowserVersionMajor() != -1 && cp.getBrowserVersionMajor() < 7)
					{
						// IE6 is no longer supported when anchoring is enabled.
						boolean enableAnchoring = Utils.getAsBoolean(Settings.getInstance().getProperty(
							"servoy.webclient.enableAnchors", Boolean.TRUE.toString())); //$NON-NLS-1$ 
						if (enableAnchoring)
						{
							throw new RestartResponseException(new UnsupportedBrowserPage("Internet Explorer 6")); //$NON-NLS-1$
						}
					}
					Page page = getResponsePage();
					if (page != null)
					{
						throw new RestartResponseAtInterceptPageException(new ServoyBrowserInfoPage(urlFor(page).toString().replaceAll("../", ""))); //$NON-NLS-1$ //$NON-NLS-2$
					}
					else
					{
						throw new RestartResponseAtInterceptPageException(new ServoyBrowserInfoPage(getRequest().getRelativePathPrefixToContextRoot() +
							getRequest().getURL()));
					}
				}
				return webClientInfo;
			}

			/**
			 * @see org.apache.wicket.RequestCycle#onRuntimeException(org.apache.wicket.Page, java.lang.RuntimeException)
			 */
			@Override
			public Page onRuntimeException(Page page, RuntimeException e)
			{
				if (e instanceof PageExpiredException || e instanceof InvalidUrlException)
				{
					if (((WebRequest)RequestCycle.get().getRequest()).isAjax())
					{
						Debug.log("ajax request with exception aborted ", e); //$NON-NLS-1$
						throw new AbortException();
					}
				}
				if (page instanceof MainPage && ((MainPage)page).getController() != null)
				{
					Debug.error("Error rendering the page " + ((MainPage)page).getController().getName(), e); //$NON-NLS-1$
				}
				else
				{
					Debug.error("Error rendering the page " + page, e); //$NON-NLS-1$
				}
				return super.onRuntimeException(page, e);
			}
		};
	}
}