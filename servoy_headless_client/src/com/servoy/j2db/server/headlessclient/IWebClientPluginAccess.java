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
package com.servoy.j2db.server.headlessclient;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.servoy.j2db.plugins.IClientPluginAccess;

/**
 * Extended plugin api for the webclient.
 * 
 * @author jblok
 * @since 3.5
 */
public interface IWebClientPluginAccess extends IClientPluginAccess
{
	/**
	 * Server a resource, returns the url under which the resource is served, can be shown with showURL. After that it is served the content will be cleaned.
	 * 
	 * @param filename
	 * @param array the resource as byte array
	 * @param mimetype
	 * @return url
	 */
	public String serveResource(String filename, byte[] array, String mimetype);

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

	/**
	 * Get the page contributor
	 * 
	 * @return the page contributor
	 */
	public IPageContributor getPageContributor();

	/**
	 * Ajax Behaviour helper for behaviours added to the page via the page contributor
	 * 
	 * @param target
	 */
	public void generateAjaxResponse(AjaxRequestTarget target);

}
