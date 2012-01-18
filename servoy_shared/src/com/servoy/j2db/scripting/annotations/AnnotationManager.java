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


	private AnnotationManager()
	{
	}

	public static AnnotationManager getInstance()
	{
		return INSTANCE;
	}

	public boolean isAnnotationPresent(Method method, Class< ? extends Annotation> annotationClass)
	{
		return getCachedAnnotation(method, annotationClass).getLeft().booleanValue();
	}

	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass)
	{
		return (T)getCachedAnnotation(method, annotationClass).getRight();
	}

	private Pair<Boolean, Annotation> getCachedAnnotation(Method method, Class< ? extends Annotation> annotationClass)
	{
		Pair<Method, Class< ? >> key = new Pair<Method, Class< ? >>(method, annotationClass);
		Pair<Boolean, Annotation> pair = annotationCache.get(key);
		if (pair == null)
		{
			Annotation annotation = method.getAnnotation(annotationClass);
			for (Class< ? > cls = method.getDeclaringClass(); annotation == null && cls != Object.class; cls = cls.getSuperclass())
			{
				// check if the method is part of an interface that has the annotation
				Class< ? >[] interfaces = cls.getInterfaces();
				for (int i = 0; annotation == null && i < interfaces.length; i++)
				{
					try
					{
						annotation = interfaces[i].getMethod(method.getName(), method.getParameterTypes()).getAnnotation(annotationClass);
					}
					catch (SecurityException e)
					{
					}
					catch (NoSuchMethodException e)
					{
					}
				}
			}
			annotationCache.put(key, pair = new Pair<Boolean, Annotation>(annotation == null ? Boolean.FALSE : Boolean.TRUE, annotation));
		}

		return pair;
	}

}
