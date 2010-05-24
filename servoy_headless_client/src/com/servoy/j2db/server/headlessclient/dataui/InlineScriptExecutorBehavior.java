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
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.servoy.j2db.ExitScriptException;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * 
 */
public final class InlineScriptExecutorBehavior extends AbstractServoyDefaultAjaxBehavior
{
	/**
	 * 
	 */
	private final Component component;

	/**
	 * @param webDataHtmlView
	 */
	InlineScriptExecutorBehavior(Component component)
	{
		this.component = component;
	}

	private static final long serialVersionUID = 1L;

	@Override
	protected void respond(AjaxRequestTarget target)
	{
		Page page = component.getPage();
		String scriptName = RequestCycle.get().getRequest().getParameter("scriptname");
		WebForm wf = component.findParent(WebForm.class);
		if (wf != null)
		{
			try
			{
				wf.getController().eval(scriptName);
			}
			catch (Exception e)
			{
				if (!(e instanceof ExitScriptException))
				{
					Debug.error("Exception evaluating: " + scriptName, e); //$NON-NLS-1$
				}
			}
			WebEventExecutor.generateResponse(target, page);
		}
		target.appendJavascript("clearDoubleClickId('" + component.getMarkupId() + "')"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}