/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.j2db.scripting;

import org.json.JSONObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.INGClientApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.ui.runtime.IBaseRuntimeComponent;
import com.servoy.j2db.util.Utils;

/**
 * <p>The <code>clientutils</code> object provides essential utilities for client-side
 * operations in applications, streamlining tasks like generating URLs, managing browser
 * functions, and retrieving interface dimensions. It is designed to enhance interaction
 * with the client environment, ensuring efficiency and flexibility in various scenarios.</p>
 *
 * <p>One key function, <code>createUrlBlobloaderBuilder</code>, enables the creation of
 * downloadable URLs for content, such as database columns or global variables, which can
 * be presented in HTML areas. This allows for flexible handling of files with customizable
 * filenames and MIME types.</p>
 *
 * <p>The <code>generateBrowserFunction</code> method facilitates the secure execution of
 * JavaScript functions on the client side by creating executable function strings. This
 * is particularly useful in environments with stringent Content Security Policies (CSP).</p>
 *
 * <p>Other functions include <code>getBounds</code>, which retrieves the position and size
 * of UI components or their sub-elements, and <code>getMediaURL</code>, which generates
 * URLs for serving media assets. Additionally, the <code>requestFullscreen</code> method
 * enables full-screen display for the HTML document, enhancing user experiences in
 * immersive scenarios.</p>
 *
 * @author jcompagner
 * @since 2024.3.1
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Client Utils", scriptingName = "clientutils")
public class JSClientUtils
{
	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSClientUtils.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return new Class< ? >[] { JSBlobLoaderBuilder.class };
			}
		});
	}

	private volatile IApplication application;

	public JSClientUtils(IApplication application)
	{
		this.application = application;
	}

	/**
	 * This will generate a browser function for the given function string - that can be executed in the browser by a component/service that needs a client side executable function for one of it's properties / method arguments.<br/><br/>
	 *
	 * The resulting object should be assigned to / given as a (config) property / method argument (that is typed as 'object'/'json'/'map' in the .spec).
	 * The component will receive this function as a real function object in TiNG (but it will still be a plain string that needs to be evalled in NG1).<br/><br/>
	 *
	 * This method is needed because in TiNG it is not allowed to eval(string) - due to the Content Security Policy (CSP) that is enforced - in order to get a function object (that then can be executed later on).<br/><br/>
	 *
	 * This is a more dynamic variant of the .spec <a href="https://docs.servoy.com/reference/servoy-developer/property_types#clientfunction">property type "clientfunction"</a>.<br/><br/>
	 *
	 * You do not need to use this for properties/arguments/return values that are declared to have "clientfunction" type in the .spec file, but rather for
	 * when you want to give it inside plain 'object' typed values.<br/><br/>
	 *
	 * Starting with 2023.09, even 'map' and 'json' property types (values nested inside those - if configured in the component/service .spec file correctly) support client functions given as simple strings, without the need to call this method.
	 *
	 * @sample
	 * var options = { myfunction: clientutils.generateBrowserFunction("function(param) { return param + 1 }") };
	 * elements.component.options = options;
	 *
	 * @param functionString The javascript function (given as a string - DON'T USE a javascript function with toString or a String constructor) that should be running in the client's browser.
	 *
	 * @return An object that can be assigned to a property of an component/service or to a method argument - if that is typed as, or is part of something typed as 'object'.
	 */
	@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
	@JSFunction
	public Object generateBrowserFunction(String functionString)
	{
		return application.generateBrowserFunction(functionString);
	}


	/**
	 * Creates a blob loader url that can be sent to the browser so that it can download the value of the given dataprovider.
	 * The dataprovider is mandatory, but also a datasource or server/tablename combination should be given if it points to a database column.<br/><br/>
	 *
	 * The .build() method of the returned builder will return the url that can be sent to the browser inside a piece of html.<br/><br/>
	 *
	 * The blob loader URL can be used in HTMl Areas for example in order to display images stored in the database, or to provide a clickable
	 * download link for that content as a file. In the other situations, the mimetype indicates to the browser what the type of file is and the browser
	 * might use that information in order to open the file in the appropriate application directly. The filename is suggested/given to the user when
	 * saving/downloading the file.
	 *
	 * @sample
	 * // server/table column
	 * var tableName = 'pdf_documents';
	 * var columnName = 'invoice_doc';
	 * var mimeType = 'application/pdf';
	 * var bloburl1 = clientutils.createUrlBlobloaderBuilder(columnName)
	 *                     .serverAndTable("example_data", tableName).rowid(doc_id)
	 *                     .filename(file_name).mimetype(mimeType).build();
	 *
	 * // datasource based column
	 * var bloburl2 = clientutils.createUrlBlobloaderBuilder("invoice_doc")
	 *                     .datasource("db:/example_data/pdf_documents").rowid(doc_id)
	 *                     .build();
	 *
	 * // global var
	 * var bloburl2 = clientutils.createUrlBlobloaderBuilder("scopes.sc1.profilePhoto")
	 *                     .filename("profilePhoto.png").mimetype("application/png")
	 *                     .build();
	 *
	 * @param dataprovider the dataprovider who's value should be sent to the browser (it can be a global scope variable or a datasource column)
	 *
	 * @return A JSBlobLoaderBuilder instance for constructing a blob loader URL.
	 */
	@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
	@JSFunction
	public JSBlobLoaderBuilder createUrlBlobloaderBuilder(String dataprovider)
	{
		return application.createUrlBlobloaderBuilder(dataprovider);
	}

	/**
	 * Get the media url that can be used to server a media in NGClient.
	 *
	 * @sample
	 * clienutils.getMediaURL('solution.css');
	 *
	 * @param mediaName Name of the media
	 *
	 * @return The media URL as a string, or null if the application is not an NGClient application.
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public String getMediaURL(String mediaName)
	{
		if (application instanceof INGClientApplication)
		{
			return ((INGClientApplication)application).getMediaURL(mediaName);
		}
		return null;
	}

	/**
	* This method is making the HTML document to be displayed in full screen mode.
	*
	* @sample
	* clientutils.requestFullscreen();
	*/
	@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
	@JSFunction
	public void requestFullscreen()
	{
		application.getRuntimeWindowManager().getCurrentWindow().requestFullscreen();
	}

	/**
	* Retrieves the screen location and size of a specific element. Returns the bounds (object with x, y, width and height properties).
	*
	* @sample
	* var bounds = clientutils.getBounds(elements.myelement);
	*
	* @param webComponent the component
	*
	* @return A JSBounds object representing the screen location and size of the specified component, or null if unavailable.
	*/
	@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
	@JSFunction
	public JSBounds getBounds(IBaseRuntimeComponent webComponent)
	{
		return getBounds(webComponent, null);
	}

	/**
	* Retrieves the screen location and size of a specific child element. Returns the bounds (object with x, y, width and height properties).
	*
	* @sample
	*  var bounds = clientutils.getBounds(elements.myelement,'.subclass');
	*
	*  @param webComponent the parent component
	*  @param subselector a selector to identify a child component starting with parent component
	*
	*  @return A JSBounds object representing the screen location and size of the specified child component, or null if unavailable.
	*/
	@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
	@JSFunction
	public JSBounds getBounds(IBaseRuntimeComponent webComponent, String subselector)
	{
		if (application instanceof INGClientApplication && webComponent instanceof Scriptable)
		{
			JSONObject bounds = ((INGClientApplication)application).getBounds((String)((Scriptable)webComponent).get("svyMarkupId", (Scriptable)webComponent),
				subselector);
			if (bounds != null)
			{
				return new JSBounds(Utils.getAsInteger(bounds.get("x")), Utils.getAsInteger(bounds.get("y")), Utils.getAsInteger(bounds.get("width")),
					Utils.getAsInteger(bounds.get("height")));
			}
		}
		return null;
	}

	/**
	* Will return the user agent string of the browser.
	*
	* @sample
	*  var useragent = clientutils.getUserAgent();
	*  var mobile = /iPhone|iPad|iPod|Android/.test(userAgent);
	*
	*  @return A String object representing the useragent of the browser.
	*/
	@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
	@JSFunction
	public String getUserAgent()
	{
		if (application instanceof INGClientApplication ng)
		{
			JSONObject userAgentAndPlatform = ng.getUserAgentAndPlatform();
			if (userAgentAndPlatform != null)
			{
				return userAgentAndPlatform.optString("userAgent"); //$NON-NLS-1$
			}
		}
		return null;
	}

	public void destroy()
	{
		this.application = null;
	}
}
