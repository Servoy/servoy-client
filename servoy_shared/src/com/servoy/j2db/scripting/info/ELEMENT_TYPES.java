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
import com.servoy.j2db.ui.runtime.IRuntimeComponent;

@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@SuppressWarnings("nls")
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
	public static final String BUTTON = IRuntimeComponent.BUTTON;

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
	public static final String CALENDAR = IRuntimeComponent.CALENDAR;

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
	public static final String CHECK = IRuntimeComponent.CHECK;

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
	public static final String IMAGE_MEDIA = IRuntimeComponent.IMAGE_MEDIA;

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
	public static final String LABEL = IRuntimeComponent.LABEL;

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
	public static final String PASSWORD = IRuntimeComponent.PASSWORD;

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
	public static final String PORTAL = IRuntimeComponent.PORTAL;

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
	public static final String RADIOS = IRuntimeComponent.RADIOS;

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
	public static final String TABPANEL = IRuntimeComponent.TABPANEL;

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
	public static final String TEXT_AREA = IRuntimeComponent.TEXT_AREA;

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
	public static final String TEXT_FIELD = IRuntimeComponent.TEXT_FIELD;

	/**
	 * Constant representing a Group of elements.
	 *
	 * @sample
	 * //the return value for an element type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.GROUP)
	 * {
	 *      // element is a group element
	 * }
	 */
	public static final String GROUP = IRuntimeComponent.GROUP;

	/**
	 * Constant representing a combobox element.
	 *
	 * @sample
	 * //the return value for an element of the TextField type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.COMBOBOX)
	 * {
	 *      // element is a COMBOBOX element
	 * }
	 */
	public static final String COMBOBOX = IRuntimeComponent.COMBOBOX;

	/**
	 * Constant representing a splitpane element.
	 *
	 * @sample
	 * //the return value for an element type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.SPLITPANE)
	 * {
	 *      // element is a splitpane element
	 * }
	 */
	public static final String SPLITPANE = IRuntimeComponent.SPLITPANE;

	/**
	 * Constant representing a accordionpanel element.
	 *
	 * @sample
	 * //the return value for an element type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.ACCORDIONPANEL)
	 * {
	 *      // element is a accordion panel element
	 * }
	 */
	public static final String ACCORDIONPANEL = IRuntimeComponent.ACCORDIONPANEL;

	/**
	 * Constant representing a rectangle element.
	 *
	 * @sample
	 * //the return value for an element of the TextField type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.RECTANGLE)
	 * {
	 *      // element is a rectangle element
	 * }
	 */
	public static final String RECTANGLE = IRuntimeComponent.RECTANGLE;

	/**
	 * Constant representing a html area element.
	 *
	 * @sample
	 * //the return value for an element of the TextField type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.HTML_AREA)
	 * {
	 *      // element is a HTML textarea
	 * }
	 */
	public static final String HTML_AREA = IRuntimeComponent.HTML_AREA;

	/**
	 * Constant representing a rtf area of element.
	 *
	 * @sample
	 * //the return value for an element of the TextField type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.RTF_AREA)
	 * {
	 *      // element is a RTF textarea.
	 * }
	 */
	public static final String RTF_AREA = IRuntimeComponent.RTF_AREA;

	/**
	 * Constant representing a typeahead element.
	 *
	 * @sample
	 * //the return value for an element of the TextField type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.TYPE_AHEAD)
	 * {
	 *      // element is a type ahead element
	 * }
	 */
	public static final String TYPE_AHEAD = IRuntimeComponent.TYPE_AHEAD;

	/**
	 * Constant representing a listbox element.
	 *
	 * @sample
	 * //the return value for an element of the ListBox type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.LIST_BOX)
	 * {
	 *      // element is a LIST_BOX element
	 * }
	 */
	public static final String LIST_BOX = IRuntimeComponent.LIST_BOX;

	/**
	 * Constant representing a spinner element.
	 *
	 * @sample
	 * //the return value for an element of the Spinner type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.SPINNER)
	 * {
	 *      // element is a SPINNER element
	 * }
	 */
	public static final String SPINNER = IRuntimeComponent.SPINNER;

	/**
	 * Constant representing a multi selection listbox element.
	 *
	 * @sample
	 * //the return value for an element of the TextField type, as returned by the following code
	 * var etype = elements.elementName.getElementType();
	 * if (etype == ELEMENT_TYPES.MULTI_SELECTION_LIST_BOX)
	 * {
	 *      // element is a MULTI_SELECTION_LIST_BOX element
	 * }
	 */
	public static final String MULTI_SELECTION_LIST_BOX = IRuntimeComponent.MULTI_SELECTION_LIST_BOX;

	/**
	 * Constant representing a form element.
	 *
	 * @sample
	 * //the return value for an element of the Form type, as returned by the following code
	 * var renderElementType = event.getRenderable().getElementType();
	 * if (renderElementType == ELEMENT_TYPES.FORM)
	 * {
	 *      // element is a type form element
	 * }
	 */
	public static final String FORM = IRuntimeComponent.FORM;

	public String getPrefix()
	{
		return "ELEMENT_TYPES";
	}
}
