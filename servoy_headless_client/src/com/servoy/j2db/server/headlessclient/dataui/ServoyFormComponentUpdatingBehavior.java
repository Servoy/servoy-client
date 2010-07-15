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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;

import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IScriptReadOnlyMethods;

/**
 * A {@link AjaxFormComponentUpdatingBehavior} for most fields that redirects {@link #onUpdate(AjaxRequestTarget)} and {@link #onError(AjaxRequestTarget, RuntimeException)} to the {@link WebEventExecutor}
 * 
 * @author jcompagner
 * 
 */
public class ServoyFormComponentUpdatingBehavior extends ServoyAjaxFormComponentUpdatingBehavior
{
	private static final long serialVersionUID = 1L;

	protected final Component component;
	protected final WebEventExecutor eventExecutor;

	/**
	 * @param event
	 * @param eventExecutor
	 */
	public ServoyFormComponentUpdatingBehavior(String event, Component component, WebEventExecutor eventExecutor)
	{
		super(event);
		this.component = component;
		this.eventExecutor = eventExecutor;
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
		eventExecutor.onEvent(JSEvent.EventType.none, target, component, IEventExecutor.MODIFIERS_UNSPECIFIED);
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
	 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#findIndicatorId()
	 */
	@Override
	protected String findIndicatorId()
	{
		return null; // main page defines it and the timer shouldnt show it
	}

	/**
	 * @see wicket.behavior.AbstractBehavior#isEnabled(Component)
	 */
	@Override
	public boolean isEnabled(Component comp)
	{
		if (super.isEnabled(comp))
		{
			if (!eventExecutor.hasLeaveCmds())
			{
				if (comp instanceof IScriptReadOnlyMethods)
				{
					return !((IScriptReadOnlyMethods)comp).js_isReadOnly() && ((IScriptReadOnlyMethods)comp).js_isEnabled();
				}
				return true;
			}
		}
		return false;
	}

}
