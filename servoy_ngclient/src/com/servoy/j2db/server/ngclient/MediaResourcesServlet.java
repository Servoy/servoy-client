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

import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.sablo.util.HTTPUtils;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.plugins.IUploadData;
import com.servoy.j2db.scripting.JSMap;
import com.servoy.j2db.server.ngclient.less.LessCompiler;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Supported resources URLs:<br><br>
 *
 * Get:
 * <ul>
 * <li>/resources/fs/[rootSolutionName]/[media.name.including.mediafolderpath] - for flattened solution access - useful when resources such as CSS link to each other relatively by name/path:</li>
 * <li>/resources/fs/[rootSolutionName]/[media.name.including.mediafolderpath]?clientnr=... - for SolutionModel altered media access ((dynamic)) flattened solution of a specific client; not cached)
 * <li>/resources/dynamic/[dynamic_uuid] - for on-the-fly content (for example being served directly from the database); 'dynamic_uuid' is the one returned by MediaResourcesServlet.getMediaInfo(byte[]))</li>
 * </ul>
 * Post:
 * <ul>
 * <li>/resources/upload/[clientnr]/[formName]/[elementName]/[propertyName]/[rowid] - for binary upload targeting an element property</li>
 * <li>/resources/upload/[clientnr] - for binary upload of files selected with the built-in file selector</li>
 * </ul>
 *
 * @author jcompagner
 */
@SuppressWarnings("nls")
@WebServlet("/resources/*")
public class MediaResourcesServlet extends AbstractMediaResourceServlet
{
	/**
	 * the folder that contains the compiled less files
	 */
	public static final String SERVOY_SOLUTION_CSS = "servoy_solution_css/";

	public static final String FLATTENED_SOLUTION_ACCESS = "fs";
	public static final String DYNAMIC_DATA_ACCESS = "dynamic";

	private static File tempDir;

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
		String clientnr = req.getParameter("clientnr");

		if (paths.length > 1)
		{
			String accessType = paths[0];
			switch (accessType)
			{
				case FLATTENED_SOLUTION_ACCESS :
					if (paths.length >= 3)
					{
						StringBuffer mediaName = new StringBuffer();
						for (int i = 2; i < paths.length - 1; i++)
							mediaName.append(paths[i]).append('/');
						mediaName.append(paths[paths.length - 1]);

						if (clientnr == null) found = sendFlattenedSolutionBasedMedia(req, resp, paths[1], mediaName.toString());
						else found = sendClientFlattenedSolutionBasedMedia(req, resp, Integer.parseInt(clientnr), mediaName.toString());
					}
					break;

				case DYNAMIC_DATA_ACCESS :
					if (paths.length == 2) found = sendDynamicData(req, resp, paths[1], Integer.parseInt(clientnr));
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
				IApplication client = null;
				if (clientnr != null && (client = getClient(req, Integer.parseInt(clientnr))) != null)
				{
					String decrypt = client.getFlattenedSolution().getEncryptionHandler().decryptString(encrypted);
					byte[] data = MediaURLStreamHandler.getBlobLoaderMedia(client, decrypt);
					found = sendData(resp, new ByteArrayInputStream(data),
						MediaURLStreamHandler.getBlobLoaderMimeType(decrypt), MediaURLStreamHandler.getBlobLoaderFileName(decrypt), null, data.length);
				}

			}
			catch (Exception e)
			{
				Debug.error("could not decrypt blobloader: " + encrypted);
			}
		}

		if (!found) resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	private boolean sendDynamicData(HttpServletRequest request, HttpServletResponse response, String dynamicID, int clientnr) throws IOException
	{
		INGClientWebsocketSession session = getSession(request, clientnr);
		if (session != null)
		{
			MediaInfo mediaInfo = session.getClient().getMedia(dynamicID);
			if (mediaInfo != null)
			{
				mediaInfo.touch();
				if (HTTPUtils.checkAndSetUnmodified(request, response, mediaInfo.getLastModifiedTimeStamp())) return true;

				return sendData(response, mediaInfo.getInputStream(), mediaInfo.getContentType(), mediaInfo.getFileName(), mediaInfo.getContentDisposition(),
					mediaInfo.getContentLength());
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
			return findAndSendMediaData(request, response, mediaName, fs);
		}
		finally
		{
			fs.close(null);
		}
	}

	protected boolean findAndSendMediaData(HttpServletRequest request, HttpServletResponse response, String mediaName, FlattenedSolution fs) throws IOException
	{
		Media media = fs.getMedia(mediaName);
		if (media == null && mediaName.endsWith(".css"))
		{
			media = fs.getMedia(mediaName.replace(".css", ".less"));
			Solution sc = fs.getSolutionCopy(false);
			if (media != null && media.getParent() != sc)
			{
				// is a less file, try to load the compiled version
				URL url = getServletConfig().getServletContext().getResource('/' + SERVOY_SOLUTION_CSS + '/' + mediaName);
				if (url != null)
				{
					setHeaders(request, response);
					// cache resources on client until changed
					if (HTTPUtils.checkAndSetUnmodified(request, response,
						media.getLastModifiedTime() != -1 ? media.getLastModifiedTime() : fs.getLastModifiedTime())) return true;

					response.setContentType("text/css");
					URLConnection con = url.openConnection();
					long lenght = con.getContentLengthLong();
					if (lenght > 0) response.setContentLengthLong(lenght);
					try (InputStream is = con.getInputStream())
					{
						IOUtils.copy(is, response.getOutputStream());
					}
					return true;
				}
			}
		}
		if (media != null)
		{
			return sendMediaData(request, response, fs, media);
		}
		return false;
	}

	protected void setHeaders(HttpServletRequest request, HttpServletResponse response)
	{
		String param = request.getParameter("t");
		try
		{
			if (param != null && Long.parseLong(param, 16) > 0)
			{
				response.addHeader("Cache-Control", "public, max-age=" + NGCachingFilter.ONE_YEAR_MAX_AGE);
			}
		}
		catch (Exception e)
		{
			// ignore, the "t" is not a hex value.
		}
	}

	private boolean sendMediaData(HttpServletRequest request, HttpServletResponse response, FlattenedSolution fs, Media media) throws IOException
	{
		setHeaders(request, response);
		// cache resources on client until changed
		if (HTTPUtils.checkAndSetUnmodified(request, response, media.getLastModifiedTime() != -1 ? media.getLastModifiedTime() : fs.getLastModifiedTime()))
			return true;
		byte[] data = media.getName().endsWith(".less") ? LessCompiler.compileSolutionLessFile(media, fs).getBytes("UTF-8")
			: media.getMediaData();
		return sendData(response,
			new ByteArrayInputStream(data),
			media.getName().endsWith(".less") ? "text/css" : media.getMimeType(), media.getName(), null, data.length);
	}

	private boolean sendClientFlattenedSolutionBasedMedia(HttpServletRequest request, HttpServletResponse response, int clientnr, String mediaName)
		throws IOException
	{
		IApplication client = getClient(request, clientnr);

		if (client != null)
		{
			FlattenedSolution fs = client.getFlattenedSolution();
			if (fs != null)
			{
				return findAndSendMediaData(request, response, mediaName, fs);
			}
		}
		return false;
	}

	private boolean sendData(HttpServletResponse resp, InputStream inputStream, String contentType, String fileName, String contentDisposition,
		int contentLength)
		throws IOException
	{
		boolean dataWasSent = false;
		if (inputStream != null)
		{
			if (contentType != null) resp.setContentType(contentType);
			resp.setContentLength(contentLength >= 0 ? contentLength : inputStream.available());
			if (fileName != null)
			{
				resp.setHeader("Content-disposition",
					sanitizeHeader((contentDisposition == null ? "attachment" : contentDisposition) + "; filename=\"" + fileName +
						"\"; filename*=UTF-8''" + Rfc5987Util.encode(fileName, "UTF8") + ""));
			}
			ServletOutputStream outputStream = resp.getOutputStream();
			IOUtils.copy(inputStream, outputStream);
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
		String reqEncoding = req.getCharacterEncoding() == null ? "UTF-8" : req.getCharacterEncoding();

		if ((paths.length == 2 || paths.length >= 5) && paths[0].equals("upload"))
		{
			if (req.getHeader("Content-Type") != null && req.getHeader("Content-Type").startsWith("multipart/form-data"))
			{
				int clientnr = paths[1].length() == 0 ? -1 : Integer.parseInt(paths[1]);
				final INGClientWebsocketSession wsSession = getSession(req, clientnr);
				try
				{
					if (wsSession != null)
					{
						Settings settings = Settings.getInstance();
						File fileUploadDir = null;
						String uploadDir = settings.getProperty("servoy.ng_web_client.temp.uploadir");
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
						int tempFileThreshold = Utils.getAsInteger(settings.getProperty("servoy.ng_web_client.tempfile.threshold", "50"), false) * 1000;
						DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory(tempFileThreshold, fileUploadDir);
						diskFileItemFactory.setFileCleaningTracker(FILE_CLEANING_TRACKER);
						ServletFileUpload upload = new ServletFileUpload(diskFileItemFactory);
						upload.setHeaderEncoding(reqEncoding);
						long maxUpload = Utils.getAsLong(settings.getProperty("servoy.webclient.maxuploadsize", "0"), false);
						if (maxUpload > 0) upload.setFileSizeMax(maxUpload * 1000);
						final List<FileUploadData> aFileUploadData = new ArrayList<FileUploadData>();
						List<FileItem> formFields = new ArrayList<>();
						for (FileItem item : upload.parseRequest(req))
						{
							if (item.isFormField())
							{
								formFields.add(item);
							}
							else
							{
								String encoding = StringUtils.defaultString(req.getCharacterEncoding(), "UTF-8");

								JSMap<String, String> fieldsMap = new JSMap<>();
								for (FileItem fileItem : formFields)
								{
									try
									{
										fieldsMap.put(fileItem.getFieldName(), fileItem.getString(encoding));
									}
									catch (UnsupportedEncodingException e)
									{
										Debug.error(e);
									}
								}

								if (callClient(req, paths, wsSession, fieldsMap, item))
								{
									formFields = new ArrayList<>();
								}
								else
								{
									// it is a file from the built-in file selector
									aFileUploadData.add(new FileUploadData(item));
								}
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
				catch (FileSizeLimitExceededException ex)
				{
					res.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
					if (wsSession != null) res.getWriter().print(
						wsSession.getClient().getI18NMessage("servoy.filechooser.sizeExceeded", new Object[] { ex.getPermittedSize() / 1000 + "KB" }));
				}
				catch (FileUploadException ex)
				{
					Debug.error(ex);
					throw new ServletException(ex.toString());
				}
			}
		}
	}

	public static final class MediaInfo
	{
		private static final long MAX_DATA_SIZE_FOR_IN_MEMORY = 5242880; // 5MB

		private final String name;
		private final String fileName;
		private final File file;
		private final String contentType;
		private final String contentDisposition;
		private final long modifiedTimeStamp;
		private long accessedTimeStamp;
		private final Dimension mediaSize;
		private byte[] data;

		MediaInfo(String name, String fileName, String contentType, String contentDisposition, byte[] data)
		{
			this.name = name;
			this.fileName = fileName;
			this.contentType = contentType;
			this.contentDisposition = contentDisposition;
			this.file = null;
			modifiedTimeStamp = accessedTimeStamp = System.currentTimeMillis();
			this.mediaSize = ImageLoader.getSize(data);
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

		MediaInfo(String name, File file, String contentType, String contentDisposition)
		{
			this.name = name;
			this.fileName = file.getName();
			this.file = file;
			this.contentType = contentType;
			this.contentDisposition = contentDisposition;
			modifiedTimeStamp = accessedTimeStamp = System.currentTimeMillis();
			this.mediaSize = null;
			this.data = null;
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

		public Dimension getMediaSize()
		{
			return mediaSize;
		}

		public InputStream getInputStream()
		{
			if (data == null)
			{
				try
				{
					if (file != null)
					{
						return new BufferedInputStream(new FileInputStream(file));
					}
					return new BufferedInputStream(new FileInputStream(new File(MediaResourcesServlet.tempDir, name)));
				}
				catch (FileNotFoundException ex)
				{
					Debug.error(ex);
				}
				return new ByteArrayInputStream(new byte[0]);
			}
			return new ByteArrayInputStream(data);
		}

		public int getContentLength()
		{
			if (data != null)
			{
				return data.length;
			}
			return -1;
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

		public String getURL(int clientnr)
		{
			return getURL(clientnr, false);
		}

		public String getURL(int clientnr, boolean includeSize)
		{
			String url = "resources/" + MediaResourcesServlet.DYNAMIC_DATA_ACCESS + "/" + this.getName() + "?clientnr=" + clientnr;
			if (includeSize && this.mediaSize != null)
			{
				url += "&imageWidth=" + this.mediaSize.width + "&imageHeight=" + this.mediaSize.height;
			}
			return url;
		}
	}

	private static final class FileUploadData implements IUploadData
	{
		private final FileItem item;

		private FileUploadData(FileItem item)
		{
			this.item = item;
		}

		public String getName()
		{
			String name = item.getName();

			// when uploading from localhost some browsers will specify the entire path, we strip it
			// down to just the file name
			name = Utils.lastPathComponent(name, '/');
			name = Utils.lastPathComponent(name, '\\');

			name = name.replace('\\', '/');
			String[] tokenized = name.split("/"); //$NON-NLS-1$
			return tokenized[tokenized.length - 1];
		}

		public String getContentType()
		{
			return item.getContentType();
		}

		public byte[] getBytes()
		{
			return item.get();
		}

		/**
		 * @see com.servoy.j2db.plugins.IUploadData#getFile()
		 */
		public File getFile()
		{
			if (item instanceof DiskFileItem)
			{
				return ((DiskFileItem)item).getStoreLocation();
			}
			return null;
		}

		/*
		 * @see com.servoy.j2db.plugins.IUploadData#getInputStream()
		 */
		public InputStream getInputStream() throws IOException
		{
			return item.getInputStream();
		}

		@Override
		public long lastModified()
		{
			return System.currentTimeMillis();
		}
	}

	private static String sanitizeHeader(String headerValue)
	{
		return headerValue.replaceAll("[\n\r]+", " ");
	}
}
