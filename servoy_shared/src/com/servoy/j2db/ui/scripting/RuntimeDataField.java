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
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.JTextComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptFieldMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportEditProvider;
import com.servoy.j2db.ui.ISupportFormatter;
import com.servoy.j2db.ui.ISupportInputSelection;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;

/**
 * Scriptable plain text component.
 * 
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeDataField extends AbstractRuntimeFormattedValuelistComponent<IFieldComponent> implements IScriptFieldMethods
{
	public RuntimeDataField(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public int js_getCaretPosition()
	{
		if (getComponent() instanceof JTextComponent)
		{
			return ((JTextComponent)getComponent()).getCaretPosition();
		}
		return 0;
	}

	public void js_setCaretPosition(int pos)
	{
		if (getComponent() instanceof JTextComponent)
		{
			if (pos < 0)
			{
				pos = 0;
			}
			if (pos > ((JTextComponent)getComponent()).getDocument().getLength())
			{
				pos = ((JTextComponent)getComponent()).getDocument().getLength();
			}
			((JTextComponent)getComponent()).requestFocus();
			((JTextComponent)getComponent()).setCaretPosition(pos);
		}
	}

	public String js_getElementType()
	{
		return IScriptBaseMethods.TEXT_FIELD;
	}

	public String js_getSelectedText()
	{
		if (getComponent() instanceof JTextComponent)
		{
			return ((JTextComponent)getComponent()).getSelectedText();
		}
		return null;
	}

	public void js_selectAll()
	{
		if (getComponent() instanceof JTextComponent)
		{
			((JTextComponent)getComponent()).selectAll();
		}
		if (getComponent() instanceof ISupportInputSelection)
		{
			((ISupportInputSelection)getComponent()).selectAll();
		}
	}

	public void js_replaceSelectedText(String s)
	{
		if (getComponent() instanceof JFormattedTextField)
		{
			JFormattedTextField textComponent = (JFormattedTextField)getComponent();
			if (textComponent instanceof ISupportEditProvider && ((ISupportEditProvider)textComponent).getEditProvider() != null) ((ISupportEditProvider)textComponent).getEditProvider().startEdit();
			int selStart = textComponent.getSelectionStart();
			int selEnd = textComponent.getSelectionEnd();
			if (!textComponent.hasFocus())
			{
				if (textComponent.getFormatterFactory() instanceof DefaultFormatterFactory &&
					((DefaultFormatterFactory)textComponent.getFormatterFactory()).getEditFormatter() != null)
				{
					DefaultFormatterFactory dff = (DefaultFormatterFactory)textComponent.getFormatterFactory();
					((ISupportFormatter)textComponent).setFormatter(dff.getEditFormatter());
					textComponent.select(selStart, selEnd);
				}
			}
			textComponent.replaceSelection(s);
			try
			{
				textComponent.commitEdit();
			}
			catch (ParseException e)
			{
				Debug.error(e);
			}
			if (textComponent instanceof ISupportEditProvider && ((ISupportEditProvider)textComponent).getEditProvider() != null) ((ISupportEditProvider)textComponent).getEditProvider().commitData();
		}
		if (getComponent() instanceof ISupportInputSelection)
		{
			((ISupportInputSelection)getComponent()).replaceSelectedText(s);
		}
	}

	@Override
	public void js_setBorder(String spec)
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
	}
}
