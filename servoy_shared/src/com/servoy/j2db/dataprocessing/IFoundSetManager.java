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
package com.servoy.j2db.dataprocessing;


import com.servoy.j2db.IManager;
import com.servoy.j2db.util.ServoyException;

/**
 * Instances of this class manages all the foundsets in a client
 * 
 * @author jblok
 */
public interface IFoundSetManager extends IManager
{
	public IFoundSet getGlobalRelatedFoundSet(String relationName) throws ServoyException;

	/**
	 * Get current shared foundset for some table,use this method to retrieve data from or manipulate a Form foundset.
	 * 
	 * @param dataSource
	 * @return IFoundSet
	 * @throws Exception
	 */
	public IFoundSet getSharedFoundSet(String dataSource) throws ServoyException;


	/**
	 * Get a new uninitialized foundset for some data source not being attached to any Form.
	 * 
	 * @param dataSource
	 * @return IFoundSet
	 * @throws ServoyException
	 */
	public IFoundSet getNewFoundSet(String dataSource) throws ServoyException;


	/**
	 * Test if shared.
	 * 
	 * @param fs
	 * @return true if shared
	 */
	public boolean isShared(IFoundSet fs);

	/**
	 * Test if new.
	 * 
	 * @param fs
	 * @return true if new
	 */
	public boolean isNew(IFoundSet fs);
}
