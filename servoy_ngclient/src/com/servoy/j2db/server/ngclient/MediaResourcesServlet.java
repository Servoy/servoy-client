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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * 
 * media resources url: /resources/[clientuuid_or_solutionname]/[mediaid]/[media.name]
 *
 */
@WebServlet("/resources/*")
public class MediaResourcesServlet extends HttpServlet
{
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
		if (paths.length == 3)
		{
			byte[] mediaData = null;
			String contentType = null;
			String clientUUID = paths[0];
			int blobID = Utils.getAsInteger(paths[1]);
			String mediaName = paths[2];
			// try to look it up as clientId. (solution model)
			INGApplication client = NGClientEndpoint.getClient(clientUUID);
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
			if (mediaData != null && mediaData.length > 0)
			{
				if (contentType == null)
				{
					contentType = MimeTypes.getContentType(mediaData, mediaName);
				}
				if (contentType != null) resp.setContentType(contentType);
				resp.setContentLength(mediaData.length);
				ServletOutputStream outputStream = resp.getOutputStream();
				outputStream.write(mediaData);
				outputStream.flush();
				dataSend = true;
			}
		}
		if (!dataSend) resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
}
