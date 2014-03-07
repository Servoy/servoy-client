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

package com.servoy.j2db.debug;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.servoy.j2db.IDebugClient;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.INGClientEndpoint;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.NGRuntimeWindow;

/**
 * @author jcompagner
 *
 */
public class DebugNGClient extends NGClient implements IDebugClient
{
	private final IDesignerCallback designerCallback;

	/**
	 * @param webSocketClientEndpoint
	 * @param designerCallback 
	 */
	public DebugNGClient(INGClientEndpoint webSocketClientEndpoint, IDesignerCallback designerCallback)
	{
		super(webSocketClientEndpoint);
		this.designerCallback = designerCallback;
	}

	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		RemoteDebugScriptEngine engine = new RemoteDebugScriptEngine(this);

		if (designerCallback != null)
		{
			designerCallback.addScriptObjects(this, engine.getSolutionScope());
		}
		return engine;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IDebugClient#setCurrent(com.servoy.j2db.persistence.Solution)
	 */
	@Override
	public void setCurrent(Solution current)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IDebugClient#refreshForI18NChange(boolean)
	 */
	@Override
	public void refreshForI18NChange(boolean recreateForms)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IDebugClient#refreshPersists(java.util.Collection)
	 */
	@Override
	public void refreshPersists(Collection<IPersist> changes)
	{
		Set<Form> changedForms = new HashSet<Form>();
		for (IPersist persist : changes)
		{
			if (persist instanceof IFormElement)
			{
				ISupportChilds parent = persist.getParent();
				while (parent != null && !(parent instanceof Form))
				{
					parent = parent.getParent();
				}
				if (parent instanceof Form)
				{
					changedForms.add((Form)parent);
				}
			}
		}
		if (changedForms.size() > 0)
		{
			boolean mustReload = false;
			ComponentFactory.reload();
			Collection<IWebFormController> activeFormControllers = getFormManager().getActiveFormControllers();
			for (Form form : changedForms)
			{
				for (IWebFormController formController : activeFormControllers)
				{
					if (formController.getForm().equals(form))
					{
						formController.getFormUI().init();
						mustReload = true;
					}
				}
			}

			if (mustReload) getActiveWebSocketClientEndpoint().executeServiceCall(NGRuntimeWindow.WINDOW_SERVICE, "reload", null);
		}

	}

	/**
	 * @param form
	 */
	public void show(Form form)
	{
		// TODO Auto-generated method stub

	}

}
