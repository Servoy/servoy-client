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

import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.resource.UrlResourceStream;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.ValueMap;

import com.servoy.j2db.FlattenedSolution;
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
 * @author jcompagner
 * 
 */
@SuppressWarnings("nls")
public class FormCssResource extends WebResource
{
	private final ServletContext context;

	/**
	 * 
	 */
	public FormCssResource(WebClientsApplication application)
	{
		this.context = application.getWicketFilter().getFilterConfig().getServletContext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.wicket.markup.html.WebResource#setHeaders(org.apache.wicket.protocol.http.WebResponse)
	 */
	@Override
	protected void setHeaders(WebResponse response)
	{
		super.setHeaders(response);
		response.setHeader("Cache-Control", "public, max-age=" + getCacheDuration());
	}

	/**
	 * @see org.apache.wicket.Resource#getResourceStream()
	 */
	@Override
	public IResourceStream getResourceStream()
	{
		String css = "";
		ValueMap params = getParameters();
		Long time = null;
		if (params.size() == 1)
		{
			Iterator iterator = params.entrySet().iterator();
			if (iterator.hasNext())
			{
				Map.Entry entry = (Entry)iterator.next();
				String solutionName = (String)entry.getKey();
				Pair<String, Long> filterTime = filterTime((String)entry.getValue());
				String formName = filterTime.getLeft();
				time = filterTime.getRight();


				String solutionAndForm = solutionName + "/" + formName;
				String templateDir = "default";
				IServiceProvider sp = null;
				Solution solution = null;
				Form form = null;
				if (Session.exists())
				{
					sp = WebClientSession.get().getWebClient();
					if (sp != null)
					{
						FlattenedSolution clientSolution = sp.getFlattenedSolution();
						form = clientSolution.getForm(formName);
					}
					templateDir = WebClientSession.get().getTemplateDirectoryName();
				}

				final String fullpath = "/servoy-webclient/templates/" + templateDir + "/" + solutionAndForm + ".css";
				try
				{
					URL url = context.getResource(fullpath);
					if (url != null)
					{
						return new NullUrlResourceStream(url);
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}


				try
				{
					IApplicationServerSingleton as = ApplicationServerSingleton.get();
					RootObjectMetaData sd = as.getLocalRepository().getRootObjectMetaData(solutionName, IRepository.SOLUTIONS);
					if (sd != null)
					{
						solution = (Solution)as.getLocalRepository().getActiveRootObject(sd.getRootObjectId());
						if (form == null)
						{
							form = solution.getForm(formName);
						}
					}
					Pair<String, String> formHTMLAndCSS = TemplateGenerator.getFormHTMLAndCSS(solution, form, sp);
					css = formHTMLAndCSS.getRight();
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		StringResourceStream stream = new StringResourceStream(css, "text/css"); //$NON-NLS-1$
		stream.setLastModified(time != null ? Time.valueOf(time.longValue()) : null);
		return stream;
	}

	private Pair<String, Long> filterTime(String name)
	{
		int index1 = name.lastIndexOf("t."); //$NON-NLS-1$
		Long time = null;
		if (index1 != -1)
		{
			int index2 = name.lastIndexOf("_t", index1); //$NON-NLS-1$
			if (index2 != -1)
			{
				String tst = name.substring(index2 + 2, index1);
				// test if this can be converted to a String.
				try
				{
					time = new Long(tst);
					name = name.substring(0, index2);
				}
				catch (RuntimeException re)
				{
					// ignore runtime exceptions, wasn't a time.
				}
			}
		}
		return new Pair<String, Long>(name, time);
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
			if (time.getMilliseconds() != 0) return time;
			return Time.now();
		}
	}
}
