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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.sablo.InMemPackageReader;
import org.sablo.specification.Package;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.persistence.ChangeHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.endpoint.NGClientEndpoint;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.server.ngclient.property.types.Types;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


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
				if (f.isDirectory()) readers.add(new Package.DirPackageReader(f));
				else readers.add(new Package.ZipPackageReader(f, f.getName().substring(0, f.getName().length() - 4)));
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
	protected NGClientEndpoint endpoint;

	public AbstractSolutionTest()
	{
		super();
	}

	private static class ZipPackageReader implements Package.IPackageReader
	{
		private final ZipFile file;
		private final String pathPrefix;

		public ZipPackageReader(ZipFile file, String pathPrefix)
		{
			this.file = file;
			this.pathPrefix = pathPrefix;
		}

		@Override
		public String getName()
		{
			String[] split = file.getEntry(pathPrefix).getName().split("/");
			return split[split.length - 1].replace("/", "");
		}

		@Override
		public String getPackageName()
		{
			try
			{
				String packageDisplayname = Package.getPackageName(getManifest());
				if (packageDisplayname != null) return packageDisplayname;
			}
			catch (IOException e)
			{
				Debug.log(e);
			}

			// fall back to symbolic name
			return FilenameUtils.getBaseName(getName());
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
		public Manifest getManifest() throws IOException
		{
			ZipEntry m = file.getEntry(pathPrefix + "META-INF/MANIFEST.MF");
			try (InputStream is = file.getInputStream(m))
			{
				return new Manifest(is);
			}
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
		public String readTextFile(String path, Charset charset) throws IOException
		{
			ZipEntry m = file.getEntry(pathPrefix + path);
			try (InputStream is = file.getInputStream(m))
			{
				return Utils.getTXTFileContent(is, charset);
			}
		}

		@Override
		public URL getUrlForPath(String path) throws MalformedURLException
		{
			return null;
		}

		@Override
		public URL getPackageURL()
		{
			return null;
		}

		@Override
		public void reportError(String specpath, Exception e)
		{
			System.err.println(e.getMessage());
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
				Debug.log("Error getting package type." + getName(), e);
			}
			return null;
		}

		@Override
		public File getResource()
		{
			return null;
		}

	}

	@Before
	public void buildSolution() throws Exception
	{
		Types.getTypesInstance().registerTypes();

		final File f = new File(PersistFieldInstanceTest.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		IPackageReader[] servicesReaders = null;
		IPackageReader[] componentsReaders = null;
		InMemPackageReader inMemPackageReader = getTestComponents();
		if (f.isFile() && f.getName().startsWith("servoy_ngclient_") && f.getName().endsWith(".jar"))
		{
			ZipFile zipFile = new ZipFile(f);
			componentsReaders = inMemPackageReader != null
				? new IPackageReader[] { new ZipPackageReader(zipFile, "war/servoycore/"), new ZipPackageReader(zipFile,
					"war/servoydefault/"), inMemPackageReader }
				: new IPackageReader[] { new ZipPackageReader(zipFile, "war/servoycore/"), new ZipPackageReader(zipFile, "war/servoydefault/") };
			servicesReaders = new IPackageReader[] { new ZipPackageReader(zipFile, "war/servoyservices/") };
		}
		else
		{
			componentsReaders = getReaders(
				new File[] { new File(f.getAbsoluteFile() + "/../war/servoycore/"), new File(f.getAbsoluteFile() + "/../war/servoydefault/") },
				inMemPackageReader); //in eclipse we .. out of bin, in jenkins we .. out of @dot
			servicesReaders = getReaders(new File[] { new File(f.getAbsoluteFile(), "/../war/servoyservices/") }, null);
		}

		WebComponentSpecProvider.init(componentsReaders);
		WebServiceSpecProvider.init(servicesReaders);

		final TestRepository tr = new TestRepository();
		try
		{
			ApplicationServerRegistry.setApplicationServerSingleton(new TestApplicationServer(tr));
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
			endpoint = new NGClientEndpoint();
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
					return true;
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
					return Collections.singletonMap("solution", Arrays.asList("Test"));
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

			INGClientWebsocketSession wsSession = (INGClientWebsocketSession)WebsocketSessionManager.getSession(endpoint.getEndpointType(), "1");
			Assert.assertNotNull("no wsSession", wsSession);

			CurrentWindow.set(new NGClientWebsocketSessionWindows(wsSession));
		}
		catch (RepositoryException e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@After
	public void tearDown() throws Exception
	{
		CurrentWindow.set(null);
		FormElementHelper.INSTANCE.reload();
		ApplicationServerRegistry.clear();
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