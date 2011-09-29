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

package com.servoy.j2db.ui.scripting;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IDepricatedScriptTabPanelMethods;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.util.ITabPaneAlike;
import com.servoy.j2db.util.PersistHelper;

/**
 * Scriptable tabpanel.
 * 
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeTabPanel extends RuntimeAccordionPanel implements IDepricatedScriptTabPanelMethods
{
	public RuntimeTabPanel(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	@Override
	public String js_getElementType()
	{
		return IScriptBaseMethods.TABPANEL;
	}

	@Deprecated
	public String js_getTabBGColorAt(int i)
	{
		if (enclosingComponent != null && i >= 1 && i <= js_getMaxTabIndex())
		{
			return PersistHelper.createColorString(((ITabPaneAlike)enclosingComponent).getBackgroundAt(i - 1));
		}
		return null;
	}

	@Deprecated
	public void js_setTabBGColorAt(int i, String clr)
	{
		if (enclosingComponent != null && i >= 1 && i <= js_getMaxTabIndex())
		{
			((ITabPaneAlike)enclosingComponent).setBackgroundAt(i - 1, PersistHelper.createColor(clr));
		}
	}

	@Deprecated
	public boolean js_isTabEnabled(int i)
	{
		return js_isTabEnabledAt(i);
	}

	@Deprecated
	public void js_setTabEnabled(int i, boolean b)
	{
		js_setTabEnabledAt(i, b);
	}

	@Deprecated
	public String js_getSelectedTabFormName()
	{
		return js_getTabFormNameAt(((Integer)js_getTabIndex()).intValue());
	}
}
