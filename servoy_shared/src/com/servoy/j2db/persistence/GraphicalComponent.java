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


import java.awt.Dimension;
import java.awt.Insets;

import com.servoy.j2db.annotations.ServoyDocumented;
import com.servoy.j2db.util.UUID;

/**
 * This component can be a label (with image),image or button (with image/or text)
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Label, Button, Image")
public class GraphicalComponent extends BaseComponent implements ISupportTextEditing, ISupportTextSetup, ISupportDataProviderID, ISupportTabSeq, ISupportMedia
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default value for the media options. Image will be reduced or enlarged while aspact ratio is kept. (the same as REDUCE|ENLARGE|KEEP_ASPECT)
	 */
	public static final int REDUCE_ENLARGE_KEEP_ASPECT = 0;

	/**
	 * Media option value to keep the image untouched, show it as is.
	 */
	public static final int CROP = 1;

	/**
	 * Media option Bit value to reduce the image. Can be used inconjunction with ENLARGE or KEEP_ASPECT.
	 */
	public static final int REDUCE = 2;
	/**
	 * Media option Bit value to enlarge the image. Can be used inconjunction with REDUCE or KEEP_ASPECT.
	 */
	public static final int ENLARGE = 4;
	/**
	 * Media option Bit value to keep the aspect ratio of the image. Must be used inconjunction with REDUCE and/or ENLARGE.
	 */
	public static final int KEEP_ASPECT = 8;
	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private String text = null;
	private int verticalAlignment = -1;
	private int horizontalAlignment = -1;
	private int imageMediaID;
	private int rolloverImageMediaID;
	private int onActionMethodID;
	private int onDoubleClickMethodID;
	private int onRightClickMethodID;
	private String toolTipText = null;
	private String dataProviderID = null;
	private Insets margin = null;
	private boolean displaysTags;
	private int rotation;
	private int mediaOptions;
	private int tabSeq = ISupportTabSeq.DEFAULT;
	private int rolloverCursor;
	private boolean showClick = true;
	private boolean showFocus = true;
	private String labelFor = null;
	private String mnemonic = null;

	/**
	 * Constructor I
	 */
	GraphicalComponent(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.GRAPHICALCOMPONENTS, parent, element_id, uuid);
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
	 * The text displayed when hovering over the component with a mouse cursor.
	 * 
	 * NOTE:
	 * HTML should be used for multi-line tooltips; you can also use any
	 * valid HTML tags to format tooltip text. For example: 
	 * <html>This includes<b>bolded text</b> and 
	 * <font color='blue'>BLUE</font> text as well.</html>
	 */
	public String getToolTipText()
	{
		return toolTipText;
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

	public void setMnemonic(String arg)
	{
		checkForChange(mnemonic, arg);
		mnemonic = arg;
	}

	/**
	 * The keyboard shortcut that activates this component. A letter must be specified, 
	 * and the actual shortcut will be combination of ALT + the specified letter.
	 * 
	 * This property can be used in two ways. Normally the keyboard shortcut activates 
	 * the onClick event of the component. But if the "labelFor" property is set for the
	 * component, then the keyboard shortcut will move the focus to the component whose
	 * label this component is.
	 */
	public String getMnemonic()
	{
		return mnemonic;
	}

	public void setLabelFor(String arg)
	{
		checkForChange(labelFor, arg);
		labelFor = arg;
	}

	/**
	 * Some components can be set to be labels of other components. This is useful in
	 * two situations. In table view mode it is used for constructing the header of the
	 * table. In record view mode, by setting mnemonics on the label, keyboard shortcuts
	 * can be used to set the focus to fields.
	 */
	public String getLabelFor()
	{
		return labelFor;
	}

	/**
	 * Set the text
	 * 
	 * @param arg the text
	 */
	public void setText(String arg)
	{
		checkForChange(text, arg);
		text = arg;
	}

	/**
	 * The text that is displayed inside the component.
	 */
	public String getText()
	{
		return text;
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

	public int getVerticalAlignment()
	{
		return verticalAlignment;
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

	public int getHorizontalAlignment()
	{
		return horizontalAlignment;
	}

	public boolean getMultiLine()
	{
		return true;
	}

	public boolean getAllowsTabs()
	{
		return true;
	}

	/**
	 * Set the rolloverImage
	 * 
	 * @param arg the rolloverImage
	 */
	public void setRolloverImageMediaID(int arg)
	{
		checkForChange(rolloverImageMediaID, arg);
		rolloverImageMediaID = arg;
	}

	/**
	 * The roll over image Media object used. When the mouse is moved over the component,
	 * this image Media will be displayed. When the mouse is moved out of the component,
	 * whatever text or image was being initially displayed will be restored.
	 */
	public int getRolloverImageMediaID()
	{
		return rolloverImageMediaID;
	}

	/**
	 * Set the Image
	 * 
	 * @param arg the Image
	 */
	public void setImageMediaID(int arg)
	{
		checkForChange(imageMediaID, arg);
		imageMediaID = arg;
	}

	/**
	 * The image Media object that should be displayed inside the component.
	 */
	public int getImageMediaID()
	{
		return imageMediaID;
	}

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
	 * The method that is executed when the component is clicked.
	 */
	public int getOnActionMethodID()
	{
		return onActionMethodID;
	}

	public void setOnDoubleClickMethodID(int arg)
	{
		checkForChange(onDoubleClickMethodID, arg);
		onDoubleClickMethodID = arg;
	}

	/**
	 * The method that is executed when the component is double clicked.
	 * 
	 * @templatedescription Perform the element double-click action
	 * @templatename onDoubleClick
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnDoubleClickMethodID()
	{
		return onDoubleClickMethodID;
	}

	public void setOnRightClickMethodID(int arg)
	{
		checkForChange(onRightClickMethodID, arg);
		onRightClickMethodID = arg;
	}

	/**
	 * The method that is executed when the component is right clicked.
	 */
	public int getOnRightClickMethodID()
	{
		return onRightClickMethodID;
	}

	@Deprecated
	public int getValuelistID()
	{
		return 0;
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

	/**
	 * Flag that enables or disables merging of data inside components using tags (placeholders).
	 * Tags (or placeholders) are words surrounded by %% on each side. There are data tags and
	 * standard tags. Data tags consist in names of dataproviders surrounded by %%. Standard tags
	 * are a set of predefined tags that are made available by the system.
	 * 
	 * See the "Merging data" section for more details about tags.
	 * 
	 * The default value of this flag is "false", that is merging of data is disabled by default.
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
	 * The rotation of the element. You can choose 0, 90, 180, or 270 and the label is rotated accordingly.  
	 * This property also applies to buttons and images.
	 */
	public int getRotation()
	{
		return rotation;
	}

	/**
	 * Sets the rotation (max 360)
	 * 
	 * @param i angle in degrees
	 */
	public void setRotation(int i)
	{
		if (i > 360) return;
		checkForChange(rotation, i);
		rotation = i;
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
			return ((getDataProviderID() != null) ? "L/B:" + getDataProviderID() : "L/B:" + getText()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Options to scale the image Media object that is displayed inside the component.
	 * Can be set to one or a combination of CROP, REDUCE, ENLARGE and KEEPASPECT.
	 * 
	 * REDUCE will scale down the image if the component is smaller than the image.
	 * REDUCE combined with KEEPASPECT will reduce the image, but keep its aspect ratio. 
	 * This is useful when the component has other proportions than the image.
	 * 
	 * ENLARGE will scale up the image if the component is larger than the image.
	 * ENLARGE combined with KEEPASPECT will scale up the image while keeping its aspect ratio.
	 * 
	 * CROP will leave the image at its original size. If the component is smaller than
	 * the image this will result in only a part of the image showing up.
	 */
	public int getMediaOptions()
	{
		if (mediaOptions == 0)
		{
			// set the default to 14 (Reduce/Enlarge with aspect)
			mediaOptions = 14;
		}
		return mediaOptions;
	}

	/**
	 * The cursor that is shown as the mouse is rolled over the component.
	 * Possible options are DEFAULT and HAND.
	 */
	public int getRolloverCursor()
	{
		return rolloverCursor;
	}

	public int getTabSeq()
	{
		if (onActionMethodID == 0) return -1;
		return tabSeq;
	}


	@Override
	public Dimension getSize()
	{
		if (size == null)
		{
			return new Dimension(80, 20);
		}
		return super.getSize();
	}

	/**
	 * Set the media options (bitset)
	 * 
	 * @param i the bitset
	 */
	public void setMediaOptions(int i)
	{
		checkForChange(mediaOptions, i);
		mediaOptions = i;
	}

	/**
	 * Set the rollover cursor type
	 * 
	 * @param i cursor type
	 * @see java.awt.Cursor
	 */
	public void setRolloverCursor(int i)
	{
		checkForChange(rolloverCursor, i);
		rolloverCursor = i;
	}

	/**
	 * Set the tab sequence
	 * 
	 * @param i
	 */
	public void setTabSeq(int i)
	{
		if (i < 1 && i != ISupportTabSeq.DEFAULT && i != ISupportTabSeq.SKIP) return;//irrelevant value from editor
		checkForChange(tabSeq, i);
		tabSeq = i;
	}

	/**
	 * When set, the element will show the clicked state when selected. 
	 * Applies to labels and buttons and images only.
	 */
	public boolean getShowClick()
	{
		return showClick;
	}

	/**
	 * Set the show click
	 * 
	 * @param arg
	 */
	public void setShowClick(boolean arg)
	{
		checkForChange(showClick, arg);
		showClick = arg;
	}

	/**
	 * When set the text of an element will showfocus when selected.
	 * Applies to labels and buttons only. 
	 * The text property for the element MUST be filled in first. 
	 *
	 * NOTE: The TAB key may also be used to select the element, depending 
	 * on the operating system being used and the selected LAF. 
	 */
	public boolean getShowFocus()
	{
		return showFocus;
	}

	/**
	 * Set the show focus
	 * 
	 * @param arg
	 */
	public void setShowFocus(boolean arg)
	{
		checkForChange(showFocus, arg);
		showFocus = arg;
	}
}
