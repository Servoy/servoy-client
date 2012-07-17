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

package com.servoy.j2db.ui.scripting;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportSpecialClientProperty;
import com.servoy.j2db.ui.runtime.HasRuntimeReadOnly;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.runtime.IRuntimeField;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.Utils;

/**
 * Abstract scriptable field.
 * 
 * @author lvostinar
 */
public abstract class AbstractRuntimeField<C extends IFieldComponent> extends AbstractRuntimeRendersupportComponent<C> implements IRuntimeField,
	HasRuntimeReadOnly
{
	public AbstractRuntimeField(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public String getDataProviderID()
	{
		return getComponent() instanceof IDisplayData ? ((IDisplayData)getComponent()).getDataProviderID() : null;
	}

	public String[] getLabelForElementNames()
	{
		List<ILabel> labels = getComponent().getLabelsFor();
		if (labels != null)
		{
			ArrayList<String> al = new ArrayList<String>(labels.size());
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				if (label.getName() != null && !"".equals(label.getName()) && !label.getName().startsWith(ComponentFactory.WEB_ID_PREFIX)) //$NON-NLS-1$
				{
					al.add(label.getName());
				}
			}
			return al.toArray(new String[al.size()]);
		}
		return new String[0];
	}

	public String getTitleText()
	{
		return getComponent().getTitleText();
	}

	public void setTitleText(String titleText)
	{
		if (!Utils.safeEquals(titleText, getTitleText()))
		{
			getComponent().setTitleText(titleText);
			getChangesRecorder().setChanged();
		}
	}

	public int getAbsoluteFormLocationY()
	{
		return getComponent().getAbsoluteFormLocationY();
	}

	public void requestFocus()
	{
		requestFocus(true);
	}

	public void requestFocus(boolean mustExecuteOnFocusGainedMethod)
	{
		if (!mustExecuteOnFocusGainedMethod)
		{
			getComponent().getEventExecutor().skipNextFocusGain();
		}

		getComponent().requestFocusToComponent();
	}

	public void setReadOnly(boolean b)
	{
		if (isReadOnly() != b)
		{
			getComponent().setReadOnly(b);
			getChangesRecorder().setChanged();
		}
	}

	public boolean isReadOnly()
	{
		return getComponent() instanceof IDisplay && ((IDisplay)getComponent()).isReadOnly();
	}

	@Override
	public void setVisible(boolean b)
	{
		if (isVisible() != b)
		{
			super.setVisible(b);
			if (getComponent().isViewable())
			{
				List<ILabel> labels = getComponent().getLabelsFor();
				if (labels != null)
				{
					for (int i = 0; i < labels.size(); i++)
					{
						ILabel label = labels.get(i);
						IScriptable scriptable = label.getScriptObject();
						if (scriptable instanceof IRuntimeComponent)
						{
							((IRuntimeComponent)scriptable).setVisible(b);
						}
						else
						{
							label.setComponentVisible(b);
						}
					}
				}
			}
		}
	}

	@Override
	public void putClientProperty(Object key, Object value)
	{
		super.putClientProperty(key, value);
		if (getComponent() instanceof IDelegate && ((IDelegate< ? >)getComponent()).getDelegate() instanceof JComponent)
		{
			((JComponent)((IDelegate< ? >)getComponent()).getDelegate()).putClientProperty(key, value);
		}
		if (getComponent() instanceof ISupportSpecialClientProperty)
		{
			((ISupportSpecialClientProperty)getComponent()).setClientProperty(key, value);
		}
	}

	public void setSize(int width, int height)
	{
		Dimension old = new Dimension(getWidth(), getHeight());
		Dimension newSize = new Dimension(width, height);
		if (!old.equals(newSize))
		{
			setComponentSize(newSize);

			getChangesRecorder().setSize(width, height, getComponent().getBorder(), getComponent().getMargin(), 0);
		}
	}

	/** Get the value for a choice on possibly multiple values.
	 * 
	 * Multiple values are joined with newline.
	 * In case of find mode a search-object is created for records with at least these values.
	 * @param strValues 
	 * @return
	 */
	public Object getChoiceValue(Object[] values, boolean keepPlainValue)
	{
		if (values == null || values.length == 0)
		{
			return null;
		}

		if ((keepPlainValue || getComponent().getEventExecutor().getValidationEnabled()) && values.length == 1)
		{
			return values[0];
		}

		List<String> strValues = new ArrayList<String>(values.length);
		for (Object value : values)
		{
			strValues.add(CustomValueList.convertToString(value, application));
		}
		Collections.sort(strValues, StringComparator.INSTANCE);//sorted to have same order to search in with LIKE (in the db)

		if (getComponent().getEventExecutor().getValidationEnabled())
		{
			return Utils.stringJoin(strValues.iterator(), '\n');
		}

		// find mode
		if (strValues.size() > 6) // 128 combinations
		{
			// sql gets way too large, fall back to less precise sql
			return '%' + Utils.stringJoin(strValues.iterator(), "%||%") + '%'; //$NON-NLS-1$
		}

		// create a condition that matches the values, for example values = [a,b], results in sql: 
		// where val = 'a\nb' -- just the 2 values
		// or val like 'a\nb\n%' -- the first 2 values
		// or val like '%\na\nb\n%' -- next to each other in the middle
		// or val like '%\na\nb' -- the last 2 values
		// or val like 'a\n%\nb' -- first a, last b
		// or val like 'a\n%\nb\n%' -- start with a
		// or val like '%\na\n%\nb\n%'-- a and b somewhere in the middle
		// or val like '%\na\n%\nb' -- end with b
		List<String> combinedValues = joinList(strValues, new String[] { "\n", "\n%\n" }); //$NON-NLS-1$ //$NON-NLS-2$
		Iterator<String> iter = combinedValues.iterator();
		StringBuilder stringRetval = new StringBuilder();
		while (iter.hasNext())
		{
			String element = iter.next();
			stringRetval.append(element);
			stringRetval.append("||").append(element).append("\n%"); // element is first //$NON-NLS-1$ //$NON-NLS-2$
			stringRetval.append("||%\n").append(element).append("\n%"); // element in the middle //$NON-NLS-1$ //$NON-NLS-2$
			stringRetval.append("||%\n").append(element); // element at the end //$NON-NLS-1$
			if (iter.hasNext())
			{
				stringRetval.append("||"); //$NON-NLS-1$
			}
		}
		return stringRetval.toString();
	}

	/**
	 * Get the real values belonging to the choice values string.
	 * @param realVal
	 * @return
	 */
	public List<Object> resolveChoiceValues(Object realVal)
	{
		List<Object> retval = new ArrayList<Object>();
		if (realVal instanceof String)
		{
			String str = (String)realVal;
			// in case of find mode, take the first bit before the '||', it contains the values separated by newline.
			int idx = getComponent().getEventExecutor().getValidationEnabled() ? -1 : str.indexOf("||"); //$NON-NLS-1$
			StringTokenizer tk = new StringTokenizer(idx < 0 ? str : str.subSequence(0, idx).toString(), "\n"); //$NON-NLS-1$
			while (tk.hasMoreTokens())
			{
				retval.add(tk.nextToken().trim()); // do a trim because "\r\n" might leave the "\r" in the token
			}
		}
		else
		{
			retval.add(realVal);//keep plain
		}
		return retval;
	}

	/**
	 * Join items of the list with separators.
	 * @param list
	 * @param separators
	 * @return
	 */
	private static List<String> joinList(List<String> list, String[] separators)
	{
		if (list.size() <= 1)
		{
			return list;
		}

		String last = list.get(list.size() - 1);
		List<String> joinedSubList = joinList(list.subList(0, list.size() - 1), separators);
		List<String> retval = new ArrayList<String>(joinedSubList.size() * separators.length);
		for (String s : joinedSubList)
		{
			for (String sep : separators)
			{
				retval.add(s + sep + last);
			}
		}
		return retval;
	}
}
