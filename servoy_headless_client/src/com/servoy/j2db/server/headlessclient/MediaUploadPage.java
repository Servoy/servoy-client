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

import org.apache.wicket.IPageMap;
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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.plugins.IUploadData;
import com.servoy.j2db.server.headlessclient.dataui.RecordItemModel;
import com.servoy.j2db.server.headlessclient.dataui.WebDataImgMediaField;
import com.servoy.j2db.ui.IMediaFieldConstants;


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
	public MediaUploadPage(IPageMap pageMap, final RecordItemModel model, final WebDataImgMediaField field, IApplication application)
	{
		super(pageMap);
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
	}

	@SuppressWarnings({ "nls", "unchecked" })
	public MediaUploadPage(IPageMap pageMap, final IMediaUploadCallback callback, boolean multiSelect, IApplication application)
	{
		super(pageMap);

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
				}
				else if (mfuf != null)
				{
					Collection<FileUpload> uploads = mfuf.getModelObject();
					if (uploads != null && uploads.size() > 0)
					{
						IUploadData[] data = new IUploadData[uploads.size()];
						int counter = 0;
						for (FileUpload fileUpload : uploads)
						{
							data[counter++] = new FileUploadData(fileUpload);
						}
						callback.uploadComplete(data);
					}
				}
				callback.onSubmit();
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
			return fu.getInputStream();
		}


	}


}
