/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.servoy.j2db.server.headlessclient.AlwaysLastPageVersionRequestListenerInterface;


/**
 * A base {@link AbstractServoyDefaultAjaxBehavior} that will only call {@link #execute(AjaxRequestTarget)} if it was triggered
 * by the active version of the page. If it was triggered before the page version switch, respond() will make sure the behavior is ignored.<br><br>
 * This avoids unintended page version switches (which can result in unresponsive browser pages) or
 * unintended behavior executions (for example after a form switch, an old action executing stuff too fast, before onDomReady of the whole page switch happens
 * in the browser - there was such a situation in Firefox).   
 * 
 * @author acostescu
 * 
 */
public abstract class AbstractServoyLastVersionAjaxBehavior extends AbstractServoyDefaultAjaxBehavior
{

	protected static final String PVS = "pvs"; // parameter used for ignoring requests on older versions of the page //$NON-NLS-1$
	protected static final String PVS_PARAM = "&" + PVS + "="; //$NON-NLS-1$//$NON-NLS-2$


	/**
	 * Equivalent to AbstractServoyDefaultAjaxBehavior's respond(AjaxRequestTarget). But it will be ignored sometime. See class description.
	 */
	protected abstract void execute(AjaxRequestTarget target);

	@Override
	protected final void respond(AjaxRequestTarget target)
	{
		if (String.valueOf(target.getPage().getCurrentVersionNumber()).equals(RequestCycle.get().getRequest().getParameter(PVS)))
		{
			execute(target);
		}
	}

	@Override
	public CharSequence getCallbackUrl(boolean onlyTargetActivePage)
	{
		if (getComponent() == null)
		{
			throw new IllegalArgumentException("Behavior must be bound to a component to create the URL"); //$NON-NLS-1$
		}
		return getComponent().urlFor(this, AlwaysLastPageVersionRequestListenerInterface.INTERFACE) + PVS_PARAM +
			getComponent().getPage().getCurrentVersionNumber();
	}

}
