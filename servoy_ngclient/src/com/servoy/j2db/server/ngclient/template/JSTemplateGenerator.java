/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.template;

import static org.sablo.util.TextUtils.escapeForDoubleQuotedJavascript;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;
import com.servoy.j2db.util.Debug;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
public class JSTemplateGenerator
{
	private final Configuration cfg;

	public JSTemplateGenerator()
	{
		cfg = new Configuration(Configuration.VERSION_2_3_28);

		cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "templates"));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
	}

	public void generate(String jsfile, Map<String, Object> substitutions, Writer writer) throws IOException
	{
		// main/sample_js.ftl is a template for sample.js in templates/main directory
		if (!jsfile.endsWith(".js"))
		{
			throw new IllegalArgumentException();
		}
		String templateName = jsfile.substring(0, jsfile.length() - 3) + "_js.ftl";

		Map<String, String> jsSubstitutions = new HashMap<>();
		substitutions.forEach((key, value) -> jsSubstitutions.put(key, escapeForDoubleQuotedJavascript(String.valueOf(value))));

		Template template = cfg.getTemplate(templateName);
		try
		{
			template.process(jsSubstitutions, writer);
		}
		catch (TemplateException e)
		{
			NGClientWebsocketSession.sendInternalError(e);
			Debug.error(e);
		}
	}

}
