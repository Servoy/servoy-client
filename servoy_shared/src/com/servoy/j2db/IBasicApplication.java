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
package com.servoy.j2db;


import java.util.concurrent.ScheduledExecutorService;

import javax.swing.ImageIcon;

import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.ITaskExecuter;
import com.servoy.j2db.util.IUIBlocker;

/**
 * Basic interface for a minimal UI based application.
 * 
 * @author jblok
 */
public interface IBasicApplication extends IUIBlocker
{
	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 * 
	 * @return Solution
	 * @exclude
	 */
	public Solution getSolution();

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 * 
	 * @return Root
	 * @exclude
	 */
	public FlattenedSolution getFlattenedSolution();

	/**
	 * Get the repository interface
	 * 
	 * @return IRepository
	 */
	public IRepository getRepository();

	/**
	 * Report an error when in a dialog.
	 * 
	 * @param parentComponent
	 * @param msg
	 * @param detail
	 */
//	public void reportError(Object parentComponent, String msg, Object detail);

	/**
	 * Report an info when in a dialog.
	 * 
	 * @param parentComponent
	 * @param msg
	 * @param detail
	 */
//	public void reportInfo(Object parentComponent, String msg, String title);

	/**
	 * Report an error.
	 * 
	 * @param msg
	 * @param detail
	 */
	public void reportError(String msg, Object detail);

	/**
	 * Report a warning in the status (will be shown in red). <br>
	 * <b>Note:</b>Status will be cleared automatically
	 * 
	 * @param s the warning
	 */
	public void reportWarningInStatus(String s);

	/**
	 * Load an application image, manly used internal.
	 * 
	 * @param name of image
	 * @return ImageIcon
	 */
	public ImageIcon loadImage(String name);

	/**
	 * Get the task executor.
	 * 
	 * @return ITaskExecuter
	 * 
	 * @deprecated use {@link #getScheduledExecutor()}
	 */
	@Deprecated
	public ITaskExecuter getThreadPool();


	/**
	 * Get the scheduled executor.
	 * 
	 * @return {@link ScheduledExecutorService}
	 * 
	 */
	public ScheduledExecutorService getScheduledExecutor();

}
