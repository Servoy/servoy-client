/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.sablo.services.server.FormServiceHandler;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.util.ValueReference;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.ExitScriptException;
import com.servoy.j2db.IBasicFormManager.History;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.InitialToJSONConverter;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;


/**
 * FormService implementation to handle methods at form level.
 *
 * @author rgansevles
 *
 */
public class NGFormServiceHandler extends FormServiceHandler
{
	private final INGClientWebsocketSession websocketSession;

	public NGFormServiceHandler(INGClientWebsocketSession websocketSession)
	{
		this.websocketSession = websocketSession;
	}

	protected INGApplication getApplication()
	{
		return websocketSession.getClient();
	}

	@Override
	protected IToJSONConverter<IBrowserConverterContext> getInitialRequestDataConverter()
	{
		return InitialToJSONConverter.INSTANCE;
	}

	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		switch (methodName)
		{
			case "svyPush" :
			{
				String formName = args.getString("formname");
				IWebFormUI form = (IWebFormUI)NGClientWindow.getCurrentWindow().getForm(formName);
				if (form == null)
				{
					log.warn("svyPush for unknown form '" + formName + "'");
				}
				else
				{
					JSONObject changes = args.getJSONObject("changes");
					WebFormComponent webComponent = form.getWebComponent(args.getString("beanname"));
					if (changes.length() > 0)
					{
						Iterator<String> keys = changes.keys();
						while (keys.hasNext())
						{
							String key = keys.next();
							Object value = changes.get(key);
							if ("".equals(value) && form.getDataAdapterList().getValueObject(form.getDataAdapterList().getRecord(),
								form.getDataAdapterList().getDataProviderID(webComponent, args.getString("property"))) == null)
							{
								keys.remove();
							}
						}
						args.put("changes", changes);
					}

					if (changes.length() > 0)
					{
						dataPush(args);
						form.getDataAdapterList().pushChanges(webComponent, args.getString("property"), args.optString("fslRowID", null));
					}
				}
				break;
			}

			case "startEdit" :
			{
				String formName = args.getString("formname");
				IWebFormUI form = getApplication().getFormManager().getFormAndSetCurrentWindow(formName).getFormUI();
				if (form == null)
				{
					log.error("startEdit for unknown form '" + formName + "'");
				}
				else
				{
					form.getDataAdapterList().startEdit(form.getWebComponent(args.optString("beanname")), args.optString("property"),
						args.optString("fslRowID", null));
				}

				break;
			}
			case "executeInlineScript" :
			{
				try
				{
					String formName = args.optString("formname", null);
					if (formName == null)
					{
						formName = getApplication().getFormManager().getCurrentForm().getName();
					}
					else
					{
						formName = SecuritySupport.decrypt(Settings.getInstance(), formName);
					}
					IWebFormUI form = getApplication().getFormManager().getFormAndSetCurrentWindow(formName).getFormUI();
					getApplication().updateLastAccessed();
					Object retVal = form.getDataAdapterList().executeInlineScript(args.optString("script"), args.optJSONObject("params"),
						args.optJSONArray("params"));
					// convert it from Rhino to sablo value; it will use the default conversion as we don't provide a PD/context/webobject
					return NGConversions.INSTANCE.convertRhinoToSabloComponentValue(retVal, null, null, null); // we don't return TypedData here as we rely on default to browser JSON conversion as well
				}
				catch (Exception ex)
				{
					if (!(ex instanceof ExitScriptException))
					{
						Debug.error("Cannot execute inline script", ex);
					}
				}
				break;
			}

			case "formvisibility" :
			{
				IWebFormController parentForm = null;
				IWebFormController controller = null;
				String formName = args.optString("formname");

				checkAndSetParentWindow(formName);
				if (args.has("parentForm") && !args.isNull("parentForm"))
				{
					checkAndSetParentWindow(args.optString("parentForm"));
					parentForm = getApplication().getFormManager().getFormAndSetCurrentWindow(args.optString("parentForm"));
					controller = getApplication().getFormManager().getForm(formName);
				}
				else
				{
					controller = getApplication().getFormManager().getFormAndSetCurrentWindow(formName);
				}

				boolean ok = true;
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
				if (controller != null)
				{
					boolean isVisible = args.getBoolean("visible");
					WebFormComponent containerComponent = null;
					if (parentForm != null)
					{
						containerComponent = parentForm.getFormUI().getWebComponent(args.getString("bean"));

						if (isVisible && containerComponent != null)
						{
							containerComponent.updateVisibleForm(controller.getFormUI(), isVisible, args.optInt("formIndex"));
						}
					}
					ok = controller.notifyVisible(isVisible, invokeLaterRunnables);
					if (ok && parentForm != null)
					{
						if (!isVisible && containerComponent != null)
						{
							containerComponent.updateVisibleForm(controller.getFormUI(), isVisible, args.optInt("formIndex"));
						}
						String relation = null;
						if (isVisible && args.has("relation") && !args.isNull("relation") && !"".equals(args.getString("relation")))
						{
							relation = args.getString("relation");
							FoundSet parentFs = parentForm.getFormModel();
							IRecordInternal selectedRecord = (IRecordInternal)parentFs.getSelectedRecord();
							if (selectedRecord != null)
							{
								controller.loadRecords(selectedRecord.getRelatedFoundSet(relation));
							}
							else
							{
								// no selected record, then use prototype so we can get global relations
								controller.loadRecords(parentFs.getPrototypeState().getRelatedFoundSet(relation));
							}
						}

						if (isVisible)
						{
							// was shown
							parentForm.getFormUI().getDataAdapterList().addVisibleChildForm(controller, relation, true);
						}
						else
						{
							// was hidden
							parentForm.getFormUI().getDataAdapterList().removeVisibleChildForm(controller, true);
						}
					}
				}

				// if this call has an show object, then we need to directly show that form right away
				if (ok && args.has("show") && args.getJSONObject("show").has("formname"))
				{
					JSONObject showing = args.getJSONObject("show");
					showing.put("visible", true);
					if (args.has("parentForm"))
					{
						showing.put("parentForm", args.getString("parentForm"));
						showing.put("bean", args.getString("bean"));
					}
					executeMethod("formvisibility", showing);
					// send the changes before returning the value, because else the values will be still
					// a bit later then the "ok" of the form can be hidden.
					CurrentWindow.get().sendChanges();
				}
				Utils.invokeAndWait(getApplication(), invokeLaterRunnables);
				Form form = getApplication().getFormManager().getPossibleForm(formName);
				if (form != null)
					NGClientWindow.getCurrentWindow().touchForm(getApplication().getFlattenedSolution().getFlattenedForm(form), formName, true, true);

				return Boolean.valueOf(ok);
			}

			case "formLoaded" :
			{
				NGClientWindow.getCurrentWindow().setFormResolved(args.optString("formname"), true);
				break;
			}

			case "formUnloaded" :
			{
				NGClientWindow.getCurrentWindow().setFormResolved(args.optString("formname"), false);
				break;
			}

			case "gotoform" :
			{
				String formName = args.optString("formname");
				IWebFormController form = getApplication().getFormManager().getForm(formName);
				if (form != null)
				{
					String windowName = form.getFormUI().getParentWindowName();
					NGRuntimeWindow window = null;
					if (windowName != null && (window = getApplication().getRuntimeWindowManager().getWindow(windowName)) != null)
					{
						History history = window.getHistory();
						if (history.getFormIndex(formName) != -1)
						{
							history.go(history.getFormIndex(formName) - history.getIndex());
						}
						else
						{
							Debug.log("Form " + formName + " was not found in the history of window " + windowName);
						}
					}
					else
					{
						Debug.error("Window was not found for form " + formName);
					}
				}
				else
				{
					Debug.error("Form " + formName + " was not found");
				}
				break;
			}

			case "getValuelistDisplayValue" :
			{
				Object realValue = args.get("realValue");
				Object valuelistID = args.get("valuelist");
				int id = Utils.getAsInteger(valuelistID);
				ValueList val = getApplication().getFlattenedSolution().getValueList(id);
				if (val != null)
				{
					IValueList realValueList = ComponentFactory.getRealValueList(getApplication(), val, true, Types.OTHER, null, null);
					if (realValueList.realValueIndexOf(realValue) != -1)
					{
						try
						{
							return realValueList.getElementAt(realValueList.realValueIndexOf(realValue));
						}
						catch (Exception ex)
						{
							Debug.error(ex);
							return realValue;
						}
					}
					if (realValueList instanceof DBValueList)
					{
						LookupValueList lookup = new LookupValueList(val, getApplication(),
							ComponentFactory.getFallbackValueList(getApplication(), null, Types.OTHER, null, val), null);
						Object displayValue = null;
						if (lookup.realValueIndexOf(realValue) != -1)
						{
							displayValue = lookup.getElementAt(lookup.realValueIndexOf(realValue));
						}
						lookup.deregister();
						return displayValue;
					}
				}
				break;
			}

			case "callServerSideApi" :
			{
				String formName = args.getString("formname");
				IWebFormUI form = (IWebFormUI)NGClientWindow.getCurrentWindow().getForm(formName);
				if (form != null)
				{
					WebFormComponent webComponent = form.getWebComponent(args.getString("beanname"));
					if (webComponent != null)
					{
						RuntimeWebComponent runtimeComponent = null;
						RuntimeWebComponent[] webComponentElements = form.getController().getWebComponentElements();
						for (RuntimeWebComponent runtimeWebComponent : webComponentElements)
						{
							if (runtimeWebComponent.getComponent() == webComponent)
							{
								runtimeComponent = runtimeWebComponent;
								break;
							}
						}

						if (runtimeComponent != null)
						{
							JSONArray methodArguments = args.getJSONArray("args");
							String componentMethodName = args.getString("methodName");

							// apply browser to sablo java value conversion - using server-side-API definition from the component's spec file if available (otherwise use default conversion)
							// the call to runtimeComponent.executeScopeFunction will do the java to Rhino one

							// find spec for method
							WebObjectSpecification componentSpec = webComponent.getSpecification();
							WebObjectFunctionDefinition functionSpec = (componentSpec != null ? componentSpec.getApiFunction(componentMethodName) : null);
							if (functionSpec == null)
							{
								functionSpec = (componentSpec != null ? componentSpec.getServerApiFunction(componentMethodName) : null);
							}
							List<PropertyDescription> argumentPDs = (functionSpec != null ? functionSpec.getParameters() : null);

							// apply conversion
							Object[] arrayOfJavaConvertedMethodArgs = new Object[methodArguments.length()];
							for (int i = 0; i < methodArguments.length(); i++)
							{
								arrayOfJavaConvertedMethodArgs[i] = JSONUtils.fromJSON(null, methodArguments.get(i),
									(argumentPDs != null && argumentPDs.size() > i) ? argumentPDs.get(i) : null,
									new BrowserConverterContext(webComponent, PushToServerEnum.allow), new ValueReference<Boolean>(false));
							}

							Object retVal = runtimeComponent.executeScopeFunction(functionSpec, arrayOfJavaConvertedMethodArgs);

							if (functionSpec != null && functionSpec.getReturnType() != null)
							{
								retVal = new TypedData<Object>(retVal, functionSpec.getReturnType()); // this means that when this return value is sent to client it will be converted to browser JSON correctly - if we give it the type
							}
							return retVal;
						}
						else
						{
							log.warn("callServerSideApi for unknown bean '" + args.getString("beanname") + "' of form '" + formName + "'");
						}
					}
					else
					{
						log.warn("callServerSideApi for unknown bean '" + args.getString("beanname") + "' of form '" + formName + "'");
					}
				}
				else
				{
					log.warn("callServerSideApi for unknown form '" + formName + "'");
				}
				break;
			}

			case "performFind" :
			{
				String formName = args.optString("formname");
				boolean clear = args.optBoolean("clear");
				boolean reduce = args.optBoolean("reduce");
				boolean showDialogOnNoResults = args.optBoolean("showDialogOnNoResults");
				IWebFormController controller = getApplication().getFormManager().getForm(formName);
				if (controller != null)
				{
					controller.performFind(clear, reduce, showDialogOnNoResults);
				}
				break;
			}

			default :
			{
				return super.executeMethod(methodName, args);
			}
		}

		return null;
	}

	@Override
	protected Object executeEvent(JSONObject obj) throws Exception
	{
		String formName = obj.optString("formname");
		if (formName != null) getApplication().getFormManager().getFormAndSetCurrentWindow(formName);
		return super.executeEvent(obj);
	}


	@Override
	protected JSONString requestData(String formName) throws JSONException
	{
		getApplication().getFormManager().getFormAndSetCurrentWindow(formName);
		return super.requestData(formName);
	}

	@Override
	public int getMethodEventThreadLevel(String methodName, JSONObject arguments, int dontCareLevel)
	{
		if ("formLoaded".equals(methodName) || ("formvisibility".equals(methodName) && arguments.optBoolean("visible")))
			return EVENT_LEVEL_INITIAL_FORM_DATA_REQUEST; // allow it to run on dispatch thread even if some API call is waiting (suspended)
		return super.getMethodEventThreadLevel(methodName, arguments, dontCareLevel);
	}

	private void checkAndSetParentWindow(String formName)
	{
		if (formName != null)
		{
			INGFormManager fm = websocketSession.getClient().getFormManager();
			if (fm.getForm(formName) != null)
			{
				IWebFormUI formUI = fm.getForm(formName).getFormUI();
				if (formUI != null && formUI.getParentContainer() == null)
				{
					NGRuntimeWindowManager wm = websocketSession.getClient().getRuntimeWindowManager();
					String currentWindowName = wm.getCurrentWindow().getName();
					if (currentWindowName == null)
					{
						currentWindowName = wm.getMainApplicationWindow().getName();
					}
					formUI.setParentWindowName(currentWindowName);
				}
			}
		}
	}

}
