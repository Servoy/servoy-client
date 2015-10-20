/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.persistence;

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Base interface for graphical components (for mobile as well as other clients).
 * 
 * @author rgansevles
 *
 * @since 7.0
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseFieldCommon extends IBaseComponent
{
	/**
	 * The dataprovider of the component.
	 */
	String getDataProviderID();

	void setDataProviderID(String arg);

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMField#getDisplayType()
	 */
	int getDisplayType();

	void setDisplayType(int arg);

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMField#getPlaceholderText()
	 */
	String getPlaceholderText();

	void setPlaceholderText(String arg);

	/**
	 * The format that should be applied when displaying the data in the component.
	 * There are different options for the different dataprovider types that are assigned to this field.
	 * For Integer fields, there is a display and an edit format, using http://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html and the max (string) length can be set.
	 * For Text/String fields, there are options to force uppercase,lowercase or only numbers. Or a mask can be set that restrict the input the pattern chars can be found here: http://docs.oracle.com/javase/7/docs/api/javax/swing/text/MaskFormatter.html
	 * A mask can have a placehoder (what is shown when there is no data) and if the data must be stored raw (without literals of the mask). A max text length can also be set to force
	 * the max text length input, this doesn't work on mask because that max length is controlled with the mask.
	 * For Date fields a display and edit format can be set by using a pattern from here: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html, you can also say this must behave like a mask (the edit format)
	 * A mask only works with when the edit format is exactly that mask (1 char is 1 number/char), because for example MM then only 2 numbers are allowed MMM that displays the month as a string is not supported as a mask.
	 * Some examples are "dd-MM-yyyy", "MM-dd-yyyy", etc.
	 * The format property is also used to set the UI Converter, this means that you can convert the value object to something else before it gets set into the field, this can also result in a type change of the data. 
	 * So a string in scripting/db is converted to a integer in the ui, then you have to set an integer format. 
	 * This property is applicable only for types: TEXT_FIELD, COMBOBOX, TYPE_AHEAD, CALENDAR and SPINNER.
	 */
	String getFormat();

	void setFormat(String format);
}