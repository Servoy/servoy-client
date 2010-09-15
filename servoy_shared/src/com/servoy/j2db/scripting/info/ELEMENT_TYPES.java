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
package com.servoy.j2db.scripting.info;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class ELEMENT_TYPES implements IPrefixedConstantsObject
{
	/**
	 * Constant representing an element of the Button type.
	 *
	 * @sample
	 * //the return value for an element of the Button type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.BUTTON)
	 * {
	 *      // element is a Button component
	 * }
	 */
	public static final String BUTTON = "BUTTON"; //$NON-NLS-1$

	/**
	 * Constant representing an element of the Calendar type.
	 *
	 * @sample
	 * //the return value for an element of the Calendar type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.CALENDAR)
	 * {
	 *      // element is a Calendar field
	 * }
	 */
	public static final String CALENDAR = "CALENDAR"; //$NON-NLS-1$

	/**
	 * Constant representing an element of the Check type.
	 *
	 * @sample
	 * //the return value for an element of the Check type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.CHECK)
	 * {
	 *      // element is a Check(box) field
	 * }
	 */
	public static final String CHECK = "CHECK"; //$NON-NLS-1$

	/**
	 * Constant representing an element of the ImageMedia type.
	 *
	 * @sample
	 * //the return value for an element of the ImageMedia type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.IMAGE_MEDIA)
	 * {
	 *      // element is a Image Media field
	 * }
	 */
	public static final String IMAGE_MEDIA = "IMAGE_MEDIA"; //$NON-NLS-1$

	/**
	 * Constant representing an element of the Label type.
	 *
	 * @sample
	 * //the return value for an element of the Label type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.LABEL)
	 * {
	 *      // element is a Label component
	 * }
	 */
	public static final String LABEL = "LABEL"; //$NON-NLS-1$

	/**
	 * Constant representing an element of the Password type.
	 *
	 * @sample
	 * //the return value for an element of the Password type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.PASSWORD)
	 * {
	 *      // element is a Password component
	 * }
	 */
	public static final String PASSWORD = "PASSWORD"; //$NON-NLS-1$

	/**
	 * Constant representing an element of the Portal type.
	 *
	 * @sample
	 * //the return value for an element of the Portal type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.PORTAL)
	 * {
	 *      // element is a Portal component
	 * }
	 */
	public static final String PORTAL = "PORTAL"; //$NON-NLS-1$

	/**
	 * Constant representing an element of the Radios type.
	 *
	 * @sample
	 * //the return value for an element of the Radios type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.RADIOS)
	 * {
	 *      // element is a Radios field.
	 * }
	 */
	public static final String RADIOS = "RADIOS"; //$NON-NLS-1$

	/**
	 * Constant representing an element of the Tabpanel type.
	 *
	 * @sample
	 * //the return value for an element of the Tabpanel type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.TABPANEL)
	 * {
	 *      // element is a Tabpanel component
	 * }
	 */
	public static final String TABPANEL = "TABPANEL"; //$NON-NLS-1$

	/**
	 * Constant representing an element of the TextArea type.
	 *
	 * @sample
	 * //the return value for an element of the TextArea type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.TEXT_AREA)
	 * {
	 *      // element is a TextArea field
	 * }
	 */
	public static final String TEXT_AREA = "TEXT_AREA"; //$NON-NLS-1$

	/**
	 * Constant representing an element of the TextField type.
	 *
	 * @sample
	 * //the return value for an element of the TextField type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.TEXT_FIELD)
	 * {
	 *      // element is a text field
	 * }
	 */
	public static final String TEXT_FIELD = "TEXT_FIELD"; //$NON-NLS-1$

	/**
	 * Constant representing a Group of elements.
	 *
	 * @sample
	 * //the return value for an element of the TextField type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.GROUP)
	 * {
	 *      // element is a group element
	 * }
	 */
	public static final String GROUP = "GROUP"; //$NON-NLS-1$

	public String getPrefix()
	{
		return "ELEMENT_TYPES"; //$NON-NLS-1$
	}
}
