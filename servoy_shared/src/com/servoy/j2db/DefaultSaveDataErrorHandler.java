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

import java.util.List;

import com.servoy.j2db.dataprocessing.ISaveDataErrorHandler;

/**
 * @author jcompagner
 *
 */
public class DefaultSaveDataErrorHandler implements ISaveDataErrorHandler
{
	IServiceProvider application;
	
	public DefaultSaveDataErrorHandler(IServiceProvider application)
	{
		this.application = application;
	}
	
	/**
	 * @see com.servoy.j2db.dataprocessing.ISaveDataErrorHandler#saveDataFailed(java.lang.Throwable)
	 */
	public void saveDataFailed(Throwable e)
	{
		application.reportError("Error saving data " + e.getLocalizedMessage(), e);
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.ISaveDataErrorHandler#saveDataFailed(java.util.List)
	 */
	public void saveDataFailed(List failedRecords)
	{
		StringBuffer sb = new StringBuffer ();
		for(int i=0; i<failedRecords.size();i++)
		{
			Object[] array = (Object[])failedRecords.get(i);
			sb.append(((Exception)array[1]).getLocalizedMessage());
			sb.append("\n");
		}
		application.reportError("Error saving " + failedRecords.size()+ " records",sb.toString());
	}

}
