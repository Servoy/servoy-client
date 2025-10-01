/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.solutionmodel.ISMTitle;

/**
 * Title label for elements in mobile form.
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@Deprecated
public class JSTitle extends JSBase<GraphicalComponent> implements ISMTitle
{
	public JSTitle(IJSParent< ? > parent, GraphicalComponent gc, boolean isNew)
	{
		super(parent, gc, isNew);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getDataProviderID()
	 *
	 * @sample
	 * var field = form.newTextField('parent_table_text', 1);
	 * field.getTitle().dataProviderID = 'mytitlevar'
	 */
	@JSGetter
	public String getDataProviderID()
	{
		return getBaseComponent(false).getDataProviderID();
	}

	@JSSetter
	public void setDataProviderID(String arg)
	{
		getBaseComponent(true).setDataProviderID(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getText()
	 *
	 * @sample
	 * var field = form.newTextField('parent_table_text', 1);
	 * field.getTitle().text = 'Parent table'
	 */
	@JSGetter
	public String getText()
	{
		return getBaseComponent(false).getText();
	}

	@JSSetter
	public void setText(String arg)
	{
		getBaseComponent(true).setText(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getDisplaysTags()
	 *
	 * @sample
	 * var field = form.newTextField('parent_table_text', 1);
	 * field.getTitle().text = 'Parent table %%customername%%'
	 * field.getTitle().displaysTags = true
	 */
	@JSGetter
	public boolean getDisplaysTags()
	{
		return getBaseComponent(false).getDisplaysTags();
	}

	@JSSetter
	public void setDisplaysTags(boolean arg)
	{
		getBaseComponent(true).setDisplaysTags(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getVisible()
	 *
	 * @sample
	 * var field = form.newTextField('parent_table_text', 1);
	 * field.getTitle().visible = false
	 */
	@JSGetter
	public boolean getVisible()
	{
		return getBaseComponent(false).getVisible();
	}

	@JSSetter
	public void setVisible(boolean arg)
	{
		getBaseComponent(true).setVisible(arg);
	}

}