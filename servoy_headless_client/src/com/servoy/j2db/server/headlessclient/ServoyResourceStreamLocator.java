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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;

import javax.servlet.ServletContext;

import org.apache.wicket.Session;
import org.apache.wicket.util.resource.AbstractStringResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.UrlResourceStream;
import org.apache.wicket.util.resource.locator.IResourceStreamLocator;
import org.apache.wicket.util.resource.locator.ResourceStreamLocator;
import org.apache.wicket.util.time.Time;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;

/**
 * Serves the static resources and templates
 * 
 * @author jcompagner
 */
public final class ServoyResourceStreamLocator implements IResourceStreamLocator
{
	public static final String[] editablePages = { "SelectSolution", "MainPage", "SignIn", "ServoyExpiredPage", "ServoyServerToBusyPage", "UnsupportedBrowserPage" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	private final IResourceStreamLocator classLoaderResourceStreamLocator = new ResourceStreamLocator();
	private final IResourceStreamLocator templateResourceStreamLocator = new ResourceStreamLocator()
	{
		@Override
		public IResourceStream locate(Class< ? > clazz, String pathname)
		{
			if (clazz == ServoyPageExpiredPage.class || clazz == ServoyErrorPage.class)
			{
				try
				{
					URL url = context.getResource("/servoy-webclient/templates/default/" + pathname); //$NON-NLS-1$
					if (url != null)
					{
						return new NullUrlResourceStream(url)
						{
							private static final long serialVersionUID = 1L;

							@Override
							public Time lastModifiedTime()
							{
								return Time.now();
							}
						};
					}
				}
				catch (MalformedURLException ex)
				{
					Debug.trace("Load failed for " + pathname + " (ignored, getting default)"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			else if (pathname.startsWith("com/servoy/j2db/server/headlessclient/WebForm_")) //$NON-NLS-1$
			{
				int index = pathname.indexOf("form::"); //$NON-NLS-1$
				if (index != -1)
				{
					int index2 = pathname.indexOf("::form", index + 6); //$NON-NLS-1$
					try
					{
						String solutionAndForm = pathname.substring(index + 6, index2);
						solutionAndForm = solutionAndForm.replace(':', '/');
						String templateDir = pathname.substring(index2 + 7);
						int index3 = templateDir.indexOf("$");//this is a bit strange, but the most easy way //$NON-NLS-1$
						if (index3 != -1) templateDir = templateDir.substring(0, index3);
						final String fullpath = "/servoy-webclient/templates/" + templateDir + "/" + solutionAndForm + ".html"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						URL url = context.getResource(fullpath);
						if (url != null)
						{
							return new NullUrlResourceStream(url);
						}

						String[] names = solutionAndForm.split("/"); //$NON-NLS-1$
						if (names != null && names.length == 2)
						{
							try
							{
								IApplicationServerSingleton as = ApplicationServerSingleton.get();
								RootObjectMetaData sd = as.getLocalRepository().getRootObjectMetaData(names[0], IRepository.SOLUTIONS);
								if (sd != null)
								{
									Solution sol = (Solution)as.getLocalRepository().getActiveRootObject(sd.getRootObjectId());
									IServiceProvider sp = null;
									Form form = null;
									if (Session.exists())
									{
										sp = ((WebClientSession)Session.get()).getWebClient();
										if (sp != null)
										{
											IForm fc = ((WebClient)sp).getFormManager().getForm(names[1]);
											if (fc instanceof FormController)
											{
												FlattenedSolution clientSolution = sp.getFlattenedSolution();
												form = clientSolution.getForm(((FormController)fc).getForm().getName());
											}
										}
									}


									if (form == null)
									{
										form = sol.getForm(names[1]);
									}

									if (form != null)
									{
										Pair<String, String> pair = TemplateGenerator.getFormHTMLAndCSS(sol, form, sp, names[1]);
										String html = pair.getLeft();
										return new TemplateResourceStream(html, sd.getRootObjectId(), fullpath);
									}
									else
									{
										Debug.error("No form found for " + names[1] + " of solution " + names[0]); //$NON-NLS-1$ //$NON-NLS-2$
									}
								}
								else
								{
									Debug.error("No solution found for " + names[0]); //$NON-NLS-1$
								}
							}
							catch (Exception ex)
							{
								Debug.error("No solution and form found from " + solutionAndForm, ex); //$NON-NLS-1$
							}
						}
						else
						{
							Debug.error("No solution and form found from " + solutionAndForm); //$NON-NLS-1$
						}
					}
					catch (MalformedURLException ex)
					{
						Debug.error(ex);
					}
				}
			}
			else if (testIfEditablePage(pathname))
			{
				String name = pathname.substring("com/servoy/j2db/server/headlessclient/".length()); //$NON-NLS-1$
				try
				{
					URL url = context.getResource("/servoy-webclient/templates/default/" + name); //$NON-NLS-1$
					if (url != null)
					{
						return new NullUrlResourceStream(url);
					}
				}
				catch (MalformedURLException ex)
				{
					Debug.trace("Load failed for " + name + " (ignored, getting default)"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			return null;
		}
	};

	private final ServletContext context;

	private boolean testIfEditablePage(String pathname)
	{
		for (String element : editablePages)
		{
			if (pathname.startsWith("com/servoy/j2db/server/headlessclient/" + element)) //$NON-NLS-1$
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Constructor
	 * 
	 * @param finder The finder to search
	 */
	public ServoyResourceStreamLocator(WebClientsApplication application)
	{
		super();
		this.context = application.getWicketFilter().getFilterConfig().getServletContext();
	}

	/**
	 * @see wicket.util.resource.locator.IResourceStreamLocator#locate(java.lang.Class, java.lang.String, java.lang.String, java.util.Locale, java.lang.String)
	 */
	public IResourceStream locate(Class< ? > clazz, String path, String style, Locale locale, String extension)
	{
		IResourceStream stream = templateResourceStreamLocator.locate(clazz, path, style, locale, extension);
		if (stream == null)
		{
			return classLoaderResourceStreamLocator.locate(clazz, path, style, locale, extension);
		}
		return stream;
	}

	/**
	 * @see wicket.util.resource.locator.IResourceStreamLocator#locate(java.lang.Class, java.lang.String)
	 */
	public IResourceStream locate(Class< ? > clazz, String path)
	{
		IResourceStream stream = templateResourceStreamLocator.locate(clazz, path);
		if (stream == null)
		{
			return classLoaderResourceStreamLocator.locate(clazz, path);
		}
		return stream;
	}

	private class TemplateResourceStream extends AbstractStringResourceStream
	{
		private static final long serialVersionUID = 1L;

		private final String html;
		private final int sol_id;
		private final String fullpath;

		TemplateResourceStream(String html, int sol_id, String fullpath)
		{
			this.html = html;
			this.sol_id = sol_id;
			this.fullpath = fullpath;
			setCharset(Charset.forName("UTF8")); //$NON-NLS-1$
		}

		/**
		 * @see wicket.util.resource.AbstractStringResourceStream#getString()
		 */
		@Override
		protected String getString()
		{
			return html;
		}

		/**
		 * @see wicket.util.resource.AbstractStringResourceStream#lastModifiedTime()
		 */
		@Override
		public Time lastModifiedTime()
		{
			try
			{
				URL url = context.getResource(fullpath);
				if (url != null)
				{
					return Time.valueOf(url.openConnection().getLastModified());
				}
			}
			catch (Exception ex)
			{
			}
			try
			{
				Solution sol = (Solution)ApplicationServerSingleton.get().getLocalRepository().getActiveRootObject(sol_id);
				if (sol != null)
				{
					return Time.valueOf(sol.getLastModifiedTime());
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
			return Time.now();
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			try
			{
				Solution sol = (Solution)ApplicationServerSingleton.get().getLocalRepository().getActiveRootObject(sol_id);
				if (sol != null)
				{
					return "Markup[solution:" + sol.getName() + ", fullpath:" + fullpath + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
			return "Markup[solution:<unknown>, fullpath:" + fullpath + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static class NullUrlResourceStream extends UrlResourceStream
	{
		private static final long serialVersionUID = 1L;

		/**
		 * @param url
		 */
		public NullUrlResourceStream(URL url)
		{
			super(url);
		}

		/**
		 * @see wicket.util.resource.UrlResourceStream#lastModifiedTime()
		 */
		@Override
		public Time lastModifiedTime()
		{
			Time time = super.lastModifiedTime();
			if (time != null && time.getMilliseconds() != 0) return time;
			return Time.now();
		}
	}
}
