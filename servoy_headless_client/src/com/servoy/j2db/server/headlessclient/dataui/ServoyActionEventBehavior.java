/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.server.headlessclient.dataui;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.markup.html.IHeaderResponse;

import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.scripting.JSEvent.EventType;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IScriptReadOnlyMethods;
import com.servoy.j2db.util.Utils;

/**
 * The ajax behavior for handling the onAction or enter in a TextField.
 * 
 * @author jcompagner
 * 
 */
public class ServoyActionEventBehavior extends ServoyAjaxFormComponentUpdatingBehavior
{
	private static final long serialVersionUID = 1L;

	protected final Component component;
	protected final WebEventExecutor eventExecutor;

	private String sharedName;

	/**
	 * @param event
	 * @param eventExecutor
	 */
	public ServoyActionEventBehavior(String event, Component component, WebEventExecutor eventExecutor)
	{
		super(event);
		this.component = component;
		this.eventExecutor = eventExecutor;
	}

	public ServoyActionEventBehavior(String event, Component component, WebEventExecutor eventExecutor, String sharedName)
	{
		super(event);
		this.component = component;
		this.eventExecutor = eventExecutor;
		this.sharedName = sharedName;
	}

	private boolean isRenderHead;

	@Override
	public void renderHead(IHeaderResponse response)
	{
		isRenderHead = true;
		super.renderHead(response);

		if (sharedName != null)
		{
			CharSequence eh = getEventHandler();
			CharSequence callbackUrl = getCallbackUrl(false);
			String compId = getComponent().getMarkupId();
			String newEh = Utils.stringReplace(eh.toString(), "'" + callbackUrl + "'", "callback"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			newEh = Utils.stringReplace(newEh, "'" + compId + "'", "componentId"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			response.renderJavascript("function " + getJSEventName() + "(event, callback, componentId ) { " + newEh + "}", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				getJSEventName());

		}
		isRenderHead = false;
	}

	@Override
	protected CharSequence generateCallbackScript(CharSequence partialCall)
	{
		if (sharedName == null || isRenderHead)
		{
			return super.generateCallbackScript(partialCall + "+'modifiers='+Servoy.Utils.getModifiers(event)"); //$NON-NLS-1$
		}
		else
		{
			return getJSEventName() + "(event, '" + getCallbackUrl(false) + "', '" + getComponent().getMarkupId() + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	@Override
	public CharSequence getCallbackUrl(boolean onlyTargetActivePage)
	{
		return super.getCallbackUrl(true);
	}

	/**
	 * @see wicket.ajax.form.AjaxFormComponentUpdatingBehavior#onUpdate(wicket.ajax.AjaxRequestTarget)
	 */
	@Override
	protected void onUpdate(AjaxRequestTarget target)
	{
		eventExecutor.onEvent(EventType.action, target, getComponent(),
			Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER)));
	}

	/**
	 * @see wicket.ajax.form.AjaxFormComponentUpdatingBehavior#onError(wicket.ajax.AjaxRequestTarget, java.lang.RuntimeException)
	 */
	@Override
	protected void onError(AjaxRequestTarget target, RuntimeException e)
	{
		super.onError(target, e);
		eventExecutor.onError(target, component);
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.ServoyFormComponentUpdatingBehavior#isEnabled(Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		if (super.isEnabled(component))
		{
			if (component instanceof IScriptableProvider && ((IScriptableProvider)component).getScriptObject() instanceof IScriptReadOnlyMethods)
			{
				return !((IScriptReadOnlyMethods)((IScriptableProvider)component).getScriptObject()).js_isReadOnly() &&
					((IScriptReadOnlyMethods)((IScriptableProvider)component).getScriptObject()).js_isEnabled();
			}
			return true;
		}
		return false;
	}

	/**
	 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#getAjaxCallDecorator()
	 */
	@Override
	protected IAjaxCallDecorator getAjaxCallDecorator()
	{
		return new AjaxCallDecorator()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public CharSequence decorateScript(CharSequence script)
			{
				if (component instanceof WebDataTextArea)
				{
					return "testEnterKey(event, function() {" + script + "});"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return "return testEnterKey(event, function() {" + script + "});"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		};
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.ServoyFormComponentUpdatingBehavior#findIndicatorId()
	 */
	@Override
	protected String findIndicatorId()
	{
		return "indicator"; //$NON-NLS-1$
	}

	private String getJSEventName()
	{
		String eventName = getEvent() + sharedName;
		if (getComponent() instanceof WebDataTextArea) eventName += "TextArea"; //$NON-NLS-1$
		return eventName;
	}
}
