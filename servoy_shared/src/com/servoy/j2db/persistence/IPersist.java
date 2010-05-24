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


import java.io.Serializable;

import com.servoy.j2db.util.UUID;

/**
 * Interface to enforce objects to be workable for the repository
 * 
 * @author jblok
 */
public interface IPersist extends Serializable
{
	/**
	 * Accept a visitor recursively.
	 * 
	 * @returns null when continueTraversal
	 */
	public Object acceptVisitor(IPersistVisitor visitor);

	/**
	 * Accept a visitor recursively, depth-first.
	 * 
	 * @returns null when continueTraversal
	 */
	public Object acceptVisitorDepthFirst(IPersistVisitor visitor) throws RepositoryException;

	/**
	 * Get the Id from a (repository) object
	 * 
	 * @return the Id
	 */
	public int getID();

	/**
	 * Set revision number
	 * 
	 * @param revision the revision number
	 */
	public void setRevisionNumber(int revision);

	/**
	 * Get the revision from an (repository) object
	 * 
	 * @return the revision
	 */
	public int getRevisionNumber();

	/**
	 * Get the type from a (repository) object
	 * 
	 * @return the type,should return a final object_type from the Repository class
	 */
	public int getTypeID();

	/**
	 * See if this (repository) object is changed
	 * 
	 * @return the state
	 */
	public boolean isChanged();

	/**
	 * Flag this object as changed.
	 */
	public void flagChanged();

	/**
	 * clears the changed flag.
	 */
	public void clearChanged();

	/**
	 * Get the root (repository) object this object belongs to
	 * 
	 * @return the Solution
	 */
	public IRootObject getRootObject();

	/**
	 * Get the parent from a (repository) object
	 * 
	 * @return the parent
	 */
	public ISupportChilds getParent();

	/**
	 * Find the first ancestor with the specified object type, starting with self, null if none found
	 * 
	 * @return the ancestor
	 */
	public IPersist getAncestor(int typeId);

	/**
	 * Returns the UUID
	 * 
	 * @return the UUID
	 */
	public UUID getUUID();

	public MetaData getMetaData();
}
