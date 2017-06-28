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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.http.HttpServlet;

import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IBeanManagerInternal;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.ILAFManagerInternal;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.IXMLExportI18NHelper;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.plugins.IPluginManagerInternal;
import com.servoy.j2db.plugins.IServerAccess;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.server.shared.IBatchManager;
import com.servoy.j2db.server.shared.IServerStatus;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.server.shared.IWebClientSessionFactory;
import com.servoy.j2db.util.JarManager.ExtensionResource;
import com.servoy.j2db.util.xmlxport.IXMLExportUserChannel;
import com.servoy.j2db.util.xmlxport.IXMLExporter;
import com.servoy.j2db.util.xmlxport.IXMLImportEngine;
import com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;
import com.servoy.j2db.util.xmlxport.RootObjectImportInfo;
import com.servoy.j2db.util.xmlxport.VersionInfo;

/**
 * @author jcompagner
 *
 */
final class TestApplicationServer implements IApplicationServerSingleton
{
	private final TestRepository tr;

	/**
	 * @param tr
	 */
	public TestApplicationServer(TestRepository tr)
	{
		this.tr = tr;
	}

	@Override
	public void shutDown() throws Exception
	{
	}

	@Override
	public void shutDown(int exitCode)
	{
	}

	@Override
	public void setWebServerPort(int port)
	{
	}

	@Override
	public void setServerProcess(String clientID)
	{
	}

	@Override
	public boolean isStarting()
	{
		return false;
	}

	@Override
	public boolean isSolutionProtected(SolutionMetaData metadata)
	{
		return false;
	}

	@Override
	public boolean isDeveloperStartup()
	{
		return false;
	}

	@Override
	public boolean isClientRepositoryAccessAllowed()
	{
		return true;
	}

	@Override
	public boolean isClientRepositoryAccessAllowed(String serverName)
	{
		return true;
	}

	@Override
	public boolean hasDeveloperLicense()
	{
		return true;
	}

	@Override
	public boolean hadIncompatibleExtensionsWhenStarted()
	{
		return false;
	}

	@Override
	public Map<String, HttpServlet> getWebServices()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getWebServerPort()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IWebClientSessionFactory getWebClientSessionFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IUserManager getUserManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getUsedRMIRegistryPort()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getStartTime()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getServoyApplicationServerDirectory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S> S getService(Class<S> reference)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IServerStatus getServerStatus()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IServerManagerInternal getServerManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IServerAccess getServerAccess()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPluginManagerInternal getPluginManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IRepository getLocalRepository()
	{
		return tr;
	}

	@Override
	public ILAFManagerInternal getLafManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScheduledExecutorService getExecutor()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IDeveloperRepository getDeveloperRepository()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IDebugClientHandler getDebugClientHandler()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IDataServer getDataServer()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClientId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBeanManagerInternal getBeanManager()
	{
		return new IBeanManagerInternal()
		{

			@Override
			public void init()
			{
			}

			@Override
			public void flushCachedItems()
			{
			}

			@Override
			public ClassLoader getClassLoader()
			{
				return TestApplicationServer.class.getClassLoader();
			}

			@Override
			public Object createInstance(String clazzName) throws Exception
			{
				return null;
			}

			@Override
			public Map<String, List<ExtensionResource>> getLoadedBeanDefs()
			{
				return null;
			}

			@Override
			public File getBeansDir()
			{
				return null;
			}


			@Override
			public void dispose()
			{

			}
		};
	}

	@Override
	public IBatchManager getBatchManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ClassLoader getBaseClassLoader()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void doNativeShutdown()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public IXMLImportHandlerVersions11AndHigher createXMLInMemoryImportHandler(VersionInfo versionInfo, IDataServer dataServer, String cid,
		IXMLImportUserChannel userChannel, AbstractRepository repository) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IXMLImportEngine createXMLImportEngine(File file, AbstractRepository repository, IDataServer dataServer, String cid,
		IXMLImportUserChannel userChannel) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IXMLExporter createXMLExporter(AbstractRepository repository, IUserManager ssm, IXMLExportUserChannel userChannel, Properties properties,
		IDataServer sqlEngine, String clientID, IXMLExportI18NHelper i18nHelper)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBeanManager createBeanManager(ClassLoader pluginClassloader)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkSolutionProtection(RootObjectImportInfo rootObjectImportInfo) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkSolutionPassword(RootObjectImportInfo rootObjectImportInfo, String protectionPassword) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkRuntimeLicense(String companyName, String license)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkMobileLicense(String companyName, String license)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String checkDefaultServoyAuthorisation(Object userName, Object password)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkClientRepositoryAccess(String serverName) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String calculateProtectionPasswordOld(SolutionMetaData metadata, String hash1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String calculateProtectionPassword(SolutionMetaData metadata, String password)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkClientLicense(String companyName, String license, String numberOfLicenses)
	{
		return false;
	}
}