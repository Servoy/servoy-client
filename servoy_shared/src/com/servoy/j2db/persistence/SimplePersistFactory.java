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

import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 * 
 */
public class SimplePersistFactory extends AbstractPersistFactory
{
	private int last_element_id;

	public SimplePersistFactory(int startElementId)
	{
		last_element_id = startElementId;
	}

	public Solution createDummyCopy(Solution sol)
	{
		Solution copy = new Solution(sol.getRepository(), sol.getSolutionMetaData());
		copy.setReleaseNumber(sol.getReleaseNumber());
		copy.setRevisionNumber(sol.getRevisionNumber());
		return copy;
	}

	/**
	 * @see com.servoy.j2db.persistence.AbstractPersistFactory#createRootObject(int)
	 */
	@Override
	protected IPersist createRootObject(int elementId) throws RepositoryException
	{
		throw new RepositoryException("creating root objects not supported with the SimplePersistFactory"); //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.persistence.AbstractPersistFactory#initClone(com.servoy.j2db.persistence.IPersist, com.servoy.j2db.persistence.IPersist)
	 */
	@Override
	public void initClone(IPersist clone, IPersist objToClone) throws RepositoryException
	{
		throw new RepositoryException("cloning not supported with the SimplePersistFactory"); //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.persistence.AbstractPersistFactory#loadContentSpec()
	 */
	@Override
	protected ContentSpec loadContentSpec() throws RepositoryException
	{
		return StaticContentSpecLoader.getContentSpec();
	}

	/**
	 * @see com.servoy.j2db.persistence.AbstractPersistFactory#resolveIdForElementUuid(com.servoy.j2db.util.UUID)
	 */
	@Override
	public int resolveIdForElementUuid(UUID id) throws RepositoryException
	{
		return getNewElementID(id);
	}

	/**
	 * @see com.servoy.j2db.persistence.AbstractPersistFactory#resolveUUIDForElementId(int)
	 */
	@Override
	public UUID resolveUUIDForElementId(int id) throws RepositoryException
	{
		return uuid_element_id_map.getKey(new Integer(id));
	}

	/**
	 * @see com.servoy.j2db.persistence.IPersistFactory#getNewElementID(com.servoy.j2db.util.UUID)
	 */
	public int getNewElementID(UUID new_uuid) throws RepositoryException
	{
		int element_id = ++last_element_id;
		if (new_uuid != null) uuid_element_id_map.put(new_uuid, new Integer(element_id));
		return element_id;
	}

}
