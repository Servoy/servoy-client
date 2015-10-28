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
package com.servoy.j2db.server.headlessclient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.wicket.IPageMap;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.MultiFileUploadField;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.util.upload.DiskFileItem;
import org.apache.wicket.util.upload.DiskFileItemFactory;
import org.apache.wicket.util.upload.FileItem;
import org.apache.wicket.util.upload.FileUploadException;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.plugins.IUploadData;
import com.servoy.j2db.scripting.info.WEBCONSTANTS;
import com.servoy.j2db.server.headlessclient.dataui.RecordItemModel;
import com.servoy.j2db.server.headlessclient.dataui.WebDataImgMediaField;
import com.servoy.j2db.ui.IMediaFieldConstants;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;


/**
 * @author jcompagner
 *
 */
public class MediaUploadPage extends WebPage
{
	private static final long serialVersionUID = 1L;

	private final SingleFileUpload fuf;
	private final MultiFileUploadField mfuf;

	/**
	 * @param pageMap
	 * @param model
	 * @param dataProviderID
	 */
	@SuppressWarnings("nls")
	public MediaUploadPage(IPageMap pageMap, final RecordItemModel model, final WebDataImgMediaField field, final IApplication application)
	{
		super(pageMap);
		add(new PageContributor(application, "contribution"));
		mfuf = null;
		fuf = new SingleFileUpload("panel", application);
		Form form = new Form("form")
		{
			private static final long serialVersionUID = 1L;
			private boolean close = false;

			@Override
			protected void onSubmit()
			{
				FileUpload fu = fuf.getFileUpload();
				if (fu != null)
				{
					// use the wrap model so that form variables also work.
					IWrapModel wrappedModel = model.wrapOnInheritance(field);
					wrappedModel.setObject(fu.getBytes());
					field.getStylePropertyChanges().setChanged();

					model.setValue(field, field.getDataProviderID() + IMediaFieldConstants.FILENAME, fu.getClientFileName());
					String contentType = fu.getContentType();
					model.setValue(field, field.getDataProviderID() + IMediaFieldConstants.MIMETYPE, contentType);
				}
				close = true;
			}

			/**
			 * @see org.apache.wicket.Component#renderHead(org.apache.wicket.markup.html.internal.HtmlHeaderContainer)
			 */
			@Override
			public void renderHead(HtmlHeaderContainer container)
			{
				super.renderHead(container);
				if (close)
				{
					container.getHeaderResponse().renderOnLoadJavascript("window.opener.triggerAjaxUpdate();");
				}
			}
		};

		form.add(fuf);
		form.setMultiPart(true);
		add(form);
		FeedbackPanel panel = new FeedbackPanel("feedback"); //$NON-NLS-1$
		add(panel);

		add(CSSPackageResource.getHeaderContribution(
			"/servoy-webclient/templates/" + application.getClientProperty(WEBCONSTANTS.WEBCLIENT_TEMPLATES_DIR) + "/servoy_web_client_default.css"));
	}

	@SuppressWarnings({ "nls", "unchecked" })
	public MediaUploadPage(IPageMap pageMap, final IMediaUploadCallback callback, boolean multiSelect, final IApplication application)
	{
		super(pageMap);

		add(new PageContributor(application, "contribution"));

		Form form = new Form("form")
		{
			private static final long serialVersionUID = 1L;
			private boolean close = false;

			@Override
			protected void onSubmit()
			{
				close = true;
				if (fuf != null)
				{
					final FileUpload fu = fuf.getFileUpload();
					if (fu != null)
					{
						IUploadData ud = new FileUploadData(fu);
						callback.uploadComplete(new IUploadData[] { ud });
					}
					else
					{
						callback.uploadComplete(new IUploadData[0]);
					}
				}
				else if (mfuf != null)
				{
					Collection<FileUpload> uploads = mfuf.getModelObject();
					if (uploads != null)
					{
						IUploadData[] data = new IUploadData[uploads.size()];
						int counter = 0;
						for (FileUpload fileUpload : uploads)
						{
							data[counter++] = new FileUploadData(fileUpload);
						}
						callback.uploadComplete(data);
					}
					else
					{
						callback.uploadComplete(new IUploadData[0]);
					}
				}
				callback.onSubmit();
				if (fuf != null)
				{
					remove(fuf);
				}
				else if (mfuf != null)
				{
					remove(mfuf);
				}
				add(new Label("panel", new Model<String>(application.getI18NMessage("servoy.filechooser.upload.finished"))));
			}

			@Override
			public void renderHead(HtmlHeaderContainer container)
			{
				super.renderHead(container);
				if (close)
				{
					container.getHeaderResponse().renderOnLoadJavascript("window.parent.triggerAjaxUpdate();");
				}
			}

			@Override
			protected boolean handleMultiPart()
			{
				// Change the request to a multipart web request that is able to work with multiple file uploads for the same field nmae
				// so parameters are parsed out correctly as well
				try
				{
					final WebRequest req = ((WebRequest)getRequest());
					MultipartServletWebRequest multipart;
					try
					{
						Settings settings = Settings.getInstance();
						File fileUploadDir = null;
						String uploadDir = settings.getProperty("servoy.ng_web_client.temp.uploadir");
						if (uploadDir != null)
						{
							fileUploadDir = new File(uploadDir);
							if (!(fileUploadDir.mkdirs() && fileUploadDir.exists()))
							{
								fileUploadDir = null;
								Debug.error("Couldn't use the property 'servoy.ng_web_client.temp.uploadir' value: '" + uploadDir +
									"', directory could not be created or doesn't exists");
							}
						}
						int tempFileThreshold = Utils.getAsInteger(settings.getProperty("servoy.ng_web_client.tempfile.threshold", "50"), false) * 1000;

						multipart = new MultipartServletWebRequest(req.getHttpServletRequest(), getMaxSize(),
							new DiskFileItemFactory(tempFileThreshold, fileUploadDir)
						{
							private final HashSet<String> fieldNames = new HashSet<String>();

							@Override
							public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName)
							{
								String adjustedFieldName = fieldName;
								int i = 1;
								while (fieldNames.contains(adjustedFieldName))
								{
									adjustedFieldName = fieldName + "_additionalFile_" + (i++);
								}
								String timestampStr = req.getParameter("last_modified_" + fieldName + "_" + fileName);
								long timestamp = System.currentTimeMillis();
								if (timestampStr != null)
								{
									try
									{
										timestamp = Long.parseLong(timestampStr);
									}
									catch (NumberFormatException ex)
									{
										timestamp = System.currentTimeMillis();
									}
								}

								fieldNames.add(adjustedFieldName);
								return new ServoyDiskFileItem(adjustedFieldName, contentType, isFormField, fileName, getSizeThreshold(), getRepository(),
									timestamp);
							}

						});
					}
					catch (FileUploadException e)
					{
						throw new WicketRuntimeException(e);
					}
					multipart.setRequestParameters(req.getRequestParameters());

					getRequestCycle().setRequest(multipart);
					return true;
				}
				catch (WicketRuntimeException wre)
				{
					if (wre.getCause() == null || !(wre.getCause() instanceof FileUploadException))
					{
						throw wre;
					}

					FileUploadException e = (FileUploadException)wre.getCause();

					// Create model with exception and maximum size values
					final Map<String, Object> model = new HashMap<String, Object>();
					model.put("exception", e);
					model.put("maxSize", getMaxSize());

					onFileUploadException((FileUploadException)wre.getCause(), model);

					// don't process the form if there is a FileUploadException
					return false;
				}
			}

		};
		if (multiSelect)
		{
			fuf = null;
			IModel<Collection<FileUpload>> model = new Model(new ArrayList());
			mfuf = new MultiFileUpload("panel", model, application);
			form.add(mfuf);
		}
		else
		{
			mfuf = null;
			fuf = new SingleFileUpload("panel", application);
			form.add(fuf);
		}
		form.add(new Label("uploadtitle", new Model<String>(application.getI18NMessage("servoy.filechooser.upload.title"))));
		form.setMultiPart(true);
		add(form);
		FeedbackPanel panel = new FeedbackPanel("feedback");
		add(panel);

		add(CSSPackageResource.getHeaderContribution(
			"/servoy-webclient/templates/" + application.getClientProperty(WEBCONSTANTS.WEBCLIENT_TEMPLATES_DIR) + "/servoy_web_client_default.css"));
	}

	public static class ServoyDiskFileItem extends DiskFileItem
	{
		long timestamp = 0;

		public ServoyDiskFileItem(String fieldName, String contentType, boolean isFormField, String fileName, int sizeThreshold, File repository,
			long timestamp)
		{
			super(fieldName, contentType, isFormField, fileName, sizeThreshold, repository);
			this.timestamp = timestamp;
		}

		public long getLastModified()
		{
			return timestamp;
		}
	}

	public static class ServoyFileUpload extends FileUpload
	{

		FileItem diskItem = null;

		public ServoyFileUpload(FileItem item)
		{
			super(item);
			diskItem = item;
		}

		public long lastModified()
		{
			if (diskItem instanceof ServoyDiskFileItem)
			{
				return ((ServoyDiskFileItem)diskItem).getLastModified();
			}
			return System.currentTimeMillis();
		}

		/**
		 * @return
		 */
		public File getFile()
		{
			if (diskItem instanceof DiskFileItem)
			{
				return ((DiskFileItem)diskItem).getStoreLocation();
			}
			return null;
		}
	}
	private static final class FileUploadData implements IUploadData
	{
		private final FileUpload fu;

		private FileUploadData(FileUpload fu)
		{
			this.fu = fu;
		}

		public String getName()
		{
			String name = fu.getClientFileName();
			name = name.replace('\\', '/');
			String[] tokenized = name.split("/"); //$NON-NLS-1$
			return tokenized[tokenized.length - 1];
		}

		public String getContentType()
		{
			return fu.getContentType();
		}

		public byte[] getBytes()
		{
			return fu.getBytes();
		}

		/**
		 * @see com.servoy.j2db.plugins.IUploadData#getFile()
		 */
		public File getFile()
		{
			if (fu instanceof ServoyFileUpload)
			{
				return ((ServoyFileUpload)fu).getFile();
			}
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.plugins.IUploadData#getInputStream()
		 */
		public InputStream getInputStream() throws IOException
		{
			return fu.getInputStream();
		}

		@Override
		public long lastModified()
		{
			if (fu instanceof ServoyFileUpload)
			{
				return ((ServoyFileUpload)fu).lastModified();
			}
			return System.currentTimeMillis();
		}

	}


}
