/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.event.ListDataListener;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.dataprocessing.FoundSetEvent;
import com.servoy.j2db.dataprocessing.IFoundSetEventListener;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.component.WebComponentSpec;
import com.servoy.j2db.server.ngclient.property.PropertyDescription;
import com.servoy.j2db.server.ngclient.property.PropertyType;
import com.servoy.j2db.server.websocket.utils.JSONUtils.JSONWritable;
import com.servoy.j2db.util.Debug;

/**
 * @author gboros
 *
 */
public class WebGridFormUI extends WebFormUI implements IFoundSetEventListener
{
	public static final int PAGE_SIZE = 25;

	private final IWebFormController formController;
	private final INGApplication application;

	private int currentPage = 1;
	private IFoundSetInternal currentFoundset;
	private final List<RowData> rowChanges = new ArrayList<RowData>();
	private boolean allChanged = false;
	private int startTabSeqIndex = 1;

	/**
	 * @param application 
	 * @param name
	 * @param form
	 * @param application
	 * @param listener
	 */
	public WebGridFormUI(INGApplication application, IWebFormController formController)
	{
		super(formController);
		this.application = application;
		this.formController = formController;

	}

	@Override
	public Map<String, Map<String, Object>> getAllProperties()
	{
		Map<String, Map<String, Object>> props = super.getAllProperties();
		appendRows(props.get(""), getRows(-1, -1).rows);
		return props;
	}

	@Override
	@SuppressWarnings("nls")
	public Map<String, Map<String, Object>> getAllChanges()
	{
		Map<String, Map<String, Object>> props = super.getAllChanges();
		if (allChanged)
		{
			try
			{
				List<Map<String, Object>> rows = getRows(-1, -1).rows;
				if (!props.containsKey("")) props.put("", new HashMap<String, Object>());
				appendRows(props.get(""), rows);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		else if (rowChanges != null && rowChanges.size() > 0)
		{
			if (!props.containsKey("")) props.put("", new HashMap<String, Object>());
			Map<String, Object> rowProps = props.get("");
			rowProps.put("updatedRows", rowChanges);
			rowProps.put("totalRows", Integer.toString(formController.getFoundSet().getSize()));
		}
		allChanged = false;
		rowChanges.clear();
		return props;
	}

	private void appendRows(Map<String, Object> props, List<Map<String, Object>> rows)
	{
		if (rows != null)
		{
			props.put("rows", rows);
			props.put("totalRows", Integer.toString(formController.getFoundSet().getSize()));
			props.put("currentPage", Integer.valueOf(currentPage));
			props.put("selectedIndex", Integer.toString(formController.getFoundSet().getSelectedIndex()));
		}
	}

	public void setCurrentPage(int currentPage)
	{
		this.currentPage = currentPage;
	}

	/**
	 * set the selected record in the DAL by the given pkhash+index string.
	 * If the record can't be found anymore then false is returned saying the index didn't move to the right record.
	 * @param pkHashAndIndex
	 * @return
	 */
	public boolean setEditingRowByPkHash(String pkHashAndIndex)
	{
		int index = pkHashAndIndex.lastIndexOf("_");
		int recordIndex = Integer.parseInt(pkHashAndIndex.substring(index + 1));
		String pkHash = pkHashAndIndex.substring(0, index);
		IRecordInternal record = currentFoundset.getRecord(recordIndex);
		if (record != null && !pkHash.equals(record.getPKHashKey())) return currentFoundset.setSelectedIndex(pkHash,
			Math.min(recordIndex + 5, currentFoundset.getSize())) != -1;
		else currentFoundset.setSelectedIndex(recordIndex);
		return true;
	}

	public int getSelectedViewIndex()
	{
		int selectedIndex = formController.getFoundSet().getSelectedIndex();
		return selectedIndex - ((currentPage - 1) * PAGE_SIZE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataprocessing.IFoundSetEventListener#foundSetChanged(com.servoy.j2db.dataprocessing.FoundSetEvent)
	 */
	@Override
	public void foundSetChanged(FoundSetEvent event)
	{
		if (allChanged) return;
		if (event.getType() == FoundSetEvent.FIND_MODE_CHANGE || event.getType() == FoundSetEvent.FOUNDSET_INVALIDATED)
		{
			// fully changed push everything
			setAllChanged();
		}
		else if (event.getType() == FoundSetEvent.CONTENTS_CHANGED)
		{
			// partial change only push the changes.
			if (event.getChangeType() == FoundSetEvent.CHANGE_DELETE)
			{
				int startIdx = (currentPage - 1) * PAGE_SIZE;
				int endIdx = currentPage * PAGE_SIZE;
				if (endIdx > currentFoundset.getSize()) endIdx = currentFoundset.getSize();
				if (event.getFirstRow() < endIdx)
				{
					int startRow = Math.max(startIdx, event.getFirstRow());
					int numberOfDeletes = Math.min(event.getLastRow(), endIdx) - event.getFirstRow() + 1;

					RowData data = getRows(endIdx - Math.min(PAGE_SIZE, numberOfDeletes), endIdx);

					rowChanges.add(new RowData(data.rows, startRow - startIdx, startRow + numberOfDeletes - startIdx, RowData.DELETE));
				}
				else if (endIdx == 0)
				{
					// a delete all, set to all changed.
					setAllChanged();
				}
			}
			else if (event.getChangeType() == FoundSetEvent.CHANGE_INSERT)
			{
				int startIdx = (currentPage - 1) * PAGE_SIZE;
				int endIdx = currentPage * PAGE_SIZE;
				if (endIdx > currentFoundset.getSize()) endIdx = currentFoundset.getSize();
				if (event.getFirstRow() < endIdx)
				{
					int startRow = Math.max(startIdx, event.getFirstRow());
					int numberOfInserts = Math.min(event.getLastRow(), endIdx) - event.getFirstRow() + 1;

					RowData rows = getRows(startRow, Math.min(startRow + numberOfInserts, endIdx));
					rows.setType(RowData.INSERT);
					rowChanges.add(rows);
				}
			}
			else if (event.getChangeType() == FoundSetEvent.CHANGE_UPDATE)
			{
				// get the rows that are changed.
				RowData rows = getRows(event.getFirstRow(), event.getLastRow() + 1);
				if (rows != RowData.EMPTY)
				{
					rowChanges.add(rows);
				}
			}
		}
		application.getChangeListener().valueChanged();
	}

	/**
	 * 
	 */
	private void setAllChanged()
	{
		allChanged = true;
		rowChanges.clear();
		currentPage = 1;
	}

	@SuppressWarnings("nls")
	private RowData getRows(int startRow, int lastRow)
	{
		if (currentFoundset == null) return RowData.EMPTY;

		List<Map<String, Object>> rows = new ArrayList<>();
		int startIdx = (currentPage - 1) * PAGE_SIZE;
		int endIdx = currentPage * PAGE_SIZE;
		if (endIdx > currentFoundset.getSize()) endIdx = currentFoundset.getSize();

		int foundsetStartRow = startIdx;
		int startOffset = 0;
		if (startRow != -1)
		{
			if (startRow < endIdx && lastRow > startIdx)
			{
				startIdx = Math.max(startIdx, startRow);
				endIdx = Math.min(endIdx, lastRow);
				startOffset = Math.max(startIdx - startRow, 0);
			}
			else return RowData.EMPTY;
		}
		int currentIndex = startTabSeqIndex + startOffset * components.values().size();
		for (int i = startIdx; i < endIdx; i++)
		{
			IRecordInternal record = currentFoundset.getRecord(i);
			dataAdapterList.setRecord(record, false);
			Map<String, Object> rowProperties = new HashMap<String, Object>();
			rowProperties.put("_svy_pk", record.getPKHashKey() + "_" + i);
			for (WebComponent wc : components.values())
			{
				// TODO: add tagsstring (%%custname%%) 
				List<String> dataproviders = getWebComponentPropertyType(wc, PropertyType.dataprovider);
				Map<String, Object> cellProperties = new HashMap<>();
				for (String dataproviderID : dataproviders)
				{
					cellProperties.put(dataproviderID, wc.getProperty(dataproviderID));
				}
				// add valuelists
				Object valuelistObj;
				for (String valuelistProperty : wc.getFormElement().getValuelistProperties())
				{
					if ((valuelistObj = wc.getProperty(valuelistProperty)) instanceof IValueList)
					{
						IValueList valuelist = (IValueList)valuelistObj;
						// if it is related, global or has fallback, then the valuelist depends on the current record
						if (valuelist.getValueList().getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES ||
							valuelist.getValueList().getDatabaseValuesType() == IValueListConstants.RELATED_VALUES || valuelist.getFallbackValueList() != null)

						cellProperties.put(valuelistProperty, new ValuelistWrapper(valuelist, record));
					}
				}

				cellProperties.put("svy_cn", wc.getName());
				// execute onrender.
				rowProperties.put(wc.getName(), cellProperties);
			}
			List<TabSequencePropertyWithComponent> tabSeqComponents = getTabSeqComponents();
			for (TabSequencePropertyWithComponent propertyWithComponent : tabSeqComponents)
			{
				Map<String, Object> cellProperties = (Map<String, Object>)rowProperties.get(propertyWithComponent.getComponent().getName());
				cellProperties.put(propertyWithComponent.getProperty(), currentIndex++);
			}
			rows.add(rowProperties);
		}
		dataAdapterList.setRecord(currentFoundset.getRecord(currentFoundset.getSelectedIndex()), false);

		return new RowData(rows, startIdx - foundsetStartRow, endIdx - foundsetStartRow);
	}

	@Override
	public boolean isDisplayingMoreThanOneRecord()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#setModel(com.servoy.j2db.dataprocessing.IFoundSetInternal)
	 */
	@Override
	public void setModel(IFoundSetInternal fs)
	{
		if (fs == currentFoundset) return;

		if (currentFoundset != null) currentFoundset.removeFoundSetEventListener(this);
		currentFoundset = fs;
		if (currentFoundset != null)
		{
			currentFoundset.addFoundSetEventListener(this);
			setAllChanged();
			application.getChangeListener().valueChanged();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.WebFormUI#destroy()
	 */
	@Override
	public void destroy()
	{
		super.destroy();
		setModel(null);
	}


	private static List<String> getWebComponentPropertyType(WebComponent wc, PropertyType type)
	{
		WebComponentSpec componentSpec = wc.getFormElement().getWebComponentSpec();
		ArrayList<String> properties = new ArrayList<>(3);
		Map<String, PropertyDescription> specProperties = componentSpec.getProperties();
		for (Entry<String, PropertyDescription> e : specProperties.entrySet())
		{
			if (e.getValue().getType() == type)
			{
				properties.add(e.getKey());
			}
		}

		return properties;
	}

	@Override
	public int recalculateTabIndex(int startIndex, TabSequencePropertyWithComponent startComponent)
	{
		this.startTabSeqIndex = startIndex;
		this.nextAvailableTabSequence = startIndex + 500;
		return nextAvailableTabSequence;
	}

	private static class RowData implements JSONWritable
	{
		public static final int CHANGE = 0;
		public static final int INSERT = 1;
		public static final int DELETE = 2;

		private static final RowData EMPTY = new RowData();
		private final List<Map<String, Object>> rows;
		private final int startIndex;
		private final int endIndex;

		private int type;

		private RowData()
		{
			rows = null;
			startIndex = -1;
			endIndex = -1;
			type = -1;
		}

		/**
		 * @param rows
		 * @param i
		 * @param j
		 */
		public RowData(List<Map<String, Object>> rows, int startIndex, int endIndex)
		{
			this(rows, startIndex, endIndex, CHANGE);
		}

		public RowData(List<Map<String, Object>> rows, int startIndex, int endIndex, int type)
		{
			this.rows = rows;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.type = type;
		}

		/**
		 * @param type the type to set
		 */
		public void setType(int type)
		{
			this.type = type;
		}

		public Map<String, Object> toMap()
		{
			Map<String, Object> retValue = new HashMap<>();
			retValue.put("rows", rows);
			retValue.put("startIndex", Integer.valueOf(startIndex));
			retValue.put("endIndex", Integer.valueOf(endIndex));
			retValue.put("type", Integer.valueOf(type));
			return retValue;
		}
	}

	private static class ValuelistWrapper implements IValueList
	{
		private final IValueList valuelist;
		private final ArrayList<Object> elements;
		private final ArrayList<Object> realElements;

		public ValuelistWrapper(IValueList valuelist, IRecordInternal record)
		{
			this.valuelist = valuelist;
			valuelist.fill(record);
			int size = valuelist.getSize();
			elements = new ArrayList<Object>(size);
			realElements = new ArrayList<Object>(size);
			for (int i = 0; i < size; i++)
			{
				elements.add(valuelist.getElementAt(i));
				realElements.add(valuelist.getRealElementAt(i));
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.ListModel#getSize()
		 */
		@Override
		public int getSize()
		{
			return elements.size();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.ListModel#getElementAt(int)
		 */
		@Override
		public Object getElementAt(int index)
		{
			return elements.get(index);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.ListModel#addListDataListener(javax.swing.event.ListDataListener)
		 */
		@Override
		public void addListDataListener(ListDataListener l)
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.ListModel#removeListDataListener(javax.swing.event.ListDataListener)
		 */
		@Override
		public void removeListDataListener(ListDataListener l)
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#getRealElementAt(int)
		 */
		@Override
		public Object getRealElementAt(int row)
		{
			return realElements.get(row);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#getRelationName()
		 */
		@Override
		public String getRelationName()
		{
			return valuelist.getRelationName();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#fill(com.servoy.j2db.dataprocessing.IRecordInternal)
		 */
		@Override
		public void fill(IRecordInternal parentState)
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#realValueIndexOf(java.lang.Object)
		 */
		@Override
		public int realValueIndexOf(Object obj)
		{
			return 0;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#indexOf(java.lang.Object)
		 */
		@Override
		public int indexOf(Object elem)
		{
			return 0;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#deregister()
		 */
		@Override
		public void deregister()
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#getAllowEmptySelection()
		 */
		@Override
		public boolean getAllowEmptySelection()
		{
			return valuelist.getAllowEmptySelection();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#getName()
		 */
		@Override
		public String getName()
		{
			return valuelist.getName();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#hasRealValues()
		 */
		@Override
		public boolean hasRealValues()
		{
			return valuelist.hasRealValues();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#setFallbackValueList(com.servoy.j2db.dataprocessing.IValueList)
		 */
		@Override
		public void setFallbackValueList(IValueList list)
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#getFallbackValueList()
		 */
		@Override
		public IValueList getFallbackValueList()
		{
			return valuelist.getFallbackValueList();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.dataprocessing.IValueList#getValueList()
		 */
		@Override
		public ValueList getValueList()
		{
			return valuelist.getValueList();
		}
	}
}