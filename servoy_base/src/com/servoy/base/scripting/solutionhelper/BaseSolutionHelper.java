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
import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.base.solutionmodel.IBaseSMButton;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMField;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.base.solutionmodel.IBaseSMLabel;
import com.servoy.base.solutionmodel.IBaseSMPortal;
import com.servoy.base.solutionmodel.IBaseSolutionModel;

/**
 * Mobile helper is used as a complementary tool to solution model.
 * Components/solution structure in mobile client needs special tags which are not supported by solution model API,
 * but this mobile helper can be used to apply them. For example a button can be the right header button or the left header button and so on.
 * 
 * @author acostescu
 */
public abstract class BaseSolutionHelper implements IPredefinedIconConstants
{
	private static final String AUTO_CREATED_LIST_INSETLIST_NAME = "list"; //$NON-NLS-1$ 

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

	protected abstract String getStringUUID(Object jsObject);

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

	public String getIconType(IBaseSMButton button)
	{
		IMobileProperties mpc = getMobileProperties(button);
		return mpc.getPropertyValue(IMobileProperties.DATA_ICON);
	}

	public void setHeaderSize(IBaseSMLabel label, int headerSize)
	{
		// only set valid values
		if (headerSize > 0 && headerSize < 7)
		{
			IMobileProperties mpc = getMobileProperties(label);
			mpc.setPropertyValue(IMobileProperties.HEADER_SIZE, Integer.valueOf(headerSize));
		}
	}

	public int getHeaderSize(IBaseSMLabel label)
	{
		IMobileProperties mpc = getMobileProperties(label);
		Number headerSize = mpc.getPropertyValue(IMobileProperties.HEADER_SIZE);
		return headerSize != null ? headerSize.intValue() : 4;
	}

	public void setRadioFieldHorizontal(IBaseSMField radioField, boolean horizontal)
	{
		if (radioField.getDisplayType() != IBaseSMField.RADIOS) return;

		int radioStyle = (horizontal ? IMobileProperties.RADIO_STYLE_HORIZONTAL : IMobileProperties.RADIO_STYLE_VERTICAL);
		IMobileProperties mpc = getMobileProperties(radioField);
		mpc.setPropertyValue(IMobileProperties.RADIO_STYLE, Integer.valueOf(radioStyle));
	}

	public boolean isRadioFieldHorizontal(IBaseSMField radioField)
	{
		IMobileProperties mpc = getMobileProperties(radioField);
		return mpc.getPropertyValue(IMobileProperties.RADIO_STYLE).intValue() == IMobileProperties.RADIO_STYLE_HORIZONTAL;
	}

	protected IBaseSMLabel getTitleForComponent(IBaseSMComponent c, boolean createIfMissing)
	{
		// fail with exception for setters that are called with unsupported component types (only fields and labels are currently supported)
		if (createIfMissing && !supportsTitleLabel(c))
		{
			throw new RuntimeException("Title labels are only supported for field and label components."); //$NON-NLS-1$
		}

		IBaseSMLabel titleLabel = null;
		IBaseSMFormInternal parentForm = getParentForm(c);
		if (parentForm != null)
		{
			String cGroup = c.getGroupID();
			if (cGroup != null)
			{
				IBaseSMComponent[] labels = parentForm.getComponentsInternal(true, Integer.valueOf(IRepositoryConstants.GRAPHICALCOMPONENTS));
				for (IBaseSMComponent label : labels)
				{
					if (label instanceof IBaseSMLabel)
					{
						IBaseSMLabel l = (IBaseSMLabel)label;
						if (cGroup.equals(l.getGroupID()))
						{
							// I guess the following if might as well not be; the location thing is for legacy solutions (before the COMPONENT_TITLE existed)
							if (Boolean.TRUE.equals(getMobileProperties(l).getPropertyValue(IMobileProperties.COMPONENT_TITLE)) || l.getY() < c.getY() ||
								(l.getY() == c.getY() && l.getX() < c.getX()))
							{
								titleLabel = l;
								break;
							}
						}
					}
				}
			}

			if (titleLabel == null && createIfMissing)
			{
				if (cGroup == null)
				{
					cGroup = createNewGroupId();
					c.setGroupID(cGroup);
				}
				titleLabel = parentForm.newLabel(null, 0, 0, 0, 0);
				titleLabel.setGroupID(cGroup);
				getMobileProperties(titleLabel).setPropertyValue(IMobileProperties.COMPONENT_TITLE, Boolean.TRUE);
			}
		}
		return titleLabel;
	}

	protected abstract boolean supportsTitleLabel(IBaseSMComponent c);

	protected abstract IBaseSMFormInternal getParentForm(IBaseSMComponent c);

	public void setTitleDisplaysTags(IBaseSMComponent c, boolean displaysTags)
	{
		IBaseSMLabel l = getTitleForComponent(c, true);
		if (l != null) l.setDisplaysTags(displaysTags);
	}

	public boolean getTitleDisplaysTags(IBaseSMComponent c)
	{
		IBaseSMLabel l = getTitleForComponent(c, false);
		return l != null ? l.getDisplaysTags() : false;
	}

	public void setTitleDataProvider(IBaseSMComponent c, String dataProvider)
	{
		IBaseSMLabel l = getTitleForComponent(c, true);
		if (l != null) l.setDataProviderID(dataProvider);
	}

	public String getTitleDataProvider(IBaseSMComponent c)
	{
		IBaseSMLabel l = getTitleForComponent(c, false);
		return l != null ? l.getDataProviderID() : null;
	}

	public void setTitleText(IBaseSMComponent c, String titleText)
	{
		IBaseSMLabel l = getTitleForComponent(c, true);
		if (l != null) l.setText(titleText);
	}

	public String getTitleText(IBaseSMComponent c)
	{
		IBaseSMLabel l = getTitleForComponent(c, false);
		return l != null ? l.getText() : null;
	}

	public void setTitleVisible(IBaseSMLabel l, boolean titleVisible)
	{
		IBaseSMLabel lbl = getTitleForComponent(l, true);
		if (lbl != null) lbl.setVisible(titleVisible);
	}

	public boolean isTitleVisible(IBaseSMLabel l)
	{
		IBaseSMLabel title = getTitleForComponent(l, false);
		return title != null ? title.getVisible() : false;
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

	public IBaseSHInsetList createInsetList(IBaseSMForm form, int yLocation, String relationName, String headerText, String textDataProviderID)
	{
		String autoGeneratedInsetListName = AUTO_CREATED_LIST_INSETLIST_NAME;
		int i = 1;
		while (form.getComponent(autoGeneratedInsetListName) != null)
		{
			autoGeneratedInsetListName = AUTO_CREATED_LIST_INSETLIST_NAME + '_' + (i++);
		}

		// create portal
		IBaseSMPortal portal = form.newPortal(autoGeneratedInsetListName, relationName, 0, yLocation, 0, 0);
		IMobileProperties mp = getMobileProperties(portal);
		mp.setPropertyValue(IMobileProperties.LIST_COMPONENT, Boolean.TRUE);

		// create list abstraction
		IBaseSHInsetList listComponent = instantiateInsetList(portal, this);

		// create other persists for remaining contents of list
		if (headerText != null) listComponent.setHeaderText(headerText);
		if (textDataProviderID != null) listComponent.setTextDataProviderID(textDataProviderID);

		return listComponent;
	}

	protected abstract IBaseSHInsetList instantiateInsetList(IBaseSMPortal portal, BaseSolutionHelper baseSolutionHelper);

	public IBaseSHList createListForm(String formName, String dataSource, String textDataProviderID)
	{
		if (solutionModel.getForm(formName) != null) return null; // a form with that name already exists

		// create form
		IBaseSMForm listForm = solutionModel.newForm(formName, dataSource, null, false, 100, 380);
		listForm.setView(IBaseSMForm.LOCKED_TABLE_VIEW);

		// create list abstraction
		IBaseSHList listComponent = instantiateList(listForm, this);

		// create other persists for remaining contents of list
		if (textDataProviderID != null) listComponent.setTextDataProviderID(textDataProviderID);

		return listComponent;
	}

	public IBaseSHList getListForm(String formName)
	{
		IBaseSHList listForm = null;
		IBaseSMForm f = solutionModel.getForm(formName);
		if (f != null && f.getView() == IBaseSMForm.LOCKED_TABLE_VIEW)
		{
			listForm = instantiateList(f, this);
		}
		return listForm;
	}

	public IBaseSHInsetList getInsetList(IBaseSMForm form, String name)
	{
		if (form == null || name == null) return null;

		IBaseSMPortal portal = form.getPortal(name);
		if (portal != null)
		{
			IMobileProperties mp = getMobileProperties(portal);
			if (Boolean.TRUE.equals(mp.getPropertyValue(IMobileProperties.LIST_COMPONENT)))
			{
				return instantiateInsetList(portal, this);
			}
		}
		return null;
	}

	protected abstract IBaseSHList instantiateList(IBaseSMForm listForm, BaseSolutionHelper baseSolutionHelper);

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
					IMobileProperties mpc = getMobileProperties(component);
					if (Boolean.TRUE.equals(mpc.getPropertyValue(property)))
					{
						components.add(component);
					}
				}
			}
		}
		return components.toArray(new IBaseSMComponent[0]);
	}

	public IBaseSHInsetList[] getAllInsetLists(IBaseSMForm form)
	{
		List<IBaseSHInsetList> insetListList = new ArrayList<IBaseSHInsetList>();
		if (form != null)
		{
			IBaseSMPortal[] portals = form.getPortals();
			if (portals != null)
			{
				for (IBaseSMPortal portal : portals)
				{
					IMobileProperties mp = getMobileProperties(portal);
					if (Boolean.TRUE.equals(mp.getPropertyValue(IMobileProperties.LIST_COMPONENT)))
					{
						insetListList.add(instantiateInsetList(portal, this));
					}
				}
			}
		}
		return insetListList.toArray(new IBaseSHInsetList[0]);
	}

	public IBaseSHList[] getAllListForms()
	{
		List<IBaseSHList> listFormsList = new ArrayList<IBaseSHList>();
		IBaseSMForm[] forms = solutionModel.getForms();
		if (forms != null)
		{
			for (IBaseSMForm form : forms)
			{
				if (form.getView() == IBaseSMForm.LOCKED_TABLE_VIEW)
				{
					listFormsList.add(instantiateList(form, this));
				}
			}
		}
		return listFormsList.toArray(new IBaseSHList[0]);
	}

	public boolean removeInsetList(IBaseSMForm form, String name)
	{
		if (form != null)
		{
			return form.removePortal(name);
		}
		return false;
	}
}