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

package com.servoy.j2db.server.headlessclient.dataui;

import javax.swing.JEditorPane;

import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.util.convert.ConversionException;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.server.headlessclient.yui.YUILoader;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.scripting.AbstractRuntimeTextEditor;

/**
 * html editor component, bound to YUI editor
 * @author jblok
 */
@SuppressWarnings("nls")
public class WebDataHtmlArea extends WebDataTextArea
{
	public static final String htmlTextStartTags = "<html><body>";
	public static final String htmlTextEndTags = "</body></html>";

	public WebDataHtmlArea(IApplication application, AbstractRuntimeTextEditor<IFieldComponent, JEditorPane> scriptable, String id)
	{
		super(application, scriptable, id);
	}

	/**
	 * @see org.apache.wicket.Component#renderHead(org.apache.wicket.markup.html.internal.HtmlHeaderContainer)
	 */
	@Override
	public void renderHead(final HtmlHeaderContainer container)
	{
		super.renderHead(container);
		IHeaderResponse response = container.getHeaderResponse();
		YUILoader.renderHTMLEdit(response);

		String script = "Servoy.HTMLEdit.attach(document.getElementById('" + getMarkupId() + "'));";
		if (findParent(WebTabPanel.class) != null)
		{
			response.renderOnDomReadyJavascript("setTimeout(function(){" + script + "}, 0);");
		}
		else
		{
			response.renderOnDomReadyJavascript(script);
		}
	}

	@Override
	protected Object convertValue(String[] value) throws ConversionException
	{
		String convert = (String)super.convertValue(value);
		if (convert != null && !convert.startsWith(htmlTextStartTags))
		{
			// html editor takes away <html> tags making text incompatible with SC
			convert = htmlTextStartTags + convert + htmlTextEndTags;
		}
		return convert;
	}
}
