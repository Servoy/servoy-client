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

import com.servoy.j2db.annotations.ServoyDocumented;


/**
 * @author jcompagner
 * 
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "TabPanel")
public interface IDepricatedScriptTabPanelMethods extends IScriptTabPanelMethods
{

	/**
	 * @see com.servoy.j2db.ui.IScriptTabPanelMethods#js_isTabEnabledAt(int)
	 */
	@Deprecated
	public abstract boolean js_isTabEnabled(int i);

	/**
	 * @see com.servoy.j2db.ui.IScriptTabPanelMethods#js_getTabFormNameAt(int)
	 */
	@Deprecated
	public abstract String js_getSelectedTabFormName();

	/**
	 * @see com.servoy.j2db.ui.IScriptTabPanelMethods#js_setTabEnabledAt(int, boolean)
	 */
	@Deprecated
	public abstract void js_setTabEnabled(int i, boolean b);

	@Deprecated
	public String js_getTabBGColorAt(int i);

	@Deprecated
	public void js_setTabBGColorAt(int i, String s);
}