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
package com.servoy.j2db.ui;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
public interface IScriptBaseMethods
{
	//types for getElementType
	public String BUTTON = "BUTTON";
	public String CALENDAR = "CALENDAR";
	public String CHECK = "CHECK";
	public String IMAGE_MEDIA = "IMAGE_MEDIA";
	public String LABEL = "LABEL";
	public String PASSWORD = "PASSWORD";
	public String PORTAL = "PORTAL";
	public String RADIOS = "RADIOS";
	public String TABPANEL = "TABPANEL";
	public String TEXT_AREA = "TEXT_AREA";
	public String TEXT_FIELD = "TEXT_FIELD";
	public String GROUP = "GROUP";
	public String COMBOBOX = "COMBOBOX";
	public String SPLITPANE = "SPLITPANE";
	public String RECTANGLE = "RECTANGLE";
	public String HTML_AREA = "HTML_AREA";
	public String RTF_AREA = "RTF_AREA";
	public String TYPE_AHEAD = "TYPE_AHEAD";

	/**
	 * Gets or sets the background color of a field. The color has to be set using the hexadecimal RGB value as used in HTML.
	 *
	 * @sample
	 * //sets the background color of the field
	 * %%prefix%%%%elementName%%.bgcolor = "#FFFFFF";
	 * //gets the background color of the field
	 * var c = %%prefix%%%%elementName%%.bgcolor;
	 */
	public String js_getBgcolor();

	public void js_setBgcolor(String clr);

	/**
	 * Gets or sets the foreground color of a field. The color has to be set using the hexadecimal RGB value as used in HTML.
	 *
	 * @sample
	 * //sets the foreground color of the field
	 * %%prefix%%%%elementName%%.fgcolor = "#000000";
	 * 
	 * //gets the foreground color of the field
	 * var c = %%prefix%%%%elementName%%.fgcolor;
	 */
	public String js_getFgcolor();

	public void js_setFgcolor(String clr);

	/**
	 * Gets or sets the visibility of an element; true - visible; false - not visible; ! - the visibility state is inverted (the opposite).
	 * 
	 * NOTE: The visibility of an element is not persistent; the state of visibility only applies to the current user in his/her current session.
	 *
	 * @sample
	 * //sets the element as visible
	 * forms.company.elements.faxBtn.visible = true;
	 * 
	 * //gets the visibility of the element
	 * var currentState = forms.company.elements.faxBtn.visible;
	 * 
	 * //sets the element as not visible when the current state is visible
	 * forms.company.elements.faxBtn.visible = !currentState;
	 */
	public boolean js_isVisible();

	public void js_setVisible(boolean b);

	/**
	 * Gets or sets the enabled state of a specified field, also known as "grayed".
	 * true - enabled; false - not enabled; ! - the enabled state is inverted (the opposite).
	 * 
	 * NOTE: A disabled element cannot be selected by clicking the element (or by pressing the TAB key even if this option is supported by the operating system).
	 *
	 * NOTE: A label or button element will not disable if the "displayType" design time property for a field is set to HTML_AREA.
	 * 
	 * NOTE: The disabled "grayed" color is dependent on the LAF set in the Servoy Client Application Preferences. For more information see Preferences: Look And Feel in the Servoy Developer User's Guide.
	 *
	 * @sample
	 * //gets the enabled state of the field
	 * var currState = %%prefix%%%%elementName%%.enabled;
	 * 
	 * //sets the enabled state of the field
	 * %%prefix%%%%elementName%%.enabled = !currentState;
	 */
	public boolean js_isEnabled();

	public void js_setEnabled(boolean b);

	/**
	 * Returns the x location of the current element. 
	 * 
	 * NOTE: getLocationX() can be used with getLocationY() to set the location of an element using the setLocation function. For Example:
	 * 
	 * //returns the X and Y coordinates
	 * var x = forms.company.elements.faxBtn.getLocationX();
	 * var y = forms.company.elements.faxBtn.getLocationY();
	 *  
	 * //sets the new location 10 px to the right; 10 px down from the current location
	 * forms.company.elements.faxBtn.setLocation(x+10,y+10);
	 *
	 * @sample
	 * var x = %%prefix%%%%elementName%%.getLocationX();
	 * 
	 * @return The x location of the element in pixels.
	 */
	public int js_getLocationX();

	/**
	 * Returns the y location of the current element. 
	 * 
	 * NOTE: getLocationY() can be used with getLocationX() to set the location of an element using the setLocation function. For Example:
	 * 
	 * //returns the X and Y coordinates
	 * var x = forms.company.elements.faxBtn.getLocationX();
	 * var y = forms.company.elements.faxBtn.getLocationY();
	 * 
	 * //sets the new location 10 px to the right; 10 px down from the current location
	 * forms.company.elements.faxBtn.setLocation(x+10,y+10);
	 *
	 * @sample 
	 * var y =  %%prefix%%%%elementName%%.getLocationY();
	 * 
	 * @return The y location of the element in pixels.
	 */
	public int js_getLocationY();

	/**
	 * Returns the absolute form (designed) Y location.
	 *
	 * @sample
	 * var absolute_y = %%prefix%%%%elementName%%.getAbsoluteFormLocationY();
	 * 
	 * @return The y location of the form in pixels.
	 */
	public int js_getAbsoluteFormLocationY();

	/**
	 * Sets the location of an element. It takes as input the X (horizontal) and Y (vertical) coordinates - starting from the TOP LEFT side of the screen.
	 * 
	 * NOTE: getLocationX() can be used with getLocationY() to return the current location of an element; then use the X and Y coordinates with the setLocation function to set a new location. For Example:
	 * 
	 *  //returns the X and Y coordinates
	 *  var x = forms.company.elements.faxBtn.getLocationX();
	 *  var y = forms.company.elements.faxBtn.getLocationY();
	 *  
	 *  //sets the new location 10 px to the right; 10 px down from the current location
	 *  forms.company.elements.faxBtn.setLocation(x+10,y+10);
	 *
	 * @sample
	 * %%prefix%%%%elementName%%.setLocation(200,200);
	 *
	 * @param x 
	 * the X coordinate of the element in pixels.
	 *
	 * @param y
	 * the Y coordinate of the element in pixels.
	 */
	public void js_setLocation(int x, int y);

	/**
	 * Sets the size of the field. It takes as input the width and the height. 
	 * 
	 * NOTE: getWidth() can be used with getHeight() to set the size of an element using the setSize function. For Example: 
	 * 
	 * //returns the width (w) and height (h)
	 * var w = forms.company.elements.faxBtn.getWidth();
	 * var h = forms.company.elements.faxBtn.getHeight();
	 * 
	 * //sets the new size
	 * forms.company.elements.faxBtn.setSize(w,h);
	 * 
	 * //sets the new size and adds 1 px to both the width and height
	 * forms.company.elements.faxBtn.setSize(w+1,h+1);
	 *
	 * @sample
	 * %%prefix%%%%elementName%%.setSize(20,30);
	 *
	 * @param width 
	 * the width of the element in pixels.
	 *
	 * @param height
	 *  the height of the element in pixels. 
	 */
	public void js_setSize(int width, int height);

	/**
	 * Returns the width of the current element. 
	 * 
	 * NOTE: getWidth() can be used with getHeight() to set the size of an element using the setSize function. For Example:
	 * 
	 *  //returns the width (w) and height (h)
	 *  var w = forms.company.elements.faxBtn.getWidth();
	 *  var h = forms.company.elements.faxBtn.getHeight();
	 *  
	 *  //sets the new size
	 *  forms.company.elements.faxBtn.setSize(w,h);
	 *  
	 *  //sets the new size and adds 1 px to both the width and height
	 *  forms.company.elements.faxBtn.setSize(w+1,h+1);
	 *
	 * @sample
	 * var w = %%prefix%%%%elementName%%.getWidth();
	 * 
	 * @return The width of the element in pixels.
	 */
	public int js_getWidth();

	/**
	 * Returns the height of the current element. 
	 * NOTE: getHeight() can be used with getWidth() to set the size of an element using the setSize function. For example:
	 * 
	 * //returns the width (w) and height (h)
	 * var w = forms.company.elements.faxBtn.getWidth();
	 * var h = forms.company.elements.faxBtn.getHeight();
	 *  
	 * //sets the new size
	 * forms.company.elements.faxBtn.setSize(w,h);
	 * 
	 * //sets the new size and adds 1 px to both the width and height
	 * forms.company.elements.faxBtn.setSize(w+1,h+1);
	 *
	 * @sample
	 * var ht = %%prefix%%%%elementName%%.getHeight();
	 * 
	 * @return The height of the element in pixels.
	 */
	public int js_getHeight();

	/**
	 * Returns the name of an element. (may be null as well)
	 *
	 * @sample 
	 * var name = %%prefix%%%%elementName%%.getName();
	 * 
	 * @return The name of the element.
	 */
	public String js_getName();

	/**
	 * Returns the type of a specified element.
	 * 
	 * @sample var et = %%prefix%%%%elementName%%.getElementType();
	 * 
	 * @return The display type of the element as String.
	 */
	public String js_getElementType();

	/**
	 * Sets the value for the specified element client property key.
	 *
	 * NOTE: Depending on the operating system, a user interface property name may be available.
	 * 
	 * @sample
	 * %%prefix%%%%elementName%%.putClientProperty('ToolTipText','some text');
	 * 
	 * @param key user interface key (depends on operating system)
	 * @param value a predefined value for the key
	 */
	public void js_putClientProperty(Object key, Object value);

	/**
	 * Gets the specified client property for the element based on a key.
	 * 
	 * NOTE: Depending on the operating system, a user interface property name may be available.
	 *
	 * @sample var property = %%prefix%%%%elementName%%.getClientProperty('ToolTipText');
	 * 
	 * @param key user interface key (depends on operating system)
	 * 
	 * @return The value of the property for specified key.
	 */
	public Object js_getClientProperty(Object key);

	/**
	 * Gets or sets the border attribute(s) of a specified element. 
	 * 
	 * The border attributes:
	 * 
	 * borderType - EmptyBorder, EtchedBorder, BevelBorder, LineBorder, TitleBorder, MatteBorder, SpecialMatteBorder.
	 * size - (numeric value) for: bottom, left, right, top.
	 * color - (hexadecimal value) for: bottom, left, right, top.
	 * dash pattern - (numeric value) for selected side(s).
	 * rounding radius - (numeric value) for selected side(s).
	 * 
	 * NOTE: Use the same value(s) and order of attribute(s) from the element design time property "borderType".
	 *
	 * @sample
	 * //sets the border type to "LineBorder"
	 * //sets a 1 px line width for the bottom and left side of the border
	 * //sets the hexadecimal color of the border to "#ccffcc"
	 * var b = %%prefix%%%%elementName%%.setBorder('LineBorder,1,#ccffcc');
	 *
	 * @param spec the border attributes
	 */
	public String js_getBorder();

	public void js_setBorder(String spec);

}