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
package com.servoy.j2db.util.model;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.ScopesUtils;

/**
 * @author jcompagner
 */
public class ComboModelListModelWrapper<E> extends AbstractListModel implements ComboBoxModel, IEditListModel, List<E>, IModificationListener
{
	protected IValueList listModel;
	protected final ListModelListener listener;
	protected Object selectedObject; // TODO can't we just remove this and count on selectedSet? otherwise we always need to keep all selection stuff in sync
	protected boolean valueListChanging;
	protected boolean hideFirstValue;
	private Object realSelectedObject; // TODO can't we just remove this and count on selectedSet? otherwise we always need to keep all selection stuff in sync
	private final boolean shouldHideEmptyValueIfPresent;
	private IRecordInternal parentState;
	private IRecordInternal relatedRecord;
	private String relatedFoundsetLookup;
	protected Set<Integer> selectedSet;

	public ComboModelListModelWrapper(IValueList listModel, boolean shouldHideEmptyValueIfPresent, boolean isSeparatorAware)
	{
		if (isSeparatorAware)
		{
			this.listModel = new SeparatorProcessingValueList(listModel);
		}
		else
		{
			this.listModel = listModel;
		}
		this.shouldHideEmptyValueIfPresent = shouldHideEmptyValueIfPresent;
		listener = new ListModelListener();
		listModel.addListDataListener(listener);
		this.hideFirstValue = (listModel.getAllowEmptySelection() && shouldHideEmptyValueIfPresent);
	}

	public void deregister()
	{
		listModel.removeListDataListener(listener);
		listModel.deregister();
	}

	public void register(IValueList vlModel)
	{
		IValueList newModel = vlModel;
		if (newModel instanceof LookupValueList)
		{
			// We never expect a LookupValueList here, these are not made for listing all values.
			// When we get a LookupValueList, this is probably a value list that was used as fallback (we create LookupValueList), replace with real value list.
			newModel = ((LookupValueList)newModel).getRealValueList();
		}

		deregister();
		Set<Integer> newSelectedSet = Collections.synchronizedSet(new HashSet<Integer>());
		if (selectedSet != null && selectedSet.size() > 0)
		{
			for (Integer selected : selectedSet)
			{
				Object obj = getRealElementAt(selected.intValue());
				int newRow = newModel.realValueIndexOf(obj);
				if (newRow >= 0)
				{
					if (newRow > 0 && hideFirstValue && newModel.getAllowEmptySelection()) newRow--;
					newSelectedSet.add(Integer.valueOf(newRow));
				}
			}
		}
		int prevSize = getSize();
		if (listModel instanceof SeparatorProcessingValueList) ((SeparatorProcessingValueList)listModel).setWrapped(newModel);
		else listModel = newModel;
		selectedSet = newSelectedSet;
		listModel.addListDataListener(listener);
		this.hideFirstValue = (listModel.getAllowEmptySelection() && shouldHideEmptyValueIfPresent);
		int currentSize = getSize();
		if (currentSize > prevSize)
		{
			listener.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, prevSize, currentSize - 1));
			listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, prevSize - 1));
		}
		else if (prevSize != currentSize)
		{
			listener.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, currentSize, prevSize - 1));
			listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, currentSize - 1));
		}
		else
		{
			listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, currentSize - 1));
		}
	}

	public Object getRealElementAt(int row)
	{
		if (hideFirstValue) return listModel.getRealElementAt(row + 1);
		return listModel.getRealElementAt(row);
	}


	/**
	 * @return
	 */
	public boolean hasRealValues()
	{
		return listModel.hasRealValues();
	}

	public int realValueIndexOf(Object obj)
	{
		int i = listModel.realValueIndexOf(obj);
		if (hideFirstValue && i != -1) i--;
		return i;
	}

	public String getRelationName()
	{
		return listModel.getRelationName();
	}

	public void fill(IRecordInternal ps)
	{
		this.parentState = ps;
		if (relatedRecord != null)
		{
			relatedRecord.removeModificationListener(this);
			relatedRecord = null;
		}
		boolean fireSelectionChange = false;
		if (selectedSet != null && selectedSet.size() > 0)
		{
			selectedSet.clear();
			fireSelectionChange = true;
		}

		Object obj = getSelectedItem();
		if (relatedFoundsetLookup == null || ps == null)
		{
			listModel.fill(ps);
		}
		else
		{
			IFoundSetInternal relatedFoundSet = ps.getRelatedFoundSet(relatedFoundsetLookup);
			if (relatedFoundSet == null || relatedFoundSet.getSize() == 0)
			{
				listModel.fill(null);
			}
			else
			{
				relatedRecord = relatedFoundSet.getRecord(relatedFoundSet.getSelectedIndex());
				if (relatedRecord != null) relatedRecord.addModificationListener(this);
				listModel.fill(relatedRecord);
			}
		}
		if (obj != null)
		{
			if (listModel.indexOf(obj) == -1 && hasRealValues())
			{
				selectedObject = null;
				realSelectedObject = null;
				fireSelectionChange = true;
			}
			else
			{
				selectedObject = null;
				realSelectedObject = null;
				setSelectedItem(obj);
			}
		}
		// fire selection change after state is changed
		if (fireSelectionChange)
		{
			fireContentsChanged(this, -1, -1);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IModificationListener#valueChanged(com.servoy.j2db.dataprocessing.ModificationEvent)
	 */
	public void valueChanged(ModificationEvent e)
	{
		valueListChanging = true;
		try
		{
			fill(parentState);
		}
		finally
		{
			valueListChanging = false;
		}
	}

	/**
	 * @param dataProviderID the dataProviderID to set
	 */
	public void setDataProviderID(String dataProviderID)
	{
		int index = (dataProviderID == null || ScopesUtils.isVariableScope(dataProviderID)) ? -1 : dataProviderID.lastIndexOf('.');
		if (index != -1)
		{
			this.relatedFoundsetLookup = dataProviderID.substring(0, index);
		}
		else
		{
			this.relatedFoundsetLookup = null;
		}
	}

	/**
	 * Set the real selected object, in case the listModel currently does not have a mapping for the real value, save the real value for a listmodel change.
	 * @param realSelectedObject
	 */
	public void setRealSelectedObject(Object realSelectedObject)
	{
		this.realSelectedObject = realSelectedObject;
	}

	public void setSelectedItem(Object anObject)
	{
		setSelectedItem(anObject, false);
	}

	private void setSelectedItem(Object anObject, boolean onlyUpdateValues)
	{
		if ((selectedObject != null && !selectedObject.equals(anObject)) || (selectedObject == null && anObject != null))
		{
			selectedObject = anObject;
			int listIndex = listModel.indexOf(selectedObject);
			if (!onlyUpdateValues)
			{
				Set<Integer> srows = getSelectedRows();
				srows.clear();
				int index = listIndex;
				if (hideFirstValue) index--;
				if (index >= 0)
				{
					srows.add(Integer.valueOf(index));
				}
			}

			realSelectedObject = selectedObject;
			if (selectedObject != null && listIndex != -1)
			{
				realSelectedObject = listModel.getRealElementAt(listIndex);
			}
			if (!onlyUpdateValues) fireContentsChanged(this, -1, -1);
		}
	}

	public Object getSelectedItem()
	{
		return selectedObject;
	}

	public Object getRealSelectedItem()
	{
		return realSelectedObject;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.ListModel#getSize()
	 */
	public int getSize()
	{
		return size();
	}

	public E getElementAt(int index)
	{
		int idx = index;
		if (hideFirstValue) idx++;

		if (idx < listModel.getSize()) return (E)listModel.getElementAt(idx);
		return null;
	}

	public boolean isCellEditable(int rowIndex)
	{
		return true;
	}


	public void setElementAt(Object aValue, int rowIndex)
	{
		setElementAt(aValue, rowIndex, listModel.getAllowEmptySelection());
	}

	/**
	 * @param false1
	 * @param i
	 * @param b
	 */
	public void setElementAt(Object aValue, int rowIndex, boolean allowEmptySelection)
	{
		// this index is the ui index (so 0 is 1 in the list model if hideFirstValue = true)
		Integer i = new Integer(rowIndex);

		getSelectedRows();//to be sure it present
		boolean b = ((Boolean)aValue).booleanValue();
		if (b)
		{
			if (selectedSet.contains(i))
			{
				// not changed return.
				return;
			}
			if (!multiValueSelect && selectedSet.size() > 0)
			{
				selectedSet.clear();
			}
			selectedSet.add(i);
		}
		else
		{
			if (selectedSet.size() == 1 && selectedSet.contains(i) && !allowEmptySelection)
			{
				// not allowed.
				return;
			}
			if (!selectedSet.contains(i))
			{
				// not changed return.
				return;
			}
			selectedSet.remove(i);
		}
		setSelectedItem(selectedSet.size() > 0 ? getElementAt(selectedSet.iterator().next().intValue()) : null, true);

		// when firing tmp remove the listener from the relatedRecord
		// so that we don't get a modification change from our own fire.
		IRecordInternal tmp = null;
		if (relatedRecord != null)
		{
			relatedRecord.removeModificationListener(this);
			tmp = relatedRecord;
			relatedRecord = null;
		}
		// data changed
		fireContentsChanged(this, rowIndex, rowIndex);
		// selection changed
		fireContentsChanged(this, -1, -1);
		if (tmp != null && relatedRecord == null)
		{
			relatedRecord = tmp;
			relatedRecord.addModificationListener(this);
		}
	}

	public boolean isRowSelected(int index)
	{
		if (selectedSet == null)
		{
			return false;
		}
		Integer i = new Integer(index);
		return selectedSet.contains(i);
	}

	protected boolean multiValueSelect;

	public void setMultiValueSelect(boolean b)
	{
		multiValueSelect = b;
	}

	public int getSelectedRow()
	{
		if (selectedSet != null && selectedSet.size() > 0)
		{
			return selectedSet.iterator().next().intValue();
		}
		return -1;
	}

	public Set<Integer> getSelectedRows()
	{
		if (selectedSet == null)
		{
			selectedSet = Collections.synchronizedSet(new HashSet<Integer>());
		}
		return selectedSet;
	}

	protected class ListModelListener implements ListDataListener
	{
		public void intervalAdded(ListDataEvent e)
		{
			valueListChanging = true;
			try
			{
				if (hideFirstValue)
				{
					if (e.getIndex0() != 0 || e.getIndex1() != 0)
					{
						//avoid getting a -1 index value when index0 or index1 = 0
						fireIntervalAdded(this, Math.max(0, e.getIndex0() - 1), Math.max(0, e.getIndex1() - 1));
					}
					//if both index0 and index1 are 0, no action needed
				}
				else
				{
					fireIntervalAdded(this, e.getIndex0(), e.getIndex1());
				}
			}
			finally
			{
				valueListChanging = false;
			}
		}

		public void intervalRemoved(ListDataEvent e)
		{
			valueListChanging = true;
			try
			{
				if (hideFirstValue)
				{
					if (e.getIndex0() != 0 || e.getIndex1() != 0)
					{
						//avoid getting a -1 index value when index0 or index1 = 0
						fireIntervalRemoved(this, Math.max(0, e.getIndex0() - 1), Math.max(0, e.getIndex1() - 1));
					}
					//if both index0 and index1 are 0, no action needed
				}
				else
				{
					fireIntervalRemoved(this, e.getIndex0(), e.getIndex1());
				}
			}
			finally
			{
				valueListChanging = false;
			}
		}

		public void contentsChanged(ListDataEvent e)
		{
			valueListChanging = true;
			try
			{
				if (hasRealValues() && realSelectedObject != null)
				{
					int index = listModel.realValueIndexOf(realSelectedObject);
					if (index == -1)
					{
						setSelectedItem(null);
					}
					else
					{
						setSelectedItem(getElementAt(index));
					}
				}
				if (hideFirstValue)
				{
					if (e.getIndex0() != 0 || e.getIndex1() != 0)
					{
						//avoid getting a -1 index value when index0 or index1 = 0
						fireContentsChanged(this, Math.max(0, e.getIndex0() - 1), Math.max(0, e.getIndex1() - 1));
					}
					//if both index0 and index1 are 0, no action needed
				}
				else
				{
					fireContentsChanged(this, e.getIndex0(), e.getIndex1());
				}
			}
			finally
			{
				valueListChanging = false;
			}
		}
	}

	/**
	 * @return Returns the valueListChanging.
	 */
	public boolean isValueListChanging()
	{
		return valueListChanging;
	}

	public String getName()
	{
		return listModel.getName();
	}

	public int getValueType()
	{
		IValueList list = (listModel instanceof SeparatorProcessingValueList) ? ((SeparatorProcessingValueList)listModel).getWrapped() : listModel;
		if (list instanceof CustomValueList)
		{
			return ((CustomValueList)list).getValueType();
		}
		return 0;
	}

	/*
	 * _____________________________________________________________ Methods for java.util.List
	 */

	public void add(int index, Object element)
	{
		throw new UnsupportedOperationException("add not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public boolean add(Object o)
	{
		throw new UnsupportedOperationException("add not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public boolean addAll(Collection< ? extends E> c)
	{
		throw new UnsupportedOperationException("add not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public boolean addAll(int index, Collection< ? extends E> c)
	{
		throw new UnsupportedOperationException("add not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public void clear()
	{
		throw new UnsupportedOperationException("clear not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public boolean contains(Object o)
	{
		return listModel.indexOf(o) != -1;
	}

	public boolean containsAll(Collection< ? > c)
	{
		throw new UnsupportedOperationException("containsAll not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public E get(int index)
	{
		return getElementAt(index);
	}


	public int indexOf(Object o)
	{
		int index = listModel.indexOf(o);
		if (hideFirstValue) index--;
		return index;
	}

	public boolean isEmpty()
	{
		int size = listModel.getSize();
		if (hideFirstValue && size > 0) size--;
		return (size == 0);
	}

	public Iterator<E> iterator()
	{
		throw new UnsupportedOperationException("iterator not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public int lastIndexOf(Object o)
	{
		throw new UnsupportedOperationException("lastIndexOf not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public ListIterator<E> listIterator()
	{
		throw new UnsupportedOperationException("iterator not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public ListIterator<E> listIterator(int index)
	{
		throw new UnsupportedOperationException("iterator not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public E remove(int index)
	{
		throw new UnsupportedOperationException("remove not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public boolean remove(Object o)
	{
		throw new UnsupportedOperationException("remove not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public boolean removeAll(Collection< ? > c)
	{
		throw new UnsupportedOperationException("removeall not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public boolean retainAll(Collection< ? > c)
	{
		throw new UnsupportedOperationException("retainAll not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public E set(int index, E element)
	{
		throw new UnsupportedOperationException("set not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public int size()
	{
		int size = listModel.getSize();
		if (hideFirstValue && size > 0) size--;
		return size;
	}

	public List<E> subList(int fromIndex, int toIndex)
	{
		throw new UnsupportedOperationException("subList not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public Object[] toArray()
	{
		throw new UnsupportedOperationException("toArray not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	public <T> T[] toArray(T[] a)
	{
		throw new UnsupportedOperationException("toArray not supported on valuelist wrapper"); //$NON-NLS-1$
	}

	/**
	 * A wrapper for valuelists that helps combos deal with situations where "-" should be a valid value in the valuelist (not just a separator) and should also reach the dataprovider this way (and not as \-).
	 * It converts the "-" that is interpreted as separator into a separator Object and the "\-" that is supposed to be interpreted as a real "-" to "-".
	 *
	 * "-" is only considered a separator if present as display value in the valuelist.
	 */
	private static class SeparatorProcessingValueList implements IValueList
	{

		private IValueList wrapped;

		public SeparatorProcessingValueList(IValueList listModel)
		{
			this.wrapped = listModel;
		}

		/**
		 * @return the wrapped
		 */
		public IValueList getWrapped()
		{
			return wrapped;
		}

		/**
		 * @param wrapped the wrapped to set
		 */
		public void setWrapped(IValueList wrapped)
		{
			this.wrapped = wrapped;
		}

		private Object convertToRuntimeSeparator(Object o)
		{
			if (SEPARATOR_DESIGN_VALUE.equals(o)) return SEPARATOR;
			if (ESCAPED_SEPARATOR_DESIGN_VALUE.equals(o)) return SEPARATOR_DESIGN_VALUE;
			return o;
		}

		private Object convertFromRuntimeSeparator(Object o)
		{
			if (SEPARATOR.equals(o)) return SEPARATOR_DESIGN_VALUE;
			if (SEPARATOR_DESIGN_VALUE.equals(o)) return ESCAPED_SEPARATOR_DESIGN_VALUE;
			return o;
		}

		public Object getRealElementAt(int row)
		{
			// if list does not have real values, then the real values will be display values... so separator affects those
			return wrapped.hasRealValues() ? wrapped.getRealElementAt(row) : convertToRuntimeSeparator(wrapped.getRealElementAt(row));
		}

		public int realValueIndexOf(Object o)
		{
			// if list does not have real values, then the real values will be display values... so separator affects those
			return wrapped.hasRealValues() ? wrapped.realValueIndexOf(o) : wrapped.realValueIndexOf(convertFromRuntimeSeparator(o));
		}

		public int indexOf(Object o)
		{
			return wrapped.indexOf(convertFromRuntimeSeparator(o));
		}

		public Object getElementAt(int index)
		{
			return convertToRuntimeSeparator(wrapped.getElementAt(index));
		}

		// the rest of the methods are forwarded to listModel
		// --------------------------------------------------

		public void setFallbackValueList(IValueList list)
		{
			wrapped.setFallbackValueList(list);
		}

		public void addListDataListener(ListDataListener l)
		{
			wrapped.addListDataListener(l);
		}

		public String getRelationName()
		{
			return wrapped.getRelationName();
		}

		public boolean hasRealValues()
		{
			return wrapped.hasRealValues();
		}

		public int getSize()
		{
			return wrapped.getSize();
		}

		public void removeListDataListener(ListDataListener l)
		{
			wrapped.removeListDataListener(l);
		}

		public void deregister()
		{
			wrapped.deregister();
		}

		public void fill(IRecordInternal rec)
		{
			wrapped.fill(rec);
		}

		public boolean getAllowEmptySelection()
		{
			return wrapped.getAllowEmptySelection();
		}

		public IValueList getFallbackValueList()
		{
			return wrapped.getFallbackValueList();
		}

		public String getName()
		{
			return wrapped.getName();
		}

		public ValueList getValueList()
		{
			return wrapped.getValueList();
		}

		@Override
		public IDataProvider[] getDependedDataProviders()
		{
			return wrapped.getDependedDataProviders();
		}

	}

}
