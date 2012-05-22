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

package com.servoy.j2db.ui.scripting;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IScriptScriptButtonMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;

/**
 * Scriptable button.
 * 
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeScriptButton extends AbstractRuntimeButton<IButton> implements IScriptScriptButtonMethods
{
	private String text;

	public RuntimeScriptButton(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public String js_getText()
	{
		if (text != null) return text;
		return getComponent().getText();
	}

	public void js_setText(String txt)
	{
		text = txt;
		if (txt != null && txt.startsWith("i18n:")) //$NON-NLS-1$
		{
			getComponent().setText(application.getI18NMessage(txt));
		}
		else
		{
			getComponent().setText(txt);
		}
		getChangesRecorder().setChanged();
	}
}
