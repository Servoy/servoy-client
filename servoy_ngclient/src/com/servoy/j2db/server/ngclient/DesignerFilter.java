/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.WebEntry;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.websocket.IWebsocketSessionFactory;

import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;

/**
 * Filter for designer editor
 * @author gboros
 */
@WebFilter(urlPatterns = { "/designer/*" })
@SuppressWarnings("nls")
public class DesignerFilter extends WebEntry
{
	private String[] locations;

	@Override
	public void init(final FilterConfig fc) throws ServletException
	{
		//when started in developer - init is done in the ResourceProvider filter
		if (!ApplicationServerRegistry.get().isDeveloperStartup())
		{
			try
			{
				InputStream is = fc.getServletContext().getResourceAsStream("/WEB-INF/components.properties");
				Properties properties = new Properties();
				properties.load(is);
				locations = properties.getProperty("locations").split(";");
			}
			catch (Exception e)
			{
				Debug.error("Exception during init components.properties reading", e);
			}
			super.init(fc);
		}
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
	{
		try
		{
			HttpServletRequest request = (HttpServletRequest)servletRequest;
			String uri = request.getRequestURI();
			if (uri != null && uri.endsWith("palette"))
			{
				WebComponentSpecProvider provider = WebComponentSpecProvider.getInstance();

				((HttpServletResponse)servletResponse).setContentType("application/json");
				try
				{
					JSONWriter jsonWriter = new JSONWriter(servletResponse.getWriter());
					jsonWriter.array();
					for (WebComponentSpecification spec : provider.getWebComponentSpecifications())
					{
						jsonWriter.object();
						jsonWriter.key("name").value(spec.getName());
						jsonWriter.key("displayName").value(spec.getDisplayName());
						if (spec.getCategoryName() != null)
						{
							jsonWriter.key("categoryName").value(spec.getCategoryName());
						}
						if (spec.getIcon() != null)
						{
							jsonWriter.key("icon").value(spec.getIcon());
						}
						jsonWriter.endObject();
					}
					jsonWriter.endArray();
				}
				catch (JSONException ex)
				{
					Debug.error("Exception during designe palette generation", ex);
				}

				return;
			}

			super.doFilter(servletRequest, servletResponse, filterChain);
		}
		catch (RuntimeException | Error e)
		{
			Debug.error(e);
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.WebEntry#getWebComponentBundleNames()
	 */
	@Override
	public String[] getWebComponentBundleNames()
	{
		return locations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.WebEntry#createSessionFactory()
	 */
	@Override
	protected IWebsocketSessionFactory createSessionFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
