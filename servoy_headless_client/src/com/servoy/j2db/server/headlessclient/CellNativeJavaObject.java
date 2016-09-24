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
package com.servoy.j2db.server.headlessclient;

import org.apache.wicket.Component;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.scripting.ITwoNativeJavaObject;
import com.servoy.j2db.server.headlessclient.dataui.WebCellBasedView;
import com.servoy.j2db.ui.scripting.AbstractRuntimeBaseComponent;

/**
 * This object intersects the requestFocus() java-script calls for elements inside a table/list view for the web client
 * and redirects them to the appropriate cells in the wicket table.<BR>
 * The component on which this function gets called is not a real component in the table view cell. That is why
 * we need this class.
 *
 * @author acostescu
 */
public class CellNativeJavaObject extends NativeJavaObject implements ITwoNativeJavaObject
{

	private final WebCellBasedView view;
	private final Component uiComponent;
	private Object realObject;

	/**
	 * Creates a new NativeJavaObject for the given object with the corresponding java members in order to
	 * intercept the requestFocus() calls on that object. It then tells the WebCellBasedView which column wants
	 * focus, so the real cell in the current record of that view can request focus.
	 * @param fs ...
	 * @param obj the object that represents a column in the table/list cell view.
	 * @param jm the members this object exposes to java-script.
	 * @param view the WebCellBasedView that knows about it's cells.
	 */
	public CellNativeJavaObject(Scriptable fs, Object obj, JavaMembers jm, WebCellBasedView view)
	{
		super(fs, ((IScriptableProvider)obj).getScriptObject(), jm);
		this.view = view;
		this.uiComponent = (Component)obj;
		realObject = null;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		Object val = super.get(name, start);

		if (val instanceof Function)
		{
			if ("requestFocus".equals(name))
			{
				// must remember to request focus for the according cell in the cell view...
				// when that cell's component is created
				view.setColumnThatRequestsFocus(uiComponent);
				val = new BaseFunction();
			}
			else if ("putClientProperty".equals(name) && javaObject instanceof AbstractRuntimeBaseComponent)
			{
				// put client properties for all elements in table view
				val = new WebCellBasedViewPutClientPropertyFunction(view, ((AbstractRuntimeBaseComponent< ? >)javaObject).getPersist(), (Function)val);
			}
			else if ("getClientProperty".equals(name) && javaObject instanceof AbstractRuntimeBaseComponent)
			{
				// put client properties for all elements in table view
				val = new WebCellBasedViewGetClientPropertyFunction(view, ((AbstractRuntimeBaseComponent< ? >)javaObject).getPersist(), (Function)val);
			}
		}

		return val;
	}

	public void setRealObject(Object realObject)
	{
		this.realObject = realObject;
	}

	@Override
	public Object unwrap()
	{
		if (realObject != null)
		{
			return realObject;
		}
		return super.unwrap();
	}

}