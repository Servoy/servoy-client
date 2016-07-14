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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author emera
 */
@WebFilter(urlPatterns = { "/wro/*" })
public class NGCachingFilter implements Filter
{
	private String group_id;
	private static final int ONE_YEAR_MAX_AGE = 60 * 60 * 24 * 31 * 12;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		group_id = (String)filterConfig.getServletContext().getAttribute(NGClientEntryFilter.SVYGRP);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		if (group_id != null)
		{
			HttpServletRequest req = (HttpServletRequest)request;
			HttpServletResponse resp = (HttpServletResponse)response;
			String uri = req.getRequestURI();
			if (uri != null && (uri.endsWith(group_id + ".js") || uri.endsWith(group_id + ".css")))
			{
				resp.addHeader("Cache-Control", "public, max-age=" + ONE_YEAR_MAX_AGE);
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy()
	{
		group_id = null;
	}

}
