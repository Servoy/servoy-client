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

import org.apache.wicket.IClusterable;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;

/**
 * @author jcompagner
 * 
 */
public class ServoyBrowserInfoPage extends WebPage
{
	private static final long serialVersionUID = 1L;

	/** the url to continue to after this page. */
	private String continueTo;


	/**
	 * Constructor. The page will redirect to the given url after waiting for the given number of seconds.
	 * 
	 * @param continueTo the url to redirect to when the browser info is handled
	 */
	public ServoyBrowserInfoPage(final String continueTo)
	{
		if (continueTo == null)
		{
			throw new IllegalArgumentException("Argument continueTo must be not null");
		}
		setContinueTo(continueTo);
		initComps();
	}

	/**
	 * @see org.apache.wicket.Component#isVersioned()
	 */
	@Override
	public boolean isVersioned()
	{
		return false;
	}

	/**
	 * Adds components.
	 */
	private final void initComps()
	{
		add(new BrowserInfoForm("postback"));
	}

	/**
	 * Set the url to continue to after this page.
	 * 
	 * @param continueTo the url
	 */
	protected final void setContinueTo(String continueTo)
	{
		this.continueTo = continueTo;
	}

	public class BrowserInfoForm extends Form
	{
		private static final long serialVersionUID = 1L;

		/**
		 * Construct.
		 * 
		 * @param id component id
		 */
		public BrowserInfoForm(String id)
		{
			super(id, new CompoundPropertyModel(new ClientPropertiesBean()));

			add(new TextField("navigatorAppName"));
			add(new TextField("navigatorAppVersion"));
			add(new TextField("navigatorAppCodeName"));
			add(new TextField("navigatorCookieEnabled"));
			add(new TextField("navigatorJavaEnabled"));
			add(new TextField("navigatorLanguage"));
			add(new TextField("navigatorPlatform"));
			add(new TextField("navigatorUserAgent"));
			add(new TextField("screenWidth"));
			add(new TextField("screenHeight"));
			add(new TextField("screenColorDepth"));
			add(new TextField("utcOffset"));
			add(new TextField("utcDSTOffset"));
			add(new TextField("browserWidth"));
			add(new TextField("browserHeight"));
			add(new TextField("hostname"));
		}

		/**
		 * @see org.apache.wicket.markup.html.form.Form#onSubmit()
		 */
		@Override
		protected void onSubmit()
		{
			ClientPropertiesBean propertiesBean = (ClientPropertiesBean)getModelObject();

			WebRequestCycle requestCycle = (WebRequestCycle)getRequestCycle();
			WebSession session = (WebSession)getSession();
			WebClientInfo clientInfo = new WebClientInfo(requestCycle);

			ClientProperties properties = clientInfo.getProperties();
			propertiesBean.merge(properties);
			session.setClientInfo(clientInfo);
			RequestCycle.get().setRequestTarget(new RedirectRequestTarget(getRequest().getRelativePathPrefixToContextRoot() + continueTo));
		}

	}
	/**
	 * Holds properties of the client.
	 */
	public static class ClientPropertiesBean implements IClusterable
	{
		private static final long serialVersionUID = 1L;

		private String navigatorAppCodeName;
		private String navigatorAppName;
		private String navigatorAppVersion;
		private Boolean navigatorCookieEnabled = Boolean.FALSE;
		private Boolean navigatorJavaEnabled = Boolean.FALSE;
		private String navigatorLanguage;
		private String navigatorPlatform;
		private String navigatorUserAgent;
		private String screenColorDepth;
		private String screenHeight;
		private String screenWidth;
		private String utcOffset;
		private String utcDSTOffset;
		private String browserWidth;
		private String browserHeight;
		private String hostname;

		/**
		 * Gets browserHeight.
		 * 
		 * @return browserHeight
		 */
		public String getBrowserHeight()
		{
			return browserHeight;
		}

		/**
		 * Gets browserWidth.
		 * 
		 * @return browserWidth
		 */
		public String getBrowserWidth()
		{
			return browserWidth;
		}

		/**
		 * Gets navigatorAppCodeName.
		 * 
		 * @return navigatorAppCodeName
		 */
		public String getNavigatorAppCodeName()
		{
			return navigatorAppCodeName;
		}

		/**
		 * Gets navigatorAppName.
		 * 
		 * @return navigatorAppName
		 */
		public String getNavigatorAppName()
		{
			return navigatorAppName;
		}

		/**
		 * Gets navigatorAppVersion.
		 * 
		 * @return navigatorAppVersion
		 */
		public String getNavigatorAppVersion()
		{
			return navigatorAppVersion;
		}

		/**
		 * Gets navigatorCookieEnabled.
		 * 
		 * @return navigatorCookieEnabled
		 */
		public Boolean getNavigatorCookieEnabled()
		{
			return navigatorCookieEnabled;
		}

		/**
		 * Gets navigatorJavaEnabled.
		 * 
		 * @return navigatorJavaEnabled
		 */
		public Boolean getNavigatorJavaEnabled()
		{
			return navigatorJavaEnabled;
		}

		/**
		 * Gets navigatorLanguage.
		 * 
		 * @return navigatorLanguage
		 */
		public String getNavigatorLanguage()
		{
			return navigatorLanguage;
		}

		/**
		 * Gets navigatorPlatform.
		 * 
		 * @return navigatorPlatform
		 */
		public String getNavigatorPlatform()
		{
			return navigatorPlatform;
		}

		/**
		 * Gets navigatorUserAgent.
		 * 
		 * @return navigatorUserAgent
		 */
		public String getNavigatorUserAgent()
		{
			return navigatorUserAgent;
		}

		/**
		 * Gets screenColorDepth.
		 * 
		 * @return screenColorDepth
		 */
		public String getScreenColorDepth()
		{
			return screenColorDepth;
		}

		/**
		 * Gets screenHeight.
		 * 
		 * @return screenHeight
		 */
		public String getScreenHeight()
		{
			return screenHeight;
		}

		/**
		 * Gets screenWidth.
		 * 
		 * @return screenWidth
		 */
		public String getScreenWidth()
		{
			return screenWidth;
		}

		/**
		 * Gets utcOffset.
		 * 
		 * @return utcOffset
		 */
		public String getUtcOffset()
		{
			return utcOffset;
		}

		/**
		 * Merge this with the given properties object.
		 * 
		 * @param properties the properties object to merge with
		 */
		public void merge(ClientProperties properties)
		{
			properties.setNavigatorAppName(navigatorAppName);
			properties.setNavigatorAppVersion(navigatorAppVersion);
			properties.setNavigatorAppCodeName(navigatorAppCodeName);
			properties.setCookiesEnabled((navigatorCookieEnabled != null) ? navigatorCookieEnabled.booleanValue() : false);
			properties.setJavaEnabled((navigatorJavaEnabled != null) ? navigatorJavaEnabled.booleanValue() : false);
			properties.setNavigatorLanguage(navigatorLanguage);
			properties.setNavigatorPlatform(navigatorPlatform);
			properties.setNavigatorUserAgent(navigatorUserAgent);
			properties.setScreenWidth(getInt(screenWidth));
			properties.setScreenHeight(getInt(screenHeight));
			properties.setBrowserWidth(getInt(browserWidth));
			properties.setBrowserHeight(getInt(browserHeight));
			properties.setScreenColorDepth(getInt(screenColorDepth));
			properties.setUtcOffset(utcOffset);
			properties.setUtcDSTOffset(utcDSTOffset);
			properties.setHostname(hostname);
		}

		/**
		 * Sets browserHeight.
		 * 
		 * @param browserHeight browserHeight
		 */
		public void setBrowserHeight(String browserHeight)
		{
			this.browserHeight = browserHeight;
		}

		/**
		 * Sets browserWidth.
		 * 
		 * @param browserWidth browserWidth
		 */
		public void setBrowserWidth(String browserWidth)
		{
			this.browserWidth = browserWidth;
		}

		/**
		 * Sets navigatorAppCodeName.
		 * 
		 * @param navigatorAppCodeName navigatorAppCodeName
		 */
		public void setNavigatorAppCodeName(String navigatorAppCodeName)
		{
			this.navigatorAppCodeName = navigatorAppCodeName;
		}

		/**
		 * Sets navigatorAppName.
		 * 
		 * @param navigatorAppName navigatorAppName
		 */
		public void setNavigatorAppName(String navigatorAppName)
		{
			this.navigatorAppName = navigatorAppName;
		}

		/**
		 * Sets navigatorAppVersion.
		 * 
		 * @param navigatorAppVersion navigatorAppVersion
		 */
		public void setNavigatorAppVersion(String navigatorAppVersion)
		{
			this.navigatorAppVersion = navigatorAppVersion;
		}

		/**
		 * Sets navigatorCookieEnabled.
		 * 
		 * @param navigatorCookieEnabled navigatorCookieEnabled
		 */
		public void setNavigatorCookieEnabled(Boolean navigatorCookieEnabled)
		{
			this.navigatorCookieEnabled = navigatorCookieEnabled;
		}

		/**
		 * Sets navigatorJavaEnabled.
		 * 
		 * @param navigatorJavaEnabled navigatorJavaEnabled
		 */
		public void setNavigatorJavaEnabled(Boolean navigatorJavaEnabled)
		{
			this.navigatorJavaEnabled = navigatorJavaEnabled;
		}

		/**
		 * Sets navigatorLanguage.
		 * 
		 * @param navigatorLanguage navigatorLanguage
		 */
		public void setNavigatorLanguage(String navigatorLanguage)
		{
			this.navigatorLanguage = navigatorLanguage;
		}

		/**
		 * Sets navigatorPlatform.
		 * 
		 * @param navigatorPlatform navigatorPlatform
		 */
		public void setNavigatorPlatform(String navigatorPlatform)
		{
			this.navigatorPlatform = navigatorPlatform;
		}

		/**
		 * Sets navigatorUserAgent.
		 * 
		 * @param navigatorUserAgent navigatorUserAgent
		 */
		public void setNavigatorUserAgent(String navigatorUserAgent)
		{
			this.navigatorUserAgent = navigatorUserAgent;
		}

		/**
		 * Sets screenColorDepth.
		 * 
		 * @param screenColorDepth screenColorDepth
		 */
		public void setScreenColorDepth(String screenColorDepth)
		{
			this.screenColorDepth = screenColorDepth;
		}

		/**
		 * Sets screenHeight.
		 * 
		 * @param screenHeight screenHeight
		 */
		public void setScreenHeight(String screenHeight)
		{
			this.screenHeight = screenHeight;
		}

		/**
		 * Sets screenWidth.
		 * 
		 * @param screenWidth screenWidth
		 */
		public void setScreenWidth(String screenWidth)
		{
			this.screenWidth = screenWidth;
		}

		/**
		 * Sets utcOffset.
		 * 
		 * @param utcOffset utcOffset
		 */
		public void setUtcOffset(String utcOffset)
		{
			this.utcOffset = utcOffset;
		}

		/**
		 * Sets utcDSTOffset.
		 * 
		 * @param utcDSTOffset utcDSTOffset
		 */
		public void setUtcDSTOffset(String utcDSTOffset)
		{
			this.utcDSTOffset = utcDSTOffset;
		}

		/**
		 * Gets utcDSTOffset.
		 * 
		 * @return utcDSTOffset
		 */
		public String getUtcDSTOffset()
		{
			return utcDSTOffset;
		}

		/**
		 * @param hostname
		 *            the hostname shown in the browser.
		 */
		public void setHostname(String hostname)
		{
			this.hostname = hostname;
		}

		/**
		 * @return The clients hostname shown in the browser
		 */
		public String getHostname()
		{
			return hostname;
		}

		private int getInt(String value)
		{
			int intValue = -1;
			try
			{
				intValue = Integer.parseInt(value);
			}
			catch (NumberFormatException e)
			{
				// Do nothing
			}
			return intValue;
		}
	}
}
