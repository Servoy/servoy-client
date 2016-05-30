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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.sablo.specification.Package;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.websocket.WebsocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.ngclient.property.types.Types;
import com.servoy.j2db.server.ngclient.startup.Activator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Filter that should only be there in a developer environment.
 *
 * @author jcompagner
 */
@WebFilter(urlPatterns = { "/*" })
public class ResourceProvider implements Filter
{
	private static final Logger log = LoggerFactory.getLogger(ResourceProvider.class.getCanonicalName());

	private static final Map<String, IPackageReader> componentReaders = new ConcurrentHashMap<>();
	private static final Map<String, IPackageReader> serviceReaders = new ConcurrentHashMap<>();
	private static final List<String> removePackageNames = new ArrayList<String>();

	private static String getName(IPackageReader reader)
	{
		if (reader.getPackageName() != null) return reader.getPackageName();
		String name = reader.getName();
		int index = name.lastIndexOf('/');
		if (index == -1) index = name.lastIndexOf('\\');
		if (index != -1) name = name.substring(index + 1);
		// strip off the zip or jar extension
		if (name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip"))
		{
			name = name.substring(0, name.length() - 4);
		}
		return name;
	}

	public synchronized static void updateComponentResources(Collection<String> toRemove, Collection<IPackageReader> toAdd)
	{
		for (String reader : toRemove)
		{
			componentReaders.remove(reader);
		}
		for (IPackageReader reader : toAdd)
		{
			componentReaders.put(getName(reader), reader);
		}

		WebComponentSpecProvider webComponentSpecProvider = WebComponentSpecProvider.getInstance();
		if (webComponentSpecProvider != null) webComponentSpecProvider.updatePackages(toRemove, toAdd);
		else initSpecProvider();
	}

	public synchronized static void updateServiceResources(Collection<String> toRemove, Collection<IPackageReader> toAdd)
	{
		for (String reader : toRemove)
		{
			serviceReaders.remove(reader);
		}
		for (IPackageReader reader : toAdd)
		{
			serviceReaders.put(getName(reader), reader);
		}

		WebServiceSpecProvider webServiceSpecProvider = WebServiceSpecProvider.getInstance();
		if (webServiceSpecProvider != null) webServiceSpecProvider.updatePackages(toRemove, toAdd);
		else initSpecProvider();
	}

	public static Set<String> getDefaultPackageNames()
	{
		Set<String> result = new HashSet<String>();
		Enumeration<String> paths = Activator.getContext().getBundle().getEntryPaths("/war/");
		while (paths.hasMoreElements())
		{
			String name = paths.nextElement().replace("war/", "");
			if (name.endsWith("/") && !name.equals("js/") && !name.equals("css/") && !name.equals("templates/"))
			{
				result.add(name.replace("/", ""));
			}
		}
		return result;
	}

	public synchronized static void setRemovedPackages(List<String> packageNames)
	{
		removePackageNames.clear();
		removePackageNames.addAll(packageNames);
	}

	private synchronized static void initSpecProvider()
	{
		//register the session factory at the manager
		if (WebsocketSessionManager.getWebsocketSessionFactory(WebsocketSessionFactory.CLIENT_ENDPOINT) == null)
		{
			WebsocketSessionManager.setWebsocketSessionFactory(WebsocketSessionFactory.CLIENT_ENDPOINT, new WebsocketSessionFactory());
		}

		registerTypes();

		List<IPackageReader> componentPackages = new ArrayList<>(componentReaders.values());
		List<IPackageReader> servicePackages = new ArrayList<>(serviceReaders.values());

		for (URL url : Utils.iterate(Activator.getContext().getBundle().findEntries("/war/", "MANIFEST.MF", true)))
		{
			String pathPrefix = url.getPath().substring(0, url.getPath().indexOf("/META-INF/MANIFEST.MF"));
			IPackageReader reader = new BundlePackageReader(Activator.getContext().getBundle(), url, pathPrefix);
			if (removePackageNames.contains(reader.getPackageName())) continue;
			if (pathPrefix.endsWith("services"))
			{
				servicePackages.add(reader);
			}
			else
			{
				componentPackages.add(reader);
			}
		}

		// Add sablo services and components
		Bundle sabloBundle = org.sablo.startup.Activator.getDefault().getContext().getBundle();
		BundlePackageReader sabloReader = new BundlePackageReader(sabloBundle, sabloBundle.getEntry("META-INF/MANIFEST.MF"), "META-INF/resources");
		servicePackages.add(sabloReader);
		componentPackages.add(sabloReader);

		WebComponentSpecProvider.init(componentPackages.toArray(new IPackageReader[componentPackages.size()]));
		WebServiceSpecProvider.init(servicePackages.toArray(new IPackageReader[servicePackages.size()]));
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		initSpecProvider();
	}

	private static void registerTypes()
	{
		Types.getTypesInstance().registerTypes();
	}

	@SuppressWarnings("nls")
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		String pathInfo = ((HttpServletRequest)request).getRequestURI();
		if (pathInfo != null && !pathInfo.equals("/"))
		{

			Bundle bundle;
			try
			{
				bundle = Activator.getContext().getBundle();
			}
			catch (IllegalStateException e)
			{
				// Context not valid
				chain.doFilter(request, response);
				return;
			}
			URL url = computeURL(pathInfo, bundle);

			if (url != null)
			{
				URLConnection connection = url.openConnection();
				connection.setUseCaches(false);
				if (connection instanceof JarURLConnection)
				{
					JarFile jarFile = ((JarURLConnection)connection).getJarFile();
					try
					{
						String file = ((JarURLConnection)connection).getEntryName();
						ZipEntry entry = jarFile.getEntry(file);
						if (testLastModified(request, response, entry.getTime() / 1000 * 1000)) return;
						response.setContentLength((int)entry.getSize());
						setContentType(response, file);
						try (InputStream is = jarFile.getInputStream(entry))
						{
							streamContent(response, file, is);
						}
					}
					finally
					{
						jarFile.close();
					}
				}
				else
				{
					if (testLastModified(request, response, connection.getLastModified() / 1000 * 1000)) return;
					response.setContentLength(connection.getContentLength());

					if (connection.getContentType() != null && connection.getContentType().indexOf("unknown") == -1)
					{
						response.setContentType(connection.getContentType());
					}
					else
					{
						setContentType(response, url.getFile());
					}
					try (InputStream is = connection.getInputStream())
					{
						streamContent(response, url.getFile(), is);
					}
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

	/**
	 * @param response
	 * @param url
	 * @param is
	 * @throws IOException
	 */
	private void streamContent(ServletResponse response, String file, InputStream is) throws IOException
	{
		String compileLessWithNashorn = null;
		if (file.toLowerCase().endsWith(".less") && (compileLessWithNashorn = compileLessWithNashorn(is)) != null)
		{
			response.setContentType("text/css");
			response.setContentLength(compileLessWithNashorn.length());
			response.getWriter().print(compileLessWithNashorn);
		}
		else
		{
			Utils.streamCopy(is, response.getOutputStream());
		}
	}

	/**
	 * @param response
	 * @param file
	 */
	private void setContentType(ServletResponse response, String file)
	{
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

	/**
	 * @param request
	 * @param response
	 * @param lastModifiedTime
	 */
	private boolean testLastModified(ServletRequest request, ServletResponse response, long lastModifiedTime)
	{
		((HttpServletResponse)response).setDateHeader("Last-Modified", lastModifiedTime);
		((HttpServletResponse)response).setHeader("Cache-Control", "max-age=0, must-revalidate, proxy-revalidate"); //HTTP 1.1
		long lm = ((HttpServletRequest)request).getDateHeader("If-Modified-Since");
		if (lm != -1 && lm == lastModifiedTime)
		{
			((HttpServletResponse)response).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return true;
		}
		return false;
	}


	private static String getText(InputStream is) throws IOException
	{
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
		StringBuilder result = new StringBuilder();
		String inputLine;
		while ((inputLine = bufferedReader.readLine()) != null)
			result.append(inputLine);
		bufferedReader.close();
		return result.toString();
	}


	public static String compileLessWithNashorn(InputStream is)
	{
		//we have to pass in null as classloader if we want to acess the java 8 nashorn
		ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
		if (engine != null)
		{
			try
			{
				Invocable invocable = (Invocable)engine;
				invocable.invokeFunction("load", ResourceProvider.class.getResource("js/less-2.5.1.js"));
				invocable.invokeFunction("load", ResourceProvider.class.getResource("js/less-env-2.5.1.js"));
				invocable.invokeFunction("load", ResourceProvider.class.getResource("js/lessrunner.js"));
				Object result = invocable.invokeFunction("convert", getText(is));
				return result.toString();
			}
			catch (ScriptException e)
			{
				Debug.log(e);
			}
			catch (NoSuchMethodException e)
			{
				Debug.log(e);
			}
			catch (Exception e)
			{
				Debug.log(e);
			}
		}
		return null;
	}

	public synchronized static URL computeURL(String pathInfo, Bundle bundle) throws UnsupportedEncodingException, MalformedURLException
	{
		URL url = null;
		try
		{
			if (pathInfo.startsWith("/")) url = bundle.getEntry("/war" + pathInfo);
			else url = bundle.getEntry("/war/" + pathInfo);
		}
		catch (Exception e)
		{
			Debug.log("can't get zip entry '" + pathInfo + "'from bundle: " + bundle, e);
		}
		if (url == null)
		{
			int index = pathInfo.indexOf('/', 1);
			if (index > 1 && !pathInfo.substring(index).equals("/"))
			{
				String packageName = URLDecoder.decode(pathInfo.substring(0, index), "UTF8");
				packageName = packageName.startsWith("/") ? packageName.substring(1) : packageName;
				IPackageReader reader = componentReaders.get(packageName);
				if (reader != null)
				{
					url = reader.getUrlForPath(pathInfo.substring(index));
					if (url == null)
					{
						Debug.error("url '" + pathInfo.substring(index) + "' for package: '" + packageName + "' is not found in the component package");
					}
				}
				else
				{
					reader = serviceReaders.get(packageName);
					if (reader != null)
					{
						url = reader.getUrlForPath(pathInfo.substring(index));
						if (url == null)
						{
							Debug.error("url '" + pathInfo.substring(index) + "' for package: '" + packageName + "' is not found in the service package");
						}
					}
				}
			}
		}

		if (url == null)
		{
			url = org.sablo.startup.Activator.getDefault().getResource(pathInfo);
		}
		return url;
	}

	@Override
	public void destroy()
	{
	}

	private static class BundlePackageReader implements Package.IPackageReader
	{
		private final URL urlOfManifest;
		private final Bundle bundle;
		private final String pathPrefix;

		public BundlePackageReader(Bundle bundle, URL urlOfManifest, String pathPrefix)
		{
			this.bundle = bundle;
			this.urlOfManifest = urlOfManifest;
			this.pathPrefix = pathPrefix;
		}

		@Override
		public String getName()
		{
			return urlOfManifest.toExternalForm();
		}

		@Override
		public String getPackageName()
		{
			try
			{
				String packageName = Package.getPackageName(getManifest());
				if (packageName != null) return packageName;
			}
			catch (IOException e)
			{
				Debug.log(e);
			}

			// fall back to based on directory name
			String[] split = pathPrefix.split("/");
			return split[split.length - 1];
		}

		@Override
		public String getPackageDisplayname()
		{
			try
			{
				String packageDisplayname = Package.getPackageDisplayname(getManifest());
				if (packageDisplayname != null) return packageDisplayname;
			}
			catch (IOException e)
			{
				Debug.log(e);
			}

			// fall back to symbolic name
			return getPackageName();
		}

		@Override
		public String getVersion()
		{
			try
			{
				return getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
			}
			catch (IOException e)
			{
			}
			return null;
		}

		@Override
		public Manifest getManifest() throws IOException
		{
			try (InputStream is = urlOfManifest.openStream())
			{
				return new Manifest(is);
			}
		}

		@Override
		public URL getUrlForPath(String path)
		{
			// when pathprefix is already in path, do not add pathprefix
			return bundle.getEntry(path.startsWith(pathPrefix) ? path : pathPrefix + (path.startsWith("/") ? path : '/' + path));
		}

		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			URL url = getUrlForPath(path);
			if (url == null) return null;

			try (InputStream is = url.openStream())
			{
				return Utils.getTXTFileContent(is, charset);
			}
		}

		@Override
		public void reportError(String specpath, Exception e)
		{
			log.error("Cannot parse spec file '" + specpath + "' from package 'BundlePackageReader[ " + urlOfManifest + " ]'. ", e);
		}

		@Override
		public URL getPackageURL()
		{
			return null;
		}

		@Override
		public String getPackageType()
		{
			try
			{
				return Package.getPackageType(getManifest());
			}
			catch (IOException e)
			{
				log.error("Error getting package type." + getName(), e);
			}
			return null;
		}

		@Override
		public String toString()
		{
			return "Bundle ng package: " + getName();
		}

		@Override
		public File getResource()
		{
			return null;
		}

	}


	public static String getServicePackageNameForFile(File file)
	{
		for (Entry<String, IPackageReader> entry : serviceReaders.entrySet())
		{
			File resource = entry.getValue().getResource();
			if (resource != null && resource.equals(file)) return entry.getKey();
		}
		return null;
	}

	public static String getComponentPackageNameForFile(File file)
	{
		for (Entry<String, IPackageReader> entry : componentReaders.entrySet())
		{
			File resource = entry.getValue().getResource();
			if (resource != null && resource.equals(file)) return entry.getValue().getPackageName();
		}
		return null;
	}
}
