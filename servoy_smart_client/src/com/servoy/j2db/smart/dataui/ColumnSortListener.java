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
package com.servoy.j2db.smart.dataui;


import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Timer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.ISaveConstants;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.gui.LFAwareSortableHeaderRenderer;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.smart.TableView;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Default sorter for tableview
 * 
 * @author jblok
 */
public class ColumnSortListener extends MouseAdapter
{
	private final IApplication application;
	private final TableView table;
	private final FormController fc;//can be null if portal!
	private final Map<Integer, Boolean> lastColumnIndex = new HashMap<Integer, Boolean>();
	private boolean lastSortAsc = false;
	private Timer sortTimer = null;

	public ColumnSortListener(IApplication app, TableView table, FormController fc)
	{
		this.application = app;
		this.table = table;
		this.fc = fc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked(final MouseEvent e)
	{
		if (!table.isEnabled()) return;
		if (e.getButton() == MouseEvent.BUTTON1)
		{
			if (fc != null) fc.setLastKeyModifiers(e.getModifiers());
			TableColumnModel colModel = table.getColumnModel();
			if (colModel == null)
			{
				return;
			}
			int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
			if (columnModelIndex < 0)
			{
				return;
			}

			final TableColumn column = colModel.getColumn(columnModelIndex);
			if (column == null)
			{
				return;
			}

			int modelIndex = column.getModelIndex();
			if (modelIndex < 0)
			{
				return;
			}
			if (table.getModel() instanceof IFoundSetInternal &&
				application.getFoundSetManager().getEditRecordList().stopIfEditing((IFoundSetInternal)table.getModel()) != ISaveConstants.STOPPED)
			{
				return;
			}
			Integer columnIndex = Integer.valueOf(columnModelIndex);
			if (lastColumnIndex.containsKey(columnIndex))
			{
				lastSortAsc = !lastColumnIndex.get(columnIndex).booleanValue();
			}
			else
			{
				lastSortAsc = true;
				if (fc != null && column instanceof CellAdapter && ((CellAdapter)column).getDataProviderID() != null && e.getClickCount() <= 1 &&
					table.getModel() instanceof IFoundSetInternal)
				{
					IFoundSetInternal foundset = (IFoundSetInternal)table.getModel();
					List<SortColumn> sortCols = foundset.getSortColumns();
					if (sortCols != null && sortCols.size() > 0)
					{
						CellAdapter ca = (CellAdapter)column;
						List<String> sortingProviders = null;
						Component renderer = ca.getRenderer();
						if (renderer instanceof ISupportValueList && ((ISupportValueList)renderer).getValueList() != null)
						{
							try
							{
								sortingProviders = DBValueList.getShowDataproviders(((ISupportValueList)renderer).getValueList().getValueList(),
									(Table)foundset.getTable(), ca.getDataProviderID(), application.getFoundSetManager());
							}
							catch (RepositoryException ex)
							{
								Debug.error(ex);
							}
						}

						if (sortingProviders == null)
						{
							// no related sort, use sort on dataProviderID instead
							sortingProviders = Collections.singletonList(ca.getDataProviderID());
						}

						for (String sortingProvider : sortingProviders)
						{
							SortColumn existingSc;
							try
							{
								FoundSetManager fsm = (FoundSetManager)foundset.getFoundSetManager();
								existingSc = fsm.getSortColumn(foundset.getTable(), sortingProvider, false);
							}
							catch (Exception ex)
							{
								Debug.error(ex);
								continue;
							}

							for (SortColumn sc : sortCols)
							{
								if (sc.equalsIgnoreSortorder(existingSc))
								{
									lastSortAsc = sc.getSortOrder() == SortColumn.DESCENDING;
								}
							}
						}
					}
				}
			}

			if (!e.isShiftDown())
			{
				// clear previous data
				lastColumnIndex.clear();
			}
			lastColumnIndex.put(columnIndex, Boolean.valueOf(lastSortAsc));

			if (column instanceof CellAdapter && table.getModel() instanceof IFoundSetInternal)
			{
				try
				{
					if (sortTimer != null)
					{
						sortTimer.stop();
					}
					sortTimer = new Timer(300, new AbstractAction()
					{
						public void actionPerformed(ActionEvent event)
						{
							try
							{
								String dataProviderID = ((CellAdapter)column).getDataProviderID();
								int labelForOnActionMethodId = 0;
								if (((CellAdapter)column).getHeaderRenderer() instanceof LFAwareSortableHeaderRenderer)
								{
									labelForOnActionMethodId = ((LFAwareSortableHeaderRenderer)((CellAdapter)column).getHeaderRenderer()).getOnActionMethodID();
								}
								if (fc != null && labelForOnActionMethodId > 0)
								{
									LFAwareSortableHeaderRenderer renderer = (LFAwareSortableHeaderRenderer)(((CellAdapter)column).getHeaderRenderer());
									fc.executeFunction(
										String.valueOf(labelForOnActionMethodId),
										Utils.arrayMerge((new Object[] { getJavaScriptEvent(e, JSEvent.EventType.action, renderer.getName()) }),
											Utils.parseJSExpressions(renderer.getFlattenedMethodArguments("onActionMethodID"))), true, null, false, "onActionMethodID"); //$NON-NLS-1$//$NON-NLS-2$
								}
								else if (fc != null && fc.getForm().getOnSortCmdMethodID() > 0)
								{
									// Also execute the on sort command on none data providers (like a label) then they can do there own sort.
									fc.executeFunction(
										String.valueOf(fc.getForm().getOnSortCmdMethodID()),
										Utils.arrayMerge(
											(new Object[] { dataProviderID, Boolean.valueOf(lastSortAsc), getJavaScriptEvent(e, JSEvent.EventType.none, null) }),
											Utils.parseJSExpressions(fc.getForm().getFlattenedMethodArguments("onSortCmdMethodID"))), true, null, false, "onSortCmdMethodID"); //$NON-NLS-1$//$NON-NLS-2$
								}
								else if (dataProviderID != null && fc.getForm().getOnSortCmdMethodID() != -1)
								{
									List<String> sortingProviders = null;
									IFoundSetInternal model = (IFoundSetInternal)table.getModel();
									Component renderer = ((CellAdapter)column).getRenderer();
									if (renderer instanceof ISupportValueList && ((ISupportValueList)renderer).getValueList() != null)
									{
										try
										{
											sortingProviders = DBValueList.getShowDataproviders(((ISupportValueList)renderer).getValueList().getValueList(),
												(Table)model.getTable(), dataProviderID, application.getFoundSetManager());
										}
										catch (RepositoryException ex)
										{
											Debug.error(ex);
										}
									}

									if (sortingProviders == null)
									{
										// no related sort, use sort on dataProviderID instead
										sortingProviders = Collections.singletonList(dataProviderID);
									}
									try
									{
										List<SortColumn> list = e.isShiftDown() ? model.getSortColumns() : new ArrayList<SortColumn>();
										for (String sortingProvider : sortingProviders)
										{
											FoundSetManager fsm = ((FoundSetManager)model.getFoundSetManager());
											SortColumn sc = fsm.getSortColumn(model.getTable(), sortingProvider, false);
											if (sc != null && sc.getColumn().getDataProviderType() != IColumnTypes.MEDIA)
											{
												for (SortColumn oldColumn : list)
												{
													if (oldColumn.equalsIgnoreSortorder(sc))
													{
														sc = oldColumn;
														break;
													}
												}
												if (!list.contains(sc)) list.add(sc);
												sc.setSortOrder(lastSortAsc ? SortColumn.ASCENDING : SortColumn.DESCENDING);
											}
											model.sort(list, false);
										}
									}
									catch (Exception ex)
									{
										Debug.error(ex);
									}
								}
							}
							finally
							{
								sortTimer.stop();
							}
						}
					});
					sortTimer.start();
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (!table.isEnabled()) return;
		if (e.getButton() == MouseEvent.BUTTON1)
		{
			application.getFoundSetManager().getEditRecordList().stopEditing(false);
		}
	}

	public JSEvent getJavaScriptEvent(MouseEvent e, JSEvent.EventType type, String sourceName)
	{
		JSEvent event = new JSEvent();
		event.setType(type);
		if (fc != null) event.setFormName(fc.getName());
		event.setElementName(sourceName);
		event.setModifiers(e.getModifiers() == IEventExecutor.MODIFIERS_UNSPECIFIED ? 0 : e.getModifiers());
		return event;
	}
}
