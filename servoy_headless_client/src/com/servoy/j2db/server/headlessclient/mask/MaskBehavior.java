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
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextField;

import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.server.headlessclient.dataui.WebDataField;
import com.servoy.j2db.ui.ISupportEventExecutor;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.runtime.IRuntimeInputComponent;

/**
 * Behavior used in {@link WebDataField} when a masked format is used.
 * It is build on jquery and jquery maskedinput.
 * 
 * @author jcompagner
 *
 */
public class MaskBehavior extends AbstractBehavior
{
	private final TextField< ? > textField;
	private final String displayFormat;
	private final String placeHolder;
	private final String allowedCharacters;

	public MaskBehavior(String displayFormat, String editFormat, TextField< ? > textField)
	{
		this(displayFormat, editFormat, textField, null);
	}

	/**
	 * @param displayFormat2
	 * @param placeHolder2
	 * @param webDataField
	 * @param allowedCharacters
	 */
	@SuppressWarnings("nls")
	public MaskBehavior(String displayFormat, String editFormat, TextField< ? > textField, String allowedCharacters)
	{
		this.textField = textField;
		this.displayFormat = displayFormat;
		this.allowedCharacters = allowedCharacters;
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

		if (allowedCharacters != null)
		{
			response.renderOnDomReadyJavascript("jQuery(function($){$(\"#" + textField.getMarkupId() + "\").mask(\"" + displayFormat.replace("\"", "\\\"") +
				"\",{placeholder:\"" + placeHolder.replace("\"", "\\\"") + "\", allowedCharacters:\"" + allowedCharacters.replace("\"", "\\\"") + "\"});});");
		}
		else
		{
			response.renderOnDomReadyJavascript("jQuery(function($){$(\"#" + textField.getMarkupId() + "\").mask(\"" + displayFormat.replace("\"", "\\\"") +
				"\",{placeholder:\"" + placeHolder.replace("\"", "\\\"") + "\"});});");
		}
	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#isEnabled(org.apache.wicket.Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		Object scriptable = component;
		if (component instanceof IScriptableProvider) scriptable = ((IScriptableProvider)component).getScriptObject();
		if (scriptable instanceof IRuntimeComponent && !((IRuntimeComponent)scriptable).isEnabled()) return false;
		if (scriptable instanceof IRuntimeInputComponent && !((IRuntimeInputComponent)scriptable).isEditable()) return false;
		if (component instanceof ISupportEventExecutor) return ((ISupportEventExecutor)component).getEventExecutor().getValidationEnabled();
		return true;
	}
}
