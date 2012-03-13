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


import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * @author sebster,jblok
 */
public class ChangeHandler
{
	protected IPersistFactory factory;
	protected AbstractRootObject rootObject;
	protected List<IPersistListener> persistListeners;

	public ChangeHandler(IPersistFactory factory)
	{
		this.factory = factory;
		persistListeners = new ArrayList<IPersistListener>();
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
	IPersist createNewObject(ISupportChilds parent, int object_type_id) throws RepositoryException
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

	public void addIPersistListener(IPersistListener listener)
	{
		if (listener != null)
		{
			if (!persistListeners.contains(listener))
			{
				persistListeners.add(listener);
			}
		}
	}

	public void removeIPersistListener(IPersistListener listener)
	{
		if (listener != null)
		{
			persistListeners.remove(listener);
		}
	}

	protected void fireIPersistCreated(IPersist persist)
	{
		for (int i = 0; i < persistListeners.size(); i++)
		{
			try
			{
				persistListeners.get(i).iPersistCreated(persist);
			}
			catch (Exception e)
			{
				Debug.error(e);//an exception should never interupt the process
			}
		}
	}

	protected void fireIPersistRemoved(IPersist persist)
	{
		for (int i = 0; i < persistListeners.size(); i++)
		{
			try
			{
				persistListeners.get(i).iPersistRemoved(persist);
			}
			catch (Exception e)
			{
				Debug.error(e);//an exception should never interupt the process
			}
		}
	}

	public void fireIPersistChanged(IPersist persist)
	{
		for (int i = 0; i < persistListeners.size(); i++)
		{
			try
			{
				persistListeners.get(i).iPersistChanged(persist);
			}
			catch (Exception e)
			{
				Debug.error(e);//an exception should never interupt the process
			}
		}
		if (persist.getParent() instanceof ISupportChilds)
		{
			// also let the parent know this one is changed. above doesn't have to be called..
			fireIPersistChanged(persist.getParent());
		}
	}

	public void clearAllPersistListeners()
	{
		persistListeners = new ArrayList<IPersistListener>();
	}

	public void rootObjectIsFlushed()
	{
	}
}
