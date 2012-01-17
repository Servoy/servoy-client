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


/**
 * Deprecated tabpanel scriptings methods.
 * 
 * @author jcompagner
 * 
 */
public interface IDepricatedScriptTabPanelMethods
{

	/**
	 * @deprecated As of release 3.x, replaced by {@link #isTabEnabledAt(int)}.
	 */
	@Deprecated
	public abstract boolean js_isTabEnabled(int i);

	/**
	 * @deprecated As of release 3.x, replaced by {@link #getTabFormNameAt(int)}.
	 */
	@Deprecated
	public abstract String js_getSelectedTabFormName();

	/**
	 * @deprecated As of release 3.x, replaced by {@link #setTabEnabledAt(int, boolean)}.
	 */
	@Deprecated
	public abstract void js_setTabEnabled(int i, boolean b);

	/**
	 * @deprecated As of release 5.0, method is obsolete.
	 */
	@Deprecated
	public String js_getTabBGColorAt(int i);

	/**
	 *  @deprecated As of release 5.0, method is obsolete.
	 */
	@Deprecated
	public void js_setTabBGColorAt(int i, String s);
}