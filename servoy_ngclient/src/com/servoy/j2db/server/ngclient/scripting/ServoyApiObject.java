/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.server.ngclient.scripting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.ViewFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.component.RhinoMapOrArrayWrapper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Provides utility methods for server side scripting.
 * @author lvostinar
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ServoyApi", scriptingName = "servoyApi")
@ServoyClientSupport(sc = false, wc = false, ng = true)
public class ServoyApiObject
{
	private final IApplication app;

	public ServoyApiObject(IApplication app)
	{
		this.app = app;
	}

	@JSFunction
	/**
	 * Creates a view (read-only) foundset.
	 * @param name foundset name
	 * @param query query builder used to get the data for the foundset
	 * @return the view foundset
	 * @throws ServoyException
	 */
	public ViewFoundSet getViewFoundSet(String name, QBSelect query) throws ServoyException
	{
		if (!app.haveRepositoryAccess())
		{
			// no access to repository yet, have to log in first
			throw new ServoyException(ServoyException.CLIENT_NOT_AUTHORIZED);
		}
		return app.getFoundSetManager().getViewFoundSet(name, query);
	}

	/**
	 * Get select query for dataSource
	 * @param dataSource the dataSource
	 * @return QB select for the dataSource
	 * @throws ServoyException, RepositoryException
	 */
	@JSFunction
	public QBSelect getQuerySelect(String dataSource) throws ServoyException, RepositoryException
	{
		if (!app.haveRepositoryAccess())
		{
			// no access to repository yet, have to log in first
			throw new ServoyException(ServoyException.CLIENT_NOT_AUTHORIZED);
		}
		return (QBSelect)app.getFoundSetManager().getQueryFactory().createSelect(dataSource);
	}

	/**
	 * Hide a form directly on the server for instance when a tab will change on the client, so it won't need to do a round trip
	 * for hiding the form through the browser's component.
	 *
	 * @sample
	 * servoyApi.hideForm(formToHideName)
	 *
	 * @param formName the form to hide
	 * @return true if the form was hidden
	 */
	@JSFunction
	public boolean hideForm(String nameOrUUID)
	{
		String formName = nameOrUUID;
		Form form = app.getFlattenedSolution().getForm(nameOrUUID);
		if (form == null)
		{
			form = (Form)app.getFlattenedSolution().searchPersist(nameOrUUID);
			if (form != null)
			{
				formName = form.getName();
			}
		}
		IWebFormController formController = (IWebFormController)app.getFormManager().getForm(formName);
		if (formController != null)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			boolean ret = formController.notifyVisible(false, invokeLaterRunnables);
			if (ret)
			{
				formController.setParentFormController(null);
			}
			Utils.invokeAndWait(app, invokeLaterRunnables);
			return ret;
		}
		return false;
	}

	/**
	 * Can be used to deep copy a custom value.
	 *
	 * @sample
	 * var eventSourceCopy = servoyApi.copyObject(eventSource);
	 *
	 * @param value the value to be copied
	 * @return a copy of the value object, the same as constructing the object in javascript from scratch
	 */
	@JSFunction
	public IdScriptableObject copyObject(Object value)
	{
		if (value instanceof NativeObject)
		{
			NativeObject nativeObject = new NativeObject();
			Object[] ids = ((NativeObject)value).getIds();
			for (Object id : ids)
			{
				Object objectValue = ((NativeObject)value).get(id.toString(), (NativeObject)value);
				if (objectValue instanceof RhinoMapOrArrayWrapper || objectValue instanceof NativeObject || objectValue instanceof NativeArray)
				{
					objectValue = copyObject(objectValue);
				}
				nativeObject.put(id.toString(), nativeObject, objectValue);
			}
			return nativeObject;
		}
		if (value instanceof NativeArray)
		{
			NativeArray arr = (NativeArray)value;
			Object[] values = new Object[arr.size()];
			for (int i = 0; i < arr.size(); i++)
			{
				Object objectValue = arr.get(i);
				if (objectValue instanceof RhinoMapOrArrayWrapper || objectValue instanceof NativeObject || objectValue instanceof NativeArray)
				{
					objectValue = copyObject(objectValue);
				}
				values[i] = objectValue;
			}
			return new NativeArray(values);
		}
		if (value instanceof RhinoMapOrArrayWrapper)
		{
			if (((RhinoMapOrArrayWrapper)value).getWrappedValue() instanceof Map)
			{
				NativeObject nativeObject = new NativeObject();
				Object[] ids = ((RhinoMapOrArrayWrapper)value).getIds();
				for (Object id : ids)
				{
					Object objectValue = ((RhinoMapOrArrayWrapper)value).get(id.toString(), (RhinoMapOrArrayWrapper)value);
					if (objectValue instanceof RhinoMapOrArrayWrapper || objectValue instanceof NativeObject || objectValue instanceof NativeArray)
					{
						objectValue = copyObject(objectValue);
					}
					nativeObject.put(id.toString(), nativeObject, objectValue);
				}
				return nativeObject;
			}
			else
			{
				Object[] ids = ((RhinoMapOrArrayWrapper)value).getIds();
				Object[] values = new Object[ids.length];
				for (int i = 0; i < ids.length; i++)
				{
					Object objectValue = ((RhinoMapOrArrayWrapper)value).get(i, (RhinoMapOrArrayWrapper)value);
					if (objectValue instanceof RhinoMapOrArrayWrapper || objectValue instanceof NativeObject || objectValue instanceof NativeArray)
					{
						objectValue = copyObject(objectValue);
					}
					values[i] = objectValue;
				}
				NativeArray nativeArray = new NativeArray(values);
				return nativeArray;
			}
		}
		Debug.error("cannot return object: " + value + " as NativeObject");
		return new NativeObject();
	}
}
