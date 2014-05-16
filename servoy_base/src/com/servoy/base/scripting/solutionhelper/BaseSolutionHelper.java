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

package com.servoy.base.scripting.solutionhelper;

import java.util.ArrayList;
import java.util.List;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.IMobileProperties.MobileProperty;
import com.servoy.base.solutionmodel.IBaseSMButton;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMField;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.base.solutionmodel.IBaseSMLabel;
import com.servoy.base.solutionmodel.mobile.IMobileSMForm;
import com.servoy.base.solutionmodel.mobile.IMobileSMHasTitle;
import com.servoy.base.solutionmodel.mobile.IMobileSMLabel;
import com.servoy.base.solutionmodel.mobile.IMobileSMRadios;
import com.servoy.base.solutionmodel.mobile.IMobileSMTitle;
import com.servoy.base.solutionmodel.mobile.IMobileSolutionModel;

/**
 * Mobile helper is used as a complementary tool to solution model.
 * Components/solution structure in mobile client needs special tags which are not supported by solution model API,
 * but this mobile helper can be used to apply them. For example a button can be the right header button or the left header button and so on.
 * 
 * @author acostescu
 */
public abstract class BaseSolutionHelper
{
	public static final String AUTO_CREATED_LIST_INSETLIST_NAME = "list"; //$NON-NLS-1$ 

	protected final IMobileSolutionModel solutionModel;

	public BaseSolutionHelper(IMobileSolutionModel solutionModel)
	{
		this.solutionModel = solutionModel;
	}

	// gets a mobile property manipulator handle from a JSXYZ (solution model type) type.

	/**
	 * Should never return null for a jsObject that is capable of having mobile properties.
	 */
	protected abstract IMobileProperties getMobileProperties(Object jsObject);

	protected abstract String getStringUUID(Object jsObject);

	public void markLeftHeaderButton(IBaseSMButton button)
	{
		IMobileProperties mpc = getMobileProperties(button);
		mpc.setPropertyValue(IMobileProperties.HEADER_LEFT_BUTTON, Boolean.TRUE);
		mpc.setPropertyValue(IMobileProperties.HEADER_ITEM, Boolean.TRUE);
	}

	public void markRightHeaderButton(IBaseSMButton button)
	{
		IMobileProperties mpc = getMobileProperties(button);
		mpc.setPropertyValue(IMobileProperties.HEADER_RIGHT_BUTTON, Boolean.TRUE);
		mpc.setPropertyValue(IMobileProperties.HEADER_ITEM, Boolean.TRUE);
	}

	public void markHeaderText(IBaseSMLabel label)
	{
		IMobileProperties mpc = getMobileProperties(label);
		mpc.setPropertyValue(IMobileProperties.HEADER_TEXT, Boolean.TRUE);
		mpc.setPropertyValue(IMobileProperties.HEADER_ITEM, Boolean.TRUE);
	}

	public void markFooterItem(IBaseSMComponent component)
	{
		getMobileProperties(component).setPropertyValue(IMobileProperties.FOOTER_ITEM, Boolean.TRUE);
	}

	public void setIconType(IBaseSMButton button, String iconType)
	{
		getMobileProperties(button).setPropertyValue(IMobileProperties.DATA_ICON, iconType);
	}

	public String getIconType(IBaseSMButton button)
	{
		return getMobileProperties(button).getPropertyValue(IMobileProperties.DATA_ICON);
	}

	public void setHeaderSize(IMobileSMLabel label, int headerSize)
	{
		label.setLabelSize(headerSize);
	}

	public int getHeaderSize(IMobileSMLabel label)
	{
		return label.getLabelSize();
	}

	public void setRadioFieldHorizontal(IBaseSMField radioField, boolean horizontal)
	{
		if (radioField instanceof IMobileSMRadios)
		{
			((IMobileSMRadios)radioField).setHorizontal(horizontal);
		}
	}

	public boolean isRadioFieldHorizontal(IBaseSMField radioField)
	{
		return radioField instanceof IMobileSMRadios && ((IMobileSMRadios)radioField).getHorizontal();
	}

	private IMobileSMTitle getTitleForComponent(IBaseSMComponent c)
	{
		if (c instanceof IMobileSMHasTitle)
		{
			return ((IMobileSMHasTitle)c).getTitle();
		}
		return null;
	}

	public void setTitleDisplaysTags(IBaseSMComponent c, boolean displaysTags)
	{
		IMobileSMTitle title = getTitleForComponent(c);
		if (title != null) title.setDisplaysTags(displaysTags);
	}

	public boolean getTitleDisplaysTags(IBaseSMComponent c)
	{
		IMobileSMTitle title = getTitleForComponent(c);
		return title != null && title.getDisplaysTags();
	}

	public void setTitleDataProvider(IBaseSMComponent c, String dataProvider)
	{
		IMobileSMTitle title = getTitleForComponent(c);
		if (title != null) title.setDataProviderID(dataProvider);
	}

	public String getTitleDataProvider(IBaseSMComponent c)
	{
		IMobileSMTitle title = getTitleForComponent(c);
		return title == null ? null : title.getDataProviderID();
	}

	public void setTitleText(IBaseSMComponent c, String titleText)
	{
		IMobileSMTitle title = getTitleForComponent(c);
		if (title != null) title.setText(titleText);
	}

	public String getTitleText(IBaseSMComponent c)
	{
		IMobileSMTitle title = getTitleForComponent(c);
		return title == null ? null : title.getText();
	}

	public void setTitleVisible(IBaseSMLabel l, boolean titleVisible)
	{
		IMobileSMTitle title = getTitleForComponent(l);
		if (title != null) title.setVisible(titleVisible);
	}

	public boolean isTitleVisible(IBaseSMLabel l)
	{
		IMobileSMTitle title = getTitleForComponent(l);
		return title != null && title.getVisible();
	}

	@Deprecated
	public void groupComponents(IBaseSMComponent c1, IBaseSMComponent c2)
	{
		String gid = c1.getGroupID();
		if (gid == null) gid = c2.getGroupID();
		if (gid == null) gid = createNewGroupId();

		c1.setGroupID(gid);
		c2.setGroupID(gid);

		// mark the label as title
		if (c1 instanceof IBaseSMLabel) getMobileProperties(c1).setPropertyValue(IMobileProperties.COMPONENT_TITLE, Boolean.TRUE);
		else if (c2 instanceof IBaseSMLabel) getMobileProperties(c2).setPropertyValue(IMobileProperties.COMPONENT_TITLE, Boolean.TRUE);
	}

	protected abstract String createNewGroupId();

	public IBaseSHInsetList createInsetList(IMobileSMForm form, int yLocation, String relationName, String headerText, String textDataProviderID)
	{
		return form.newInsetList(yLocation, relationName, headerText, textDataProviderID);
	}

	public IBaseSHList createListForm(String formName, String dataSource, String textDataProviderID)
	{
		return solutionModel.newListForm(formName, dataSource, textDataProviderID);
	}

	public IBaseSHList getListForm(String formName)
	{
		return solutionModel.getListForm(formName);
	}

	public IBaseSHInsetList getInsetList(IMobileSMForm form, String name)
	{
		if (form == null) return null;
		return form.getInsetList(name);
	}

	private IBaseSMButton getHeaderButton(IBaseSMForm form, boolean left)
	{
		IBaseSMComponent[] components = getAllComponents(form, left ? IMobileProperties.HEADER_LEFT_BUTTON : IMobileProperties.HEADER_RIGHT_BUTTON);
		if (components != null && components.length > 0 && components[0] instanceof IBaseSMButton)
		{
			return (IBaseSMButton)components[0];
		}
		return null;
	}

	public IBaseSMButton getLeftHeaderButton(IBaseSMForm form)
	{
		return getHeaderButton(form, true);
	}

	public IBaseSMButton getRightHeaderButton(IBaseSMForm form)
	{
		return getHeaderButton(form, false);
	}

	public IBaseSMLabel getHeaderLabel(IBaseSMForm form)
	{
		IBaseSMComponent[] components = getAllComponents(form, IMobileProperties.HEADER_TEXT);
		if (components != null && components.length > 0 && components[0] instanceof IBaseSMLabel)
		{
			return (IBaseSMLabel)components[0];
		}
		return null;
	}

	public IBaseSMComponent[] getAllFooterComponents(IBaseSMForm form)
	{
		return getAllComponents(form, IMobileProperties.FOOTER_ITEM);
	}

	private IBaseSMComponent[] getAllComponents(IBaseSMForm form, MobileProperty<Boolean> property)
	{
		List<IBaseSMComponent> components = new ArrayList<IBaseSMComponent>();
		if (form != null)
		{
			IBaseSMComponent[] formComponents = form.getComponents();
			if (components != null)
			{
				for (IBaseSMComponent component : formComponents)
				{
					if (Boolean.TRUE.equals(getMobileProperties(component).getPropertyValue(property)))
					{
						components.add(component);
					}
				}
			}
		}
		return components.toArray(new IBaseSMComponent[0]);
	}

	public IBaseSHInsetList[] getAllInsetLists(IMobileSMForm form)
	{
		return form.getInsetLists();
	}

	public IBaseSHList[] getAllListForms()
	{
		return solutionModel.getListForms();
	}

	public boolean removeInsetList(IMobileSMForm form, String name)
	{
		return form != null && form.removeInsetList(name);
	}

	public void setComponentOrder(IBaseSMComponent[] components)
	{
		int currentHeight = 1;
		if (components != null && components.length > 0)
		{
			boolean footerItems = false;
			IMobileProperties mpc = getMobileProperties(components[0]);
			if (Boolean.TRUE.equals(mpc.getPropertyValue(IMobileProperties.FOOTER_ITEM)))
			{
				footerItems = true;
			}
			for (IBaseSMComponent comp : components)
			{
				if (comp != null)
				{
					if (footerItems)
					{
						comp.setX(currentHeight);
					}
					else
					{
						comp.setY(currentHeight);
					}
					currentHeight += footerItems ? comp.getWidth() : comp.getHeight();
				}
			}
		}
	}
}