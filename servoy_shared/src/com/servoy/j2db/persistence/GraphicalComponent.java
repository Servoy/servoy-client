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

import com.servoy.base.persistence.IBaseGraphicalComponent;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.util.UUID;

/**
 * This component can be a label (with image),image or button (with image/or text)
 *
 * @author jblok
 */
public class GraphicalComponent extends BaseComponent
	implements ISupportTextEditing, ISupportTextSetup, ISupportDataProviderID, ISupportTabSeq, ISupportMedia, IBaseGraphicalComponent
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default value for the media options. Image will be reduced or enlarged while aspect ratio is kept. (the same as REDUCE|ENLARGE|KEEP_ASPECT)
	 */
	public static final int REDUCE_ENLARGE_KEEP_ASPECT = 0;

	/**
	 * Media option value to keep the image untouched, show it as is.
	 */
	public static final int CROP = 1;

	/**
	 * Media option Bit value to reduce the image. Can be used in conjunction with ENLARGE or KEEP_ASPECT.
	 */
	public static final int REDUCE = 2;
	/**
	 * Media option Bit value to enlarge the image. Can be used in conjunction with REDUCE or KEEP_ASPECT.
	 */
	public static final int ENLARGE = 4;
	/**
	 * Media option Bit value to keep the aspect ratio of the image. Must be used in conjunction with REDUCE and/or ENLARGE.
	 */
	public static final int KEEP_ASPECT = 8;

	/**
	 * Constructor I
	 */
	protected GraphicalComponent(ISupportChilds parent, int element_id, UUID uuid)
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
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TOOLTIPTEXT, arg);
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
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TOOLTIPTEXT);
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

	public void setMnemonic(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_MNEMONIC, arg);
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
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_MNEMONIC);
	}

	public void setLabelFor(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LABELFOR, arg);
	}

	/**
	 * Some components can be set to be labels of other components. This is useful in
	 * two situations. In table view mode it is used for constructing the header of the
	 * table. In record view mode, by setting mnemonics on the label, keyboard shortcuts
	 * can be used to set the focus to fields.
	 */
	public String getLabelFor()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_LABELFOR);
	}

	/**
	 * Set the text
	 *
	 * @param arg the text
	 */
	public void setText(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TEXT, arg);
	}

	public String getText()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TEXT);
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

	public int getVerticalAlignment()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_VERTICALALIGNMENT).intValue();
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

	public int getHorizontalAlignment()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_HORIZONTALALIGNMENT).intValue();
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
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ROLLOVERIMAGEMEDIAID, arg);
	}

	/**
	 * The roll over image Media object used. It will only work if a property image is also used.
	 * When the mouse is moved over the component, this image Media will be displayed.
	 * When the mouse is moved out of the component, whatever text or image was being initially
	 * displayed will be restored. Note that roll over image is not supported in Smart client for list view and tableview forms.
	 */
	public int getRolloverImageMediaID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ROLLOVERIMAGEMEDIAID).intValue();
	}

	/**
	 * Set the Image
	 *
	 * @param arg the Image
	 */
	public void setImageMediaID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_IMAGEMEDIAID, arg);
	}

	/**
	 * The image Media object that should be displayed inside the component.
	 */
	public int getImageMediaID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_IMAGEMEDIAID).intValue();
	}

	public void setOnActionMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID, arg);
	}

	/**
	 * @templatedescription Perform the element onclick action
	 * @templatename onClick
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnActionMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID).intValue();
	}

	public void setOnDoubleClickMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDOUBLECLICKMETHODID, arg);
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
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDOUBLECLICKMETHODID).intValue();
	}

	public void setOnRightClickMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID, arg);
	}

	/**
	 * The method that is executed when the component is right clicked.
	 *
	 * @templatedescription Perform the element on right click action
	 * @templatename onClick
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnRightClickMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID).intValue();
	}

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnRenderMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID, arg);
	}

	/**
	 * The method that is executed when the component is rendered.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnRenderMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID).intValue();
	}

	@Deprecated
	public int getValuelistID()
	{
		return 0;
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
	 * The rotation of the element. You can choose 0, 90, 180, or 270 and the label is rotated accordingly.
	 * This property also applies to buttons and images.
	 *
	 * @deprecated Renamed to textRotation.
	 */
	@Deprecated
	public int getRotation()
	{
		return getTextRotation();
	}

	/**
	 * Sets the rotation (max 360)
	 *
	 * @param i angle in degrees
	 *
	 * @deprecated Renamed to textRotation.
	 */
	@Deprecated
	public void setRotation(int i)
	{
		setTextRotation(i);
	}


	/**
	 * Sets the rotation (max 360)
	 *
	 * @param i angle in degrees
	 */
	public void setTextRotation(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TEXTROTATION, i);
	}

	/**
	 * The rotation of the element. You can choose 0, 90, 180, or 270 and the label is rotated accordingly.
	 * This property also applies to buttons and images.
	 */
	public int getTextRotation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TEXTROTATION).intValue();
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
		int mediaOptions = getTypedProperty(StaticContentSpecLoader.PROPERTY_MEDIAOPTIONS).intValue();
		if (mediaOptions == 0)
		{
			// set the default to 14 (Reduce/Enlarge with aspect)
			mediaOptions = 14;
		}
		return mediaOptions;
	}

	/**
	 * The cursor that is shown as the mouse is rolled over the component.
	 * Possible options are DEFAULT and HAND. Note that roll over cursor is not supported in Smart client for list view and tableview forms.
	 */
	public int getRolloverCursor()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ROLLOVERCURSOR).intValue();
	}

	public int getTabSeq()
	{
		if (getOnActionMethodID() == 0) return -1;
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TABSEQ).intValue();
	}


	@Override
	public Dimension getSize()
	{
		Dimension size = getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE);
		if (size == null)
		{
			return new Dimension(80, 20);
		}
		return size;
	}

	/**
	 * Set the media options (bitset)
	 *
	 * @param i the bitset
	 */
	public void setMediaOptions(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_MEDIAOPTIONS, i);
	}

	/**
	 * Set the rollover cursor type
	 *
	 * @param i cursor type
	 * @see java.awt.Cursor
	 */
	public void setRolloverCursor(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ROLLOVERCURSOR, i);
	}

	/**
	 * Set the tab sequence
	 *
	 * @param i
	 */
	public void setTabSeq(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TABSEQ, i);
	}

	/**
	 * When set, the element will show the clicked state when selected.
	 * Applies to labels and buttons and images only.
	 */
	public boolean getShowClick()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWCLICK).booleanValue();
	}

	/**
	 * Set the show click
	 *
	 * @param arg
	 */
	public void setShowClick(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWCLICK, arg);
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
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWFOCUS).booleanValue();
	}

	/**
	 * Set the show focus
	 *
	 * @param arg
	 */
	public void setShowFocus(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWFOCUS, arg);
	}

	public void setFormat(String format)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FORMAT, format);
	}

	public String getFormat()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FORMAT);
	}

}
