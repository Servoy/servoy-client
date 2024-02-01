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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.sablo.services.server.FormServiceHandler;
import org.sablo.specification.IFunctionParameters;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.util.ValueReference;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.ExitScriptException;
import com.servoy.j2db.IBasicFormManager.History;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.InitialToJSONConverter;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;
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

	@SuppressWarnings("nls")
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
					String propertyName = args.getString("property"); // this can contain dots or square brackets in case of nested DPs

					// now... I think... the following code avoids setting "" from client into a DP that is already null
					if (changes.length() > 0)
					{
						Iterator<String> keys = changes.keys();
						while (keys.hasNext())
						{
							String key = keys.next();
							Object value = changes.get(key);
							if ("".equals(value) && form.getDataAdapterList().getValueObject(form.getDataAdapterList().getRecord(),
								form.getDataAdapterList().getDataProviderID(webComponent, propertyName)) == null)
							{
								keys.remove();
							}
						}
						args.put("changes", changes);
					}

					// now change the value of the DP prop. and then push the changed DP value to Record
					if (changes.length() > 0)
					{
						Object propValue = webComponent.getProperty(propertyName);
						if (propValue instanceof FoundsetLinkedTypeSabloValue< ? , ? >)
							((FoundsetLinkedTypeSabloValue)propValue).setApplyingDPValueFromClient(true);
						try
						{
							dataPush(args);
							form.getDataAdapterList().pushChanges(webComponent, propertyName, args.optString("fslRowID", null));
						}
						finally
						{
							if (propValue instanceof FoundsetLinkedTypeSabloValue< ? , ? >)
								((FoundsetLinkedTypeSabloValue)propValue).setApplyingDPValueFromClient(false);
						}
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
					IWebFormUI form = getApplication().getFormManager().getFormAndSetCurrentWindow(formName).getFormUI();
					if (!form.getController().isFormVisible())
					{
						Debug.warn("execute inline script called on a none visible form: " + formName + " call stopped, returning null");
						return null;
					}
					if (form.getController().getDesignModeCallbacks() != null)
					{
						// ignoring all calls from the client
						return null;
					}
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
						Debug.error("Cannot execute inline script: " + args, ex);
					}
				}
				break;
			}

			case "formvisibility" :
			{
				IWebFormController parentForm = null;
				IWebFormController controller = null;
				String formName = args.optString("formname", null);

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
				if (controller != null && !controller.isDestroyed())
				{
					boolean isVisible = args.getBoolean("visible");
					WebFormComponent containerComponent = null;
					String relationName = null;
					if (parentForm != null)
					{
						containerComponent = parentForm.getFormUI().getWebComponent(args.getString("bean"));
					}
					if (isVisible)
					{
						// if the parent container form and component are given, then check for those;
						// note: the show operation (isVisible = true in if above) of a form cannot be denied, so we can update things before the call to controller.notifyVisible below
						if (parentForm != null && containerComponent != null)
						{
							if (!parentForm.isFormVisible() || !containerComponent.isVisible())
							{
								throw new IllegalAccessException("Can't show form " + formName + " when the parent form " + parentForm +
									" or the component + " + containerComponent + " is not visible");
							}
							relationName = NGClientWindow.getCurrentWindow().isVisibleAllowed(formName, args.optString("relation", null),
								containerComponent.getFormElement());
							containerComponent.updateVisibleForm(controller.getFormUI(), isVisible, args.optInt("formIndex"));
						}
						else
						{
							// else this form can only be allowed for the "null" component
							relationName = NGClientWindow.getCurrentWindow().isVisibleAllowed(formName, args.optString("relation", null), null);
						}

						if (parentForm != null && relationName != null)
						{
							IFoundSetInternal parentFs = parentForm.getFormModel();
							IRecordInternal selectedRecord = parentFs.getRecord(parentFs.getSelectedIndex());
							if (selectedRecord != null)
							{
								try
								{
									controller.loadRecords(selectedRecord.getRelatedFoundSet(relationName));
								}
								catch (RuntimeException re)
								{
									throw new RuntimeException("Can't load records on form " + controller.getName() + ", of parent record: " +
										selectedRecord + " with relation " + relationName + " for parent form  " + parentForm + " and bean " +
										containerComponent, re);
								}
							}
							else
							{
								// no selected record, then use prototype so we can get global relations
								try
								{
									controller.loadRecords(parentFs.getPrototypeState().getRelatedFoundSet(relationName));
								}
								catch (RuntimeException re)
								{
									throw new RuntimeException("Can't load records on form " + controller.getName() + ", of parent record: " +
										selectedRecord + " with relation " + relationName + " for parent form  " + parentForm + " and bean " +
										containerComponent, re);
								}

							}
						}
					}
					ok = controller.notifyVisible(isVisible, invokeLaterRunnables, true);
					if (ok && parentForm != null)
					{
						if (!isVisible && containerComponent != null)
						{
							containerComponent.updateVisibleForm(controller.getFormUI(), isVisible, args.optInt("formIndex"));
						}
						if (isVisible)
						{
							// was shown
							parentForm.getFormUI().getDataAdapterList().addVisibleChildForm(controller, relationName, true);
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
				if (form != null && controller.isFormVisible())
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
					checkAndSetParentWindow(formName); // maybe the form was destroyed because of memory limits, make sure we set the parent window
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
				String formName = args.optString("formName", null);
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
						IRecordInternal formRecord = null;
						if (formName != null)
						{
							IWebFormUI form = getApplication().getFormManager().getForm(formName).getFormUI();
							formRecord = form.getDataAdapterList().getRecord();
						}

						LookupValueList lookup = new LookupValueList(val, getApplication(),
							ComponentFactory.getFallbackValueList(getApplication(), null, Types.OTHER, null, val), null, formRecord);
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

				IWebFormUI form = null;
				INGFormManager fm = websocketSession.getClient().getFormManager();
				if (fm != null)
				{
					IWebFormController formController = fm.getCachedFormController(formName);
					if (formController == null)
					{
						// if form is already destroyed / not leased in form manager - log it and ignore in this situation;
						// this did happen with a datagrid that send a "filterMyFoundset" server side api call (which was marked in spec to
						// be allowed even for non-visible forms for some reason) after it was no longer visible due to a default tabpanel tab switch
						// that also ended up destroying this form...
						// and this resulted in an unwanted re-load of that form and an error in server side scripting code
						log.info(
							"callServerSideApi for a form that is not in use (or does not exist, or has already been destroyed); form: '" + formName + "'");
					}
					else
					{
						form = formController.getFormUI();
						if (form != null)
						{
							if (form.getController().getDesignModeCallbacks() != null)
							{
								// ignoring all calls from the client
								return null;
							}
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
									WebObjectFunctionDefinition functionSpec = (componentSpec != null
										? componentSpec.getInternalApiFunction(componentMethodName)
										: null);

									if (functionSpec == null)
									{
										log.warn("trying to call a function that does not exist in .spec of component '" +
											webComponent.getName() + " with spec name: " + (componentSpec != null ? componentSpec.getName() : null));
										throw new RuntimeException("trying to call a function that does not exist in .spec of component '" +
											webComponent.getName() + " with spec name: " + (componentSpec != null ? componentSpec.getName() : null));
									}
									else
									{
										// verify if component is accessible due to security options
										webComponent.checkMethodExecutionSecurityAccess(functionSpec);

										if (!runtimeComponent.getComponent().isVisible() || !form.getController().isFormVisible())
										{
											List<String> allowAccessProperties = null;
											if (functionSpec != null)
											{
												String allowAccess = functionSpec.getAllowAccess();
												if (allowAccess != null)
												{
													allowAccessProperties = Arrays.asList(allowAccess.split(","));
												}
											}

											if (!runtimeComponent.getComponent().isVisible())
											{
												boolean allowAccessComponentVisibility = false;
												if (allowAccessProperties != null)
												{
													for (String p : allowAccessProperties)
													{
														allowAccessComponentVisibility = allowAccessComponentVisibility ||
															runtimeComponent.getComponent().isVisibilityProperty(p);
													}
												}

												if (!allowAccessComponentVisibility)
												{
													log.warn(
														"callServerSideApi called on a none visible component: " + runtimeComponent +
															" call stopped, returning null");
													return null; // TODO shouldn't we throw an exception here as well so that the client-side promise for this server side call would be rejected, not resolved?
												}
											}
											else
											{
												if (allowAccessProperties == null || allowAccessProperties.indexOf("visible") == -1)
												{
													log.warn("callServerSideApi called on a none visible form: " + formName + " call stopped, returning null");
													return null; // TODO shouldn't we throw an exception here as well so that the client-side promise for this server side call would be rejected, not resolved?
												}
											}
										}

										IFunctionParameters argumentPDs = (functionSpec != null ? functionSpec.getParameters() : null);

										// apply conversion
										Object[] arrayOfJavaConvertedMethodArgs = new Object[methodArguments.length()];
										for (int i = 0; i < methodArguments.length(); i++)
										{
											arrayOfJavaConvertedMethodArgs[i] = JSONUtils.fromJSON(null, methodArguments.get(i),
												(argumentPDs != null && argumentPDs.getDefinedArgsCount() > i) ? argumentPDs.getParameterDefinition(i) : null,
												new BrowserConverterContext(webComponent, PushToServerEnum.allow), new ValueReference<Boolean>(false));
										}

										Object retVal = runtimeComponent.executeScopeFunction(functionSpec, arrayOfJavaConvertedMethodArgs);
										if (functionSpec != null && functionSpec.getReturnType() != null)
										{
											EmbeddableJSONWriter w = new EmbeddableJSONWriter(true);
											FullValueToJSONConverter.INSTANCE.toJSONValue(w, null, retVal, functionSpec.getReturnType(),
												new BrowserConverterContext(webComponent, PushToServerEnum.reject));
											retVal = w;
										}
										return retVal;
									}
								}
								else
								{
									log.warn("callServerSideApi for unknown bean '" + args.getString("beanname") + "' of form '" + formName + "'");
									throw new RuntimeException(
										"callServerSideApi for unknown bean '" + args.getString("beanname") + "' of form '" + formName + "'");
								}
							}
							else
							{
								log.warn("callServerSideApi for unknown bean '" + args.getString("beanname") + "' of form '" + formName + "'");
								throw new RuntimeException(
									"callServerSideApi for unknown bean '" + args.getString("beanname") + "' of form '" + formName + "'");
							}
						}
						else
						{
							log.warn("callServerSideApi failed; null form UI for form controller of '" + formName + "'");
							throw new RuntimeException("callServerSideApi failed; null form UI for form controller of '" + formName + "'");
						}
					}
				}
				else log.warn("callServerSideApi cannot access form manager; is client shut down? (form: '" + formName + "', comp: '" +
					args.optString("beanname") + "', method: '" + args.optString("methodName") + "'");

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
	protected JSONString executeEvent(JSONObject obj) throws Exception
	{
		String formName = obj.optString("formname");
		if (formName != null)
		{
			getApplication().getFormManager().getFormAndSetCurrentWindow(formName);
			IWebFormUI form = getApplication().getFormManager().getFormAndSetCurrentWindow(formName).getFormUI();
			if (form.getController().getDesignModeCallbacks() != null)
			{
				// ignoring all calls from the client
				return null;
			}
		}
		JSONString retValue = super.executeEvent(obj);
		if (getApplication().getFoundSetManager() != null && getApplication().getFoundSetManager().hasTransaction())
		{
			log.warn("Transaction still active after event was executed: " + ServoyJSONObject.toString(obj, false, false, false) +
				". This is highly discouraged and transaction will be rolled back in future releases.");
		}
		return retValue;
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
		if ("formLoaded".equals(methodName) || "formUnloaded".equals(methodName))
		{
			return EVENT_LEVEL_INITIAL_FORM_DATA_REQUEST; // allow it to run on dispatch thread even if some API call is waiting (suspended)
		}
		else if ("formvisibility".equals(methodName) && arguments.optBoolean("visible"))
		{
			// only allow formvisibilty (to true) when the form is not loaded yet, or it is not in a changing state (DAL.setRecord)
			// if DAL.setRecord is happening then we need to postpone it.
			String formName = arguments.optString("formname");

			IWebFormController cachedFormController = getApplication().getFormManager().getCachedFormController(formName);
			if (cachedFormController == null || !cachedFormController.getFormUI().isChanging())
			{
				return EVENT_LEVEL_INITIAL_FORM_DATA_REQUEST;
			}
		}
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
