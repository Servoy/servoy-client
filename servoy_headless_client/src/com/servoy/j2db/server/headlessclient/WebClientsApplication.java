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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.AbstractRestartResponseException;
import org.apache.wicket.AccessStackPageMap;
import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IPageMap;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Response;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.application.IComponentOnBeforeRenderListener;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.PackageResource;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.AjaxEnclosureListener;
import org.apache.wicket.protocol.http.BufferedWebResponse;
import org.apache.wicket.protocol.http.HttpSessionStore;
import org.apache.wicket.protocol.http.MockServletContext;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.protocol.http.RequestUtils;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.http.request.CryptedUrlWebRequestCodingStrategy;
import org.apache.wicket.protocol.http.request.urlcompressing.UrlCompressingWebCodingStrategy;
import org.apache.wicket.protocol.http.request.urlcompressing.UrlCompressingWebRequestProcessor;
import org.apache.wicket.request.IRequestCodingStrategy;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.request.RequestParameters;
import org.apache.wicket.request.target.basic.EmptyAjaxRequestTarget;
import org.apache.wicket.request.target.basic.EmptyRequestTarget;
import org.apache.wicket.request.target.coding.BookmarkablePageRequestTargetUrlCodingStrategy;
import org.apache.wicket.request.target.coding.HybridUrlCodingStrategy;
import org.apache.wicket.request.target.coding.SharedResourceRequestTargetUrlCodingStrategy;
import org.apache.wicket.request.target.component.listener.BehaviorRequestTarget;
import org.apache.wicket.request.target.resource.SharedResourceRequestTarget;
import org.apache.wicket.session.ISessionStore;
import org.apache.wicket.session.pagemap.IPageMapEntry;
import org.apache.wicket.settings.IRequestCycleSettings;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.ValueMap;
import org.odlabs.wiquery.core.commons.IWiQuerySettings;
import org.odlabs.wiquery.core.commons.WiQuerySettings;
import org.odlabs.wiquery.ui.themes.IThemableApplication;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ServoyBeanState;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.headlessclient.dataui.RecordItemModel;
import com.servoy.j2db.server.headlessclient.dataui.WebBaseLabel;
import com.servoy.j2db.server.headlessclient.dataui.WebBaseSelectBox;
import com.servoy.j2db.server.headlessclient.dataui.WebCellBasedView;
import com.servoy.j2db.server.headlessclient.dataui.WebDataCheckBoxChoice;
import com.servoy.j2db.server.headlessclient.dataui.WebDataComboBox;
import com.servoy.j2db.server.headlessclient.dataui.WebDataCompositeTextField;
import com.servoy.j2db.server.headlessclient.dataui.WebDataHtmlArea;
import com.servoy.j2db.server.headlessclient.dataui.WebDataListBox;
import com.servoy.j2db.server.headlessclient.dataui.WebDataLookupField;
import com.servoy.j2db.server.headlessclient.dataui.WebDataRadioChoice;
import com.servoy.j2db.server.headlessclient.jquery.JQueryLoader;
import com.servoy.j2db.server.headlessclient.mask.MaskBehavior;
import com.servoy.j2db.server.headlessclient.tinymce.TinyMCELoader;
import com.servoy.j2db.server.headlessclient.yui.YUILoader;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IWebClientSessionFactory;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ISupportEventExecutor;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Class to create (special)sessions and is a container which can hold a reference to ApplicationServer (actually application server should have bin the
 * org.apache.wicket application but it requires to be a subclass from webapp)
 *
 * @author jblok
 */
public class WebClientsApplication extends WebApplication implements IWiQuerySettings, IThemableApplication
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

		@SuppressWarnings("nls")
		@Override
		protected String decodeURL(String url)
		{
			try
			{
				return super.decodeURL(url);
			}
			catch (Exception ex)
			{
				Debug.trace("Unable to decode the url: '" + url + "' most likely because the session expired", ex);
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
					if (((WebRequest)RequestCycle.get().getRequest()).isAjax())
					{
						RequestCycle.get().setRequestTarget(new RedirectAjaxRequestTarget(getApplicationSettings().getPageExpiredErrorPage()));
						throw new AbstractRestartResponseException()
						{
						};
					}
					else
					{
						throw new RestartResponseException(getApplicationSettings().getPageExpiredErrorPage());
					}
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
	private RequestCycleSettings rcSettings;

	/**
	 * Constructor
	 */
	public WebClientsApplication()
	{
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.wicket.Application#onDestroy()
	 */
	@Override
	protected void onDestroy()
	{
		SessionClient.onDestroy();
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


	@Override
	public RequestCycleSettings getRequestCycleSettings()
	{
		if (rcSettings == null)
		{
			IRequestCycleSettings superSettings = super.getRequestCycleSettings();
			if (superSettings != null) rcSettings = new RequestCycleSettings(superSettings);
		}
		return rcSettings;
	}

	/**
	 * @see wicket.protocol.http.WebApplication#init()
	 */
	@Override
	protected void init()
	{
		if (ApplicationServerSingleton.get() == null) return; // TODO this is a workaround to allow mobile test client that only starts Tomcat not to give exceptions (please remove if mobile test client initialises a full app. server in the future)

		getResourceSettings().setResourceWatcher(new ServoyModificationWatcher(Duration.seconds(5)));
//		getResourceSettings().setResourcePollFrequency(Duration.seconds(5));
		getResourceSettings().setAddLastModifiedTimeToResourceReferenceUrl(true);
		getResourceSettings().setDefaultCacheDuration((int)Duration.days(365).seconds());
		getMarkupSettings().setCompressWhitespace(true);
		getMarkupSettings().setMarkupCache(new ServoyMarkupCache(this));
		// getMarkupSettings().setStripWicketTags(true);
		getResourceSettings().setResourceStreamLocator(new ServoyResourceStreamLocator(this));
		getResourceSettings().setPackageResourceGuard(new ServoyPackageResourceGuard());
		// getResourceSettings().setResourceFinder(createResourceFinder());
		getResourceSettings().setThrowExceptionOnMissingResource(false);
		getApplicationSettings().setPageExpiredErrorPage(ServoyExpiredPage.class);
		getApplicationSettings().setClassResolver(new ServoyClassResolver());
		getSessionSettings().setMaxPageMaps(15);
//		getRequestCycleSettings().setGatherExtendedBrowserInfo(true);

		getSecuritySettings().setCryptFactory(new CachingKeyInSessionSunJceCryptFactory());

		Settings settings = Settings.getInstance();
		getDebugSettings().setOutputComponentPath(Utils.getAsBoolean(settings.getProperty("servoy.webclient.debug.wicketpath", "false"))); //$NON-NLS-1$ //$NON-NLS-2$
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
		getSharedResources().putClassAlias(JQueryLoader.class, "jquery"); //$NON-NLS-1$
		getSharedResources().putClassAlias(TinyMCELoader.class, "tinymce"); //$NON-NLS-1$
		getSharedResources().putClassAlias(org.apache.wicket.markup.html.WicketEventReference.class, "wicketevent"); //$NON-NLS-1$
		getSharedResources().putClassAlias(org.apache.wicket.ajax.WicketAjaxReference.class, "wicketajax"); //$NON-NLS-1$
		getSharedResources().putClassAlias(MainPage.class, "servoyjs"); //$NON-NLS-1$
		getSharedResources().putClassAlias(org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.class, "modalwindow"); //$NON-NLS-1$

		PackageResource.bind(this, IApplication.class, "images/open_project.gif"); //$NON-NLS-1$
		PackageResource.bind(this, IApplication.class, "images/save.gif"); //$NON-NLS-1$

		mountSharedResource("/formcss", "servoy/formcss"); //$NON-NLS-1$//$NON-NLS-2$

		sharedMediaResource = new SharedMediaResource();
		getSharedResources().add("media", sharedMediaResource); //$NON-NLS-1$

		mount(new SharedResourceRequestTargetUrlCodingStrategy("mediafolder", "servoy/media") //$NON-NLS-1$ //$NON-NLS-2$
		{
			@Override
			protected void appendParameters(AppendingStringBuffer url, Map<String, ? > parameters)
			{
				if (parameters != null && parameters.size() > 0)
				{
					Object solutionName = parameters.get("s"); //$NON-NLS-1$
					if (solutionName != null) appendPathParameter(url, null, solutionName.toString());
					Object resourceId = parameters.get("id"); //$NON-NLS-1$
					if (resourceId != null) appendPathParameter(url, null, resourceId.toString());

					StringBuilder queryParams = new StringBuilder();
					for (Entry< ? , ? > entry1 : parameters.entrySet())
					{
						Object value = ((Entry< ? , ? >)entry1).getValue();
						if (value != null)
						{
							Object key = ((Entry< ? , ? >)entry1).getKey();
							if (!"s".equals(key) && !"id".equals(key)) //$NON-NLS-1$ //$NON-NLS-2$
							{
								if (value instanceof String[])
								{
									String[] values = (String[])value;
									for (String value1 : values)
									{
										if (queryParams.length() > 0) queryParams.append("&"); //$NON-NLS-1$
										queryParams.append(key).append("=").append(value1);//$NON-NLS-1$
									}
								}
								else
								{
									if (queryParams.length() > 0) queryParams.append("&"); //$NON-NLS-1$
									queryParams.append(key).append("=").append(value);//$NON-NLS-1$
								}
							}
						}
					}
					if (queryParams.length() > 0)
					{
						url.append("?").append(queryParams);//$NON-NLS-1$
					}
				}
			}


			@Override
			protected void appendPathParameter(AppendingStringBuffer url, String key, String value)
			{
				String escapedValue = value;
				String[] values = escapedValue.split("/");//$NON-NLS-1$
				if (values.length > 1)
				{
					StringBuilder sb = new StringBuilder(escapedValue.length());
					for (String str : values)
					{
						sb.append(urlEncodePathComponent(str));
						sb.append('/');
					}
					sb.setLength(sb.length() - 1);
					escapedValue = sb.toString();
				}
				else
				{
					escapedValue = urlEncodePathComponent(escapedValue);
				}

				if (!Strings.isEmpty(escapedValue))
				{
					if (!url.endsWith("/"))//$NON-NLS-1$
					{
						url.append("/");//$NON-NLS-1$
					}
					if (key != null) url.append(urlEncodePathComponent(key)).append("/");//$NON-NLS-1$
					url.append(escapedValue);
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.apache.wicket.request.target.coding.AbstractRequestTargetUrlCodingStrategy#decodeParameters(java.lang.String, java.util.Map)
			 */
			@Override
			protected ValueMap decodeParameters(String urlFragment, Map<String, ? > urlParameters)
			{
				ValueMap map = new ValueMap();
				final String[] pairs = urlFragment.split("/"); //$NON-NLS-1$
				if (pairs.length > 1)
				{
					map.add("s", pairs[1]); //$NON-NLS-1$
					StringBuffer sb = new StringBuffer();
					for (int i = 2; i < pairs.length; i++)
					{
						sb.append(pairs[i]);
						sb.append("/"); //$NON-NLS-1$
					}
					sb.setLength(sb.length() - 1);
					map.add("id", sb.toString()); //$NON-NLS-1$
				}
				if (urlParameters != null)
				{
					map.putAll(urlParameters);
				}
				return map;
			}
		});

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
				else if (!(component.getParent() instanceof WebDataCompositeTextField))
				{
					if (!component.isEnabled())
					{
						boolean hasOnRender = (component instanceof IFieldComponent &&
							((IFieldComponent)component).getScriptObject() instanceof ISupportOnRenderCallback &&
							((ISupportOnRenderCallback)((IFieldComponent)component).getScriptObject()).getRenderEventExecutor().hasRenderCallback());
						if (!hasOnRender)
						{
							// onrender may change the enable state
							return;
						}
					}
					Component targetComponent = null;
					boolean hasFocus = false, hasBlur = false;
					if (component instanceof IFieldComponent && ((IFieldComponent)component).getEventExecutor() != null)
					{
						if (component instanceof WebDataCompositeTextField && ((WebDataCompositeTextField)component).getDelegate() instanceof Component)
						{
							targetComponent = (Component)((WebDataCompositeTextField)component).getDelegate();
						}
						else
						{
							targetComponent = component;
						}
						if (component instanceof WebBaseSelectBox)
						{
							Component[] cs = ((WebBaseSelectBox)component).getFocusChildren();
							if (cs != null && cs.length == 1) targetComponent = cs[0];
						}
						if (component instanceof WebDataHtmlArea) hasFocus = true;

						// always install a focus handler when in a table view to detect change of selectedIndex and test for record validation
						if (((IFieldComponent)component).getEventExecutor().hasEnterCmds() ||
							component.findParent(WebCellBasedView.class) != null ||
							(((IFieldComponent)component).getScriptObject() instanceof ISupportOnRenderCallback && ((ISupportOnRenderCallback)((IFieldComponent)component).getScriptObject()).getRenderEventExecutor().hasRenderCallback()))
						{
							hasFocus = true;
						}
						// Always trigger event on focus lost:
						// 1) check for new selected index, record validation may have failed preventing a index changed
						// 2) prevent focus gained to be called when field validation failed
						// 3) general ondata change
						hasBlur = true;
					}
					else if (component instanceof WebBaseLabel)
					{
						targetComponent = component;
						hasFocus = true;
					}

					if (targetComponent != null)
					{
						MainPage mainPage = targetComponent.findParent(MainPage.class);
						if (mainPage.isUsingAjax())
						{
							AbstractAjaxBehavior eventCallback = mainPage.getPageContributor().getEventCallback();
							if (eventCallback != null)
							{
								String callback = eventCallback.getCallbackUrl().toString();
								if (component instanceof WebDataRadioChoice || component instanceof WebDataCheckBoxChoice ||
									component instanceof WebDataLookupField || component instanceof WebDataComboBox || component instanceof WebDataListBox ||
									component instanceof WebDataHtmlArea || component instanceof WebDataCompositeTextField)
								{
									// is updated via ServoyChoiceComponentUpdatingBehavior or ServoyFormComponentUpdatingBehavior, this is just for events
									callback += "&nopostdata=true";
								}
								for (IBehavior behavior : targetComponent.getBehaviors())
								{
									if (behavior instanceof EventCallbackModifier)
									{
										targetComponent.remove(behavior);
									}
								}
								if (hasFocus)
								{
									StringBuilder js = new StringBuilder();
									js.append("eventCallback(this,'focus','").append(callback).append("',event)"); //$NON-NLS-1$ //$NON-NLS-2$
									targetComponent.add(new EventCallbackModifier("onfocus", true, new Model<String>(js.toString()))); //$NON-NLS-1$
									targetComponent.add(new EventCallbackModifier("onmousedown", true, new Model<String>("focusMousedownCallback(event)"))); //$NON-NLS-1$ //$NON-NLS-2$
								}
								if (hasBlur)
								{
									boolean blockRequest = false;
									// if component has ondatachange, check for blockrequest
									if (component instanceof ISupportEventExecutor && ((ISupportEventExecutor)component).getEventExecutor().hasChangeCmd())
									{
										WebClientSession webClientSession = WebClientSession.get();
										blockRequest = webClientSession != null && webClientSession.blockRequest();
									}

									StringBuilder js = new StringBuilder();
									js.append("postEventCallback(this,'blur','").append(callback).append("',event," + blockRequest + ")"); //$NON-NLS-1$ //$NON-NLS-2$
									targetComponent.add(new EventCallbackModifier("onblur", true, new Model<String>(js.toString()))); //$NON-NLS-1$
								}
							}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.wicket.protocol.http.WebApplication#newWebResponse(javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected WebResponse newWebResponse(HttpServletResponse servletResponse)
	{
		// over ride this so that redirects are not relative but absolute
		// Websphere doesn't work correctly with relative urls.
		return (getRequestCycleSettings().getBufferResponse() ? new BufferedWebResponse(servletResponse)
		{
			private String reqUrl = null;

			@Override
			public CharSequence encodeURL(CharSequence url)
			{
				if (reqUrl == null) reqUrl = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getRequestURI();
				return super.encodeURL(url);
			}

			@Override
			protected void sendRedirect(String url) throws IOException
			{
				if (reqUrl != null && url.indexOf("://") == -1) //$NON-NLS-1$
				{
					String absUrl = RequestUtils.toAbsolutePath(reqUrl, url);
					getHttpServletResponse().sendRedirect(absUrl);
				}
				else
				{
					super.sendRedirect(url);
				}
			}

		} : new WebResponse(servletResponse)
		{
			@Override
			protected void sendRedirect(String url) throws IOException
			{
				String reqUrl = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getRequestURI();
				String absUrl = RequestUtils.toAbsolutePath(reqUrl, url);
				getHttpServletResponse().sendRedirect(absUrl);
			}
		});
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
			public void respond(RequestCycle requestCycle)
			{
				// execute events from WebClient.invokeLater() before the respond (render) is started
				Session session = Session.get();
				if (session instanceof WebClientSession && ((WebClientSession)session).getWebClient() != null)
				{
					((WebClientSession)session).getWebClient().executeEvents();
				}

				super.respond(requestCycle);
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
						if (!(target instanceof Page))
						{
							boolean invalidPage = false;
							Page page2 = null;
							try
							{
								page2 = target.findParent(Page.class); // test if it has a page.
							}
							catch (Exception e)
							{
								Debug.trace(e);
								invalidPage = true;
							}
							if (page2 == null || !page2.getId().equals(page.getId()))
							{
								invalidPage = true;
							}

							if (invalidPage)
							{
								Debug.log("Couldn't resolve the page of the component, component already gone from page? returning empty"); //$NON-NLS-1$
								return EmptyRequestTarget.getInstance();
							}
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
		return new HttpSessionStore(this)
		{
			@Override
			public IPageMap createPageMap(String name)
			{
				return new ModifiedAccessStackPageMap(name);
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.wicket.protocol.http.WebApplication#newAjaxRequestTarget(org.apache.wicket.Page)
	 */
	@Override
	public AjaxRequestTarget newAjaxRequestTarget(Page page)
	{
		AjaxRequestTarget target = new CloseableAjaxRequestTarget(page);
		target.addListener(new AjaxEnclosureListener());
		return target;
	}

	/**
	 * @see org.apache.wicket.protocol.http.WebApplication#newRequestCycle(org.apache.wicket.Request, org.apache.wicket.Response)
	 */
	@Override
	public RequestCycle newRequestCycle(Request request, Response response)
	{
		// Respond to request
		return new ServoyRequestCycle(this, (WebRequest)request, (WebResponse)response);
	}

	public WiQuerySettings getWiQuerySettings()
	{
		WiQuerySettings settings = new WiQuerySettings();
		if (getDebugSettings().isAjaxDebugModeEnabled())
		{
			settings.setJQueryCoreResourceReference(JQueryLoader.JS_JQUERY_DEBUG);
		}
		else
		{
			settings.setJQueryCoreResourceReference(JQueryLoader.JS_JQUERY);
		}
		return settings;
	}

	public ResourceReference getTheme(Session session)
	{
		return JQueryLoader.CSS_UI;
	}

	public class ModifiedAccessStackPageMap extends AccessStackPageMap
	{
		public ModifiedAccessStackPageMap(final String name)
		{
			super(name);
		}

		public void flagDirty()
		{
			super.dirty();
		}

		@Override
		public void removeEntry(IPageMapEntry entry)
		{
			if (entry instanceof MainPage)
			{
				WebClient webClient = WebClientSession.get().getWebClient();
				((FormManager)webClient.getFormManager()).removeContainer(getName());
			}
			super.removeEntry(entry);
		}

	}

	private class EventCallbackModifier extends AttributeModifier
	{
		private static final String STR_EVENT_CALLBACK = "eventCallback("; //$NON-NLS-1$
		private static final String STR_POST_EVENT_CALLBACK = "postEventCallback("; //$NON-NLS-1$
		private static final String STR_FOCUS_MOUSEDOWN_CALLBACK = "focusMousedownCallback("; //$NON-NLS-1$

		EventCallbackModifier(final String attribute, final boolean addAttributeIfNotPresent, final IModel< ? > replaceModel)
		{
			super(attribute, addAttributeIfNotPresent, replaceModel);
		}

		@Override
		protected String newValue(final String currentValue, final String replacementValue)
		{
			if (currentValue != null)
			{
				if (replacementValue != null)
				{
					StringBuilder newValue = new StringBuilder();
					Iterator<String> st = getTokens(currentValue).iterator();
					String t;
					boolean replacementValueAdded = false;
					while (st.hasNext())
					{
						t = st.next();
						if ((t.startsWith(STR_EVENT_CALLBACK) && replacementValue.startsWith(STR_EVENT_CALLBACK)) ||
							(t.startsWith(STR_POST_EVENT_CALLBACK) && replacementValue.startsWith(STR_POST_EVENT_CALLBACK)) ||
							(t.startsWith(STR_FOCUS_MOUSEDOWN_CALLBACK) && replacementValue.startsWith(STR_FOCUS_MOUSEDOWN_CALLBACK)))
						{
							newValue.append(replacementValue);
							replacementValueAdded = true;
						}
						else
						{
							newValue.append(t);
						}
						newValue.append(';');
					}
					if (!replacementValueAdded) newValue.append(replacementValue);
					if (newValue.length() > 0) return newValue.toString();
				}

				return currentValue;
			}
			return replacementValue;
		}

		private ArrayList<String> getTokens(String s)
		{
			ArrayList<String> tokens = new ArrayList<String>();
			int start = 0;
			int index = 0;
			int len = s.length();
			boolean parsingQuote = false;
			char quote = 0;
			while (index < len)
			{
				switch (s.charAt(index))
				{
					case ';' :
						if (!parsingQuote)
						{
							tokens.add(s.substring(start, index));
							start = index + 1;
						}
						break;
					case '"' :
					case '\'' :
						if (index > 0 && s.charAt(index - 1) == '\\') break;
						if (parsingQuote)
						{
							if (quote == s.charAt(index))
							{
								parsingQuote = false;
							}
						}
						else
						{
							parsingQuote = true;
							quote = s.charAt(index);
						}

				}
				index++;
			}

			if (tokens.size() == 0) tokens.add(s);
			return tokens;
		}
	}
}