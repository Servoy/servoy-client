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

package com.servoy.j2db.server.ngclient.property;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.MessageHandler.Partial;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.junit.Before;
import org.sablo.InMemPackageReader;
import org.sablo.specification.WebComponentPackage;
import org.sablo.specification.WebComponentPackage.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.persistence.ChangeHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.server.ngclient.endpoint.NGClientEndpoint;
import com.servoy.j2db.server.ngclient.property.types.Types;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;


/**
 * @author jcompagner
 *
 */
public abstract class AbstractSolutionTest
{

	private static IPackageReader[] getReaders(File[] packages, IPackageReader customComponents)
	{
		ArrayList<IPackageReader> readers = new ArrayList<>();
		if (customComponents != null) readers.add(customComponents);
		for (File f : packages)
		{
			if (f.exists())
			{
				if (f.isDirectory()) readers.add(new WebComponentPackage.DirPackageReader(f));
				else readers.add(new WebComponentPackage.JarPackageReader(f));
			}
			else
			{
				Debug.error("A web component package location does not exist: " + f.getAbsolutePath()); //$NON-NLS-1$
			}
		}
		return readers.toArray(new IPackageReader[readers.size()]);
	}

	protected IValidateName validator = new IValidateName()
	{
		@Override
		public void checkName(String nameToCheck, int skip_element_id, ValidatorSearchContext searchContext, boolean sqlRelated) throws RepositoryException
		{
		}
	};
	protected Solution solution;
	protected TestNGClient client;

	/**
	 *
	 */
	public AbstractSolutionTest()
	{
		super();
	}

	@Before
	public void buildSolution() throws Exception
	{
		Types.registerTypes();

		File[] locations = new File[1];
		final File f = new File(PersistFieldInstanceTest.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		locations[0] = new File(f.getAbsoluteFile() + "/war/servoydefault/"); //in eclipse we .. out of bin, in jenkins we .. out of @dot

		InMemPackageReader inMemPackageReader = getTestComponents();

		WebComponentSpecProvider.init(getReaders(locations, inMemPackageReader));

		WebServiceSpecProvider.init(getReaders(new File[] { new File(f.getAbsoluteFile(), "/../war/servoyservices/") }, null));

		final TestRepository tr = new TestRepository();
		try
		{
			ApplicationServerRegistry.setApplicationServerSingleton(new TestApplicationServer());
			UUID uuid = UUID.randomUUID();
			final RootObjectMetaData metadata = tr.createRootObjectMetaData(tr.getElementIdForUUID(uuid), uuid, "Test", IRepository.SOLUTIONS, 1, 1);

			solution = (Solution)tr.createRootObject(metadata);
			tr.cacheRootObject(solution);
			solution.setChangeHandler(new ChangeHandler(tr));
			fillTestSolution();

			client = new TestNGClient(tr)
			{
				@Override
				public boolean loadSolutionsAndModules(SolutionMetaData solutionMetaData)
				{
					boolean b = super.loadSolutionsAndModules(solutionMetaData);
					try
					{
						setupData();
					}
					catch (ServoyException e)
					{
						e.printStackTrace();
					}
					return b;
				}
			};
			J2DBGlobals.setServiceProvider(client);
			client.setUseLoginSolution(false);
			NGClientEndpoint endpoint = new NGClientEndpoint();
			endpoint.start(new Session()
			{

				@Override
				public void setMaxTextMessageBufferSize(int arg0)
				{
				}

				@Override
				public void setMaxIdleTimeout(long arg0)
				{
				}

				@Override
				public void setMaxBinaryMessageBufferSize(int arg0)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void removeMessageHandler(MessageHandler arg0)
				{
					// TODO Auto-generated method stub

				}

				@Override
				public boolean isSecure()
				{
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean isOpen()
				{
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public Map<String, Object> getUserProperties()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Principal getUserPrincipal()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public URI getRequestURI()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Map<String, List<String>> getRequestParameterMap()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String getQueryString()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String getProtocolVersion()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Map<String, String> getPathParameters()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Set<Session> getOpenSessions()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String getNegotiatedSubprotocol()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public List<Extension> getNegotiatedExtensions()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Set<MessageHandler> getMessageHandlers()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public int getMaxTextMessageBufferSize()
				{
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public long getMaxIdleTimeout()
				{
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public int getMaxBinaryMessageBufferSize()
				{
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public String getId()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public WebSocketContainer getContainer()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Basic getBasicRemote()
				{
					return new Basic()
					{

						@Override
						public void setBatchingAllowed(boolean arg0) throws IOException
						{
							// TODO Auto-generated method stub

						}

						@Override
						public void sendPong(ByteBuffer arg0) throws IOException, IllegalArgumentException
						{
							// TODO Auto-generated method stub

						}

						@Override
						public void sendPing(ByteBuffer arg0) throws IOException, IllegalArgumentException
						{
							// TODO Auto-generated method stub

						}

						@Override
						public boolean getBatchingAllowed()
						{
							// TODO Auto-generated method stub
							return false;
						}

						@Override
						public void flushBatch() throws IOException
						{
							// TODO Auto-generated method stub

						}

						@Override
						public void sendText(String arg0, boolean arg1) throws IOException
						{
							// TODO Auto-generated method stub

						}

						@Override
						public void sendText(String arg0) throws IOException
						{
							// TODO Auto-generated method stub

						}

						@Override
						public void sendObject(Object arg0) throws IOException, EncodeException
						{
							// TODO Auto-generated method stub

						}

						@Override
						public void sendBinary(ByteBuffer arg0, boolean arg1) throws IOException
						{
							// TODO Auto-generated method stub

						}

						@Override
						public void sendBinary(ByteBuffer arg0) throws IOException
						{
							// TODO Auto-generated method stub

						}

						@Override
						public Writer getSendWriter() throws IOException
						{
							// TODO Auto-generated method stub
							return null;
						}

						@Override
						public OutputStream getSendStream() throws IOException
						{
							// TODO Auto-generated method stub
							return null;
						}
					};
				}

				@Override
				public Async getAsyncRemote()
				{
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void close(CloseReason arg0) throws IOException
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void close() throws IOException
				{
					// TODO Auto-generated method stub

				}

				@Override
				public <T> void addMessageHandler(Class<T> arg0, Whole<T> arg1) throws IllegalStateException
				{
					// TODO Auto-generated method stub

				}

				@Override
				public <T> void addMessageHandler(Class<T> arg0, Partial<T> arg1) throws IllegalStateException
				{
					// TODO Auto-generated method stub

				}

				@Override
				public void addMessageHandler(MessageHandler arg0) throws IllegalStateException
				{
					// TODO Auto-generated method stub

				}
			}, "1", null, "Test");
			WebsocketEndpoint.set(endpoint);
		}
		catch (RepositoryException e)
		{
			e.printStackTrace();
		}
	}

	protected abstract void setupData() throws ServoyException;

	/**
	 * @throws RepositoryException
	 * @throws ServoyException
	 */
	protected abstract void fillTestSolution() throws ServoyException;

	/**
	 * @return
	 * @throws IOException
	 */
	protected abstract InMemPackageReader getTestComponents() throws IOException;

}