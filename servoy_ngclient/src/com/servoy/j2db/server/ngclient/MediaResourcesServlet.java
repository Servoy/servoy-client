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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
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
import org.sablo.websocket.CurrentWindow;
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
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.plugins.IUploadData;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.ui.IMediaFieldConstants;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HTTPUtils;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

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
 * <li>/resources/upload/[clientuuid]/[formName]/[elementName]/[propertyName] - for binary upload targeting an element property</li>
 * <li>/resources/upload/[clientuuid] - for binary upload of files selected with the built-in file selector</li>
 * </ul>
 *
 * @author jcompagner
 */
@SuppressWarnings("nls")
@WebServlet("/resources/*")
public class MediaResourcesServlet extends HttpServlet
{
	public static final String FLATTENED_SOLUTION_ACCESS = "fs";
	public static final String DYNAMIC_DATA_ACCESS = "dynamic";

	private static File tempDir;
	private static final ConcurrentHashMap<String, MediaInfo> dynamicMediasMap = new ConcurrentHashMap<>();

	public static MediaInfo createMediaInfo(byte[] mediaBytes, String fileName, String contentType, String contentDisposition)
	{
		MediaInfo mediaInfo = new MediaInfo(UUID.randomUUID().toString(), fileName, contentType == null ? MimeTypes.getContentType(mediaBytes, null)
			: contentType, contentDisposition, mediaBytes);
		dynamicMediasMap.put(mediaInfo.getName(), mediaInfo);
		return mediaInfo;
	}

	public static MediaInfo createMediaInfo(byte[] mediaBytes)
	{
		return createMediaInfo(mediaBytes, null, null, null);
	}

	private static void cleanupDynamicMediasMap(boolean forceAll)
	{
		long now = System.currentTimeMillis();
		for (MediaInfo mediaInfo : dynamicMediasMap.values())
		{
			if (forceAll || now - mediaInfo.getLastAccessedTimeStamp() > 3600000)
			{
				mediaInfo.destroy();
				dynamicMediasMap.remove(mediaInfo.getName());
			}
		}
	}

	@Override
	public void init(ServletConfig context) throws ServletException
	{
		super.init(context);
		try
		{
			tempDir = (File)context.getServletContext().getAttribute("javax.servlet.context.tempdir");
			if (tempDir != null)
			{
				tempDir = new File(tempDir, DYNAMIC_DATA_ACCESS);
				deleteAll(tempDir);
				tempDir.mkdir();
			}
		}
		catch (Exception ex)
		{
			Debug.error("Cannot create temp folder for dynamic resources", ex);
			tempDir = null;
		}
	}

	@Override
	public void destroy()
	{
		super.destroy();
		cleanupDynamicMediasMap(true);
		if (tempDir != null)
		{
			deleteAll(tempDir);
		}
	}

	private void deleteAll(File f)
	{
		if (!f.exists()) return;
		if (f.isDirectory())
		{
			for (File fl : f.listFiles())
				deleteAll(fl);
		}
		f.delete();
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
					MediaURLStreamHandler.getBlobLoaderMimeType(decrypt), MediaURLStreamHandler.getBlobLoaderFileName(decrypt), null);
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
		if (dynamicMediasMap.containsKey(dynamicID))
		{
			MediaInfo mediaInfo = dynamicMediasMap.get(dynamicID);
			mediaInfo.touch();
			cleanupDynamicMediasMap(false);
			if (HTTPUtils.checkAndSetUnmodified(request, response, mediaInfo.getLastModifiedTimeStamp())) return true;

			return sendData(response, mediaInfo.getData(), mediaInfo.getContentType(), mediaInfo.getFileName(), mediaInfo.getContentDisposition());
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
			SolutionMetaData solutionMetaData = (SolutionMetaData)ApplicationServerRegistry.get().getLocalRepository().getRootObjectMetaData(rootSolutionName,
				IRepository.SOLUTIONS);
			if (solutionMetaData == null)
			{
				Debug.error("Solution '" + rootSolutionName + "' was not found when sending media data for '" + mediaName + "'.");
				return false;
			}
			fs = new FlattenedSolution(solutionMetaData, new AbstractActiveSolutionHandler(as)
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

		return sendData(response, media.getMediaData(), media.getMimeType(), media.getName(), null);
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

	private boolean sendData(HttpServletResponse resp, byte[] mediaData, String contentType, String fileName, String contentDisposition) throws IOException
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
			if (fileName != null)
			{
				resp.setHeader("Content-disposition", (contentDisposition == null ? "attachment" : contentDisposition) + "; filename=\"" + fileName + "\"");
			}
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

		if ((paths.length == 2 || paths.length == 5) && paths[0].equals("upload"))
		{
			if (req.getHeader("Content-Type") != null && req.getHeader("Content-Type").startsWith("multipart/form-data"))
			{
				try
				{
					String clientID = paths[1];
					String formName = paths.length == 5 ? paths[2] : null;
					String elementName = paths.length == 5 ? paths[3] : null;
					final String propertyName = paths.length == 5 ? paths[4] : null;
					final INGClientWebsocketSession wsSession = (INGClientWebsocketSession)WebsocketSessionManager.getSession(
						WebsocketSessionFactory.CLIENT_ENDPOINT, clientID);
					if (wsSession != null)
					{
						ServletFileUpload upload = new ServletFileUpload();
						FileItemIterator iterator = upload.getItemIterator(req);
						final ArrayList<FileUploadData> aFileUploadData = new ArrayList<FileUploadData>();
						while (iterator.hasNext())
						{
							FileItemStream item = iterator.next();
							byte[] data = read(item.openStream());

							if (formName != null && elementName != null && propertyName != null)
							{
								final Map<String, Object> fileData = new HashMap<String, Object>();
								fileData.put("", data);
								fileData.put(IMediaFieldConstants.FILENAME, item.getName());
								fileData.put(IMediaFieldConstants.MIMETYPE, item.getContentType());

								final IWebFormUI form = wsSession.getClient().getFormManager().getForm(formName).getFormUI();
								final WebFormComponent webComponent = form.getWebComponent(elementName);
								CurrentWindow.runForWindow(new NGClientWebsocketSessionWindows(wsSession), new Runnable()
								{
									@Override
									public void run()
									{
										form.getDataAdapterList().pushChanges(webComponent, propertyName, fileData);
										wsSession.valueChanged();
									}
								});
							}
							else
							{
								// it is a file from the built-in file selector
								aFileUploadData.add(new FileUploadData(item.getName(), item.getContentType(), data));
							}
						}
						if (aFileUploadData.size() > 0)
						{
							final IMediaUploadCallback mediaUploadCallback = ((NGClient)wsSession.getClient()).getMediaUploadCallback();
							if (mediaUploadCallback != null)
							{
								// leave time for this request to finish, before executing the callback, so the file
								// dialog can do its close
								((NGClient)wsSession.getClient()).invokeLater(new Runnable()
								{
									@Override
									public void run()
									{
										mediaUploadCallback.uploadComplete(aFileUploadData.toArray(new FileUploadData[aFileUploadData.size()]));
										mediaUploadCallback.onSubmit();
									}
								});
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

	public static final class MediaInfo
	{
		private static final long MAX_DATA_SIZE_FOR_IN_MEMORY = 5242880; // 5MB

		private final String name;
		private final String fileName;
		private final String contentType;
		private final String contentDisposition;
		private final long modifiedTimeStamp;
		private long accessedTimeStamp;
		private byte[] data;

		MediaInfo(String name, String fileName, String contentType, String contentDisposition, byte[] data)
		{
			this.name = name;
			this.fileName = fileName;
			this.contentType = contentType;
			this.contentDisposition = contentDisposition;
			modifiedTimeStamp = accessedTimeStamp = System.currentTimeMillis();
			if (data.length < MAX_DATA_SIZE_FOR_IN_MEMORY)
			{
				this.data = data;
			}
			else
			{
				this.data = null;
				if (MediaResourcesServlet.tempDir != null)
				{
					Utils.writeFile(new File(MediaResourcesServlet.tempDir, name), data);
				}
				else
				{
					Debug.error("Cannot save dynamic data to servlet temp dir!");
				}
			}
		}

		public String getName()
		{
			return name;
		}

		public String getFileName()
		{
			return fileName;
		}

		public String getContentDisposition()
		{
			return contentDisposition;
		}

		public String getContentType()
		{
			return contentType;
		}

		public long getLastModifiedTimeStamp()
		{
			return modifiedTimeStamp;
		}

		public byte[] getData()
		{
			if (data == null)
			{
				return Utils.readFile(new File(MediaResourcesServlet.tempDir, name), -1);
			}
			return data;
		}

		public void touch()
		{
			accessedTimeStamp = System.currentTimeMillis();
		}

		public long getLastAccessedTimeStamp()
		{
			return accessedTimeStamp;
		}

		public void destroy()
		{
			if (data == null)
			{
				try
				{
					new File(MediaResourcesServlet.tempDir, name).delete();
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
			else
			{
				data = null;
			}
		}
	}

	private static final class FileUploadData implements IUploadData
	{
		String name;
		String contentType;
		byte[] data;

		private FileUploadData(String name, String contentType, byte[] data)
		{
			this.name = name;
			this.contentType = contentType;
			this.data = data;
		}

		public String getName()
		{
			String fixedName = name.replace('\\', '/');
			String[] tokenized = fixedName.split("/"); //$NON-NLS-1$
			return tokenized[tokenized.length - 1];
		}

		public String getContentType()
		{
			return contentType;
		}

		public byte[] getBytes()
		{
			return data;
		}

		/**
		 * @see com.servoy.j2db.plugins.IUploadData#getFile()
		 */
		public File getFile()
		{
			// does not represent a real file object.
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.plugins.IUploadData#getInputStream()
		 */
		public InputStream getInputStream() throws IOException
		{
			return new ByteArrayInputStream(data);
		}

		@Override
		public long lastModified()
		{
			return System.currentTimeMillis();
		}

	}
}
