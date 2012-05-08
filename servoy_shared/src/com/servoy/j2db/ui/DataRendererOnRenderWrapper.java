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

import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.PersistHelper;


/**
 * Wrapper used for data renderers for on render callback.
 * 
 * @author gboros
 *
 */
public class DataRendererOnRenderWrapper implements ISupportOnRenderCallback, IScriptRenderMethods
{
	private final RenderEventExecutor renderEventExecutor;
	private final ISupportOnRenderWrapper onRenderComponent;

	public DataRendererOnRenderWrapper(ISupportOnRenderWrapper onRenderComponent)
	{
		this.onRenderComponent = onRenderComponent;
		renderEventExecutor = new RenderEventExecutor(this);
	}

	public String getBgcolor()
	{
		return PersistHelper.createColorString(onRenderComponent.getBackground());
	}

	public void setBgcolor(String clr)
	{
		if (onRenderComponent instanceof IProviderStylePropertyChanges &&
			((IProviderStylePropertyChanges)onRenderComponent).getStylePropertyChanges() instanceof IStylePropertyChangesRecorder)
		{
			((IStylePropertyChangesRecorder)((IProviderStylePropertyChanges)onRenderComponent).getStylePropertyChanges()).setBgcolor(clr);
		}
		onRenderComponent.setBackground(PersistHelper.createColor(clr));
	}

	public String getFgcolor()
	{
		return null;
	}

	public void setFgcolor(String clr)
	{
		// ignore
	}

	public void setVisible(boolean b)
	{
		// ignore
	}

	public void setEnabled(boolean b)
	{
		// ignore
	}

	public int getLocationX()
	{
		return 0;
	}

	public int getLocationY()
	{
		return 0;
	}

	public int getAbsoluteFormLocationY()
	{
		return 0;
	}

	public int getWidth()
	{
		return 0;
	}

	public int getHeight()
	{
		return 0;
	}

	public String getName()
	{
		return null;
	}

	public String getElementType()
	{
		return onRenderComponent.getOnRenderElementType();
	}

	public void putClientProperty(Object key, Object value)
	{
		// ignore
	}

	public Object getClientProperty(Object key)
	{
		return null;
	}

	public String getBorder()
	{
		return ComponentFactoryHelper.createBorderString(onRenderComponent.getBorder());
	}

	public void setBorder(String spec)
	{
		if (onRenderComponent instanceof IProviderStylePropertyChanges &&
			((IProviderStylePropertyChanges)onRenderComponent).getStylePropertyChanges() instanceof IStylePropertyChangesRecorder)
		{
			((IStylePropertyChangesRecorder)((IProviderStylePropertyChanges)onRenderComponent).getStylePropertyChanges()).setBorder(spec);
		}
		onRenderComponent.setBorder(ComponentFactoryHelper.createBorder(spec));
	}

	public String getFont()
	{
		return null;
	}

	public void setFont(String spec)
	{
		// ignore
	}

	public boolean isTransparent()
	{
		return false;
	}

	public void setTransparent(boolean b)
	{
		// ignore

	}

	public String getDataProviderID()
	{
		return null;
	}

	public RenderEventExecutor getRenderEventExecutor()
	{
		return renderEventExecutor;
	}

	public IScriptRenderMethods getRenderable()
	{
		return this;
	}

	public void setRenderableStateChanged()
	{
		if (onRenderComponent instanceof IProviderStylePropertyChanges) ((IProviderStylePropertyChanges)onRenderComponent).getStylePropertyChanges().setChanged();
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderCallback#getComponent()
	 */
	public Object getComponent()
	{
		return onRenderComponent;
	}

	@Override
	public String toString()
	{
		return onRenderComponent.getOnRenderToString();
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#setComponentEnabled(boolean)
	 */
	public void setComponentEnabled(boolean enabled)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#isEnabled()
	 */
	public boolean isEnabled()
	{
		return true;
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#setComponentVisible(boolean)
	 */
	public void setComponentVisible(boolean visible)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#isVisible()
	 */
	public boolean isVisible()
	{
		return true;
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#setForeground(java.awt.Color)
	 */
	public void setForeground(Color foreground)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#getForeground()
	 */
	public Color getForeground()
	{
		return null;
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#setOpaque(boolean)
	 */
	public void setOpaque(boolean opaque)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#isOpaque()
	 */
	public boolean isOpaque()
	{
		return true;
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#setToolTipText(java.lang.String)
	 */
	public void setToolTipText(String tooltip)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		return null;
	}
}