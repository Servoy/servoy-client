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

package com.servoy.j2db.scripting;

import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMButton;
import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMComponent;
import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMLabel;

/**
 * Mobile helper is used as a complementary tool to solution model.
 * Components/solution structure in mobile client needs special tags which are not supported by solution model API,
 * but this mobile helper can be used to apply them. For example a button can be the right header button or the left header button and so on.
 * 
 * @author acostescu
 */
public abstract class BaseSolutionHelper implements IPredefinedIconConstants
{

	public abstract void markLeftHeaderButton(IBaseSMButton button);

	public abstract void markRightHeaderButton(IBaseSMButton button);

	public abstract void markHeaderText(IBaseSMLabel label);

	public abstract void markFooterItem(IBaseSMComponent component);

	public abstract void setIconType(IBaseSMButton button, String iconType);

	public void groupComponents(IBaseSMComponent c1, IBaseSMComponent c2)
	{
		String gid = c1.getGroupID();
		if (gid == null) gid = c2.getGroupID();
		if (gid == null) gid = createNewGroupId();

		c1.setGroupID(gid);
		c2.setGroupID(gid);
	}

	protected abstract String createNewGroupId();

}