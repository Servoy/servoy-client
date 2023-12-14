/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;

import com.servoy.j2db.plugins.IFile;
import com.servoy.j2db.scripting.JSMap;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;

/**
 * @author jcompagner
 * @since 2021.12
 */
@SuppressWarnings("nls")
@WebServlet("/tus/*")
public class TusServlet extends AbstractMediaResourceServlet
{
	private TusFileUploadService tusFileUploadService = null;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		Settings settings = Settings.getInstance();
		String uploadDir = settings.getProperty("servoy.ng_web_client.temp.uploadir");
		long maxUpload = Utils.getAsLong(settings.getProperty("servoy.webclient.maxuploadsize", "0"), false);
		File fileUploadDir = null;
		if (uploadDir != null)
		{
			fileUploadDir = new File(uploadDir);
			if (!fileUploadDir.exists() && !fileUploadDir.mkdirs())
			{
				fileUploadDir = null;
				Debug.error("Couldn't use the property 'servoy.ng_web_client.temp.uploadir' value: '" + uploadDir +
					"', directory could not be created or doesn't exists");
			}
		}
		tusFileUploadService = new TusFileUploadService().withUploadURI(config.getServletContext().getContextPath() + "/tus/upload/[0-9]+/.+/.+/.+/");
		if (fileUploadDir != null)
		{
			try
			{
				tusFileUploadService = tusFileUploadService.withStoragePath(fileUploadDir.getCanonicalPath());
			}
			catch (IOException e)
			{
				throw new ServletException(e);
			}
		}
		if (maxUpload > 0)
		{
			tusFileUploadService = tusFileUploadService.withMaxUploadSize(Long.valueOf(maxUpload));
		}
	}

	@Override
	public void destroy()
	{
		super.destroy();
		try
		{
			this.tusFileUploadService.cleanup();
		}
		catch (IOException e)
		{
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		tusFileUploadService.process(req, resp);
		String uploadURI = Paths.get(req.getRequestURI()).normalize().toString().replace('\\', '/');
		UploadInfo uploadInfo = null;
		try
		{
			uploadInfo = this.tusFileUploadService.getUploadInfo(uploadURI);
		}
		catch (IOException | TusException e)
		{
			e.printStackTrace();
		}

		if (uploadInfo != null && !uploadInfo.isUploadInProgress())
		{
			try
			{
				Path uploadedPath = this.tusFileUploadService.getUploadedPath(uploadURI);
				File file = uploadedPath.toFile();
				TusFileItem fileItem = new TusFileItem(file, uploadInfo, uploadURI);
				AbstractMediaResourceServlet.FILE_CLEANING_TRACKER.track(file, file, new TusFileDeleteStategy(fileItem));

				String path = req.getPathInfo();
				if (path.startsWith("/")) path = path.substring(1);
				String[] paths = path.split("/");

				int clientnr = paths[1].length() == 0 ? -1 : Integer.parseInt(paths[1]);
				final INGClientWebsocketSession wsSession = getSession(req, clientnr);
				if (wsSession != null)
				{
					paths[paths.length - 1] = null; // clear out the tus id that is the last path entry (so it is not confused with the row id)
					JSMap<String, String> map = new JSMap<>();
					map.putAll(uploadInfo.getMetadata());
					callClient(req, paths, wsSession, map, fileItem);
				}
			}
			catch (IOException | TusException e)
			{
				Debug.error(e);
			}
		}
	}

	private class TusFileDeleteStategy extends FileDeleteStrategy
	{

		private final TusFileItem fileItem;

		/**
		 * @param name
		 */
		protected TusFileDeleteStategy(TusFileItem fileItem)
		{
			super("TUS");
			this.fileItem = fileItem;
		}

		@Override
		protected boolean doDelete(File fileToDelete) throws IOException
		{
			this.fileItem.delete();
			return true;
		}
	}

	private class TusFileItem implements FileItem, IFile
	{

		private final File file;
		private final UploadInfo info;
		private final String uploadURI;

		private TusFileItem(File file, UploadInfo info, String uploadURI)
		{
			this.file = file;
			this.info = info;
			this.uploadURI = uploadURI;
		}

		@Override
		public FileItemHeaders getHeaders()
		{
			return null;
		}

		@Override
		public void setHeaders(FileItemHeaders arg0)
		{
		}

		@Override
		public void delete()
		{
			try
			{
				tusFileUploadService.deleteUpload(uploadURI);
			}
			catch (IOException | TusException e)
			{
				Debug.error(e);
			}
		}

		@Override
		public byte[] get()
		{
			try
			{
				return FileUtils.readFileToByteArray(file);
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
			return null;
		}

		@Override
		public String getContentType()
		{
			return this.info.getFileMimeType();
		}

		@Override
		public String getFieldName()
		{
			return null;
		}

		@Override
		public InputStream getInputStream() throws IOException
		{
			return new FileInputStream(file);
		}

		@Override
		public String getName()
		{
			return this.info.getFileName();
		}

		@Override
		public OutputStream getOutputStream() throws IOException
		{
			return new FileOutputStream(this.file);
		}

		@Override
		public long getSize()
		{
			return this.file.length();
		}

		public String getString(final String charset)
			throws UnsupportedEncodingException
		{
			return new String(get(), charset);
		}

		@Override
		public String getString()
		{
			byte[] rawdata = get();
			try
			{
				return new String(rawdata, "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				return new String(rawdata);
			}
		}

		@Override
		public boolean isFormField()
		{
			return false;
		}

		@Override
		public boolean isInMemory()
		{
			return false;
		}

		@Override
		public void setFieldName(String arg0)
		{
		}

		@Override
		public void setFormField(boolean arg0)
		{
		}

		@Override
		public void write(File toFile) throws Exception
		{
			FileUtils.moveFile(this.file, toFile);
			delete();
		}

		@Override
		public File getFile()
		{
			return file;
		}

	}
}
