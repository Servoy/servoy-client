/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.sablo.IEventHandler;

import com.servoy.j2db.dataprocessing.FoundSetEvent;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetEventListener;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;

/**
 * @author obuligan
 *
 */
public class DefaultNavigatorWebComponent extends WebFormComponent implements IFoundSetEventListener, ListSelectionListener
{
	private IFoundSet foundset;

	/**
	 * @param name
	 * @param fe
	 * @param dataAdapterList
	 */
	public DefaultNavigatorWebComponent(IDataAdapterList dataAdapterList)
	{
		super(DefaultNavigator.NAME_PROP_VALUE, new FormElement(DefaultNavigator.INSTANCE, new ServoyDataConverterContext(dataAdapterList.getForm()),
			new PropertyPath()), dataAdapterList);
		addEventHandler(DefaultNavigator.SETELECTEDINDEX_FUNCTION_NAME, new IEventHandler()
		{
			@Override
			public Object executeEvent(Object[] args)
			{
				if (foundset != null)
				{
					foundset.setSelectedIndex(((Integer)args[0]).intValue() - 1);
				}
				return null;
			}
		});
	}

	public void newFoundset(IFoundSet fs)
	{
		if (this.foundset != null)
		{
			this.foundset.removeFoundSetEventListener(this);
			((ISwingFoundSet)foundset).getSelectionModel().removeListSelectionListener(this);
		}
		this.foundset = fs;
		if (this.foundset != null)
		{
			this.foundset.addFoundSetEventListener(this);
			((ISwingFoundSet)foundset).getSelectionModel().addListSelectionListener(this);
		}
		foundsetChanged();
	}

	public void setCurrentIndex(int index)
	{
		setProperty(DefaultNavigator.CURRENTINDEX_PROP, Integer.valueOf(index));
	}

	public void setMaxIndex(int maxIndex)
	{
		setProperty(DefaultNavigator.MAXINDEX_PROP, Integer.valueOf(maxIndex));
	}

	private void foundsetChanged()
	{
		if (foundset != null)
		{
			setCurrentIndex(foundset.getSelectedIndex() + 1);
			setMaxIndex(foundset.getSize());
		}
		else
		{
			setCurrentIndex(0);
			setMaxIndex(0);
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		foundsetChanged();
	}

	@Override
	public void foundSetChanged(FoundSetEvent e)
	{
		foundsetChanged();
	}
}
