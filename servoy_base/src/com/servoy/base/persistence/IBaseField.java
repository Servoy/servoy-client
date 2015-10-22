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

package com.servoy.base.persistence;

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Base interface for graphical components (for mobile as well as other clients).
 *
 * @author rgansevles
 *
 * @since 7.0
 */
@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
public interface IBaseField extends IBaseFieldCommon
{
	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMField#getValuelist()
	 */
	int getValuelistID();

	void setValuelistID(int arg);

	int getOnActionMethodID();

	void setOnActionMethodID(int arg);

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMField#getOnDataChange()
	 *
	 */
	int getOnDataChangeMethodID();

	void setOnDataChangeMethodID(int arg);

	@ServoyClientSupport(ng = false, mc = false, wc = true, sc = true)
	void setOnRenderMethodID(int arg);

	@ServoyClientSupport(ng = false, mc = false, wc = true, sc = true)
	int getOnRenderMethodID();


}