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

import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.WrapperContainer;
import com.servoy.j2db.server.headlessclient.dataui.WebCellBasedView.CellContainer;
import com.servoy.j2db.server.headlessclient.dataui.WebCellBasedView.WebCellBasedViewListViewItem;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.scripting.AbstractRuntimeRendersupportComponent;
import com.servoy.j2db.util.Debug;

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
					boolean hasOnRender = false;
					Iterator iterator2 = li.iterator();
					while (iterator2.hasNext())
					{
						ArrayList<Object> cellDisplays = new ArrayList<Object>();
						Object cell = iterator2.next();
						cell = CellContainer.getContentsForCell((Component)cell);

						if (cell instanceof WebCellBasedViewListViewItem)
						{
							Iterator listItemIte = ((WebCellBasedViewListViewItem)cell).iterator();
							while (listItemIte.hasNext())
							{
								Object listItemDisplay = listItemIte.next();
								if (listItemDisplay instanceof WrapperContainer)
								{
									listItemDisplay = ((WrapperContainer)listItemDisplay).getDelegate();
								}
								cellDisplays.add(listItemDisplay);
							}
						}
						else
						{
							cellDisplays.add(cell);
						}

						for (Object cellDisplay : cellDisplays)
						{
							if (cellDisplay instanceof IProviderStylePropertyChanges && cellDisplay instanceof IDisplayData &&
								((IDisplayData)cellDisplay).getDataProviderID() == dataprovider)
							{
								// only test if it is not already changed
								view.checkForValueChanges(cellDisplay);

								// do fire on render on all components for record change
								if (cellDisplay instanceof ISupportOnRender && cellDisplay instanceof IScriptableProvider)
								{
									IScriptable so = ((IScriptableProvider)cellDisplay).getScriptObject();
									if (so instanceof AbstractRuntimeRendersupportComponent &&
										((ISupportOnRenderCallback)so).getRenderEventExecutor().hasRenderCallback())
									{
										String componentDataproviderID = ((AbstractRuntimeRendersupportComponent)so).getDataProviderID();
										if (record != null || (e.getName() != null && e.getName().equals(componentDataproviderID)))
										{
											((ISupportOnRender)cellDisplay).fireOnRender(true);
											hasOnRender = true;
										}
									}
								}
							}
						}
					}
					if (record != null || (!hasOnRender && !canChangeValue(e))) break;
				}
			}
		}
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
				Iterator<Column> columns = tableObj.getColumns().iterator();
				while (columns.hasNext())
				{
					Column col = columns.next();
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

				// check if is is calculation
				try
				{
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
				catch (RepositoryException ex)
				{
					Debug.error("Cannot determin if " + dataprovider + " is a calculation");
				}
			}
		}
		return isDBDataproviderObj.booleanValue();
	}

	private boolean canChangeValue(ModificationEvent e)
	{
		IRecord record = e.getRecord();
		if (record == null && isDBDataprovider()) // it is a change of a global or form variable, and the dataprovider is a db value
		{
			return false;
		}
		return true;
	}
}
