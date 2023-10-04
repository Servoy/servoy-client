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
import com.servoy.j2db.ui.ISupportPlaceholderText;
import com.servoy.j2db.ui.ISupportSpecialClientProperty;
import com.servoy.j2db.ui.runtime.HasRuntimePlaceholder;
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
	HasRuntimeReadOnly, HasRuntimePlaceholder
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
			for (ILabel label : labels)
			{
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
			if (getComponent().isViewable())
			{
				List<ILabel> labels = getComponent().getLabelsFor();
				if (labels != null)
				{
					for (ILabel label : labels)
					{
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
			super.setVisible(b);
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
		Dimension newSize = new Dimension(width, height);
		setComponentSize(newSize);
		getChangesRecorder().setSize(width, height, getComponent().getBorder(), getComponent().getMargin(), 0);
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
		return Utils.getFindModeValueForMultipleValues(strValues);
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

	private String placeholderText = null;

	public String getPlaceholderText()
	{
		return placeholderText;
	}

	public void setPlaceholderText(String placeholder)
	{
		if (!Utils.safeEquals(placeholder, getPlaceholderText()))
		{
			if (getComponent() instanceof ISupportPlaceholderText)
			{
				((ISupportPlaceholderText)getComponent()).setPlaceholderText(placeholder);
			}
			this.placeholderText = placeholder;
			getChangesRecorder().setChanged();
		}
	}
}
