/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.extension;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;

import com.servoy.extension.install.CopyZipEntryImporter;
import com.servoy.extension.parser.ParseDependencyMetadata;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * This class provides extension info & data taken from the Servoy Marketplace
 * @author gboros
 */
public class MarketPlaceExtensionProvider extends CachingExtensionProvider
{
	public static final String MARKETPLACE_WS = "http://192.168.1.16:8080/servoy-service/rest_ws/marketplace/ws_extensions/"; //$NON-NLS-1$
	private static final String WS_ACTION_VERSIONS = "versions"; //$NON-NLS-1$
	private static final String WS_ACTION_EXP = "exp"; //$NON-NLS-1$
	private static final String WS_ACTION_PACKAGE_XML = "xml"; //$NON-NLS-1$
	private static final String TO_BE_INSTALLED_FOLDER = ".ws"; //$NON-NLS-1$

	private final File destinationDir;

	private final HashMap<String, String[]> availableVersionsMap = new HashMap<String, String[]>();
	private final HashMap<Pair<String, String>, File> expFileMap = new HashMap<Pair<String, String>, File>();
	private final MessageKeeper messages = new MessageKeeper();

	public MarketPlaceExtensionProvider(File installDir)
	{
		destinationDir = new File(new File(installDir, CopyZipEntryImporter.EXPFILES_FOLDER), TO_BE_INSTALLED_FOLDER);
	}

	public String[] getAvailableVersions(String extensionID)
	{
		if (!availableVersionsMap.containsKey(extensionID)) availableVersionsMap.put(extensionID, ws_getVersions(extensionID));
		return availableVersionsMap.get(extensionID);
	}

	public File getEXPFile(String extensionId, String version)
	{
		Pair<String, String> extVersion = new Pair<String, String>(extensionId, version);
		if (!expFileMap.containsKey(extVersion)) expFileMap.put(extVersion, ws_getEXP(extensionId, version));
		return expFileMap.get(extVersion);
	}

	@Override
	protected DependencyMetadata[] getDependencyMetadataImpl(ExtensionDependencyDeclaration extensionDependency)
	{
		ArrayList<DependencyMetadata> dmA = new ArrayList<DependencyMetadata>();
		for (String version : getAvailableVersions(extensionDependency.id))
		{
			if (VersionStringUtils.belongsToInterval(version, extensionDependency.minVersion, extensionDependency.maxVersion))
			{
				BufferedInputStream bis = null;
				try
				{
					URLConnection ws_connection = ws_getConnection(WS_ACTION_PACKAGE_XML, "application/binary", extensionDependency.id, version); //$NON-NLS-1$
					bis = new BufferedInputStream(ws_connection.getInputStream());
					ParseDependencyMetadata parseDM = new ParseDependencyMetadata(extensionDependency.id, messages);
					dmA.add(parseDM.runOnEntryInputStream(bis));
				}
				catch (Exception ex)
				{
					String msg = "Cannot get extension definition from marketplace. Error is : " + ex.getMessage();
					Debug.error(msg);
					messages.addError(msg);
				}
				finally
				{
					Utils.closeInputStream(bis);
				}
			}
		}

		return dmA.toArray(new DependencyMetadata[dmA.size()]);
	}

	public void dispose()
	{
		messages.clear();
		availableVersionsMap.clear();
		flushCache();

		FileUtils.deleteQuietly(destinationDir);
		expFileMap.clear();
	}

	public Message[] getMessages()
	{
		return messages.getMessages();
	}

	public void clearMessages()
	{
		messages.clear();
	}

	private String[] ws_getVersions(String extensionId)
	{
		ArrayList<String> versions = new ArrayList<String>();

		ByteArrayOutputStream bos = null;
		BufferedInputStream bis = null;
		try
		{
			URLConnection ws_connection = ws_getConnection(WS_ACTION_VERSIONS, "text/json", extensionId, null); //$NON-NLS-1$
			bos = new ByteArrayOutputStream();
			bis = new BufferedInputStream(ws_connection.getInputStream());
			byte[] buffer = new byte[1024];
			int len;
			while ((len = bis.read(buffer)) != -1)
				bos.write(buffer, 0, len);

			JSONArray jsonVersions = new JSONArray(new String(bos.toByteArray(), ws_connection.getContentEncoding()));
			for (int i = 0; i < jsonVersions.length(); i++)
				versions.add(jsonVersions.getString(i));

		}
		catch (Exception ex)
		{
			String msg = "Cannot get extension versions from marketplace. Error is : " + ex.getMessage();
			Debug.error(msg);
			messages.addError(msg);
		}
		finally
		{
			Utils.closeInputStream(bis);
			Utils.closeOutputStream(bos);
		}

		return versions.toArray(new String[versions.size()]);
	}

	private File ws_getEXP(String extensionId, String version)
	{
		BufferedInputStream bis = null;
		FileOutputStream fos = null;
		File outputFile = new File(destinationDir, extensionId + "_" + version + ".exp"); //$NON-NLS-1$ //$NON-NLS-2$
		try
		{
			destinationDir.mkdirs();
			URLConnection ws_connection = ws_getConnection(WS_ACTION_EXP, "application/binary", extensionId, version); //$NON-NLS-1$
			fos = new FileOutputStream(outputFile);
			bis = new BufferedInputStream(ws_connection.getInputStream());
			byte[] buffer = new byte[1024];
			int len;
			while ((len = bis.read(buffer)) != -1)
				fos.write(buffer, 0, len);
		}
		catch (Exception ex)
		{
			String msg = "Cannot get extension file from marketplace. Error is : " + ex.getMessage();
			Debug.error(msg);
			messages.addError(msg);
		}
		finally
		{
			Utils.closeInputStream(bis);
			Utils.closeOutputStream(fos);
		}

		return outputFile;
	}

	private URLConnection ws_getConnection(String action, String acceptContentType, String extensionId, String version) throws Exception
	{
		URL mpURL = new URL(MARKETPLACE_WS + action + "/" + extensionId + (version != null ? "/" + version : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		URLConnection urlConnection = mpURL.openConnection();

		urlConnection.addRequestProperty("accept", acceptContentType); //$NON-NLS-1$

		return urlConnection;
	}
}
