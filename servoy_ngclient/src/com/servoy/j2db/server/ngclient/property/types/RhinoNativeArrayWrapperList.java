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

import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ChangeAwareList.IAttachAware;
import org.sablo.specification.property.ChangeAwareList.IAttachHandler;
import org.sablo.specification.property.ConvertedList;
import org.sablo.specification.property.IWrappedBaseListProvider;
import org.sablo.specification.property.IWrapperType;
import org.sablo.specification.property.WrappingContext;

/**
 * This list is able to act as a Sablo wrap-aware map that is based on a (native or otherwise) Rhino JS array value.
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
 * We need then the window.popupMenus property to really be based on the javascript underlying JS Rhino array, not on a copy made on one point in time of it so
 * that changing previously kept references to subtrees of it will be reflected in Java side as well.
 * <br><br>
 * This list behaves a bit strange as it will not keep an array of Wrapped sablo values, but it will keep a JS Rhino array based conversion array and when a wrapped sablo value is needed
 * is needed it will offer another converted list that converts from the sablo value (that is converted from Rhino) to wrapped sablo values on the fly.
 *
 * @author acostescu
 * @param <SabloT> the Sablo value type
 * @param <SabloWT> the Sablo wrapped value type
 */
public class RhinoNativeArrayWrapperList<SabloT, SabloWT> extends ConvertedList<SabloT, Object>
	implements IWrappedBaseListProvider, IRhinoNativeProxy, IAttachAware<SabloWT>
{

	protected Map<Integer, SabloT> previousValues;
	protected BaseWebObject componentOrService;
	protected final PropertyDescription elementTypeDefinition;
	protected final Scriptable rhinoScriptable;
	protected ConvertedList<SabloWT, SabloT> sabloWrappedBaseList;
	protected IAttachHandler<SabloWT> attachHandler;

	public RhinoNativeArrayWrapperList(NativeArray rhinoArray, PropertyDescription elementTypeDefinition, List<SabloT> previousComponentValue,
		BaseWebObject componentOrService)
	{
		this(new NativeArrayProxyList<Object>(rhinoArray), elementTypeDefinition, previousComponentValue, componentOrService, rhinoArray);
	}

	public Scriptable getBaseRhinoScriptable()
	{
		return rhinoScriptable;
	}

	@Override
	public void setAttachHandler(IAttachHandler<SabloWT> attachHandler)
	{
		this.attachHandler = attachHandler;
	}

	public RhinoNativeArrayWrapperList(List<Object> rhinoBasedList, PropertyDescription elementTypeDefinition, List<SabloT> previousComponentValue,
		BaseWebObject componentOrService, Scriptable rhinoScriptable)
	{
		super(rhinoBasedList);
		this.previousValues = new HashMap<Integer, SabloT>();
		if (previousComponentValue != null)
		{
			for (int i = previousComponentValue.size() - 1; i >= 0; i--)
				this.previousValues.put(Integer.valueOf(i), previousComponentValue.get(i));
		}
		this.componentOrService = componentOrService;
		this.elementTypeDefinition = elementTypeDefinition;
		this.rhinoScriptable = rhinoScriptable;
	}

	@Override
	protected SabloT convertFromBase(int i, Object value)
	{
		SabloT old = (previousValues != null && previousValues.size() > i) ? previousValues.get(Integer.valueOf(i)) : null;
		SabloT v = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, old, elementTypeDefinition, componentOrService);

		// previousComponentValue.get(i) might have been null if native JS Rhino objects/arrays were set there directly
		// and the parent was also a native object that had previously been already attached to a component (but used via the initial Rhino reference not the Rhino wrapper
		// so it doesn't trigger anything); in this case getting it now will actually create the ChangeAware instance - which needs to be attached to the component
		previousValues.put(Integer.valueOf(i), v);
		if (old != v)
		{
			if (attachHandler != null)
			{
				attachHandler.detachFromBaseObjectIfNeeded(i, wrap(old));
				attachHandler.attachToBaseObjectIfNeeded(i, wrap(v));
			}
		}
		return v;
	}

	@Override
	protected Object convertToBase(int i, SabloT value)
	{
		return NGConversions.INSTANCE.convertSabloComponentToRhinoValue(value, elementTypeDefinition, componentOrService, rhinoScriptable);
	}

	public List<SabloWT> getWrappedBaseList()
	{
		if (!(elementTypeDefinition.getType() instanceof IWrapperType< ? , ? >)) return (List<SabloWT>)this; // sablo type == wrapped type, no wrapping will happen

		if (sabloWrappedBaseList == null)
		{
			// here in this converted map, the "base" is "sablo type" and the external is the "sablo wrapped type"; the other way around then when storage is java maps or arrays
			sabloWrappedBaseList = new ConvertedList<SabloWT, SabloT>(this)
			{

				@Override
				protected SabloWT convertFromBase(int i, SabloT value)
				{
					return wrap(value);
				}

				@Override
				protected SabloT convertToBase(int i, SabloWT value)
				{
					IWrapperType<SabloT, SabloWT> wt = (IWrapperType<SabloT, SabloWT>)elementTypeDefinition.getType();
					return wt.unwrap(value);
				}

			};
		}
		return sabloWrappedBaseList;
	}

	protected SabloWT wrap(SabloT value)
	{
		if (elementTypeDefinition.getType() instanceof IWrapperType)
		{
			IWrapperType<SabloT, SabloWT> wt = (IWrapperType<SabloT, SabloWT>)elementTypeDefinition.getType();
			return wt.wrap(value, null /* we never store the wrapped value here... */, elementTypeDefinition,
				new WrappingContext(componentOrService, elementTypeDefinition.getName()));
		}
		else return (SabloWT)value;
	}

	/**
	 * This is a proxy List to the underlying Rhino NativeArray. It works directly with scriptable values, doesn't take into consideration
	 * Rhino wrap/unwrap operations and NativeArray (that itself implements List) in it's List impl.
	 *
	 * @author acostescu
	 */
	public static class NativeArrayProxyList<T> extends AbstractList<T>
	{

		private final NativeArray nativeArray;

		public NativeArrayProxyList(NativeArray nativeArray)
		{
			this.nativeArray = nativeArray;
		}

		protected Object executeNativeFunc(String functionName, Object[] args)
		{
			Context cx = Context.enter();
			try
			{
				return ScriptableObject.callMethod(cx, nativeArray, functionName, args);
			}
			finally
			{
				Context.exit();
			}
		}

		@Override
		public T get(int index)
		{
			Context.enter();
			try
			{
				return (T)nativeArray.get(index, nativeArray);
			}
			finally
			{
				Context.exit();
			}
		}

		@Override
		public T set(int index, T element)
		{
			Context.enter();
			try
			{
				T old = size() > index ? (T)nativeArray.get(index, nativeArray) : null;
				nativeArray.put(index, nativeArray, element);
				return old;
			}
			finally
			{
				Context.exit();
			}
		}

		@Override
		public boolean add(T e)
		{
			int oldSize = size();
			executeNativeFunc("push", new Object[] { e });
			return oldSize != size();
		}

		@Override
		public T remove(int index)
		{
			T oldValue = size() > index ? get(index) : null;
			executeNativeFunc("splice", new Object[] { Integer.valueOf(index), Integer.valueOf(1) });
			return oldValue;
		}

		@Override
		public int size()
		{
			return nativeArray.size();
		}

	}

}
