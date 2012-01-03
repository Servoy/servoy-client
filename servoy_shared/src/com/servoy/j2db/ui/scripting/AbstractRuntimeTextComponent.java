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

import java.awt.Rectangle;

import javax.swing.text.JTextComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IScriptScrollableMethods;
import com.servoy.j2db.ui.IScriptTextInputMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportEditProvider;
import com.servoy.j2db.ui.ISupportInputSelection;

/**
 * Abstract scriptable text component.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeTextComponent<C extends IFieldComponent, T extends JTextComponent> extends AbstractRuntimeField<C> implements
	IScriptTextInputMethods, IScriptScrollableMethods
{
	protected T textComponent;

	public AbstractRuntimeTextComponent(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	/**
	 * @param textComponent the textComponent to set
	 */
	public void setTextComponent(T textComponent)
	{
		this.textComponent = textComponent;
	}

	public int js_getCaretPosition()
	{
		if (textComponent != null)
		{
			return textComponent.getCaretPosition();
		}
		return 0;
	}

	public void js_setCaretPosition(int pos)
	{
		if (textComponent != null)
		{
			if (pos < 0) textComponent.setCaretPosition(0);
			else if (pos > textComponent.getDocument().getLength()) textComponent.setCaretPosition(textComponent.getDocument().getLength());
			else textComponent.setCaretPosition(pos);
		}
	}

	public void js_setScroll(int x, int y)
	{
		if (textComponent != null)
		{
			textComponent.scrollRectToVisible(new Rectangle(x, y, textComponent.getWidth(), textComponent.getHeight()));
		}
	}

	public int js_getScrollX()
	{
		if (textComponent != null)
		{
			return textComponent.getVisibleRect().x;
		}
		return 0;
	}

	public int js_getScrollY()
	{
		if (textComponent != null)
		{
			return textComponent.getVisibleRect().y;
		}
		return 0;
	}

	@Deprecated
	public boolean js_isEditable()
	{
		return getComponent().isEditable();
	}

	@Deprecated
	public void js_setEditable(boolean b)
	{
		getComponent().setEditable(b);
		getChangesRecorder().setChanged();
	}

	public void js_selectAll()
	{
		if (textComponent != null)
		{
			textComponent.selectAll();
		}
		if (getComponent() instanceof ISupportInputSelection)
		{
			((ISupportInputSelection)getComponent()).selectAll();
		}
	}

	public String js_getSelectedText()
	{
		if (textComponent != null)
		{
			return textComponent.getSelectedText();
		}
		if (getComponent() instanceof ISupportInputSelection)
		{
			return ((ISupportInputSelection)getComponent()).getSelectedText();
		}
		return null;
	}

	public void js_replaceSelectedText(String s)
	{
		if (textComponent != null)
		{
			if (getComponent() instanceof ISupportEditProvider && ((ISupportEditProvider)getComponent()).getEditProvider() != null) ((ISupportEditProvider)getComponent()).getEditProvider().startEdit();
			textComponent.replaceSelection(s);
			if (getComponent() instanceof ISupportEditProvider && ((ISupportEditProvider)getComponent()).getEditProvider() != null) ((ISupportEditProvider)getComponent()).getEditProvider().commitData();
		}
		if (getComponent() instanceof ISupportInputSelection)
		{
			((ISupportInputSelection)getComponent()).replaceSelectedText(s);
		}
	}

	@Override
	public String js_getToolTipText()
	{
		if (textComponent != null)
		{
			return textComponent.getToolTipText();
		}
		else
		{
			return super.js_getToolTipText();
		}
	}

}
