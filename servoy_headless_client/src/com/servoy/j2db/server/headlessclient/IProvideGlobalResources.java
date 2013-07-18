/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.util.List;

import org.apache.wicket.ResourceReference;

/**
 * Interface to provide access to global resources(js, css) for a client.
 *  
 * @author lvostinar
 * @since 7.3
 * 
 */
public interface IProvideGlobalResources
{
	/**
	 * Add a js file reference in all html pages.
	 * 
	 * @param resource Reference to js file.
	 */
	public void addGlobalJSResourceReference(ResourceReference resource);

	/**
	 * Add a css file reference in all html pages.
	 * 
	 * @param resource Reference to css file.
	 */
	public void addGlobalCSSResourceReference(ResourceReference resource);

	/**
	 * Add a js file reference in all html pages.
	 * 
	 * @param url url to resource (can be media url)
	 */
	public void addGlobalJSResourceReference(String url);

	/**
	 * Add a css file reference in all html pages.
	 * 
	 * @param url url to resource (can be media url)
	 */
	public void addGlobalCSSResourceReference(String url);

	/**
	 * Remove a previously added resource from the page. 
	 * 
	 * @param resource Resource to be removed.
	 */
	public void removeGlobalResourceReference(ResourceReference resource);

	/**
	 * Remove a previously added resource from the page. 
	 * 
	 * @param url url to be removed.
	 */
	public void removeGlobalResourceReference(String url);

	/**
	 * 	Gets all defined css global resources (ResourceReference or url).
	 */
	public List<Object> getGlobalCSSResources();

	/**
	 * 	Gets all defined js global resources (ResourceReference or url).
	 */
	public List<Object> getGlobalJSResources();
}
