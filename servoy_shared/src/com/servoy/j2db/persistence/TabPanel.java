/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.persistence;


import java.util.Iterator;

import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * A normal tabpanel
 *
 * @author jblok
 */
public class TabPanel extends BaseComponent implements ISupportChilds, ISupportTabSeq
{

	private static final long serialVersionUID = 1L;

	//orientations, see also SwingConstants.TOP,RIGHT,BOTTOM,LEFT
	public static final int DEFAULT_ORIENTATION = 0; // DEFAULT would conflict with inherited interface constant
	public static final int HIDE = -1;
	public static final int SPLIT_HORIZONTAL = -2;
	public static final int SPLIT_VERTICAL = -3;
	public static final int ACCORDION_PANEL = -4;

	/**
	 * Constructor I
	 */
	protected TabPanel(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.TABPANELS, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */
	/**
	 * Set the tabOrientation
	 *
	 * @param arg the tabOrientation
	 */
	public void setTabOrientation(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TABORIENTATION, arg);
	}

	/**
	 * The position of the tabs related to the tab panel. Can be one of TOP, RIGHT, BOTTOM, LEFT,
	 * HIDE, SPLIT_HORIZONTAL, SPLIT_VERTICAL, ACCORDION_PANEL. The HIDE option makes the tabs invisible, SPLIT_HORIZONTAL
	 * makes the tab panel horizontal split pane, SPLIT_VERTICAL makes the tab panel vertical split pane, ACCORDION_PANEL turns
	 * the tab panel into an accordion.
	 */
	public int getTabOrientation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TABORIENTATION).intValue();
	}

	/*
	 * _____________________________________________________________ Methods for Tab handling
	 */
	public Iterator<IPersist> getTabs()
	{
		return Utils.asSortedIterator(new TypeIterator<IPersist>(getAllObjectsAsList(), IRepository.TABS), PositionComparator.XY_PERSIST_COMPARATOR);
	}

	public Tab createNewTab(String text, String relationName, Form f) throws RepositoryException
	{
		Tab obj = (Tab)getRootObject().getChangeHandler().createNewObject(this, IRepository.TABS);
		//set all the required properties

		obj.setText(text);
		obj.setRelationName(relationName);
		obj.setContainsFormID(f.getID());

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

	/**
	 * Returns the selectedTabColor.
	 *
	 * @return java.awt.Color
	 */
	@Deprecated
	public java.awt.Color getSelectedTabColor()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SELECTEDTABCOLOR);
	}

	/**
	 * Sets the selectedTabColor.
	 *
	 * @param selectedTabColor The selectedTabColor to set
	 */
	@Deprecated
	public void setSelectedTabColor(java.awt.Color arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SELECTEDTABCOLOR, arg);
	}

	public boolean hasOneTab()
	{
		Iterator<IPersist> it = getTabs();
		if (it.hasNext()) it.next();
		if (it.hasNext()) return false;
		return true;
	}

	/**
	 * Flag that tells how to arrange the tabs if they don't fit on a single line.
	 * If this flag is set, then the tabs will stay on a single line, but there will
	 * be the possibility to scroll them to the left and to the right. If this flag
	 * is not set, then the tabs will be arranged on multiple lines.
	 */
	public boolean getScrollTabs()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SCROLLTABS).booleanValue();
	}

	public void setScrollTabs(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SCROLLTABS, arg);
	}

	@Deprecated
	public boolean getCloseOnTabs()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_CLOSEONTABS).booleanValue();
	}

	@Deprecated
	public void setCloseOnTabs(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CLOSEONTABS, arg);
	}

	@Deprecated
	public int getOnTabChangeMethodID()
	{
		return getOnChangeMethodID();
	}

	@Deprecated
	public void setOnTabChangeMethodID(int arg)
	{
		setOnChangeMethodID(arg);
	}

	/**
	 * Method to be executed when the selected tab is changed in the tab panel or divider position is changed in split pane.
	 *
	 * @templatedescription Callback method when the user changes tab in a tab panel or divider position in split pane
	 * @templatename onTabChange
	 * @templateparam Number previousIndex index of tab shown before the change
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnChangeMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID).intValue();
	}

	/**
	 * Sets the onTabChangeMethodID.
	 *
	 * @param arg The onChangeMethodID to set
	 */
	public void setOnChangeMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID, arg);
	}

	public void setTabSeq(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TABSEQ, arg);
	}

	public int getTabSeq()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TABSEQ).intValue();
	}

	/**
	 * @param arg the horizontal alignment
	 */
	public void setHorizontalAlignment(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_HORIZONTALALIGNMENT, arg);
	}

	/**
	 * The horizontal alignment of the tabpanel.
	 */
	public int getHorizontalAlignment()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_HORIZONTALALIGNMENT).intValue();
	}

	@Override
	public String toString()
	{
		String name = getName();
		if (name != null && !(name = getName().trim()).equals("")) //$NON-NLS-1$
		{
			return name;
		}
		else
		{
			return "no name/provider"; //$NON-NLS-1$
		}
	}

}
