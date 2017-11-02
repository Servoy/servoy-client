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

import org.apache.wicket.Request;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.IMultipartWebRequest;
import org.apache.wicket.util.upload.FileItem;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.server.headlessclient.MediaUploadPage.ServoyFileUpload;

/**
 * @author jcompagner
 *
 */
public class SingleFileUpload extends Panel implements IHeaderContributor
{
	private static final ResourceReference JS = new JavascriptResourceReference(SingleFileUpload.class, "SingleFileUpload.js");
	private final FileUploadField fuf;
	private final IApplication application;

	/**
	 * @param id
	 */
	public SingleFileUpload(String id, IApplication application, String acceptFilter)
	{
		super(id);
		this.application = application;
		fuf = new FileUploadField("fileupload")
		{
			protected ServoyFileUpload overriddenFileUpload;

			/**
			 * @see org.apache.wicket.MarkupContainer#onRender(org.apache.wicket.markup.MarkupStream)
			 */
			@Override
			protected void onRender(MarkupStream markupStream)
			{
				// TODO Auto-generated method stub
				super.onRender(markupStream);
			}

			/**
			 * this method is identical to it's super except it uses overridenFileUpload fild
			 * This field is of type ServoyFileUpload which is again identical to FileUpload except it has access to the FileItem object
			 */
			@Override
			public FileUpload getFileUpload()
			{
				// Get request
				final Request request = getRequest();

				// If we successfully installed a multipart request
				if (request instanceof IMultipartWebRequest)
				{
					// Get the item for the path
					final FileItem item = ((IMultipartWebRequest)request).getFile(getInputName());

					// Only update the model when there is a file (larger than zero
					// bytes)
					if (item != null && item.getSize() > 0)
					{
						if (overriddenFileUpload == null)
						{
							overriddenFileUpload = new ServoyFileUpload(item);
						}

						return overriddenFileUpload;
					}
				}
				return null;
			}

			/**
			 * Identical with super except for overriddenFileUplod
			 */
			@Override
			protected void onDetach()
			{
				if ((overriddenFileUpload != null) && forceCloseStreamsOnDetach())
				{
					overriddenFileUpload.closeStreams();
					overriddenFileUpload = null;

					if (getModel() != null)
					{
						getModel().setObject(null);
					}
				}
				super.onDetach();
			}
		};

		if (acceptFilter.length() > 0)
		{
			fuf.add(new SimpleAttributeModifier("accept", acceptFilter));
		}
		add(fuf);
		add(new Label("filelabel", application.getI18NMessage("servoy.menuitem.file")));
		Button fileButton = new Button("filebutton", new Model<String>(application.getI18NMessage("servoy.filechooser.button.upload")));

		fileButton.setOutputMarkupId(false);
		add(fileButton);
	}

	/**
	 * @return
	 */
	public FileUpload getFileUpload()
	{
		return fuf.getFileUpload();
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		response.renderJavascriptReference(JS);
		String translatedMessage = "'" + application.getI18NMessage("servoy.filechooser.upload.fileUploading") + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		response.renderOnLoadJavascript("addSFUInputChangeListener(" + translatedMessage + ")"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
