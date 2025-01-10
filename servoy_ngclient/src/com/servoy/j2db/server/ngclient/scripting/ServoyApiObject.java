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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.annotations.JSFunction;
import org.sablo.Container;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ViewFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.NGClientWindow;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RhinoMapOrArrayWrapper;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * <p>
 * The <code>ServoyApiObject</code> provides server-side utility methods to facilitate interactions within
 * the Servoy environment, primarily for NG and Titanium client components or services. These utilities are
 * integral to the internal implementation of web objects, enabling seamless data management and dynamic
 * UI interactions.
 * </p>
 *
 * <p>
 * The API supports functionalities such as creating foundsets and view foundsets using <code>QBSelect</code>
 * for flexible data retrieval and filtering. It allows direct manipulation of forms on the server, including
 * methods to show or hide forms dynamically, ensuring efficient updates without additional client-side round trips.
 * Session-specific filters can be applied to foundsets, offering dynamic, user-specific data restrictions.
 * </p>
 *
 * <p>
 * Additional capabilities include performing SQL queries with query builders, generating media URLs from
 * server-side byte arrays, and creating custom <code>JSEvent</code> instances for event-driven workflows.
 * Developers can also use utility functions like deep copying of complex objects and creating empty datasets,
 * enhancing data and object management within the platform.
 * </p>
 *
 * @author lvostinar
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ServoyApi", scriptingName = "servoyApi")
@ServoyClientSupport(sc = false, wc = false, ng = true)
public class ServoyApiObject
{
	private final INGApplication app;
	private final WebFormComponent component;

	public ServoyApiObject(INGApplication app, WebFormComponent component)
	{
		this.app = app;
		this.component = component;
	}


	/**
	 * Creates a view (read-only) foundset.
	 * @param name foundset name
	 * @param query query builder used to get the data for the foundset
	 * @return The view foundset created based on the specified query.
	 * @throws ServoyException
	 */
	@JSFunction
	public ViewFoundSet getViewFoundSet(String name, QBSelect query) throws ServoyException
	{
		app.checkAuthorized();
		return app.getFoundSetManager().getViewFoundSet(name, query, false);
	}

	/**
	 * Creates a foundset.
	 * @param query query builder used to get the data for the foundset
	 * @return The foundset created based on the specified query.
	 * @throws ServoyException
	 */
	@JSFunction
	public FoundSet getFoundSet(QBSelect query) throws ServoyException
	{
		app.checkAuthorized();
		return (FoundSet)app.getFoundSetManager().getFoundSet(query);
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
		app.checkAuthorized();
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
		IWebFormController formController = app.getFormManager().getForm(formName);
		if (formController != null)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			boolean ret = formController.notifyVisible(false, invokeLaterRunnables, true);
			if (ret)
			{
				component.updateVisibleForm(formController.getFormUI(), false, 0);
				Container parent = component.getParent();
				while (parent != null && !(parent instanceof IWebFormUI))
				{
					parent = parent.getParent();
				}
				if (parent instanceof IWebFormUI parentUI)
					parentUI.getDataAdapterList().removeVisibleChildForm(formController, true);
			}
			Utils.invokeAndWait(app, invokeLaterRunnables);
			return ret;
		}
		return false;
	}

	/**
	 * Show a form directly on the server for instance when a tab will change on the client, so it won't need to do a round trip
	 * for showing the form through the browser's component.
	 *
	 * NOTE: Make sure this isn't called with a form name that can direclty come from the client, because this call allows all forms to be shown!
	 *
	 * @sample
	 * servoyApi.showForm(formToShowName)
	 *
	 * @param nameOrUUID the form to show
	 * @return true if the form was marked as visible
	 */
	@JSFunction
	public boolean showForm(String nameOrUUID)
	{
		return this.showForm(nameOrUUID, null);
	}

	/**
	 * Show a form directly on the server for instance when a tab will change on the client, so it won't need to do a round trip
	 * for showing the form through the browser's component.
	 *
	 * NOTE: Make sure this isn't called with a form name that can direclty come from the client, because this call allows all forms to be shown!
	 *
	 * @sample
	 * servoyApi.showForm(formToShowName)
	 *
	 * @param nameOrUUID the form to show
	 * @param relationName the parent container
	 * @return true if the form was marked as visible
	 */
	@JSFunction
	public boolean showForm(String nameOrUUID, String relationName)
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
		IWebFormController formController = app.getFormManager().getForm(formName, this.component);
		IWebFormController parentFormController = null;
		if (this.component != null)
		{
			parentFormController = this.component.findParent(IWebFormUI.class).getController();
		}
		if (formController != null)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			boolean ret = formController.notifyVisible(true, invokeLaterRunnables, true);
			if (ret)
			{
				if (parentFormController != null)
				{
					IFoundSetInternal parentFs = parentFormController.getFormModel();
					IRecordInternal selectedRecord = parentFs.getRecord(parentFs.getSelectedIndex());
					if (selectedRecord != null)
					{
						try
						{
							formController.loadRecords(selectedRecord.getRelatedFoundSet(relationName));
						}
						catch (RuntimeException re)
						{
							throw new RuntimeException("Can't load records on form " + formController.getName() + ", of parent record: " +
								selectedRecord + " with relation " + relationName + " for parent form  " + parentFormController + " and bean " +
								component, re);
						}
					}
					else
					{
						// no selected record, then use prototype so we can get global relations
						try
						{
							formController.loadRecords(parentFs.getPrototypeState().getRelatedFoundSet(relationName));
						}
						catch (RuntimeException re)
						{
							throw new RuntimeException("Can't load records on form " + formController.getName() + ", of parent record: " +
								selectedRecord + " with relation " + relationName + " for parent form  " + parentFormController + " and bean " +
								component, re);
						}

					}

					parentFormController.getFormUI().getDataAdapterList().addVisibleChildForm(formController, relationName, true);
					if (component != null)
					{
						component.updateVisibleForm(formController.getFormUI(), true, 0);
					}
				}

			}
			Utils.invokeAndWait(app, invokeLaterRunnables);

			if (ret)
			{
				NGClientWindow.getCurrentWindow().registerAllowedForm(formName, this.component.getFormElement());
				NGClientWindow.getCurrentWindow().touchForm(app.getFlattenedSolution().getFlattenedForm(form), formName, true, true);
			}
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

	/**
	 * This will generate a url from a byte array so that the client can get the bytes from that url.
	 *
	 * @sample
	 * var url = servoyApi.getMediaUrl(bytes);
	 *
	 * @param bytes The value where an url should be created for
	 * @return the url where the bytes can be downloaded from
	 */
	@JSFunction
	public String getMediaUrl(byte[] bytes)
	{
		MediaResourcesServlet.MediaInfo mediaInfo = app.createMediaInfo(bytes);
		return mediaInfo.getURL(app.getWebsocketSession().getSessionKey().getClientnr());
	}


	/**
	 *	This will generate a list of primary keys names for the given data source.
	 *
	 * @sample
	 * var pkNames = servoyApi.getDatasourcePKs(datasource);
	 *
	 * @param datasource the data source
	 * @return a list of primary key names
	 * @throws ServoyException
	 */
	@JSFunction
	public String[] getDatasourcePKs(String datasource) throws ServoyException
	{
		app.checkAuthorized();
		List<String> listOfPrimaryKeyNames = new ArrayList<String>();
		ITable table = app.getFoundSetManager().getTable(datasource);
		if (table != null)
		{
			table.getRowIdentColumnNames().forEachRemaining(listOfPrimaryKeyNames::add);
		}

		return listOfPrimaryKeyNames.toArray(new String[0]);
	}

	/**
	 * Performs a sql query with a query builder object.
	 * Will throw an exception if anything did go wrong when executing the query.
	 * Will use any data filter defined on table.
	 *
	 * @sample
	 *  var dataset = servoyApi.getDataSetByQuery(qbselect, 10);
	 *
	 * @param query QBSelect query.
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 *
	 * @return The JSDataSet containing the results of the query.
	 */
	@JSFunction
	public JSDataSet getDataSetByQuery(QBSelect query, Number max_returned_rows)
	{
		int _max_returned_rows = Utils.getAsInteger(max_returned_rows);

		String serverName = DataSourceUtils.getDataSourceServerName(query.getDataSource());

		if (serverName == null)
			throw new RuntimeException(new ServoyException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { query.getDataSource() }));
		QuerySelect select = query.build();

		try
		{
			return new JSDataSet(app, ((FoundSetManager)app.getFoundSetManager()).getDataSetByQuery(serverName, select,
				true, _max_returned_rows));
		}
		catch (ServoyException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates an empty JSDataSet
	 *
	 * @return an empty JSDataSet
	 */
	@JSFunction
	public JSDataSet createEmptyDataSet()
	{
		return new JSDataSet(app);
	}

	/**
	 * Add a filter parameter that is permanent per user session to limit a specified foundset of records.
	 * This is similar as calling foundset.js_addFoundSetFilterParam, but the main difference is that this
	 * works also on related foundsets.
	 *
	 * @param foundset The foundset to add the filter param/query to
	 * @param query The query repesenting the filter
	 * @param filterName a name given to this foundset filter
	 *
	 * @see Foundset.js_addFoundSetFilterParam
	 *
	 * @return true if the filter parameter was successfully added, false otherwise.
	 */
	@JSFunction
	public boolean addFoundSetFilterParam(FoundSet foundset, QBSelect query, String filterName)
	{
		return foundset.addFoundSetFilterParam(query, filterName);
	}

	/**
	 * This will create a JSEvent filled with component information.
	 *
	 * @sample
	 * var event = servoyApi.createJSEvent();
	 *
	 * @return the jsevent
	 */
	@JSFunction
	public JSEvent createJSEvent()
	{
		JSEvent event = new JSEvent();
		event.setTimestamp(new Date());
		event.setSource(new RuntimeWebComponent(this.component, this.component.getSpecification()));
		event.setFormName(this.component.findParent(IWebFormUI.class).getController().getName());
		event.setElementName(this.component.getFormElement().getRawName());
		return event;
	}
}
