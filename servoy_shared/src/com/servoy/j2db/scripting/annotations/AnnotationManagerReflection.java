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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.annotations.AnnotationManager.IAnnotatedClass;
import com.servoy.j2db.scripting.annotations.AnnotationManager.IAnnotatedField;
import com.servoy.j2db.scripting.annotations.AnnotationManager.IAnnotatedMethod;


/**
 * Handles annotations form classes, caches annotations defined for the class itself and for the interfaces it implements.
 * 
 * Annotations have to defined at element (method or field) level, or at interface or class level.
 * When an annotation is defined at class or interface, all elements in that class are annotated, but not all methods from classes inheriting from that interface or class.
 * 
 * @author rgansevles
 *
 */
public class AnnotationManagerReflection
{
	private static AnnotationManagerReflection INSTANCE = new AnnotationManagerReflection();

	private final AnnotationManager<Annotation, Class< ? extends Annotation>> annotationManager = new AnnotationManager<Annotation, Class< ? extends Annotation>>();

	private AnnotationManagerReflection()
	{
	}

	public static AnnotationManagerReflection getInstance()
	{
		return INSTANCE;
	}

	public <T extends Annotation> boolean isAnnotationPresent(Method method, Class< ? > originalClass, Class<T> annotationClass)
	{
		return getAnnotation(method, originalClass, annotationClass) != null;
	}

	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(Method method, Class< ? > originalClass, Class<T> annotationClass)
	{
		if (method == null) return null;
		return (T)annotationManager.getAnnotation(new ReflectionAnnotatedMethod(method), new ReflectionAnnotatedClass(originalClass), annotationClass);
	}

	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(Field field, Class<T> annotationClass)
	{
		if (field == null) return null;
		return (T)annotationManager.getAnnotation(new ReflectionAnnotatedField(field), annotationClass);
	}

	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(Class< ? > targetClass, Class<T> annotationClass)
	{
		if (targetClass == null) return null;
		return (T)annotationManager.getAnnotation(new ReflectionAnnotatedClass(targetClass), annotationClass);
	}

	public boolean hasSupportForClientType(Method method, Class< ? > originalClass, ClientSupport clientType, ClientSupport defaultClientType)
	{
		return getClientSupport(method, originalClass, defaultClientType).hasSupport(clientType);
	}

	public boolean hasSupportForClientType(Class< ? extends IScriptable> cls, ClientSupport clientType, ClientSupport defaultClientType)
	{
		return getClientSupport(cls, defaultClientType).hasSupport(clientType);
	}

	public boolean hasSupportForClientType(Field field, ClientSupport clientType, ClientSupport defaultClientType)
	{
		return getClientSupport(field, defaultClientType).hasSupport(clientType);
	}

	public ClientSupport getClientSupport(Method method, Class< ? > originalClass, ClientSupport defaultClientType)
	{
		ServoyClientSupport csp = getAnnotation(method, originalClass, ServoyClientSupport.class);
		return csp == null ? defaultClientType : ClientSupport.fromAnnotation(csp);
	}

	public ClientSupport getClientSupport(Class< ? > cls, ClientSupport defaultClientType)
	{
		ServoyClientSupport csp = getAnnotation(cls, ServoyClientSupport.class);
		return csp == null ? defaultClientType : ClientSupport.fromAnnotation(csp);
	}

	public ClientSupport getClientSupport(Field field, ClientSupport defaultClientType)
	{
		ServoyClientSupport csp = getAnnotation(field, ServoyClientSupport.class);
		return csp == null ? defaultClientType : ClientSupport.fromAnnotation(csp);
	}

	public static void flushCachedItems()
	{
		INSTANCE = null;
	}

	/////////////// Wrapper classes ///////////////////

	private static class ReflectionAnnotatedClass implements IAnnotatedClass<Annotation, Class< ? extends Annotation>>
	{
		private final Class< ? > originalClass;

		private ReflectionAnnotatedClass(Class< ? > originalClass)
		{
			this.originalClass = originalClass;
		}

		public Class< ? > getOriginalClass()
		{
			return originalClass;
		}

		@Override
		public Annotation getAnnotation(Class< ? extends Annotation> searchedAnnotation)
		{
			return originalClass.getAnnotation(searchedAnnotation);
		}

		@Override
		public IAnnotatedClass<Annotation, Class< ? extends Annotation>> getSuperclass()
		{
			Class< ? > superclass = originalClass.getSuperclass();
			return superclass == null || superclass == Object.class ? null : new ReflectionAnnotatedClass(superclass);
		}

		@Override
		public IAnnotatedMethod<Annotation, Class< ? extends Annotation>> getMethod(Object signature)
		{
			try
			{
				return new ReflectionAnnotatedMethod(originalClass.getMethod(((ReflectionSignature)signature).getName(),
					((ReflectionSignature)signature).getParameterTypes()));
			}
			catch (NoSuchMethodException e)
			{
				return null;
			}
		}

		@Override
		public IAnnotatedField<Annotation, Class< ? extends Annotation>> getField(String name)
		{
			try
			{
				return new ReflectionAnnotatedField(originalClass.getField(name));
			}
			catch (NoSuchFieldException e)
			{
				return null;
			}
		}

		@Override
		public Collection<IAnnotatedClass<Annotation, Class< ? extends Annotation>>> getInterfaces()
		{
			Class< ? >[] intfs = originalClass.getInterfaces();
			IAnnotatedClass<Annotation, Class< ? extends Annotation>>[] interfaces = new ReflectionAnnotatedClass[intfs.length];
			for (int i = 0; i < intfs.length; i++)
			{
				interfaces[i] = new ReflectionAnnotatedClass(intfs[i]);
			}
			return Arrays.asList(interfaces);
		}

		@Override
		public boolean isAssignableFrom(IAnnotatedClass<Annotation, Class< ? extends Annotation>> declaringClass)
		{
			return originalClass.isAssignableFrom(((ReflectionAnnotatedClass)declaringClass).getOriginalClass());
		}

		@Override
		public int hashCode()
		{
			return originalClass.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ReflectionAnnotatedClass other = (ReflectionAnnotatedClass)obj;
			if (originalClass == null)
			{
				if (other.originalClass != null) return false;
			}
			else if (!originalClass.equals(other.originalClass)) return false;
			return true;
		}
	}


	private static class ReflectionAnnotatedField implements IAnnotatedField<Annotation, Class< ? extends Annotation>>
	{
		private final Field field;

		private ReflectionAnnotatedField(Field field)
		{
			this.field = field;
		}

		@Override
		public Annotation getAnnotation(Class< ? extends Annotation> searchedAnnotation)
		{
			return field.getAnnotation(searchedAnnotation);
		}

		@Override
		public String getName()
		{
			return field.getName();
		}

		@Override
		public IAnnotatedClass<Annotation, Class< ? extends Annotation>> getDeclaringClass()
		{
			return new ReflectionAnnotatedClass(field.getDeclaringClass());
		}

		@Override
		public int hashCode()
		{
			return field.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ReflectionAnnotatedField other = (ReflectionAnnotatedField)obj;
			if (field == null)
			{
				if (other.field != null) return false;
			}
			else if (!field.equals(other.field)) return false;
			return true;
		}
	}

	private static class ReflectionAnnotatedMethod implements IAnnotatedMethod<Annotation, Class< ? extends Annotation>>
	{
		private final Method method;

		private ReflectionAnnotatedMethod(Method method)
		{
			this.method = method;
		}

		@Override
		public Annotation getAnnotation(Class< ? extends Annotation> searchedAnnotation)
		{
			return method.getAnnotation(searchedAnnotation);
		}

		@Override
		public ReflectionSignature getSignature()
		{
			return new ReflectionSignature(method.getName(), method.getParameterTypes());
		}

		@Override
		public IAnnotatedClass<Annotation, Class< ? extends Annotation>> getDeclaringClass()
		{
			return new ReflectionAnnotatedClass(method.getDeclaringClass());
		}

		@Override
		public int hashCode()
		{
			return method.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ReflectionAnnotatedMethod other = (ReflectionAnnotatedMethod)obj;
			if (method == null)
			{
				if (other.method != null) return false;
			}
			else if (!method.equals(other.method)) return false;
			return true;
		}
	}

	private static class ReflectionSignature
	{
		private final String name;
		private final Class< ? >[] parameterTypes;

		private ReflectionSignature(String name, Class< ? >[] parameterTypes)
		{
			this.name = name;
			this.parameterTypes = parameterTypes;
		}

		public String getName()
		{
			return name;
		}

		public Class< ? >[] getParameterTypes()
		{
			return parameterTypes;
		}
	}
}
