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
package com.servoy.j2db.server.headlessclient;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * Web-client DIV window customised to Servoy needs.
 * @author acostescu
 */
public class ServoyDivDialog extends DivWindow
{

	private boolean closeAll = false; // for legacy modal dialog behavior (when multiple forms were shown in same dialog and close would either close one at a time or all)

	public ServoyDivDialog(String id)
	{
		super(id);
	}

	public ServoyDivDialog(String id, IModel< ? > model, boolean isInsideIFrame)
	{
		super(id, model, isInsideIFrame);
	}

	public void setCloseAll(boolean closeAll)
	{
		this.closeAll = closeAll;
	}

	public boolean getCloseAll()
	{
		return closeAll;
	}

	@Deprecated
	@Override
	public void show(AjaxRequestTarget target)
	{
		if (!isShown())
		{
			target.appendJavascript("Wicket.Window.unloadConfirmation = false;");
		}
		super.show(target);
	}

	protected Page page;

	public void setPage(Page page)
	{
		this.page = page;
	}
}