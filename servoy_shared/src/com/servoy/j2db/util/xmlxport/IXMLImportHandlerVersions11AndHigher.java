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

package com.servoy.j2db.util.xmlxport;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.servoy.j2db.persistence.I18NUtil.MessageEntry;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;

/**
 * Handler for importing servoy files of versions 11 of higher.
 */

public interface IXMLImportHandlerVersions11AndHigher
{
	void getRootObjectNameForImport(ImportInfo importInfo, RootObjectImportInfo rootObjectImportInfo) throws RepositoryException;

	void getRootObjectIdForImport(ImportInfo importInfo, RootObjectImportInfo rootObjectImportInfo) throws RepositoryException;

	IRootObject importRootObject(RootObjectImportInfo rootObjectImportInfo)
		throws IllegalAccessException, IntrospectionException, InvocationTargetException, RepositoryException;

	void importSecurityInfo(ImportInfo importInfo, RootObjectImportInfo rootObjectImportInfo) throws ServoyException;

	void handleI18NImport(ImportInfo importInfo, String i18NServerName, String i18NTableName, TreeMap<String, MessageEntry> messages) throws Exception;

	void importMetaData(ImportInfo importInfo) throws RepositoryException;

	void importSampleData(JarFile jarFile, ImportInfo importInfo);

	void importBlobs(JarFile jarFile, List<String> blobs, ImportInfo importInfo, Map<String, byte[]> digestMap)
		throws IOException, RepositoryException, NoSuchAlgorithmException;

	void checkDatabaseInfo(ImportInfo importInfo, ImportTransactable importTransactable) throws RepositoryException;

	void importDatabaseInfo(ImportInfo importInfo, ImportTransactable importTransactable) throws Exception;

	void importUserInfo(Set<UserInfo> userInfoSet);

	IXMLImportUserChannel getUserChannel();

	void addSubstitutionName(ImportInfo importInfo, String name, RootObjectImportInfo rootObjectImportInfo, int typeId);

	void importRevisionInfo(ImportInfo importInfo) throws RepositoryException;

	void executePreImport(ImportInfo importInfo, IRootObject[] rootObjects, ImportTransactable importTransactable) throws RepositoryException;

	void importingDone(ImportInfo importInfo, IRootObject[] rootObjects, ImportTransactable importTransactable) throws RepositoryException;

	void startImport(ImportTransactable importTransactable) throws RepositoryException;

	void importingFailed(ImportInfo importInfo, ImportTransactable importTransactable, Exception e);

	boolean checkI18NStorage();

	String getPropertyValue(String oldValue);

	IPersist loadDeletedObjectByElementId(IRootObject rootObject, int elementId, ISupportChilds parent)
		throws RepositoryException, IllegalAccessException, IntrospectionException, InvocationTargetException;

	void setStyleActiveRelease(IRootObject[] rootObjects) throws RepositoryException;

	int getObjectId(boolean b, String string) throws RepositoryException;

	void setAskForImportServerName(boolean askForImportServerName);

	void checkMovedObjects(ImportInfo importInfo) throws RepositoryException, SQLException;

	void loadWebPackage(String solutionName, String webPackageName, JarFile jarFile, JarEntry jarEntry);

	List<Pair<String, byte[]>> getWebPackages(String solutionName);
}