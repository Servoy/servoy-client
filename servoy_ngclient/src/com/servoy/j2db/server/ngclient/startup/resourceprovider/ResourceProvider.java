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
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.DispatcherType;
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

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.WebObjectRegistry;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.ngclient.property.types.Types;
import com.servoy.j2db.server.ngclient.startup.Activator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HTTPUtils;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Utils;

/**
 * Filter that should only be there in a developer environment.
 *
 * @author jcompagner
 */
@WebFilter(urlPatterns = { "/*" }, dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD })
public class ResourceProvider implements Filter
{
	private static final String SERVOY_LESS_PATH = "resources/servoy.less";
	public static final String PROPERTIES_LESS = "servoy_theme_properties.less";

	private static final Logger log = LoggerFactory.getLogger(ResourceProvider.class.getCanonicalName());

	// TODO add comment; what is the key? resource name, package name, ...?
	private static final Map<String, List<IPackageReader>> componentReaders = new ConcurrentHashMap<>();
	private static final Map<String, List<IPackageReader>> serviceReaders = new ConcurrentHashMap<>();
	private static final List<String> removePackageNames = new ArrayList<String>();
	private static Invocable invocable;

	private final File templatesDir = new File(ApplicationServerRegistry.get().getServoyApplicationServerDirectory(), "server/webapps/ROOT/templates");

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

	public synchronized static void setPackages(Collection<List<IPackageReader>> newPackages)
	{
		List<IPackageReader> toRemoveComponentNames = new ArrayList<>();
		List<IPackageReader> toRemoveServiceNames = new ArrayList<>();
		for (List<IPackageReader> lst : componentReaders.values())
		{
			for (IPackageReader reader : lst)
			{
				toRemoveComponentNames.add(reader);
			}
		}
		for (List<IPackageReader> lst : serviceReaders.values())
		{
			for (IPackageReader reader : lst)
			{
				toRemoveServiceNames.add(reader);
			}
		}
		componentReaders.clear();
		serviceReaders.clear();
		for (IPackageReader reader : flatten(newPackages))
		{
			if (IPackageReader.WEB_SERVICE.equals(reader.getPackageType()))
			{
				addReader(serviceReaders, reader);
			}
			else
			{
				addReader(componentReaders, reader);
			}
		}

		WebComponentSpecProvider webComponentSpecProvider = WebComponentSpecProvider.getInstance();
		WebServiceSpecProvider webServiceSpecProvider = WebServiceSpecProvider.getInstance();
		if (webComponentSpecProvider == null || webServiceSpecProvider == null) initSpecProvider();
		if (webComponentSpecProvider != null) webComponentSpecProvider.updatePackages(toRemoveComponentNames, flatten(componentReaders.values()));
		if (webServiceSpecProvider != null) webServiceSpecProvider.updatePackages(toRemoveServiceNames, flatten(serviceReaders.values()));

	}

	/**
	 * @param reader
	 * @param name
	 */
	private static void addReader(Map<String, List<IPackageReader>> map, IPackageReader reader)
	{
		String name = getName(reader);
		List<IPackageReader> list = map.get(name);
		if (list == null)
		{
			list = new ArrayList<>(3);
			map.put(name, list);
		}
		list.add(reader);
	}

	/**
	 * @param values
	 * @return
	 */
	private static List<IPackageReader> flatten(Collection<List<IPackageReader>> values)
	{
		List<IPackageReader> lst = new ArrayList<>();
		for (List<IPackageReader> list : values)
		{
			lst.addAll(list);
		}
		return lst;
	}


	public synchronized static void updatePackageResources(Collection<IPackageReader> componentsToRemove, Collection<IPackageReader> componentsToAdd,
		Collection<IPackageReader> servicesToRemove, Collection<IPackageReader> servicesToAdd)
	{
		for (IPackageReader reader : componentsToRemove)
		{
			List<IPackageReader> list = componentReaders.get(reader.getPackageName());
			list.remove(reader);
		}
		for (IPackageReader reader : componentsToAdd)
		{
			addReader(componentReaders, reader);
		}

		for (IPackageReader reader : servicesToRemove)
		{
			List<IPackageReader> list = serviceReaders.get(reader.getPackageName());
			list.remove(reader);
		}
		for (IPackageReader reader : servicesToAdd)
		{
			addReader(serviceReaders, reader);
		}

		WebComponentSpecProvider webComponentSpecProvider = WebComponentSpecProvider.getInstance();
		if (webComponentSpecProvider != null)
		{
			WebServiceSpecProvider.getInstance().updatePackages(servicesToRemove, servicesToAdd);
			webComponentSpecProvider.updatePackages(componentsToRemove, componentsToAdd);
		}
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

		List<IPackageReader> componentPackages = flatten(componentReaders.values());
		List<IPackageReader> servicePackages = flatten(serviceReaders.values());

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
		// this is second init in developer, so make sure all web components use the new loaded specs
		WebObjectRegistry.clearWebObjectCaches();
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

			URL url = null;
			if (pathInfo.startsWith("/templates/"))
			{
				File templateFile = new File(templatesDir, pathInfo.substring("/templates/".length()));
				if (templateFile.exists())
				{
					url = templateFile.toURI().toURL();
				}
			}
			if (url == null) url = computeURL(pathInfo, bundle);

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
						if (HTTPUtils.checkAndSetUnmodified((HttpServletRequest)request, (HttpServletResponse)response, entry.getTime())) return;
						response.setContentLength((int)entry.getSize());
						response.setContentType(MimeTypes.guessContentTypeFromName(file));
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
					if (HTTPUtils.checkAndSetUnmodified((HttpServletRequest)request, (HttpServletResponse)response, connection.getLastModified())) return;
					response.setContentLength(connection.getContentLength());

					response.setContentType(MimeTypes.guessContentTypeFromName(url.getFile()));
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
		try
		{
			return compileLessWithNashorn(getText(is), null, null);
		}
		catch (IOException e)
		{
			Debug.log(e);
		}
		return null;
	}

	public static String compileLessWithNashorn(String text, FlattenedSolution fs, String name)
	{

		try
		{
			Invocable engine = getInvocable();
			synchronized (engine)
			{
				Object result = engine.invokeFunction("convert", text, new LessFileManager(fs, name));
				return result.toString();
			}
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
		return "";
	}

	private static Invocable getInvocable() throws NoSuchMethodException, ScriptException
	{
		if (invocable == null)
		{
			//we have to pass in null as classloader if we want to acess the java 8 nashorn
			ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
			if (engine != null)
			{
				invocable = (Invocable)engine;
				invocable.invokeFunction("load", ResourceProvider.class.getResource("js/less-2.5.1.js"));
				invocable.invokeFunction("load", ResourceProvider.class.getResource("js/less-env-2.5.1.js"));
				invocable.invokeFunction("load", ResourceProvider.class.getResource("js/lessrunner.js"));
			}
		}
		return invocable;
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
				List<IPackageReader> reader = componentReaders.get(packageName);
				if (reader != null && !reader.isEmpty())
				{
					// just take the first?
					url = reader.get(0).getUrlForPath(pathInfo.substring(index));
					if (url == null)
					{
						Debug.error("url '" + pathInfo.substring(index) + "' for package: '" + packageName + "' is not found in the component package");
					}
				}
				else
				{
					reader = serviceReaders.get(packageName);
					if (reader != null && !reader.isEmpty())
					{
						// just take the first?
						url = reader.get(0).getUrlForPath(pathInfo.substring(index));
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
				return getManifest().getMainAttributes().getValue("Bundle-Version");
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
		public void clearError()
		{
			// TODO Auto-generated method stub

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


	public static IPackageReader getServicePackageReader(File file)
	{
		for (Entry<String, List<IPackageReader>> entry : serviceReaders.entrySet())
		{
			for (IPackageReader pr : entry.getValue())
			{
				File resource = pr.getResource();
				if (resource != null && resource.equals(file)) return pr;
			}
		}
		return null;
	}

	public static IPackageReader getComponentPackageReader(File file)
	{
		for (Entry<String, List<IPackageReader>> entry : componentReaders.entrySet())
		{
			for (IPackageReader pr : entry.getValue())
			{
				File resource = pr.getResource();
				if (resource != null && resource.equals(file)) return pr;
			}
		}
		return null;
	}

	public static String compileSolutionLessFile(Media media, FlattenedSolution fs)
	{
		return compileSolutionLessFile(media, fs, true);
	}

	public static String compileSolutionLessFile(Media media, FlattenedSolution fs, boolean includeServoyDefaultLess)
	{
		StringBuilder sb = new StringBuilder();
		Media properties = fs.getMedia(PROPERTIES_LESS);
		if (properties != null)
		{
			//if there is a properties file, then we concatenate the properties, servoy default less and solution less files
			sb.append(new String(properties.getMediaData()));
			if (includeServoyDefaultLess)
			{
				try (InputStream is = ResourceProvider.class.getResource(SERVOY_LESS_PATH).openStream())
				{
					sb.append(Utils.getTXTFileContent(is, Charset.forName("UTF8")));
				}
				catch (Exception e)
				{
					log.error("Cannot find servoy default less file.", e);
				}
			}
		}
		sb.append(new String(media.getMediaData()));
		return ResourceProvider.compileLessWithNashorn(sb.toString(), fs, media.getName());
	}
}
