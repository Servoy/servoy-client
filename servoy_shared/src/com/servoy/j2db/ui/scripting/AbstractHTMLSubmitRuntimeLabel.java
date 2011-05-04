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
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.util.ComponentFactoryHelper;

/**
 * @author lvostinar
 * @since 6.0
 *
 */
public abstract class AbstractHTMLSubmitRuntimeLabel extends AbstractRuntimeLabel
{
	public AbstractHTMLSubmitRuntimeLabel(ILabel label, IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(label, jsChangeRecorder, application);
	}

	/*
	 * size---------------------------------------------------
	 */
	@Override
	public void js_setSize(int x, int y)
	{
		super.js_setSize(x, y);
		Border b = label.getBorder();
		Insets m = null;
		// empty border gets handled as margin
		if (b instanceof EmptyBorder)
		{
			m = b.getBorderInsets(null);
			b = null;
		}
		jsChangeRecorder.setSize(x, y, b, m, label.getFontSize(), false, label.getVerticalAlignment());
	}

	public void js_setBorder(String spec)
	{
		Border border = ComponentFactoryHelper.createBorder(spec);
		Border oldBorder = label.getBorder();
		if (label instanceof Component && oldBorder instanceof CompoundBorder && ((CompoundBorder)oldBorder).getInsideBorder() != null)
		{
			Insets insets = ((CompoundBorder)oldBorder).getInsideBorder().getBorderInsets((Component)label);
			label.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right)));
		}
		else
		{
			label.setBorder(border);
		}
		jsChangeRecorder.setBorder(spec);
		Border b = border;
		Insets m = null;
		// empty border gets handled as margin
		if (b instanceof EmptyBorder)
		{
			m = b.getBorderInsets(null);
			b = null;
		}
		jsChangeRecorder.setSize(label.getSize().width, label.getSize().height, b, m, label.getFontSize(), false, label.getVerticalAlignment());
	}

	@Deprecated
	public String js_getParameterValue(String param)
	{
		return label.getParameterValue(param);
	}

	public String js_getLabelForElementName()
	{
		Object component = label.getLabelFor();
		if (component instanceof IFieldComponent)
		{
			return ((IFieldComponent)component).getName();
		}
		return null;
	}

}
