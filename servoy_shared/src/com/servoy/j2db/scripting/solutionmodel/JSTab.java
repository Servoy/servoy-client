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

import java.awt.Dimension;
import java.awt.Point;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.ServoyDocumented;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSTab extends JSBase<Tab> implements IJavaScriptType
{
	private final IApplication application;

	/**
	 * @param tabPanel
	 * @param fs
	 * @param createNewTab
	 */
	public JSTab(JSTabPanel tabPanel, Tab tab, IApplication application, boolean isNew)
	{
		super(tabPanel, tab, isNew);
		this.application = application;
	}

	@Deprecated
	public String js_getBackground()
	{
		return PersistHelper.createColorString(getBaseComponent(false).getBackground());
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getContainsForm()
	 * 
	 * @sample
	 * var childForm = solutionModel.newForm('childForm', 'example_data', 'child_table', 'null', false, 400, 300);
	 * var anotherChildForm = solutionModel.newForm('anotherChildForm', 'example_data', 'child_table', 'null', false, 400, 300);
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.containsForm = anotherChildForm;
	 */
	public JSForm js_getContainsForm()
	{
		Form form = application.getFlattenedSolution().getForm(getBaseComponent(false).getContainsFormID());
		if (form != null)
		{
			return new JSForm(application, form, false);
		}
		return null;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getForeground()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.foreground = '#FF0000';
	 */
	public String js_getForeground()
	{
		return PersistHelper.createColorString(getBaseComponent(false).getForeground());
	}


	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getImageMediaID()
	 * 
	 * @sample
	 * var bytes = plugins.file.readFile('d:/ball.jpg');
	 * var ballImage = solutionModel.newMedia('ball.jpg', bytes);
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.imageMedia = ballImage;
	 */
	public JSMedia js_getImageMedia()
	{
		Media media = application.getFlattenedSolution().getMedia(getBaseComponent(false).getImageMediaID());
		if (media != null)
		{
			return new JSMedia(media, application.getFlattenedSolution(), false);
		}
		return null;
	}

	/**
	 * The X coordinate of the tab. This influences the order in which the tabs are displayed. 
	 * The tabs are displayed in increasing order of the X coordinate. If two tabs have the 
	 * same X coordinate, then they are displayed in increasing order of the Y coordinate.
	 * 
	 * @sample
	 * // Create two tabs, then make the second one be displayed to the left of the first
	 * // by setting their X coordinates in the needed order.
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.x = 10;
	 * var secondTab = tabs.newTab('secondTab', 'Another Child Form', anotherChildForm);
	 * secondTab.x = 0;
	 */
	public int js_getX()
	{
		return getBaseComponent(false).getLocation().x;
	}

	/**
	 * The Y coordinate of the tab. Together with the X coordinate, this influences the order 
	 * in which the tabs are displayed. The tabs are displayed in increasing order of the X coordinate,
	 * and if two tabs have the same X coordinate, then they are displayed in increasing order 
	 * of the Y coordinate.
	 * 
	 * @sample
	 * // Create two tabs, then make the second one be displayed to the left of the first
	 * // by setting their X to the same value and Y coordinates in the needed order. 
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.x = 0;
	 * firstTab.y = 10;
	 * var secondTab = tabs.newTab('secondTab', 'Another Child Form', anotherChildForm);
	 * secondTab.x = 0;
	 * secondTab.y = 0;
	 */
	public int js_getY()
	{
		return getBaseComponent(false).getLocation().y;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getName()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.name = 'firstTabRenamed';
	 */
	public String js_getName()
	{
		return getBaseComponent(false).getName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getRelationName()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm);
	 * firstTab.relationName = 'parent_table_to_child_table';
	 */
	public String js_getRelationName()
	{
		return getBaseComponent(false).getRelationName();
	}

	@Deprecated
	public int js_getWidth()
	{
		return getBaseComponent(false).getSize().width;
	}

	@Deprecated
	public int js_getHeight()
	{
		return getBaseComponent(false).getSize().height;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getText()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.text = 'Better Title';
	 */
	public String js_getText()
	{
		return getBaseComponent(false).getText();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getToolTipText()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.toolTipText = 'Tooltip';
	 */
	public String js_getToolTipText()
	{
		return getBaseComponent(false).getToolTipText();
	}

	@Deprecated
	public void js_setBackground(String arg)
	{
		checkModification();
		getBaseComponent(true).setBackground(PersistHelper.createColor(arg));
	}

	public void js_setContainsForm(JSForm form)
	{
		checkModification();
		if (form == null)
		{
			getBaseComponent(true).setContainsFormID(0);
		}
		else
		{
			getBaseComponent(true).setContainsFormID(form.getForm().getID());
		}
	}

	public void js_setForeground(String arg)
	{
		checkModification();
		getBaseComponent(true).setForeground(PersistHelper.createColor(arg));
	}

	public void js_setImageMedia(JSMedia media)
	{
		checkModification();
		if (media == null)
		{
			getBaseComponent(true).setImageMediaID(0);
		}
		else
		{
			getBaseComponent(true).setImageMediaID(media.getMedia().getID());
		}
	}

	public void js_setX(int x)
	{
		checkModification();
		getBaseComponent(true).setLocation(new Point(x, getBaseComponent(true).getLocation().y));
	}

	public void js_setY(int y)
	{
		checkModification();
		getBaseComponent(true).setLocation(new Point(getBaseComponent(true).getLocation().x, y));
	}

	public void js_setName(String arg)
	{
		checkModification();
		try
		{
			getBaseComponent(true).updateName(DummyValidator.INSTANCE, arg);
		}
		catch (RepositoryException e)
		{
			// should never happen with dummy validator
			Debug.log("could not set name on tab", e); //$NON-NLS-1$
		}
	}

	public void js_setRelationName(String arg)
	{
		checkModification();
		getBaseComponent(true).setRelationName(Utils.toEnglishLocaleLowerCase(arg));
	}

	public void js_setWidth(int width)
	{
		checkModification();
		getBaseComponent(true).setSize(new Dimension(width, getBaseComponent(true).getSize().height));
	}

	public void js_setHeight(int height)
	{
		checkModification();
		getBaseComponent(true).setSize(new Dimension(getBaseComponent(true).getSize().width, height));
	}


	public void js_setText(String arg)
	{
		checkModification();
		getBaseComponent(true).setText(arg);
	}

	public void js_setToolTipText(String arg)
	{
		checkModification();
		getBaseComponent(true).setToolTipText(arg);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Tab: " + getBaseComponent(false).getName(); //$NON-NLS-1$
	}
}
