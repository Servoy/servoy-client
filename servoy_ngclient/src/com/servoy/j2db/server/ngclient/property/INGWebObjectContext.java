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

import org.sablo.IWebObjectContext;

import com.servoy.j2db.server.ngclient.IDataAdapterList;

/**
 * A web object context that is able to give NG/Servoy specific context as well.
 *
 * @author acostescu
 */
public interface INGWebObjectContext extends IWebObjectContext
{

	/**
	 * Gives the DataAdapterList that should be used.<br/>
	 * Some properties might wrap other properties while giving them a different DAL to work with. For example {@link FoundsetLinkedPropertyType}.<br/><br/>
	 *
	 * Note that the dataAdapterList is null in case of ng/sablo services. So this is only relevant for when properties are attached to components.
	 *
	 * @return the dal.
	 */
	IDataAdapterList getDataAdapterList();

}
