/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * Runtime property interface for tooltip.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeTooltip
{
	/**
	 * Gets or sets the tool tip text of an element; text displays when the mouse cursor hovers over an element. 
	 * 
	 * NOTE: HTML should be used for multi-line tooltips; you can also use any valid HTML tags to format tooltip text.
	 *
	 * @sample
	 * //gets the tooltip text of the element
	 * var toolTip = %%prefix%%%%elementName%%.toolTipText;
	 * 
	 * //sets the tooltip text of the element
	 * %%prefix%%%%elementName%%.toolTipText = "New tip";
	 * %%prefix%%%%elementName%%.toolTipText = "<html>This includes <b>bolded text</b> and <font color='blue'>BLUE</font> text as well.";
	 */
	@JSReadonlyProperty
	public String getToolTipText();

	public void setToolTipText(String tooltip);
}
