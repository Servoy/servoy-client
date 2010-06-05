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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;

import com.servoy.j2db.server.headlessclient.dataui.WebEventExecutor;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.util.Debug;


/**
 * HTML submit form
 * 
 * @author jcompagner
 */
public final class ServoyForm extends Form
{
	private static final long serialVersionUID = 1L;
	private final List delayedActions = new ArrayList();

	/**
	 * @param id
	 */
	public ServoyForm(String id)
	{
		super(id);
	}

	/**
	 * @see wicket.markup.html.form.Form#onSubmit()
	 */
	@Override
	protected void onSubmit()
	{

	}

	/**
	 * @see wicket.markup.html.form.Form#process()
	 */
	@Override
	public boolean process()
	{
		try
		{
			return super.process();
		}
		finally
		{
			processDelayedActions();
		}
	}

	/**
	 * 
	 */
	public void processDelayedActions()
	{
		for (int i = 0; i < delayedActions.size(); i++)
		{
			IDelayedAction sa = (IDelayedAction)delayedActions.get(i);
			try
			{
				WebEventExecutor.setSelectedIndex(sa.getComponent(), null, IEventExecutor.MODIFIERS_UNSPECIFIED);
				sa.execute();
			}
			catch (RuntimeException re)
			{
				Debug.error("Error executing a delayed(datachange action)", re);
			}
		}
		delayedActions.clear();
	}

	/**
	 * @param webEventExecutor
	 * @return
	 */
	public void addDelayedAction(IDelayedAction delayedAction)
	{
		delayedActions.add(delayedAction);
	}

	public String getHiddenField()
	{
		return getHiddenFieldId();
	}

	public String getJavascriptCssId()
	{
		return super.getJavascriptId();
	}


	public interface IDelayedAction
	{
		public void execute();

		/**
		 * @return
		 */
		public Component getComponent();
	}

	@Override
	public String getMarkupId()
	{
		return getId();
	}
}