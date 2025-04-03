/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.server.ngclient.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author emera
 */
public class StatelessLoginUtils
{
	public static final String JWT_Password = "servoy.jwt.logintoken.password";
	public static final String SVYLOGIN_PATH = "svylogin";
	static final Logger log = LoggerFactory.getLogger("stateless.login");

	public static String getServerURL(HttpServletRequest req)
	{
		String scheme = req.getScheme();
		String serverName = req.getServerName();
		int serverPort = req.getServerPort();
		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);
		if (serverPort != 80 && serverPort != 443)
		{
			url.append(":").append(serverPort);
		}
		url.append(HTMLWriter.getPath(req));
		return url.toString();
	}

	public static HttpServletRequest checkForPossibleSavedDeeplink(HttpServletRequest request)
	{
		String state = request.getParameter("state");

		if (state != null && state.contains("="))
		{
			Map<String, String[]> stateParams = new HashMap<>();
			String[] pairs = state.split("&");
			for (String pair : pairs)
			{
				String[] keyValue = pair.split("=", 2);
				try
				{
					String key = URLDecoder.decode(keyValue[0], "UTF-8");
					if ("svyuuid".equals(key))
					{
						continue; // skip svyuuid
					}
					String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], "UTF-8") : "";
					stateParams.put(key, new String[] { value });
				}
				catch (UnsupportedEncodingException e)
				{
					log.error("Error decoding state parameter", e);
				}
			}

			return new HttpServletRequestWrapper(request)
			{
				private final Map<String, String[]> params = stateParams;

				@Override
				public String getParameter(String name)
				{
					String[] values = params.get(name);
					if (values != null && values.length > 0)
					{
						return values[0];
					}
					return super.getParameter(name);
				}

				@Override
				public String[] getParameterValues(String name)
				{
					return params.getOrDefault(name, super.getParameterValues(name));
				}

				@Override
				public Map<String, String[]> getParameterMap()
				{
					Map<String, String[]> combinedParams = new HashMap<>(super.getParameterMap());
					combinedParams.putAll(params);
					return combinedParams;
				}

				@Override
				public String getQueryString()
				{
					StringBuilder queryString = new StringBuilder(super.getQueryString() != null ? super.getQueryString() + "&" : "");
					params.forEach((key, values) -> {
						for (String value : values)
						{
							try
							{
								String encodedKey = URLEncoder.encode(key, "UTF-8");
								String encodedValue = URLEncoder.encode(value, "UTF-8");
								queryString.append(encodedKey).append("=").append(encodedValue).append("&");
							}
							catch (UnsupportedEncodingException e)
							{
								log.error("Error encoding key or value", e);
							}
						}
					});
					if (queryString.length() > 0 && queryString.charAt(queryString.length() - 1) == '&')
					{
						queryString.setLength(queryString.length() - 1);
					}

					return queryString.toString();
				}
			};
		}
		return request;
	}
}
