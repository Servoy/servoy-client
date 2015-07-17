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

import java.util.Iterator;

import com.servoy.j2db.util.UUID;


/**
 * Interface for consistency let the repository delete the child if it has deleted from db or a object which is created via cloning to inform the parent about
 * his presence.<br>
 * mainly used to keep the object structure consistent
 *
 * @author jblok, jcompagner
 */
public interface ISupportChilds extends IPersist
{

	Iterator<IPersist> getAllObjects();

	<T extends IPersist> Iterator<T> getObjects(int type) throws RepositoryException;

	IPersist getChild(UUID childUuid);

	/**
	 * Never call these methods directly, use obj.createXYZ() and obj.delete() instead
	 */
	void addChild(IPersist child);

	/**
	 * Only changes the in-mem model, does not do actual deletes anywhere else!
	 */
	void removeChild(IPersist child);
}
