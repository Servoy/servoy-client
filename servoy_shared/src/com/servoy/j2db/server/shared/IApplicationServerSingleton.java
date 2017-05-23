/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.server.shared;

import java.io.File;
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
import com.servoy.j2db.util.xmlxport.IXMLExportUserChannel;
import com.servoy.j2db.util.xmlxport.IXMLExporter;
import com.servoy.j2db.util.xmlxport.IXMLImportEngine;
import com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;
import com.servoy.j2db.util.xmlxport.RootObjectImportInfo;
import com.servoy.j2db.util.xmlxport.VersionInfo;


/**
 * public application server methods
 * @author rgansevles
 */
public interface IApplicationServerSingleton
{
	IRepository getLocalRepository();

	IDataServer getDataServer();

	String getClientId();

	/**
	 * Get the classloader for use by JarManager subclasses like plugin/bean/laf/dirver managers
	 * @return
	 */
	ClassLoader getBaseClassLoader();

	IDebugClientHandler getDebugClientHandler();

	IPluginManagerInternal getPluginManager();

	ILAFManagerInternal getLafManager();

	IBeanManagerInternal getBeanManager();

	IBeanManager createBeanManager(ClassLoader pluginClassloader);

	IUserManager getUserManager();

	IBatchManager getBatchManager();

	boolean isDeveloperStartup();

	boolean isServerStartup();

	IWebClientSessionFactory getWebClientSessionFactory();

	int getUsedRMIRegistryPort();

	Map<String, HttpServlet> getWebServices();

	int getWebServerPort();

	void setWebServerPort(int port);

	boolean checkRuntimeLicense(String companyName, String license);

	boolean checkMobileLicense(String companyName, String license);

	boolean hasDeveloperLicense();

	String checkDefaultServoyAuthorisation(Object userName, Object password);

	long getStartTime();

	void shutDown(int exitCode);

	ScheduledExecutorService getExecutor();

	boolean isStarting();

	String getServoyApplicationServerDirectory();

	void shutDown() throws Exception;

	IDeveloperRepository getDeveloperRepository();

	IServerManagerInternal getServerManager();

	IXMLExporter createXMLExporter(AbstractRepository repository, IUserManager ssm, IXMLExportUserChannel userChannel, Properties properties,
		IDataServer sqlEngine, String clientID, IXMLExportI18NHelper i18nHelper);

	IXMLImportEngine createXMLImportEngine(File file, AbstractRepository repository, IDataServer dataServer, String cid, IXMLImportUserChannel userChannel)
		throws RepositoryException;

	IXMLImportHandlerVersions11AndHigher createXMLInMemoryImportHandler(VersionInfo versionInfo, IDataServer dataServer, String cid,
		IXMLImportUserChannel userChannel, AbstractRepository repository) throws RepositoryException;

	boolean checkSolutionProtection(RootObjectImportInfo rootObjectImportInfo) throws RepositoryException;

	boolean checkSolutionPassword(RootObjectImportInfo rootObjectImportInfo, String protectionPassword) throws RepositoryException;

	void doNativeShutdown();

	String calculateProtectionPassword(SolutionMetaData metadata, String password);

	String calculateProtectionPasswordOld(SolutionMetaData metadata, String hash1);

	boolean isSolutionProtected(SolutionMetaData metadata);

	void setServerProcess(String clientID);

	IServerStatus getServerStatus();

	IServerAccess getServerAccess();

	public <S> S getService(Class<S> reference);

	public boolean hadIncompatibleExtensionsWhenStarted();

	boolean isClientRepositoryAccessAllowed(String serverName);

	boolean isClientRepositoryAccessAllowed();

	void checkClientRepositoryAccess(String serverName) throws RepositoryException;

	boolean checkClientLicense(String companyName, String license, String numberOfLicenses);
}
