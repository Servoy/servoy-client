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
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.string.AppendingStringBuffer;

import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IScriptReadOnlyMethods;

/**
 * @author jcompagner
 * 
 */
public class ServoyChoiceComponentUpdatingBehavior extends AbstractServoyDefaultAjaxBehavior
{
	private static final long serialVersionUID = 1L;

	protected final Component component;
	protected final WebEventExecutor eventExecutor;

	/**
	 * @param event
	 * @param eventExecutor
	 */
	public ServoyChoiceComponentUpdatingBehavior(Component component, WebEventExecutor eventExecutor)
	{
		super();
		this.component = component;
		this.eventExecutor = eventExecutor;
	}

	/**
	 * @see wicket.ajax.form.AjaxFormComponentUpdatingBehavior#onUpdate(wicket.ajax.AjaxRequestTarget)
	 */
	protected void onUpdate(AjaxRequestTarget target)
	{
		eventExecutor.onEvent(JSEvent.EventType.none, target, component, IEventExecutor.MODIFIERS_UNSPECIFIED);
	}

	/**
	 * @see wicket.ajax.AbstractDefaultAjaxBehavior#renderHead(wicket.markup.html.IHeaderResponse)
	 */
	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);

		response.renderOnLoadJavascript("attachChoiceHandlers('" + component.getMarkupId() + "', function() {" + getEventHandler() + "});"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		response.renderOnLoadJavascript("fixTabIndexes('" + component.getMarkupId() + "');"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Called to handle any error resulting from updating form component. Errors thrown from {@link #onUpdate(AjaxRequestTarget)} will not be caught here.
	 * 
	 * The RuntimeException will be null if it was just a validation or conversion error of the FormComponent
	 * 
	 * @param target
	 * @param e
	 */
	protected void onError(AjaxRequestTarget target, RuntimeException e)
	{
		if (e != null) throw e;
	}

	/**
	 * @see wicket.behavior.AbstractBehavior#isEnabled(Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		if (super.isEnabled(component))
		{
			if (component instanceof IScriptReadOnlyMethods)
			{
				return !((IScriptReadOnlyMethods)component).js_isReadOnly() && ((IScriptReadOnlyMethods)component).js_isEnabled();
			}
			return true;
		}
		return false;
	}


	/**
	 * 
	 * @see wicket.behavior.AbstractAjaxBehavior#onBind()
	 */
	@Override
	protected void onBind()
	{
		super.onBind();

		if (!(getComponent() instanceof FormComponent))
		{
			throw new WicketRuntimeException("Behavior " + getClass().getName() + " can only be added to an instance of a FormComponent");
		}
	}

	/**
	 * 
	 * @return FormComponent
	 */
	protected final FormComponent getFormComponent()
	{
		return (FormComponent)getComponent();
	}

	/**
	 * @see wicket.ajax.AjaxEventBehavior#getEventHandler()
	 */
	protected final CharSequence getEventHandler()
	{
		return generateCallbackScript(new AppendingStringBuffer("wicketAjaxPost('").append(getCallbackUrl()).append(
			"', wicketSerializeForm(document.getElementById('" + getComponent().getMarkupId() + "',false))"));
	}

	/**
	 * @see wicket.ajax.AjaxEventBehavior#onCheckEvent(java.lang.String)
	 */
	protected void onCheckEvent(String event)
	{
		if ("href".equalsIgnoreCase(event))
		{
			throw new IllegalArgumentException("this behavior cannot be attached to an 'href' event");
		}
	}

	/**
	 * 
	 * @see wicket.ajax.AbstractDefaultAjaxBehavior#respond(wicket.ajax.AjaxRequestTarget)
	 */
	@Override
	protected final void respond(final AjaxRequestTarget target)
	{
		final FormComponent formComponent = getFormComponent();

		try
		{
			formComponent.inputChanged();
			formComponent.validate();
			if (formComponent.hasErrorMessage())
			{
				formComponent.invalid();

				onError(target, null);
			}
			else
			{
				formComponent.valid();
				formComponent.updateModel();
				onUpdate(target);
			}
		}
		catch (RuntimeException e)
		{
			onError(target, e);

		}
	}
}
