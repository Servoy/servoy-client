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
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;

import com.servoy.j2db.server.headlessclient.IDesignModeListener;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.util.Utils;

/**
 * Base class for all the form component updating behaviors.
 * This one makes sure that onAjaxError() is called in the browser if there is an error.
 * It will disable it self if the webclient is not in ajax mode or if the component is in design mode. 
 * 
 * @author jcompagner
 * 
 */
public abstract class ServoyAjaxFormComponentUpdatingBehavior extends AjaxFormComponentUpdatingBehavior implements IDesignModeListener
{
	private boolean designMode;
	protected String sharedName;

	/**
	 * @param event
	 */
	public ServoyAjaxFormComponentUpdatingBehavior(String event)
	{
		super(event);
	}

	public ServoyAjaxFormComponentUpdatingBehavior(String event, String sharedName)
	{
		super(event);
		this.sharedName = sharedName;
	}

	protected boolean isRenderHead;

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
			return super.generateCallbackScript(partialCall);
		}
		else
		{
			return getJSEventName() + "(event, '" + getCallbackUrl(false) + "', '" + getComponent().getMarkupId() + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/**
	 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#getFailureScript()
	 */
	@Override
	protected CharSequence getFailureScript()
	{
		return "onAjaxError();"; //$NON-NLS-1$
	}

	/**
	 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#getPreconditionScript()
	 */
	@Override
	protected final CharSequence getPreconditionScript()
	{
		return getPreconditionAjaxCall() + super.getPreconditionScript();
	}

	protected String getPreconditionAjaxCall()
	{
		return "onAjaxCall();"; //$NON-NLS-1$
	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#isEnabled(org.apache.wicket.Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		return super.isEnabled(component) && !designMode && WebClientSession.get().useAjax();
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.IDesignModeListener#setDesignMode(boolean)
	 */
	public void setDesignMode(boolean designMode)
	{
		this.designMode = designMode;
	}

	protected String getJSEventName()
	{
		return getEvent() + sharedName;
	}
}