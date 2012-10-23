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

import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.scripting.api.IJSEvent.EventType;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.runtime.HasRuntimeEnabled;
import com.servoy.j2db.ui.runtime.HasRuntimeReadOnly;
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
		super(event, sharedName);
		this.component = component;
		this.eventExecutor = eventExecutor;
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
			return super.generateCallbackScript(partialCall);
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

	@Override
	protected String getPreconditionAjaxCall()
	{
		WebClientSession webClientSession = WebClientSession.get();
		if (webClientSession != null && webClientSession.blockRequest()) return "onABC();"; //$NON-NLS-1$
		return super.getPreconditionAjaxCall();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#getSuccessScript()
	 */
	@Override
	protected CharSequence getSuccessScript()
	{
		WebClientSession webClientSession = WebClientSession.get();
		if (webClientSession != null && webClientSession.blockRequest()) return "hideBlocker();"; //$NON-NLS-1$
		return null;
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.ServoyFormComponentUpdatingBehavior#isEnabled(Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		if (!super.isEnabled(component))
		{
			return false;
		}

		if (component instanceof IScriptableProvider)
		{
			IScriptable scriptObject = ((IScriptableProvider)component).getScriptObject();
			if (scriptObject instanceof HasRuntimeReadOnly)
			{
				if (((HasRuntimeReadOnly)scriptObject).isReadOnly())
				{
					return false;
				}
			}
			if (scriptObject instanceof HasRuntimeEnabled)
			{
				if (!((HasRuntimeEnabled)scriptObject).isEnabled())
				{
					return false;
				}
			}
		}

		return true;
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

	@Override
	protected String getJSEventName()
	{
		String eventName = super.getJSEventName();
		if (getComponent() instanceof WebDataTextArea) eventName += "TextArea"; //$NON-NLS-1$
		return eventName;
	}
}
