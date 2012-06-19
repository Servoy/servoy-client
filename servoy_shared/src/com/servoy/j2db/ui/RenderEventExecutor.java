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

package com.servoy.j2db.ui;

import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.JSRenderEvent;
import com.servoy.j2db.util.Utils;

/**
 * On render event executor.
 * 
 * @author gboros
 *
 */
public class RenderEventExecutor
{
	private final ISupportOnRenderCallback onRenderComponent;
	private String renderCallback;
	private Object[] renderCallbackArgs;
	private IScriptExecuter renderScriptExecuter;
	private IRecordInternal renderRecord;
	private int renderIndex;
	private boolean renderIsSelected;
	private boolean isRenderStateChanged;
	private boolean isOnRenderExecuting;

	public RenderEventExecutor(ISupportOnRenderCallback onRenderComponent)
	{
		this.onRenderComponent = onRenderComponent;
	}

	public void setRenderCallback(String id, Object[] args)
	{
		renderCallback = id;
		renderCallbackArgs = args;
	}

	public void setRenderScriptExecuter(IScriptExecuter scriptExecuter)
	{
		renderScriptExecuter = scriptExecuter;
	}

	public boolean hasRenderCallback()
	{
		return renderCallback != null;
	}

	public void setRenderState(IRecordInternal record, int index, boolean isSelected)
	{
		renderRecord = record;
		renderIndex = index;
		renderIsSelected = isSelected;
		setRenderStateChanged();
	}

	public void setRenderStateChanged()
	{
		isRenderStateChanged = true;
		onRenderComponent.setRenderableStateChanged();
	}

	public boolean isRenderStateChanged()
	{
		return isRenderStateChanged;
	}

	public boolean isOnRenderExecuting()
	{
		return isOnRenderExecuting;
	}

	public void fireOnRender(boolean hasFocus)
	{
		if (isRenderStateChanged && renderScriptExecuter != null && renderCallback != null)
		{
			isOnRenderExecuting = true;

			IScriptRenderMethods renderable = onRenderComponent.getRenderable();
			if (renderable instanceof RenderableWrapper) ((RenderableWrapper)renderable).resetProperties();

			JSRenderEvent event = new JSRenderEvent();
			event.setElement(onRenderComponent);
			event.setHasFocus(hasFocus);
			event.setRecord(renderRecord);
			event.setIndex(renderIndex);
			event.setSelected(renderIsSelected);

			renderScriptExecuter.executeFunction(renderCallback, Utils.arrayMerge(new Object[] { event }, renderCallbackArgs), false,
				onRenderComponent.getComponent(), false, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID.getPropertyName(), true);
			isRenderStateChanged = false;
			isOnRenderExecuting = false;
		}
	}
}