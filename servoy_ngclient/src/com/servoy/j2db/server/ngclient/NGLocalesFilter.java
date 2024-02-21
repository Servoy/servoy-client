/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;

import com.servoy.j2db.util.MimeTypes;

/**
 * @author jcompagner
 * @since 2024.03
 *
 */
@SuppressWarnings("nls")
@WebFilter(urlPatterns = { "/locales/*" }, dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD })
public class NGLocalesFilter implements Filter
{
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		if (request.getParameter("localeid") != null)
		{
			// this code is a bit of copy of the IndexPageFilter locale handling part.
			String requestURI = ((HttpServletRequest)request).getServletPath();
			Path normalize = Paths.get(requestURI).normalize();
			Set<String> resourcePaths = request.getServletContext().getResourcePaths(normalize.getParent().toString().replace('\\', '/'));
			if (resourcePaths != null)
			{
				String[] locales = generateLocaleIds(request.getParameter("localeid"));
				List<String> listFiles = resourcePaths.stream().filter((name) -> {
					for (String locale : locales)
					{
						if (name.contains(locale)) return true;
					}
					return false;
				}).collect(Collectors.toList());
				if (listFiles != null && listFiles.size() > 0)
				{
					if (listFiles.size() > 1)
					{
						listFiles.sort((a, b) -> {
							return b.length() - a.length();
						});
					}
					String contentType = MimeTypes.guessContentTypeFromName(requestURI);
					if (contentType != null) response.setContentType(contentType);
					InputStream resourceAsStream = request.getServletContext().getResourceAsStream(listFiles.get(0));
					try (InputStream is = resourceAsStream)
					{
						IOUtils.copy(is, response.getOutputStream());
					}
					return;
				}
			}
		}
		chain.doFilter(request, response);
	}


	public static String[] generateLocaleIds(String locale)
	{
		String[] languageAndCountry = locale.split("-");
		if (languageAndCountry.length == 1)
		{
			languageAndCountry = locale.split("_");
		}
		if (languageAndCountry.length == 1)
		{
			String country = locale.toUpperCase();
			return new String[] { locale + '.', locale + '_' + country + '.', locale + '-' + country + '.' };
		}
		return new String[] { locale.replace('-', '_') + '.', locale.replace('_', '-') + '.', languageAndCountry[0] + '.' };
	}

}
