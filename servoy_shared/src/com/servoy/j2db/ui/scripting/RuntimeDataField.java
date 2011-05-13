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
import javax.swing.text.JTextComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptFieldMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportEditProvider;
import com.servoy.j2db.ui.ISupportInputSelection;
import com.servoy.j2db.util.ComponentFactoryHelper;

/**
 * Scriptable plain text component.
 * 
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeDataField extends AbstractRuntimeFormattedValuelistComponent implements IScriptFieldMethods
{
	public RuntimeDataField(IFieldComponent component, IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(component, jsChangeRecorder, application);
	}

	public int js_getCaretPosition()
	{
		if (component instanceof JTextComponent)
		{
			return ((JTextComponent)component).getCaretPosition();
		}
		return 0;
	}

	public void js_setCaretPosition(int pos)
	{
		if (component instanceof JTextComponent)
		{
			if (pos < 0)
			{
				pos = 0;
			}
			if (pos > ((JTextComponent)component).getDocument().getLength())
			{
				pos = ((JTextComponent)component).getDocument().getLength();
			}
			((JTextComponent)component).requestFocus();
			((JTextComponent)component).setCaretPosition(pos);
		}
	}

	public String js_getElementType()
	{
		return IScriptBaseMethods.TEXT_FIELD;
	}

	public String js_getSelectedText()
	{
		if (component instanceof JTextComponent)
		{
			return ((JTextComponent)component).getSelectedText();
		}
		return null;
	}

	public void js_selectAll()
	{
		if (component instanceof JTextComponent)
		{
			((JTextComponent)component).selectAll();
		}
		if (component instanceof ISupportInputSelection)
		{
			((ISupportInputSelection)component).selectAll();
		}
	}

	public void js_replaceSelectedText(String s)
	{
		if (component instanceof JTextComponent)
		{
			if (component instanceof ISupportEditProvider && ((ISupportEditProvider)component).getEditProvider() != null) ((ISupportEditProvider)component).getEditProvider().startEdit();
			((JTextComponent)component).replaceSelection(s);
			if (component instanceof ISupportEditProvider && ((ISupportEditProvider)component).getEditProvider() != null) ((ISupportEditProvider)component).getEditProvider().commitData();
		}
		if (component instanceof ISupportInputSelection)
		{
			((ISupportInputSelection)component).replaceSelectedText(s);
		}
	}

	@Override
	public void js_setBorder(String spec)
	{
		Border border = ComponentFactoryHelper.createBorder(spec);
		Border oldBorder = component.getBorder();
		if (component instanceof Component && oldBorder instanceof CompoundBorder && ((CompoundBorder)oldBorder).getInsideBorder() != null)
		{
			Insets insets = ((CompoundBorder)oldBorder).getInsideBorder().getBorderInsets((Component)component);
			component.setBorder(BorderFactory.createCompoundBorder(border,
				BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right)));
		}
		else
		{
			component.setBorder(border);
		}
		jsChangeRecorder.setBorder(spec);
	}
}
