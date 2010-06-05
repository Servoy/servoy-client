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
package com.servoy.j2db.ui;

import java.util.Properties;

import org.apache.wicket.Component;

/**
 * Components which wants to update themselfs with an ajax call should implement this interface.
 * <p>
 * It has a {@link #setChanged()} method for marking the component for render.
 * when calling {@link #setChanged()} on it the component will be re rendered the next time a (ajax) request comes in
 * This can be the ajax polling behavior that every page of a servoy application has if ajax mode is enabled.
 * </p>
 * When setChanged() is called you have to call {@link #setRendered()} when the component is rendered again
 * else it will be re rendered for every coming request. This can be done by calling {@link #setRendered()} from the {@link Component#onAfterRender()}
 * that the wicket component needs to override.
 * <p>
 * NOTE: Try to use the the implementation class {@link ChangesRecorder} 
 * this interface can change with new Servoy versions if new functionality is needed.
 * </p>
 * 
 * @author jcompagner
 * 
 * @since 5.0
 * 
 * @see ChangesRecorder 
 */
public interface IStylePropertyChanges
{
	/**
	 * @return All the current css properties of this component
	 */
	public Properties getChanges();

	/**
	 * Returns true if this change recorder is changed and its component will be rendered the next time.
	 */
	public boolean isChanged();

	/**
	 * Adds all the css properties to the changed set and calls setChanged()
	 * 
	 * @param changes
	 */
	public void setChanges(Properties changes);

	/**
	 *  Call this method from the {@link Component#onBeforeRender()} call te let the  change recorder know it has been rendered.
	 */
	public void setRendered();

	/**
	 * Set the change flag to true so that the component will be rendered the next time.
	 */
	public void setChanged();

	/**
	 * returns true if its component model object is changed 
	 */
	public boolean isValueChanged();

	/**
	 * sets the value changed to true so that servoy knows that it is the value object that is changed.
	 */
	public void setValueChanged();
}
