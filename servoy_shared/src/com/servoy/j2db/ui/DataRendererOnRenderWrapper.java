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

import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.PersistHelper;


/**
 * Wrapper used for data renderers for on render callback.
 * 
 * @author gabi
 *
 */
public class DataRendererOnRenderWrapper implements ISupportOnRenderCallback
{
	private final RenderEventExecutor renderEventExecutor;
	private final ISupportOnRenderWrapper onRenderComponent;

	public DataRendererOnRenderWrapper(ISupportOnRenderWrapper onRenderComponent)
	{
		this.onRenderComponent = onRenderComponent;
		renderEventExecutor = new RenderEventExecutor();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getBgcolor()
	 */
	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(getBackground());
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setBgcolor(java.lang.String)
	 */
	public void js_setBgcolor(String clr)
	{
		setBackground(PersistHelper.createColor(clr));
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getFgcolor()
	 */
	public String js_getFgcolor()
	{
		return null;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setFgcolor(java.lang.String)
	 */
	public void js_setFgcolor(String clr)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_isVisible()
	 */
	public boolean js_isVisible()
	{
		return true;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setVisible(boolean)
	 */
	public void js_setVisible(boolean b)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_isEnabled()
	 */
	public boolean js_isEnabled()
	{
		return true;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setEnabled(boolean)
	 */
	public void js_setEnabled(boolean b)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getLocationX()
	 */
	public int js_getLocationX()
	{
		return 0;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getLocationY()
	 */
	public int js_getLocationY()
	{
		return 0;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getAbsoluteFormLocationY()
	 */
	public int js_getAbsoluteFormLocationY()
	{
		return 0;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getWidth()
	 */
	public int js_getWidth()
	{
		return 0;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getHeight()
	 */
	public int js_getHeight()
	{
		return 0;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getName()
	 */
	public String js_getName()
	{
		return null;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return onRenderComponent.getOnRenderElementType();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_putClientProperty(java.lang.Object, java.lang.Object)
	 */
	public void js_putClientProperty(Object key, Object value)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getClientProperty(java.lang.Object)
	 */
	public Object js_getClientProperty(Object key)
	{
		return null;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getBorder()
	 */
	public String js_getBorder()
	{
		return ComponentFactoryHelper.createBorderString(getBorder());
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setBorder(java.lang.String)
	 */
	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getToolTipText()
	 */
	public String js_getToolTipText()
	{
		return null;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setToolTipText(java.lang.String)
	 */
	public void js_setToolTipText(String tooltip)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getFont()
	 */
	public String js_getFont()
	{
		return null;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setFont(java.lang.String)
	 */
	public void js_setFont(String spec)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_isTransparent()
	 */
	public boolean js_isTransparent()
	{
		return false;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setTransparent(boolean)
	 */
	public void js_setTransparent(boolean b)
	{
		// ignore

	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getDataProviderID()
	 */
	public String js_getDataProviderID()
	{
		return null;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderCallback#getRenderEventExecutor()
	 */
	public RenderEventExecutor getRenderEventExecutor()
	{
		return renderEventExecutor;
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
	 * @see com.servoy.j2db.ui.IRenderComponent#setBackground(java.awt.Color)
	 */
	public void setBackground(Color background)
	{
		onRenderComponent.setBackground(background);
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#getBackground()
	 */
	public Color getBackground()
	{
		return onRenderComponent.getBackground();
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#setFont(java.awt.Font)
	 */
	public void setFont(Font font)
	{
		// ignore
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#getFont()
	 */
	public Font getFont()
	{
		return null;
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#setBorder(javax.swing.border.Border)
	 */
	public void setBorder(Border border)
	{
		onRenderComponent.setBorder(border);
	}

	/*
	 * @see com.servoy.j2db.ui.IRenderComponent#getBorder()
	 */
	public Border getBorder()
	{
		return onRenderComponent.getBorder();
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