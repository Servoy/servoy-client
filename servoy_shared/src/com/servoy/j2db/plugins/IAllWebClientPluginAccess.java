/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.plugins;

import java.io.File;

/**
 * Extended plugin api for all webclient.
 *
 * @author gboros
 * @since 8.0
 */
public interface IAllWebClientPluginAccess extends IClientPluginAccess
{
	/**
	 * Serve a resource, returns the url under which the resource is served, can be shown with showURL. After that it is served the content will be cleaned.
	 * The "Content-disposition" header parameter is set to "attachment", so the clients will know this resource is for downloading.
	 *
	 * @param filename
	 * @param array the resource as byte array
	 * @param mimetype
	 * @return url
	 */
	public String serveResource(String filename, byte[] array, String mimetype);

	/**
	 * Serve a resource, returns the url under which the resource is served, can be shown with showURL. After that it is served the content will be cleaned.
	 *
	 * @param filename
	 * @param array the resource as byte array
	 * @param mimetype
	 * @param contentDisposition value used for the "Content-disposition" header parameter; this can be "attachment", if the resource is for downloading,
	 * or "inline" if it is for viewing; if null, it is considered to be "attachment"
	 * @return url
	 *
	 * @sample
	 *  boolean hintDownload = true;
	 * 	String url = ((IWebClientPluginAccess)access).serveResource(fileName, data, ImageLoader.getContentType(data, fileName), hintDownload ? "attachment" : "inline");
		((IWebClientPluginAccess)access).showURL(url, hintDownload ? "_self" : "_blank", null, 0, false);
	 */
	public String serveResource(String filename, byte[] array, String mimetype, String contentDisposition);

	/**
	 * Serve a resource, returns the url under which the resource is served, can be shown with showURL.
	 *
	 * @param file
	 * @param mimetype
	 * @param contentDisposition
	 * @return
	 */
	public String serveResource(File file, String mimetype, String contentDisposition);

	/**
	 * Show a url in the browser.
	 *
	 * @param url the url
	 * @param target (_blank,_self, framename)
	 * @param target_options usefull option for new window ('height=200,width=400,status=yes,toolbar=no,menubar=no,location=no')
	 * @return true if successful
	 */
	public boolean showURL(String url, String target, String target_options);

	/**
	 * Show a url in the browser.
	 *
	 * @param url the url
	 * @param target (_blank,_self, framename)
	 * @param target_options usefull option for new window ('height=200,width=400,status=yes,toolbar=no,menubar=no,location=no')
	 * @param timeout_ms time to act in milliseconds
	 * @return true if successful
	 */
	public boolean showURL(String url, String target, String target_options, int timeout_ms);

	/**
	 * Show a url in the browser.
	 *
	 * @param url the url
	 * @param target (_blank,_self, framename)
	 * @param target_options usefull option for new window ('height=200,width=400,status=yes,toolbar=no,menubar=no,location=no')
	 * @param timeout_ms time to act in milliseconds
	 * @param onRootFrame whether or not the URL should be shown in the root Servoy window/iFrame in case of dialogs (iFrame)
	 * @return true if successful
	 */
	public boolean showURL(String url, String target, String target_options, int timeout_ms, boolean onRootFrame);
}
