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

package com.servoy.j2db.server.headlessclient;


import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.EventObject;
import java.util.TimeZone;

import javax.swing.Action;
import javax.swing.ImageIcon;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.ILAFManager;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.ModeManager;
import com.servoy.j2db.cmd.ICmd;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.PluginManager;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.WebCredentials;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * @author Johan
 *
 */
public abstract class AbstractApplication extends ClientState implements IApplication
{
	private transient ICmdManager cmdManager;
	protected final WebCredentials credentials;
	private transient IBeanManager beanManager;


	public AbstractApplication(WebCredentials credentials)
	{
		super();
		this.credentials = credentials;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IApplication#getOSName()
	 */
	public String getClientOSName()
	{
		return System.getProperty("os.name"); //$NON-NLS-1$
	}

	public int getClientPlatform()
	{
		// unknown client platform, overridden in WebClient
		return Utils.PLATFORM_OTHER;
	}

	@Override
	public boolean isRunningRemote()
	{
		return false;
	}

	@Override
	public URL getServerURL()
	{
		try
		{
			return new URL("http://localhost:" + ApplicationServerRegistry.get().getWebServerPort()); //$NON-NLS-1$
		}
		catch (MalformedURLException e)
		{
			Debug.error(e);
			return null;
		}
	}

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
	public void blockGUI(String reason)
	{
	}

	@Override
	public void releaseGUI()
	{
	}

	@Override
	protected void bindUserClient()
	{
		//not needed in headless
	}

	@Override
	protected void unBindUserClient() throws Exception
	{
		//not needed in headless
	}

	public void reportWarningInStatus(String s)
	{
		reportWarning(s);
	}

	public void reportInfo(String message)
	{
		Debug.log(message);
	}

	public void setTimeZone(TimeZone timeZone)
	{
		ClientInfo clientInfo = getClientInfo();
		clientInfo.setTimeZone(timeZone);
		try
		{
			getClientHost().pushClientInfo(clientInfo.getClientId(), clientInfo);
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
	}

	@Override
	protected IModeManager createModeManager()
	{
		return new ModeManager(this);
	}


	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		return new ScriptEngine(this);
	}

	@Override
	protected void checkForActiveTransactions(boolean force)
	{
		if (foundSetManager != null)
		{
			foundSetManager.rollbackTransaction(true, false, true);
		}
	}

	@Override
	public void showDefaultLogin() throws ServoyException
	{
		if (credentials.getUserName() != null && credentials.getPassword() != null)
		{
			authenticate(null, null, new Object[] { credentials.getUserName(), credentials.getPassword() });
		}
		if (getClientInfo().getUserUid() == null)
		{
			shutDown(true);
			throw new ApplicationException(ServoyException.INCORRECT_LOGIN);
		}
	}

	@Override
	public void logout(Object[] solution_to_open_args)
	{
		if (getClientInfo().getUserUid() != null)
		{
			if (getSolution() != null && getSolution().requireAuthentication())
			{
				if (closeSolution(false, solution_to_open_args)) // don't shutdown if already closing; wait for the first closeSolution to finish
				{
					if (!isClosing)
					{
						shutDown(false);//no way to enter username password so shutdown
					}
					else
					{
						credentials.clear();
						getClientInfo().clearUserInfo();
					}
				}
			}
			else
			{
				credentials.clear();
				getClientInfo().clearUserInfo();
			}
		}
	}

	public IBeanManager getBeanManager()
	{
		if (beanManager == null)
		{
			beanManager = ApplicationServerRegistry.get().getBeanManager();
		}
		return beanManager;
	}

	@Override
	protected void createPluginManager()
	{
		pluginManager = ApplicationServerRegistry.get().getPluginManager().createEfficientCopy(this);
		pluginManager.init();
		((PluginManager)pluginManager).initClientPlugins(this, (IClientPluginAccess)(pluginAccess = createClientPluginAccess()));
		((FoundSetManager)getFoundSetManager()).setColumnManangers(pluginManager.getColumnValidatorManager(), pluginManager.getColumnConverterManager(),
			pluginManager.getUIConverterManager());
	}

	protected IClientPluginAccess createClientPluginAccess()
	{
		return new ClientPluginAccessProvider(this);
	}

	public ICmdManager getCmdManager()
	{
		if (cmdManager == null)
		{
			cmdManager = new ICmdManager()
			{
				public void executeRegisteredAction(String name)
				{
				}

				public void registerAction(String name, Action a)
				{
				}

				public Action getRegisteredAction(String name)
				{
					return null;
				}

				public void executeCmd(ICmd c, EventObject ie)
				{
				}

				public void init()
				{
				}

				public void flushCachedItems()
				{
				}
			};
		}
		return cmdManager;
	}

	public ILAFManager getLAFManager()
	{
		return null;
	}


	@Override
	public boolean saveSolution()
	{
		return true;//not needed here
	}

	public void updateUI(int time)
	{
		// no use for session/webclients/headless clients
	}

	@Override
	protected void saveSettings()
	{
		//do nothing
	}

	@Override
	protected SolutionMetaData showSolutionSelection(SolutionMetaData[] solutions)
	{
		return null;
	}

	@Override
	public void activateSolutionMethod(String globalMethodName, StartupArguments argumentsScope)
	{
		//not needed cannot push to client
	}

	public void setStatusProgress(int progress)
	{
	}

	@Override
	public void showSolutionLoading(boolean loading)
	{
	}

	public void setStatusText(String text, String tooltip)
	{
	}

	public void setTitle(String title)
	{
	}

	public ImageIcon loadImage(String name)
	{
		return null;
	}

}