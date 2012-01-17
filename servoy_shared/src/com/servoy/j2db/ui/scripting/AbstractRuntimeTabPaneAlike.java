/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
import com.servoy.j2db.IForm;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.ui.IScriptTabPaneAlikeMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportReadOnly;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.runtime.IRuntimeTabPaneAlike;
import com.servoy.j2db.util.ITabPaneAlike;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Scriptable accordion panel.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeTabPaneAlike extends AbstractRuntimeFormContainer<ITabPanel, JComponent> implements IRuntimeTabPaneAlike,
	IScriptTabPaneAlikeMethods
{
	public AbstractRuntimeTabPaneAlike(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public void setReadOnly(boolean b)
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

	public int getAbsoluteFormLocationY()
	{
		return getComponent().getAbsoluteFormLocationY();
	}

	// 1-based
	public boolean js_removeTabAt(int i)
	{
		return removeTabAt(i - 1);
	}

	// 0-based
	public boolean removeTabAt(int i)
	{
		if (i >= 0 && i <= getMaxTabIndex() && getComponent().removeTabAt(i))
		{
			getChangesRecorder().setChanged();
			return true;
		}
		return false;
	}

	public boolean removeAllTabs()
	{
		getChangesRecorder().setChanged();
		return getComponent().removeAllTabs();
	}

	public boolean addTab(IForm formController, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg,
		IFoundSet relatedFoundSet, int tabIndex)
	{
		if (relatedFoundSet instanceof RelatedFoundSet)
		{
			return getComponent().addTab(formController, formController.getName(), tabName, tabText, toolTip, iconURL, fg, bg,
				relatedFoundSet.getRelationName(), (RelatedFoundSet)relatedFoundSet, tabIndex);
		}
		return false;
	}

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg, String relationName,
		int tabIndex)
	{
		return getComponent().addTab(null, formName, tabName, tabText, toolTip, iconURL, fg, bg, relationName, null, tabIndex);
	}

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg, IFoundSet relatedFoundSet,
		int tabIndex)
	{
		if (relatedFoundSet instanceof RelatedFoundSet)
		{
			return getComponent().addTab(null, formName, tabName, tabText, toolTip, iconURL, fg, bg, relatedFoundSet.getRelationName(),
				(RelatedFoundSet)relatedFoundSet, tabIndex);
		}
		return false;
	}

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg, String relationName)
	{
		return addTab(formName, tabName, tabText, toolTip, iconURL, fg, bg, relationName, -1);
	}

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg, IFoundSet relatedFoundSet)
	{
		return addTab(formName, tabName, tabText, toolTip, iconURL, fg, bg, relatedFoundSet, -1);
	}

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg)
	{
		return addTab(formName, tabName, tabText, toolTip, iconURL, fg, bg, (String)null, -1);
	}

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL)
	{
		return addTab(formName, tabName, tabText, toolTip, iconURL, null, null, (String)null, -1);
	}

	public boolean addTab(String formName, String tabName)
	{
		return addTab(formName, tabName, tabName, "", "", null, null, (String)null, -1);
	}

	public boolean js_addTab(Object[] vargs)
	{
		if (vargs.length < 1) return false;

		int index = 0;
		Object form = vargs[index++];

		IForm formController = null;
		String formName = null;
		if (form instanceof IForm)
		{
			formController = (IForm)form;
		}
		if (form instanceof FormController.JSForm)
		{
			formController = ((FormController.JSForm)form).getFormPanel();
		}

		if (formController != null) formName = formController.getName();
		if (form instanceof String) formName = (String)form;
		if (formName == null)
		{
			return false;
		}

		String tabName = formName;
		if (vargs.length >= 2)
		{
			tabName = (String)vargs[index++];
		}
		String tabText = tabName;
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
				relationName = relatedFs.getRelationName();
				if (formController != null && !relatedFs.getDataSource().equals(formController.getDataSource()))
				{
					return false;
				}
				// TODO do this check to check if the parent table has this relation? How to get the parent table 
//				Table parentTable = null;
//				application.getSolution().getRelations(Solution.SOLUTION+Solution.MODULES, parentTable, true, false);
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

		return getComponent().addTab(formController, formName, tabName, tabText, tooltip, iconURL, fg, bg, relationName, relatedFs, tabIndex);
	}

	@Override
	public void putClientProperty(Object key, Object value)
	{
		super.putClientProperty(key, value);
		if (enclosingComponent != null && !(enclosingComponent instanceof JPanel))
		{
			enclosingComponent.putClientProperty(key, value);
		}
	}

	public String getElementType()
	{
		return IRuntimeComponent.ACCORDIONPANEL;
	}

	// 1-based
	public String js_getTabFGColorAt(int i)
	{
		return getTabFGColorAt(i - 1);
	}

	// 0-based
	public String getTabFGColorAt(int i)
	{
		if (enclosingComponent != null && i >= 0 && i <= getMaxTabIndex())
		{
			return PersistHelper.createColorString(((ITabPaneAlike)enclosingComponent).getForegroundAt(i));
		}
		return null;
	}

	// 1-based
	public void js_setTabFGColorAt(int i, String clr)
	{
		setTabFGColorAt(i - 1, clr);
	}

	// 0-based
	public void setTabFGColorAt(int i, String clr)
	{
		if (enclosingComponent != null && i >= 0 && i <= getMaxTabIndex())
		{
			((ITabPaneAlike)enclosingComponent).setForegroundAt(i, PersistHelper.createColor(clr));
		}
	}

	// 1-based
	public String js_getTabRelationNameAt(int i)
	{
		return getTabRelationNameAt(i - 1);
	}

	// 0-based
	public String getTabRelationNameAt(int i)
	{
		if (getComponent() instanceof IDisplayRelatedData && i >= 0 && i <= getMaxTabIndex())
		{
			return ((IDisplayRelatedData)getComponent()).getAllRelationNames()[i];
		}
		return null;
	}

	// 1-based
	public void js_setTabEnabledAt(int i, boolean b)
	{
		setTabEnabledAt(i - 1, b);
	}

	// 0-based
	public void setTabEnabledAt(int i, boolean b)
	{
		if (i >= 0 && i <= getMaxTabIndex())
		{
			getComponent().setTabEnabledAt(i, b);
			getChangesRecorder().setChanged();
		}
	}

	// 1-based
	public void js_setTabTextAt(int i, String text)
	{
		setTabTextAt(i - 1, text);
	}

	// 0-based
	public void setTabTextAt(int i, String text)
	{
		if (i >= 0 && i <= getMaxTabIndex())
		{
			getComponent().setTabTextAt(i, text);
			getChangesRecorder().setChanged();
		}
	}

	// 1-based
	public String js_getTabTextAt(int i)
	{
		return getTabTextAt(i - 1);
	}

	// 0-based
	public String getTabTextAt(int i)
	{
		if (i >= 0 && i <= getMaxTabIndex())
		{
			return getComponent().getTabTextAt(i);
		}
		return null;
	}

	// 1-based
	public String js_getTabNameAt(int i)
	{
		return getTabNameAt(i - 1);
	}

	// 0-based
	public String getTabNameAt(int i)
	{
		if (i >= 0 && i <= getMaxTabIndex())
		{
			return getComponent().getTabNameAt(i);
		}
		return null;
	}

	// 1-based
	public String js_getTabFormNameAt(int i)
	{
		return getTabFormNameAt(i - 1);
	}

	// 0-based
	public String getTabFormNameAt(int i)
	{
		if (i >= 0 && i <= getMaxTabIndex())
		{
			return getComponent().getTabFormNameAt(i);
		}
		return null;
	}

	// 1-based
	public void js_setTabIndex(Object arg)
	{
		int index = Utils.getAsInteger(arg);
		if (index > 0)
		{
			setTabIndex(index - 1);
		}
		else
		{
			setTabIndex("" + arg);
		}
	}

	// 0-based
	public void setTabIndex(int i)
	{
		if (i >= 0 && i <= getMaxTabIndex())
		{
			getComponent().setTabIndex(i);
		}
	}

	public void setTabIndex(String name)
	{
		if (!Utils.stringIsEmpty(name))
		{
			getComponent().setTabIndex(name);
		}
	}

	// 1-based
	public boolean js_isTabEnabledAt(int i)
	{
		return isTabEnabledAt(i - 1);
	}

	// 0-based
	public boolean isTabEnabledAt(int i)
	{
		if (i >= 0 && i <= getMaxTabIndex())
		{
			return getComponent().isTabEnabledAt(i);
		}
		return false;
	}

	// 1-based
	public Object js_getTabIndex()
	{
		int index = getTabIndex();
		if (index >= 0)
		{
			return Integer.valueOf(index + 1);
		}
		return Integer.valueOf(-1);
	}

	// 0-based
	public int getTabIndex()
	{
		return getComponent().getTabIndex();
	}

	// 1-based
	public int js_getMaxTabIndex()
	{
		return getMaxTabIndex() + 1;
	}

	// 0-based
	public int getMaxTabIndex()
	{
		return getComponent().getMaxTabIndex();
	}
}
