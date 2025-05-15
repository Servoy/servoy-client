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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.json.JsonParser;
import org.sablo.WebComponent;
import org.sablo.specification.IFunctionParameters;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.base.persistence.constants.IContentSpecConstantsBase;
import com.servoy.base.scripting.api.IJSEvent;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.JSEventType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGCustomJSONObjectType;
import com.servoy.j2db.server.ngclient.property.types.RecordPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;


/**
 * @author lvostinar
 *
 */
public class EventExecutor
{
	public static final Logger EVENT_TRACING_LOG = LoggerFactory.getLogger("com.servoy.event.tracing");

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
			ScriptMethod scriptMethod = formController.getApplication().getFlattenedSolution().getScriptMethod(eventId);
			if (scriptMethod != null)
			{
				if (scriptMethod.getParent() instanceof Form)
				{
					FormScope formScope = formController.getFormScope();
					f = formScope.getFunctionByName(scriptMethod.getName());
					if (f != null && f != Scriptable.NOT_FOUND)
					{
						scope = formScope;
					}
				}
				// is it a global method
				else if (scriptMethod.getParent() instanceof Solution)
				{
					scope = formController.getApplication().getScriptEngine().getScopesScope().getGlobalScope(scriptMethod.getScopeName());
					if (scope != null)
					{
						f = ((GlobalScope)scope).getFunctionByName(scriptMethod.getName());
					}
				}
				// very like a foundset/entity method
				else
				{
					Scriptable foundsetScope = null;
					if (component instanceof WebFormComponent)
					{
						IRecord rec = ((WebFormComponent)component).getDataAdapterList().getRecord();
						if (rec != null)
						{
							foundsetScope = (Scriptable)rec.getParentFoundSet();
						}
					}
					if (foundsetScope == null) foundsetScope = (Scriptable)formController.getFormModel();
					if (foundsetScope != null)
					{
						scope = foundsetScope; // TODO ViewFoundSets should be come a scriptable if they have foundset methods..
						Object scopeMethod = scope.getPrototype().get(scriptMethod.getName(), scope);
						if (scopeMethod instanceof Function)
							f = (Function)scopeMethod;
					}
				}
				if (f == null)
				{
					Debug.error("No function found for " + scriptMethod + " when trying to execute the event " + eventType + '(' + eventId + //$NON-NLS-1$ //$NON-NLS-2$
						") of component: " + component, new RuntimeException()); //$NON-NLS-1$
					return null;
				}
			}
			else
			{
				Debug.warn("Couldn't find the ScriptMethod for event: " + eventType + " with event id: " + eventId + " to execute for component " + component);
			}
		}
		if (formController.isInFindMode() && !Utils.getAsBoolean(f.get("_AllowToRunInFind_", f))) return null; //$NON-NLS-1$

		if (newargs != null)
		{
			WebObjectFunctionDefinition propertyDesc = component.getSpecification().getHandler(eventType);
			IFunctionParameters parameters = propertyDesc.getParameters();

			for (int i = 0; i < newargs.length; i++)
			{
				boolean isEvent = false, isSubEvent = false;

				if (newargs[i] instanceof JSONObject)
				{
					isEvent = "event".equals(((JSONObject)newargs[i]).optString("type"));
					if (!isEvent && i < parameters.getDefinedArgsCount())
					{
						PropertyDescription parameterPropertyDescription = parameters.getParameterDefinition(i);
						isSubEvent = parameterPropertyDescription.getType() instanceof NGCustomJSONObjectType &&
							JSEvent.class.getSimpleName().equals(((NGCustomJSONObjectType)parameterPropertyDescription.getType()).getExtends());
					}
				}

				if (isEvent || isSubEvent)
				{
					// FIXME I think (but we must check how existing things work to not break stuff) that this
					// whole if branch can be a part of the JSEventType class that could implement IServerRhinoToRhino conversion;
					// and this conversion has to be done before this method is even called... see SVY-18096

					JSONObject json = (JSONObject)newargs[i];
					JSEvent event = new JSEvent();
					JSEventType.fillJSEvent(event, isSubEvent ? json.getJSONObject(NGCustomJSONObjectType.getValueKey()) : json, component, formController);
					event.setType(getEventType(eventType));
					event.setName(RepositoryHelper.getDisplayName(eventType, BaseComponent.class));

					if (isSubEvent)
					{
						Context cx = Context.enter();
						try
						{
							Object object = new JsonParser(cx, scope).parseValue(json.get(NGCustomJSONObjectType.getValueKey()).toString());
							if (object instanceof Scriptable s)
							{
								s.setPrototype(cx.getWrapFactory().wrapAsJavaObject(cx, scope, event, JSEvent.class));
							}
							newargs[i] = object;
						}
						catch (Exception ex)
						{
							Debug.log(ex);
						}
						finally
						{
							Context.exit();
						}
					}
					else
					{
						newargs[i] = event;
					}
				}
				else if (newargs[i] == JSONObject.NULL)
				{
					newargs[i] = null;
				}
				else
				{
					// FIXME I think the convertSabloComponentToRhinoValue should only happen if args come from sablo/java value;
					// and this conversion has to be done before this method is even called... see SVY-18096

					// try to convert the received arguments
					if (i < parameters.getDefinedArgsCount())
					{
						PropertyDescription parameterPropertyDescription = parameters.getParameterDefinition(i);
						ValueReference<Boolean> returnValueAdjustedIncommingValueForIndex = new ValueReference<Boolean>(Boolean.FALSE);
						newargs[i] = NGConversions.INSTANCE.convertSabloComponentToRhinoValue(JSONUtils.fromJSON(null, newargs[i], parameterPropertyDescription,
							new BrowserConverterContext(component, PushToServerEnum.allow), returnValueAdjustedIncommingValueForIndex),
							parameterPropertyDescription, component, scope);
						if (parameterPropertyDescription.getType() == RecordPropertyType.INSTANCE &&
							parameterPropertyDescription.getTag("skipCallIfNotSelected") instanceof Boolean &&
							((Boolean)parameterPropertyDescription.getTag("skipCallIfNotSelected")).booleanValue())
						{
							if (newargs[i] == null) return null;
							IRecord recordArg = (IRecord)newargs[i];
							IFoundSet foundset = recordArg.getParentFoundSet();
							if (!foundset.isMultiSelect() && foundset instanceof FoundSet)
							{
								FoundSet foundsetObj = (FoundSet)foundset;
								if (!recordArg.equals(foundsetObj.getSelectedRecord()))
								{
									return null;
								}
							}
						}
					}
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
					// create entries for the instanceMethodArguments if they are more then callback arguments
					if (instanceMethodArguments.size() > newargs.length)
					{
						newargs = Utils.arrayJoin(newargs, new Object[instanceMethodArguments.size() - newargs.length]);
					}

					// use instanceMethodArguments if not null, else just use the callback argument
					for (int i = 0; i < instanceMethodArguments.size(); i++)
					{
						Object value = instanceMethodArguments.get(i);
						if (value != null && value != JSONObject.NULL)
						{
							newargs[i] = Utils.parseJSExpression(value);
						}
					}
				}
			}
		}

		if (EVENT_TRACING_LOG.isTraceEnabled())
		{
			INGApplication application = formController.getApplication();
			Object[] tenantValue = application.getScriptEngine().getJSSecurity().getTenantValue();
			String argsAsString = Arrays.asList(newargs).stream()
				.map(value -> value instanceof JSEvent ? "JSEvent" //$NON-NLS-1$
					: value instanceof Record ? "JSRecord" //$NON-NLS-1$
						: value instanceof String str && str.length() > 30 ? str.substring(0, 30)
							: value instanceof Scriptable s ? Utils.getScriptableString(s) : String.valueOf(value))
				.collect(Collectors.joining(",")); //$NON-NLS-1$
			EVENT_TRACING_LOG.trace(application.getUserUID() + '|' + application.getClientID() + '|' + Arrays.toString(tenantValue) + '|' +
				application.getSolutionName() + '|' + formController.getName() + '|' + component.getName() + '|' + eventType + '|' + argsAsString);
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
