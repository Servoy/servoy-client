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

package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Point;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPostprocessingCallDecorator;

import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 *
 */
public class ServoySelectBoxUpdatingBehavior extends ServoyFormComponentUpdatingBehavior
{
	private boolean isFireActionCommand;

	public ServoySelectBoxUpdatingBehavior(String event, Component component, WebEventExecutor eventExecutor, String sharedName)
	{
		super(event, component, eventExecutor, sharedName);
	}

	public void setFireActionCommand(boolean isFireActionCommand)
	{
		this.isFireActionCommand = isFireActionCommand;
	}

	@Override
	protected void onUpdate(AjaxRequestTarget target)
	{
		super.onUpdate(target);
		if (isFireActionCommand)
		{
			eventExecutor.onEvent(JSEvent.EventType.action, target, component,
				Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER)),
				new Point(Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("mx")), //$NON-NLS-1$
					Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("my")))); //$NON-NLS-1$
		}
	}

	@Override
	protected CharSequence generateCallbackScript(final CharSequence partialCall)
	{
		return super.generateCallbackScript(partialCall + "+actionParam"); //$NON-NLS-1$
	}

	@Override
	protected IAjaxCallDecorator getAjaxCallDecorator()
	{
		return new AjaxPostprocessingCallDecorator(null)
		{
			private static final long serialVersionUID = 1L;

			@SuppressWarnings("nls")
			@Override
			public CharSequence postDecorateScript(CharSequence script)
			{
				return "var actionParam = Servoy.Utils.getActionParams(event,false); " + script;
			}
		};
	}
}
