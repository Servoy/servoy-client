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

import static com.servoy.j2db.util.UUID.randomUUID;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.util.HTTPUtils;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.plugins.IUploadData;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.JSMap;
import com.servoy.j2db.scripting.JSUpload;
import com.servoy.j2db.server.ngclient.less.LessCompiler;
import com.servoy.j2db.server.ngclient.property.ComponentTypeConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.ui.IMediaFieldConstants;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SecuritySupport;
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
public class MediaResourcesServlet extends HttpServlet
{
	private final FileCleaningTracker FILE_CLEANING_TRACKER = new FileCleaningTracker();

	/**
	 * constant for calling a service, should be in sync with servoy.ts generateServiceUploadUrl() function
	 */
	private static final String SERVICE_UPLOAD = "svy_services";

	/**
	 * the folder that contains the compiled less files
	 */
	public static final String SERVOY_SOLUTION_CSS = "servoy_solution_css/";

	public static final String FLATTENED_SOLUTION_ACCESS = "fs";
	public static final String DYNAMIC_DATA_ACCESS = "dynamic";

	private static File tempDir;
	private static final ConcurrentHashMap<String, MediaInfo> dynamicMediasMap = new ConcurrentHashMap<>();

	public static MediaInfo createMediaInfo(byte[] mediaBytes, String fileName, String contentType, String contentDisposition)
	{
		MediaInfo mediaInfo = new MediaInfo(randomUUID().toString(), fileName, contentType == null ? MimeTypes.getContentType(mediaBytes, null) : contentType,
			contentDisposition, mediaBytes);
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
		FILE_CLEANING_TRACKER.exitWhenFinished();
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
				String decrypt = SecuritySupport.decrypt(Settings.getInstance(), encrypted);
				found = clientnr != null && sendData(resp, MediaURLStreamHandler.getBlobLoaderMedia(getClient(req, Integer.parseInt(clientnr)), decrypt),
					MediaURLStreamHandler.getBlobLoaderMimeType(decrypt), MediaURLStreamHandler.getBlobLoaderFileName(decrypt), null);
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
		if (getSession(request, clientnr) != null && dynamicMediasMap.containsKey(dynamicID))
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
		return sendData(response, media.getName().endsWith(".less") ? LessCompiler.compileSolutionLessFile(media, fs).getBytes("UTF-8") : media.getMediaData(),
			media.getName().endsWith(".less") ? "text/css" : media.getMimeType(), media.getName(), null);
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

	protected IApplication getClient(HttpServletRequest request, int clientnr)
	{
		INGClientWebsocketSession wsSession = getSession(request, clientnr);
		return wsSession != null ? wsSession.getClient() : null;
	}

	protected INGClientWebsocketSession getSession(HttpServletRequest request, int clientnr)
	{
		// try to look it up as clientnr. (solution model)
		HttpSession httpSession = request.getSession(false);
		if (httpSession != null)
		{
			return (INGClientWebsocketSession)WebsocketSessionManager.getSession(WebsocketSessionFactory.CLIENT_ENDPOINT, httpSession, clientnr);
		}
		return null;
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
		String reqEncoding = req.getCharacterEncoding() == null ? "UTF-8" : req.getCharacterEncoding();

		if ((paths.length == 2 || paths.length >= 5) && paths[0].equals("upload"))
		{
			if (req.getHeader("Content-Type") != null && req.getHeader("Content-Type").startsWith("multipart/form-data"))
			{
				int clientnr = paths[1].length() == 0 ? -1 : Integer.parseInt(paths[1]);
				final INGClientWebsocketSession wsSession = getSession(req, clientnr);
				try
				{
					final String formName = paths.length >= 5 ? paths[2] : null;
					final String elementName = paths.length >= 5 ? paths[3] : null;
					final String propertyName = paths.length >= 5 ? paths[4] : null;
					final String rowID = paths.length >= 6 ? paths[5] : null;
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
						Iterator<FileItem> iterator = upload.parseRequest(req).iterator();
						final List<FileUploadData> aFileUploadData = new ArrayList<FileUploadData>();
						List<FileItem> formFields = new ArrayList<>();
						while (iterator.hasNext())
						{
							FileItem item = iterator.next();
							if (item.isFormField())
							{
								formFields.add(item);
							}
							else if (formName != null && elementName != null && propertyName != null)
							{
								final Map<String, Object> fileData = new HashMap<String, Object>();
								fileData.put("", item.get());
								fileData.put(IMediaFieldConstants.FILENAME, item.getName());
								fileData.put(IMediaFieldConstants.MIMETYPE, item.getContentType());
								final List<FileItem> fields = formFields;
								formFields = new ArrayList<>();

								((NGClient)wsSession.getClient()).invokeLater(new Runnable()
								{
									@Override
									public void run()
									{
										String encoding = StringUtils.defaultString(req.getCharacterEncoding(), "UTF-8");

										Map<String, String> formFields = new JSMap<>();
										for (FileItem fileItem : fields)
										{
											try
											{
												formFields.put(fileItem.getFieldName(), fileItem.getString(encoding));
											}
											catch (UnsupportedEncodingException e)
											{
												Debug.error(e);
											}
										}
										if (formName.equals(SERVICE_UPLOAD))
										{
											Scriptable plugins = (Scriptable)wsSession.getClient().getScriptEngine().getSolutionScope().get(
												IExecutingEnviroment.TOPLEVEL_PLUGINS, null);
											Scriptable plugin = (Scriptable)plugins.get(elementName, plugins);
											if (plugin != null)
											{
												Object func = plugin.get(propertyName, plugin);
												if (func instanceof Function)
												{
													Context context = Context.enter();
													try
													{
														((Function)func).call(context, plugin, plugin, new Object[] { new JSUpload(item, formFields) });
													}
													finally
													{
														Context.exit();
													}
												}
											}
										}
										else
										{
											IWebFormUI form = wsSession.getClient().getFormManager().getForm(formName).getFormUI();
											if (form == null)
											{
												Debug.error("uploading data for:  " + formName + ", element: " + elementName + ", property: " + propertyName +
													" but form is not found, data: " + fileData);
												return;
											}
											WebFormComponent webComponent = form.getWebComponent(elementName);
											if (webComponent == null)
											{
												Debug.error("uploading data for:  " + formName + ", element: " + elementName + ", property: " + propertyName +
													" but component  is not found, data: " + fileData);
												return;
											}
											// if the property is a event handler  then just call that event with the FileUploadData as the argument
											if (webComponent.hasEvent(propertyName))
											{
												try
												{
													webComponent.executeEvent(propertyName, new Object[] { new JSUpload(item, formFields) });
												}
												catch (Exception e)
												{
													Debug.error("Error calling the upload event handler " + propertyName + "   of " + webComponent, e);
												}
											}
											else
											{
												boolean isListFormComponent = false;
												WebObjectSpecification spec = webComponent.getParent().getSpecification();
												if (spec != null)
												{
													Collection<PropertyDescription> formComponentProperties = spec
														.getProperties(FormComponentPropertyType.INSTANCE);
													if (formComponentProperties != null)
													{
														for (PropertyDescription property : formComponentProperties)
														{
															if (property.getConfig() instanceof ComponentTypeConfig &&
																((ComponentTypeConfig)property.getConfig()).forFoundset != null)
															{
																isListFormComponent = true;
																FoundsetTypeSabloValue foundsetPropertyValue = (FoundsetTypeSabloValue)webComponent
																	.getParent().getProperty(((ComponentTypeConfig)property.getConfig()).forFoundset);
																if (rowID != null)
																{
																	IFoundSetInternal foundset = foundsetPropertyValue.getFoundset();

																	Pair<String, Integer> splitHashAndIndex = FoundsetTypeSabloValue.splitPKHashAndIndex(rowID);
																	if (foundset != null)
																	{
																		int recordIndex = foundset.getRecordIndex(splitHashAndIndex.getLeft(),
																			splitHashAndIndex.getRight().intValue());

																		if (recordIndex != -1)
																		{
																			foundsetPropertyValue.getDataAdapterList()
																				.setRecordQuietly(foundset.getRecord(recordIndex));
																		}
																	}
																}
																foundsetPropertyValue.getDataAdapterList().pushChanges(webComponent, propertyName, fileData,
																	null);
																break;
															}

														}
													}
												}
												if (!isListFormComponent) form.getDataAdapterList().pushChanges(webComponent, propertyName, fileData, null);
											}
										}
									}
								});
							}
							else
							{
								// it is a file from the built-in file selector
								aFileUploadData.add(new FileUploadData(item));
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
					res.getWriter().print(
						wsSession.getClient().getI18NMessage("servoy.filechooser.sizeExceeded", new Object[] { ex.getPermittedSize() / 1000 + "KB" }));
				}
				catch (FileUploadException ex)
				{
					ex.printStackTrace();
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
}
