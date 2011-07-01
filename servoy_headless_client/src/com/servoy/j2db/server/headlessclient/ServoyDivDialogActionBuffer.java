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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;

public class ServoyDivDialogActionBuffer
{

	private class Action
	{

		public static final int OP_SHOW = 1;
		public static final int OP_CLOSE = 2;
		public static final int OP_DIALOG_ADDED_OR_REMOVED = 3;
		public static final int OP_TO_FRONT = 4;
		public static final int OP_TO_BACK = 5;

		private final ServoyDivDialog divDialog;
		private final int operation;
		private final Object[] parameters;

		public Action(ServoyDivDialog divDialog, int operation, Object[] parameters)
		{
			this.divDialog = divDialog;
			this.operation = operation;
			this.parameters = parameters;
		}

		public ServoyDivDialog getDivDialog()
		{
			return divDialog;
		}

		public int getOperation()
		{
			return operation;
		}

		public Object[] getParameters()
		{
			return parameters;
		}

	}

	private final List<Action> buffer = new ArrayList<Action>();

	private boolean closePopup = false;

	public void add(ServoyDivDialog divDialog, WebMarkupContainer parentToUpdate)
	{
		buffer.add(new Action(divDialog, Action.OP_DIALOG_ADDED_OR_REMOVED, new Object[] { parentToUpdate }));
	}

	public void remove(ServoyDivDialog divDialog, WebMarkupContainer parentToUpdate)
	{
		buffer.add(new Action(divDialog, Action.OP_DIALOG_ADDED_OR_REMOVED, new Object[] { parentToUpdate }));
	}

	public void show(ServoyDivDialog divDialog, String pageMapName)
	{
		buffer.add(new Action(divDialog, Action.OP_SHOW, new Object[] { pageMapName }));
	}

	public boolean isClosing()
	{
		return closePopup;
	}

	public void close(ServoyDivDialog divDialog)
	{
		closePopup = true;
		buffer.add(new Action(divDialog, Action.OP_CLOSE, null));
	}

	public void toFront(ServoyDivDialog divDialog)
	{
		buffer.add(new Action(divDialog, Action.OP_TO_FRONT, null));
	}

	public void toBack(ServoyDivDialog divDialog)
	{
		buffer.add(new Action(divDialog, Action.OP_TO_BACK, null));
	}

	public void apply(AjaxRequestTarget target)
	{
		closePopup = false;
		for (Action a : buffer)
		{
			ServoyDivDialog divDialog = a.getDivDialog();
			switch (a.getOperation())
			{
				case Action.OP_SHOW :
					if (!divDialog.isShown())
					{
						divDialog.setPageMapName((String)a.getParameters()[0]);
						divDialog.show(target);
					}
					break;
				case Action.OP_CLOSE :
					if (divDialog.isShown())
					{
						divDialog.close(target);
					}
					break;
				case Action.OP_TO_FRONT :
					if (divDialog.getPageMapName() != null && divDialog.isShown())
					{
						divDialog.toFront(target);
					}
					break;
				case Action.OP_TO_BACK :
					if (divDialog.getPageMapName() != null && divDialog.isShown())
					{
						divDialog.toBack(target);
					}
					break;
				case Action.OP_DIALOG_ADDED_OR_REMOVED :
					target.addComponent((WebMarkupContainer)a.getParameters()[0]);
					break;
			}
		}
		buffer.clear();
	}

	public boolean hasActions()
	{
		return !buffer.isEmpty();
	}

}