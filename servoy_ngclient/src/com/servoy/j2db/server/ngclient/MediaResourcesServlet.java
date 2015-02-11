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

package com.servoy.j2db.server.ngclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.util.upload.FileItemIterator;
import org.apache.wicket.util.upload.FileItemStream;
import org.apache.wicket.util.upload.FileUploadException;
import org.apache.wicket.util.upload.ServletFileUpload;
import org.sablo.eventthread.WebsocketSessionEndpoints;
import org.sablo.websocket.IWebsocketEndpoint;
import org.sablo.websocket.WebsocketEndpoint;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.ui.IMediaFieldConstants;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HTTPUtils;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.UUID;

/**
 * Supported resources URLs:<br><br>
 *
 * Get:
 * <ul>
 * <li>/resources/fs/[rootSolutionName]/[media.name.including.mediafolderpath] - for flattened solution access - useful when resources such as CSS link to each other relatively by name/path:</li>
 * <li>/resources/fs/[rootSolutionName]/[media.name.including.mediafolderpath]?uuid=... - for SolutionModel altered media access ((dynamic)) flattened solution of a specific client; not cached)
 * <li>/resources/dynamic/[dynamic_uuid] - for on-the-fly content (for example being served directly from the database); 'dynamic_uuid' is the one returned by MediaResourcesServlet.getMediaInfo(byte[]))</li>
 * </ul>
 * Post:
 * <ul>
 * <li>/resources/upload/[clientuuid]/[formName]/[elementName]/[propertyName] - for binary upload</li>
 * </ul>
 *
 * @author jcompagner
 */
//@SuppressWarnings("nls")
@WebServlet("/resources/*")
public class MediaResourcesServlet extends HttpServlet
{

	public static final String FLATTENED_SOLUTION_ACCESS = "fs";
	public static final String DYNAMIC_DATA_ACCESS = "dynamic";

	// the key here is normally referenced inside the data model (record, ...) so it shouldn't get disposed
	// while it's still in use there
	private static final WeakHashMap<byte[], MediaInfo> mediaBytesMap = new WeakHashMap<>();

	public static synchronized MediaInfo getMediaInfo(byte[] mediaBytes)
	{
		MediaInfo mediaInfo = null;
		if (!mediaBytesMap.containsKey(mediaBytes))
		{
			mediaInfo = new MediaInfo(UUID.randomUUID().toString(), MimeTypes.getContentType(mediaBytes, null));
			mediaBytesMap.put(mediaBytes, mediaInfo);
		}

		return mediaInfo == null ? mediaBytesMap.get(mediaBytes) : mediaInfo;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		boolean found = false;

		String path = req.getPathInfo();
		if (path.startsWith("/")) path = path.substring(1);
		String[] paths = path.split("/");

		if (paths.length > 1)
		{
			String accessType = paths[0];
			switch (accessType)
			{
				case FLATTENED_SOLUTION_ACCESS :
					if (paths.length >= 3)
					{
						String clientUUID = req.getParameter("uuid");
						StringBuffer mediaName = new StringBuffer();
						for (int i = 2; i < paths.length - 1; i++)
							mediaName.append(paths[i]).append('/');
						mediaName.append(paths[paths.length - 1]);

						if (clientUUID == null) found = sendFlattenedSolutionBasedMedia(req, resp, paths[1], mediaName.toString());
						else found = sendClientFlattenedSolutionBasedMedia(req, resp, clientUUID, mediaName.toString());
					}
					break;

				case DYNAMIC_DATA_ACCESS :
					if (paths.length == 2) found = sendDynamicData(req, resp, paths[1]);
					break;

				default :
					break;
			}
		}
		else if ("servoy_blobloader".equals(path))
		{
			String encrypted = req.getParameter("blob");
			try
			{
				String decrypt = SecuritySupport.decrypt(Settings.getInstance(), encrypted);
				String clientUUID = req.getParameter("uuid");
				found = sendData(resp, MediaURLStreamHandler.getBlobLoaderMedia(getClient(clientUUID), decrypt),
					MediaURLStreamHandler.getBlobLoaderMimeType(decrypt), MediaURLStreamHandler.getBlobLoaderFileName(decrypt));
			}
			catch (Exception e)
			{
				Debug.error("could not decrypt blobloader: " + encrypted);
			}
		}

		if (!found) resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	private boolean sendDynamicData(HttpServletRequest request, HttpServletResponse response, String dynamicID) throws IOException
	{
		Iterator<Map.Entry<byte[], MediaInfo>> entryIte = mediaBytesMap.entrySet().iterator();
		Map.Entry<byte[], MediaInfo> entry;
		while (entryIte.hasNext())
		{
			entry = entryIte.next();
			if (dynamicID.equals(entry.getValue().getName()))
			{
				if (HTTPUtils.checkAndSetUnmodified(request, response, entry.getValue().getLastModifiedTimeStamp())) return true;

				return sendData(response, entry.getKey(), entry.getValue().getContentType(), null);
			}
		}
		return false;
	}

	private boolean sendFlattenedSolutionBasedMedia(HttpServletRequest request, HttpServletResponse response, String rootSolutionName, String mediaName)
		throws IOException
	{
		FlattenedSolution fs = null;
		try
		{
			IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
			fs = new FlattenedSolution((SolutionMetaData)ApplicationServerRegistry.get().getLocalRepository().getRootObjectMetaData(rootSolutionName,
				IRepository.SOLUTIONS), new AbstractActiveSolutionHandler(as)
			{
				@Override
				public IRepository getRepository()
				{
					return ApplicationServerRegistry.get().getLocalRepository();
				}
			});
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		try
		{
			Media media = fs.getMedia(mediaName);
			if (media != null)
			{
				return sendData(request, response, fs, media);
			}
		}
		finally
		{
			fs.close(null);
		}
		return false;
	}

	private boolean sendData(HttpServletRequest request, HttpServletResponse response, FlattenedSolution fs, Media media) throws IOException
	{
		// cache resources on client until changed
		if (HTTPUtils.checkAndSetUnmodified(request, response, fs.getLastModifiedTime())) return true;

		return sendData(response, media.getMediaData(), media.getMimeType(), media.getName());
	}

	private boolean sendClientFlattenedSolutionBasedMedia(HttpServletRequest request, HttpServletResponse response, String clientUUID, String mediaName)
		throws IOException
	{
		IApplication client = getClient(clientUUID);

		if (client != null)
		{
			FlattenedSolution fs = client.getFlattenedSolution();
			if (fs != null)
			{
				Media media = fs.getMedia(mediaName);
				if (media != null)
				{
					return sendData(request, response, fs, media);
				}
			}
		}
		return false;
	}

	/**
	 * @param clientUUID
	 * @return
	 */
	private IApplication getClient(String clientUUID)
	{
		// try to look it up as clientId. (solution model)
		INGClientWebsocketSession wsSession = (INGClientWebsocketSession)WebsocketSessionManager.getSession(WebsocketSessionFactory.CLIENT_ENDPOINT, clientUUID);

		IApplication client = null;
		if (wsSession == null)
		{
			IDebugClientHandler debugClientHandler = ApplicationServerRegistry.get().getDebugClientHandler();
			if (debugClientHandler != null)
			{
				client = debugClientHandler.getDebugNGClient();
			}
		}
		else
		{
			client = wsSession.getClient();
		}
		return client;
	}

	private boolean sendData(HttpServletResponse resp, byte[] mediaData, String contentType, String fileName) throws IOException
	{
		boolean dataWasSent = false;
		if (mediaData != null && mediaData.length > 0)
		{
			String ct = contentType;
			if (ct == null)
			{
				ct = MimeTypes.getContentType(mediaData, fileName);
			}
			if (ct != null) resp.setContentType(ct);
			resp.setContentLength(mediaData.length);
			ServletOutputStream outputStream = resp.getOutputStream();
			outputStream.write(mediaData);
			outputStream.flush();
			dataWasSent = true;
		}
		return dataWasSent;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
	{
		String path = req.getPathInfo();
		if (path.startsWith("/")) path = path.substring(1);
		String[] paths = path.split("/");

		if (paths.length == 5 && paths[0].equals("upload"))
		{
			if (req.getHeader("Content-Type") != null && req.getHeader("Content-Type").startsWith("multipart/form-data"))
			{
				try
				{
					String clientID = paths[1];
					String formName = paths[2];
					String elementName = paths[3];
					String propertyName = paths[4];
					INGClientWebsocketSession wsSession = (INGClientWebsocketSession)WebsocketSessionManager.getSession(
						WebsocketSessionFactory.CLIENT_ENDPOINT, clientID);
					if (wsSession != null)
					{
						ServletFileUpload upload = new ServletFileUpload();
						FileItemIterator iterator = upload.getItemIterator(req);
						if (iterator.hasNext())
						{
							FileItemStream item = iterator.next();
							byte[] data = read(item.openStream());
							HashMap<String, Object> fileData = new HashMap<String, Object>();
							fileData.put("", data);
							fileData.put(IMediaFieldConstants.FILENAME, item.getName());
							fileData.put(IMediaFieldConstants.MIMETYPE, item.getContentType());

							IWebFormUI form = wsSession.getClient().getFormManager().getForm(formName).getFormUI();
							WebFormComponent webComponent = form.getWebComponent(elementName);
							IWebsocketEndpoint previous = WebsocketEndpoint.set(new WebsocketSessionEndpoints(wsSession));
							try
							{

								form.getDataAdapterList().pushChanges(webComponent, propertyName, fileData);
								wsSession.valueChanged();
							}
							finally
							{
								WebsocketEndpoint.set(previous);
							}
						}
					}
				}
				catch (FileUploadException ex)
				{
					throw new ServletException(ex.toString());
				}
			}
		}
	}

	private byte[] read(InputStream stream) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try
		{
			byte[] buffer = new byte[2048];
			int len;
			while ((len = stream.read(buffer)) != -1)
			{
				bos.write(buffer, 0, len);
			}
			bos.flush();
			return bos.toByteArray();
		}
		finally
		{
			bos.close();
		}
	}

	public static class MediaInfo
	{
		private final String name;
		private final String contentType;
		private final long modifiedTimeStamp;

		MediaInfo(String name, String contentType)
		{
			this.name = name;
			this.contentType = contentType;
			modifiedTimeStamp = System.currentTimeMillis();
		}

		public String getName()
		{
			return name;
		}

		public String getContentType()
		{
			return contentType;
		}

		public long getLastModifiedTimeStamp()
		{
			return modifiedTimeStamp;
		}

	}
}
