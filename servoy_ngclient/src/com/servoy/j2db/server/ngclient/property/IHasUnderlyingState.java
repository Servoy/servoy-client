/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import org.sablo.IChangeListener;


/**
 * A sablo value that has internal state that can change and affect the outside world (so not only it's value as a reference can affect the outside world).<br/>
 * Usually this means that it is based on (wraps) another value internally but this is not always the case.
 *
 * The underlying value or state (that is really meaningful) can change for various reasons while this value by reference remains the same. (or it can get initialized later)
 *
 * The idea is the other 'smart' property values that depend on this one might want to know when this value is fully operational or not.
 *
 * @author acostescu
 */
public interface IHasUnderlyingState
{

	/**
	 * Adds a listener interested in knowing when the underlying value is completely changes (due to a replace from Rhino, a init, ...)
	 * If the listener is already present it will not be added again.
	 *
	 * @param stateChangeListener the listener to be added.
	 */
	void addStateChangeListener(IChangeListener stateChangeListener);

	void removeStateChangeListener(IChangeListener stateChangeListener);

}