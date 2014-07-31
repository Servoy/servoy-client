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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.AggregatedPropertyType;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.JSONUtils.JSONWritable;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.dataprocessing.FoundSetEvent;
import com.servoy.j2db.dataprocessing.IFoundSetEventListener;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.TagStringPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 *
 */
public class WebGridFormUI extends WebFormUI implements IFoundSetEventListener, ListSelectionListener
{
	public static final int HEADER_HEIGHT = 30;

	private int currentPage = 1;
	private IFoundSetInternal currentFoundset;
	private final List<RowData> rowChanges = new ArrayList<RowData>();
	private boolean allChanged = false;
	private boolean selectionChanged = false;
	private int startTabSeqIndex = 1;
	private int pageSize;
	private IFoundSetInternal previousFS;

	/**
	 * @param application
	 * @param name
	 * @param form
	 * @param application
	 * @param listener
	 */
	public WebGridFormUI(IWebFormController formController)
	{
		super(formController);
	}

	@Override
	public TypedData<Map<String, Map<String, Object>>> getAllComponentsProperties()
	{
		TypedData<Map<String, Map<String, Object>>> props = super.getAllComponentsProperties();
		appendRows(props, "", getRows(-1, -1).rows);
		selectionChanged = false;
		allChanged = false;
		rowChanges.clear();
		return props;
	}

	@Override
	@SuppressWarnings("nls")
	public TypedData<Map<String, Map<String, Object>>> getAllComponentsChanges()
	{
		TypedData<Map<String, Map<String, Object>>> props = super.getAllComponentsChanges(); // we might need to use contentType in the future as well
		if (allChanged)
		{
			try
			{
				TypedData<List<Map<String, Object>>> rows = getRows(-1, -1).rows;
				if (!props.content.containsKey("")) props.content.put("", new HashMap<String, Object>());
				appendRows(props, "", rows);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		else if (rowChanges.size() > 0)
		{
			if (!props.content.containsKey(""))
			{
				props.content.put("", new HashMap<String, Object>());
				if (props.contentType == null) props.contentType = AggregatedPropertyType.newAggregatedProperty();
				props.contentType.putProperty("", AggregatedPropertyType.newAggregatedProperty());
			}
			Map<String, Object> rowProps = props.content.get("");
			PropertyDescription rowTypes = props.contentType.getProperty("");
			ArrayList<Map<String, Object>> rowData = new ArrayList<>(rowChanges.size());
			PropertyDescription rowDataTypes = AggregatedPropertyType.newAggregatedProperty();

			for (RowData rowChange : rowChanges)
			{
				TypedData<Map<String, Object>> change = rowChange.toMap();
				rowData.add(change.content);
				if (change.contentType != null) rowDataTypes.putProperty(String.valueOf(rowData.size() - 1), change.contentType);
			}
			rowProps.put("updatedRows", rowData);
			if (rowDataTypes != null) rowTypes.putProperty("updatedRows", rowDataTypes);
			rowProps.put("totalRows", Integer.toString(getController().getFoundSet().getSize()));
			rowProps.put("selectedIndex", Integer.toString(getController().getFoundSet().getSelectedIndex()));
			if (!rowTypes.hasChildProperties()) props.contentType.putProperty("", null);
			if (!props.contentType.hasChildProperties()) props.contentType = null;
		}
		else if (selectionChanged)
		{
			if (!props.content.containsKey("")) props.content.put("", new HashMap<String, Object>());
			Map<String, Object> rowProps = props.content.get("");
			rowProps.put("selectedIndex", Integer.toString(getController().getFoundSet().getSelectedIndex()));
		}
		selectionChanged = false;
		allChanged = false;
		rowChanges.clear();
		return props;
	}

	private void appendRows(TypedData<Map<String, Map<String, Object>>> props, String propsChild, TypedData<List<Map<String, Object>>> rows)
	{
		Map<String, Object> map = props.content.get(propsChild);
		if (props.contentType == null) props.contentType = AggregatedPropertyType.newAggregatedProperty();
		PropertyDescription mapTypes = props.contentType.getProperty(propsChild);
		if (mapTypes == null)
		{
			mapTypes = AggregatedPropertyType.newAggregatedProperty();
			props.contentType.putProperty(propsChild, mapTypes);
		}

		if (rows != null)
		{
			map.put("rows", rows.content);
			if (rows.contentType != null) mapTypes.putProperty("rows", rows.contentType);
			map.put("totalRows", Integer.toString(getController().getFoundSet().getSize()));
			map.put("currentPage", Integer.valueOf(currentPage));
			map.put("selectedIndex", Integer.toString(getController().getFoundSet().getSelectedIndex()));
		}
		if (!mapTypes.hasChildProperties()) props.contentType.putProperty(propsChild, null);
		if (!props.contentType.hasChildProperties()) props.contentType = null;
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
	public static boolean setEditingRowByPkHash(IFoundSetInternal currentFoundset, String pkHashAndIndex)
	{
		Pair<String, Integer> splitHashAndIndex = splitPKHashAndIndex(pkHashAndIndex);
		int recordIndex = splitHashAndIndex.getRight().intValue();
		IRecordInternal record = currentFoundset.getRecord(recordIndex);
		String pkHash = splitHashAndIndex.getLeft();
		if (record != null && !pkHash.equals(record.getPKHashKey()))
		{
			recordIndex = currentFoundset.getRecordIndex(pkHash, recordIndex);
			if (recordIndex != -1)
			{
				currentFoundset.setSelectedIndex(recordIndex);
				return true;
			}
			else return false;
		}
		else currentFoundset.setSelectedIndex(recordIndex);
		return true;
	}

	public static Pair<String, Integer> splitPKHashAndIndex(String pkHashAndIndex)
	{
		int index = pkHashAndIndex.lastIndexOf("_");
		int recordIndex = Integer.parseInt(pkHashAndIndex.substring(index + 1));
		String pkHash = pkHashAndIndex.substring(0, index);
		return new Pair<>(pkHash, Integer.valueOf(recordIndex));
	}

	public boolean setEditingRowByPkHash(String pkHashAndIndex)
	{
		return setEditingRowByPkHash(currentFoundset, pkHashAndIndex);
	}

	public int getSelectedViewIndex()
	{
		int selectedIndex = getController().getFoundSet().getSelectedIndex();
		return selectedIndex - ((currentPage - 1) * getPageSize());
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
				int startIdx = (currentPage - 1) * getPageSize();
				int endIdx = currentPage * getPageSize();
				if ((startIdx <= event.getFirstRow() && event.getFirstRow() < endIdx) || (startIdx <= event.getLastRow() && event.getLastRow() < endIdx))
				{
					// delete already happened so foundset size is changed

					// first row to be deleted inside current page
					int startRow = Math.max(startIdx, event.getFirstRow());
					// number of deletes from current page
					int numberOfDeletes = Math.min(event.getLastRow() + 1, endIdx) - startRow;

					// we need to replace same amount of records in current page; append rows if available
					RowData data = getRows(Math.max(event.getLastRow() + 1, endIdx), Math.max(event.getLastRow() + 1, endIdx) + numberOfDeletes);

					rowChanges.add(new RowData(data.rows, startRow - startIdx, startRow + numberOfDeletes - startIdx, RowData.DELETE));
				}
			}
			else if (event.getChangeType() == FoundSetEvent.CHANGE_INSERT)
			{
				int startIdx = (currentPage - 1) * getPageSize();
				int endIdx = currentPage * getPageSize();
				if (endIdx > currentFoundset.getSize()) endIdx = currentFoundset.getSize();
				if ((startIdx <= event.getFirstRow() && event.getFirstRow() < endIdx) || (startIdx <= event.getLastRow() && event.getLastRow() < endIdx))
				{
					int startRow = Math.max(startIdx, event.getFirstRow());
					// number of inserts from current page
					int numberOfInserts = Math.min(event.getLastRow() + 1, endIdx) - startRow;

					// add records that fit current page
					RowData rows = getRows(startRow, startRow + numberOfInserts);
					rows.setType(RowData.INSERT);
					rowChanges.add(rows);
				}
			}
			else if (event.getChangeType() == FoundSetEvent.CHANGE_UPDATE)
			{
				if (currentFoundset != null && event.getFirstRow() == 0 && event.getLastRow() == currentFoundset.getSize() - 1)
				{
					// if all the rows were changed, do not add to rows as it could add same thing multiple times
					allChanged = true;
				}
				// get the rows that are changed.
				RowData rows = getRows(event.getFirstRow(), event.getLastRow() + 1);
				if (rows != RowData.EMPTY)
				{
					rowChanges.add(rows);
				}
			}
		}
		getApplication().getChangeListener().valueChanged();
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (!e.getValueIsAdjusting())
		{
			selectionChanged = true;
		}
	}

	/**
	 * @return
	 */
	public int getPageSize()
	{
		return pageSize;
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

	/*
	 * Get rows between two foundset indexes. The indexes are 0-based. startRow is inclusive, lastRow is exclusive.
	 */
	@SuppressWarnings("nls")
	private RowData getRows(int startRow, int lastRow)
	{
		if (currentFoundset == null || getPageSize() == 0 || getController().isRendering()) return RowData.EMPTY;
		try
		{
			getController().setRendering(true);
			List<Map<String, Object>> rows = new ArrayList<>();
			PropertyDescription rowsTypes = AggregatedPropertyType.newAggregatedProperty();

			int startIdx = (currentPage - 1) * getPageSize();
			int endIdx = currentPage * getPageSize();
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
			Collection<WebFormComponent> bodyComponents = getBodyComponents(getComponents());
			int currentIndex = startTabSeqIndex + startOffset * bodyComponents.size();
			for (int i = startIdx; i < endIdx; i++)
			{
				IRecordInternal record = currentFoundset.getRecord(i);
				dataAdapterList.setRecord(record, false);
				Map<String, Object> rowProperties = new HashMap<String, Object>();
				PropertyDescription rowT = AggregatedPropertyType.newAggregatedProperty();

				rowProperties.put("_svyRowId", record.getPKHashKey() + "_" + i);
				for (WebFormComponent wc : bodyComponents)
				{
					//TODO
					//this approach does not work with  complex types like namepanel2 (see namepanel2 spec for more details)
					// namepanel2 has a nested property which is a dataproviderID
					Map<String, Object> cellProperties = new HashMap<>();
					PropertyDescription cellTypes = AggregatedPropertyType.newAggregatedProperty();
					List<String> tagstrings = getWebComponentPropertyType(wc.getFormElement().getWebComponentSpec(), TagStringPropertyType.INSTANCE);
					for (String tagstringPropID : tagstrings)
					{
						cellProperties.put(tagstringPropID, wc.getRawProperties().get(tagstringPropID));
						cellTypes.putProperty(tagstringPropID, wc.getFormElement().getWebComponentSpec().getProperty(tagstringPropID));
					}

					List<String> dataproviders = getWebComponentPropertyType(wc.getFormElement().getWebComponentSpec(), DataproviderPropertyType.INSTANCE);
					for (String dataproviderID : dataproviders)
					{
						cellProperties.put(dataproviderID, wc.getRawProperties().get(dataproviderID));
						cellTypes.putProperty(dataproviderID, wc.getFormElement().getWebComponentSpec().getProperty(dataproviderID));
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
								valuelist.getValueList().getDatabaseValuesType() == IValueListConstants.RELATED_VALUES ||
								valuelist.getFallbackValueList() != null)

							cellProperties.put(valuelistProperty, new ValuelistWrapper(valuelist, record));
							cellTypes.putProperty(valuelistProperty, wc.getFormElement().getWebComponentSpec().getProperty(valuelistProperty));
						}
					}

					cellProperties.put("svy_cn", wc.getName());
					// execute onrender.
					rowProperties.put(wc.getName(), cellProperties);
					if (cellTypes.hasChildProperties()) rowT.putProperty(wc.getName(), cellTypes);
				}
				List<TabSequencePropertyWithComponent> tabSeqComponents = getTabSeqComponents();
				for (TabSequencePropertyWithComponent propertyWithComponent : tabSeqComponents)
				{
					Map<String, Object> cellProperties = (Map<String, Object>)rowProperties.get(propertyWithComponent.getComponent().getName());
					if (cellProperties != null) cellProperties.put(propertyWithComponent.getProperty(), currentIndex++);
				}
				rows.add(rowProperties);
				if (rowT.hasChildProperties()) rowsTypes.putProperty(String.valueOf(rows.size() - 1), rowT);
			}
			dataAdapterList.setRecord(currentFoundset.getRecord(currentFoundset.getSelectedIndex()), false);
			return new RowData(new TypedData<List<Map<String, Object>>>(rows, rowsTypes), startIdx - foundsetStartRow, endIdx - foundsetStartRow);
		}
		finally
		{
			getController().setRendering(false);
		}
	}

	private Collection<WebFormComponent> getBodyComponents(Collection<WebComponent> components)
	{
		Form frm = getController().getForm();
		Part body = getBodyPart(frm);
		int bodyStartY = frm.getPartStartYPos(body.getID());
		int bodyEndY = body.getHeight();

		List<WebFormComponent> ret = new ArrayList<>();
		//filter only body elements
		for (WebComponent comp : components)
		{
			WebFormComponent wc = (WebFormComponent)comp;
			if (!wc.getFormElement().isForm())
			{
				Point location = wc.getFormElement().getDesignLocation();

				if (location != null && !(wc instanceof DefaultNavigatorWebComponent) && (bodyStartY <= location.y && bodyEndY > location.y))
				{
					ret.add(wc);
				}
			}
		}
		return ret;
	}

	private Part getBodyPart(Form frm)
	{
		Part part = null;
		for (Part prt : Utils.iterate(frm.getParts()))
		{
			if (prt.getPartType() == Part.BODY)
			{
				part = prt;
				break;
			}
		}
		return part;
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

		if (currentFoundset != null)
		{
			currentFoundset.removeFoundSetEventListener(this);
			((ISwingFoundSet)currentFoundset).getSelectionModel().removeListSelectionListener(this);
		}
		if (fs == null)
		{
			previousFS = currentFoundset;
		}
		currentFoundset = fs;
		if (currentFoundset != null)
		{
			currentFoundset.addFoundSetEventListener(this);
			((ISwingFoundSet)currentFoundset).getSelectionModel().addListSelectionListener(this);
			int page = previousFS == currentFoundset ? currentPage : -1;
			setAllChanged();
			if (page != -1) currentPage = page;
			valueChanged();
			previousFS = null;
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


	public static List<String> getWebComponentPropertyType(WebComponentSpecification componentSpec, IPropertyType type)
	{
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

	public static class RowData implements JSONWritable
	{
		public static final int CHANGE = 0;
		public static final int INSERT = 1;
		public static final int DELETE = 2;

		private static final RowData EMPTY = new RowData();
		private final TypedData<List<Map<String, Object>>> rows;
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

		public RowData(TypedData<List<Map<String, Object>>> rows, int startIndex, int endIndex)
		{
			this(rows, startIndex, endIndex, CHANGE);
		}

		public RowData(TypedData<List<Map<String, Object>>> rows, int startIndex, int endIndex, int type)
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

		public TypedData<Map<String, Object>> toMap()
		{
			PropertyDescription rowType = null;

			Map<String, Object> retValue = new HashMap<>();
			retValue.put("rows", rows.content);
			if (rows.contentType != null)
			{
				rowType = AggregatedPropertyType.newAggregatedProperty();
				rowType.putProperty("rows", rows.contentType);
			}
			retValue.put("startIndex", Integer.valueOf(startIndex));
			retValue.put("endIndex", Integer.valueOf(endIndex));
			retValue.put("type", Integer.valueOf(type));

			return new TypedData<Map<String, Object>>(retValue, rowType);
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

	public void setPageSize(int size)
	{
		pageSize = size;
	}
}