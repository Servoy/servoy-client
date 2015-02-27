/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient.design;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.NGClientWindow;
import com.servoy.j2db.server.ngclient.NGRuntimeWindow;
import com.servoy.j2db.server.ngclient.NGRuntimeWindowManager;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Sablo window for NGClient, used in developer.
 * 
 * @author rgansevles
 *
 */
public class DesignNGClientWindow extends NGClientWindow
{

	/**
	 * @param websocketSession
	 * @param windowName
	 */
	public DesignNGClientWindow(INGClientWebsocketSession websocketSession, String windowName)
	{
		super(websocketSession, windowName);
	}

	@Override
	public void formCreated(final String formName)
	{
		getSession().getEventDispatcher().addEvent(new Runnable()
		{
			@Override
			public void run()
			{
				IWebFormController form = getClient().getFormManager().getForm(formName);
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
				form.notifyVisible(true, invokeLaterRunnables);
				Utils.invokeLater(getClient(), invokeLaterRunnables);
				touchForm(form.getForm(), form.getForm().getName(), true);
				formCreatedImp(formName);
			}
		});
	}

	private void formCreatedImp(String formName)
	{
		super.formCreated(formName);
	}

	@Override
	protected void updateController(Form form, String realFormName, String formUrl, boolean forceLoad)
	{
		try
		{
			// in design the controller should be quickly set in the current generated window.
			// (only the first form is set as main, for tabs this should be already set)
			NGRuntimeWindow currentWindow = getClient().getRuntimeWindowManager().getCurrentWindow();
			if (currentWindow != null && currentWindow.getController() == null)
			{
				IWebFormController controller = getClient().getFormManager().getForm(realFormName);
				currentWindow.setController(controller);
			}
			String realUrl = formUrl + "?lm:" + System.currentTimeMillis() + "&sessionId=" + getSession().getUuid();
			StringWriter sw = new StringWriter(512);
			// for al js code design flag should be true.
			new FormTemplateGenerator(new ServoyDataConverterContext(getClient()), true, true).generate(form, realFormName, "form_recordview_js.ftl", sw);
			getSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("updateController",
				new Object[] { realFormName, sw.toString(), realUrl, Boolean.valueOf(forceLoad) });
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
	}


}
