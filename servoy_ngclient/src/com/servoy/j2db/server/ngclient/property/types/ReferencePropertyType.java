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

import org.sablo.BaseWebObject;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.types.DefaultPropertyType;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.server.ngclient.IContextProvider;

/**
 * @author gboros
 */
@SuppressWarnings("nls")
public abstract class ReferencePropertyType<T, RT> extends DefaultPropertyType<T>
{

	private static String WEAK_REFS_PER_CLIENT_FOR_PROPERTY_TYPE = "weakRefsPerClientForPT";

	/**
	 * {@link ReferencePropertyType} uses this to keep references separated between running clients - so that you can't end up with one client's refs
	 * inside another client.
	 */
	private static class WeakRefs<T, RT>
	{

		private final ReferenceQueue<T> garbageCollectedRefQueue = new ReferenceQueue<>();
		private final WeakHashMap<T, RT> refsToIDs = new WeakHashMap<>();

		private final Map<RT, WeakReference<T>> allWeakRefsByID = new HashMap<RT, WeakReference<T>>();
		private final Map<WeakReference<T>, RT> allIDsByWeakRef = new HashMap<WeakReference<T>, RT>();

	}

	// this is just a fallback and should never be used I think; a ref property type trying to work without an application? maybe before an app is available? template values?
	private WeakRefs<T, RT> globalWeakRefsForPT;

	@SuppressWarnings("unchecked")
	private WeakRefs<T, RT> getWeakRefs(IServiceProvider app)
	{
		Map<Object, Object> applicationRuntimeProperties = (app != null ? app.getRuntimeProperties() : null);

		WeakRefs<T, RT> weakRefsToUse = null;
		if (applicationRuntimeProperties != null)
		{
			weakRefsToUse = ((WeakRefs<T, RT>)applicationRuntimeProperties.get(WEAK_REFS_PER_CLIENT_FOR_PROPERTY_TYPE));
			if (weakRefsToUse == null)
			{
				weakRefsToUse = new WeakRefs<T, RT>();
				applicationRuntimeProperties.put(WEAK_REFS_PER_CLIENT_FOR_PROPERTY_TYPE, weakRefsToUse);
			}
		}

		if (weakRefsToUse == null)
		{
			// this should never happen I think; a ref property type trying to work without an application? maybe somehow before an app is available?
			if (globalWeakRefsForPT == null) globalWeakRefsForPT = new WeakRefs<T, RT>();
			weakRefsToUse = globalWeakRefsForPT;
		}

		return weakRefsToUse;
	}

	private IServiceProvider getApplication(IBrowserConverterContext converterContext)
	{
		// we try to keep weak refs separate per client - in the client's runtime properties map
		BaseWebObject webObject = (converterContext != null ? converterContext.getWebObject() : null);

		IServiceProvider app = null;
		if (webObject instanceof IContextProvider)
			// webObject is probably a WebFromComponent or ServoyClientService
			app = ((IContextProvider)webObject).getDataConverterContext().getApplication();
		if (app == null) app = J2DBGlobals.getServiceProvider();
		return app;

	}

	protected RT addReference(T ref, IBrowserConverterContext converterContext)
	{
		WeakRefs<T, RT> weakRefsToUse = getWeakRefs(getApplication(converterContext));

		cleanGarbageCollectedReferences(weakRefsToUse);
		if (ref == null) return null;
		RT refID = weakRefsToUse.refsToIDs.get(ref);
		if (refID == null)
		{
			refID = createUniqueIdentifier(ref);
		}
		else
		{
			WeakReference<T> weakReference = weakRefsToUse.allWeakRefsByID.get(refID);
			if (weakReference != null) weakReference.clear();
		}
		WeakReference<T> weakRef = new WeakReference<T>(ref, weakRefsToUse.garbageCollectedRefQueue);
		weakRefsToUse.allWeakRefsByID.put(refID, weakRef);
		weakRefsToUse.refsToIDs.put(ref, refID);
		weakRefsToUse.allIDsByWeakRef.put(weakRef, refID);
		return refID;
	}

	protected abstract RT createUniqueIdentifier(T ref);

	public T getReference(RT refID, IServiceProvider serviceProvider)
	{
		WeakRefs<T, RT> weakRefsToUse = getWeakRefs(serviceProvider);

		cleanGarbageCollectedReferences(weakRefsToUse);
		if (refID != null)
		{
			WeakReference<T> ref = weakRefsToUse.allWeakRefsByID.get(refID);
			return ref != null ? ref.get() : null;
		}
		return null;
	}

	protected T getReference(RT refID, IBrowserConverterContext converterContext)
	{
		return getReference(refID, getApplication(converterContext));
	}

	private void cleanGarbageCollectedReferences(WeakRefs<T, RT> weakRefsToUse)
	{
		Reference< ? extends T> ref;
		while ((ref = weakRefsToUse.garbageCollectedRefQueue.poll()) != null)
		{
			RT refId = weakRefsToUse.allIDsByWeakRef.remove(ref);
			weakRefsToUse.allWeakRefsByID.remove(refId);
			// no need to clear here refsToUUIDs, as it is a weak hash-map and when T key is garbage collected it clears itself anyway
		}
	}

}
