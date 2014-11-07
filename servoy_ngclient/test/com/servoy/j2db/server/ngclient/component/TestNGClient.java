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

package com.servoy.j2db.server.ngclient.component;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;
import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.IWebsocketEndpoint;

import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerAccess;
import com.servoy.j2db.server.shared.IClientManager;

/**
 * @author Johan
 *
 */
final class TestNGClient extends NGClient
{
	/**
	 *
	 */
	private final TestRepository tr;

	/**
	 * @param wsSession
	 * @param tr
	 */
	TestNGClient(TestRepository tr) throws Exception
	{
		super(new TestClientWebsocketSession());
		this.tr = tr;
		((TestClientWebsocketSession)getWebsocketSession()).setClient(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.NGClient#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		return Locale.ENGLISH;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ClientState#createRepository()
	 */
	@Override
	protected IRepository createRepository() throws RemoteException
	{
		return tr;
	}

	@Override
	protected boolean startApplicationServerConnection()
	{
		applicationServer = new IApplicationServer()
		{

			@Override
			public ClientLogin login(Credentials credentials) throws RemoteException, RepositoryException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SolutionMetaData[] getSolutionDefinitions(int solutionTypeFilter) throws RemoteException, RepositoryException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SolutionMetaData getSolutionDefinition(String solutionName, int solutionTypeFilter) throws RemoteException, RepositoryException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Remote getRemoteService(String cid, String rmiLookupName) throws RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SolutionMetaData[] getLoginSolutionDefinitions(SolutionMetaData solutionMetaData) throws RemoteException, RepositoryException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Solution getLoginSolution(SolutionMetaData mainSolution, SolutionMetaData loginSolution) throws RemoteException, RepositoryException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getClientID(String user_uid, String password) throws RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IClientHost getClientHost() throws RemoteException
			{

				System.out.println("return le client host now");
				return new IClientHost()
				{

					@Override
					public void unregister(String client_id) throws RemoteException
					{
						// TODO Auto-generated method stub

					}

					@Override
					public Object[] register(IUserClient c, ClientInfo clientInfo) throws RemoteException
					{
						// TODO Auto-generated method stub
						return new Object[] { "uuid", new Integer(IClientManager.REGISTER_OK) };
					}

					@Override
					public void pushClientInfo(String clientId, ClientInfo clientInfo) throws RemoteException
					{
						// TODO Auto-generated method stub

					}

					@Override
					public Date getServerTime(String client_id) throws RemoteException
					{
						// TODO Auto-generated method stub
						return null;
					}
				};
			}

			@Override
			public IApplicationServerAccess getApplicationServerAccess(String clientId) throws RemoteException
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public long[] getActiveRootObjectsLastModified(int[] rootObjectIds) throws RemoteException, RepositoryException
			{
				// TODO Auto-generated method stub
				return null;
			}
		};
		return true;
	}

	@Override
	protected void createPluginManager()
	{
	}
}

/**
 * @author jcompagner
 *
 */
class TestClientWebsocketSession implements INGClientWebsocketSession
{
	private INGApplication client;

	/**
	 * @param client the client to set
	 */
	public void setClient(INGApplication client)
	{
		this.client = client;
	}

	@Override
	public void valueChanged()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void registerEndpoint(IWebsocketEndpoint endpoint)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onOpen(String argument)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isValid()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object invokeApi(WebComponent receiver, WebComponentApiDefinition apiFunction, Object[] arguments, PropertyDescription argumentTypes)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handleMessage(JSONObject obj)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getUuid()
	{
		return null;
	}

	@Override
	public List<IWebsocketEndpoint> getRegisteredEnpoints()
	{
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}

	@Override
	public IEventDispatcher getEventDispatcher()
	{
		return new IEventDispatcher()
		{

			@Override
			public void run()
			{
			}

			@Override
			public void suspend(Object object)
			{
			}

			@Override
			public void resume(Object object)
			{
			}

			@Override
			public boolean isEventDispatchThread()
			{
				return true;
			}

			@Override
			public void destroy()
			{
			}

			@Override
			public void addEvent(Runnable event)
			{
				event.run();
			}
		};
	}

	@Override
	public void deregisterEndpoint(IWebsocketEndpoint endpoint)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void updateForm(Form form, String name)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void touchForm(Form flattenedForm, String realInstanceName, boolean async)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void stopHandlingEvent()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void startHandlingEvent()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void solutionLoaded(Solution flattenedSolution)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public INGApplication getClient()
	{
		return client;
	}

	@Override
	public void formCreated(String formName)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void closeSession()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void registerServerService(String name, IServerService service)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public IServerService getServerService(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IClientService getService(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.websocket.IWebsocketSession#getForm(java.lang.String)
	 */
	@Override
	public Container getForm(String formName)
	{
		// TODO Auto-generated method stub
		return null;
	}
}