/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.scripting.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.servoy.j2db.util.Pair;

/**
 * Handles annotions form classes, caches annotations defined for the class itself and for the interfaces it implements.
 * 
 * @author rgansevles
 *
 */
public class AnnotationManager
{
	private static AnnotationManager INSTANCE = new AnnotationManager();

	private final Map<Pair<Method, Class< ? >>, Pair<Boolean, Annotation>> annotationCache = new ConcurrentHashMap<Pair<Method, Class< ? >>, Pair<Boolean, Annotation>>();
	private final Map<Pair<Class< ? >, Class< ? >>, Pair<Boolean, Annotation>> classAnnotationCache = new ConcurrentHashMap<Pair<Class< ? >, Class< ? >>, Pair<Boolean, Annotation>>();


	private AnnotationManager()
	{
	}

	public static AnnotationManager getInstance()
	{
		return INSTANCE;
	}

	public boolean isAnnotationPresent(Method method, Class< ? extends Annotation> annotationClass)
	{
		return getCachedAnnotation(method, annotationClass, null).getLeft().booleanValue();
	}

	public boolean isMobileAnnotationPresent(Method method)
	{
		return getCachedAnnotation(method, ServoyMobile.class, ServoyMobileFilterOut.class).getLeft().booleanValue();
	}

	public boolean isAnnotationPresent(Method method, Class< ? extends Annotation>[] annotationClasses)
	{
		boolean allTested = true;
		for (Class< ? extends Annotation> annotationClass : annotationClasses)
		{
			Pair<Method, Class< ? >> key = new Pair<Method, Class< ? >>(method, annotationClass);
			Pair<Boolean, Annotation> pair = annotationCache.get(key);
			if (pair != null)
			{
				if (pair.getLeft().booleanValue()) return true;
				continue;
			}
			else
			{
				allTested = false;
			}
			Annotation annotation = method.getAnnotation(annotationClass);
			if (annotation != null)
			{
				annotationCache.put(key, pair = new Pair<Boolean, Annotation>(Boolean.TRUE, annotation));
				return true;
			}
		}
		if (allTested) return false;

		boolean found = false;

		for (Class< ? > cls = method.getDeclaringClass(); (cls != Object.class && cls != null); cls = cls.getSuperclass())
		{
			// check if the method is part of an interface that has the annotation
			Class< ? >[] interfaces = cls.getInterfaces();
			for (Class< ? > interface1 : interfaces)
			{
				try
				{
					Method interfaceMethod = interface1.getMethod(method.getName(), method.getParameterTypes());
					for (Class< ? extends Annotation> annotationClass : annotationClasses)
					{
						Annotation annotation = interfaceMethod.getAnnotation(annotationClass);
						if (annotation != null)
						{
							Pair<Method, Class< ? >> key = new Pair<Method, Class< ? >>(method, annotationClass);
							annotationCache.put(key, new Pair<Boolean, Annotation>(Boolean.TRUE, annotation));
							found = true;
						}
					}
				}
				catch (SecurityException e)
				{
				}
				catch (NoSuchMethodException e)
				{
				}
			}
		}

		for (Class< ? extends Annotation> annotationClass : annotationClasses)
		{
			Pair<Method, Class< ? >> key = new Pair<Method, Class< ? >>(method, annotationClass);
			Pair<Boolean, Annotation> pair = annotationCache.get(key);
			if (pair == null)
			{
				annotationCache.put(key, new Pair<Boolean, Annotation>(Boolean.FALSE, null));
			}
		}

		return found;
	}

	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass)
	{
		return (T)getCachedAnnotation(method, annotationClass, null).getRight();
	}

	private Pair<Boolean, Annotation> getCachedAnnotation(Method method, Class< ? extends Annotation> annotationClass,
		Class< ? extends Annotation> stopAnnotation)
	{
		Pair<Method, Class< ? >> key = new Pair<Method, Class< ? >>(method, annotationClass);
		Pair<Boolean, Annotation> pair = annotationCache.get(key);
		if (pair == null)
		{
			Annotation annotation = null;
			for (Class< ? > cls = method.getDeclaringClass(); annotation == null && (cls != Object.class && cls != null); cls = cls.getSuperclass())
			{
				// check if the method is part of an interface that has the annotation
				Pair<Boolean, Annotation> x = getAnnotationFromInterfaces(cls, method, annotationClass, stopAnnotation, false);
				annotation = x.getRight();
				if (x.getLeft().booleanValue()) break; // stop encountered
			}
			annotationCache.put(key, pair = new Pair<Boolean, Annotation>(annotation == null ? Boolean.FALSE : Boolean.TRUE, annotation));
		}

		return pair;
	}

	private Pair<Boolean, Annotation> getAnnotationFromInterfaces(Class< ? > cls, Method method, Class< ? extends Annotation> searchedAnnotation,
		Class< ? extends Annotation> stopAnnotation, boolean stopAlreadyEncountered)
	{
		Annotation a = null;
		boolean stopped = false;
		boolean stopEncountered = false;

		if (method != null)
		{
			Method m = null;
			try
			{
				m = (method.getDeclaringClass() == cls) ? method : cls.getMethod(method.getName(), method.getParameterTypes());
			}
			catch (SecurityException e)
			{
			}
			catch (NoSuchMethodException e)
			{
			}

			if (m != null)
			{
				if (stopAlreadyEncountered) stopped = true;
				else
				{
					a = m.getAnnotation(searchedAnnotation);
					if (a == null && stopAnnotation != null) // this assumes that only start+stop annotations can be set at class level as well as method level (so ServoyMobile annotation)
					{
						a = cls.getAnnotation(searchedAnnotation);
						if (a instanceof ServoyMobile)
						{
							if (!((ServoyMobile)a).value()) a = null; // so class level annotation is configured to not auto-include all members
						}
					}
					if (a == null && stopAnnotation != null && (m.getAnnotation(stopAnnotation) != null)) stopped = true;
				}
			}
			else if (stopAnnotation != null) stopEncountered = stopAlreadyEncountered || (cls.getAnnotation(stopAnnotation) != null);
		}
		else
		{
			a = cls.getAnnotation(searchedAnnotation);
			if (a == null && stopAnnotation != null) stopped = (cls.getAnnotation(stopAnnotation) != null);
		}

		if (a == null && !stopped)
		{
			for (Class< ? > anInterface : cls.getInterfaces())
			{
				Pair<Boolean, Annotation> x = getAnnotationFromInterfaces(anInterface, method, searchedAnnotation, stopAnnotation, stopEncountered);
				a = x.getRight();
				stopped = x.getLeft().booleanValue();
				if (a != null) break;
			}
		}
		return new Pair<Boolean, Annotation>(Boolean.valueOf(stopped), a);
	}

	public boolean isAnnotationPresent(Class< ? > cls, Class< ? extends Annotation> annotationClass)
	{
		return getCachedAnnotation(cls, annotationClass, null).getLeft().booleanValue();
	}

	public boolean isMobileAnnotationPresent(Class< ? > cls)
	{
		return getCachedAnnotation(cls, ServoyMobile.class, ServoyMobileFilterOut.class).getLeft().booleanValue();
	}

	private Pair<Boolean, Annotation> getCachedAnnotation(Class< ? > targetClass, Class< ? extends Annotation> annotationClass,
		Class< ? extends Annotation> stopAnnotation)
	{
		Pair<Class< ? >, Class< ? >> key = new Pair<Class< ? >, Class< ? >>(targetClass, annotationClass);
		Pair<Boolean, Annotation> pair = classAnnotationCache.get(key);
		if (pair == null)
		{
			Class< ? > clz = targetClass;
			Annotation annotation = null;
			for (; annotation == null && (clz != Object.class && clz != null); clz = clz.getSuperclass())
			{
				Pair<Boolean, Annotation> x = getAnnotationFromInterfaces(clz, null, annotationClass, stopAnnotation, false);
				annotation = x.getRight();
				if (x.getLeft().booleanValue()) break; // stop encountered
			}
			classAnnotationCache.put(key, pair = new Pair<Boolean, Annotation>(annotation == null ? Boolean.FALSE : Boolean.TRUE, annotation));
		}

		return pair;
	}
}
