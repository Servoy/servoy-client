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
package com.servoy.j2db.server.headlessclient.dataui;

import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.util.model.ComboModelListModelWrapper;

/**
 * ComboModelListModelWrapper to be used in WebClient (ComboModelListModelWrapper is implements List based on display values, web components expects List based
 * on real values)
 * 
 * @author rgansevles
 * 
 */
public class WebComboModelListModelWrapper extends ComboModelListModelWrapper
{

	public WebComboModelListModelWrapper(IValueList listModel, boolean shouldHideEmptyValueIfPresent)
	{
		super(listModel, shouldHideEmptyValueIfPresent);
	}

	@Override
	public int indexOf(Object o)
	{
		int index = listModel.realValueIndexOf(o);
		if (hideFirstValue) index--;
		return index;
	}

	@Override
	public Object get(int index)
	{
		return getRealElementAt(index);
	}

	@Override
	public boolean contains(Object o)
	{
		return listModel.realValueIndexOf(o) != -1;
	}

	@Override
	public Object[] toArray()
	{
		int s = size();
		Object[] a = new Object[s];
		for (int i = 0; i < s; i++)
			a[i] = get(i);

		return a;
	}

	public boolean compareTo(Object[] list)
	{
		int currentListSize = size();
		if (list != null && currentListSize == list.length)
		{
			for (int i = 0; i < currentListSize; i++)
			{
				if (get(i) == null)
				{
					if (list[i] != null) return false;
				}
				else
				{
					if (list[i] == null || !get(i).equals(list[i])) return false;
				}
			}

			return true;
		}

		return false;
	}
}
