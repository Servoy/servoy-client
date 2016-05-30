/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.component;

import java.awt.Point;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.base.persistence.constants.IContentSpecConstantsBase;
import com.servoy.base.scripting.api.IJSEvent;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;


/**
 * @author lvostinar
 *
 */
public class EventExecutor
{
	private final IWebFormController formController;

	public EventExecutor(IWebFormController formController)
	{
		this.formController = formController;
	}

	public Object executeEvent(WebComponent component, String eventType, int eventId, Object[] eventArgs)
	{
		Scriptable scope = null;
		Function f = null;

		Object[] newargs = eventArgs != null ? Arrays.copyOf(eventArgs, eventArgs.length) : null;

		if (eventId > 0)
		{
			FormScope formScope = formController.getFormScope();
			String name = formScope.getFunctionName(new Integer(eventId));
			if (name != null)
			{
				f = formScope.getFunctionByName(name);
				if (f != null && f != Scriptable.NOT_FOUND)
				{
					scope = formScope;
				}
			}

			if (scope == null)
			{
				ScriptMethod scriptMethod = formController.getApplication().getFlattenedSolution().getScriptMethod(eventId);
				if (scriptMethod != null)
				{
					scope = formController.getApplication().getScriptEngine().getScopesScope().getGlobalScope(scriptMethod.getScopeName());
				}
				if (scope != null)
				{
					name = ((GlobalScope)scope).getFunctionName(new Integer(eventId));
					f = ((GlobalScope)scope).getFunctionByName(name);
				}
			}
			if (name == null && scope == null && formController.getFormModel() != null)
			{
				try
				{
					ScriptMethod method = AbstractBase.selectById(
						formController.getApplication().getFlattenedSolution().getFoundsetMethods(formController.getTable(), false).iterator(), eventId);
					if (method != null)
					{
						name = method.getName();
						scope = formController.getFormModel();
						f = (Function)scope.getPrototype().get(name, scope);
					}
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}

		}
		if (formController.isInFindMode() && !Utils.getAsBoolean(f.get("_AllowToRunInFind_", f))) return null;

		if (newargs != null)
		{
			for (int i = 0; i < newargs.length; i++)
			{
				if (newargs[i] instanceof JSONObject && "event".equals(((JSONObject)newargs[i]).optString("type")))
				{
					JSONObject json = (JSONObject)newargs[i];
					JSEvent event = new JSEvent();
					event.setType(getEventType(eventType));
					event.setFormName(formController.getName());
					// the names used in scripting are the actual persist names
					// not the processed names (web friendly)
					String componentName = component instanceof WebFormComponent ? ((WebFormComponent)component).getFormElement().getRawName()
						: component.getName();
					event.setElementName(componentName);
					FormScope formScope = formController.getFormScope();
					if (formScope != null)
					{
						ElementScope elementsScope = (ElementScope)formScope.get("elements", null);
						if (elementsScope != null)
						{
							Object scriptableElement = componentName != null ? elementsScope.get(componentName, null) : null;
							if (scriptableElement != null && scriptableElement != Scriptable.NOT_FOUND)
							{
								event.setSource(scriptableElement);
							}
							else if (component instanceof WebFormComponent)
							{
								// quickly create a scriptable wrappar around the component so that the source can be set to a value that we expect.
								FormElement fe = ((WebFormComponent)component).getFormElement();
								RuntimeWebComponent runtimeComponent = new RuntimeWebComponent((WebFormComponent)component, component.getSpecification());
								if (fe.isLegacy() || ((fe.getForm().getView() == IForm.LIST_VIEW || fe.getForm().getView() == FormController.LOCKED_LIST_VIEW ||
									fe.getForm().getView() == FormController.TABLE_VIEW || fe.getForm().getView() == FormController.LOCKED_TABLE_VIEW) &&
									fe.getTypeName().startsWith("svy-")))
								{
									// add legacy behavior
									runtimeComponent.setPrototype(new RuntimeLegacyComponent((WebFormComponent)component));
								}
								event.setSource(runtimeComponent);
							}
						}
					}
					try
					{
						event.setTimestamp(new Timestamp(json.getLong("timestamp")));
						if (json.has("x")) event.setLocation(new Point(json.getInt("x"), json.getInt("y")));
						if (json.has("modifiers")) event.setModifiers(json.getInt("modifiers"));
					}
					catch (Exception ex)
					{
						Debug.error("error setting event properties from " + json + ", for component: " + componentName, ex);
					}
					newargs[i] = event;
				}
				else
				{ //try to convert the received arguments
					WebObjectFunctionDefinition propertyDesc = component.getSpecification().getHandler(eventType);
					List<PropertyDescription> parameters = propertyDesc.getParameters();
					PropertyDescription parameterPropertyDescription = parameters.get(i);


					ValueReference<Boolean> returnValueAdjustedIncommingValueForIndex = new ValueReference<Boolean>(Boolean.FALSE);
					newargs[i] = NGConversions.INSTANCE.convertSabloComponentToRhinoValue(JSONUtils.fromJSON(null, newargs[i], parameterPropertyDescription,
						new BrowserConverterContext(component, PushToServerEnum.allow), returnValueAdjustedIncommingValueForIndex),
						parameterPropertyDescription, component, null);
					//TODO? if in propertyDesc.getAsPropertyDescription().getConfig() we have  "type":"${dataproviderType}" and parameterPropertyDescription.getType() is Object
					//then get the type from the dataprovider and try to convert the json to that type instead of simply object
				}
			}
		}

		if (component instanceof WebFormComponent)
		{
			IPersist persist = ((WebFormComponent)component).getFormElement().getPersistIfAvailable();
			if (persist instanceof AbstractBase)
			{
				List<Object> instanceMethodArguments = ((AbstractBase)persist).getFlattenedMethodArguments(eventType);
				if (instanceMethodArguments != null && instanceMethodArguments.size() > 0)
				{
					newargs = Utils.arrayMerge(newargs, Utils.parseJSExpressions(instanceMethodArguments));
				}
			}
		}

		try
		{
			formController.getApplication().updateLastAccessed();
			return formController.getApplication().getScriptEngine().executeFunction(f, scope, scope, newargs, false, false);
		}
		catch (Exception ex)
		{
			formController.getApplication().reportJSError(ex.getMessage(), ex);
			return null;
		}
	}

	/**
	 * Get the event type based on the methodID property.
	 * @param methodID
	 * @return
	 */
	private String getEventType(String methodID)
	{
		switch (methodID)
		{
			case IContentSpecConstantsBase.PROPERTY_ONACTIONMETHODID :
				return IJSEvent.ACTION;
			case IContentSpecConstants.PROPERTY_ONDOUBLECLICKMETHODID :
				return IJSEvent.DOUBLECLICK;
			case IContentSpecConstants.PROPERTY_ONRIGHTCLICKMETHODID :
				return IJSEvent.RIGHTCLICK;
			case IContentSpecConstants.PROPERTY_ONDATACHANGEMETHODID :
				return IJSEvent.DATACHANGE;
			case IContentSpecConstants.PROPERTY_ONFOCUSGAINEDMETHODID :
				return IJSEvent.FOCUSGAINED;
			case IContentSpecConstants.PROPERTY_ONFOCUSLOSTMETHODID :
				return IJSEvent.FOCUSLOST;
			case IContentSpecConstants.PROPERTY_ONDRAGMETHODID :
				return JSEvent.EventType.onDrag.toString();
			case IContentSpecConstants.PROPERTY_ONDRAGENDMETHODID :
				return JSEvent.EventType.onDragEnd.toString();
			case IContentSpecConstants.PROPERTY_ONDRAGOVERMETHODID :
				return JSEvent.EventType.onDragOver.toString();
			case IContentSpecConstants.PROPERTY_ONDROPMETHODID :
				return JSEvent.EventType.onDrop.toString();
			default :
				return methodID;
		}
	}

	public static JSONObject createEvent(String type, int i)
	{
		JSONObject event = new JSONObject();
		try
		{
			event.put("type", "event");
			event.put("eventName", type);
			event.put("timestamp", System.currentTimeMillis());
			event.put("modifiers", 0);
			event.put("x", 0);
			event.put("y", 0);
			event.put("selectedIndex", i);
		}
		catch (JSONException ex)
		{
			Debug.error(ex);
		}

		return event;
	}
}
