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
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * <p>The NGClient/Titanium Client constants provide essential configurations to
 * control the behavior and appearance of web applications. These constants allow
 * developers to customize aspects such as browser history, search mechanisms,
 * branding, and session management, offering flexibility in creating user-centric
 * experiences.</p>
 *
 * <p>Browser history can be managed using the <code>FORM_BASED_BROWSER_HISTORY</code>
 * constant, which determines whether the main form's name is appended to the URL in
 * the browser's address bar. For enhanced search functionality, the
 * <code>VALUELIST_CONTAINS_SEARCH</code> constant enables "contains" searches,
 * offering broader query matching compared to default behaviors.</p>
 *
 * <p>Branding is supported through properties like <code>WINDOW_BRANDING_ICON_32</code>
 * and <code>WINDOW_BRANDING_ICON_192</code>, which define icon images for windows or
 * app shortcuts. The <code>WINDOW_BRANDING_TITLE</code> constant allows customization
 * of the window's title text, providing a consistent and professional appearance
 * when branding is enabled.</p>
 *
 * <p>Session management is facilitated by the <code>WINDOW_TIMEOUT</code> constant,
 * which specifies the duration for which a user session remains active after
 * disconnection or navigation away from the app. This feature ensures continuity
 * and flexibility for users returning within the configured timeframe.</p>
 *
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
	 * application.putClientProperty with NGCONSTANTS.WINDOW_TIMEOUT.
	 * <p>
	 * The value is specified in seconds.
	 *
	 * @sample
	 * // allow the user to return within 1 hour before the session is cleaned up
	 * application.putClientProperty(NGCONSTANTS.WINDOW_TIMEOUT, 3600);
	 *
	 * // get the current active timeout value, when not overriden via putClientProperty this will return the system value.
	 * var timeout = application.getClientProperty(NGCONSTANTS.WINDOW_TIMEOUT);
	 *
	 * // reset the value to the system value.
	 * application.putClientProperty(NGCONSTANTS.WINDOW_TIMEOUT, null);
	 */
	public static final String WINDOW_TIMEOUT = "window.timeout"; //$NON-NLS-1$

	/**
	 * When use branding is enabled (see servoy.branding setting in Servoy Admin Page / servoy.properties) this client
	 * property can be used to set the main window title text.
	 *
	 * @sample
	 * // set main window title onSolutionOpen
	 * application.putClientProperty(NGCONSTANTS.WINDOW_BRANDING_TITLE, "My app title");
	 */
	public static final String WINDOW_BRANDING_TITLE = "window.branding.title";

	/**
	 * Client property used to set the icon of the main window. This should be a PNG
	 * image of size 32x32, and it can be the file name that is stored under the web app root
	 * or it can be a base64 encoded image from the solution.
	 *
	 * @sample
	 * // set image from the web app root onSolutionOpen
	 * application.putClientProperty(NGCONSTANTS.WINDOW_BRANDING_ICON_32, "favicon32x32.png");
	 * // set base64 encoded image from solution onSolutionOpen
	 * 	var img = solutionModel.getMedia("favicon32x32.png")
	 *	var imgAsBase64 = new Packages.org.apache.commons.codec.binary.Base64().encodeAsString(img.bytes);
	 *	var imgHref = "data:image/png;base64," + imgAsBase64;
	 *	application.putClientProperty(NGCONSTANTS.WINDOW_BRANDING_ICON_32, imgHref);
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
	 * application.putClientProperty(NGCONSTANTS.FORM_BASED_BROWSER_HISTORY, false);
	 */
	public static final String FORM_BASED_BROWSER_HISTORY = "servoy.ngclient.formbased_browser_history"; //$NON-NLS-1$


	/**
	 * the client property that can be set to always do a like search when filtering over valuelist on a typeahead like component.
	 * So a component that has a valuelist as a property and uses user input to search in that valuelist.
	 * By default it uses a like search with a % at the end (startsWith search on the fields of the valuelist).
	 * But this makes it a like %value% so a contains search.
	 *
	 * This can be set on the element (element.putClientProperty() or on a application wide level (application.putClientProperty())
	 *
	 * DEFAULT: false
	 *
	 * @sample
	 * elements.typeahead.putClientProperty(NGCONSTANTS.VALUELIST_CONTAINS_SEARCH, true);
	 *
	 */
	public static final String VALUELIST_CONTAINS_SEARCH = IApplication.VALUELIST_CONTAINS_SEARCH;

	public String getPrefix()
	{
		return "NGCONSTANTS"; //$NON-NLS-1$
	}
}
