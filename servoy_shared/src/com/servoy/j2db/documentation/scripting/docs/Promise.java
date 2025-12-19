/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.documentation.scripting.docs;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * The Promise object represents the eventual completion (or failure) of an asynchronous operation and its resulting value
 *
 * For more information see: <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise">Promise</a>.
 *
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Promise", scriptingName = "Promise")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Promise
{
	/**
	 * The Promise object represents the eventual completion (or failure) of an asynchronous operation and its resulting value
	 *
	 * @param executor
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise
	 */
	public void jsConstructor_Promise(Function executor)
	{
	}

	/**
	 * The Promise.all() static method takes an iterable of promises as input and returns a single Promise.
	 * This returned promise fulfills when all of the input's promises fulfill (including when an empty iterable is passed),
	 * with an array of the fulfillment values. It rejects when any of the input's promises rejects, with this first rejection reason.
	 *
	 * @param iterable {Array<Promise>} An array of Promises to be resolved collectively.
	 *
	 * @return A Promise that fulfills with an array of values when all input promises resolve, or rejects with the reason of the first rejected promise.
	 */
	@SuppressWarnings("unused")
	@JSFunction
	public Promise all(Array iterable)
	{
		return null;
	}


	/**
	 * The Promise.allSettled() static method takes an iterable of promises as input and returns a single Promise.
	 * This returned promise fulfills when all of the input's promises settle (including when an empty iterable is passed),
	 * with an array of objects that describe the outcome of each promise.
	 *
	 * @param iterable {Array<Promise>} An array of promises to be settled.
	 *
	 * @return A promise that resolves with an array of outcome objects for each settled input promise.
	 */
	@JSFunction
	public Promise allSettled(Array iterable)
	{
		return null;
	}

	/**
	 * The Promise.race() static method takes an iterable of promises as input and returns a single Promise.
	 * This returned promise settles with the eventual state of the first promise that settles.
	 *
	 * @param iterable {Array<Promise>} An array of promises to race.
	 *
	 * @return A promise that settles with the outcome of the first settled input promise.
	 */
	@JSFunction
	public Promise race(Array iterable)
	{
		return null;
	}

	/**
	 * The Promise.reject() static method returns a Promise object that is rejected with a given reason.
	 *
	 * @param reason The reason for rejecting the promise.
	 *
	 * @return A promise that is rejected with the specified reason.
	 */
	@JSFunction
	public Promise reject(Object reason)
	{
		return null;
	}

	/**
	 * The Promise.resolve() static method "resolves" a given value to a Promise.
	 * If the value is a promise, that promise is returned;
	 * if the value is a thenable, Promise.resolve() will call the then() method with two callbacks it prepared;
	 * otherwise the returned promise will be fulfilled with the value.
	 *
	 * This function flattens nested layers of promise-like objects
	 * (e.g. a promise that fulfills to a promise that fulfills to something)
	 * into a single layer — a promise that fulfills to a non-thenable value.
	 *
	 * @param value A value or then able to resolve as a promise.
	 *
	 * @return A promise resolved with the given value or flattened if it is thenable.
	 */
	@JSFunction
	public Promise resolve(Object value)
	{
		return null;
	}

	/**
	 * The then() method of Promise instances takes up to two arguments:
	 * callback functions for the fulfilled and rejected cases of the Promise.
	 * It stores the callbacks within the promise it is called on and immediately returns another Promise object,
	 * allowing you to chain calls to other promise methods.
	 *
	 * @param onFulfilled {Function} A function called when the promise is fulfilled.
	 *
	 * @return A promise that resolves or rejects based on the provided fulfillment or rejection handler.
	 */
	@JSFunction
	public Promise then(Function onFulfilled)
	{
		return null;
	}

	/**
	 * The then() method of Promise instances takes up to two arguments:
	 * callback functions for the fulfilled and rejected cases of the Promise.
	 * It stores the callbacks within the promise it is called on and immediately returns another Promise object,
	 * allowing you to chain calls to other promise methods.
	 *
	 * @param onFulfilled The callback function that is invoked when the Promise is fulfilled.
	 * @param onRejected The callback function that is invoked when the Promise is rejected.
	 * @return A Promise that resolves or rejects based on the outcome of the provided fulfillment or rejection callbacks
	 */
	@JSFunction
	public Promise then(Function onFulfilled, Function onRejected)
	{
		return null;
	}

	/**
	 * The catch() method of Promise instances schedules a function to be called when the promise is rejected.
	 * It immediately returns another Promise object, allowing you to chain calls to other promise methods.
	 * It is a shortcut for then(undefined, onRejected).
	 *
	 * @param onRejected {Function} A function invoked when the promise is rejected.
	 *
	 * @return A promise that executes the rejection handler and resolves based on its outcome.
	 */
	public Promise js_catch(Function onRejected)
	{
		return null;
	}

	/**
	 * The finally() method of Promise instances schedules a function to be called when the promise is settled
	 * (either fulfilled or rejected). It immediately returns another Promise object,
	 * allowing you to chain calls to other promise methods.
	 *
	 * This lets you avoid duplicating code in both the promise's then() and catch() handlers.
	 *
	 * @param onFinally {Function} A function called once the promise is settled, regardless of outcome.
	 *
	 * @return A promise that executes the finalizing function after the original promise settles.
	 */
	public Promise js_finally(Function onFinally)
	{
		return null;
	}


}
