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

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.solutionmodel.mobile.IMobileSMLabel;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.solutionmodel.ISMLabel;

@ServoyDocumented(category = ServoyDocumented.RUNTIME, extendsComponent = "JSComponent", scriptingName = "JSLabel")
@Deprecated
public class JSLabel extends JSGraphicalComponent implements ISMLabel, IMobileSMLabel
{
	public JSLabel(IJSParent< ? > parent, GraphicalComponent gc, IApplication application, boolean isNew)
	{
		super(parent, gc, application, isNew);
	}

	@JSGetter
	public int getLabelSize()
	{
		Number labelSize = (Number)getBaseComponent(false).getCustomMobileProperty(IMobileProperties.HEADER_SIZE.propertyName);
		if (labelSize != null)
		{
			return labelSize.intValue();
		}
		return IMobileProperties.HEADER_SIZE.defaultValue.intValue();
	}

	@JSSetter
	public void setLabelSize(int size)
	{
		if (size > 0 && size < 7)
		{
			getBaseComponent(true).putCustomDesignTimeProperty(IMobileProperties.HEADER_SIZE.propertyName, Integer.valueOf(size));
		}
	}

	/**
	 * Get title label for the label.
	 *
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table');
	 * var label = form.newLabel('Customers', 1);
	 * label.getTitle().text = 'Some text'
	 * forms['someForm'].controller.show()
	 */
	@Override
	@JSFunction
	public JSTitle getTitle()
	{
		return new JSTitle(getJSParent(), JSField.getTitleForComponent(this), false);
	}

}