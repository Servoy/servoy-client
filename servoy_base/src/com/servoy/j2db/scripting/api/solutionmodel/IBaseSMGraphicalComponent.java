/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.scripting.api.solutionmodel;

import com.servoy.j2db.scripting.annotations.ServoyMobile;


/**
 * Solution model base interface for graphical components (for mobile as well as other clients).
 * 
 * @author rgansevles
 * @author acostescu
 *
 * @since 7.0
 */
@ServoyMobile
public interface IBaseSMGraphicalComponent extends IBaseSMComponent
{
	public String getDataProviderID();

	public boolean getDisplaysTags();

	public String getText();

	public void setDataProviderID(String arg);

	public void setDisplaysTags(boolean arg);

	public void setText(String arg);

	public void setOnAction(IBaseSMMethod method);

	public IBaseSMMethod getOnAction();

}