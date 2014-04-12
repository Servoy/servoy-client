package com.servoy.j2db.server.ngclient;

import java.awt.Dimension;
import java.awt.print.PageFormat;
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

import javax.swing.ImageIcon;

import com.servoy.j2db.ClientState;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.ILAFManager;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.ModeManager;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.SwingFoundSetFactory;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.server.headlessclient.eventthread.IEventDispatcher;
import com.servoy.j2db.server.ngclient.eventthread.EventDispatcher;
import com.servoy.j2db.server.ngclient.eventthread.NGEvent;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyScheduledExecutor;
import com.servoy.j2db.util.Settings;

// TODO we should add a subclass between ClientState and SessionClient, (remove all "session" and wicket related stuff out of SessionClient)
// then we can extend that one.
public class NGClient extends ClientState implements INGApplication, IChangeListener
{
	private static final long serialVersionUID = 1L;

	private final INGClientWebsocketSession wsSession;

	private IEventDispatcher<NGEvent> executor;

	private transient volatile ServoyScheduledExecutor scheduledExecutorService;

	private transient IBeanManager beanManager;

	private NGRuntimeWindowMananger runtimeWindowManager;


	public NGClient(INGClientWebsocketSession wsSession)
	{
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

	public Map<String, Map<String, Map<String, Object>>> getChanges()
	{
		Map<String, Map<String, Map<String, Object>>> changes = new HashMap<>(8);

		for (IFormController fc : getFormManager().getCachedFormControllers())
		{
			if (fc.isFormVisible())
			{
				Map<String, Map<String, Object>> formChanges = ((WebFormUI)fc.getFormUI()).getAllChanges();
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
	public void reportInfo(String msg)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Locale getLocale() // TODO provide actual Implementatin
	{
		return new Locale("en", "US");
	}

	@Override
	public TimeZone getTimeZone()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLocale(Locale locale)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setTimeZone(TimeZone timeZone)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void doInvokeLater(Runnable r)
	{
		getEventDispatcher().addEvent(new NGEvent(this, r));
	}

	@Override
	public boolean isEventDispatchThread()
	{
		return getEventDispatcher().isEventDispatchThread();
	}

	@Override
	public void invokeAndWait(Runnable r)
	{
		FutureTask<Object> future = new FutureTask<Object>(r, null);
		getEventDispatcher().addEvent(new NGEvent(this, future));
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
	protected void bindUserClient()
	{
		// TODO Auto-generated method stub

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
	protected SolutionMetaData showSolutionSelection(SolutionMetaData[] solutions)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void unBindUserClient() throws Exception
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		return new ScriptEngine(this);
	}

	@Override
	protected IModeManager createModeManager()
	{
		return new ModeManager(this);
	}

	@Override
	protected IBasicFormManager createFormManager()
	{
		return new NGFormManager(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.INGApplication#getChangeListener()
	 */
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
			synchronized (J2DBGlobals.class)
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

	@Override
	public boolean isRunningRemote()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public URL getServerURL()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void saveSettings()
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void checkForActiveTransactions(boolean force)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean saveSolution()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void createPluginManager()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void refreshI18NMessages()
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void showDefaultLogin() throws ServoyException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void activateSolutionMethod(String globalMethodName, StartupArguments argumentsScope)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void blockGUI(String reason)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void releaseGUI()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void reportWarningInStatus(String s)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public ImageIcon loadImage(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getApplicationType()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getClientOSName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getClientPlatform()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setStatusProgress(int progress)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatusText(String text, String tooltip)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void showSolutionLoading(boolean loading)
	{
		// not used
	}

	@Override
	public ICmdManager getCmdManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBeanManager getBeanManager()
	{
		if (beanManager == null)
		{
			beanManager = ApplicationServerRegistry.get().getBeanManager();
		}
		return beanManager;
	}

	@Override
	public String getApplicationName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean putClientProperty(Object name, Object val)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getClientProperty(Object key)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTitle(String title)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public ILAFManager getLAFManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void output(Object msg, int level)
	{
		if (level == DEBUG)
		{
			Debug.debug(msg);
		}
		else if (level == WARNING)
		{
			Debug.warn(msg);
		}
		else if (level == ERROR)
		{
			Debug.error(msg);
		}
		else if (level == FATAL)
		{
			Debug.fatal(msg);
		}
		else
		{
			Debug.log(msg);
		}
	}

	@Override
	public ItemFactory getItemFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IDataRendererFactory getDataRenderFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RendererParentWrapper getPrintingRendererParent()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageFormat getPageFormat()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPageFormat(PageFormat currentPageFormat)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getUserProperty(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUserProperty(String name, String value)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String[] getUserPropertyNames()
	{
		// TODO Auto-generated method stub
		return null;
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
	public Dimension getScreenSize()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean showURL(String url, String target, String target_options, int timeout_ms, boolean onRootFrame)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public NGRuntimeWindowMananger getRuntimeWindowManager()
	{
		if (runtimeWindowManager == null)
		{
			runtimeWindowManager = new NGRuntimeWindowMananger(this);
		}
		return runtimeWindowManager;
	}

	@Override
	public void looseFocus()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void updateUI(int time)
	{
		// TODO Auto-generated method stub

	}

	public final synchronized IEventDispatcher<NGEvent> getEventDispatcher()
	{
		if (executor == null)
		{
			Thread thread = new Thread(executor = createDispatcher(), "Executor,clientid:" + getClientID());
			thread.setDaemon(true);
			thread.start();
		}
		return executor;
	}

	/**
	 * Method to create the {@link IEventDispatcher} runnable
	 */
	protected IEventDispatcher<NGEvent> createDispatcher()
	{
		return new EventDispatcher<NGEvent>(this);
	}

	@Override
	public void shutDown(boolean force)
	{
		super.shutDown(force);
		if (executor != null) executor.destroy();
		executor = null;
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
}
