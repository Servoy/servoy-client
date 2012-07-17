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
import java.awt.Rectangle;

import javax.swing.JViewport;
import javax.swing.text.JTextComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportEditProvider;
import com.servoy.j2db.ui.ISupportInputSelection;
import com.servoy.j2db.ui.runtime.HasRuntimeScroll;
import com.servoy.j2db.ui.runtime.HasRuntimeTextInput;
import com.servoy.j2db.ui.runtime.IRuntimeInputComponent;

/**
 * Abstract scriptable text component.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeTextComponent<C extends IFieldComponent, T extends JTextComponent> extends AbstractRuntimeField<C> implements
	IRuntimeInputComponent, HasRuntimeTextInput, HasRuntimeScroll
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

	public int getCaretPosition()
	{
		if (textComponent != null)
		{
			return textComponent.getCaretPosition();
		}
		return 0;
	}

	public void setCaretPosition(int pos)
	{
		if (textComponent != null)
		{
			if (pos < 0) textComponent.setCaretPosition(0);
			else if (pos > textComponent.getDocument().getLength()) textComponent.setCaretPosition(textComponent.getDocument().getLength());
			else textComponent.setCaretPosition(pos);
		}
	}

	public void setScroll(int x, int y)
	{
		if (textComponent != null)
		{
			Rectangle r = new Rectangle(x, y, 0, 0);
			if (textComponent.getParent() instanceof JViewport)
			{
				// you cannot ask for a region bigger then the actual view extent size to be visible - that would have no effect in some cases;
				// but if you want x and y to be the coordinates where the visible area starts (if that is possible) then a rectangle the same size as the visible area must be used
				Dimension s = ((JViewport)textComponent.getParent()).getExtentSize();
				r.width = s.width;
				r.height = s.height;
			}
			else
			{
				// hopefully this will never happen - cause if it's a scrolled component these will probably be bigger then any visible area of the textComponent
				r.width = textComponent.getWidth();
				r.height = textComponent.getHeight();
			}
			textComponent.scrollRectToVisible(r);
		}
	}

	public int getScrollX()
	{
		if (textComponent != null)
		{
			return textComponent.getVisibleRect().x;
		}
		return 0;
	}

	public int getScrollY()
	{
		if (textComponent != null)
		{
			return textComponent.getVisibleRect().y;
		}
		return 0;
	}

	@Deprecated
	public boolean isEditable()
	{
		return getComponent().isEditable();
	}

	@Deprecated
	public void setEditable(boolean b)
	{
		if (isEditable() != b)
		{
			getComponent().setEditable(b);
			getChangesRecorder().setChanged();
		}
	}

	public void selectAll()
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

	public String getSelectedText()
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

	public void replaceSelectedText(String s)
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
	public String getToolTipText()
	{
		if (textComponent != null)
		{
			return textComponent.getToolTipText();
		}
		return super.getToolTipText();
	}

}
