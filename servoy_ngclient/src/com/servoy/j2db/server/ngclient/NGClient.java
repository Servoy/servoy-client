package com.servoy.j2db.server.ngclient;

import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.IChangeListener;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.dataprocessing.SwingFoundSetFactory;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.headlessclient.AbstractApplication;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.WebCredentials;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.Ad;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.ServoyScheduledExecutor;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

// TODO we should add a subclass between ClientState and SessionClient, (remove all "session" and wicket related stuff out of SessionClient)
// then we can extend that one.
public class NGClient extends AbstractApplication implements INGApplication, IChangeListener
{
	private static final long serialVersionUID = 1L;

	private final INGClientWebsocketSession wsSession;

	private transient volatile ServoyScheduledExecutor scheduledExecutorService;

	private volatile NGRuntimeWindowManager runtimeWindowManager;

	private Map<Object, Object> uiProperties;

	public static final String APPLICATION_SERVICE = "$applicationService";

	public NGClient(INGClientWebsocketSession wsSession)
	{
		super(new WebCredentials());

		this.wsSession = wsSession;
		settings = Settings.getInstance();
		try
		{
			applicationSetup();
			applicationInit();
			applicationServerInit();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Debug.error(e);
		}
	}

	public void loadSolution(String solutionName) throws RepositoryException
	{
		try
		{
			SolutionMetaData solutionMetaData = getApplicationServer().getSolutionDefinition(solutionName, getSolutionTypeFilter());
			if (solutionMetaData == null)
			{
				throw new IllegalArgumentException(Messages.getString("servoy.exception.solutionNotFound", new Object[] { solutionName })); //$NON-NLS-1$
			}
			loadSolution(solutionMetaData);
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ClientState#getFormManager()
	 */
	@Override
	public INGFormManager getFormManager()
	{
		return (INGFormManager)super.getFormManager();
	}

	public synchronized Map<String, Map<String, Map<String, Object>>> getChanges()
	{
		Map<String, Map<String, Map<String, Object>>> changes = new HashMap<>(8);
		if (isShutDown()) return changes;
		for (IFormController fc : getFormManager().getCachedFormControllers())
		{
			if (fc.isFormVisible())
			{
				Map<String, Map<String, Object>> formChanges = ((WebFormUI)fc.getFormUI()).getAllComponentsChanges();
				if (formChanges.size() > 0)
				{
					changes.put(fc.getName(), formChanges);
				}
			}
		}
		return changes;
	}

	@Override
	protected void solutionLoaded(Solution s)
	{
		super.solutionLoaded(s);
		getWebsocketSession().solutionLoaded(s);
	}

	@Override
	public INGClientWebsocketSession getWebsocketSession()
	{
		return wsSession;
	}

	@Override
	public void valueChanged()
	{
		getWebsocketSession().valueChanged();
	}

	@Override
	protected void doInvokeLater(Runnable r)
	{
		wsSession.getEventDispatcher().addEvent(r);
	}

	@Override
	public boolean isEventDispatchThread()
	{
		return wsSession.getEventDispatcher().isEventDispatchThread();
	}

	@Override
	public void invokeAndWait(Runnable r)
	{
		FutureTask<Object> future = new FutureTask<Object>(r, null);
		wsSession.getEventDispatcher().addEvent(future);
		try
		{
			future.get(); // blocking
		}
		catch (InterruptedException e)
		{
			Debug.trace(e);
		}
		catch (ExecutionException e)
		{
			e.getCause().printStackTrace();
			Debug.error(e.getCause());
		}
	}

	@Override
	public Locale getLocale() // TODO provide actual Implementatin
	{
		return new Locale("en", "US");
	}

	@Override
	public void setLocale(Locale locale)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public TimeZone getTimeZone()
	{
		// TODO get from actual client?
		return null;
	}

	@Override
	public void setTimeZone(TimeZone timeZone)
	{
		// TODO should this be remembered?
		super.setTimeZone(timeZone);
	}

	@Override
	public String getI18NMessage(String i18nKey)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getI18NMessage(String i18nKey, Object[] array)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getI18NMessageIfPrefixed(String i18nKey)
	{
		// TODO Auto-generated method stub
		return i18nKey;
	}

	@Override
	public void setI18NMessage(String i18nKey, String value)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void refreshI18NMessages()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setI18NMessagesFilter(String columnname, String[] value)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public ResourceBundle getResourceBundle(Locale locale)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean startApplicationServerConnection()
	{
		try
		{
			applicationServer = ApplicationServerRegistry.getService(IApplicationServer.class);
			return true;
		}
		catch (Exception ex)
		{
			reportError(Messages.getString("servoy.client.error.finding.dataservice"), ex); //$NON-NLS-1$
			return false;
		}
	}

	@Override
	protected void loadSolution(SolutionMetaData solutionMeta) throws RepositoryException
	{
		if (loadSolutionsAndModules(solutionMeta))
		{
			J2DBGlobals.firePropertyChange(this, "solution", null, getSolution()); //$NON-NLS-1$
		}
	}

	@Override
	protected IBasicFormManager createFormManager()
	{
		return new NGFormManager(this);
	}

	@Override
	public IChangeListener getChangeListener()
	{
		return this;
	}

	@Override
	protected void createFoundSetManager()
	{
		foundSetManager = new FoundSetManager(this, new SwingFoundSetFactory());
		foundSetManager.init();
	}

	@Override
	public ScheduledExecutorService getScheduledExecutor()
	{
		if (scheduledExecutorService == null && !isShutDown())
		{
			synchronized (this)
			{
				if (scheduledExecutorService == null)
				{
					scheduledExecutorService = new ServoyScheduledExecutor(1, 4, 1)
					{
						private IServiceProvider prev;

						@Override
						protected void beforeExecute(Thread t, Runnable r)
						{
							super.beforeExecute(t, r);
							prev = J2DBGlobals.getServiceProvider();
							if (prev != NGClient.this)
							{
								// if this happens it is a webclient in developer..
								// and the provider is not set for this web client. so it must be set.
								J2DBGlobals.setServiceProvider(NGClient.this);
							}
						}

						@Override
						protected void afterExecute(Runnable r, Throwable t)
						{
							super.afterExecute(r, t);
							J2DBGlobals.setServiceProvider(prev);
						}
					};
				}
			}
		}
		return scheduledExecutorService;
	}

	@SuppressWarnings("nls")
	@Override
	public Dimension getScreenSize()
	{
		try
		{
			Object retValue = this.getWebsocketSession().executeServiceCall(NGClient.APPLICATION_SERVICE, "getScreenSize", null);
			if (retValue instanceof JSONObject)
			{
				int orientation = ((JSONObject)retValue).optInt("orientation", 0);
				int width = ((JSONObject)retValue).optInt("width", -1);
				int height = ((JSONObject)retValue).optInt("height", -1);
				if (orientation == 90 || orientation == -90)
				{
					return new Dimension(height, width);
				}
				return new Dimension(width, height);
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public URL getServerURL()
	{
		try
		{
			Object retValue = this.getWebsocketSession().executeServiceCall(NGClient.APPLICATION_SERVICE, "getLocation", null);
			if (retValue instanceof String)
			{
				String url = (String)retValue;
				int index = url.indexOf("/solutions/");
				if (index != -1)
				{
					url = url.substring(0, index);
				}
				if (!url.toLowerCase().startsWith("http"))
				{
					url = "http://" + url;
				}
				return new URL(url);
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
		return super.getServerURL();
	}

	@Override
	public int getApplicationType()
	{
		return IApplication.NG_CLIENT;
	}

	@Override
	public String getClientOSName()
	{
		try
		{
			Object retValue = this.getWebsocketSession().executeServiceCall(NGClient.APPLICATION_SERVICE, "getUserAgentAndPlatform", null);
			if (retValue instanceof JSONObject)
			{
				String userAgent = ((JSONObject)retValue).optString("userAgent");
				if (userAgent != null)
				{
					if (userAgent.indexOf("NT 6.1") != -1) return "Windows 7";
					if (userAgent.indexOf("NT 6.0") != -1) return "Windows Vista";
					if (userAgent.indexOf("NT 5.1") != -1 || userAgent.indexOf("Windows XP") != -1) return "Windows XP";
					if (userAgent.indexOf("Linux") != -1) return "Linux";
					if (userAgent.indexOf("Mac") != -1) return "Mac OS";
				}
				String platform = ((JSONObject)retValue).optString("platform");
				if (platform != null) return platform;
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
		return super.getClientOSName();
	}

	@Override
	public int getClientPlatform()
	{
		try
		{
			Object retValue = this.getWebsocketSession().executeServiceCall(NGClient.APPLICATION_SERVICE, "getUserAgentAndPlatform", null);
			if (retValue instanceof JSONObject)
			{
				String platform = ((JSONObject)retValue).optString("platform");
				if (platform != null)
				{
					return Utils.getPlatform(platform);
				}
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
		return super.getClientPlatform();
	}

	@Override
	public String getApplicationName()
	{
		return "Servoy NGClient";
	}

	@Override
	public boolean putClientProperty(Object name, Object val)
	{
		if (uiProperties == null)
		{
			uiProperties = new HashMap<Object, Object>();
		}
		uiProperties.put(name, val);
		return true;
	}

	@Override
	public Object getClientProperty(Object name)
	{
		return (uiProperties == null) ? null : uiProperties.get(name);
	}

	@Override
	public void setTitle(String title)
	{
		getRuntimeWindowManager().getCurrentWindow().setTitle(title);
	}

	@Override
	public ItemFactory getItemFactory()
	{
		// Not used in NGClient
		return null;
	}

	@Override
	public IDataRendererFactory getDataRenderFactory()
	{
		// Not used in NGClient
		return null;
	}

	@Override
	public RendererParentWrapper getPrintingRendererParent()
	{
		// Not used in NGClient
		return null;
	}

	@Override
	public PageFormat getPageFormat()
	{
		// Not used in NGClient
		return null;
	}

	@Override
	public void setPageFormat(PageFormat currentPageFormat)
	{
		// Not used in NGClient
	}

	@Override
	public String getUserProperty(String name)
	{
		try
		{
			return (String)getWebsocketSession().executeServiceCall("$applicationService", "getUserProperty", new Object[] { name });
		}
		catch (IOException e)
		{
			Debug.error("Error getting getting property '" + name + "'", e);
		}
		return null;
	}

	@Override
	public void setUserProperty(String name, String value)
	{
		try
		{
			getWebsocketSession().executeServiceCall("$applicationService", "setUserProperty", new Object[] { name, value });
		}
		catch (IOException e)
		{
			Debug.error("Error getting setting property '" + name + "' value: " + value, e);
		}

	}

	@SuppressWarnings("nls")
	@Override
	public String[] getUserPropertyNames()
	{
		JSONArray result;
		try
		{
			result = (JSONArray)getWebsocketSession().executeServiceCall("$applicationService", "getUserPropertyNames", null);
			String[] names = new String[result.length()];
			for (int i = 0; i < names.length; i++)
			{
				names[i] = result.optString(i);
			}
			return names;
		}
		catch (IOException e)
		{
			Debug.error("Error getting user property names", e);
		}
		return new String[0];
	}

	@Override
	public void looseFocus()
	{
		// TODO call request focus on a div in a client?
	}

	@Override
	public boolean showURL(String url, String target, String target_options, int timeout_ms, boolean onRootFrame)
	{
		this.getWebsocketSession().executeAsyncServiceCall(NGClient.APPLICATION_SERVICE, "showUrl", new Object[] { url, target, target_options, timeout_ms });
		return true;
	}

	@Override
	public NGRuntimeWindowManager getRuntimeWindowManager()
	{
		if (runtimeWindowManager == null)
		{
			synchronized (this)
			{
				if (runtimeWindowManager == null) runtimeWindowManager = new NGRuntimeWindowManager(this);
			}
		}
		return runtimeWindowManager;
	}

	@Override
	public synchronized void shutDown(boolean force)
	{
		super.shutDown(force);
		if (scheduledExecutorService != null)
		{
			scheduledExecutorService.shutdownNow();
			try
			{
				scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException e)
			{
			}
			scheduledExecutorService = null;
		}
		getWebsocketSession().closeSession();
	}

	private transient Object[] adsInfo = null;//cache to expensive to get each time

	@Override
	protected boolean registerClient(IUserClient uc) throws Exception
	{
		boolean registered = false;
		try
		{
			registered = super.registerClient(uc);
			if (!registered)
			{
				((NGClientWebsocketSession)wsSession).setClient(this);
				invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						if (adsInfo == null) adsInfo = Ad.getAdInfo();
						final int w = Utils.getAsInteger(adsInfo[1]);
						final int h = Utils.getAsInteger(adsInfo[2]);
						if (w > 50 && h > 50)
						{
							final URL url = (URL)adsInfo[0];
							final int t = Utils.getAsInteger(adsInfo[3]);
							getWebsocketSession().executeAsyncServiceCall(NGClient.APPLICATION_SERVICE, "showInfoPanel",
								new Object[] { url.toString(), w, h, t, getI18NMessage("servoy.button.close") });
						}
					}
				});
			}
		}
		catch (final ApplicationException e)
		{
			//TODO
			throw e;
//			if (e.getErrorCode() == ServoyException.NO_LICENSE)
//			{
//				throw new RestartResponseException(ServoyServerToBusyPage.class);
//			}
//			else if (e.getErrorCode() == ServoyException.MAINTENANCE_MODE)
//			{
//				throw new RestartResponseException(ServoyServerInMaintenanceMode.class);
//			}
		}
		return registered;
	}
}
