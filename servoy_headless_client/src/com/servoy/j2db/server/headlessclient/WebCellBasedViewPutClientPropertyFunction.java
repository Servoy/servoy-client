/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.j2db.server.headlessclient;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.scripting.FunctionDelegate;
import com.servoy.j2db.server.headlessclient.dataui.WebCellBasedView;

/**
 * Function delegate to reroute putClientProperty to the WebCellBasedView.
 *
 * @author rgansevles
 *
 */
public class WebCellBasedViewPutClientPropertyFunction extends FunctionDelegate
{
	private final WebCellBasedView view;
	private final IPersist persist;

	public WebCellBasedViewPutClientPropertyFunction(WebCellBasedView view, IPersist persist, Function function)
	{
		super(function);
		this.view = view;
		this.persist = persist;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		if (args != null && args.length == 2)
		{
			putClientProperty(args[0], args[1]);
			return null;
		}
		return super.call(cx, scope, thisObj, args);
	}

	private void putClientProperty(Object key, Object value)
	{
		view.putClientProperty(persist, key, value);
	}
}
