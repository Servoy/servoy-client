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

import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * @author gboros
 * @since 6.1
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "Renderable", publicName = "Renderable")
public interface IScriptRenderMethods
{
	/**
	 * @see IScriptRenderMethods
	 */
	public static final String JS_RENDERABLE = "Renderable";//$NON-NLS-1$

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getBgcolor()
	 */
	public String getBgcolor();

	public void setBgcolor(String clr);

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getFgcolor()
	 */
	public String getFgcolor();

	public void setFgcolor(String clr);

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#isVisible()
	 */
	public boolean isVisible();

	public void setVisible(boolean b);

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#isEnabled()
	 */
	public boolean isEnabled();

	public void setEnabled(boolean b);

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getLocationX()
	 */
	public int getLocationX();

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getLocationY()
	 */
	public int getLocationY();

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getAbsoluteFormLocationY()
	 */
	public int getAbsoluteFormLocationY();

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getWidth()
	 */
	public int getWidth();

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getHeight()
	 */
	public int getHeight();

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getName()
	 */
	public String getName();

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getElementType()
	 */
	public String getElementType();

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#putClientProperty(Object, Object)
	 */
	public void putClientProperty(Object key, Object value);

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getClientProperty(Object)
	 */
	public Object getClientProperty(Object key);

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getBorder()
	 */
	public String getBorder();

	public void setBorder(String spec);

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getToolTipText()
	 */
	public String getToolTipText();

	public void setToolTipText(String tooltip);

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#getFont()
	 */
	public String getFont();

	public void setFont(String spec);

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeComponent#isTransparent()
	 */
	public boolean isTransparent();

	public void setTransparent(boolean b);

	/**
	 * @sameas com.servoy.j2db.ui.runtime.IRuntimeDataProviderComponent#getDataProviderID()
	 */
	public String getDataProviderID();
}
