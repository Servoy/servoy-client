/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.util.Properties;

import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.ISupportOnRender;


/**
 * Helper class for onRender event handling
 * 
 * @author gboros
 */
public class WebOnRenderHelper
{
	/**
	 * Calls onRender for the renderComponent and clears its changed flag if
	 * the callback did not change any properties
	 * 
	 * @param renderComponent
	 * @return true if properties were changed during onRender, otherwise returns false
	 */
	public static boolean doRender(ISupportOnRender renderComponent)
	{
		if (renderComponent instanceof IProviderStylePropertyChanges)
		{
			Properties changesBeforeOnRender = (Properties)((IProviderStylePropertyChanges)renderComponent).getStylePropertyChanges().getChanges().clone();
			renderComponent.fireOnRender(false);
			Properties changesAfterOnRender = ((IProviderStylePropertyChanges)renderComponent).getStylePropertyChanges().getChanges();
			if (changesBeforeOnRender.equals(changesAfterOnRender))
			{
				((IProviderStylePropertyChanges)renderComponent).getStylePropertyChanges().setRendered();
				return false;
			}
		}

		return true;
	}
}
