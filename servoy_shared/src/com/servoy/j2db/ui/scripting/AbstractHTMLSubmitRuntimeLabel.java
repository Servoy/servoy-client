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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.runtime.HasRuntimeLabelFor;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Utils;

/**
 * Abstract scriptable component which has label + html submit behavior.
 *
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractHTMLSubmitRuntimeLabel<C extends ILabel> extends AbstractRuntimeLabel<C> implements HasRuntimeLabelFor
{
	public AbstractHTMLSubmitRuntimeLabel(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	/*
	 * size---------------------------------------------------
	 */
	public void setSize(int width, int height)
	{
		Dimension newSize = new Dimension(width, height);
		setComponentSize(newSize);
		Border b = getComponent().getBorder();
		Insets m = null;
		// empty border gets handled as margin
		if (b instanceof EmptyBorder)
		{
			m = ComponentFactoryHelper.getBorderInsetsForNoComponent(b);
			b = null;
		}
		getChangesRecorder().setSize(width, height, b, m, getComponent().getFontSize(), false, getComponent().getVerticalAlignment());
	}

	@Override
	public void setBorder(String spec)
	{
		if (!Utils.safeEquals(getBorder(), spec))
		{
			Border border = ComponentFactoryHelper.createBorder(spec);
			Border oldBorder = getComponent().getBorder();
			if (getComponent() instanceof Component && oldBorder instanceof CompoundBorder && ((CompoundBorder)oldBorder).getInsideBorder() != null)
			{
				Insets insets = ((CompoundBorder)oldBorder).getInsideBorder().getBorderInsets((Component)getComponent());
				getComponent().setBorder(
					BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right)));
			}
			else
			{
				getComponent().setBorder(border);
			}
			getChangesRecorder().setBorder(spec);
			Border b = border;
			Insets m = null;
			// empty border gets handled as margin
			if (b instanceof EmptyBorder && !(b instanceof MatteBorder))
			{
				m = ComponentFactoryHelper.getBorderInsetsForNoComponent(b);
				b = null;
			}
			getChangesRecorder().setSize(getComponent().getSize().width, getComponent().getSize().height, b, m, getComponent().getFontSize(), false,
				getComponent().getVerticalAlignment());
		}
	}

	@Deprecated
	public String js_getParameterValue(String param)
	{
		return getComponent().getParameterValue(param);
	}

	public String getLabelForElementName()
	{
		Object component = getComponent().getLabelFor();
		if (component instanceof IFieldComponent)
		{
			return ((IFieldComponent)component).getName();
		}
		return null;
	}

}
