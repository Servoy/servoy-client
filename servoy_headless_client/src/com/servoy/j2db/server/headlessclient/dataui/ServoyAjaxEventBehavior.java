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
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;

import com.servoy.j2db.server.headlessclient.IDesignModeListener;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.util.Utils;

/**
 * Base class for all the ajax behaviors.
 * This one makes sure that onAjaxError() is called in the browser if there is an error.
 * It will disable it self if the webclient is not in ajax mode or if the component is in design mode. 
 * 
 * @author jcompagner
 */
public abstract class ServoyAjaxEventBehavior extends AjaxEventBehavior implements IDesignModeListener
{
	private boolean designMode;
	private String sharedName;

	/**
	 * @param event
	 */
	public ServoyAjaxEventBehavior(String event)
	{
		super(event);
	}

	public ServoyAjaxEventBehavior(String event, String sharedName)
	{
		super(event);
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
			return super.generateCallbackScript(partialCall);
		}
		else
		{
			return getJSEventName() + "(event, '" + getCallbackUrl(false) + "', '" + getComponent().getMarkupId() + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}


	private String getJSEventName()
	{
		return getEvent() + sharedName;
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
	protected CharSequence getPreconditionScript()
	{
		WebClientSession webClientSession = WebClientSession.get();
		if (webClientSession != null && webClientSession.blockRequest()) return "onABC();" + super.getPreconditionScript(); //$NON-NLS-1$
		return "onAjaxCall();" + super.getPreconditionScript(); //$NON-NLS-1$
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
		if (webClientSession != null && webClientSession.blockRequest()) return "wicketHide('blocker');"; //$NON-NLS-1$
		return null;
	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#isEnabled(org.apache.wicket.Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		return WebClientSession.get().useAjax() && super.isEnabled(component) && !designMode;
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.IDesignModeListener#setDesignMode(boolean)
	 */
	public void setDesignMode(boolean designMode)
	{
		this.designMode = designMode;
	}

	public boolean isDesignMode()
	{
		return designMode;
	}

}