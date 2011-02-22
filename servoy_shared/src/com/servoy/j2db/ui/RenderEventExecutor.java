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

import java.awt.Color;
import java.awt.Font;

import javax.swing.border.Border;

import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.scripting.JSRenderEvent;

/**
 * On render event executor.
 * 
 * @author gboros
 *
 */
public class RenderEventExecutor implements IRenderEventExecutor
{
	private String renderCallback;
	private IScriptExecuter renderScriptExecuter;
	private IRecordInternal renderRecord;
	private int renderIndex;
	private boolean renderIsSelected;

	public void setRenderCallback(String id)
	{
		renderCallback = id;
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
	}

	protected Color renderDefaultBgColor;
	protected Border renderDefaultBorder;
	protected boolean renderDefaultEnabled;
	protected Color renderDefaultFgColor;
	protected Font renderDefaultFont;
	protected String renderDefaultTooltipText;
	protected boolean renderDefaultOpaque;
	protected boolean renderDefaultVisible;

	private boolean useDefaultTransparent = true;
	private boolean useDefaultBackround = true;
	private boolean useDefaultForeground = true;
	private boolean useDefaultFont = true;

	public void saveDefaultRenderProperties(ISupportOnRenderCallback display)
	{
		renderDefaultBgColor = display.getBackground();
		renderDefaultBorder = display.getBorder();
		renderDefaultEnabled = display.isEnabled();
		renderDefaultFgColor = display.getForeground();
		renderDefaultFont = display.getFont();
		renderDefaultTooltipText = display.getToolTipText();
		renderDefaultOpaque = display.isOpaque();
		renderDefaultVisible = display.isVisible();
	}

	public void setUseDefaultTransparent(boolean useDefaultTransparent)
	{
		this.useDefaultTransparent = useDefaultTransparent;
	}

	public boolean isUseDefaultTransparent()
	{
		return useDefaultTransparent;
	}

	public void setUseDefaultBackground(boolean useDefaultBackround)
	{
		this.useDefaultBackround = useDefaultBackround;
	}

	public boolean isUseDefaultBackground()
	{
		return useDefaultBackround;
	}

	public void setUseDefaultForeground(boolean useDefaultForeground)
	{
		this.useDefaultForeground = useDefaultForeground;
	}

	public boolean isUseDefaultForeground()
	{
		return useDefaultForeground;
	}

	public void setUseDefaultFont(boolean useDefaultFont)
	{
		this.useDefaultFont = useDefaultFont;
	}

	public boolean isUseDefaultFont()
	{
		return useDefaultFont;
	}

	protected void setDefaultRenderProperties(ISupportOnRenderCallback display)
	{
		if (isUseDefaultBackground()) display.setBackground(renderDefaultBgColor);
		display.setBorder(renderDefaultBorder);
		display.setComponentEnabled(renderDefaultEnabled);
		if (isUseDefaultForeground()) display.setForeground(renderDefaultFgColor);
		if (isUseDefaultFont()) display.setFont(renderDefaultFont);
		display.setToolTipText(renderDefaultTooltipText);
		if (isUseDefaultTransparent()) display.setOpaque(renderDefaultOpaque);
		display.setComponentVisible(renderDefaultVisible);

	}

	private boolean isOnRenderRunningOnComponentPaint;

	public boolean isOnRenderRunningOnComponentPaint()
	{
		return isOnRenderRunningOnComponentPaint;
	}

	public void fireOnRender(ISupportOnRenderCallback display, boolean hasFocus)
	{
		fireOnRender(display, hasFocus, true);
	}

	public void fireOnRender(ISupportOnRenderCallback display, boolean hasFocus, boolean isRunningOnComponentPaint)
	{
		if (renderScriptExecuter != null && renderCallback != null)
		{
			isOnRenderRunningOnComponentPaint = isRunningOnComponentPaint;
			JSRenderEvent event = new JSRenderEvent();
			event.setElement(display);
			event.setHasFocus(hasFocus);
			event.setRecord(renderRecord);
			event.setIndex(renderIndex);
			event.setSelected(renderIsSelected);
			setDefaultRenderProperties(display);
			renderScriptExecuter.executeFunction(renderCallback, new Object[] { event }, false, display, false, "onRenderMethodID", true); //$NON-NLS-1$
			isOnRenderRunningOnComponentPaint = false;
		}
	}
}