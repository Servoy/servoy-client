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

package com.servoy.j2db.scripting;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.info.EVENTS_AGGREGATION_TYPE;
import com.servoy.j2db.scripting.info.EventType;

/**
 * This is the EventsManager where you can register for events that are fired by the servoy for the default events
 * and custom events can be listened to and fired for in code.
 *
 * The EventType can be a build in one or custom one that are set in the solution/module eventTypes property.
 *
 * @author jcompagner
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Events Manager", scriptingName = "eventsManager")
public class JSEventsManager implements IReturnedTypesProvider
{

	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSEventsManager.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return getAllReturnedTypesImpl();
			}
		});
	}

	private final IApplication application;

	public JSEventsManager(IApplication app)
	{
		application = app;
	}


	/**
	 * Adds a listener for a custom EventType or for one of default event types. The custom event is defined on solution eventType property and can be fired using fireEventListeners.
	 *
	 * The callback first parameter is always a JSEvent object. The source is the context (for default form event types this is 'forms.MyForm') also the formName is set to the form that triggered this for the default events.
	 * The data object of the JSEven object is the argument array, those are also given as parameters after the first JSEvent parameter.
	 * For default types these are the same arguments then what the forms event function also would get (so that could also containe a JSEvent parameter)
	 * For custom types the given arguments in the fireEventListeners call are given as parameters after the JSEvent parameter and as Array in the data object of the JSEvent object
	 *
	 * @sample
	 * eventsManager.addEventListener(EventType.myCustomEvent,this.callback);
	 *
	 * @param eventType Event type to listen to.
	 * @param callback callback to be called.
	 *
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public void addEventListener(EventType eventType, Function callback)
	{
		addEventListener(eventType, callback, null);
	}

	/**
	 * Adds a listener for a custom EventType or for one of default event types. The custom event is defined on solution eventType property and can be fired using fireEventListeners.
	 * When context is specified, the callback will only be called on that context: for custom events, when fireEventListeners is called using same context and for default events when context is the specific form that triggers the default form event.
	 *
	 * The callback first parameter is always a JSEvent object. The source is the context (for default form event types this is 'forms.MyForm') also the formName is set to the form that triggered this for the default events.
	 * The data object of the JSEven object is the argument array, those are also given as parameters after the first JSEvent parameter.
	 * For default types these are the same arguments then what the forms event function also would get (so that could also containe a JSEvent parameter)
	 * For custom types the given arguments in the fireEventListeners call are given as parameters after the JSEvent parameter and as Array in the data object of the JSEvent object
	 *
	 *
	 * @sample
	 * eventsManager.addEventListener(EventType.onShowMethodID,this.callback,forms.myform);
	 *
	 * @param eventType Event type to listen to.
	 * @param callback callback to be called.
	 * @param context Can be a form, global scope or any string. Will cause callback to only be called on that context.
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public void addEventListener(EventType eventType, Function callback, Object context)
	{
		application.getEventsManager().addListener(eventType, callback, getContextAsString(context));
	}

	/**
	 * Removes one or multiple listeners (depending on parameters). Only works for custom event listeners that were added using addEventListener.
	 *
	 * @sample
	 * eventsManager.removeEventListener(EventType.myCustomEvent,this.callback,'mycontext');
	 *
	 * @param eventType Event type for listener to remove. Cannot be null.
	 * @param callback callback to be removed. Can be null (any listener).
	 * @param context Context for listener to remove. Can be null (any context).
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public void removeEventListener(EventType eventType, Function callback, Object context)
	{
		application.getEventsManager().removeListener(eventType, callback, getContextAsString(context));
	}

	/**
	 * Checks if listeners were added for a certain event type (and possibly context)
	 *
	 * @sample
	 * eventsManager.hasEventListeners(EventType.myCustomEvent,'mycontext');
	 *
	 * @param eventType Event type for listener to check.
	 * @param context Context for listener to check. Can be null (any context).
	 *
	 * @return Boolean (true) if listeners were added, (false) otherwise
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public boolean hasEventListeners(EventType eventType, Object context)
	{
		return application.getEventsManager().hasListeners(eventType, getContextAsString(context));
	}

	/**
	 * Calls all listeners for a certain event type (and optionally, for a certain context).
	 * Will return either a Boolean calculated as logical AND between all listeners return value or an Array with all return values.
	 *
	 * @sample
	 * eventsManager.fireEventListeners(EventType.myCustomEvent,'mycontext',null,EVENTS_AGGREGATION_TYPE.RETURN_VALUE_BOOLEAN);
	 *
	 * @param eventType Event type for listeners to be called.
	 * @param context Context for listeners to be called. Can be null (any context).
	 * @param callbackArguments Arguments for listener to be called with. Can be null.
	 * @param returnValueAggregationType Return value constant. Should be taken from EVENTS_AGGREGATION_TYPE.
	 *
	 * @return Boolean or Array depending on returnValueAggregationType. Boolean value is a logical AND between all listeners return value and Array contains all return values of the listeners.
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public Object fireEventListeners(EventType eventType, Object context, Object[] callbackArguments, EVENTS_AGGREGATION_TYPE returnValueAggregationType)
	{
		return application.getEventsManager().fireListeners(eventType, getContextAsString(context), callbackArguments, returnValueAggregationType);
	}

	/**
	 * Calls all listeners for a certain event type
	 * Will return a Boolean calculated as logical AND between all listeners return value (default value).
	 *
	 * @sample
	 * eventsManager.fireEventListeners(EventType.myCustomEvent,'mycontext');
	 *
	 * @param eventType Event type for listeners to be called.
	 *
	 * @return Boolean value that is a logical AND between all listeners return value.
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public Object fireEventListeners(EventType eventType)
	{
		return fireEventListeners(eventType, null, null, EVENTS_AGGREGATION_TYPE.RETURN_VALUE_BOOLEAN);
	}

	/**
	 * Calls all listeners for a certain event type and for a certain context).
	 * Will return a Boolean calculated as logical AND between all listeners return value (default value).
	 *
	 * @sample
	 * eventsManager.fireEventListeners(EventType.myCustomEvent,'mycontext');
	 *
	 * @param eventType Event type for listeners to be called.
	 * @param context Context for listeners to be called. Can be null (any context).
	 *
	 * @return Boolean value that is a logical AND between all listeners return value.
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public Object fireEventListeners(EventType eventType, Object context)
	{
		return fireEventListeners(eventType, getContextAsString(context), null, EVENTS_AGGREGATION_TYPE.RETURN_VALUE_BOOLEAN);
	}

	/**
	 * Calls all listeners for a certain event type
	 * Will return a Boolean calculated as logical AND between all listeners return value (default value).
	 *
	 * @sample
	 * eventsManager.fireEventListeners(EventType.myCustomEvent,'mycontext');
	 *
	 * @param eventType Event type for listeners to be called.
	 * @param callbackArguments Arguments for listener to be called with. Can be null.
	 *
	 * @return Boolean value that is a logical AND between all listeners return value.
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public Object fireEventListeners(EventType eventType, Object[] callbackArguments)
	{
		return fireEventListeners(eventType, null, callbackArguments, EVENTS_AGGREGATION_TYPE.RETURN_VALUE_BOOLEAN);
	}

	/**
	 * Calls all listeners for a certain event type and certain context.
	 * Will return a Boolean calculated as logical AND between all listeners return value (default value).
	 *
	 * @sample
	 * eventsManager.fireEventListeners(EventType.myCustomEvent,'mycontext');
	 *
	 * @param eventType Event type for listeners to be called.
	 * @param context Context for listeners to be called. Can be null (any context).
	 * @param callbackArguments Arguments for listener to be called with. Can be null.
	 *
	 * @return Boolean value that is a logical AND between all listeners return value.
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public Object fireEventListeners(EventType eventType, Object context, Object[] callbackArguments)
	{
		return fireEventListeners(eventType, getContextAsString(context), callbackArguments, EVENTS_AGGREGATION_TYPE.RETURN_VALUE_BOOLEAN);
	}

	private String getContextAsString(Object context)
	{
		String contextAsString = null;
		if (context != null)
		{
			if (context instanceof BasicFormController form)
			{
				contextAsString = IExecutingEnviroment.TOPLEVEL_FORMS + '.' + form.getName();
			}
			else if (context instanceof FormScope form)
			{
				contextAsString = IExecutingEnviroment.TOPLEVEL_FORMS + '.' + form.getFormController().getName();
			}
			else if (context instanceof BasicFormController.JSForm form)
			{
				contextAsString = IExecutingEnviroment.TOPLEVEL_FORMS + '.' + form.getFormPanel().getName();
			}
			else if (context instanceof GlobalScope scope)
			{
				contextAsString = IExecutingEnviroment.TOPLEVEL_SCOPES + '.' + scope.getScopeName();
			}
			else if (context instanceof String)
			{
				contextAsString = (String)context;
			}
		}
		return contextAsString;
	}

	@Override
	public Class< ? >[] getAllReturnedTypes()
	{
		return getAllReturnedTypesImpl();
	}

	private static Class< ? >[] getAllReturnedTypesImpl()
	{
		return new Class[] { EVENTS_AGGREGATION_TYPE.class, EventType.class };
	}


}
