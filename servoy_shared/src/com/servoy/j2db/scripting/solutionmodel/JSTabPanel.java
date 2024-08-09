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
import java.util.Arrays;
import java.util.Iterator;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.solutionmodel.ISMMethod;
import com.servoy.j2db.solutionmodel.ISMTabPanel;

/**
 * @author jcompagner
 */
@ServoyClientSupport(mc = false, wc = true, sc = true)
@ServoyDocumented(category = ServoyDocumented.RUNTIME, extendsComponent = "JSComponent")
@Deprecated
public class JSTabPanel extends JSComponent<TabPanel> implements IJSParent<TabPanel>, ISMTabPanel, IConstantsObject
{
	private final IApplication application;

	public JSTabPanel(IJSParent< ? > parent, TabPanel tabPanel, IApplication application, boolean isNew)
	{
		super(parent, tabPanel, isNew);
		this.application = application;
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getSupportChild()
	 */
	public TabPanel getSupportChild()
	{
		return getBaseComponent(false);
	}

	/**
	 * Adds a new tab with the text label and JSForm.
	 *
	 * @sample
	 * // Create a parent form.
	 * var form = solutionModel.newForm('parentForm', 'db:/example_data/parent_table', null, false, 640, 480);
	 * // Create a first child form.
	 * var childOne = solutionModel.newForm('childOne', 'db:/example_data/child_table', null, false, 400, 300);
	 * childOne.newField('child_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * // Create a relation to link the parent form to the first child form.
	 * var parentToChild = solutionModel.newRelation('parentToChild','db:/example_data/parent_table','db:/example_data/child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_parent_id');
	 * // Create a second child form.
	 * var childTwo = solutionModel.newForm('childTwo', 'db:/example_data/my_table', null, false, 400, 300);
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
	 * @return A JSTab instance representing the newly created and added tab.
	 */
	@JSFunction
	public JSTab newTab(String name, String text, IBaseSMForm form)
	{
		return newTab(name, text, form, null);
	}

	/**
	 * Adds a new tab with the text label and JSForm and JSRelation (can be null for unrelated).
	 *
	 * @sample
	 * // Create a parent form.
	 * var form = solutionModel.newForm('parentForm', 'db:/example_data/parent_table', null, false, 640, 480);
	 * // Create a first child form.
	 * var childOne = solutionModel.newForm('childOne', 'db:/example_data/child_table', null, false, 400, 300);
	 * childOne.newField('child_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * // Create a relation to link the parent form to the first child form.
	 * var parentToChild = solutionModel.newRelation('parentToChild','db:/example_data/parent_table','db:/example_data/child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_parent_id');
	 * // Create a second child form.
	 * var childTwo = solutionModel.newForm('childTwo', 'db:/example_data/my_table', null, false, 400, 300);
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
	 * @param relation A JSRelation object that relates the parent form with the form
	 *                          that will be displayed in the new tab.
	 *
	 * @return A JSTab instance representing the newly created and added tab.
	 */
	@JSFunction
	public JSTab newTab(String name, String text, IBaseSMForm form, Object relation)
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
			relationName = ((JSRelation)relation).getName();
		}
		try
		{
			if (relationName != null && application.getFlattenedSolution().getRelationSequence(relationName) == null)
			{
				// invalid relation
				return null;
			}
			Tab newTab = getBaseComponent(true).createNewTab(text, relationName, ((JSForm)form).getSupportChild());
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
	@JSFunction
	public JSTab getTab(String name)
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
	 * Removes the tab with the specified name from the tab panel.
	 *
	 * @param name the name of the tab to be removed
	 *
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne);
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * tabPanel.removeTab('tab1');
	 *
	 */
	@JSFunction
	public void removeTab(String name)
	{
		if (name == null) return;

		TabPanel tp = getBaseComponent(true);
		Iterator<IPersist> tabs = tp.getTabs();
		while (tabs.hasNext())
		{
			Tab tab = (Tab)tabs.next();
			if (name.equals(tab.getName()))
			{
				//removing the child tab from the tabpanel
				tp.removeChild(tab);
				break;
			}
		}
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
	@JSFunction
	public JSTab[] getTabs()
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

	/**
	 * @deprecated not used
	 */
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
	@JSGetter
	public boolean getScrollTabs()
	{
		return getBaseComponent(false).getScrollTabs();
	}

	@JSSetter
	public void setScrollTabs(boolean arg)
	{
		getBaseComponent(true).setScrollTabs(arg);
	}

	/**
	 * @deprecated not supported
	 */
	@Deprecated
	public Color js_getSelectedTabColor()
	{
		return getBaseComponent(false).getSelectedTabColor();
	}

	/**
	 * Specifies either the position of the tabs related to the tab panel or the type of tab-panel.
	 * Can be one of SM_ALIGNMENT.(TOP, RIGHT, BOTTOM, LEFT), DEFAULT_ORIENTATION, HIDE, SPLIT_HORIZONTAL, SPLIT_VERTICAL, ACCORDION_PANEL.
	 *
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne, parentToChild); // The first form uses the relation.
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * // The SM_ALIGNMENT constants TOP, RIGHT, BOTTOM and LEFT can be used to put the
	 * // tabs into the needed position. Use HIDE to hide the tabs. Use DEFAULT_ORIENTATION to restore it to it's initial state.
	 * // The constants SPLIT_HORIZONTAL, SPLIT_VERTICAL can be used to create a split pane,
	 * // where the first tab will be the first component and the second tab will the second component.
	 * // ACCORDION_PANEL can be used to create an accordion pane.
	 * tabPanel.tabOrientation = SM_ALIGNMENT.BOTTOM;
	 */
	@JSGetter
	public int getTabOrientation()
	{
		return getBaseComponent(false).getTabOrientation();
	}

	@JSSetter
	public void setTabOrientation(int arg)
	{
		getBaseComponent(true).setTabOrientation(arg);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getTabSeq()
	 */
	@JSGetter
	public int getTabSeq()
	{
		return getBaseComponent(false).getTabSeq();
	}

	@JSSetter
	public void setTabSeq(int arg)
	{
		getBaseComponent(true).setTabSeq(arg);
	}

	@Deprecated
	public void js_setCloseOnTabs(boolean arg)
	{
		getBaseComponent(true).setCloseOnTabs(arg);
	}

	/**
	 * @deprecated As of release 5.0, replaced by onChange property.
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

	@Deprecated
	public void js_setOnTabChange(JSMethod method)
	{
		setOnChange(method);
	}

	/**
	 * @deprecated As of release 5.0, replaced by onChange property.
	 */
	@Deprecated
	public JSMethod js_getOnTabChange()
	{
		return getOnChange();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.TabPanel#getOnChangeMethodID()
	 *
	 * @sample
	 * var onChangeMethod = form.newMethod('function onTabChange(previousIndex, event) { application.output("Tab changed from previous index " + previousIndex + " at " + event.getTimestamp()); }');
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne);
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * tabPanel.onChange = onChangeMethod;
	 */
	@JSGetter
	public JSMethod getOnChange()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID);
	}

	@JSSetter
	public void setOnChange(ISMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID, (JSMethod)method);
	}

	@Deprecated
	public void js_setSelectedTabColor(Color arg)
	{
		getBaseComponent(true).setSelectedTabColor(arg);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSTabPanel[name:" + getBaseComponent(false).getName() + ",tabs:" + Arrays.toString(getTabs()) + ']';
	}
}
