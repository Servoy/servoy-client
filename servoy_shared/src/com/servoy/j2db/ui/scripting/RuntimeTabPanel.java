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

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.ui.IDepricatedScriptTabPanelMethods;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportReadOnly;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.util.ITabPaneAlike;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author lvostinar
 * @since 6.0
 *
 */
public class RuntimeTabPanel extends AbstractRuntimeFormContainer implements IDepricatedScriptTabPanelMethods
{
	private final ITabPanel tabPanel;

	public RuntimeTabPanel(ITabPanel component, IStylePropertyChangesRecorder jsChangeRecorder, IApplication application, JComponent enclosingComponent)
	{
		super(component, jsChangeRecorder, application, enclosingComponent);
		this.tabPanel = component;
	}

	public void js_setReadOnly(boolean b)
	{
		if (enclosingComponent instanceof ISupportReadOnly)
		{
			((ISupportReadOnly)enclosingComponent).setReadOnly(b);
		}
		else
		{
			tabPanel.setReadOnly(b);
		}
		jsChangeRecorder.setChanged();
	}

	public int js_getAbsoluteFormLocationY()
	{
		return tabPanel.getAbsoluteFormLocationY();
	}

	public boolean js_removeTabAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex() && tabPanel.removeTabAt(i - 1))
		{
			jsChangeRecorder.setChanged();
			return true;
		}
		return false;
	}

	public boolean js_removeAllTabs()
	{
		jsChangeRecorder.setChanged();
		return tabPanel.removeAllTabs();
	}

	public boolean js_addTab(Object[] vargs)
	{
		return tabPanel.addTab(vargs);
	}

	@Override
	public void js_putClientProperty(Object key, Object value)
	{
		super.js_putClientProperty(key, value);
		if (enclosingComponent != null && !(enclosingComponent instanceof JPanel))
		{
			enclosingComponent.putClientProperty(key, value);
		}
	}

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

	public String js_getTabFGColorAt(int i)
	{
		if (enclosingComponent != null && i >= 1 && i <= js_getMaxTabIndex())
		{
			return PersistHelper.createColorString(((ITabPaneAlike)enclosingComponent).getForegroundAt(i - 1));
		}
		return null;
	}

	public void js_setTabFGColorAt(int i, String clr)
	{
		if (enclosingComponent != null && i >= 1 && i <= js_getMaxTabIndex())
		{
			((ITabPaneAlike)enclosingComponent).setForegroundAt(i - 1, PersistHelper.createColor(clr));
		}
	}

	@Deprecated
	public void js_setTabBGColorAt(int i, String clr)
	{
		if (enclosingComponent != null && i >= 1 && i <= js_getMaxTabIndex())
		{
			((ITabPaneAlike)enclosingComponent).setBackgroundAt(i - 1, PersistHelper.createColor(clr));
		}
	}

	public String js_getTabRelationNameAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return ((IDisplayRelatedData)tabPanel).getAllRelationNames()[i - 1];
		}
		return null;
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

	public void js_setTabEnabledAt(int i, boolean b)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			tabPanel.setTabEnabledAt(i - 1, b);
			jsChangeRecorder.setChanged();
		}
	}

	@Deprecated
	public String js_getSelectedTabFormName()
	{
		return js_getTabFormNameAt(((Integer)js_getTabIndex()).intValue());
	}

	public void js_setTabTextAt(int i, String text)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			tabPanel.setTabTextAt(i - 1, text);
			jsChangeRecorder.setChanged();
		}
	}

	public String js_getTabTextAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return tabPanel.getTabTextAt(i - 1);
		}
		return null;
	}

	public String js_getTabNameAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return tabPanel.getTabFormNameAt(i - 1);
		}
		return null;
	}

	public String js_getTabFormNameAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return tabPanel.getTabFormNameAt(i - 1);
		}
		return null;
	}

	public void js_setTabIndex(Object arg)
	{
		tabPanel.setTabIndex(arg);
	}

	public boolean js_isTabEnabledAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return tabPanel.isTabEnabledAt(i - 1);
		}
		return false;
	}

	public Object js_getTabIndex()
	{
		return tabPanel.getTabIndex();
	}

	public int js_getMaxTabIndex()
	{
		return tabPanel.getMaxTabIndex();
	}

}
