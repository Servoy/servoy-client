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
package com.servoy.j2db.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import com.servoy.j2db.dataprocessing.Record;

/**
 * Class used to define transfer data for drag and drop operations in smart client
 * 
 * @author gboros
 */
public class FormDataTransferable implements Transferable
{
	private final Object data;
	private final DataFlavor dataFlavor;

	public FormDataTransferable(Object o, String mimeType) throws ClassNotFoundException
	{
		this.data = o;
		if (mimeType == null)
		{
			if (o instanceof String)
			{
				this.dataFlavor = DataFlavor.stringFlavor;
			}
			else if (o instanceof Record)
			{
				this.dataFlavor = new DataFlavor(DRAGNDROP.MIME_TYPE_SERVOY_RECORD);
			}
			else
			{
				this.dataFlavor = new DataFlavor(DRAGNDROP.MIME_TYPE_SERVOY);
			}
		}
		else
		{
			this.dataFlavor = new DataFlavor(mimeType);
		}
	}

	/**
	 * @see java.awt.datatransfer.Transferable#getTransferData(java.awt.datatransfer.DataFlavor)
	 */
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
	{
		if (dataFlavor == flavor)
		{
			return data;
		}
		return null;
	}

	/**
	 * @see java.awt.datatransfer.Transferable#getTransferDataFlavors()
	 */
	public DataFlavor[] getTransferDataFlavors()
	{
		return new DataFlavor[] { dataFlavor };
	}

	/**
	 * @see java.awt.datatransfer.Transferable#isDataFlavorSupported(java.awt.datatransfer.DataFlavor)
	 */
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		return dataFlavor == flavor;
	}


}
