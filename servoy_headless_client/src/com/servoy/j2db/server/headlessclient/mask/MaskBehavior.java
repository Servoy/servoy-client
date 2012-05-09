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
package com.servoy.j2db.server.headlessclient.mask;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;

import com.servoy.j2db.server.headlessclient.dataui.WebDataField;
import com.servoy.j2db.ui.ISupportEventExecutor;

/**
 * Behavior used in {@link WebDataField} when a masked format is used.
 * It is build on jquery and jquery maskedinput.
 * 
 * @author jcompagner
 *
 */
public class MaskBehavior extends AbstractBehavior
{
	public final static ResourceReference jquery_js = new JavascriptResourceReference(MaskBehavior.class, "jquery.min.js"); //$NON-NLS-1$
	public final static ResourceReference masked_js = new JavascriptResourceReference(MaskBehavior.class, "jquery.maskedinput-1.2.2.js"); //$NON-NLS-1$
	private final TextField< ? > textField;
	private final String displayFormat;
	private final String placeHolder;

	@SuppressWarnings("nls")
	public MaskBehavior(String displayFormat, String editFormat, TextField< ? > textField)
	{
		this.textField = textField;
		this.displayFormat = displayFormat;
		this.placeHolder = editFormat == null || editFormat.trim().equals("") ? " " : editFormat;
	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#renderHead(org.apache.wicket.markup.html.IHeaderResponse)
	 */
	@SuppressWarnings("nls")
	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.renderJavascriptReference(jquery_js);
		response.renderJavascriptReference(masked_js);

		String js = "jQuery(function($){$(\"#" + textField.getMarkupId() + "\").mask(\"" + displayFormat.replace("\"", "\\\"") + "\",{placeholder:\"" +
			placeHolder.replace("\"", "\\\"") + "\"});});";
		response.renderOnDomReadyJavascript(js);


	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#isEnabled(org.apache.wicket.Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		if (component instanceof ISupportEventExecutor) return ((ISupportEventExecutor)component).getEventExecutor().getValidationEnabled();
		return true;
	}
}
