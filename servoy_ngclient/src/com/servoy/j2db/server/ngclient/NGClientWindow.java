/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

import org.json.JSONString;
import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.eventthread.EventDispatcher;
import org.sablo.specification.WebObjectApiFunctionDefinition;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.websocket.BaseWindow;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.IToJSONWriter;
import org.sablo.websocket.IWebsocketEndpoint;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.endpoint.NGClientSideWindowState;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.WeakHashSet;

/**
 * Sablo window for NGClient
 *
 * @author rgansevles
 *
 */
public class NGClientWindow extends BaseWindow implements INGClientWindow
{
	protected static final Logger log = LoggerFactory.getLogger("com.servoy.j2db.server.ngclient.api");

	private final WeakHashSet<IWebFormUI> pendingApiCallFormsOnNextResponse = new WeakHashSet<>();

	private final ConcurrentMap<String, WeakHashSet<INGFormElement>> allowedForms = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, WeakHashMap<INGFormElement, String>> allowedRelation = new ConcurrentHashMap<>();

	public NGClientWindow(INGClientWebsocketSession websocketSession, int windowNr, String windowName)
	{
		super(websocketSession, windowNr, windowName);
	}

	public static INGClientWindow getCurrentWindow()
	{
		return (INGClientWindow)CurrentWindow.get();
	}

	@Override
	public INGClientWebsocketSession getSession()
	{
		return (INGClientWebsocketSession)super.getSession();
	}

	/**
	 * Gives the opportunity of creatin their own type of ClientSideWindowState to subclasses.
	 */
	@Override
	protected NGClientSideWindowState createClientSideWindowState()
	{
		return new NGClientSideWindowState(this);
	}

	@Override
	protected NGClientSideWindowState getClientSideWindowState()
	{
		return (NGClientSideWindowState)super.getClientSideWindowState();
	}

	public INGApplication getClient()
	{
		return getSession().getClient();
	}

	@Override
	public Container getForm(String formName)
	{
		if (getClient() == null || getClient().getFormManager() == null || getClient().getFormManager().getForm(formName) == null) return null;
		return (Container)getClient().getFormManager().getForm(formName).getFormUI();
	}

	@Override
	protected Container getFormContainer(WebComponent component)
	{
		return (Container)component.findParent(IWebFormUI.class);
	}

	@Override
	public void sendChanges() throws IOException
	{
		try
		{
			if (getSession().getClient() != null) getSession().getClient().changesWillBeSend();
			super.sendChanges();
		}
		finally
		{
			pendingApiCallFormsOnNextResponse.clear();
		}
	}

	@Override
	protected boolean shouldSendChangesToClientWhenAvailable(Container fc)
	{
		boolean sendChanges = pendingApiCallFormsOnNextResponse.contains(fc) || fc.isVisible(); // we want to send changes for all forms that are visible and for forms that will get API calls as well;
		// this was needed now instead of only isVisible() in order to also send changes to hidden div forms before any API call gets executed on them;
		// NOTE: for forms that are not yet visible but API form them is called, the data changes will not be present as their data broadcast is disabled from DataAdapterList and FormController
		// while they are hidden, but property changes will be sent - this is a known thing

		if (!sendChanges)
		{
			// if a pending delayed API call will get called as well, send changes for that form so it's up-to-date
			sendChanges = isFormResolved(fc) && hasPendingDelayedCalls(fc);
		}

		if (sendChanges && fc instanceof WebFormUI)
		{
			sendChanges = !((WebFormUI)fc).isChanging();
		}

		return sendChanges;
	}

	@Override
	protected Object invokeApi(WebComponent receiver, WebObjectApiFunctionDefinition apiFunction, Object[] arguments,
		Map<String, JSONString> callContributions)
	{
		Map<String, JSONString> newCallContributions = new HashMap<>();
		if (callContributions != null) newCallContributions.putAll(callContributions); // probably always null

		IWebFormUI formUI = receiver.findParent(IWebFormUI.class);
		IWebFormController form = formUI.getController();

		if (!isDelayedApiCall(apiFunction))
		{
			if (!isAsyncApiCall(apiFunction) && !getClientSideWindowState().isFormAttachedToDOM(form.getName()))
			{
				log.warn("You are calling a component sync api method (to browser) in form '" + form.getName() +
					"' before the form is available in browser's DOM. This should be avoided. (Component : " + receiver.getName() + " , api: " +
					apiFunction.getName() +
					"). Titanim NG client will try to wait for form load only if it sees that it's in the process of showing a form; if not or if the form will not show after a few seconds, it will just fail/ignore the call. NG client used to show the form in a hidden div just in order to be able to call the API. Both can slow down your solution. The solution's code can be changed, or the component's impl could take advantage of server side impl (if a return value is needed, but that value does not necessarily need the form's UI to be loaded in the browser) + model properties, or, if a return value is not needed or can be delayed to a callback param, use async calls with delayUntilFormLoads.");
			}
			touchForm(form.getForm(), form.getName(), false, false);
			pendingApiCallFormsOnNextResponse.add(formUI); // the form will be on client, make sure we send changes for it as well... if it would be delayed it might not even be present on client for a while, so we will send changes only when it is attached to DOM and has delayed
		}
		if (receiver instanceof WebFormComponent && ((WebFormComponent)receiver).getComponentContext() != null)
		{
			ComponentContext componentContext = ((WebFormComponent)receiver).getComponentContext();

			// write the path to JSON
			EmbeddableJSONWriter ejw = new JSONUtils.EmbeddableJSONWriter(true);
			JSONUtils.defaultToJSONValue(FullValueToJSONConverter.INSTANCE, ejw, null, componentContext.getPropertyPath(), null, null);

			newCallContributions.put("propertyPath", ejw);
		}

		Pair<Long, Long> perfId = getClient().onStartSubAction(receiver.getSpecification().getName(), apiFunction.getName(), apiFunction, arguments);

		try
		{
			// actual call
			return super.invokeApi(receiver, apiFunction, arguments, newCallContributions);
		}
		finally
		{
			getClient().onStopSubAction(perfId);
		}
	}

	@Override
	public void registerAllowedForm(String formName, INGFormElement element)
	{
		WeakHashSet<INGFormElement> weakSet = allowedForms.get(formName);
		if (weakSet == null)
		{
			weakSet = new WeakHashSet<>();
			WeakHashSet<INGFormElement> current = allowedForms.putIfAbsent(formName, weakSet);
			if (current != null) weakSet = current;
		}
		weakSet.add(element);
	}

	@Override
	public String registerAllowedRelation(String relationName, INGFormElement element)
	{
		String relName = "-1";
		if (relationName != null)
		{
			Relation[] relations = getSession().getClient().getFlattenedSolution().getRelationSequence(relationName);
			if (relations != null)
			{
				for (Relation relation : relations)
				{
					if (relation != null)
					{
						if ("-1".equals(relName))
						{
							relName = Integer.toHexString(relation.getID());
						}
						else
						{
							relName += Integer.toHexString(relation.getID());
						}
					}
				}

			}
		}

		WeakHashMap<INGFormElement, String> weakSet = allowedRelation.get(relName);
		if (weakSet == null)
		{
			weakSet = new WeakHashMap<>();
			WeakHashMap<INGFormElement, String> current = allowedRelation.putIfAbsent(relName, weakSet);
			if (current != null) weakSet = current;
		}
		weakSet.put(element, relationName);
		return relName;
	}

	@Override
	public String isVisibleAllowed(String formName, String uuidRelationName, INGFormElement element) throws IllegalAccessException
	{
		WeakHashSet<INGFormElement> weakHashSet = allowedForms.get(formName);
		if (weakHashSet != null && weakHashSet.contains(element))
		{
			if (uuidRelationName == null) return null;
			WeakHashMap<INGFormElement, String> relationMap = allowedRelation.get(uuidRelationName);
			if (relationMap != null && relationMap.containsKey(element)) return relationMap.get(element);
		}
		throw new IllegalAccessException("Can't show form " + formName + " for component + " + element);
	}

	@Override
	public void touchForm(Form form, String realInstanceName, boolean async, boolean testForValidForm)
	{
		if (form == null) return;
		String formName = realInstanceName == null ? form.getName() : realInstanceName;
		if (testForValidForm && !allowedForms.containsKey(formName) && getClientSideWindowState().getFormUrl(formName) == null)
		{
			throw new IllegalStateException("Can't show form: " + formName + " because it is not allowed in the client");
		}
		IFormHTMLAndJSGenerator generator = getSession().getFormHTMLAndJSGenerator(form, formName);
		String formUrl = getRealFormURLAndSeeIfItIsACopy(form, formName).getLeft();
		boolean nowSentToClient = getClientSideWindowState().addFormIfAbsent(formName, formUrl);
		if (nowSentToClient)
		{
			IWebFormController cachedFormController = getSession().getClient().getFormManager().getCachedFormController(formName);
			IWebFormUI formUI = cachedFormController != null ? cachedFormController.getFormUI() : null;
			if (formUI != null && formUI.getParentContainer() == null)
			{
				String currentWindowName = getCurrentWindow().getName();
				if (currentWindowName == null)
				{
					currentWindowName = getSession().getClient().getRuntimeWindowManager().getMainApplicationWindow().getName();
				}
				formUI.setParentWindowName(currentWindowName);
			}
			// form is not yet on the client, send over the controller
			updateController(form, formName, !async, generator);
			// if recreateUI was also called (even that is not really needed), do flush the recreate map, so the form is not send again in the same response.
			getClient().flushRecreatedForm(form, formName);
			if (Debug.tracing()) Debug.trace("touchForm(" + async + ") - addFormIfAbsent: " + form.getName());
		}
		else
		{
			formUrl = getClientSideWindowState().getFormUrl(formName);
			if (Debug.isDebugEnabled()) Debug.debug("touchForm(" + async + ") - formAlreadyPresent: " + form.getName());
		}

		// if sync wait until we got response from client as it is loaded
		if (!async)
		{
			if (!getClientSideWindowState().isFormAttachedToDOM(formName))
			{
				if (!nowSentToClient)
				{
					// this means a previous async touchForm already sent URL and JS code (updateController) to client, but the client form was not yet loaded (directives, scopes....)
					// so probably a tabpanel or component that asked for it changed it's mind and no longer showed it; make sure it will show before waiting!
					getSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("requireFormLoaded",
						new Object[] { formName });
				}
				if (Debug.isDebugEnabled()) Debug.debug("touchForm(" + async + ") - will suspend: " + form.getName());
				// really send the changes
				try
				{
					sendChanges();
				}
				catch (IOException e)
				{
					Debug.error(e);
				}
				if (generator.waitForBackgroundFormLoad())
				{
					try
					{
						getSession().getEventDispatcher().suspend(formUrl, IWebsocketEndpoint.EVENT_LEVEL_SYNC_API_CALL, EventDispatcher.CONFIGURED_TIMEOUT);
					}
					catch (CancellationException e)
					{
						throw e; // full browser refresh while doing this?
					}
					catch (TimeoutException e)
					{
						throw new RuntimeException("Touch form realInstanceName (" + form.getName() + ") timed out.", e); // timeout... something went wrong; propagate this exception to calling code...
					}
				}
			}
		}
	}

	protected void updateController(Form form, final String realFormName, final boolean forceLoad, IFormHTMLAndJSGenerator formTemplateGenerator)
	{
		try
		{
			Pair<String, Boolean> urlAndCopyState = getRealFormURLAndSeeIfItIsACopy(form, realFormName);
			final String realUrl = urlAndCopyState.getLeft();
			boolean copy = urlAndCopyState.getRight().booleanValue();

			boolean needsToGenerateTemplates = (copy || !Boolean.valueOf(System.getProperty("servoy.generateformscripts", "false")).booleanValue());
			final String jsTemplate = (needsToGenerateTemplates ? formTemplateGenerator.generateJS() : "");
			final String htmlTemplate = (needsToGenerateTemplates ? formTemplateGenerator.generateHTMLTemplate() : "");

			// update endpoint URL if needed
			String previousURL = getClientSideWindowState().getFormUrl(realFormName);
			if (previousURL != null && !realUrl.equals(previousURL))
			{
				getClientSideWindowState().formDestroyed(realFormName);
				getClientSideWindowState().addFormIfAbsent(realFormName, realUrl);
			}

			CurrentWindow.runForWindow(this, new Runnable()
			{

				@Override
				public void run()
				{
					if (getSession().getClient().isEventDispatchThread() && forceLoad)
					{
						// async now instead of sync call; because we just need to send data right away; return value is not needed;

						// and a possible suspend of the event thread if a sync call would be used here messes up with the fix for SVY-19635
						// that schedules a high eventLevel even on the event thread to execute after a form's onShow handler execution
						// or if that handler does a component sync call to client - in order to send data and call
						// session.getSabloService().setExpectFormToShowOnClient(false)... if we used here a sync call here,
						// then that event would execute too soon if onShow calls a component sync api in it - before the component sync call
						// is actually sent to client... because it would be suspended by this call before that (if this call would be
						// sync instead of asyncnow)

						getSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncNowServiceCall("updateController",
							new Object[] { realFormName, jsTemplate, realUrl, Boolean.valueOf(forceLoad), htmlTemplate }, true);
					}
					else
					{
						getSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("updateController",
							new Object[] { realFormName, jsTemplate, realUrl, Boolean.valueOf(forceLoad), htmlTemplate });
					}
				}
			});
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
	}

	@Override
	public boolean hasFormChangedSinceLastSendToClient(Form flattenedForm, String realName)
	{
		boolean changed = true;
		String clientUsedFormURL = getClientSideWindowState().getFormUrl(realName);
		if (clientUsedFormURL == null)
		{
			// need to add the session id to the default, because all urls will have that (also the one from the end point)
			clientUsedFormURL = getDefaultFormURLStart(flattenedForm, realName) + "?clientnr=" + getSession().getSessionKey().getClientnr();
		}
		if (clientUsedFormURL != null)
		{
			changed = !clientUsedFormURL.equals(getRealFormURLAndSeeIfItIsACopy(flattenedForm, realName).getLeft());
		}
		return changed;
	}

	protected Pair<String, Boolean> getRealFormURLAndSeeIfItIsACopy(Form form, String realFormName)
	{
		FlattenedSolution fs = getSession().getClient().getFlattenedSolution();
		Solution sc = fs.getSolutionCopy(false);
		String realUrl = getDefaultFormURLStart(form, realFormName);
		boolean copy = false;

		if (sc != null && sc.getChild(form.getUUID()) != null)
		{
			realUrl = realUrl + "?lm:" + form.getLastModified() + "&clientnr=" + getSession().getSessionKey().getClientnr();
			copy = true;
		}
		else if (!form.getName().endsWith(realFormName))
		{
			realUrl = realUrl + "?lm:" + form.getLastModified() + "&clientnr=" + getSession().getSessionKey().getClientnr();
		}
		else
		{
			realUrl = realUrl + "?clientnr=" + getSession().getSessionKey().getClientnr();
		}

		return new Pair<String, Boolean>(realUrl, Boolean.valueOf(copy));
	}

	@Override
	public void updateForm(Form form, String name, IFormHTMLAndJSGenerator formTemplateGenerator)
	{
		/**
		 * we should have to check for hasForm here, because we shouldn't push a recreatedUI form when it is not
		 * on the client. Because it could be not visible again and not have all the data, then the next time the touch will
		 * just fully ignore it.
		 */
		if (hasForm(name))
		{
			// if form was not sent to client, do not send now; this is just recreateUI
			updateController(form, name, false, formTemplateGenerator);
		}
	}

	@Override
	public boolean hasForm(String realName)
	{
		return getEndpoint() != null && getClientSideWindowState().getFormUrl(realName) != null;
	}

	protected String getDefaultFormURLStart(Form form, String name)
	{
		return NGClientEntryFilter.SOLUTIONS_PATH.substring(1) + form.getSolution().getName() + NGClientEntryFilter.FORMS_PATH + name + ".html";
	}

	public void destroyForm(String name)
	{
		getSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("destroyController", new Object[] { name });
		getClientSideWindowState().formDestroyed(name);
		clearAllDelayedCallsToForm(name);
	}

	/**
	 * This 'resolved' is equivalent to client-side 'resolved' state. As sablo puts it, it means that a form is attached to the DOM of this window or not.
	 */
	public void setFormResolved(String formName, boolean resolved)
	{
		String formUrl = getClientSideWindowState().getFormUrl(formName);
		if (formUrl != null)
		{
			synchronized (formUrl)
			{
				getClientSideWindowState().setAttachedToDOM(formName, resolved);
				if (Debug.isDebugEnabled())
					Debug.debug((resolved ? "formIsNowMarkedAsResolvedOnServer(" : "formIsNowMarkedAsUNResolvedOnServer(") + formUrl + "): " + formName);
				if (resolved) getSession().getEventDispatcher().resume(formUrl);
			}
		}
	}

	/**
	 * This 'resolved' is equivalent to client-side 'resolved' state. As sablo puts it, it means that a form is attached to the DOM of this window or not.
	 */
	@Override
	protected boolean isFormResolved(Container form)
	{
		IWebFormController controller = ((IWebFormUI)form).getController();
		if (controller == null) return false;

		String formName = controller.getName();
		String formUrl = getClientSideWindowState().getFormUrl(formName);
		if (formUrl != null)
		{
			synchronized (formUrl)
			{
				return getClientSideWindowState().isFormAttachedToDOM(formName);
			}
		}
		return false;
	}

	@Override
	public Object executeServiceCall(IClientService clientService, String functionName, Object[] arguments, WebObjectApiFunctionDefinition apiFunction,
		IToJSONWriter<IBrowserConverterContext> pendingChangesWriter, boolean blockEventProcessing) throws IOException
	{
		Pair<Long, Long> perfId = getClient().onStartSubAction(clientService.getName(), functionName, apiFunction, arguments);
		try
		{
			return super.executeServiceCall(clientService, functionName, arguments, apiFunction, pendingChangesWriter, blockEventProcessing);
		}
		finally
		{
			getClient().onStopSubAction(perfId);
		}
	}

	@Override
	public String getRelationName(String uuidRelationName, INGFormElement element)
	{
		if (uuidRelationName == null) return null;
		WeakHashMap<INGFormElement, String> relationMap = allowedRelation.get(uuidRelationName);
		if (relationMap != null && relationMap.containsKey(element)) return relationMap.get(element);
		return uuidRelationName;
	}
}
