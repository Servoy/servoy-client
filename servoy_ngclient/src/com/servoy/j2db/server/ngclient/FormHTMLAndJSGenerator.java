/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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
import java.io.PrintWriter;
import java.io.StringWriter;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.INGClientWindow.IFormHTMLAndJSGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormLayoutStructureGenerator;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;

/**
 * Class that can generate and cache the form template HTML and JS.
 * Useful when a form was used in multiple windows and we need to resend it's templates, so that we don't regenerate the HTML and JS for each window (it uses tbe cacbe instead).
 *
 * @author acostescu
 */
public class FormHTMLAndJSGenerator implements IFormHTMLAndJSGenerator
{

	protected final INGApplication application;
	protected final Form form;
	protected final String realFormName;
	protected String cachedHTMLTemplate;
	protected String cachedJSTemplate;

	public FormHTMLAndJSGenerator(INGApplication application, Form form, String realFormName)
	{
		this.application = application;
		this.form = form;
		this.realFormName = realFormName;
	}

	@Override
	public String generateHTMLTemplate()
	{
		if (cachedHTMLTemplate == null)
		{
			StringWriter htmlTemplate = new StringWriter(512);
			PrintWriter w = new PrintWriter(htmlTemplate);
			if (form.isResponsiveLayout())
			{
				FormLayoutStructureGenerator.generateLayout(form, realFormName, application.getFlattenedSolution(), w, false);
			}
			else
			{
				FormLayoutGenerator.generateRecordViewForm(w, form, realFormName, new ServoyDataConverterContext(application), false);
			}
			w.flush();
			w.close();
			cachedHTMLTemplate = htmlTemplate.toString();
		}
		return cachedHTMLTemplate;
	}

	@Override
	public String generateJS() throws IOException
	{
		if (cachedJSTemplate == null)
		{
			StringWriter jsTemplate = new StringWriter(512);
			IWebFormController cachedFormController = application.getFormManager().getCachedFormController(realFormName);
			ServoyDataConverterContext context = cachedFormController != null ? new ServoyDataConverterContext(cachedFormController)
				: new ServoyDataConverterContext(application);
			new FormTemplateGenerator(context, true, false).generate(form, realFormName, "form_recordview_js.ftl", jsTemplate);
			cachedJSTemplate = jsTemplate.toString();
		}
		return cachedJSTemplate;
	}
}
