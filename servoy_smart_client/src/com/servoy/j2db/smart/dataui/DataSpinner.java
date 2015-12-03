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
import java.util.HashMap;

import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.scripting.AbstractRuntimeValuelistComponent;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.model.ComboModelListModelWrapper;
import com.servoy.j2db.util.model.IEditListModel;


/**
 * Spinner-like smart client component. For now it is based on DataChoice implementation but if we will want it to be
 * editable we will have to make it similar to editable combobox but based on JSpinner.
 * @author acostescu
 */
public class DataSpinner extends DataChoice
{

	// while we internally modify list input element or scroll to an index, ignore adjustment listeners that are only interested in direct user actions;
	// while list cell height changes, we must ignore generated (scroll) events - as those will probably lead to incorrect indexes as they are not a user wish to change the value
	private boolean disableUserAdjustmentListeners = false;

	public DataSpinner(IApplication app, AbstractRuntimeValuelistComponent<IFieldComponent> scriptable, IValueList vl)
	{
		super(app, scriptable, vl, Field.SPINNER, false);

		super.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener()
		{
			public void adjustmentValueChanged(AdjustmentEvent e)
			{
				if (disableUserAdjustmentListeners || e.getValueIsAdjusting()) return;
//				int y = (int)(enclosedComponent.getHeight() * (((double)e.getValue()) / e.getAdjustable().getMaximum()));
//				final int idx = enclosedComponent.locationToIndex(new Point(getViewport().getViewPosition().x, y));
				final int idx = getVisibleIndex();
				if (idx > 0 && !isRowSelected(idx) && idx < list.getSize() + 1) //list.getSize() returns SpinnerModel -2 size. We have +2 size on the SpinnerModel for First and last blank model elements.
				{
					setSelectedIndex(idx);
				}
				else if (list.getSize() > 0 && idx == 0 && list.getSelectedRow() != -1) //user reached the first element and pressed again 'up' . Circular behavior should go to last
				{
					setSelectedIndex(list.getSize());
					ensureSelectedIsVisible(false); // do not allow blank value as a user choice
				}
				else if (idx > list.getSize())
				// user has reached the last element .Circular behavior should go to first element
				{
					setSelectedIndex(1);
					ensureSelectedIsVisible(false); // do not allow blank value as a user choice
				}
			}

			/**
			 * This method belongs to the anonymous inner class AdjustmentListener(){...} .
			 * Currently only it uses this method when needing to change the selected index.
			 * @param idx
			 */
			private void setSelectedIndex(final int idx)
			{
				boolean focused = enclosedComponent.hasFocus();
				if (!focused) enclosedComponent.requestFocus();

				// if you would create a new record with non-nullable dataProvider for this spinner, type something in another text field then click
				// on an arrow of the spinner, this would determine something like spinner:commit(someValue)->currentComponentNotCommitted->currentComponentCommit->TextFieldCommit->notifyDisplayAdaptersRecordChanged->spinner:changeValueTo(null)
				// after that the spinner:commit continued with wrong (null) value... so we will do this later to allow the currentComponent to commit it's value
				Runnable r = new Runnable()
				{
					public void run()
					{
						enclosedComponent.editCellAt(idx);
						setElementAt(Boolean.TRUE, idx);
						boolean old = disableUserAdjustmentListeners;
						try
						{
							enclosedComponent.setSelectedIndex(idx);
						}
						finally
						{
							disableUserAdjustmentListeners = old;
						}
					}
				};
				if (!focused)
				{
					application.invokeLater(r);
				}
				else
				{
					r.run();
				}
			}
		});

		enclosedComponent.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if (disableUserAdjustmentListeners || e.getValueIsAdjusting()) return;

				int idx = enclosedComponent.getSelectedIndex();
				if (idx > 0)
				{
					if (!isRowSelected(idx)) setElementAt(Boolean.TRUE, idx);
				}
				else if (list.getSize() > 0)
				{
					int previousIdx = (e.getFirstIndex() == idx) ? e.getLastIndex() : e.getFirstIndex();
					boolean old = disableUserAdjustmentListeners;
					try
					{
						enclosedComponent.setSelectedIndex(previousIdx);
					}
					finally
					{
						disableUserAdjustmentListeners = old;
					}
				}
			}
		});

		enclosedComponent.getModel().addListDataListener(new ListDataListener()
		{
			public void intervalRemoved(ListDataEvent e)
			{
				ensureSelectedIsVisible(false);
			}

			public void intervalAdded(ListDataEvent e)
			{
				ensureSelectedIsVisible(false);
			}

			public void contentsChanged(ListDataEvent e)
			{
				// wait for selection changed (-1) event
				if (e.getIndex0() == e.getIndex1() && e.getIndex0() >= 0) return;
				ensureSelectedIsVisible(false);
			}
		});

		// listen for view port resize; adding a component listener for this does not help
		// because that one decouples to invokeLater(...) but when in table-view the spinner renderer/editor needs to be paint ready right away
		getViewport().addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				pinCellHeight();
			}
		});
		pinCellHeight();
	}

	private Object valueObject;

	@Override
	public void setValueObject(Object data)
	{
		if (Utils.equalObjects(getValueObject(), data) && list.getSelectedItem() != null)
		{
			// do nothing, data may be invalid value; same condition as for combo
			return;
		}
		valueObject = data;
		super.setValueObject(data);
	}

	protected int getVisibleIndex()
	{
		return enclosedComponent.locationToIndex(getViewport().getViewPosition());
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
	}

	protected void pinCellHeight()
	{
		int height = getViewport().getHeight() - getViewport().getInsets().top - getViewport().getInsets().bottom;
		if (height != 0 && enclosedComponent.getFixedCellHeight() != height)
		{
			boolean old = disableUserAdjustmentListeners;
			disableUserAdjustmentListeners = true;
			try
			{
				enclosedComponent.setFixedCellHeight(height);
				validate();
			}
			finally
			{
				disableUserAdjustmentListeners = old;
			}
			ensureSelectedIsVisible(true); // force because even if there is only a few pixels change (table view when you use keys to navigate to a cell) that might not change the visible index, we still have to reposition to where the selected index cell y begins
		}
	}

	@Override
	public void setVerticalScrollBarPolicy(int policy)
	{
		// spinner always shows vertical scroll bar
	}

	@Override
	public void setReadOnly(boolean b)
	{
		super.setReadOnly(b);
		applyScrollBarPolicy();
	}

	@Override
	public void setEditable(boolean b)
	{
		super.setEditable(b);
		applyScrollBarPolicy();
	}

	protected void ensureSelectedIsVisible(boolean force)
	{
		// avoid doing this needlessly, currently this method gets called at least once for each list item when selection changes from outside the component
		int idx = list.getSelectedRow() + 1;
		if (force || (getVisibleIndex() != idx))
		{
			boolean old = disableUserAdjustmentListeners;
			disableUserAdjustmentListeners = true;
			try
			{
				enclosedComponent.ensureIndexIsVisible(idx);
				enclosedComponent.setSelectedIndex(idx);
			}
			finally
			{
				disableUserAdjustmentListeners = old;
			}
		}
	}

	@Override
	public void setComponentEnabled(boolean b)
	{
		super.setComponentEnabled(b);
		applyScrollBarPolicy();
	}

	private void applyScrollBarPolicy()
	{
		if (isEnabled() && !isReadOnly())
		{
			setVerticalScrollBarPolicySpecial(VERTICAL_SCROLLBAR_ALWAYS);
		}
		else
		{
			setVerticalScrollBarPolicySpecial(VERTICAL_SCROLLBAR_NEVER);
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
			/*
			 * we have 2 empty model elements one in the front of the list and one at the end of the list The first empty element is needed for the reason in
			 * the SpinnerModel Description, plus detect when the beginning of the list reached (for circular spinner model) The last empty element is needed
			 * only for detection of the end of the list (for circular cycling of the spinner).
			 */
			return comboModel.getSize() + 2;
		}

		public Object getElementAt(int index)
		{
			if (index < 1) return valueObject;
			return comboModel.getElementAt(index - 1); //returns null for the last empty element from getSize()+2
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
