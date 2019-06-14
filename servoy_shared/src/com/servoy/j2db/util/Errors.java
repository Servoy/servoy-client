/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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
package com.servoy.j2db.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Utilities for using exceptions in streams.
 *
 *<pre>
 *		....stream()
 *			.map(s -> catchExceptions(() -> someMappingThrowingExceptions(s)))
 *			.filter(Objects::nonNull); // filter out errors in mapping
 *
 *		....stream()
 *			.filter(s -> catchExceptions(() -> someFilterThrowingExceptions(s), false));
 *
 *		....stream()
 *			.forEach(catchExceptions(s -> someConsumerThrowingExceptions(s)));
 *</pre>
 *
 * @author rgansevles
 *
 */
public final class Errors
{
	/**
	 * Call the callable, catch and log exceptions.
	 */
	public static <T> T catchExceptions(Callable<T> callable)
	{
		return catchExceptions(callable, null);
	}

	/**
	 * Call the callable, catch and log exceptions.
	 */
	public static <T> T catchExceptions(Callable<T> callable, T defValue)
	{
		try
		{
			return callable.call();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return defValue;
	}

	/**
	 * Run the runnable, catch and log exceptions.
	 */
	public static void catchExceptions(RunnableWithException runnable)
	{
		catchExceptions(() -> {
			runnable.run();
			return null;
		});
	}

	/**
	 * Consume, catch and log exceptions.
	 */
	public static <T> Consumer<T> catchExceptions(ConsumerWithException<T> consumer)
	{
		return t -> catchExceptions(() -> consumer.accept(t));
	}

	@FunctionalInterface
	public interface RunnableWithException
	{
		void run() throws Exception;
	}

	@FunctionalInterface
	public interface ConsumerWithException<T>
	{
		void accept(T t) throws Exception;
	}
}
