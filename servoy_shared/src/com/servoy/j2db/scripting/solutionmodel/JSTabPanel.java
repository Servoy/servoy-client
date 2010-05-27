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
package com.servoy.j2db.scripting.solutionmodel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

import org.mozilla.javascript.Function;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSTabPanel extends JSComponent<TabPanel> implements IJSParent
{
	private final IApplication application;

	/**
	 * @param fs
	 * @param createNewgetBaseComponent()
	 */
	public JSTabPanel(JSForm parent, TabPanel tabPanel, IApplication application, boolean isNew)
	{
		super(parent, tabPanel, isNew);
		this.application = application;
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getSupportChild()
	 */
	public ISupportChilds getSupportChild()
	{
		return getBaseComponent(false);
	}

	public JSTab js_newTab(String name, String text, JSForm form)
	{
		return js_newTab(name, text, form, null);
	}

	/**
	 * Adds a new tab with the text label and JSForm and JSRelation (can be null for unrelated).
	 *
	 * @sample 
	 * // Create a parent form.
	 * var form = solutionModel.newForm('parentForm', 'example_data', 'parent_table', 'null', false, 640, 480);
	 * // Create a first child form.
	 * var childOne = solutionModel.newForm('childOne', 'example_data', 'child_table', 'null', false, 400, 300);
	 * childOne.newField('child_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * // Create a relation to link the parent form to the first child form.
	 * var parentToChild = solutionModel.newRelation('parentToChild','example_data','parent_table','example_data','child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_parent_id');
	 * // Create a second child form.
	 * var childTwo = solutionModel.newForm('childTwo', 'example_data', 'my_table', 'null', false, 400, 300);
	 * childTwo.newField('my_table_image', JSField.IMAGE_MEDIA, 10, 10, 100, 100);
	 * // Create a tab panel and add two tabs to it, with the two child forms.
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne, parentToChild); // The first form uses the relation.
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 *
	 * @param name The name of the new tab.
	 *
	 * @param text The text to be displayed on the new tab.
	 *
	 * @param form The JSForm instance that should be displayed in the new tab.
	 *
	 * @param relation optional A JSRelation object that relates the parent form with the form
	 *                          that will be displayed in the new tab.
	 *                          
	 * @return A JSTab instance representing the newly created and added tab.
	 */
	public JSTab js_newTab(String name, String text, JSForm form, Object relation)
	{
		String relationName = null;
		if (relation instanceof RelatedFoundSet)
		{
			relationName = ((RelatedFoundSet)relation).getRelationName();
		}
		else if (relation instanceof String)
		{
			relationName = (String)relation;
		}
		else if (relation instanceof JSRelation)
		{
			relationName = ((JSRelation)relation).js_getName();
		}
		try
		{
			if (relationName != null && application.getFlattenedSolution().getRelationSequence(relationName) == null)
			{
				// invalid relation
				return null;
			}
			Tab newTab = getBaseComponent(true).createNewTab(text, relationName, form.getForm());
			newTab.setName(name);
			return new JSTab(this, newTab, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a JSTab instance representing the tab which has the specified name.
	 * 
	 * @param name The name of the tab that should be returned.
	 * 
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne);
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * tabPanel.getTab('tab2').text = 'Child Two Changed';
	 * 
	 * @return A JSTab instance represented the requested tab.
	 */
	public JSTab js_getTab(String name)
	{
		if (name == null) return null;
		Iterator<IPersist> tabs = getBaseComponent(false).getTabs();
		while (tabs.hasNext())
		{
			Tab tab = (Tab)tabs.next();
			if (name.equals(tab.getName()))
			{
				return new JSTab(this, tab, application, false);
			}
		}
		return null;
	}

	/**
	 * Returns an array of JSTab instances holding the tabs of the tab panel.
	 *
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne);
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * var tabs = tabPanel.getTabs();
	 * for (var i=0; i<tabs.length; i++)
	 * 	application.output("Tab " + i + " has text " + tabs[i].text);
	 * 
	 * @return An array of JSTab instances representing all tabs of this tabpanel.
	 */
	public JSTab[] js_getTabs()
	{
		ArrayList<JSTab> labels = new ArrayList<JSTab>();
		Iterator<IPersist> tabs = getBaseComponent(false).getTabs();
		while (tabs.hasNext())
		{
			Tab tab = (Tab)tabs.next();
			labels.add(new JSTab(this, tab, application, false));
		}
		return labels.toArray(new JSTab[labels.size()]);
	}

	@Deprecated
	public boolean js_getCloseOnTabs()
	{
		return getBaseComponent(false).getCloseOnTabs();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.TabPanel#getScrollTabs()
	 * 
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 200, 200);
	 * tabPanel.newTab('tab1', 'Child Two', childOne, parentToChild); // The first form uses the relation.
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * tabPanel.scrollTabs = true;
	 */
	public boolean js_getScrollTabs()
	{
		return getBaseComponent(false).getScrollTabs();
	}

	@Deprecated
	public Color js_getSelectedTabColor()
	{
		return getBaseComponent(false).getSelectedTabColor();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.TabPanel#getTabOrientation()
	 * 
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne, parentToChild); // The first form uses the relation.
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * // The SM_ALIGNMENT constants TOP, RIGHT, BOTTOM and LEFT can be used to put the
	 * // tabs into the needed position. Use SM_DEFAULTS.NONE to hide the tabs.
	 * // The SM_ALIGNMENT constants SPLIT_HORIZONTAL, SPLIT_VERTICAL can be used to create a split pane
	 * // where the first tab will be left component and the second tab will the right component. 
	 * tabPanel.tabOrientation = SM_ALIGNMENT.BOTTOM;  
	 */
	public int js_getTabOrientation()
	{
		return getBaseComponent(false).getTabOrientation();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getTabSeq()
	 */
	public int js_getTabSeq()
	{
		return getBaseComponent(false).getTabSeq();
	}

	public boolean hasOneTab()
	{
		return getBaseComponent(false).hasOneTab();
	}

	@Deprecated
	public void js_setCloseOnTabs(boolean arg)
	{
		getBaseComponent(true).setCloseOnTabs(arg);
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSTabPanel#js_getOnTabChange() 
	 */
	@Deprecated
	public void js_setOnTabChangeMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnTabChangeMethodID(scriptMethod.getID());
		}
		else
		{
			getBaseComponent(true).setOnTabChangeMethodID(0);
		}
	}


	public void js_setOnTabChange(JSMethod method)
	{
		getBaseComponent(true).setOnTabChangeMethodID(JSForm.getMethodId(application, getBaseComponent(false), method));
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.TabPanel#getOnTabChangeMethodID()
	 *
	 * @sample 
	 * var onTabChangeMethod = form.newFormMethod('function onTabChange(previousIndex, event) { application.output("Tab changed from previous index " + previousIndex + " at " + event.getTimestamp()); }');
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne);
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * tabPanel.onTabChange = onTabChangeMethod;
	 */
	public JSMethod js_getOnTabChange()
	{
		return JSForm.getMethod(application, getJSParent(), getBaseComponent(false).getOnTabChangeMethodID(), false);
	}

	public void js_setScrollTabs(boolean arg)
	{
		getBaseComponent(true).setScrollTabs(arg);
	}

	@Deprecated
	public void js_setSelectedTabColor(Color arg)
	{
		getBaseComponent(true).setSelectedTabColor(arg);
	}

	/**
	 * sets the tab orientation, use one of the ALIGNMENT constants: SM_ALIGNMENT.TOP,BOTTOM,LEFT,RIGHT
	 *
	 * @sample 
	 */
	public void js_setTabOrientation(int arg)
	{
		getBaseComponent(true).setTabOrientation(arg);
	}

	public void js_setTabSeq(int arg)
	{
		getBaseComponent(true).setTabSeq(arg);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "TabPanel: " + getBaseComponent(false).getName(); //$NON-NLS-1$
	}
}
