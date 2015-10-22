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
package com.servoy.j2db.smart.cmd;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import com.servoy.j2db.Messages;

/**
 * @author jcompagner
 */
public abstract class MessageTextAction extends AbstractAction
{
	private String messageKey = null;

	public MessageTextAction(String messageKey)
	{
		super();
		this.messageKey = messageKey;
		if (messageKey != null) putValue(Action.DEFAULT, messageKey);
	}

	/**
	 * @param name
	 */
	public MessageTextAction(String name, String messageKey)
	{
		super(name);
		this.messageKey = messageKey;
		if (messageKey != null) putValue(Action.DEFAULT, messageKey);
	}

	/**
	 * @param name
	 * @param icon
	 */
	public MessageTextAction(String name, String messageKey, Icon icon)
	{
		super(name, icon);
		this.messageKey = messageKey;
		if (messageKey != null) putValue(Action.DEFAULT, messageKey);
	}

	public void refresh()
	{
		if (messageKey != null)
		{
			String name = Messages.getString(messageKey);
			putValue(Action.NAME, name);
			putValue(Action.SHORT_DESCRIPTION, name);
			String mnemonic = Messages.getString(messageKey + ".mnemonic"); //$NON-NLS-1$
			if (mnemonic != null && mnemonic.length() == 1)
			{
				putValue(Action.MNEMONIC_KEY, new Integer(mnemonic.charAt(0)));
			}
		}
	}
}
