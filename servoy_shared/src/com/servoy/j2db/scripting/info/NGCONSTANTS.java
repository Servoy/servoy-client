/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@ServoyClientSupport(ng = true, wc = false, sc = false)
public class NGCONSTANTS implements IPrefixedConstantsObject
{

	/**
	 * When the user navigates to another page, closes the browser or is disconnected from the server, the client-session on the server
	 * will be kept for a limited time. If the user returns within that time the client session is continued.
	 * <p>
	 * This time can be configured at the server (60 seconds by default) and can overridden for the current ng-client session using
	 * application.putClientProperty with APP_NG_PROPERTY.WINDOW_TIMEOUT.
	 * <p>
	 * The value is specified in seconds.
	 *
	 * @sample
	 * // allow the user to return within 1 hour before the session is cleaned up
	 * application.putClientProperty(APP_NG_PROPERTY.WINDOW_TIMEOUT, 3600);
	 *
	 * // get the current active timeout value, when not overriden via putClientProperty this will return the system value.
	 * var timeout = application.getClientProperty(APP_NG_PROPERTY.WINDOW_TIMEOUT);
	 *
	 * // reset the value to the system value.
	 * application.putClientProperty(APP_NG_PROPERTY.WINDOW_TIMEOUT, null);
	 */
	public static final String WINDOW_TIMEOUT = "window.timeout"; //$NON-NLS-1$

	/**
	 * When use branding is enabled (see servoy.branding setting in Servoy Admin Page / servoy.properties) this client
	 * property can be used to set the main window title text.
	 *
	 * @sample
	 * // set main window title onSolutionOpen
	 * application.putClientProperty(APP_NG_PROPERTY.WINDOW_BRANDING_TITLE, "My app title");
	 */
	public static final String WINDOW_BRANDING_TITLE = "window.branding.title";

	/**
	 * Client property used to set the icon of the main window. This should be a PNG
	 * image of size 32x32, and it can be the file name that is stored under the web app root
	 * or it can be a base64 encoded image from the solution.
	 *
	 * @sample
	 * // set image from the web app root onSolutionOpen
	 * application.putClientProperty(APP_NG_PROPERTY.WINDOW_BRANDING_ICON_32, "favicon32x32.png");
	 * // set base64 encoded image from solution onSolutionOpen
	 * 	var img = solutionModel.getMedia("favicon32x32.png")
	 *	var imgAsBase64 = new Packages.org.apache.commons.codec.binary.Base64().encodeAsString(img.bytes);
	 *	var imgHref = "data:image/png;base64," + imgAsBase64;
	 *	application.putClientProperty(APP_NG_PROPERTY.WINDOW_BRANDING_ICON_32, imgHref);
	 */
	public static final String WINDOW_BRANDING_ICON_32 = "window.branding.icon.32";

	/**
	 * Same as the WINDOW_BRANDING_ICON_32 client property just for images of size 192x192,
	 * usually used as shortcut icon for the web app.
	 */
	public static final String WINDOW_BRANDING_ICON_192 = "window.branding.icon.192";

	/**
	 * By default the NGClient appends the name of the current main form to the url in the address bar of the browser using a fragment identifier (#....)
	 * <p>
	 * By setting the FORM_BASED_BROWSER_HISTORY property to false, this is disabled
	 *
	 * The value can be true/false
	 * DEFAULT: true
	 *
	 * @sample
	 * application.putClientProperty(APP_NG_PROPERTY.FORM_BASED_BROWSER_HISTORY, false);
	 */
	public static final String FORM_BASED_BROWSER_HISTORY = "servoy.ngclient.formbased_browser_history"; //$NON-NLS-1$

	public String getPrefix()
	{
		return "APP_NG_PROPERTY";
	}
}
