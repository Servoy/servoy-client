/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.j2db.debug;

import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.ImageIcon;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IDebugNGDesktopClient;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.ILAFManager;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.DataServerProxy;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.plugins.IPluginAccess;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerAccess;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.ServoyException;

/**
 * @author costinchiulan
 *
 */
public class DebugNGDesktopClient implements IDebugNGDesktopClient
{
	private final IDesignerCallback designerCallback;
	private Solution current;


	/**
	 * @param webSocketClientEndpoint
	 * @param designerCallback
	 */
	public DebugNGDesktopClient(INGClientWebsocketSession wsSession, IDesignerCallback designerCallback) throws Exception
	{
		this.designerCallback = designerCallback;
	}


	@Override
	public void shutDown(boolean force)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setCurrent(Solution current)
	{
		this.current = current;
		closeSolution(true, null);
	}

	@Override
	public void refreshForI18NChange(boolean recreateForms)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void reportToDebugger(String messsage, boolean errorLevel)
	{
		if (!errorLevel)
		{
			DebugUtils.stdoutToDebugger(getScriptEngine(), messsage);
		}
		else
		{
			DebugUtils.stderrToDebugger(getScriptEngine(), messsage);
		}
	}

	@Override
	public int getApplicationType()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getClientOSName()
	 */
	@Override
	public String getClientOSName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getClientPlatform()
	 */
	@Override
	public int getClientPlatform()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#setStatusProgress(int)
	 */
	@Override
	public void setStatusProgress(int progress)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#setStatusText(java.lang.String, java.lang.String)
	 */
	@Override
	public void setStatusText(String text, String tooltip)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @return the designerCallback
	 */
	IDesignerCallback getDesignerCallback()
	{
		return designerCallback;
	}

	@Override
	public void reportJSError(String msg, Object detail)
	{
		errorToDebugger(msg, detail);
	}

	@Override
	public void reportJSWarning(String msg)
	{
		errorToDebugger(msg, null);
		Debug.warn(msg);
	}

	@Override
	public void reportJSWarning(String msg, Throwable t)
	{
		errorToDebugger(msg, t);
		if (t == null) Debug.warn(msg);
//		else super.reportJSWarning(msg, t);
	}

	@Override
	public void reportJSInfo(String msg)
	{
//		if (Boolean.valueOf(settings.getProperty(Settings.DISABLE_SERVER_LOG_FORWARDING_TO_DEBUG_CLIENT_CONSOLE, "false")).booleanValue())
//		{
		DebugUtils.stdoutToDebugger(getScriptEngine(), "INFO: " + msg);
//		}
	}

	@Override
	public void reportError(String msg, Object detail)
	{
		errorToDebugger(msg, detail);
	}

	@Override
	public void reportWarningInStatus(String s)
	{

	}

	@Override
	public void recreateForms()
	{

	}

	@Override
	public void refreshPersists(Collection<IPersist> changes)
	{

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IDebugClient#loadSecuritySettings(com.servoy.j2db.FlattenedSolution)
	 */
	@Override
	public void loadSecuritySettings(FlattenedSolution root) throws ServoyException, RemoteException
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#showSolutionLoading(boolean)
	 */
	@Override
	public void showSolutionLoading(boolean loading)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getFormManager()
	 */
	@Override
	public IBasicFormManager getFormManager()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getCmdManager()
	 */
	@Override
	public ICmdManager getCmdManager()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getBeanManager()
	 */
	@Override
	public IBeanManager getBeanManager()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getPluginManager()
	 */
	@Override
	public IPluginManager getPluginManager()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getModeManager()
	 */
	@Override
	public IModeManager getModeManager()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getUserManager()
	 */
	@Override
	public IUserManager getUserManager()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getApplicationName()
	 */
	@Override
	public String getApplicationName()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#putClientProperty(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean putClientProperty(Object name, Object val)
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getClientProperty(java.lang.Object)
	 */
	@Override
	public Object getClientProperty(Object key)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(String title)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getLAFManager()
	 */
	@Override
	public ILAFManager getLAFManager()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#output(java.lang.Object, int)
	 */
	@Override
	public void output(Object msg, int level)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#authenticate(java.lang.String, java.lang.String, java.lang.Object[])
	 */
	@Override
	public Object authenticate(String authenticator_solution, String method, Object[] credentials) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#logout(java.lang.Object[])
	 */
	@Override
	public void logout(Object[] solution_to_open_args)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#closeSolution(boolean, java.lang.Object[])
	 */
	@Override
	public boolean closeSolution(boolean force, Object[] args)
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getClientInfo()
	 */
	@Override
	public ClientInfo getClientInfo()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getItemFactory()
	 */
	@Override
	public ItemFactory getItemFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getDataRenderFactory()
	 */
	@Override
	public IDataRendererFactory getDataRenderFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getPluginAccess()
	 */
	@Override
	public IPluginAccess getPluginAccess()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getPrintingRendererParent()
	 */
	@Override
	public RendererParentWrapper getPrintingRendererParent()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getPageFormat()
	 */
	@Override
	public PageFormat getPageFormat()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#setPageFormat(java.awt.print.PageFormat)
	 */
	@Override
	public void setPageFormat(PageFormat currentPageFormat)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#clearLoginForm()
	 */
	@Override
	public void clearLoginForm()
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getUserProperty(java.lang.String)
	 */
	@Override
	public String getUserProperty(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#setUserProperty(java.lang.String, java.lang.String)
	 */
	@Override
	public void setUserProperty(String name, String value)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getUserPropertyNames()
	 */
	@Override
	public String[] getUserPropertyNames()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#handleClientUserUidChanged(java.lang.String, java.lang.String)
	 */
	@Override
	public void handleClientUserUidChanged(String userUidBefore, String userUidAfter)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getServerService(java.lang.String)
	 */
	@Override
	public Remote getServerService(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#setI18NMessagesFilter(java.lang.String, java.lang.String[])
	 */
	@Override
	public void setI18NMessagesFilter(String columnname, String[] value)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getResourceBundle(java.util.Locale)
	 */
	@Override
	public ResourceBundle getResourceBundle(Locale locale)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getScreenSize()
	 */
	@Override
	public Dimension getScreenSize()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#showURL(java.lang.String, java.lang.String, java.lang.String, int, boolean)
	 */
	@Override
	public boolean showURL(String url, String target, String target_options, int timeout, boolean onRootFrame)
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#isInDeveloper()
	 */
	@Override
	public boolean isInDeveloper()
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#isShutDown()
	 */
	@Override
	public boolean isShutDown()
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getDataServerProxy()
	 */
	@Override
	public DataServerProxy getDataServerProxy()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getRuntimeWindowManager()
	 */
	@Override
	public RuntimeWindowManager getRuntimeWindowManager()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getSolutionName()
	 */
	@Override
	public String getSolutionName()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#looseFocus()
	 */
	@Override
	public void looseFocus()
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#updateUI(int)
	 */
	@Override
	public void updateUI(int time)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#setValueListItems(java.lang.String, java.lang.Object[], java.lang.Object[], boolean)
	 */
	@Override
	public void setValueListItems(String name, Object[] displayValues, Object[] realValues, boolean autoconvert)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicApplication#getSolution()
	 */
	@Override
	public Solution getSolution()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicApplication#getFlattenedSolution()
	 */
	@Override
	public FlattenedSolution getFlattenedSolution()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicApplication#getRepository()
	 */
	@Override
	public IRepository getRepository()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicApplication#getApplicationServerAccess()
	 */
	@Override
	public IApplicationServerAccess getApplicationServerAccess()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicApplication#loadImage(java.lang.String)
	 */
	@Override
	public ImageIcon loadImage(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicApplication#getScheduledExecutor()
	 */
	@Override
	public ScheduledExecutorService getScheduledExecutor()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.IUIBlocker#blockGUI(java.lang.String)
	 */
	@Override
	public void blockGUI(String reason)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.IUIBlocker#releaseGUI()
	 */
	@Override
	public void releaseGUI()
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#haveRepositoryAccess()
	 */
	@Override
	public boolean haveRepositoryAccess()
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getApplicationServer()
	 */
	@Override
	public IApplicationServer getApplicationServer()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getDataServer()
	 */
	@Override
	public IDataServer getDataServer()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getClientHost()
	 */
	@Override
	public IClientHost getClientHost()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#isSolutionLoaded()
	 */
	@Override
	public boolean isSolutionLoaded()
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#handleException(java.lang.String, java.lang.Exception)
	 */
	@Override
	public void handleException(String servoyMsg, Exception e)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#reportInfo(java.lang.String)
	 */
	@Override
	public void reportInfo(String msg)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#reportWarning(java.lang.String)
	 */
	@Override
	public void reportWarning(String msg)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getScriptEngine()
	 */
	@Override
	public IExecutingEnviroment getScriptEngine()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getUserUID()
	 */
	@Override
	public String getUserUID()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getUserName()
	 */
	@Override
	public String getUserName()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getClientID()
	 */
	@Override
	public String getClientID()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getTenantValue()
	 */
	@Override
	public Object getTenantValue()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getTimeZone()
	 */
	@Override
	public TimeZone getTimeZone()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getSettings()
	 */
	@Override
	public Properties getSettings()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getRuntimeProperties()
	 */
	@Override
	public Map getRuntimeProperties()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getFoundSetManager()
	 */
	@Override
	public IFoundSetManagerInternal getFoundSetManager()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#isRunningRemote()
	 */
	@Override
	public boolean isRunningRemote()
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#getServerURL()
	 */
	@Override
	public URL getServerURL()
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#setLocale(java.util.Locale)
	 */
	@Override
	public void setLocale(Locale locale)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IServiceProvider#setTimeZone(java.util.TimeZone)
	 */
	@Override
	public void setTimeZone(TimeZone timeZone)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IEventDelegator#invokeLater(java.lang.Runnable)
	 */
	@Override
	public void invokeLater(Runnable r)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IEventDelegator#isEventDispatchThread()
	 */
	@Override
	public boolean isEventDispatchThread()
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IEventDelegator#invokeAndWait(java.lang.Runnable)
	 */
	@Override
	public void invokeAndWait(Runnable r)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.base.util.I18NProvider#getI18NMessage(java.lang.String)
	 */
	@Override
	public String getI18NMessage(String i18nKey)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.base.util.I18NProvider#getI18NMessage(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String getI18NMessage(String i18nKey, String language, String country)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.base.util.I18NProvider#getI18NMessage(java.lang.String, java.lang.Object[])
	 */
	@Override
	public String getI18NMessage(String i18nKey, Object[] array)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.base.util.I18NProvider#getI18NMessage(java.lang.String, java.lang.Object[], java.lang.String, java.lang.String)
	 */
	@Override
	public String getI18NMessage(String i18nKey, Object[] array, String language, String country)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.base.util.I18NProvider#getI18NMessageIfPrefixed(java.lang.String)
	 */
	@Override
	public String getI18NMessageIfPrefixed(String i18nKey)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.base.util.I18NProvider#setI18NMessage(java.lang.String, java.lang.String)
	 */
	@Override
	public void setI18NMessage(String i18nKey, String value)
	{
		// TODO Auto-generated method stub

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IDebugClient#errorToDebugger(java.lang.String, java.lang.Object)
	 */
	@Override
	public void errorToDebugger(String message, Object detail)
	{
		// TODO Auto-generated method stub

	}

}
