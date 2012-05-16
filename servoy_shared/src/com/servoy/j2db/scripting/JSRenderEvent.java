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
import com.servoy.j2db.ui.IScriptRenderMethodsWithFormat;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.RenderableWrapper;

/**
 * JSRenderEvent, used as argument to render callbacks.
 * 
 * @author gboros 
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSRenderEvent implements IJavaScriptType
{
	private ISupportOnRenderCallback element;
	private IRecordInternal record;
	private boolean hasFocus;
	private boolean isSelected;
	private int index;

	public void setElement(ISupportOnRenderCallback element)
	{
		this.element = element;
	}


	/** 
	 * @deprecated
	 */
	@Deprecated
	public IScriptRenderMethods js_getElement()
	{
		if (element instanceof IScriptableProvider) return (IScriptRenderMethods)((IScriptableProvider)element).getScriptObject();
		return (IScriptRenderMethods)element;
	}

	/**
	 * Returns the element that is being rendered.
	 *
	 * @sample event.getRenderable();
	 * 
	 * @return Renderable the element that is being rendered 
	 */
	public IScriptRenderMethodsWithFormat js_getRenderable()
	{
		IScriptRenderMethods renderable = element.getRenderable();
		if (renderable instanceof IScriptRenderMethodsWithFormat) return (IScriptRenderMethodsWithFormat)renderable;
		return new RenderableWrapper(renderable);
	}

	public void setRecord(IRecordInternal record)
	{
		this.record = record;
	}

	/**
	 * Returns the record of the element that is being rendered.
	 * This is null for elements of type form when they are in table view mode.
	 *
	 * @sample
	 * // type the record returned from the call with JSDoc, fill in the right server/tablename
	 * /** @type {JSRecord<db:/servername/tablename>} *&#47;
	 * var record = event.getRecord();
	 * 
	 * @return Record of the element that is being rendered
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
	 * Returns whether or not the element that is being rendered has focus.
	 *
	 * @sample event.hasFocus();
	 * 
	 * @return true if the element that is being rendered has the focus, false otherwise
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
	 * Returns the record index of the element that is being rendered.
	 *
	 * @sample event.getRecordIndex()
	 * 
	 * @return record index of the element that is being rendered
	 */
	public int js_getRecordIndex()
	{
		if (index == -1) return -1;
		return index + 1;
	}

	public void setSelected(boolean isSelected)
	{
		this.isSelected = isSelected;
	}

	/**
	 * Returns whatever or not the record of the element that is being rendered is selected.
	 *
	 * @sample event.isRecordSelected()
	 * 
	 * @return true if the record of the element that is being rendered is selected
	 */
	public boolean js_isRecordSelected()
	{
		return isSelected;
	}


	@Override
	public String toString()
	{
		return "JSRenderEvent(element = " + element + ", hasFocus = " + hasFocus + ", isSelected = " + isSelected + ", index = " + js_getRecordIndex() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
}
