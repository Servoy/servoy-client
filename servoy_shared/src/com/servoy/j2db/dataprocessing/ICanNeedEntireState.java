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

package com.servoy.j2db.dataprocessing;

/**
 * Refactored this to be the same methods; before they were defined in IDisplayData as well as partially in IFieldComponent - but completely independently...
 *
 * @author acostescu
 */
// TODO I think these actually could be part of {@link IDisplayTagText} but as IDisplayData doesn't implement that I couldn't do it as part of refactor.
public interface ICanNeedEntireState
{

	/**
	 * This makes a display no longer updateable, needFocusListener should return false used by display which uses multiple values to display information typical
	 * use a dataprovider with a letter which contains %%tags%% where tags are other dataprovider names, the tags are replaced with the values
	 */
	public boolean needEntireState();

	public void setNeedEntireState(boolean needEntireState);

}
