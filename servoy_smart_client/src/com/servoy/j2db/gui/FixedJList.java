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
package com.servoy.j2db.gui;


import java.util.Vector;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.text.Position;

/**
 * @author jcompagner
 */
public class FixedJList extends JList
{
	private CellStringValue cellStringValue;

	/**
	 * @param dataModel
	 */
	public FixedJList(ListModel dataModel)
	{
		super(dataModel);
	}

	/**
	 * @param listData
	 */
	public FixedJList(Object[] listData)
	{
		super(listData);
	}

	/**
	 * @param listData
	 */
	public FixedJList(Vector listData)
	{
		super(listData);
	}

	/**
	 * 
	 */
	public FixedJList()
	{
		super();
	}

	@Override
	public int getNextMatch(String prefix, int startIndex, Position.Bias bias)
	{
		ListModel model = getModel();
		int max = model.getSize();
		if (prefix == null)
		{
			throw new IllegalArgumentException();
		}
		if (startIndex < 0 || startIndex >= max)
		{
			throw new IllegalArgumentException();
		}
		prefix = prefix.trim().toUpperCase();
		if (prefix.equals("")) return -1; //$NON-NLS-1$

		// start search from the next element after the selected element
		int increment = (bias == Position.Bias.Forward) ? 1 : -1;
		int index = startIndex;
		do
		{
			Object o = model.getElementAt(index);

			if (o != null)
			{
				String string;

				if (cellStringValue != null)
				{
					string = cellStringValue.getValue(o);
				}
				else if (o instanceof String)
				{
					string = (String)o;
				}
				else
				{
					string = o.toString();
				}
				if (string != null)
				{
					string = string.toUpperCase();
				}

				if (string != null && string.startsWith(prefix))
				{
					ensureIndexIsVisible(index);
					return index;
				}
			}
			index = (index + increment + max) % max;
		}
		while (index != startIndex);
		return -1;
	}

	public interface CellStringValue
	{
		public String getValue(Object object);
	}

	/**
	 * @return
	 */
	public CellStringValue getCellStringValue()
	{
		return cellStringValue;
	}

	/**
	 * @param value
	 */
	public void setCellStringValue(CellStringValue value)
	{
		cellStringValue = value;
	}

}
