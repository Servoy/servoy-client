/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.util;

/**
 * Interface for message dispatchers, broadcast messages to all handlers registered to the same subject
 * 
 * @author rgansevles
 *
 */
public interface IMessageDispatcher
{
	/**
	 * Register to a subject.
	 * 
	 * @param subject
	 * @param handler
	 */
	void register(String subject, IMessageHandler handler);

	/**
	 * Deregister from a subject.
	 * 
	 * @param subject
	 * @param handler
	 */
	void deregister(String subject, IMessageHandler handler);

	/**
	 * Send a message to all handlers registered to the subject.
	 * When the sender is non-null, that handler will not receive the message.
	 * 
	 * @param subject
	 * @param message
	 * @param sender
	 */
	void sendMessage(String subject, String message, IMessageHandler sender);
}
