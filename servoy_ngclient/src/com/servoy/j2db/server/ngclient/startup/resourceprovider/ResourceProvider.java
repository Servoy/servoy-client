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

package com.servoy.j2db.server.ngclient.startup.resourceprovider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.servoy.j2db.server.ngclient.component.WebComponentPackage;
import com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader;
import com.servoy.j2db.server.ngclient.component.WebComponentSpecProvider;
import com.servoy.j2db.server.ngclient.startup.Activator;
import com.servoy.j2db.util.Utils;

/**
 * 
 * Filter that should only be there in a developer environment.
 * @author jcompagner
 */
@WebFilter(urlPatterns = { "/*" })
public class ResourceProvider implements Filter
{
	private static final List<IPackageReader> packageReaders = new ArrayList<>();

	public static void addResources(Collection<IPackageReader> readers)
	{
		packageReaders.addAll(readers);
		initSpecProvider();
	}

	public static void removeResources(Collection<IPackageReader> readers)
	{
		packageReaders.removeAll(readers);
		initSpecProvider();
	}

	private static void initSpecProvider()
	{
		ArrayList<IPackageReader> readers = new ArrayList<>(packageReaders);
		Enumeration<URL> findEntries = Activator.getContext().getBundle().findEntries("/war/", "MANIFEST.MF", true);
		while (findEntries.hasMoreElements())
		{
			readers.add(new URLPackageReader(findEntries.nextElement()));
		}

		WebComponentSpecProvider.init(readers.toArray(new IPackageReader[readers.size()]));
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		initSpecProvider();
	}

	@SuppressWarnings("nls")
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		String pathInfo = ((HttpServletRequest)request).getRequestURI();
		if (pathInfo != null)
		{
			URL url = null;
			if (pathInfo.startsWith("/")) url = Activator.getContext().getBundle().getEntry("/war" + pathInfo);
			else url = Activator.getContext().getBundle().getEntry("/war/" + pathInfo);

			if (url == null)
			{
				int index = pathInfo.indexOf('/', 1);
				if (index > 1)
				{
					for (IPackageReader reader : packageReaders)
					{
						url = reader.getUrlForPath(pathInfo.substring(index));
						if (url != null) break;
					}
				}
			}
			if (url != null)
			{
				URLConnection connection = url.openConnection();
				long lastModifiedTime = connection.getLastModified() / 1000 * 1000;
				((HttpServletResponse)response).setDateHeader("Last-Modified", lastModifiedTime);
				long lm = ((HttpServletRequest)request).getDateHeader("If-Modified-Since");
				if (lm != -1 && lm == lastModifiedTime)
				{
					((HttpServletResponse)response).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					return;
				}

				response.setContentLength(connection.getContentLength());
				if (connection.getContentType() != null && connection.getContentType().indexOf("unknown") == -1)
				{
					response.setContentType(connection.getContentType());
				}
				else
				{
					String file = url.getFile();
					if (file.toLowerCase().endsWith(".js"))
					{
						response.setContentType("text/javascript");
					}
					else if (file.toLowerCase().endsWith(".css"))
					{
						response.setContentType("text/css");
					}
					else if (file.toLowerCase().endsWith(".html"))
					{
						response.setContentType("text/html");
					}
				}
				InputStream is = connection.getInputStream();
				try
				{
					Utils.streamCopy(is, response.getOutputStream());
				}
				finally
				{
					is.close();
				}
			}
			else
			{
				chain.doFilter(request, response);
			}
		}
		else
		{
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy()
	{
	}

	private static class URLPackageReader implements WebComponentPackage.IPackageReader
	{
		private final URL urlOfManifest;
		private final String packageName;

		public URLPackageReader(URL urlOfManifest)
		{
			this.urlOfManifest = urlOfManifest;
			String file = urlOfManifest.getFile();
			int warIndex = file.indexOf("/war/");
			int componentJarIndex = file.indexOf("/", warIndex + 5);
			packageName = file.substring(warIndex + 5, componentJarIndex);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getName()
		 */
		@Override
		public String getName()
		{
			return urlOfManifest.toExternalForm();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getPackageName()
		 */
		@Override
		public String getPackageName()
		{
			return packageName;
		}

		@Override
		public Manifest getManifest() throws IOException
		{
			InputStream is = urlOfManifest.openStream();
			try
			{
				Manifest manifest = new Manifest();
				manifest.read(is);
				return manifest;
			}
			finally
			{
				is.close();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getUrlForPath(java.lang.String)
		 */
		@Override
		public URL getUrlForPath(String path)
		{
			return Activator.getContext().getBundle().getEntry("/war/" + packageName + path); // path includes /
		}

		@SuppressWarnings("nls")
		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			URL url = Activator.getContext().getBundle().getEntry("/war/" + packageName + '/' + path);
			if (url == null) return null;
			InputStream is = null;
			try
			{
				is = url.openStream();
				return Utils.getTXTFileContent(is, charset);
			}
			finally
			{
				if (is != null) is.close();
			}
		}
	}
}
