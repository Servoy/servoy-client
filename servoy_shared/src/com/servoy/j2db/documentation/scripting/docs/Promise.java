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
	 * @param onFulfilled
	 */
	public Promise then(Function onFulfilled)
	{
		return null;
	}

	/**
	 * @param onFulfilled
	 * @param onRejected
	 */
	public Promise then(Function onFulfilled, Function onRejected)
	{
		return null;
	}

	/**
	 * @param onRejected
	 */
	public Promise js_catch(Function onRejected)
	{
		return null;
	}

	/**
	 * @param onFinally
	 */
	public Promise js_finally(Function onFinally)
	{
		return null;
	}

	/**
	 * @param iterable
	 */
	public Promise allSettled(Array iterable)
	{
		return null;
	}

	/**
	 * @param iterable
	 */
	public Promise all(Array iterable)
	{
		return null;
	}

	/**
	 * @param iterable
	 */
	public Promise race(Array iterable)
	{
		return null;
	}

	/**
	 * @param reason
	 */
	public Promise reject(Object reason)
	{
		return null;
	}

	/**
	 * @param value
	 */
	public Promise resolve(Object value)
	{
		return null;
	}
}
