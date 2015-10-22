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

package com.servoy.base.solutionmodel.mobile;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMLabel;

/**
 * Solution model label component for mobile.
 * 
 * @author rgansevles
 * @author acostescu
 *
 * @since 7.0
 */
@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
public interface IMobileSMLabel extends IBaseSMLabel, IMobileSMHasTitle
{
	/** 
	 * Size property for a label, values 1 to 60 correspond to header class h1 to h6
	 * 
	 * @sample
	 * var label = form.newLabel('Hello', 1);
	 * label.labelSize = 2 // corresponds to header class h2
	 */
	public int getLabelSize();

	public void setLabelSize(int size);

}
