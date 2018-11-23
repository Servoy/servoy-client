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
 * @author sebster,jblok
 */
public class ChangeHandler
{
	protected IPersistFactory factory;
	protected AbstractRootObject rootObject;

	public ChangeHandler(IPersistFactory factory)
	{
		this.factory = factory;
	}

	void setRootObject(AbstractRootObject rootObject)
	{
		if (this.rootObject != null)
		{
			throw new IllegalArgumentException("root object already set"); //$NON-NLS-1$
		}
		this.rootObject = rootObject;
	}

	/**
	 * Create a repositoy object like Form,fields,portals,beans,etc.
	 *
	 * @param style the style to use
	 * @param parent the parent
	 * @param object_type_id the type
	 * @return the created object
	 */
	public IPersist createNewObject(ISupportChilds parent, int object_type_id) throws RepositoryException
	{
		final UUID uuid = UUID.randomUUID();
		final int element_id = factory.getNewElementID(uuid);
		return createNewObject(parent, object_type_id, element_id, uuid);
	}

	public IPersist createNewObject(ISupportChilds parent, int object_type_id, int element_id, UUID uuid) throws RepositoryException
	{
		// Create object.
		IPersist object = factory.createObject(parent, object_type_id, element_id, uuid);
		rootObject.registerNewObject(object);
		return object;
	}

	IPersist cloneObj(IPersist objToClone, ISupportChilds newParent, boolean flattenOverrides) throws RepositoryException
	{
		if (newParent == null) newParent = objToClone.getParent();//if null use current

		IPersist clone = createNewObject(newParent, objToClone.getTypeID());
		factory.initClone(clone, objToClone, flattenOverrides);
		newParent.addChild(clone);
		return clone;
	}

	public boolean isLatestRelease() throws RepositoryException
	{
		return true;
	}

	public void addIPersistListener(IItemChangeListener<IPersist> listener)
	{
		PersistChangeHandler.getInstance().add(this, listener);
	}

	public void removeIPersistListener(IItemChangeListener<IPersist> listener)
	{
		PersistChangeHandler.getInstance().remove(this, listener);
	}

	protected void fireIPersistCreated(IPersist persist)
	{
		PersistChangeHandler.getInstance().fireItemCreated(this, persist);
	}

	protected void fireIPersistRemoved(IPersist persist)
	{
		PersistChangeHandler.getInstance().fireItemRemoved(this, persist);
	}

	public void fireIPersistChanged(IPersist persist)
	{
		PersistChangeHandler.getInstance().fireItemChanged(this, persist);

		if (persist.getParent() instanceof ISupportChilds)
		{
			// also let the parent know this one is changed. above doesn't have to be called..
			fireIPersistChanged(persist.getParent());
		}
	}

	public void rootObjectIsFlushed()
	{
	}
}
