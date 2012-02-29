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
import java.util.Properties;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Response;
import org.apache.wicket.util.string.Strings;
import org.wicketstuff.minis.spinner.Spinner;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.SortColumn;
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
		return new SpinField(application, fieldScriptable);
	}

	private Spinner createNewSpinner()
	{
		currentValues = getNewValues(valueList);
		return spinnerBehavior = new ModifiedSpinner(currentValues);
	}

	private String[] getNewValues(IValueList vl)
	{
		String[] values = null;
		if (vl != null)
		{
			int size = vl.getSize();
			if (size > 1 || (size == 1 && vl.getElementAt(0) != null && String.valueOf(vl.getElementAt(0)).trim().length() > 0))
			{
				values = new String[size];
				Object v;
				for (int i = 0; i < size; i++)
				{
					v = vl.getElementAt(i);
					values[size - i - 1] = (v == null) ? "" : String.valueOf(v); //$NON-NLS-1$
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
			listContentChanged = !Arrays.equals(currentValues, getNewValues(valueList));
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

	protected class SpinField extends AugmentedTextField
	{

		public SpinField(IApplication application, RuntimeDataField scriptable)
		{
			super(application, scriptable);
			super.setEditable(false);
		}

		@Override
		public void setEditable(boolean b)
		{
			// do not allow this to change - currently spinner field is never editable
		}

	}

	protected class ModifiedSpinner extends Spinner
	{

		String[] values;

		public ModifiedSpinner(String[] values)
		{
			this.values = values;
		}

		@Override
		protected void configure(Properties p)
		{
			/**
			 * This method can be overridden to customize the Spinner.<br/>
			 * The following options are used by the Spinner:
			 * <ul>
			 * <li>interval The amount to increment (default=1)
			 * <li>round The number of decimal points to which to round (default=0)
			 * <li>min The lowest allowed value, false for no min (default=false)
			 * <li>max The highest allowed value, false for no max (default=false)
			 * <li>prefix String to prepend when updating (default='')
			 * <li>suffix String to append when updating (default='')
			 * <li>data An array giving a list of items through which to iterate
			 * <li>onIncrement Function to call after incrementing
			 * <li>onDecrement Function to call after decrementing
			 * <li>afterUpdate Function to call after update of the value
			 * <li>onStop Function to call on click or mouseup (default=false)
			 * </ul>
			 * 
			 * @param p
			 */
			super.configure(p);
			p.put("data", values); //$NON-NLS-1$
			p.put("afterUpdate", new Object() { //$NON-NLS-1$
					@Override
					public String toString()
					{
						return "function(spinnerBehavior) { spinnerBehavior.inputElement.onchange(); }"; //$NON-NLS-1$
					}
				});
		}

		@Override
		public void beforeRender(Component component)
		{
			// do not add default spans for + -; img buttons substitute those
		}

		/**
		 * @see org.apache.wicket.behavior.AbstractBehavior#onRendered(org.apache.wicket.Component)
		 */
		@Override
		public void onRendered(Component component)
		{
			// do not add default spans for + -; img buttons substitute those
			if (getSpinUpComponent() == null)
			{
				Response response = component.getResponse();
				response.write("</td><td style = \"margin: 0px; padding: 0px; width: 5px;\">&nbsp</td><td style = \"margin: 0px; padding: 0px; width: 16px;\">"); //$NON-NLS-1$
				response.write("<table style=\"margin: 0px; padding: 0px; border-collapse: collapse; table-layout: fixed;\"><tbody><tr style=\"margin: 0px; padding: 0px;\">"); //$NON-NLS-1$
				response.write("<td style=\"margin: 0px; padding: 0px;\">"); //$NON-NLS-1$
				response.write("\n<img style=\"cursor: pointer; border: none;\" id=\""); //$NON-NLS-1$
				response.write(field.getMarkupId() + "-SpinnerUp"); //$NON-NLS-1$
				response.write("\" src=\""); //$NON-NLS-1$
				response.write(Strings.escapeMarkup(getUpIconUrl().toString()));
				response.write("\"/></td></tr><tr style=\"margin: 0px; padding: 0px;\"><td style=\"margin: 0px; padding: 0px;\">"); //$NON-NLS-1$
				response.write("\n<img style=\"cursor: pointer; border: none;\" id=\""); //$NON-NLS-1$
				response.write(field.getMarkupId() + "-SpinnerDown"); //$NON-NLS-1$
				response.write("\" src=\""); //$NON-NLS-1$
				response.write(Strings.escapeMarkup(getDownIconUrl().toString()));
				response.write("\"/></td></tr></tbody></table>"); //$NON-NLS-1$
			}
		}

		protected CharSequence getUpIconUrl()
		{
			return RequestCycle.get().urlFor(new ResourceReference(WebDataSpinner.class, "images/spinnerUp.gif")); //$NON-NLS-1$
		}

		protected CharSequence getDownIconUrl()
		{
			return RequestCycle.get().urlFor(new ResourceReference(WebDataSpinner.class, "images/spinnerDown.gif")); //$NON-NLS-1$
		}

		@Override
		public boolean isEnabled(Component component)
		{
			return shouldShowExtraComponents();
		}

	}

}