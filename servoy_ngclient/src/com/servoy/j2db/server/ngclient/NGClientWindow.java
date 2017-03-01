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
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.eventthread.EventDispatcher;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.websocket.BaseWindow;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IToJSONWriter;
import org.sablo.websocket.IWebsocketEndpoint;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.endpoint.INGClientWebsocketEndpoint;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.WeakHashSet;

/**
 * Sablo window for NGClient
 *
 * @author rgansevles
 *
 */
public class NGClientWindow extends BaseWindow implements INGClientWindow
{

	private final WeakHashSet<IWebFormUI> pendingApiCallFormsOnNextResponse = new WeakHashSet<>();

	private final HashSet<String> allowedForms = new HashSet<>();

	public NGClientWindow(INGClientWebsocketSession websocketSession, String windowUuid, String windowName)
	{
		super(websocketSession, windowUuid, windowName);
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

	@Override
	public INGClientWebsocketEndpoint getEndpoint()
	{
		return (INGClientWebsocketEndpoint)super.getEndpoint();
	}

	public INGApplication getClient()
	{
		return getSession().getClient();
	}

	@Override
	public Container getForm(String formName)
	{
		return (Container)getSession().getClient().getFormManager().getForm(formName).getFormUI();
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

		return sendChanges;
	}

	@Override
	protected Object invokeApi(WebComponent receiver, WebObjectFunctionDefinition apiFunction, Object[] arguments, PropertyDescription argumentTypes,
		Map<String, Object> callContributions)
	{
		Map<String, Object> call = new HashMap<>();
		if (callContributions != null) call.putAll(callContributions);

		IWebFormUI formUI = receiver.findParent(IWebFormUI.class);
		IWebFormController form = getSession().getClient().getFormManager().getForm(formUI.getName());

		if (!isDelayedApiCall(receiver, apiFunction))
		{
			touchForm(form.getForm(), form.getName(), false, false);
			pendingApiCallFormsOnNextResponse.add(formUI); // the form will be on client, make sure we send changes for it as well... if it would be delayed it might not even be present on client for a while, so we will send changes only when it is attached to dom and has delayed
		}
		if (receiver instanceof WebFormComponent && ((WebFormComponent)receiver).getComponentContext() != null)
		{
			ComponentContext componentContext = ((WebFormComponent)receiver).getComponentContext();
			call.put("propertyPath", componentContext.getPropertyPath());
		}

		Pair<UUID, UUID> perfId = getClient().onStartSubAction(receiver.getSpecification().getName(), apiFunction.getName(), apiFunction, arguments);

		try
		{
			// actual call
			return super.invokeApi(receiver, apiFunction, arguments, argumentTypes, call);
		}
		finally
		{
			if (perfId != null) getClient().onStopSubAction(perfId);
		}
	}

	@Override
	public void registerAllowedForm(String formName)
	{
		allowedForms.add(formName);
	}

	@Override
	public void touchForm(Form form, String realInstanceName, boolean async, boolean testForValidForm)
	{
		if (form == null) return;
		String formName = realInstanceName == null ? form.getName() : realInstanceName;
		if (testForValidForm && !allowedForms.contains(formName) && getEndpoint().getFormUrl(formName) == null)
		{
			throw new IllegalStateException("Can't show form: " + formName + " because it is not allowed in the client");
		}
		String formUrl = getRealFormURLAndSeeIfItIsACopy(form, formName, false).getLeft();
		boolean nowSentToClient = getEndpoint().addFormIfAbsent(formName, formUrl);
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
			updateController(form, formName, !async, new FormHTMLAndJSGenerator(getSession().getClient(), form, formName));
			// if recreateUI was also called (even that is not really needed), do flush the recreate map, so the form is not send again in the same response.
			getClient().flushRecreatedForm(form, formName);
			if (Debug.tracing()) Debug.trace("touchForm(" + async + ") - addFormIfAbsent: " + form.getName());
		}
		else
		{
			formUrl = getEndpoint().getFormUrl(formName);
			if (Debug.isDebugEnabled()) Debug.debug("touchForm(" + async + ") - formAlreadyPresent: " + form.getName());
		}

		// if sync wait until we got response from client as it is loaded
		if (!async)
		{
			if (!getEndpoint().isFormAttachedToDOM(formName))
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

	protected void updateController(Form form, final String realFormName, final boolean forceLoad, IFormHTMLAndJSGenerator formTemplateGenerator)
	{
		try
		{
			Pair<String, Boolean> urlAndCopyState = getRealFormURLAndSeeIfItIsACopy(form, realFormName, true);
			final String realUrl = urlAndCopyState.getLeft();
			boolean copy = urlAndCopyState.getRight().booleanValue();

			boolean needsToGenerateTemplates = (copy || !Boolean.valueOf(System.getProperty("servoy.generateformscripts", "false")).booleanValue());
			final String jsTemplate = (needsToGenerateTemplates ? formTemplateGenerator.generateJS() : "");
			final String htmlTemplate = (needsToGenerateTemplates ? formTemplateGenerator.generateHTMLTemplate() : "");

			// update endpoint URL if needed
			String previousURL = getEndpoint().getFormUrl(realFormName);
			String realURLWithoutSessionId = dropSessionIdFrom(realUrl);
			if (previousURL != null && !realURLWithoutSessionId.equals(previousURL))
			{
				getEndpoint().formDestroyed(realFormName);
				getEndpoint().addFormIfAbsent(realFormName, realURLWithoutSessionId);
			}

			CurrentWindow.runForWindow(this, new Runnable()
			{

				@Override
				public void run()
				{
					if (getSession().getClient().isEventDispatchThread() && forceLoad)
					{
						try
						{
							getSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeServiceCall("updateController",
								new Object[] { realFormName, jsTemplate, realUrl, Boolean.valueOf(forceLoad), htmlTemplate });
						}
						catch (IOException e)
						{
							Debug.error(e);
						}
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
		String clientUsedFormURL = getEndpoint().getFormUrl(realName);
		if (clientUsedFormURL != null)
		{
			changed = !clientUsedFormURL.equals(getRealFormURLAndSeeIfItIsACopy(flattenedForm, realName, false).getLeft());
		}
		return changed;
	}

	protected String dropSessionIdFrom(String realNewURL)
	{
		// drop the "?sessionId=...." or "&sessionId=..." when comparing cause those are not part of end-point kept URLs
		return realNewURL.substring(0, realNewURL.indexOf("sessionId=") - 1);
	}

	protected Pair<String, Boolean> getRealFormURLAndSeeIfItIsACopy(Form form, String realFormName, boolean addSessionID)
	{
		FlattenedSolution fs = getSession().getClient().getFlattenedSolution();
		Solution sc = fs.getSolutionCopy(false);
		String realUrl = getDefaultFormURLStart(form, realFormName);
		boolean copy = false;

		if (sc != null && sc.getChild(form.getUUID()) != null)
		{
			realUrl = realUrl + "?lm:" + form.getLastModified() + (addSessionID ? "&sessionId=" + getSession().getUuid() : "");
			copy = true;
		}
		else if (!form.getName().endsWith(realFormName))
		{
			realUrl = realUrl + "?lm:" + form.getLastModified() + (addSessionID ? "&sessionId=" + getSession().getUuid() : "");
		}
		else
		{
			realUrl = realUrl + (addSessionID ? "?sessionId=" + getSession().getUuid() : "");
		}

		return new Pair<String, Boolean>(realUrl, Boolean.valueOf(copy));
	}

	@Override
	public void updateForm(Form form, String name, IFormHTMLAndJSGenerator formTemplateGenerator)
	{
		if (hasForm(name))
		{
			// if form was not sent to client, do not send now; this is just recreateUI
			updateController(form, name, false, formTemplateGenerator);
		}
	}

	@Override
	public boolean hasForm(String realName)
	{
		INGClientWebsocketEndpoint endpoint = getEndpoint();
		return endpoint != null && endpoint.getFormUrl(realName) != null;
	}

	protected String getDefaultFormURLStart(Form form, String name)
	{
		return "solutions/" + form.getSolution().getName() + "/forms/" + name + ".html";
	}

	public void destroyForm(String name)
	{
		try
		{
			getSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeServiceCall("destroyController", new Object[] { name });
		}
		catch (IOException e)
		{
			Debug.log("Error sending destroy command to client for form: " + name, e);
		}
		getEndpoint().formDestroyed(name);
	}

	/**
	 * This 'resolved' is equivalent to client-side 'resolved' state. As sablo puts it, it means that a form is attached to the DOM of this window or not.
	 */
	public void setFormResolved(String formName, boolean resolved)
	{
		String formUrl = getEndpoint().getFormUrl(formName);
		if (formUrl != null)
		{
			synchronized (formUrl)
			{
				getEndpoint().setAttachedToDOM(formName, resolved);
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
		String formUrl = getEndpoint().getFormUrl(formName);
		if (formUrl != null)
		{
			synchronized (formUrl)
			{
				return getEndpoint().isFormAttachedToDOM(formName);
			}
		}
		return false;
	}

	@Override
	public Object executeServiceCall(String serviceName, String functionName, Object[] arguments, WebObjectFunctionDefinition apiFunction,
		IToJSONWriter<IBrowserConverterContext> pendingChangesWriter, boolean blockEventProcessing) throws IOException
	{
		Pair<UUID, UUID> perfId = getClient().onStartSubAction(serviceName, functionName, apiFunction, arguments);
		try
		{
			return super.executeServiceCall(serviceName, functionName, arguments, apiFunction, pendingChangesWriter, blockEventProcessing);
		}
		finally
		{
			if (perfId != null) getClient().onStopSubAction(perfId);
		}
	}

}
