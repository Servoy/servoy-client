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
package com.servoy.j2db.server.headlessclient.dataui;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;

import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.util.model.ComboModelListModelWrapper;

/**
 * A {@link IChoiceRenderer} implementation for getting id and display values of a form components with {@link ComboModelListModelWrapper} valuelist.
 * 
 * @author jcompagner
 */
public final class WebChoiceRenderer implements IChoiceRenderer
{
	private final ComboModelListModelWrapper list;
	private final FormComponent component;

	/**
	 * @param component can be null if no converter is used by the component using this renderer.
	 * @param list
	 */
	public WebChoiceRenderer(FormComponent component, ComboModelListModelWrapper list)
	{
		this.list = list;
		this.component = component;
	}

	public String getIdValue(Object object, int index)
	{
		Object o = list.getRealElementAt(index);
		if (o == null) return "";
		return o.toString();
	}

	public Object getDisplayValue(Object object)
	{
		int index = list.realValueIndexOf(object);
		if (index == -1) return "";
		Object display = list.getElementAt(index);
		if (display == null) return "";
		// if component != null then it's converter will convert this to String
		return (component == null && !IValueList.SEPARATOR.equals(display)) ? display.toString() : display;
	}
}