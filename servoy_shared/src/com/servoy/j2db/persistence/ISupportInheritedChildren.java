/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.servoy.j2db.util.JSONWrapperList;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;

/**
 * @author emera
 */
public interface ISupportInheritedChildren extends ISupportChilds, ISupportExtendsID
{
	Optional<Set<ISupportInheritedChildren>> getListeners();

	default int indexOf(IPersist singleSelection)
	{
		CopyOnWriteArrayList<String> uuids = getSortedChildren();
		return uuids != null ? uuids.indexOf(singleSelection.getUUID().toString()) : -1;
	}

	default void addSuperListener(ISupportInheritedChildren listener)
	{
		if (getListeners().isPresent() && getListeners().get().contains(listener)) return;
		if (getExtendsID() > 0 && !isListeningToParent())
		{
			setListeningToParent(true);
			ISupportChilds realParent = getRealParent();
			if (realParent instanceof ISupportInheritedChildren)
			{
				((ISupportInheritedChildren)realParent).addSuperListener(this);
			}
		}
	}

	default boolean removeSuperListener(ISupportInheritedChildren listener)
	{
		return getListeners().isPresent() ? getListeners().get().remove(listener) : false;
	}

	default void insertBeforeUUID(UUID uuid, Object nextSibling)
	{
		if (getExtendsID() < 0) return;
		CopyOnWriteArrayList<String> uuids = getSortedChildren();
		IPersist next = nextSibling instanceof IPersist ? (IPersist)nextSibling : null;
		Optional<IPersist> overrideOptional = Optional.empty();
		if (uuids != null)
		{
			String _uuid = uuid.toString();
			String nextUUID = nextSibling instanceof IPersist ? next.getUUID().toString() : (String)nextSibling;
			if (nextSibling != null)
			{
				if (uuids.contains(_uuid)) uuids.remove(_uuid); //reorder operation
				int index = uuids.indexOf(nextUUID);
				if (index == -1 && next != null)
				{
					overrideOptional = findOverride(next);
					if (overrideOptional.isPresent())
					{
						index = uuids.indexOf(overrideOptional.get().getUUID().toString());
					}
				}
				uuids.add(index, _uuid);
			}
			else if (!uuids.contains(_uuid))
			{
				uuids.add(_uuid);
			}
			putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_CHILDREN_UUIDS }, uuids);
		}
		Object nxt = overrideOptional.isPresent() ? overrideOptional.get() : nextSibling;
		getListeners().ifPresent(listeners -> listeners.forEach(l -> l.insertBeforeUUID(uuid, nxt)));
	}

	/**
	 *
	 * This is a helper method, do not implement.
	 * @param next
	 * @return
	 */
	//TODO move to PersistHelper?
	default Optional<IPersist> findOverride(IPersist next)
	{
		Optional<IPersist> overrideOptional;
		overrideOptional = ((Form)getAncestor(IRepository.FORMS)).getAllObjectsAsList().stream().filter(p -> (p instanceof ISupportExtendsID) &&
			((ISupportExtendsID)p).getExtendsID() > 0).filter(p -> next.equals(PersistHelper.getSuperPersist((ISupportExtendsID)p))).findAny();
		return overrideOptional;
	}

	default void removeUUID(IPersist persist)
	{
		CopyOnWriteArrayList<String> uuids = getSortedChildren();
		IPersist superPersist = null;
		if (uuids != null)
		{
			String _uuid = persist.getUUID().toString();

			int index = uuids.indexOf(_uuid);
			if (index == -1)
			{
				//next sibling was overridden in child form
				if (((ISupportExtendsID)persist).getExtendsID() > 0)
				{
					Optional<IPersist> overrideOptional = findOverride(persist);
					if (overrideOptional.isPresent())
					{
						index = uuids.indexOf(overrideOptional.get().getUUID().toString());
					}
				}
			}
			uuids.remove(index);
			putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_CHILDREN_UUIDS }, uuids);
		}

		IPersist toRemove = superPersist != null ? superPersist : persist;
		getListeners().ifPresent(listeners -> listeners.forEach(l -> l.removeUUID(toRemove)));
	}

	default void updateLocation(UUID uuid)
	{
		if (getExtendsID() < 0) return;
		CopyOnWriteArrayList<String> uuids = getSortedChildren();
		if (uuids != null && uuids.contains(uuid.toString()))
		{
			List<IPersist> all = PersistHelper.getHierarchyChildren(this);
			Collections.sort(all, PositionComparator.XY_PERSIST_COMPARATOR);
			List<String> currentUUIDS = all.stream().map(p -> p.getUUID().toString()).collect(Collectors.toList());
			int newLocation = currentUUIDS.indexOf(uuid.toString());
			if (newLocation < 0 || newLocation >= uuids.size())
			{
				//TODO log?
				return;
			}
			int oldIndex = uuids.indexOf(uuid.toString());
			if (oldIndex > 0 && oldIndex < newLocation)
			{
				newLocation -= 1;
			}
			uuids.remove(uuid.toString());
			uuids.add(newLocation, uuid.toString());
		}
	}

	default CopyOnWriteArrayList<String> getSortedChildren()
	{
		Object value = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_CHILDREN_UUIDS });
		if (value != null)
		{
			//TODO THIS IS WEIRD - sometimes it is jsonwrapper list and sometimes is CopyOnWriteArrayList
			if (value instanceof JSONWrapperList)
			{
				JSONWrapperList list = (JSONWrapperList)value;
				return list.stream().map(e -> (String)e).collect(Collectors.toCollection(CopyOnWriteArrayList<String>::new));
			}
			else if (value instanceof CopyOnWriteArrayList< ? >)
			{
				return (CopyOnWriteArrayList<String>)value;
			}
		}
		return null;
	}

	void setListeningToParent(boolean listening);

	boolean isListeningToParent();

	Object getCustomProperty(String[] strings);

	Object putCustomProperty(String[] path, Object value);
}
