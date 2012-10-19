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
import java.util.Iterator;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JViewport;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormattingComponent;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.runtime.HasRuntimeScroll;
import com.servoy.j2db.ui.runtime.HasRuntimeValuelistItems;
import com.servoy.j2db.util.model.ComboModelListModelWrapper;

/**
 * Abstract scriptable component with scroll and valuelist support.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeScrollableValuelistComponent<C extends IFieldComponent, F extends JComponent> extends AbstractRuntimeValuelistComponent<C>
	implements HasRuntimeScroll, IFormatScriptComponent, HasRuntimeValuelistItems
{
	protected F field;
	protected ComboModelListModelWrapper list;
	private ComponentFormat componentFormat;

	public AbstractRuntimeScrollableValuelistComponent(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.scripting.IFormatScriptComponent#setComponentFormat(com.servoy.j2db.component.ComponentFormat)
	 */
	public void setComponentFormat(ComponentFormat componentFormat)
	{
		this.componentFormat = componentFormat;
		if (componentFormat != null && getComponent() instanceof IFormattingComponent)
		{
			((IFormattingComponent)getComponent()).installFormat(componentFormat.uiType, componentFormat.parsedFormat.getFormatString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.scripting.IFormatScriptComponent#getComponentFormat()
	 */
	public ComponentFormat getComponentFormat()
	{
		return componentFormat;
	}

	/**
	 * @param field the field to set
	 */
	public void setField(F field)
	{
		this.field = field;
	}

	/**
	 * @param list the list to set
	 */
	public void setList(ComboModelListModelWrapper list)
	{
		this.list = list;
	}

	public void setScroll(int x, int y)
	{
		if (field != null)
		{
			Rectangle rect = new Rectangle(x, y, getComponent().getSize().width, getComponent().getSize().height);
			if (field.getParent() instanceof JViewport)
			{
				// you cannot ask for a region bigger then the actual view extent size to be visible - that would have no effect in some cases;
				// but if you want x and y to be the coordinates where the visible area starts (if that is possible) then a rectangle the same size as the visible area must be used
				Dimension s = ((JViewport)field.getParent()).getExtentSize();
				rect.width = s.width;
				rect.height = s.height;
			}
			field.scrollRectToVisible(rect);
		}
	}

	public int getScrollX()
	{
		if (field != null)
		{
			return field.getVisibleRect().x;
		}
		return 0;
	}

	public int getScrollY()
	{
		if (field != null)
		{
			return field.getVisibleRect().y;
		}
		return 0;
	}

	public Object[] getSelectedElements()
	{
		Set rows = list.getSelectedRows();
		if (rows != null)
		{
			Object[] values = new Object[rows.size()];
			Iterator it = rows.iterator();
			int i = 0;
			while (it.hasNext())
			{
				Integer element = (Integer)it.next();
				values[i++] = list.getRealElementAt(element.intValue());
			}
			return values;
		}
		return new Object[0];
	}
}
