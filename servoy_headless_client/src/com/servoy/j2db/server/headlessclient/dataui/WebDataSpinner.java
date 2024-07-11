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

package com.servoy.j2db.server.headlessclient.dataui;

import java.util.Arrays;
import java.util.List;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.apache.wicket.Component;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.scripting.RuntimeDataField;
import com.servoy.j2db.ui.scripting.RuntimeSpinner;


/**
 * Spinner-like component for web. Similar in implementation to calendar (it adds markup for spin buttons)
 * and to comboBox - because it can change values based on a valuelist.
 * @author acostescu
 */
public class WebDataSpinner extends WebDataCompositeTextField implements ISupportValueList, IDisplayRelatedData
{

	private static final long serialVersionUID = 1L;
	private IValueList valueList;
	private ModifiedSpinner spinnerBehavior;
	private ListDataListener valueListChangeListener;
	private String relationName = null;
	private String[] currentValues;
	private boolean ignoreChanges = false;

	public WebDataSpinner(IApplication application, RuntimeSpinner scriptable, String id, IValueList valueList)
	{
		super(application, scriptable, id);
		setValueList(valueList);
	}

	@Override
	protected WebDataField createTextField(RuntimeDataField fieldScriptable)
	{
		return new SpinField(application, fieldScriptable, this);
	}

	private Component createNewSpinner()
	{
		currentValues = getNewValues();
		return spinnerBehavior = new ModifiedSpinner(currentValues);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebDataCompositeTextField#setValidationEnabled(boolean)
	 */
	@Override
	public void setValidationEnabled(boolean b)
	{
		super.setValidationEnabled(b);
		if (valueList.getFallbackValueList() != null)
		{
			updateSpinnerBehavior();
		}
	}

	private String[] getNewValues()
	{
		String[] values = null;
		if (valueList != null)
		{
			IValueList vl = valueList;
			if (!field.getEventExecutor().getValidationEnabled() && vl.getFallbackValueList() != null)
			{
				vl = vl.getFallbackValueList();
			}
			int size = vl.getSize();
			if (size > 1 || (size == 1 && vl.getElementAt(0) != null && String.valueOf(vl.getElementAt(0)).trim().length() > 0))
			{
				values = new String[size];
				Object v;
				for (int i = 0; i < size; i++)
				{
					v = vl.getRealElementAt(i);
					String val = (v != null) ? v.toString() : "";
					if (val == null) val = "";
					values[size - i - 1] = val;
				}
			}
		}
		if (values == null)
		{
			values = new String[] { "", "" }; //$NON-NLS-1$//$NON-NLS-2$
		}
		return values;
	}

	public IValueList getValueList()
	{
		return valueList;
	}

	public void setValueList(IValueList vl)
	{
		if (valueList != null) vl.removeListDataListener(valueListChangeListener);
		valueList = vl;

		if (valueList != null && valueListChangeListener == null)
		{
			valueListChangeListener = new ListDataListener()
			{
				public void intervalRemoved(ListDataEvent e)
				{
					updateSpinnerBehavior();
				}

				public void intervalAdded(ListDataEvent e)
				{
					updateSpinnerBehavior();
				}

				public void contentsChanged(ListDataEvent e)
				{
					updateSpinnerBehavior();
				}
			};
		}
		field.setValueList(vl);
		if (valueList != null) valueList.addListDataListener(valueListChangeListener);

		updateSpinnerBehavior();
	}

	private void updateSpinnerBehavior()
	{
		if (ignoreChanges) return;

		if (spinnerBehavior != null) field.remove(spinnerBehavior);
		field.add(createNewSpinner());
		getStylePropertyChanges().setChanged();
	}

	public ListDataListener getListener()
	{
		return valueListChangeListener;
	}

	public void setRecord(IRecordInternal state, boolean stopEditing)
	{
		boolean listContentChanged = false;
		try
		{
			ignoreChanges = true;
			valueList.fill(state);
			listContentChanged = !Arrays.equals(currentValues, getNewValues());
		}
		finally
		{
			ignoreChanges = false;
		}
		if (listContentChanged)
		{
			updateSpinnerBehavior();
		}
	}

	public String getSelectedRelationName()
	{
		if (relationName == null && valueList != null)
		{
			relationName = valueList.getRelationName();
		}
		return relationName;
	}

	public String[] getAllRelationNames()
	{
		String selectedRelationName = getSelectedRelationName();
		if (selectedRelationName == null)
		{
			return new String[0];
		}
		else
		{
			return new String[] { selectedRelationName };
		}
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		//ignore
	}

	public List<SortColumn> getDefaultSort()
	{
		return null;
	}

	public void destroy()
	{
		valueList.deregister();
	}

	private boolean editState;

	@Override
	public void setReadOnly(boolean b)
	{
		if (b && !editable) return;
		if (b)
		{
			setEditable(false);
			editState = true;
		}
		else
		{
			setEditable(editState);
		}
		applyReadonlyState();
	}

	@Override
	public void setEditable(boolean b)
	{
		super.setEditable(b);
		editState = b;
		applyReadonlyState();
	}

	@Override
	public boolean isReadOnly()
	{
		return !isEditable();
	}

	private void applyReadonlyState()
	{
		showExtraComponents = !isReadOnly();
	}

	protected class SpinField extends AugmentedTextField
	{

		public SpinField(IApplication application, RuntimeDataField scriptable, IComponent enclosingComponent)
		{
			super(application, scriptable, enclosingComponent);
			super.setEditable(false);
		}

		@Override
		public void setEditable(boolean b)
		{
			// do not allow this to change - currently spinner field is never editable
		}
	}

	protected class ModifiedSpinner extends Component
	{

		String[] values;

		public ModifiedSpinner(String[] values)
		{
			super(null);
			this.values = values;
		}
	}

}