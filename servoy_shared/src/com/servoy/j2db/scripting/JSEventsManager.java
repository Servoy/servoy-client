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

import java.util.List;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
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
	 * <br/>
	 * The callback first parameter is always a JSEvent object. The source is the context (for default form event types this is 'forms.MyForm') also the formName is set to the form that triggered this for the default events.
	 * The data object of the JSEven object is the argument array, those are also given as parameters after the first JSEvent parameter.
	 * For default types these are the same arguments then what the forms event function also would get (so that could also containe a JSEvent parameter)
	 * For custom types the given arguments in the fireEventListeners call are given as parameters after the JSEvent parameter and as Array in the data object of the JSEvent object
	 * <br/><br/>
	 * The callback function can look like this:
	 * <pre>
	 * /*&#42;
	 *  * &#64;param {JSEvent} event the event object that is fired
	 *  * &#64;param {Object} arg1 the first argument that is given by the system or the fireEventListeners call
	 *  * &#64;param {Object} arg2 the second argument that is given by the system or the fireEventListeners call
	 *  *&#47;
	 * function myCallback(event, arg1, arg2) {}
	 * </pre>
	 *
	 * @sample
	 * var deregister = eventsManager.addEventListener(EventType.myCustomEvent, myCallback);
	 * deregister();
	 *
	 * @param eventType Event type to listen to.
	 * @param callback {(event:JSEvent,customArguments:...Object)=>Object} the function callback
	 *
	 * @return returns the deregister function that can be used to remove the listener.
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public Function addEventListener(EventType eventType, Function callback)
	{
		return addEventListener(eventType, callback, null);
	}

	/**
	 * Adds a listener for a custom EventType or for one of default event types. The custom event is defined on solution eventType property and can be fired using fireEventListeners.
	 * When context is specified, the callback will only be called on that context: for custom events, when fireEventListeners is called using same context and for default events when context is the specific form that triggers the default form event.
	 * <br/>
	 * The callback first parameter is always a JSEvent object. The source is the context (for default form event types this is 'forms.MyForm') also the formName is set to the form that triggered this for the default events.
	 * The data object of the JSEven object is the argument array, those are also given as parameters after the first JSEvent parameter.
	 * For default types these are the same arguments then what the forms event function also would get (so that could also containe a JSEvent parameter)
	 * For custom types the given arguments in the fireEventListeners call are given as parameters after the JSEvent parameter and as Array in the data object of the JSEvent object
	 * <br/><br/>
	 * The callback function can look like this:
	 * <pre>
	 * /*&#42;
	 *  * &#64;param {JSEvent} event the event object that is fired
	 *  * &#64;param {Object} arg1 the first argument that is given by the system or the fireEventListeners call
	 *  * &#64;param {Object} arg2 the second argument that is given by the system or the fireEventListeners call
	 *  *&#47;
	 * function myCallback(event, arg1, arg2) {}
	 * </pre>
	 *
	 * @sample
	 * var deregister = eventsManager.addEventListener(EventType.onShow,myCallback,forms.myform);
	 * deregister();
	 *
	 * @param eventType Event type to listen to.
	 * @param callback {(event:JSEvent,customArguments:...Object)=>Object} the function callback
	 * @param context Can be a form, global scope or any string. Will cause callback to only be called on that context.
	 *
	 * @return returns the deregister function that can be used to remove the listener.
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public Function addEventListener(EventType eventType, Function callback, Object context)
	{
		application.getEventsManager().addListener(eventType, callback, getContextAsString(context));
		return new BaseFunction()
		{
			@Override
			public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
			{
				return Boolean.valueOf(removeEventListener(eventType, callback, context));
			}
		};
	}

	/**
	 * Removes one or multiple listeners (depending on parameters). Only works for custom event listeners that were added using addEventListener.
	 *
	 * @sample
	 * eventsManager.removeEventListener(EventType.myCustomEvent,callback,'mycontext');
	 *
	 * @param eventType Event type for listener to remove. Cannot be null.
	 * @param callback callback to be removed. Can be null (any listener).
	 * @param context Context for listener to remove. Can be null (any context).
	 *
	 * @return true if the deregister was successful
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public boolean removeEventListener(EventType eventType, Function callback, Object context)
	{
		return application.getEventsManager().removeListener(eventType, callback, getContextAsString(context));
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
		Object array = application.getEventsManager().fireListeners(eventType, getContextAsString(context), callbackArguments, returnValueAggregationType);
		if (array instanceof List< ? > lst)
		{
			Context cx = Context.getCurrentContext();
			return cx.newArray(ScriptRuntime.getTopCallScope(cx), lst.toArray());
		}
		return array;
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
