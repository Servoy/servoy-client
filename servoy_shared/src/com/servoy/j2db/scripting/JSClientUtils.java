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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.INGClientApplication;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * This is a class that is on the root of scripting under "clientutils".
 * This provides some utility functions purely for interaction with the client, like generating urls or client side functions.
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
	 * This generates a browser function for the given function string that can be executed in the browser by a component that needs a function for a certain property value.
	 * The resulting object should be assigned into a config/property object (where the property it typed as 'object'/'json'/'map' in the .spec) that is then assigned to a component.
	 * The component will receive this function as a real function object in TiNG (but still as a plain string that needs to be evalled in NG1).<br/><br/>
	 *
	 * This is needed because in TiNG it is not allowed - due to the Content Security Policy (CSP) that is enforced - to eval(string) in order to get a function object (that then can be executed later on).<br/><br/>
	 *
	 * This is a more dynamic variant of the .spec property type "clientfunction": https://docs.servoy.com/reference/servoy-developer/property_types#clientfunction<br/><br/>
	 *
	 * You do not need to use this for properties/arguments/return values that are declared to have "clientfunction" type in the .spec file, but rather for
	 * when you want to give it inside plain 'object' typed values. Starting with 2023.09, 'map' and 'json' property types (even nested if configured in the spec correctly) are supported.
	 *
	 * @sample
	 * var options = { myfunction: clientutils.generateBrowserFunction("function(param) { return param + 1 }") };
	 * elements.component.options = options;
	 *
	 * @param functionString The javascript function (given as a string) that should be running in the client's browser.
	 *
	 * @return An object that can be assigned to a property of an component or custom type. (but which is then nested/part of an object type)
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
	 * var bloburl1 = clientutils.createUrlBlobloaderBuilder(columnName).serverAndTable("example_data", tableName).rowid(doc_id).filename(file_name).mimetype(mimeType).build();
	 *
	 * // datasource based column
	 * var bloburl2 = clientutils.createUrlBlobloaderBuilder("invoice_doc").datasource("db:/example_data/pdf_documents").rowid(doc_id).build();
	 *
	 * // global var
	 * var bloburl2 = clientutils.createUrlBlobloaderBuilder("scopes.sc1.profilePhoto").filename("profilePhoto.png").mimetype("application/png").build();
	 *
	 * @param dataprovider the dataprovider who's value should be sent to the browser (it can be a global scope variable or a datasource column)
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

	public void destroy()
	{
		this.application = null;
	}
}
