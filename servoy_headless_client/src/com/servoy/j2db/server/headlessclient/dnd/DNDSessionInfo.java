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

package com.servoy.j2db.server.headlessclient.dnd;

import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.dnd.DRAGNDROP;
import com.servoy.j2db.ui.IComponent;

/**
 * Class used to store current drag and drop operation details 
 * 
 * @author gboros
 */
public class DNDSessionInfo
{
	private Object data;
	private String mimeType;
	private int currentOperation = DRAGNDROP.NONE;
	private IComponent component;
	private boolean dropResult;

	public Object getData()
	{
		return data;
	}

	public void setData(Object data, String mimeType)
	{
		this.data = data;
		if (mimeType == null)
		{
			if (data instanceof String)
			{
				this.mimeType = "application/x-java-serialized-object";
			}
			else if (data instanceof Record)
			{
				this.mimeType = DRAGNDROP.MIME_TYPE_SERVOY_RECORD;
			}
			else
			{
				this.mimeType = DRAGNDROP.MIME_TYPE_SERVOY;
			}
		}
		else
		{
			this.mimeType = mimeType;
		}
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public IComponent getComponent()
	{
		return component;
	}

	public void setComponent(IComponent component)
	{
		this.component = component;
	}

	public boolean getDropResult()
	{
		return dropResult;
	}

	public void setDropResult(boolean dropResult)
	{
		this.dropResult = dropResult;
	}

	public int getCurrentOperation()
	{
		return currentOperation;
	}

	public void setCurrentOperation(int currentOperation)
	{
		this.currentOperation = currentOperation;
	}
}
