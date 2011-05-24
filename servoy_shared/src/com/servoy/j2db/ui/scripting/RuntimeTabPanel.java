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

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.ui.IDepricatedScriptTabPanelMethods;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportReadOnly;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.util.ITabPaneAlike;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Scriptable tabpanel.
 * 
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeTabPanel extends AbstractRuntimeFormContainer<ITabPanel, JComponent> implements IDepricatedScriptTabPanelMethods
{
	public RuntimeTabPanel(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public void js_setReadOnly(boolean b)
	{
		if (enclosingComponent instanceof ISupportReadOnly)
		{
			((ISupportReadOnly)enclosingComponent).setReadOnly(b);
		}
		else
		{
			getComponent().setReadOnly(b);
		}
		getChangesRecorder().setChanged();
	}

	public int js_getAbsoluteFormLocationY()
	{
		return getComponent().getAbsoluteFormLocationY();
	}

	public boolean js_removeTabAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex() && getComponent().removeTabAt(i - 1))
		{
			getChangesRecorder().setChanged();
			return true;
		}
		return false;
	}

	public boolean js_removeAllTabs()
	{
		getChangesRecorder().setChanged();
		return getComponent().removeAllTabs();
	}

	public boolean js_addTab(Object[] vargs)
	{
		if (vargs.length < 1) return false;

		int index = 0;
		Object form = vargs[index++];

		FormController f = null;
		String fName = null;
		boolean readOnly = false;
		if (form instanceof FormController)
		{
			f = (FormController)form;
			readOnly = f.isReadOnly();
		}
		if (form instanceof FormController.JSForm)
		{
			f = ((FormController.JSForm)form).getFormPanel();
			readOnly = f.isReadOnly();
		}

		if (f != null) fName = f.getName();
		if (form instanceof String) fName = (String)form;
		if (fName != null)
		{
			String name = fName;
			if (vargs.length >= 2)
			{
				name = (String)vargs[index++];
			}
			String tabText = name;
			if (vargs.length >= 3)
			{
				tabText = (String)vargs[index++];
			}
			String tooltip = ""; //$NON-NLS-1$
			if (vargs.length >= 4)
			{
				tooltip = (String)vargs[index++];
			}
			String iconURL = ""; //$NON-NLS-1$
			if (vargs.length >= 5)
			{
				iconURL = (String)vargs[index++];
			}
			String fg = null;
			if (vargs.length >= 6)
			{
				fg = (String)vargs[index++];
			}
			String bg = null;
			if (vargs.length >= 7)
			{
				bg = (String)vargs[index++];
			}

			RelatedFoundSet relatedFs = null;
			String relationName = null;
			int tabIndex = -1;
			if (vargs.length > 7)
			{
				Object object = vargs[index++];
				if (object instanceof RelatedFoundSet)
				{
					relatedFs = (RelatedFoundSet)object;
				}
				else if (object instanceof String)
				{
					relationName = (String)object;
				}
				else if (object instanceof Number)
				{
					tabIndex = ((Number)object).intValue();
				}
			}
			if (vargs.length > 8)
			{
				tabIndex = Utils.getAsInteger(vargs[index++]);
			}

			if (relatedFs != null)
			{
				relationName = relatedFs.getRelationName();
				if (f != null && !relatedFs.getDataSource().equals(f.getDataSource()))
				{
					return false;
				}
				// TODO do this check to check if the parent table has this relation? How to get the parent table 
//				Table parentTable = null;
//				application.getSolution().getRelations(Solution.SOLUTION+Solution.MODULES, parentTable, true, false);
			}
			return getComponent().addTab(f != null ? f : fName, name, tabText, tooltip, iconURL, fg, bg, relatedFs != null ? relatedFs : relationName,
				tabIndex, readOnly);
		}
		return false;
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
		if (getComponent() instanceof IDisplayRelatedData && i >= 1 && i <= js_getMaxTabIndex())
		{
			return ((IDisplayRelatedData)getComponent()).getAllRelationNames()[i - 1];
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
			getComponent().setTabEnabledAt(i - 1, b);
			getChangesRecorder().setChanged();
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
			getComponent().setTabTextAt(i - 1, text);
			getChangesRecorder().setChanged();
		}
	}

	public String js_getTabTextAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return getComponent().getTabTextAt(i - 1);
		}
		return null;
	}

	public String js_getTabNameAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return getComponent().getTabNameAt(i - 1);
		}
		return null;
	}

	public String js_getTabFormNameAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return getComponent().getTabFormNameAt(i - 1);
		}
		return null;
	}

	public void js_setTabIndex(Object arg)
	{
		getComponent().setTabIndex(arg);
	}

	public boolean js_isTabEnabledAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			getComponent().isTabEnabledAt(i - 1);
		}
		return false;
	}

	public Object js_getTabIndex()
	{
		return getComponent().getTabIndex();
	}

	public int js_getMaxTabIndex()
	{
		return getComponent().getMaxTabIndex();
	}

}
