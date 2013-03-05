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

package com.servoy.j2db.solutionmodel;

import com.servoy.base.solutionmodel.IBaseSMDefaults;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.j2db.scripting.solutionmodel.JSMethod;

/**
 * Default/none/ignore constants for some property types.
 * 
 * @since 7.0
 * @author acostescu
 */
public interface ISMDefaults extends IBaseSMDefaults
{

	/**
	 * Constants used for setting commands to "default".
	 * 
	 * @sample
	 * var form = solutionModel.newForm('parentForm', 'db:/example_data/parent_table', null, false, 1200, 800);
	 * form.onFindCmd = SM_DEFAULTS.COMMAND_DEFAULT; // This makes the find work like it does by default.
	 */
	public static final JSMethod COMMAND_DEFAULT = JSMethod.createDummy();


	/**
	 * Constant used for setting commands to "none".
	 * 
	 * @sample
	 * var form = solutionModel.newForm('parentForm', 'db:/example_data/parent_table', null, false, 1200, 800);
	 * form.onFindCmd = SM_DEFAULTS.COMMAND_NONE; // This disables the find on the form.
	 */
	public static final IBaseSMMethod COMMAND_NONE = null;

}
