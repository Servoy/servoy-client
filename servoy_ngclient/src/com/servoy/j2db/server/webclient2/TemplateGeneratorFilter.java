package com.servoy.j2db.server.webclient2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IRemoteRepository;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRepositoryFactory;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.main.IResourceContext;
import com.servoy.j2db.server.main.RuntimeBeanManager;
import com.servoy.j2db.server.main.RuntimeLAFManager;
import com.servoy.j2db.server.main.ServerPluginManager;
import com.servoy.j2db.server.main.ServerStarter;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.starter.IServerStarter;
import com.servoy.j2db.server.webclient2.component.WebComponentSpecProvider;
import com.servoy.j2db.server.webclient2.template.FormTemplateGenerator;
import com.servoy.j2db.server.webclient2.template.IndexTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.JarManager.ExtensionResource;
import com.servoy.j2db.util.LocalRegistry;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Settings;

@WebFilter("/solutions/*")
@SuppressWarnings("nls")
public class TemplateGeneratorFilter implements Filter
{
	public static final String SOLUTIONS_PATH = "solutions/";
	public static final String FORMS_PATH = "forms/";

	@Override
	public void destroy()
	{
		try
		{
			ApplicationServerRegistry.get().shutDown();
			ApplicationServerRegistry.clear();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
	{
		try
		{
			if (Boolean.valueOf(Settings.getInstance().getProperty("servoy.internal.reloadSpecsAllTheTime", "false")).booleanValue()) WebComponentSpecProvider.reload();

			HttpServletRequest request = (HttpServletRequest)servletRequest;
			String uri = request.getRequestURI();
			if (uri != null && (uri.endsWith(".html") || uri.endsWith(".js")))
			{
				int solutionIndex = uri.indexOf(SOLUTIONS_PATH);
				int formIndex = uri.indexOf(FORMS_PATH);
				if (solutionIndex > 0)
				{
					String solutionName = uri.substring(solutionIndex + SOLUTIONS_PATH.length(), uri.indexOf("/", solutionIndex + SOLUTIONS_PATH.length() + 1));
					FlattenedSolution fs = null;
					try
					{
						fs = new FlattenedSolution((SolutionMetaData)ApplicationServerRegistry.get().getLocalRepository().getRootObjectMetaData(solutionName,
							IRepository.SOLUTIONS), new AbstractActiveSolutionHandler()
						{

							@Override
							public IRepository getRepository()
							{
								return ApplicationServerRegistry.get().getLocalRepository();
							}

						});
					}
					catch (RepositoryException e)
					{
						Debug.error(e);
					}
					if (fs != null && formIndex > 0)
					{
						String formName = uri.substring(formIndex + FORMS_PATH.length());
						formName = formName.replace(".html", "");
						formName = formName.replace(".js", "");
						Form f = fs.getForm(formName);
						Form form = (f != null) ? fs.getFlattenedForm(f) : null;
						if (form != null)
						{
							String view = form.getView() == IFormConstants.VIEW_TYPE_TABLE || form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED
								? "tableview" : "recordview";
							String output = uri.endsWith(".html") ? "html" : "js";
							((HttpServletResponse)servletResponse).setContentType("text/" + (output == "html" ? "html" : "javascript"));
							new FormTemplateGenerator(fs).generate(form, "form_" + view + "_" + output + ".ftl", servletResponse.getWriter());
							return;
						}
					}
					else if (uri.endsWith("index.html"))
					{
						((HttpServletResponse)servletResponse).setContentType("text/html");
						new IndexTemplateGenerator(fs).generate(fs, "index.ftl", servletResponse.getWriter());
						return;
					}
				}
			}

			filterChain.doFilter(servletRequest, servletResponse);
		}
		catch (RuntimeException | Error e)
		{
			Debug.error(e);
			throw e;
		}
	}

	@Override
	public void init(final FilterConfig fc) throws ServletException
	{
		String contextPath = fc.getServletContext().getContextPath();
		if (contextPath == null) contextPath = "";
		File parent = new File(System.getProperty("user.home"), ".servoy/server/" + contextPath);
		if (!parent.exists()) parent.mkdirs();
		System.setProperty("servoy.application_server.dir", parent.getAbsolutePath());
		Settings settings = Settings.getInstance();
		settings.put(J2DBGlobals.SERVOY_APPLICATION_SERVER_DIRECTORY_KEY, parent.getAbsolutePath());
		try
		{
			settings.loadFromURL(fc.getServletContext().getResource("/WEB-INF/servoy.properties"));
			File file = new File(parent, "servoy_server.properties");
			if (!file.exists())
			{
				if (!file.createNewFile())
				{
					System.err.println("Couldn't create server properties file in user home: " + file.getAbsolutePath());
				}
			}
			settings.loadFromFile(file);
			settings.put(J2DBGlobals.SERVOY_APPLICATION_SERVER_CONTEXT_KEY, contextPath);

			List<ExtensionResource> pluginUrls = new ArrayList<ExtensionResource>();
			List<ExtensionResource> supportLibUrls = new ArrayList<ExtensionResource>();
			InputStream is = fc.getServletContext().getResourceAsStream("/plugins/plugins.properties");
			try
			{
				Properties pluginProperties = new Properties();
				pluginProperties.load(is);

				Iterator<Entry<Object, Object>> iterator = pluginProperties.entrySet().iterator();
				while (iterator.hasNext())
				{
					Entry<Object, Object> next = iterator.next();
					String jarname = (String)next.getKey();
					String timeAndSize = (String)next.getValue();
					int index = timeAndSize.indexOf(':');
					Long time = Long.valueOf(timeAndSize.substring(0, index));

					URL resource = fc.getServletContext().getResource("/plugins/" + jarname);
					if (jarname.indexOf('/') == -1)
					{
						pluginUrls.add(new ExtensionResource(resource, jarname, time));
					}
					else
					{
						supportLibUrls.add(new ExtensionResource(resource, jarname, time));
					}
				}
			}
			finally
			{
				is.close();
			}

			List<ExtensionResource> beanUrls = new ArrayList<ExtensionResource>();
			is = fc.getServletContext().getResourceAsStream("/beans/beans.properties");
			try
			{
				Properties beanProperties = new Properties();
				beanProperties.load(is);

				Iterator<Entry<Object, Object>> iterator = beanProperties.entrySet().iterator();
				while (iterator.hasNext())
				{
					Entry<Object, Object> next = iterator.next();
					String jarname = (String)next.getKey();
					String timeAndSize = (String)next.getValue();
					int index = timeAndSize.indexOf(':');
					Long time = Long.valueOf(timeAndSize.substring(0, index));

					URL resource = fc.getServletContext().getResource("/beans/" + jarname);
					beanUrls.add(new ExtensionResource(resource, jarname, time));
				}

			}
			finally
			{
				is.close();
			}

			List<ExtensionResource> lafUrls = new ArrayList<ExtensionResource>();
			is = fc.getServletContext().getResourceAsStream("/lafs/lafs.properties");
			try
			{
				Properties lafProperties = new Properties();
				lafProperties.load(is);

				Iterator<Entry<Object, Object>> iterator = lafProperties.entrySet().iterator();
				while (iterator.hasNext())
				{
					Entry<Object, Object> next = iterator.next();
					String jarname = (String)next.getKey();
					String timeAndSize = (String)next.getValue();
					int index = timeAndSize.indexOf(':');
					Long time = Long.valueOf(timeAndSize.substring(0, index));

					URL resource = fc.getServletContext().getResource("/lafs/" + jarname);
					lafUrls.add(new ExtensionResource(resource, jarname, time));
				}

			}
			finally
			{
				is.close();
			}

			Solution solution = null;
			Solution[] mods = null;
			ObjectInputStream ois = null;
			try
			{
				BufferedInputStream bis = new BufferedInputStream(fc.getServletContext().getResourceAsStream("/WEB-INF/solution.runtime"));
				GZIPInputStream zip = new GZIPInputStream(bis);
				ois = new ObjectInputStream(zip);
				solution = (Solution)ois.readObject();

				int modCount = ois.readInt();
				if (modCount > 0)
				{
					mods = (Solution[])ois.readObject();
				}
			}
			catch (IOException ex)
			{
				// ignore, file not found
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
			finally
			{
				if (ois != null)
				{
					ois.close();
				}
			}

			final Solution mainSolution = solution;
			final Solution[] modules = mods;

			ApplicationServerRegistry.setServiceRegistry(new LocalRegistry());
			RuntimeLAFManager lafManager = new RuntimeLAFManager(lafUrls);
			ServerPluginManager pluginManager = new ServerPluginManager(pluginUrls, supportLibUrls, lafManager.getClassLoader());
			RuntimeBeanManager beanManager = new RuntimeBeanManager(pluginManager.getClassLoader(), beanUrls);
			IServerStarter ss = new ServerStarter(settings, ApplicationServerRegistry.getServiceRegistry());
			ss.setAppServerStartup(true);
			ss.init();
			ss.setRepositoryFactory(new IRepositoryFactory()
			{
				public IRemoteRepository getRemoteRepository(IServerManagerInternal serverManager, Properties s)
				{
					try
					{
						IRemoteRepository repository = (IRemoteRepository)((IServerInternal)serverManager.getRepositoryServer()).getRepository();
						if (mainSolution != null)
						{
							if (!((AbstractRepository)repository).isRootObjectCacheInitialized())
							{
								((AbstractRepository)repository).cacheRootObject(mainSolution);
								mainSolution.setRepository(repository);
								if (modules != null)
								{
									for (Solution module : modules)
									{
										((AbstractRepository)repository).cacheRootObject(module);
										module.setRepository(repository);
									}
								}
							}
						}
						return repository;
					}
					catch (Exception e)
					{
						boolean no_repository_found = (e instanceof RepositoryException) &&
							((ServoyException)e).getErrorCode() == ServoyException.InternalCodes.ERROR_NO_REPOSITORY_IN_DB;
						boolean old_repository_found = (e instanceof RepositoryException) &&
							((ServoyException)e).getErrorCode() == ServoyException.InternalCodes.ERROR_OLD_REPOSITORY_IN_DB;

						if (no_repository_found || old_repository_found)
						{
							Debug.log(e);
							try
							{
								return (IRemoteRepository)((IServerInternal)serverManager.getRepositoryServer()).createRepositoryTables();
							}
							catch (Exception ex)
							{
								throw new RuntimeException("Cannot create repository", ex); //$NON-NLS-1$ 
							}
						}
						throw new RuntimeException("Cannot connect to repository", e); //$NON-NLS-1$
					}
				}

				public IDeveloperRepository getDeveloperRepository(IServerManagerInternal serverManager, Properties s)
				{
					return null;
				}
			});
			ss.setPluginManager(pluginManager);
			ss.setBeanManager(beanManager);
			ss.setLAFManager(lafManager);
			ss.setResourceContext(new IResourceContext()
			{

				public InputStream getResourceAsStream(String filename)
				{
					return fc.getServletContext().getResourceAsStream(filename.startsWith("/") ? filename : "/" + filename);
				}

				public URL getResource(String filename)
				{
					try
					{
						return fc.getServletContext().getResource(filename.startsWith("/") ? filename : "/" + filename);
					}
					catch (MalformedURLException e)
					{
						Debug.error(e);
					}
					return null;
				}
			});
			ss.start();
			WebComponentSpecProvider.init(fc.getServletContext());
		}
		catch (Exception e)
		{
			throw new ServletException("Can't start the application server", e);
		}
	}
}
