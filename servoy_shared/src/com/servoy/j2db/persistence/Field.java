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


import java.awt.Insets;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.UUID;

/**
 * A normal field (data display)
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class Field extends BaseComponent implements ISupportTextSetup, ISupportText, ISupportDataProviderID, ISupportScrollbars, ISupportTabSeq
{
	/*
	 * displayTypes
	 */
	public static final int TEXT_FIELD = 0;
	public static final int TEXT_AREA = 1;
	public static final int COMBOBOX = 2;
	public static final int RADIOS = 3;
	public static final int CHECKS = 4;
	public static final int CALENDAR = 5;
	public static final int PASSWORD = 6;
	public static final int RTF_AREA = 7;
	public static final int HTML_AREA = 8;
	public static final int IMAGE_MEDIA = 9;
	public static final int TYPE_AHEAD = 10;

//	list (to have a jlist alike)
//	color_picker
//	html_browser
//	tree_view (if placed in table_view it becomes treetable)
//	jsplitpane servoy imp where you can really drop left/right forms on.

	/**
	 * Constructor I
	 */
	Field(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.FIELDS, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */


	/**
	 * Set the toolTipText
	 * 
	 * @param arg the toolTipText
	 */
	public void setToolTipText(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TOOLTIPTEXT, arg);
	}

	/**
	 * @sameas com.servoy.j2db.persistence.GraphicalComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TOOLTIPTEXT);
	}

	/**
	 * Set the editable
	 * 
	 * @param arg the editable
	 */
	public void setEditable(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_EDITABLE, arg);
	}

	/**
	 * Flag that tells if the content of the field can be edited or not. 
	 * The default value of this flag is "true", that is the content can be edited.
	 */
	public boolean getEditable()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_EDITABLE).booleanValue();
	}

	/**
	 * Set the formatter
	 * 
	 * @param format the formatter
	 */
	public void setFormat(String arg)
	{
		String format = arg;
		if (format != null) format = format.trim();
		if (format != null && format.length() == 0) format = null;

		setTypedProperty(StaticContentSpecLoader.PROPERTY_FORMAT, format);
	}

	/**
	 * The format that should be applied when displaying the data in the component.
	 * Some examples are "#%", "dd-MM-yyyy", "MM-dd-yyyy", etc.
	 */
	public String getFormat()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FORMAT);
	}

	/**
	 * Set the onChangeMethodID
	 * 
	 * @param arg the onChangeMethodID
	 */
	public void setOnDataChangeMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID, arg);
	}

	/**
	 * Method that is executed when the data in the component is successfully changed.
	 * 
	 * @templatedescription Handle changed data
	 * @templatename onDataChange
	 * @templatetype Boolean
	 * @templateparam Object oldValue old value
	 * @templateparam Object newValue new value
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	public int getOnDataChangeMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID).intValue();
	}

	public void setOnRightClickMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID, arg);
	}

	/**
	 * @sameas com.servoy.j2db.persistence.GraphicalComponent#getOnRightClickMethodID()
	 * 
	 * @templatedescription Perform the element right-click action
	 * @templatename onRightClick
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnRightClickMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID).intValue();
	}

	public void setOnRenderMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID, arg);
	}

	/**
	 * The method that is executed when the component is rendered.
	 */
	public int getOnRenderMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID).intValue();
	}

	/**
	 * Set the enterMethodID
	 * 
	 * @param arg the enterMethodID
	 */
	public void setOnFocusGainedMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID, arg);
	}

	/**
	 * The method that is executed when the component gains focus.
	 * NOTE: Do not call methods that will influence the focus itself.
	 *
	 * @templatedescription Handle focus element gaining focus
	 * @templatename onFocusGained
	 * @templatetype Boolean
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	public int getOnFocusGainedMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID).intValue();
	}

	public void setOnFocusLostMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID, arg);
	}

	/**
	 * The method that is executed when the component looses focus.
	 * 
	 * @templatedescription Handle focus element loosing focus
	 * @templatename onFocusLost
	 * @templatetype Boolean
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	public int getOnFocusLostMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID).intValue();
	}

	/**
	 * Set the scrollable
	 * 
	 * @param arg the scrollable
	 */
	public void setScrollbars(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SCROLLBARS, arg);
	}

	public int getScrollbars()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SCROLLBARS).intValue();
	}

	/**
	 * Set the selectOnEnter
	 * 
	 * @param arg the selectOnEnter
	 */
	public void setSelectOnEnter(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SELECTONENTER, arg);
	}

	/**
	 * Flag that tells if the content of the field should be automatically selected
	 * when the field receives focus. The default value of this field is "false".
	 */
	public boolean getSelectOnEnter()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SELECTONENTER).booleanValue();
	}

	/**
	 * Set the tabSeq
	 * 
	 * @param arg the tabSeq
	 */
	public void setTabSeq(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TABSEQ, arg);
	}

	public int getTabSeq()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TABSEQ).intValue();
	}

	/**
	 * Set the horizontalAlignment
	 * 
	 * @param arg the horizontalAlignment
	 */
	public void setHorizontalAlignment(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_HORIZONTALALIGNMENT, arg);
	}

	/**
	 * @sameas com.servoy.j2db.persistence.GraphicalComponent#getHorizontalAlignment()
	 */
	public int getHorizontalAlignment()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_HORIZONTALALIGNMENT).intValue();
	}

	/**
	 * Set the verticalAlignment
	 * 
	 * @param arg the verticalAlignment
	 */
	public void setVerticalAlignment(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_VERTICALALIGNMENT, arg);
	}

	/**
	 * Get the verticalAlignment
	 * 
	 * @return the verticalAlignment
	 */
	public int getVerticalAlignment()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_VERTICALALIGNMENT).intValue();
	}

	/**
	 * Set the valuelist_id
	 * 
	 * @param arg the valuelist_id
	 */
	public void setValuelistID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_VALUELISTID, arg);
	}

	public int getValuelistID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_VALUELISTID).intValue();
	}

	/**
	 * Set the dataProviderID
	 * 
	 * @param arg the dataProviderID
	 */
	public void setDataProviderID(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID, arg);
	}

	public String getDataProviderID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID);
	}

	/**
	 * Set the relationID
	 * 
	 * @param arg the relationID public void setRelationID(int arg) { relationID = arg; } public int getRelationID() { return relationID; }
	 */

	/**
	 * Set the actionMethodID
	 * 
	 * @param arg the actionMethodID
	 */
	public void setOnActionMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID, arg);
	}

	/**
	 * @sameas com.servoy.j2db.persistence.GraphicalComponent#getOnActionMethodID()
	 * 
	 * @templatedescription Perform the element default action
	 * @templatename onAction
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnActionMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID).intValue();
	}

	@Override
	public String toString()
	{
		String name = getName();
		if (name != null && !(name = getName().trim()).equals("")) //$NON-NLS-1$
		{
			if (getDataProviderID() != null)
			{
				return getName() + " [" + getDataProviderID() + "]"; //$NON-NLS-1$//$NON-NLS-2$
			}
			return getName();
		}
		else
		{
			return ((getDataProviderID() != null) ? getDataProviderID() : "no name/provider"); //$NON-NLS-1$
		}
	}

	public String getPropertyName()
	{
		return "dataProviderID"; //$NON-NLS-1$
	}

	/**
	 * The type of display used by the field. Can be one of CALENDAR, CHECKS,
	 * COMBOBOX, HTML_AREA, IMAGE_MEDIA, PASSWORD, RADIOS, RTF_AREA, TEXT_AREA,
	 * TEXT_FIELD or TYPE_AHEAD.
	 */
	public int getDisplayType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DISPLAYTYPE).intValue();
	}

	public void setDisplayType(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DISPLAYTYPE, arg);
	}

	public Insets getMargin()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_MARGIN);
	}

	/**
	 * Sets the margin.
	 * 
	 * @param arg The margin to set
	 */
	public void setMargin(Insets arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_MARGIN, arg);
	}

	/*
	 * useRTF is not longer used (it is hidden in the app) do not remove methods since it is still defined in repository
	 */
	public boolean getUseRTF()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_USERTF).booleanValue();
	}

	public void setUseRTF(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_USERTF, arg);
	}

	/**
	 * @sameas com.servoy.j2db.persistence.GraphicalComponent#getDisplaysTags()
	 */
	public boolean getDisplaysTags()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DISPLAYSTAGS).booleanValue();
	}

	/**
	 * Sets the displaysTags.
	 * 
	 * @param arg The displaysTags to set
	 */
	public void setDisplaysTags(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DISPLAYSTAGS, arg);
	}

	/**
	 * The text that is displayed in the column header associated with the component when the form
	 * is in table view.
	 */
	public String getText()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TEXT);
	}

	/**
	 * Sets the text.
	 * 
	 * @param arg The text to set
	 */
	public void setText(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TEXT, arg);
	}
}
