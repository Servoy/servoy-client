/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;

import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.scripting.AbstractRuntimeValuelistComponent;
import com.servoy.j2db.util.model.ComboModelListModelWrapper;
import com.servoy.j2db.util.model.IEditListModel;


/**
 * Spinner-like smart client component. For now it is based on DataChoice implementation but if we will want it to be
 * editable we will have to make it similar to editable combobox but based on JSpinner.
 * @author acostescu
 */
public class DataSpinner extends DataChoice
{

	private boolean willEnsureSelectedIsVisible = false;

	public DataSpinner(IApplication app, AbstractRuntimeValuelistComponent<IFieldComponent> scriptable, IValueList vl)
	{
		super(app, scriptable, vl, Field.SPINNER);

		super.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener()
		{
			public void adjustmentValueChanged(AdjustmentEvent e)
			{
				if (e.getValueIsAdjusting()) return;

				int idx = enclosedComponent.locationToIndex(getViewport().getViewPosition());
				if (idx > 0 && !isRowSelected(idx))
				{
					setElementAt(Boolean.TRUE, idx);
					if (!enclosedComponent.hasFocus()) enclosedComponent.requestFocus();
					enclosedComponent.setSelectedIndex(idx);
				}
				else if (list.getSize() > 0)
				{
					ensureSelectedIsVisible(); // do not allow blank value as a user choice
				}
			}
		});

		enclosedComponent.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting()) return;

				int idx = enclosedComponent.getSelectedIndex();
				if (idx > 0)
				{
					if (!isRowSelected(idx)) setElementAt(Boolean.TRUE, idx);
				}
				else if (list.getSize() > 0)
				{
					int previousIdx = (e.getFirstIndex() == idx) ? e.getLastIndex() : e.getFirstIndex();
					enclosedComponent.setSelectedIndex(previousIdx);
				}
			}
		});

		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentShown(ComponentEvent e)
			{
				pinCellHeight();
			}

			@Override
			public void componentResized(ComponentEvent e)
			{
				pinCellHeight();
			}
		});
	}

	@Override
	protected ListModel createJListModel(ComboModelListModelWrapper comboModel)
	{
		return new SpinnerModel(comboModel);
	}

	@Override
	protected boolean isRowSelected(int idx)
	{
		return idx > 0 ? list.isRowSelected(idx - 1) : false;
	}

	@Override
	protected void setElementAt(Object b, int idx)
	{
		if (idx > 0) list.setElementAt(b, idx - 1);
		else list.setSelectedItem(null);
	}

	private void pinCellHeight()
	{
		enclosedComponent.setFixedCellHeight(getViewport().getHeight() - getViewport().getInsets().top - getViewport().getInsets().bottom);
		enclosedComponent.validate();
	}

	@Override
	public void setVerticalScrollBarPolicy(int policy)
	{
		// spinner always shows vertical scroll bar
	}

	@Override
	protected void setMultiValueSelect()
	{
		list.setMultiValueSelect(false);
	}

	@Override
	public void contentsChanged(ListDataEvent e)
	{
		super.contentsChanged(e);

		ensureSelectedIsVisible();
	}

	private void ensureSelectedIsVisible()
	{
		// avoid doing this needlessly, currently this method gets called at least once for each list item when selection changes from outside the component
		if (willEnsureSelectedIsVisible == false)
		{
			willEnsureSelectedIsVisible = true;
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					willEnsureSelectedIsVisible = false;
					enclosedComponent.ensureIndexIsVisible(list.getSelectedRow() + 1);
				}
			});
		}
	}

	@Override
	protected boolean shouldPaintSelection()
	{
		return false;
	}

	/**
	 * Model that allows a (first) null entry for situations where the dataProvider value is not part of the valuelist (so the component should show blank content).
	 * 
	 * @author acostescu
	 */
	protected class SpinnerModel implements IEditListModel
	{

		private final ComboModelListModelWrapper<Object> comboModel;
		private final HashMap<ListDataListener, ListDataListener> lTol = new HashMap<ListDataListener, ListDataListener>();

		public SpinnerModel(ComboModelListModelWrapper<Object> comboModel)
		{
			this.comboModel = comboModel;
		}

		public int getSize()
		{
			return comboModel.getSize() + 1;
		}

		public Object getElementAt(int index)
		{
			if (index < 1) return null;
			return comboModel.getElementAt(index - 1);
		}

		public void addListDataListener(final ListDataListener l)
		{
			if (!lTol.containsKey(l))
			{
				ListDataListener toL = new ListDataListener()
				{

					public void intervalRemoved(ListDataEvent e)
					{
						ListDataEvent myE = new ListDataEvent(this, e.getType(), e.getIndex0() >= 0 ? e.getIndex0() + 1 : e.getIndex0(), e.getIndex1() >= 0
							? e.getIndex1() + 1 : e.getIndex1());
						l.intervalRemoved(myE);
					}

					public void intervalAdded(ListDataEvent e)
					{
						ListDataEvent myE = new ListDataEvent(this, e.getType(), e.getIndex0() >= 0 ? e.getIndex0() + 1 : e.getIndex0(), e.getIndex1() >= 0
							? e.getIndex1() + 1 : e.getIndex1());
						l.intervalAdded(myE);
					}

					public void contentsChanged(ListDataEvent e)
					{
						ListDataEvent myE = new ListDataEvent(this, e.getType(), e.getIndex0() >= 0 ? e.getIndex0() + 1 : e.getIndex0(), e.getIndex1() >= 0
							? e.getIndex1() + 1 : e.getIndex1());
						l.contentsChanged(myE);
					}
				};
				lTol.put(l, toL);
				comboModel.addListDataListener(toL);
			}
		}

		public void removeListDataListener(ListDataListener l)
		{
			if (lTol.containsKey(l))
			{
				comboModel.removeListDataListener(lTol.remove(l));
			}
		}

		public boolean isCellEditable(int rowIndex)
		{
			return comboModel.isCellEditable(rowIndex - 1);
		}

		public void setElementAt(Object aValue, int rowIndex)
		{
			DataSpinner.this.setElementAt(aValue, rowIndex);
		}

	}

}
