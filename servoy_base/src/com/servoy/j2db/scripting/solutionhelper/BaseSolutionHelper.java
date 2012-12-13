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

package com.servoy.j2db.scripting.solutionhelper;

import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMButton;
import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMComponent;
import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMLabel;
import com.servoy.j2db.scripting.api.solutionmodel.IBaseSolutionModel;

/**
 * Mobile helper is used as a complementary tool to solution model.
 * Components/solution structure in mobile client needs special tags which are not supported by solution model API,
 * but this mobile helper can be used to apply them. For example a button can be the right header button or the left header button and so on.
 * 
 * @author acostescu
 */
public abstract class BaseSolutionHelper implements IPredefinedIconConstants
{

	protected final IBaseSolutionModel solutionModel;

	public BaseSolutionHelper(IBaseSolutionModel solutionModel)
	{
		this.solutionModel = solutionModel;
	}

	// gets a mobile property manipulator handle from a JSXYZ (solution model type) type.
	// TODO ac in order to avoid casts we could introduce a new interface for getting mobile properties + a new interface for each IBaseSM... type that also extends that interface
	/**
	 * Should never return null for a jsObject that is capable of having mobile properties.
	 */
	protected abstract IMobileProperties getMobileProperties(Object jsObject);

	public void markLeftHeaderButton(IBaseSMButton button)
	{
		IMobileProperties mpc = getMobileProperties(button);
		mpc.setPropertyValue(IMobileProperties.HEADER_LEFT_BUTTON, Boolean.TRUE);
	}

	public void markRightHeaderButton(IBaseSMButton button)
	{
		IMobileProperties mpc = getMobileProperties(button);
		mpc.setPropertyValue(IMobileProperties.HEADER_RIGHT_BUTTON, Boolean.TRUE);
	}

	public void markHeaderText(IBaseSMLabel label)
	{
		IMobileProperties mpc = getMobileProperties(label);
		mpc.setPropertyValue(IMobileProperties.HEADER_TEXT, Boolean.TRUE);
	}

	public void markFooterItem(IBaseSMComponent component)
	{
		IMobileProperties mpc = getMobileProperties(component);
		mpc.setPropertyValue(IMobileProperties.FOOTER_ITEM, Boolean.TRUE);
	}

	public void setIconType(IBaseSMButton button, String iconType)
	{
		IMobileProperties mpc = getMobileProperties(button);
		mpc.setPropertyValue(IMobileProperties.DATA_ICON, iconType);
	}

	public void groupComponents(IBaseSMComponent c1, IBaseSMComponent c2)
	{
		String gid = c1.getGroupID();
		if (gid == null) gid = c2.getGroupID();
		if (gid == null) gid = createNewGroupId();

		c1.setGroupID(gid);
		c2.setGroupID(gid);
	}

	protected abstract String createNewGroupId();

	public IBaseSHInsetList createInsetList(String dataSource, String relationName, String headerText, String textDataProviderID)
	{
		// TODO ac
		return null;
	}

}