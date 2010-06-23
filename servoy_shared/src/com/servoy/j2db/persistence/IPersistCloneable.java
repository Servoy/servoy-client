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
package com.servoy.j2db.persistence;


/**
 * Special case of cloneable for solution model
 * @author jblok
 */
public interface IPersistCloneable extends Cloneable
{
	/**
	 * Interface for cloning repository objects
	 * 
	 * @param parent if parent is null, current parent is used,if parent supplied, it will create the clone under that parent
	 * @param deep is specified, does deep clone if possible
	 * @param validator needed to change name, if names must stay unueqe
	 * @param changeChildNames specifies whether or not child names should be changed when cloning
	 * @param changeName specifies whether or not this persist name should be changed when cloning
	 */
	public IPersist cloneObj(ISupportChilds parent, boolean deep, IValidateName validator, boolean changeName, boolean changeChildNames)
		throws RepositoryException;
}
