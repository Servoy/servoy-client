/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.server.ngclient.scripting;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class ServoyApiObject
{
	private final IApplication app;

	public ServoyApiObject(IApplication app)
	{
		this.app = app;
	}

	@JSFunction
	public boolean hideForm(String formName)
	{
		UUID uuid = Utils.getAsUUID(formName, false);
		if (uuid != null)
		{
			Form form = (Form)app.getFlattenedSolution().searchPersist(uuid);
			if (form != null)
			{
				formName = form.getName();
			}
		}
		IWebFormController formController = (IWebFormController)app.getFormManager().getForm(formName);
		if (formController != null)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			boolean ret = formController.notifyVisible(false, invokeLaterRunnables);
			if (ret)
			{
				formController.setParentFormController(null);
			}
			Utils.invokeAndWait(app, invokeLaterRunnables);
			return ret;
		}
		return false;
	}
}