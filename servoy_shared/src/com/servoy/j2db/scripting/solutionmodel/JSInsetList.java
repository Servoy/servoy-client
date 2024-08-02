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

package com.servoy.j2db.scripting.solutionmodel;

import com.servoy.base.scripting.solutionhelper.BaseSHInsetList;
import com.servoy.base.scripting.solutionhelper.IBaseSMFormInternal;
import com.servoy.base.solutionmodel.IBaseSMPortal;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IJavaScriptType;

/**
 * This class is the representation of a mobile list component/form.
 *
 * @author acostescu
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@Deprecated
public class JSInsetList extends BaseSHInsetList implements IJavaScriptType, ISHInsetList
{
	public JSInsetList(IBaseSMPortal portal, IBaseSMFormInternal contextForm)
	{
		super(portal, contextForm);
	}
}