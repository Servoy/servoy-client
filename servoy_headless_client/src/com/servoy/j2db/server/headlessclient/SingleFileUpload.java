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

import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.servoy.j2db.IApplication;

/**
 * @author jcompagner
 *
 */
public class SingleFileUpload extends Panel
{

	private final FileUploadField fuf;

	/**
	 * @param id
	 */
	public SingleFileUpload(String id, IApplication application)
	{
		super(id);
		fuf = new FileUploadField("fileupload")
		{
			/**
			 * @see org.apache.wicket.MarkupContainer#onRender(org.apache.wicket.markup.MarkupStream)
			 */
			@Override
			protected void onRender(MarkupStream markupStream)
			{
				// TODO Auto-generated method stub
				super.onRender(markupStream);
			}
		};
		add(fuf);
		add(new Label("filelabel", application.getI18NMessage("servoy.menuitem.file")));
		add(new Button("filebutton", new Model<String>(application.getI18NMessage("servoy.filechooser.button.upload"))));
	}

	/**
	 * @return
	 */
	public FileUpload getFileUpload()
	{
		return fuf.getFileUpload();
	}

}
