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

package com.servoy.j2db.scripting;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.ui.IScriptRenderMethods;

/**
 * JSRenderEvent, used as argument to render callbacks.
 * 
 * @author gboros 
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSRenderEvent implements IJavaScriptType
{
	private IScriptRenderMethods element;
	private IRecordInternal record;
	private boolean hasFocus;
	private boolean isSelected;
	private int index;

	public void setElement(IScriptRenderMethods element)
	{
		this.element = element;
	}

	/**
	 * Returns the rendering element.
	 *
	 * @sample event.getElement();
	 * 
	 * @return Renderable the rendering element 
	 */
	public IScriptRenderMethods js_getElement()
	{
		return element;
	}

	public void setRecord(IRecordInternal record)
	{
		this.record = record;
	}

	/**
	 * Returns the record of the rendering element.
	 *
	 * @sample event.getRecord();
	 * 
	 * @return Record of the rendering element
	 */
	public Record js_getRecord()
	{
		return record instanceof Record ? (Record)record : null;
	}

	public void setHasFocus(boolean hasFocus)
	{
		this.hasFocus = hasFocus;
	}

	/**
	 * Returns whatever the rendering element has the focus.
	 *
	 * @sample event.hasFocus();
	 * 
	 * @return true if the rendering element has the focus, false otherwise
	 */
	public boolean js_hasFocus()
	{
		return hasFocus;
	}

	public void setIndex(int index)
	{
		this.index = index;
	}

	/**
	 * Returns the rendering element's record index.
	 *
	 * @sample event.getIndex()
	 * 
	 * @return rendering element's record index
	 */
	public int js_getIndex()
	{
		return index;
	}

	public void setSelected(boolean isSelected)
	{
		this.isSelected = isSelected;
	}

	/**
	 * Returns whatever the rendering element is selected.
	 *
	 * @sample event.isSelected()
	 * 
	 * @return true if the rendering element is selected, false otherwise
	 */
	public boolean js_isSelected()
	{
		return isSelected;
	}


	@Override
	public String toString()
	{
		return "JSRenderEvent(element = " + element + ", hasFocus = " + hasFocus + ", isSelected = " + isSelected + ", index = " + index + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
}
