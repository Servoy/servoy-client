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

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;

import com.servoy.j2db.ui.ISupportScroll;
import com.servoy.j2db.util.Utils;

/**
 * Behavior used for updating scroll position of the component
 * @author gboros
 *
 */
public class ScrollBehavior extends ServoyAjaxEventBehavior implements IHeaderContributor
{
	private final ISupportScroll component;

	public ScrollBehavior(ISupportScroll component)
	{
		super("onscroll"); //$NON-NLS-1$
		this.component = component;
	}

	@Override
	public void renderHead(final IHeaderResponse response)
	{
		response.renderOnDomReadyJavascript("Servoy.Utils.setScrollPosition('" + component.getScrollComponentMarkupId() + "', " + component.getScroll().x + ", " + component.getScroll().y + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	protected void onEvent(AjaxRequestTarget target)
	{
		component.setScroll(Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("locationX")), //$NON-NLS-1$
			Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("locationY"))); //$NON-NLS-1$
	}

	@Override
	protected CharSequence getCallbackScript(boolean onlyTargetActivePage)
	{
		return "Servoy.Utils.onScroll('" + component.getScrollComponentMarkupId() + "','" + getCallbackUrl() + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
