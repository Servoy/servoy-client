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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * 
 * media resources url:
 * 	/resources/[clientuuid_or_solutionname]/[mediaid]/[media.name]
 *  /resources/[mediaid] (mediaid returned by MediaResourcesServlet.getMediaInfo(byte[]))
 *  /resources/upload/[clientuuid]/[formName]/[elementName]/[propertyName] for binary upload
 *
 */
@WebServlet("/resources/*")
public class MediaResourcesServlet extends HttpServlet
{

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		boolean dataSend = false;
		String path = req.getPathInfo();
		if (path.startsWith("/")) path = path.substring(1);
		String[] paths = path.split("/");
		byte[] mediaData = null;
		String contentType = null;
		if (paths.length == 3)
		{
			String clientUUID = paths[0];
			int blobID = Utils.getAsInteger(paths[1]);
			String mediaName = paths[2];
			// try to look it up as clientId. (solution model)
			IApplication client = NGClientEndpoint.getClient(clientUUID);
			if (client == null)
			{
				IDebugClientHandler debugClientHandler = ApplicationServerRegistry.get().getDebugClientHandler();
				if (debugClientHandler != null)
				{
					client = debugClientHandler.getDebugNGClient();
				}
			}
			if (client != null)
			{
				Media media = client.getFlattenedSolution().getMedia(mediaName);
				if (media != null)
				{
					mediaData = media.getMediaData();
					contentType = media.getMimeType();
				}
			}
			else
			{
				IRepository localRepository = ApplicationServerRegistry.get().getLocalRepository();
				try
				{
					mediaData = localRepository.getMediaBlob(blobID);
				}
				catch (RepositoryException e)
				{
				}
			}
		}
		else if (paths.length == 1)
		{
			Iterator<Map.Entry<byte[], MediaInfo>> entryIte = mediaBytesMap.entrySet().iterator();
			Map.Entry<byte[], MediaInfo> entry;
			while (entryIte.hasNext())
			{
				entry = entryIte.next();
				if (paths[0].equals(entry.getValue().getName()))
				{
					mediaData = entry.getKey();
					contentType = entry.getValue().getContentType();
				}
			}
		}
		if (mediaData != null && mediaData.length > 0)
		{
			if (contentType == null)
			{
				contentType = MimeTypes.getContentType(mediaData, null);
			}
			if (contentType != null) resp.setContentType(contentType);
			resp.setContentLength(mediaData.length);
			ServletOutputStream outputStream = resp.getOutputStream();
			outputStream.write(mediaData);
			outputStream.flush();
			dataSend = true;
		}

		if (!dataSend) resp.sendError(HttpServletResponse.SC_NOT_FOUND);
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
					ServletFileUpload upload = new ServletFileUpload();
					FileItemIterator iterator = upload.getItemIterator(req);
					if (iterator.hasNext())
					{
						FileItemStream item = iterator.next();
						byte[] data = read(item.openStream());
						String clientID = paths[1];
						String formName = paths[2];
						String elementName = paths[3];
						String propertyName = paths[4];

						NGClient client = (NGClient)NGClientEndpoint.getClient(clientID);
						IWebFormUI form = client.getFormManager().getForm(formName).getFormUI();
						WebComponent webComponent = form.getWebComponent(elementName);
						form.getDataAdapterList().pushChanges(webComponent, propertyName, data);
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

		MediaInfo(String name, String contentType)
		{
			this.name = name;
			this.contentType = contentType;
		}

		public String getName()
		{
			return name;
		}

		public String getContentType()
		{
			return contentType;
		}
	}
}
