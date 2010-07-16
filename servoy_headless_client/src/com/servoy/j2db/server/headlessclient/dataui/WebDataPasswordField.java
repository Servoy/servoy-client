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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IScriptDataPasswordMethods;

/**
 * Represents a password field in the webbrowser.
 * 
 * @author jcompagner
 */
public class WebDataPasswordField extends WebDataField implements IScriptDataPasswordMethods
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param application
	 * @param id
	 */
	public WebDataPasswordField(IApplication application, String id)
	{
		super(application, id);
	}

	/**
	 * @see wicket.markup.html.form.TextField#getInputType()
	 */
	@Override
	protected String getInputType()
	{
		return "password";
	}


	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebDataField#js_getElementType()
	 */
	@Override
	public String js_getElementType()
	{
		return "PASSWORD";
	}

	@Override
	public String toString()
	{
		return js_getElementType() + "(web)[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ 
			",height:" + js_getHeight() + "]";
	}
}
