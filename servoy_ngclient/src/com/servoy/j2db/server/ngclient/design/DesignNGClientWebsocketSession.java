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

package com.servoy.j2db.server.ngclient.design;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.sablo.specification.WebComponentSpecification;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.impl.ClientService;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.NGRuntimeWindowManager;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public final class DesignNGClientWebsocketSession extends NGClientWebsocketSession
{
	public static final String EDITOR_CONTENT_SERVICE = "$editorContentService";

	private static final WebComponentSpecification EDITOR_CONTENT_SERVICE_SPECIFICATION = new WebComponentSpecification(EDITOR_CONTENT_SERVICE, "",
		EDITOR_CONTENT_SERVICE, null, null, "", null);

	/**
	 * @param uuid
	 */
	public DesignNGClientWebsocketSession(String uuid)
	{
		super(uuid);
	}

	@Override
	protected IClientService createClientService(String name)
	{
		if (EDITOR_CONTENT_SERVICE.equals(name))
		{
			return new ClientService(EDITOR_CONTENT_SERVICE, EDITOR_CONTENT_SERVICE_SPECIFICATION);
		}
		return super.createClientService(name);
	}

	@Override
	protected void updateController(Form form, String realFormName, String formUrl, boolean forceLoad)
	{
		try
		{
			String realUrl = formUrl + "?lm:" + System.currentTimeMillis() + "&sessionId=" + getUuid();
			StringWriter sw = new StringWriter(512);
			boolean tableview = (form.getView() == IFormConstants.VIEW_TYPE_TABLE || form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED);
			String view = (tableview ? "tableview" : "recordview");
			new FormTemplateGenerator(new ServoyDataConverterContext(getClient()), true).generate(form, realFormName, "form_" + view + "_js.ftl", sw);
			getService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("updateController",
				new Object[] { realFormName, sw.toString(), realUrl, Boolean.valueOf(forceLoad) });
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
	}

	@Override
	public void formCreated(final String formName)
	{
		getEventDispatcher().addEvent(new Runnable()
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
}