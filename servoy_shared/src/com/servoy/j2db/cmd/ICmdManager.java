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
package com.servoy.j2db.cmd;


import java.util.EventObject;

import javax.swing.Action;

import com.servoy.j2db.IManager;

/**
 * The Action and Command manager
 * 
 * @author jblok
 */
public interface ICmdManager extends IManager
{
	/**
	 * Execute a known previously registered command.
	 * 
	 * @param name
	 */
	public void executeRegisteredAction(String name);

	/**
	 * Register an action.
	 * 
	 * @param name the name
	 * @param a the action
	 */
	public void registerAction(String name, Action a);

	/**
	 * Get a previously registered command.
	 * 
	 * @param name
	 * @return Action
	 */
	public Action getRegisteredAction(String name);

	/**
	 * Execute a ICmd.
	 * 
	 * @param c the comamnd
	 * @param ie the event which causes this (or null if none)
	 */
	public void executeCmd(ICmd c, EventObject ie);

}
