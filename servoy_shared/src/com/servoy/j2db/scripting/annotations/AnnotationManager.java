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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.servoy.j2db.util.Pair;

/**
 * Handles annotations form classes, caches annotations defined for the class itself and for the interfaces it implements.
 * 
 * Annotations have to defined at element (method or field) level, or at interface or class level.
 * When an annotation is defined at class or interface, all elements in that class are annotated, but not all methods from classes inheriting from that interface or class.
 * 
 * @author rgansevles
 *
 */
public class AnnotationManager<A, AC>
{
	private final Map<Pair<Pair<IAnnotatedMethod<A, AC>, IAnnotatedClass<A, AC>>, AC>, Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>>> methodAnnotationCache = new ConcurrentHashMap<Pair<Pair<IAnnotatedMethod<A, AC>, IAnnotatedClass<A, AC>>, AC>, Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>>>();
	private final Map<Pair<IAnnotatedField<A, AC>, AC>, Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>>> fieldAnnotationCache = new ConcurrentHashMap<Pair<IAnnotatedField<A, AC>, AC>, Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>>>();
	private final Map<Pair<IAnnotatedClass<A, AC>, AC>, Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>>> classAnnotationCache = new ConcurrentHashMap<Pair<IAnnotatedClass<A, AC>, AC>, Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>>>();

	public boolean isAnnotationPresent(IAnnotatedMethod<A, AC> method, IAnnotatedClass<A, AC> originalClass, AC annotationClass)
	{
		return method != null && getCachedAnnotation(method, originalClass, annotationClass).getLeft().booleanValue();
	}

	public A getAnnotation(IAnnotatedMethod<A, AC> method, IAnnotatedClass<A, AC> originalClass, AC annotationClass)
	{
		if (method == null) return null;
		Pair<IAnnotatedElement<A, AC>, A> pair = getCachedAnnotation(method, originalClass, annotationClass).getRight();
		return pair == null ? null : pair.getRight();
	}


	public A getAnnotation(IAnnotatedField<A, AC> field, AC annotationClass)
	{
		if (field == null) return null;
		Pair<IAnnotatedElement<A, AC>, A> pair = getCachedAnnotation(field, annotationClass).getRight();
		return pair == null ? null : pair.getRight();
	}

	public A getAnnotation(IAnnotatedClass<A, AC> targetClass, AC annotationClass)
	{
		if (targetClass == null) return null;
		Pair<IAnnotatedElement<A, AC>, A> pair = getCachedAnnotation(targetClass, annotationClass).getRight();
		return pair == null ? null : pair.getRight();
	}

	private Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>> getCachedAnnotation(IAnnotatedMethod<A, AC> method, IAnnotatedClass<A, AC> originalClass,
		AC annotationClass)
	{
		Pair<Pair<IAnnotatedMethod<A, AC>, IAnnotatedClass<A, AC>>, AC> key = new Pair<Pair<IAnnotatedMethod<A, AC>, IAnnotatedClass<A, AC>>, AC>(
			new Pair<IAnnotatedMethod<A, AC>, IAnnotatedClass<A, AC>>(method, originalClass), annotationClass);
		Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>> pair = methodAnnotationCache.get(key);
		if (pair == null)
		{
			methodAnnotationCache.put(key, pair = getAnnotationFromSuperclasses(originalClass, method, null, annotationClass));
		}

		return pair;
	}

	private Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>> getAnnotationFromSuperclasses(IAnnotatedClass<A, AC> originalClass,
		IAnnotatedMethod<A, AC> method, IAnnotatedField<A, AC> field, AC annotationClass)
	{
		for (IAnnotatedClass<A, AC> cls = originalClass; cls != null; cls = cls.getSuperclass())
		{
			// check if the method is part of an interface that has the annotation
			Pair<IAnnotatedElement<A, AC>, A> pair = getAnnotationFromInterfaces(cls, method, field, annotationClass);
			if (pair != null)
			{
				return new Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>>(Boolean.TRUE, pair);
			}
		}
		return new Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>>(Boolean.FALSE, null);
	}

	private Pair<IAnnotatedElement<A, AC>, A> getAnnotationFromInterfaces(IAnnotatedClass<A, AC> cls, IAnnotatedMethod<A, AC> method,
		IAnnotatedField<A, AC> field, AC searchedAnnotation)
	{
		A annotation = null;
		IAnnotatedElement<A, AC> annotatedElement = null;

		if (method != null)
		{
			IAnnotatedMethod<A, AC> m = cls.getMethod(method.getSignature());
			if (m != null)
			{
				annotation = m.getAnnotation(searchedAnnotation);
				annotatedElement = m;
				if (annotation == null)
				{
					// first check if the class where the method is defined has the annotation
					annotation = method.getDeclaringClass().getAnnotation(searchedAnnotation);
					annotatedElement = method.getDeclaringClass();
				}
			}
		}

		if (annotation == null && field != null)
		{
			IAnnotatedField<A, AC> f = cls.getField(field.getName());
			if (f != null)
			{
				annotation = f.getAnnotation(searchedAnnotation);
				annotatedElement = f;
				// first check if the class where the field is defined has the annotation
				if (annotation == null)
				{
					annotation = field.getDeclaringClass().getAnnotation(searchedAnnotation);
					annotatedElement = field.getDeclaringClass();
				}
			}
		}

		if (annotation == null)
		{
			// for fields and methods only use annotations when the field or method is defined in the class
			if (method == null && field == null)
			{
				// looking for annotation at class level
				annotation = cls.getAnnotation(searchedAnnotation);
				annotatedElement = cls;

				if (annotation == null)
				{
					// search interfaces
					for (IAnnotatedClass<A, AC> anInterface : cls.getInterfaces())
					{
						Pair<IAnnotatedElement<A, AC>, A> pair = getAnnotationFromInterfaces(anInterface, method, field, searchedAnnotation);
						if (pair != null)
						{
							return pair;
						}
					}
				}
			}
			else
			{
				// for methods and fields first search interfaces in class itself and superclasses
				for (IAnnotatedClass<A, AC> methodClass = cls; methodClass != null; methodClass = methodClass.getSuperclass())
				{
					// check if the method is part of an interface that has the annotation
					for (IAnnotatedClass<A, AC> anInterface : methodClass.getInterfaces())
					{
						Pair<IAnnotatedElement<A, AC>, A> pair = getAnnotationFromInterfaces(anInterface, method, field, searchedAnnotation);
						if (pair != null)
						{
							return pair;
						}
					}
				}

				// looking for annotation at field or method level, use cached annotation for class
				Pair<IAnnotatedElement<A, AC>, A> pair = getCachedAnnotation(cls, searchedAnnotation).getRight();
				if (pair != null)
				{
					// check if the place where the annotation was configured the method/field is present
					if (pair.getLeft() instanceof IAnnotatedClass< ? , ? >)
					{
						if (method != null && ((IAnnotatedClass<A, AC>)pair.getLeft()).isAssignableFrom(method.getDeclaringClass()) &&
							((IAnnotatedClass<A, AC>)pair.getLeft()).getMethod(method.getSignature()) != null)
						{
							return pair;
						}
						else if (field != null && ((IAnnotatedClass<A, AC>)pair.getLeft()).isAssignableFrom(field.getDeclaringClass()) &&
							((IAnnotatedClass<A, AC>)pair.getLeft()).getField(field.getName()) != null)
						{
							return pair;
						}
						// if we get here, class/interface-level annotation is not applicable to the method/field
					}
				}
			}
		}

		return annotation == null ? null : new Pair<IAnnotatedElement<A, AC>, A>(annotatedElement, annotation);
	}

	private Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>> getCachedAnnotation(IAnnotatedClass<A, AC> targetClass, AC annotationClass)
	{
		Pair<IAnnotatedClass<A, AC>, AC> key = new Pair<IAnnotatedClass<A, AC>, AC>(targetClass, annotationClass);
		Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>> pair = classAnnotationCache.get(key);
		if (pair == null)
		{
			classAnnotationCache.put(key, pair = getAnnotationFromSuperclasses(targetClass, null, null, annotationClass));
		}

		return pair;
	}

	private Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>> getCachedAnnotation(IAnnotatedField<A, AC> field, AC annotationClass)
	{
		Pair<IAnnotatedField<A, AC>, AC> key = new Pair<IAnnotatedField<A, AC>, AC>(field, annotationClass);
		Pair<Boolean, Pair<IAnnotatedElement<A, AC>, A>> pair = fieldAnnotationCache.get(key);
		if (pair == null)
		{
			fieldAnnotationCache.put(key, pair = getAnnotationFromSuperclasses(field.getDeclaringClass(), null, field, annotationClass));
		}

		return pair;
	}

	//////////////// interfaces //////////////////////

	public interface IAnnotatedElement<A, AC>
	{
		A getAnnotation(AC searchedAnnotation);
	}

	public interface IAnnotatedMethod<A, AC> extends IAnnotatedElement<A, AC>
	{
		Object getSignature();

		IAnnotatedClass<A, AC> getDeclaringClass();
	}

	public interface IAnnotatedClass<A, AC> extends IAnnotatedElement<A, AC>
	{
		IAnnotatedClass<A, AC> getSuperclass();

		IAnnotatedField<A, AC> getField(String name);

		Collection<IAnnotatedClass<A, AC>> getInterfaces();

		boolean isAssignableFrom(IAnnotatedClass<A, AC> declaringClass);

		IAnnotatedMethod<A, AC> getMethod(Object signature);
	}

	public interface IAnnotatedField<A, AC> extends IAnnotatedElement<A, AC>
	{
		String getName();

		IAnnotatedClass<A, AC> getDeclaringClass();
	}
}
