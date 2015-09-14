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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.Response;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.MultiFileUploadField;
import org.apache.wicket.markup.html.internal.HeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.IMultipartWebRequest;
import org.apache.wicket.util.convert.ConversionException;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.server.headlessclient.MediaUploadPage.ServoyFileUpload;

/**
 * @author jcompagner
 *
 */
public class MultiFileUpload extends MultiFileUploadField
{

	private static final ResourceReference JS = new JavascriptResourceReference(MultiFileUpload.class, "MultiFileUpload.js");
	private final IApplication application;

	public MultiFileUpload(String id, IModel<Collection<FileUpload>> model, IApplication application)
	{
		super(id, model);
		this.application = application;
		Button fileButton = new Button("filebutton", new Model<String>(application.getI18NMessage("servoy.filechooser.button.upload")));
		fileButton.setOutputMarkupId(false);
		add(fileButton);
	}

	@Override
	public void renderHead(final IHeaderResponse response)
	{
		response.renderJavascriptReference(JS); // overwrites the MultiFileUploadField.js completely (can't be extended using prototypes because it assigns it's methods in the constructor)

		// the HeaderResponse dummy is a hack to be able to intercept the object creation and replace some of the code in parent JS (it is using private fields)
		super.renderHead(new HeaderResponse()
		{
			@Override
			public void renderJavascriptReference(ResourceReference reference)
			{
				response.renderJavascriptReference(reference);
			}

			@Override
			public void renderOnDomReadyJavascript(String javascript)
			{
				int splitIdx = javascript.lastIndexOf(".addElement("); //$NON-NLS-1$
				if (splitIdx >= 0)
				{
					String constructorCall = javascript.substring(0, splitIdx);
					String functionCall = javascript.substring(splitIdx);
					String translatedMessages = "[ '" + application.getI18NMessage("servoy.filechooser.upload.addMoreFiles") + "' , '" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						application.getI18NMessage("servoy.filechooser.upload.filesUploading") + "' ]"; //$NON-NLS-1$ //$NON-NLS-2$
					response.renderOnDomReadyJavascript(
						"var o = " + constructorCall + "; MultipleFileUploadInterceptor(o, " + translatedMessages + ")" + functionCall); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				else response.renderOnDomReadyJavascript(javascript);
			}

			@Override
			protected Response getRealResponse()
			{
				// not interested
				return null;
			}
		});
	}


	/**
	 * this method is identical to it's super except it creates a ServoyFileUpload.
	 * ServoyFileUpload is again identical to FileUpload except it has access to the FileItem object
	 */
	@Override
	protected Collection<FileUpload> convertValue(String[] value) throws ConversionException
	{
		// convert the array of filenames into a collection of FileItems

		Collection<FileUpload> uploads = null;

		final String[] filenames = getInputAsArray();

		if (filenames != null)
		{
			final IMultipartWebRequest request = (IMultipartWebRequest)getRequest();

			uploads = new ArrayList<FileUpload>(filenames.length);

			for (String filename : filenames)
			{
				uploads.add(new ServoyFileUpload(request.getFile(filename)));
			}
		}

		return uploads;

	}

}
