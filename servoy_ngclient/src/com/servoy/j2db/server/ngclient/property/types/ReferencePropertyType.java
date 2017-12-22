/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.sablo.specification.property.types.DefaultPropertyType;

/**
 * @author gboros
 *
 */
public abstract class ReferencePropertyType<T, RT> extends DefaultPropertyType<T>
{
	private final ReferenceQueue<T> garbageCollectedRefQueue = new ReferenceQueue<>();
	private final WeakHashMap<T, RT> refsToIDs = new WeakHashMap<>();

	private final Map<RT, WeakReference<T>> allWeakRefsByID = new HashMap<RT, WeakReference<T>>();
	private final Map<WeakReference<T>, RT> allIDsByWeakRef = new HashMap<WeakReference<T>, RT>();

	protected RT addReference(T ref)
	{
		cleanGarbageCollectedReferences();
		if (ref == null) return null;
		RT refID = refsToIDs.get(ref);
		if (refID == null)
		{
			refID = createUniqueIdentifier(ref);
			WeakReference<T> weakRef = new WeakReference<T>(ref, garbageCollectedRefQueue);
			allWeakRefsByID.put(refID, weakRef);
			refsToIDs.put(ref, refID);
			allIDsByWeakRef.put(weakRef, refID);
		}
		return refID;
	}

	protected abstract RT createUniqueIdentifier(T ref);

	protected T getReference(RT refID)
	{
		cleanGarbageCollectedReferences();
		if (refID != null)
		{
			WeakReference<T> ref = allWeakRefsByID.get(refID);
			return ref != null ? ref.get() : null;
		}
		return null;
	}

	private void cleanGarbageCollectedReferences()
	{
		Reference< ? extends T> ref;
		while ((ref = garbageCollectedRefQueue.poll()) != null)
		{
			RT refId = allIDsByWeakRef.remove(ref);
			allWeakRefsByID.remove(refId);
			// no need to clear here refsToUUIDs, as it is a weak hash-map and when T key is garbage collected it clears itself anyway
		}
	}

}
