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

	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private String text = null;
	private String toolTipText = null;
	private boolean editable = true;//remark not default
	private String format = null;

	private int onFocusGainedMethodID;
	private int onFocusLostMethodID;
	private int onDataChangeMethodID;
	private int onActionMethodID;
	private int onRightClickMethodID;
	private int onRenderMethodID;

	private int displayType;
	private int scrollbars;
	private boolean selectOnEnter;
	private int tabSeq = ISupportTabSeq.DEFAULT;
	private int horizontalAlignment = -1;
	private int verticalAlignment = -1;//not implemented not possible ,also hiddden in properties
	private int valuelist_id;
	private String dataProviderID = null;
	private Insets margin = null;
	private boolean displaysTags;

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
		checkForChange(toolTipText, arg);
		toolTipText = arg;
	}

	/**
	 * @sameas com.servoy.j2db.persistence.GraphicalComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		return toolTipText;
	}

	/**
	 * Set the editable
	 * 
	 * @param arg the editable
	 */
	public void setEditable(boolean arg)
	{
		checkForChange(editable, arg);
		editable = arg;
	}

	/**
	 * Flag that tells if the content of the field can be edited or not. 
	 * The default value of this flag is "true", that is the content can be edited.
	 */
	public boolean getEditable()
	{
		return editable;
	}

	/**
	 * Set the formatter
	 * 
	 * @param arg the formatter
	 */
	public void setFormat(String arg)
	{
		if (arg != null) arg = arg.trim();
		if (arg != null && arg.length() == 0) arg = null;
		checkForChange(format, arg);
		format = arg;
	}

	/**
	 * The format that should be applied when displaying the data in the component.
	 * Some examples are "#%", "dd-MM-yyyy", "MM-dd-yyyy", etc.
	 */
	public String getFormat()
	{
		return format;
	}

	/**
	 * Set the onChangeMethodID
	 * 
	 * @param arg the onChangeMethodID
	 */
	public void setOnDataChangeMethodID(int arg)
	{
		checkForChange(onDataChangeMethodID, arg);
		onDataChangeMethodID = arg;
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
		return onDataChangeMethodID;
	}


	public void setOnRightClickMethodID(int arg)
	{
		checkForChange(onRightClickMethodID, arg);
		onRightClickMethodID = arg;
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
		return onRightClickMethodID;
	}

	public void setOnRenderMethodID(int arg)
	{
		checkForChange(onRenderMethodID, arg);
		onRenderMethodID = arg;
	}

	/**
	 * The method that is executed when the component is rendered.
	 */
	public int getOnRenderMethodID()
	{
		return onRenderMethodID;
	}

	/**
	 * Set the enterMethodID
	 * 
	 * @param arg the enterMethodID
	 */
	public void setOnFocusGainedMethodID(int arg)
	{
		checkForChange(onFocusGainedMethodID, arg);
		onFocusGainedMethodID = arg;
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
		return onFocusGainedMethodID;
	}

	public void setOnFocusLostMethodID(int arg)
	{
		checkForChange(onFocusLostMethodID, arg);
		onFocusLostMethodID = arg;
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
		return onFocusLostMethodID;
	}

	/**
	 * Set the scrollable
	 * 
	 * @param arg the scrollable
	 */
	public void setScrollbars(int arg)
	{
		checkForChange(scrollbars, arg);
		scrollbars = arg;
	}

	public int getScrollbars()
	{
		return scrollbars;
	}

	/**
	 * Set the selectOnEnter
	 * 
	 * @param arg the selectOnEnter
	 */
	public void setSelectOnEnter(boolean arg)
	{
		checkForChange(selectOnEnter, arg);
		selectOnEnter = arg;
	}

	/**
	 * Flag that tells if the content of the field should be automatically selected
	 * when the field receives focus. The default value of this field is "false".
	 */
	public boolean getSelectOnEnter()
	{
		return selectOnEnter;
	}

	/**
	 * Set the tabSeq
	 * 
	 * @param arg the tabSeq
	 */
	public void setTabSeq(int arg)
	{
		if (arg < 1 && arg != ISupportTabSeq.DEFAULT && arg != ISupportTabSeq.SKIP) return;//irrelevant value from editor
		checkForChange(tabSeq, arg);
		tabSeq = arg;
	}

	public int getTabSeq()
	{
		return tabSeq;
	}

	/**
	 * Set the horizontalAlignment
	 * 
	 * @param arg the horizontalAlignment
	 */
	public void setHorizontalAlignment(int arg)
	{
		checkForChange(horizontalAlignment, arg);
		horizontalAlignment = arg;
	}

	/**
	 * @sameas com.servoy.j2db.persistence.GraphicalComponent#getHorizontalAlignment()
	 */
	public int getHorizontalAlignment()
	{
		return horizontalAlignment;
	}

	/**
	 * Set the verticalAlignment
	 * 
	 * @param arg the verticalAlignment
	 */
	public void setVerticalAlignment(int arg)
	{
		checkForChange(verticalAlignment, arg);
		verticalAlignment = arg;
	}

	/**
	 * Get the verticalAlignment
	 * 
	 * @return the verticalAlignment
	 */
	public int getVerticalAlignment()
	{
		return verticalAlignment;
	}

	/**
	 * Set the valuelist_id
	 * 
	 * @param arg the valuelist_id
	 */
	public void setValuelistID(int arg)
	{
		checkForChange(valuelist_id, arg);
		valuelist_id = arg;
	}

	public int getValuelistID()
	{
		return valuelist_id;
	}

	/**
	 * Set the dataProviderID
	 * 
	 * @param arg the dataProviderID
	 */
	public void setDataProviderID(String arg)
	{
		checkForChange(dataProviderID, arg);
		dataProviderID = arg;
	}

	public String getDataProviderID()
	{
		return dataProviderID;
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
		checkForChange(onActionMethodID, arg);
		onActionMethodID = arg;
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
		return onActionMethodID;
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
		return displayType;
	}

	public void setDisplayType(int arg)
	{
		checkForChange(displayType, arg);
		displayType = arg;
	}

	public Insets getMargin()
	{
		if (margin != null)
		{
			return new Insets(margin.top, margin.left, margin.bottom, margin.right);
		}
		return margin;
	}

	/**
	 * Sets the margin.
	 * 
	 * @param arg The margin to set
	 */
	public void setMargin(Insets arg)
	{
		checkForChange(margin, arg);
		margin = arg;
	}

	/*
	 * useRTF is not longer used (it is hidden in the app) do not remove methods since it is still defined in repository
	 */
	public boolean getUseRTF()
	{
		return false;
	}

	public void setUseRTF(boolean arg)
	{
	}

	/**
	 * @sameas com.servoy.j2db.persistence.GraphicalComponent#getDisplaysTags()
	 */
	public boolean getDisplaysTags()
	{
		return displaysTags;
	}

	/**
	 * Sets the displaysTags.
	 * 
	 * @param arg The displaysTags to set
	 */
	public void setDisplaysTags(boolean arg)
	{
		checkForChange(displaysTags, arg);
		displaysTags = arg;
	}

	/**
	 * The text that is displayed in the column header associated with the component when the form
	 * is in table view.
	 */
	public String getText()
	{
		return text;
	}

	/**
	 * Sets the text.
	 * 
	 * @param arg The text to set
	 */
	public void setText(String arg)
	{
		checkForChange(text, arg);
		text = arg;
	}
}
