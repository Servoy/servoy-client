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
		return PersistHelper.createColorString(onRenderComponent.getBackground());
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setBgcolor(java.lang.String)
	 */
	public void js_setBgcolor(String clr)
	{
		onRenderComponent.setBackground(PersistHelper.createColor(clr));
	}

	public String getBgcolor()
	{
		return js_getBgcolor();
	}

	public void setBgcolor(String clr)
	{
		js_setBgcolor(clr);
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
		return null;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setBorder(java.lang.String)
	 */
	public void js_setBorder(String spec)
	{
		// ignore
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
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getFormat()
	 */
	public String js_getFormat()
	{
		return null;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setFormat(java.lang.String)
	 */
	public void js_setFormat(String textFormat)
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
}
