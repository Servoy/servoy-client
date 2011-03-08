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

import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.CancelEventIfNoAjaxDecorator;
import org.apache.wicket.ajax.markup.html.IAjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.SubmitLink;

import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.util.Utils;

/**
 * A {@link SubmitLink} that can be configured to use ajax.
 * s
 * @author jcompagner
 */
public abstract class ServoySubmitLink extends SubmitLink implements IAjaxLink
{
	private String inputId;
	private final boolean useAJAX;
	private int modifiers;

	public ServoySubmitLink(String id, boolean useAJAX)
	{
		super(id);
		this.useAJAX = useAJAX;
		if (useAJAX)
		{
			add(new ServoyAjaxEventBehavior("onclick")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onEvent(AjaxRequestTarget target)
				{
					modifiers = Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER));
					onClick(target);
				}

				@Override
				public CharSequence getCallbackUrl(boolean onlyTargetActivePage)
				{
					return super.getCallbackUrl(true);
				}

				@Override
				protected IAjaxCallDecorator getAjaxCallDecorator()
				{
					return new CancelEventIfNoAjaxDecorator(ServoySubmitLink.this.getAjaxCallDecorator());
				}

				@Override
				protected void onComponentTag(ComponentTag tag)
				{
					// only render handler if link is enabled
					if (isLinkEnabled())
					{
						super.onComponentTag(tag);
					}
				}

				@Override
				protected CharSequence generateCallbackScript(final CharSequence partialCall)
				{
					return super.generateCallbackScript(partialCall + "+'&modifiers='+getModifiers(event)"); //$NON-NLS-1$
				}
			});
		}
	}

	/**
	 * @see org.apache.wicket.markup.html.form.SubmitLink#onComponentTag(org.apache.wicket.markup.ComponentTag)
	 */
	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		if (!useAJAX && tag.getName().equalsIgnoreCase("button"))
		{
			tag.put("type", "submit");
		}
	}

	protected IAjaxCallDecorator getAjaxCallDecorator()
	{
		return null;
	}

	@Override
	public void onSubmit()
	{
		onClick(null);
	}

	/**
	 * @see wicket.markup.html.form.FormComponent#getInputName()
	 */
	@Override
	public String getInputName()
	{
		if (inputId == null)
		{
			Page page = findPage();
			if (page instanceof MainPage)
			{
				inputId = ((MainPage)page).nextInputNameId();
			}
			else
			{
				return super.getInputName();
			}
		}
		return inputId;
	}

	public int getModifiers()
	{
		return modifiers;
	}

	/**
	 * Callback for the onClick event. If ajax failed and this event was generated via a normal link the target argument will be null
	 * 
	 * @param target ajax target if this linked was invoked using ajax, null otherwise
	 */
	public abstract void onClick(final AjaxRequestTarget target);
}
