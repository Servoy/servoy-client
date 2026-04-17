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
package com.servoy.j2db.scripting;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.BeanProperty;
import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.ClassCache.CacheKey;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.JavaMembers_jdk11;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

import com.servoy.j2db.scripting.annotations.AnnotationManagerReflection;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.keyword.Ident;

/**
 * @author jcompagner
 */
public class InstanceJavaMembers extends JavaMembers_jdk11
{
	private List<String> gettersAndSettersToHide;

	/**
	 * Constructor for InstanceJavaMembers.
	 *
	 * @param scope
	 * @param cl
	 */
	public InstanceJavaMembers(Scriptable scope, Class< ? > cl)
	{
		super(scope == null ? new DummyScope() : scope, cl, false);
	}

	/**
	 * @see JavaMembers#reflectField(Scriptable, Field)
	 */
	@Override
	protected void reflectField(Scriptable scope, TypeInfoFactory typeFactory, Field field)
	{
		if (IConstantsObject.class.isAssignableFrom(cl) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) &&
			Modifier.isPublic(field.getModifiers()))
		{
			super.reflectField(scope, typeFactory, field);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.JavaMembers#discoverAccessibleMethods(java.lang.Class, boolean, boolean)
	 */
	@Override
	protected final Method[] discoverAccessibleMethods(Class< ? > clazz, boolean includeProtected, boolean includePrivate)
	{
		Method[] discoverAccessibleMethods = super.discoverAccessibleMethods(clazz, includeProtected, includePrivate);
		List<Method> lst = new ArrayList<Method>(discoverAccessibleMethods.length);
		for (Method discoverAccessibleMethod : discoverAccessibleMethods)
		{
			if (isJsMethod(discoverAccessibleMethod, clazz))
			{
				lst.add(discoverAccessibleMethod);
			}
		}
		return lst.toArray(new Method[lst.size()]);
	}

	protected boolean isJsMethod(Method method, Class< ? > originalClass)
	{
		return method.getName().startsWith("js_") || method.getName().startsWith("jsFunction_") || //$NON-NLS-1$ //$NON-NLS-2$
			AnnotationManagerReflection.getInstance().isAnnotationPresent(method, originalClass, JSReadonlyProperty.class) ||
			AnnotationManagerReflection.getInstance().isAnnotationPresent(method, originalClass, JSFunction.class) ||
			AnnotationManagerReflection.getInstance().isAnnotationPresent(method, originalClass, JSGetter.class) ||
			AnnotationManagerReflection.getInstance().isAnnotationPresent(method, originalClass, JSSetter.class);
	}

	/**
	 * @see org.mozilla.javascript.JavaMembers#makeBeanProperties(Scriptable, boolean)
	 */
	@SuppressWarnings("nls")
	@Override
	protected Map<String, BeanProperty> extractBeaning(Map<String, Object> members, boolean isStatic, boolean includePrivate)
	{
		HashMap<String, Object> copy = new HashMap<>(members);
		for (Entry<String, Object> entry : members.entrySet())
		{
			String name = entry.getKey();
			String newName = null;
			if (name.startsWith("js_")) //$NON-NLS-1$
			{
				newName = name.substring(3);
			}
			else
			{
				Object member = entry.getValue();
				if (member instanceof NativeJavaMethod)
				{
					if (((NativeJavaMethod)member).getMethods().length == 1)
					{
						MemberBox mb = ((NativeJavaMethod)member).getMethods()[0];
						if (mb.isMethod())
						{
							if (AnnotationManagerReflection.getInstance().isAnnotationPresent(mb.method(), cl, JSReadonlyProperty.class))
							{
								newName = AnnotationManagerReflection.getInstance().getAnnotation(mb.method(), cl, JSReadonlyProperty.class).property();
								if (newName == null || newName.length() == 0 && (entry.getKey().startsWith("get") || entry.getKey().startsWith("is")))
								{
									newName = entry.getKey().substring(entry.getKey().startsWith("get") ? 3 : 2);

									// Make the bean property name.
									char ch0 = newName.charAt(0);
									if (Character.isUpperCase(ch0))
									{
										if (newName.length() == 1)
										{
											newName = newName.toLowerCase();
										}
										else
										{
											char ch1 = newName.charAt(1);
											if (!Character.isUpperCase(ch1))
											{
												newName = Character.toLowerCase(ch0) + newName.substring(1);
											}
										}
									}
								}
							}
							else if (AnnotationManagerReflection.getInstance().isAnnotationPresent(mb.method(), cl, JSGetter.class))
							{
								newName = AnnotationManagerReflection.getInstance().getAnnotation(mb.method(), cl, JSGetter.class).value();
							}
							else if (AnnotationManagerReflection.getInstance().isAnnotationPresent(mb.method(), cl, JSSetter.class))
							{
								newName = AnnotationManagerReflection.getInstance().getAnnotation(mb.method(), cl, JSSetter.class).value();
							}
						}
					}
					for (MemberBox mb : ((NativeJavaMethod)member).getMethods())
					{
						if (mb.isMethod() && AnnotationManagerReflection.getInstance().isAnnotationPresent(mb.method(), cl, JSFunction.class))
						{
							String funcName = AnnotationManagerReflection.getInstance().getAnnotation(mb.method(), cl, JSFunction.class).value();
							if (funcName == null || funcName.length() == 0)
							{
								funcName = entry.getKey();
							}
							newName = "jsFunction_".concat(funcName); //$NON-NLS-1$
							break;
						}
					}
				}
			}
			if (newName != null && newName.length() > 0 && !newName.equals(name) && !Ident.checkIfJavascriptKeyword(newName))
			{
				putNewValueMergeForDuplicates(copy, name, newName);
			}
		}

		Map<String, BeanProperty> beans = super.extractBeaning(copy, isStatic, includePrivate);

		// filter out the beans that only have a set or getter.
		Iterator<BeanProperty> beansIterator = beans.values().iterator();
		while (beansIterator.hasNext())
		{
			BeanProperty beanProperty = beansIterator.next();
			if (beanProperty.getGetter() == null || beanProperty.getSetter() == null)
			{
				beansIterator.remove();
			}
			else
			{
				addMethodToHide(beanProperty.getGetter().getName());
				addMethodToHide(beanProperty.getSetter().getName());
			}
		}

		HashMap<String, Object> iterable = copy;
		copy = new HashMap<>(copy);
		for (Entry<String, Object> entry : iterable.entrySet())
		{
			String name = entry.getKey();
			if (name.startsWith("jsFunction_")) //$NON-NLS-1$
			{
				String newName = name.substring(11);
				if (!Ident.checkIfJavascriptKeyword(newName))
				{
					putNewValueMergeForDuplicates(copy, name, newName);
				}
			}
			else
			{
				Object member = entry.getValue();
				if (member instanceof NativeJavaMethod njm && ((NativeJavaMethod)member).getMethods().length == 1)
				{
					MemberBox mb = njm.getMethods()[0];
					if (mb.isMethod())
					{
						// make bean property

						JSReadonlyProperty jsReadonlyProperty = AnnotationManagerReflection.getInstance().getAnnotation(mb.method(), mb.getDeclaringClass(),
							JSReadonlyProperty.class);
						if (jsReadonlyProperty != null)
						{

							String propertyName = jsReadonlyProperty.property();
							if (propertyName == null || propertyName.length() == 0)
							{
								propertyName = name;
							}

							BeanProperty bp = new BeanProperty(propertyName, njm);
							Object oldValue = copy.put(propertyName, bp);
							if (oldValue instanceof NativeJavaMethod)
							{
								// allow the method to be called directly as well
								String functionName = ((NativeJavaMethod)oldValue).getFunctionName();
								if (!functionName.equals(propertyName))
								{
									copy.put(functionName, oldValue);
									// but do not show it
									addMethodToHide(functionName);
								}
							}
						}
					}
				}
			}
		}
		members.clear();
		members.putAll(copy);
		return beans;
	}

	/**
	 * @param copy
	 * @param name
	 * @param newName
	 */
	@SuppressWarnings("nls")
	private void putNewValueMergeForDuplicates(Map<String, Object> copy, String name, String newName)
	{
		Object oldValue = copy.put(newName, copy.remove(name));
		if (oldValue != null)
		{
			Object newValue = copy.get(newName);
			if (oldValue instanceof NativeJavaMethod && newValue instanceof NativeJavaMethod)
			{
				MemberBox[] oldMethods = ((NativeJavaMethod)oldValue).getMethods();
				MemberBox[] newMethods = ((NativeJavaMethod)newValue).getMethods();
				copy.put(newName, new NativeJavaMethod(Utils.arrayJoin(oldMethods, newMethods), newName));
			}
			else
			{
				Debug.error("illegal state new_name '" + newName + "' from old_name '" + name + "' can't be merged:  " + oldValue + ", " + newValue,
					new RuntimeException());
			}
		}
	}

//	/**
//	 * @see JavaMembers#shouldDeleteGetAndSetMethods()
//	 */
//	@Override
//	protected boolean shouldDeleteGetAndSetMethods()
//	{
//		return true;
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	protected void deleteGetAndSetMethods(boolean isStatic, List toRemove)
//	{
//		if (gettersAndSettersToHide == null)
//		{
//			gettersAndSettersToHide = new ArrayList<String>(toRemove);
//		}
//		else
//		{
//			gettersAndSettersToHide.addAll(toRemove);
//		}
//	}

	protected void addMethodToHide(String name)
	{
		if (gettersAndSettersToHide == null)
		{
			gettersAndSettersToHide = new ArrayList<String>();
		}
		String newName = name;
		if (newName.startsWith("js_")) //$NON-NLS-1$
		{
			newName = newName.substring(3);
		}
		gettersAndSettersToHide.add(newName);
	}

	public List<String> getGettersAndSettersToHide()
	{
		return gettersAndSettersToHide == null ? Collections.<String> emptyList() : Collections.<String> unmodifiableList(gettersAndSettersToHide);
	}

	static void registerClass(Scriptable scope, Class< ? > cls, InstanceJavaMembers ijm)
	{
		ClassCache cache = ClassCache.get(scope);
		Map<CacheKey, JavaMembers> ct = cache.getClassCacheMap();

		ct.put(new ClassCache.CacheKey(cls, null), ijm);
	}

	public static void deRegisterClass(Scriptable scope)
	{
		ClassCache cache = ClassCache.get(scope);
		cache.getClassCacheMap().clear();
		cache = null;

	}
}
