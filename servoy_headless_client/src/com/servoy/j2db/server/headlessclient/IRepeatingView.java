/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.RepeatingView;

/**
 * @author jcompagner
 *
 */
public interface IRepeatingView
{

	/**
	 * gets a component by the name it was added.
	 * 
	 * @param name
	 * @return The component if found else null.
	 */
	public Component getComponent(String name);

	/**
	 * Add a component that must be rendered,
	 * this is done by a {@link RepeatingView}, the component's markup will be a <div></div>
	 * 
	 * @param name
	 * @param component
	 * @return true if the component was added, false if there was already a component registered under that name 
	 */
	public boolean addComponent(String name, Component component);

	/**
	 * Remove a behaviour
	 * 
	 * @param name the name
	 * @return true if the component was registered and removed, false if under that name no component was found.
	 */
	public boolean removeComponent(String name);

	/**
	 * Generates a unique id string. This makes it easy to add items to be rendered w/out having to
	 * worry about generating unique id strings in your code.
	 * 
	 * @return unique child id
	 */
	public String newChildId();
}
