/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that is able to easily create, keep, clear and provide messages.
 * 
 * @author acostescu
 */
public class MessageKeeper implements IMessageProvider
{

	protected List<Message> messages;

	public void addInfo(String info)
	{
		createMessagesListIfNeeded();
		messages.add(new Message(info, Message.INFO));
	}

	public void addWarning(String warning)
	{
		createMessagesListIfNeeded();
		messages.add(new Message(warning, Message.WARNING));
	}

	public void addError(String error)
	{
		createMessagesListIfNeeded();
		messages.add(new Message(error, Message.ERROR));
	}

	protected void createMessagesListIfNeeded()
	{
		if (messages == null) messages = new ArrayList<Message>();
	}

	public void clearMessages()
	{
		messages = null;
	}

	public Message[] getMessages()
	{
		return (messages != null) ? messages.toArray(new Message[messages.size()]) : new Message[0];
	}

	public void addAll(Message[] messagesToAdd)
	{
		if (messagesToAdd != null && messagesToAdd.length > 0)
		{
			createMessagesListIfNeeded();
			messages.addAll(Arrays.asList(messagesToAdd));
		}
	}

	@Override
	public String toString()
	{
		return messages == null ? "" : messages.toString(); //$NON-NLS-1$
	}

}