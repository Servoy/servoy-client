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

package com.servoy.base.scripting.solutionhelper;

import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.base.solutionmodel.IBaseSMLabel;

/**
 * Gives access to non-public functionality, such as being able to access hidden content.
 * @author acostescu
 */
public interface IBaseSMFormInternal extends IBaseSMForm
{

	/**
	 * In mobile some components such as title labels are hidden from public API access. This method allows access to them.
	 * @param showInternal true if all should be revealed, false for normal public access.
	 * @return the list of components in this form, filtered or not from public API access point of view.
	 */
	IBaseSMComponent[] getComponentsInternal(boolean showInternal);

	/**
	 * In mobile title labels are hidden from public API access. This method allows access to them.
	 * @param showInternal true if all should be revealed, false for normal public access.
	 * @return the list of components in this form, filtered or not from public API access point of view.
	 */
	IBaseSMLabel[] getLabelsInternal(boolean showInternal);

}
