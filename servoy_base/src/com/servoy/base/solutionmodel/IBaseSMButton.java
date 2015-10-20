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

package com.servoy.base.solutionmodel;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.mobile.IMobileSMButtonConstants;

/**
 * Solution model button object (for mobile as well as other clients).
 * 
 * @author rgansevles
 *
 * @since 7.1
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseSMButton extends IBaseSMGraphicalComponent, IMobileSMButtonConstants
{
	/**
	 * Icon shown on a button.
	 * 
	 * @sample
	 * var btn = form.newButton('I am a button', 1, null);
	 * btn.iconType = JSButton.ICON_STAR
	 */
	@ServoyClientSupport(mc = true, wc = false, sc = false)
	public String getIconType();

	public void setIconType(String method);

	@Override
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public IBaseSMMethod getOnAction();

}
