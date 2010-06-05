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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.SimpleAttributeModifier;

import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptInputMethods;

/**
 * @author jcompagner
 *
 */
class ReadOnlyAndEnableTestAttributeModifier extends SimpleAttributeModifier
{

	/**
	 * @param eventExecutor
	 * @param attribute
	 * @param value
	 */
	ReadOnlyAndEnableTestAttributeModifier(String attribute, CharSequence value)
	{
		super(attribute, value);
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.FindModeDisabledSimpleAttributeModifier#isEnabled(org.apache.wicket.Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		if (super.isEnabled(component))
		{
			if (component instanceof IScriptBaseMethods && !((IScriptBaseMethods)component).js_isEnabled()) return false;
			if (component instanceof IScriptInputMethods && !((IScriptInputMethods)component).js_isEditable()) return false;
		}
		return true;
	}

}
