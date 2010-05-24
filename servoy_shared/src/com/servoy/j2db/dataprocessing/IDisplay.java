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
package com.servoy.j2db.dataprocessing;


/**
 * Interface for components that have some additional functionality.<br>
 * It is meant for components that are linked to dataProviders.
 * 
 * @author Jan Blok
 */
public interface IDisplay
{

	/**
	 * Displays usually have special behavior in find mode - most of the times disabling validation & becoming editable.<BR>
	 * This is called when entering/exiting find mode.
	 * 
	 * @param mode <b>true</b> if not in find mode (so normal mode) and <b>false</b> if in find mode.
	 */
	public void setValidationEnabled(boolean mode);

	/**
	 * The method gets called when Servoy wants the contents of an display to be committed to it's model and make sure it is not editing content (can be record
	 * change, active form change, save, ...). You can use the method for validation purposes too.<BR>
	 * For example you could have a text field in which the user typed something, and when a method executes you would want that content to be available to the
	 * script although you did not press enter/tab out of that field.
	 * 
	 * @param looseFocus if true perform any special action needed for the display to loose focus. Most of the times this is not used. Example - a table like
	 *            display might need to do some "cell editing stop" related tasks.
	 * @return <b>false</b> means the bean refuses to exit edit mode - and the operation that triggered this request will most likely be canceled (depending on
	 *         what triggered it). For example this can deny save, triggering of events...<BR>
	 *         <b>true</b> means the bean is no longer in "edit" state and Servoy can continue with the operation that triggered it.
	 */
	public boolean stopUIEditing(boolean looseFocus);

	/**
	 * Read-only displays are displays that do not let the user alter their displayed value.
	 * 
	 * @return the read-only state of this display.
	 */
	public boolean isReadOnly();

	/**
	 * Enabled displays are displays the user can interact with and are an active part of the UI. Disabled displays are generally grayed-out and the user does
	 * not interact with them.
	 * 
	 * @return the enabled state of this display.
	 */
	public boolean isEnabled();

}
