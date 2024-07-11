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

import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;

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
	private Boolean isDBDataproviderObj;

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
	}


	private boolean isDBDataprovider()
	{
		if (isDBDataproviderObj == null)
		{
			isDBDataproviderObj = Boolean.FALSE;
			ITable table = view.getDataAdapterList().getFormController().getTable();
			if (table instanceof Table)
			{
				Table tableObj = (Table)table;
				for (Column col : tableObj.getColumns())
				{
					ColumnInfo ci = col.getColumnInfo();
					if (ci != null && ci.isExcluded())
					{
						continue;
					}
					if (col.getDataProviderID() == dataprovider)
					{
						isDBDataproviderObj = Boolean.TRUE;
						break;
					}
				}

				Iterator<TableNode> tableNodes = view.getDataAdapterList().getApplication().getFlattenedSolution().getTableNodes(table);
				while (tableNodes.hasNext())
				{
					TableNode tableNode = tableNodes.next();
					if (tableNode != null)
					{
						Iterator<IPersist> it2 = tableNode.getAllObjects();
						while (it2.hasNext())
						{
							IPersist persist = it2.next();
							if (persist instanceof IDataProvider && (((IDataProvider)persist).getDataProviderID() == dataprovider))
							{
								isDBDataproviderObj = Boolean.FALSE;
							}
						}
					}
				}
			}
		}
		return isDBDataproviderObj.booleanValue();
	}
}
