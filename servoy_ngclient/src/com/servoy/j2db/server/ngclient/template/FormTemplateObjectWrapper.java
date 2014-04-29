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

package com.servoy.j2db.server.ngclient.template;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.DefaultNavigator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.util.Debug;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Wrap some objects for form file generation using freemarker templates.
 *
 * @author rgansevles
 *
 */
public class FormTemplateObjectWrapper extends DefaultObjectWrapper implements IFormElementValidator
{
	private final FlattenedSolution fs;
	private final boolean useControllerProvider;
	private Form flattenedForm;

	/**
	 * @param fs
	 */
	public FormTemplateObjectWrapper(FlattenedSolution fs, boolean useControllerProvider)
	{
		this.fs = fs;
		this.useControllerProvider = useControllerProvider;
	}

	@Override
	public TemplateModel wrap(Object obj) throws TemplateModelException
	{
		Object wrapped;
		if (obj instanceof Form)
		{
			this.flattenedForm = fs.getFlattenedForm((Form)obj);
			wrapped = new FormWrapper(flattenedForm, null, useControllerProvider, this);
		}
		else if (obj instanceof Object[])
		{
			this.flattenedForm = fs.getFlattenedForm((Form)((Object[])obj)[0]);
			wrapped = new FormWrapper(flattenedForm, (String)((Object[])obj)[1], useControllerProvider, this);
		}
		else if (obj == DefaultNavigator.INSTANCE)
		{
			wrapped = new FormElement(DefaultNavigator.INSTANCE, null);
		}
		else if (obj instanceof Part)
		{
			wrapped = new PartWrapper((Part)obj, flattenedForm);
		}
		else if (obj instanceof IFormElement)
		{
			wrapped = ComponentFactory.getFormElement((IFormElement)obj, fs);
		}
		else
		{
			wrapped = obj;
		}
		return super.wrap(wrapped);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.template.IFormElementValidator#isSpecValid(com.servoy.j2db.persistence.BaseComponent)
	 */
	@Override
	public boolean isComponentSpecValid(IFormElement formElement)
	{
		FormElement fe = ComponentFactory.getFormElement(formElement, fs);
		try
		{
			return fe.getWebComponentSpec(true) != null;
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return false;
		}
	}
}
