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
package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;


/**
 * Interface for components with formatting support.
 * 
 * @author jcompagner
 * @since 6.1
 */
public interface HasRuntimeFormat
{
	/**
	 * Gets or sets the display formatting of an element for number and text values; does not affect the actual value stored in the database column.
	 *
	 * There are different options for the different dataprovider types that is assigned to this field.
	 * <p>For Integer fields, there is a display and edit format, using http://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html and the max (string) length can be set.
	 * <p>For Text/String fields, there are options to force uppercase,lowercase or only numbers. Or a mask can be set that restrict the input the pattern chars can be found here: http://docs.oracle.com/javase/7/docs/api/javax/swing/text/MaskFormatter.html
	 * A mask can have a placehoder (what is shown when there is no data) and if the data must be stored raw (without literals of the mask). A max text length can also be set to force
	 * the max text length input, this doesn't work on mask because that max length is controlled with the mask.
	 * <p>For Date fields a display and edit format can be set by using a pattern from here: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html, you can also say this must behave like a mask (the edit format)
	 * A mask only works with when the edit format is exactly that mask (1 char is 1 number/char), because for example MM then only 2 numbers are allowed MMM that displays the month as a string is not supported as a mask.
	 * Some examples are "dd-MM-yyyy", "MM-dd-yyyy", etc.
	 * <p>The format property is also used to set the UI Converter, this means that you can convert the value object to something else before it gets set into the field, this can also result in a type change of the data. 
	 * So a string in scripting/db is converted to a integer in the ui, then you have to set an integer format.
	 * <p>It only returns it's correct value if it was explicitly set, otherwise null.
	 * 
	 * @sample
	 * //sets the display formatting of the field
	 * %%prefix%%%%elementName%%.format = '###';
	 * 
	 * //gets the display formatting of the field
	 * var format = %%prefix%%%%elementName%%.format;
	 */
	@JSGetter
	public String getFormat();

	@JSSetter
	public void setFormat(String format);
}
