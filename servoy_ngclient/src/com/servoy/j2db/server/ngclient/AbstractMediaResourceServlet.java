/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.j2db.server.ngclient;


import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileCleaningTracker;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.scripting.JSMap;
import com.servoy.j2db.scripting.JSUpload;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.server.ngclient.property.ComponentTypeConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.ui.IMediaFieldConstants;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * @since 2021.12
 *
 */
public abstract class AbstractMediaResourceServlet extends HttpServlet
{
	/**
	 * constant for calling a service, should be in sync with servoy.ts generateServiceUploadUrl() function
	 */
	private static final String SERVICE_UPLOAD = "svy_services";

	protected static final FileCleaningTracker FILE_CLEANING_TRACKER = new FileCleaningTracker();

	/**
	 *
	 */
	public AbstractMediaResourceServlet()
	{
		super();
	}

	@Override
	public void destroy()
	{
		super.destroy();
		FILE_CLEANING_TRACKER.exitWhenFinished();
	}

	protected IApplication getClient(HttpServletRequest request, int clientnr)
	{
		INGClientWebsocketSession wsSession = getSession(request, clientnr);
		return wsSession != null ? wsSession.getClient() : null;
	}

	protected INGClientWebsocketSession getSession(HttpServletRequest request, int clientnr)
	{
		// try to look it up as clientnr. (solution model)
		HttpSession httpSession = request.getSession(false);
		if (httpSession != null)
		{
			return (INGClientWebsocketSession)WebsocketSessionManager.getSession(WebsocketSessionFactory.CLIENT_ENDPOINT, httpSession, clientnr);
		}
		return null;
	}

	/**
	 * @param req
	 * @param paths
	 * @param wsSession
	 * @param formFields
	 * @param item
	 * @return
	 */
	protected boolean callClient(HttpServletRequest req, String[] paths, final INGClientWebsocketSession wsSession, final JSMap<String, String> fieldsMap,
		FileItem item)
	{
		final String formName = paths.length >= 5 ? paths[2] : null;
		final String elementName = paths.length >= 5 ? paths[3] : null;
		final String propertyName = paths.length >= 5 ? paths[4] : null;
		final String rowID = paths.length >= 6 ? paths[5] : null;

		if (formName != null && elementName != null && propertyName != null)
		{

			wsSession.getEventDispatcher().addEvent(new Runnable()
			{
				@SuppressWarnings("nls")
				@Override
				public void run()
				{
					if (formName.equals(SERVICE_UPLOAD))
					{
						Scriptable plugins = (Scriptable)wsSession.getClient().getScriptEngine().getSolutionScope().get(
							IExecutingEnviroment.TOPLEVEL_PLUGINS, null);
						Scriptable plugin = (Scriptable)plugins.get(elementName, plugins);
						if (plugin != null)
						{
							Object func = plugin.get(propertyName, plugin);
							if (func instanceof Function)
							{
								Context context = Context.enter();
								try
								{
									((Function)func).call(context, plugin, plugin, new Object[] { new JSUpload(item, fieldsMap) });
								}
								finally
								{
									Context.exit();
								}
							}
						}
					}
					else
					{
						final Map<String, Object> fileData = new HashMap<String, Object>();
						fileData.put(IMediaFieldConstants.FILENAME, item.getName());
						fileData.put(IMediaFieldConstants.MIMETYPE, item.getContentType());
						IWebFormUI form = wsSession.getClient().getFormManager().getForm(formName).getFormUI();
						if (form == null)
						{
							Debug.error("uploading data for:  " + formName + ", element: " + elementName + ", property: " + propertyName +
								" but form is not found, data: " + fileData);
							return;
						}
						WebFormComponent webComponent = form.getWebComponent(elementName);
						if (webComponent == null)
						{
							Debug.error("uploading data for:  " + formName + ", element: " + elementName + ", property: " + propertyName +
								" but component  is not found, data: " + fileData);
							return;
						}
						// if the property is a event handler  then just call that event with the FileUploadData as the argument
						if (webComponent.hasEvent(propertyName))
						{
							try
							{
								JSEvent event = new JSEvent();
								event.setElementName(elementName);
								event.setFormName(formName);
								event.setName("file-upload");
								event.setModifiers(0);
								event.setSource(new RuntimeWebComponent(webComponent, webComponent.getSpecification()));
								event.setTimestamp(new Date());
								event.setType("file-upload");
								webComponent.executeEvent(propertyName, new Object[] { new JSUpload(item, fieldsMap), event });
							}
							catch (Exception e)
							{
								Debug.error("Error calling the upload event handler " + propertyName + "   of " + webComponent, e);
							}
						}
						else
						{
							boolean isListFormComponent = false;
							WebObjectSpecification spec = webComponent.getParent().getSpecification();
							if (spec != null)
							{
								Collection<PropertyDescription> formComponentProperties = spec
									.getProperties(FormComponentPropertyType.INSTANCE);
								if (formComponentProperties != null)
								{
									for (PropertyDescription property : formComponentProperties)
									{
										if (property.getConfig() instanceof ComponentTypeConfig &&
											((ComponentTypeConfig)property.getConfig()).forFoundset != null)
										{
											isListFormComponent = true;
											FoundsetTypeSabloValue foundsetPropertyValue = (FoundsetTypeSabloValue)webComponent
												.getParent().getProperty(((ComponentTypeConfig)property.getConfig()).forFoundset);
											if (rowID != null)
											{
												IFoundSetInternal foundset = foundsetPropertyValue.getFoundset();

												if (foundset != null)
												{
													int recordIndex = foundset.getRecordIndex(rowID, foundsetPropertyValue.getRecordIndexHint());

													if (recordIndex != -1)
													{
														foundsetPropertyValue.getDataAdapterList()
															.setRecordQuietly(foundset.getRecord(recordIndex));
													}
												}
											}
											fileData.put("", item.get());
											foundsetPropertyValue.getDataAdapterList().pushChanges(webComponent, propertyName, fileData,
												null);
											foundsetPropertyValue.setDataAdapterListToSelectedRecord();
											break;
										}

									}
								}
							}
							if (!isListFormComponent)
							{
								fileData.put("", item.get());
								form.getDataAdapterList().pushChanges(webComponent, propertyName, fileData, null);
							}
						}
					}
				}
			}, IEventDispatcher.EVENT_LEVEL_SYNC_API_CALL);

			return true;
		}
		return false;
	}

}