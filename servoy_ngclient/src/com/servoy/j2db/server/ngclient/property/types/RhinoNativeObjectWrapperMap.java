/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.servoy.j2db.server.ngclient.property.types;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ChangeAwareMap.IAttachAware;
import org.sablo.specification.property.ChangeAwareMap.IAttachHandler;
import org.sablo.specification.property.ConvertedMap;
import org.sablo.specification.property.IWrappedBaseMapProvider;
import org.sablo.specification.property.IWrapperType;
import org.sablo.specification.property.WrappingContext;

import com.servoy.j2db.server.ngclient.INGWebObject;

/**
 * This map is able to act as a Sablo wrap-aware map that is based on a native Rhino JS object value.
 * It is required when server side JS code does something like this:
 * <pre>
 * var x = [];
 * window.popupMenus = [{ "name": "a", "items": x } // window is a service or component here
 * x.push(...)
 * </pre>
 *
 * So in this case window.popupMenus when it's assigned can't just rebase all it's nested values on Java maps/lists cause then
 * changing x in JS will have no effect on the mirrored property value (so one could only do window.popupMenus[0].items.push in order for it to work correctly).
 * <br/><br/>
 * We need then the window.popupMenus property to really be based on the javascript underlying NativeObject, not on a copy made on one point in time of it so
 * that changing previously kept references to subtrees of it will be reflected in Java side as well.
 * <br><br>
 * This map behaves a bit strange as it will not keep a map of Wrapped sablo values, but it will keep a NativeObject based conversion map and when a wrapped sablo value
 * is needed it will offer another converted map that converts from the sablo value (that is converted from Rhino) to wrapped sablo values on the fly.
 *
 * @author acostescu
 * @param <SabloT> the Sablo value type
 * @param <SabloWT> the Sablo wrapped value type
 */
//TODO these ET and WT are improper - as for object type they can represent multiple types (a different set for each child key), but they help to avoid some bugs at compile-time
public class RhinoNativeObjectWrapperMap<SabloT, SabloWT> extends ConvertedMap<SabloT, Object>
	implements IWrappedBaseMapProvider, IRhinoNativeProxy, IAttachAware<SabloWT>
{

	protected Map<String, IWrapperType<SabloT, SabloWT>> childPropsThatNeedWrapping;
	protected Map<String, SabloT> previousValues;
	protected INGWebObject componentOrService;
	protected final PropertyDescription customJSONTypeDefinition;
	protected final NativeObject rhinoObject;
	protected ConvertedMap<SabloWT, SabloT> sabloWrappedBaseMap;
	protected IAttachHandler<SabloWT> attachHandler;

	public RhinoNativeObjectWrapperMap(NativeObject rhinoObject, PropertyDescription customJSONTypeDefinition, INGWebObject componentOrService,
		Map<String, IWrapperType<SabloT, SabloWT>> childPropsThatNeedWrapping)
	{
		super(new NativeObjectProxyMap<String, Object>(rhinoObject));
		this.childPropsThatNeedWrapping = childPropsThatNeedWrapping;
		this.previousValues = new HashMap<String, SabloT>();
		this.componentOrService = componentOrService;
		this.customJSONTypeDefinition = customJSONTypeDefinition;
		this.rhinoObject = rhinoObject;
	}

	public Scriptable getBaseRhinoScriptable()
	{
		return rhinoObject;
	}

	@Override
	public void setAttachHandler(IAttachHandler<SabloWT> attachHandler)
	{
		this.attachHandler = attachHandler;
	}

	@Override
	protected SabloT convertFromBase(String key, Object value)
	{
		SabloT old = previousValues != null ? previousValues.get(key) : null;
		SabloT v = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, old, customJSONTypeDefinition.getProperty(key), componentOrService);

		// previousComponentValue.get(key) might have been null if native JS Rhino objects/arrays were set there directly
		// and the parent was also a native object that had previously been already attached to a component (but used via the initial Rhino reference not the Rhino wrapper
		// so it doesn't trigger anything); in this case getting it now will actually create the ChangeAware instance - which needs to be attached to the component
		previousValues.put(key, v);
		if (old != v)
		{
			if (attachHandler != null)
			{
				attachHandler.detachFromBaseObjectIfNeeded(key, wrap(key, old));
				attachHandler.attachToBaseObjectIfNeeded(key, wrap(key, v));
			}
		}
		return v;
	}

	@Override
	protected Object convertToBase(String key, boolean ignoreOldValue, SabloT value)
	{
		return NGConversions.INSTANCE.convertSabloComponentToRhinoValue(value, customJSONTypeDefinition.getProperty(key), componentOrService, rhinoObject);
	}

	public Map<String, SabloWT> getWrappedBaseMap()
	{
		if (childPropsThatNeedWrapping == null || childPropsThatNeedWrapping.size() == 0) return (Map<String, SabloWT>)this; // sablo type == wrapped type, no wrapping will happen

		if (sabloWrappedBaseMap == null)
		{
			// here in this converted map, the "base" is "sablo type" and the external is the "sablo wrapped type"; the other way around then when storage is java maps or arrays
			sabloWrappedBaseMap = new ConvertedMap<SabloWT, SabloT>(this)
			{

				@Override
				protected SabloWT convertFromBase(String forKey, SabloT value)
				{
					return wrap(forKey, value);
				}

				@Override
				protected SabloT convertToBase(String forKey, boolean ignoreOldValue, SabloWT value)
				{
					IWrapperType<SabloT, SabloWT> wt = childPropsThatNeedWrapping.get(forKey);
					return wt != null ? wt.unwrap(value) : (SabloT)value;
				}

			};
		}
		return sabloWrappedBaseMap;
	}

	protected SabloWT wrap(String forKey, SabloT value)
	{
		IWrapperType<SabloT, SabloWT> wt = childPropsThatNeedWrapping.get(forKey);
		return wt != null ? wt.wrap(value, null /* we never store the wrapped value here... */, customJSONTypeDefinition.getProperty(forKey),
			new WrappingContext(componentOrService.getUnderlyingWebObject(), forKey)) : (SabloWT)value;
	}

	/**
	 * This is a proxy Map to the underlying Rhino NativeObject. It works directly with scriptable values, doesn't take into consideration
	 * Rhino wrap/unwrap operations and NativeObject (that itself implements Map) in it's Map impl.
	 *
	 * K and V generic types are not really useful but they help make code more readable. K can be a String or Number, V can be anything.
	 *
	 *
	 * @author acostescu
	 */
	public static class NativeObjectProxyMap<K, V> extends AbstractMap<K, V>
	{

		// TODO improve runtime performance of this map

		private final NativeObject nativeObject;
		protected NativeObjectProxyMap<K, V>.EntrySet entrySet;

		public NativeObjectProxyMap(NativeObject nativeObject)
		{
			this.nativeObject = nativeObject;
		}

		@Override
		public V put(K key, V value)
		{
			Context.enter();
			try
			{
				if (key instanceof String)
				{
					V old = (V)nativeObject.get((String)key, nativeObject);
					nativeObject.put((String)key, nativeObject, value);
					return old;
				}
				else if (key instanceof Number)
				{
					V old = (V)nativeObject.get(((Number)key).intValue(), nativeObject);
					nativeObject.put(((Number)key).intValue(), nativeObject, value);
					return old;
				}
				throw new RuntimeException("JS Object key must be either string or number");
			}
			finally
			{
				Context.exit();
			}
		}

		@Override
		public Set<Map.Entry<K, V>> entrySet()
		{
			Set<Map.Entry<K, V>> es = entrySet;
			return es != null ? es : (entrySet = new EntrySet());
		}

		protected final class EntrySet extends AbstractSet<Map.Entry<K, V>>
		{
			@Override
			public Iterator<Map.Entry<K, V>> iterator()
			{
				return new Iterator<Map.Entry<K, V>>()
				{
					Object[] ids = nativeObject.getIds();
					Object key = null;
					int index = 0;

					public boolean hasNext()
					{
						return index < ids.length;
					}

					public Map.Entry<K, V> next()
					{
						final Object ekey = key = ids[index++];
						final Object value;

						Context.enter();
						try
						{
							if (ekey instanceof String)
							{
								value = nativeObject.get((String)ekey, nativeObject);
							}
							else if (ekey instanceof Number)
							{
								value = nativeObject.get(((Number)ekey).intValue(), nativeObject);
							}
							else throw new RuntimeException("JS Object key must be either String or Number.");
						}
						finally
						{
							Context.exit();
						}

						return new Map.Entry<K, V>()
						{
							public K getKey()
							{
								return (K)ekey;
							}

							public V getValue()
							{
								return (V)value;
							}

							public V setValue(Object v)
							{
								throw new UnsupportedOperationException();
							}

							@Override
							public boolean equals(Object other)
							{
								if (!(other instanceof Map.Entry))
								{
									return false;
								}
								Map.Entry e = (Map.Entry)other;
								return (ekey == null ? e.getKey() == null : ekey.equals(e.getKey())) &&
									(value == null ? e.getValue() == null : value.equals(e.getValue()));
							}

							@Override
							public int hashCode()
							{
								return (ekey == null ? 0 : ekey.hashCode()) ^ (value == null ? 0 : value.hashCode());
							}

							@Override
							public String toString()
							{
								return ekey + "=" + value;
							}
						};
					}

					public void remove()
					{
						if (key == null)
						{
							throw new IllegalStateException();
						}
						nativeObject.remove(key);
						key = null;
					}
				};
			}

			@Override
			public int size()
			{
				return nativeObject.size();
			}

		}

	}

}
