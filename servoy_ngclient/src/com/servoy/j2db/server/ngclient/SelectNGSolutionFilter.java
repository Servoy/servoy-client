/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HTTPUtils;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Provides an entry page that lists all available ng client solutions on the app. server.
 * I will not work if property 'servoy.allowSolutionBrowsing' is set to false.
 *
 * @author acostescu
 */
@WebFilter(urlPatterns = { "/servoy-ngclient/solutions.js", "/servoy-ngclient" })
@SuppressWarnings("nls")
public class SelectNGSolutionFilter implements Filter
{

	private static final String CONTRIBUTIONS = "<!-- contributions -->";

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
	{
		if (Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.allowSolutionBrowsing", "true")))
		{
			HttpServletRequest request = (HttpServletRequest)servletRequest;
			String uri = request.getServletPath();
			if (uri != null)
			{
				if (uri.equals("/servoy-ngclient"))
				{
					// html contents
					((HttpServletResponse)servletResponse).setContentType("text/html");

					PrintWriter w = servletResponse.getWriter();
					addNeededJSAndCSS(getClass().getResource("solution_list.html"), w);
					w.flush();
					return;
				}
				else if (uri.equals("/servoy-ngclient/solutions.js"))
				{
					HTTPUtils.setNoCacheHeaders((HttpServletResponse)servletResponse);

					IApplicationServerSingleton as = ApplicationServerRegistry.get();
					// js contents giving the actual solutions list
					List<Solution> ngCompatibleSolutions = new ArrayList<Solution>();
					if (as.isDeveloperStartup())
					{
						Solution active = as.getDebugClientHandler().getDebugSmartClient().getCurrent();
						if ((((SolutionMetaData)active.getMetaData()).getSolutionType() & (SolutionMetaData.SOLUTION | SolutionMetaData.NG_CLIENT_ONLY)) != 0)
							ngCompatibleSolutions.add(active);
					}
					else
					{
						try
						{
							RootObjectMetaData[] smds = as.getLocalRepository().getRootObjectMetaDatasForType(IRepository.SOLUTIONS);
							int solutionType;
							for (RootObjectMetaData element : smds)
							{
								solutionType = ((SolutionMetaData)element).getSolutionType();
								if ((solutionType & (SolutionMetaData.SOLUTION | SolutionMetaData.NG_CLIENT_ONLY)) > 0)
								{
									Solution solution = (Solution)as.getLocalRepository().getActiveRootObject(element.getRootObjectId());
									if (solution != null)
									{
										ngCompatibleSolutions.add(solution);
									}
								}
							}
						}
						catch (RepositoryException e)
						{
							Debug.error(e);
						}
					}

					// now generate the js containing these solutions

					((HttpServletResponse)servletResponse).setContentType("text/javascript");

					PrintWriter w = servletResponse.getWriter();
					w.println("angular.module('solutionsListModule', []).value('$solutionsList', {");
					w.println("    ngSolutions: [");
					boolean putComma = false;
					for (Solution s : ngCompatibleSolutions)
					{
						if (putComma) w.println(",");
						else putComma = true;
						String titleText = (s.getTitleText() != null ? "'" + s.getTitleText() + "'" : null);
						if (titleText != null && (titleText.contains("#") || titleText.startsWith("i18n:"))) titleText = null; // it wouldn't look nice in the list of solutions to pick + client can't convert those

						w.print("        { name : '" + s.getName() + "', titleText : " + titleText + ", requiresAuth : " + s.requireAuthentication() + " }");
					}
					if (putComma) w.println("");
					w.println("    ]");
					w.println("});");

					w.flush();
					return;
				}
			}
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	protected void addNeededJSAndCSS(URL resource, Writer writer) throws IOException
	{
		String htmlAsString = IOUtils.toString(resource);
		htmlAsString = htmlAsString.replace(CONTRIBUTIONS, getNeededJSAndCSS());
		writer.append(htmlAsString);
	}

	protected CharSequence getNeededJSAndCSS()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("<script src=\"%s\"></script>\n", NGClientEntryFilter.ANGULAR_JS));
		sb.append(String.format("<link rel=\"stylesheet\" href=\"%s\"/>\n", NGClientEntryFilter.BOOTSTRAP_CSS));
		return sb;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		// nothing to do there
	}

	@Override
	public void destroy()
	{
		// nothing to do there
	}

}
