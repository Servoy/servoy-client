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

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.dataui.WebCellBasedView.CellContainer;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.util.Utils;

/**
 * A {@link IDataAdapter} used in {@link WebCellBasedView} for there columns to handle valuechanged events to make sure that the right cells are set to changed.
 * 
 * @author jcompagner
 */
public class WebCellAdapter implements IDataAdapter
{
	private final ArrayList listners = new ArrayList();


	private final String dataprovider;
	private final WebCellBasedView view;

	public WebCellAdapter(String dataprovider, WebCellBasedView view)
	{
		this.dataprovider = dataprovider;
		this.view = view;
	}

	public void addDataListener(IDataAdapter l)
	{
		if (!listners.contains(l) && l != this) listners.add(l);
	}

	public void removeDataListener(IDataAdapter listner)
	{
		listners.remove(listner);
	}

	public void displayValueChanged(ModificationEvent event)
	{
	}

	public String getDataProviderID()
	{
		return dataprovider;
	}

	public void setFindMode(boolean b)
	{
	}

	public void setRecord(IRecordInternal state)
	{
	}

	public void valueChanged(ModificationEvent e)
	{
		MainPage mp = view.findParent(MainPage.class);
		if (mp != null) mp.touch();

		IRecord record = e.getRecord();
		Iterator iterator = ((MarkupContainer)view.getTable()).iterator();
		while (iterator.hasNext())
		{
			Object next = iterator.next();
			if (next instanceof ListItem)
			{
				ListItem li = (ListItem)next;
				Object modelObject = li.getModelObject();
				if (record == null || modelObject == record)
				{
					Iterator iterator2 = li.iterator();
					while (iterator2.hasNext())
					{
						Object cell = iterator2.next();
						cell = CellContainer.getContentsForCell((Component)cell);
						if (cell instanceof IProviderStylePropertyChanges && cell instanceof IDisplayData)
						{
							IModel innermostModel = ((Component)cell).getInnermostModel();
							if (innermostModel instanceof RecordItemModel)
							{
								Object lastRenderedValue = ((RecordItemModel)innermostModel).getLastRenderedValue((Component)cell);
								Object object = ((Component)cell).getDefaultModelObject();
								if (!Utils.equalObjects(lastRenderedValue, object))
								{
									((IProviderStylePropertyChanges)cell).getStylePropertyChanges().setValueChanged();
								}
							}
							else
							{
								((IProviderStylePropertyChanges)cell).getStylePropertyChanges().setChanged();
							}
						}
					}
					if (record != null) break;
				}
			}
		}
	}
}
