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
import java.nio.file.Paths;

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
		String localeId = request.getParameter("localeid");
		if (localeId != null)
		{
			// this code is a bit of copy of the IndexPageFilter locale handling part.
			String requestURI = ((HttpServletRequest)request).getServletPath();
			String normalize = Paths.get(requestURI).normalize().toString().replace('\\', '/');
			if (normalize.startsWith("/locales/"))
			{
				InputStream resourceAsStream = request.getServletContext().getResourceAsStream(normalize);
				if (resourceAsStream == null)
				{
					String[] locales = generateLocaleIds(localeId);
					for (String locale : locales)
					{
						String localeNormalized = Paths.get(requestURI.replace(localeId, locale)).normalize().toString().replace('\\', '/');
						if (localeNormalized.startsWith("/locales/"))
						{
							resourceAsStream = request.getServletContext().getResourceAsStream(localeNormalized);
							if (resourceAsStream != null) break;
						}
					}
				}
				if (resourceAsStream != null)
				{
					String contentType = MimeTypes.guessContentTypeFromName(requestURI);
					if (contentType != null) response.setContentType(contentType);
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
			return new String[] { locale + '_' + country, locale + '-' + country, locale, "en" };
		}
		return new String[] { locale.replace('-', '_'), locale.replace('_', '-'), languageAndCountry[0], "en" };
	}

}
