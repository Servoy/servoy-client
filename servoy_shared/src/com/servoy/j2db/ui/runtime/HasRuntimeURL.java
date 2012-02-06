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

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

/**
 * Interface for components with value list support
 * 
 * @author rgansevles
 * 
 * @since 6.1
 */
public interface HasRuntimeURL
{
	/**
	 * Gets or sets the relative URL string contained in a field with a design time display property of HTML_AREA only. 
	 * 
	 * NOTE: For information about field element design time properties, see the section on Field elements in the Forms chapter of the Servoy Developer User's Guide.
	 * 
	 * @sample
	 * //sets the relative URL of the HTML_AREA field
	 * %%prefix%%%%elementName%%.URL = "http://www.examples.com/icon.gif";
	 * 
	 * //gets the relative URL of the HTML_AREA field
	 * var theURL = %%prefix%%%%elementName%%.URL;
	 */
	@JSGetter
	public String getURL();

	@JSSetter
	public void setURL(String url);

	/**
	 * Gets or sets the absolute base URL for the URL string contained in a field with a design time display property of HTML_AREA only.
	 *
	 * @sample
	 * //gets the base URL of theField when display is HTML_AREA
	 * var baseURL = %%prefix%%%%elementName%%.baseURL;
	 * 
	 * //sets the base URL of theField when display is HTML_AREA
	 * %%prefix%%%%elementName%%.baseURL = "http://www.examples.com";
	 */
	@JSGetter
	public String getBaseURL();

	@JSSetter
	public void setBaseURL(String url);
}
