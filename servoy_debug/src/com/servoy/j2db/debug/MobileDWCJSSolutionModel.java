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

package com.servoy.j2db.debug;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.persistence.constants.IFieldConstants;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.scripting.solutionmodel.IJSParent;
import com.servoy.j2db.scripting.solutionmodel.JSCalendar;
import com.servoy.j2db.scripting.solutionmodel.JSChecks;
import com.servoy.j2db.scripting.solutionmodel.JSCombobox;
import com.servoy.j2db.scripting.solutionmodel.JSField;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSList;
import com.servoy.j2db.scripting.solutionmodel.JSPassword;
import com.servoy.j2db.scripting.solutionmodel.JSRadios;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.scripting.solutionmodel.JSText;
import com.servoy.j2db.scripting.solutionmodel.JSTextArea;

/**
 * Solution model for when a mobile solution is debugged using the web-client. It has some mobile specific behavior, for example filtering out title labels.
 * 
 * @author acostescu
 */
public class MobileDWCJSSolutionModel extends JSSolutionModel
{

	public MobileDWCJSSolutionModel(IApplication application)
	{
		super(application);
	}

	@Override
	protected JSForm instantiateForm(Form form, boolean isNew)
	{
		return new MobileDWCJSForm(getApplication(), form, isNew);
	}

	@Override
	protected Form createNewForm(Style style, String name, String dataSource, boolean show_in_menu, Dimension size) throws RepositoryException
	{
		return super.createNewForm(style, name, dataSource, show_in_menu, null); // form dimensions are ignored in mobile
	}

	@Override
	public JSField createField(IJSParent< ? > parent, Field field, boolean isNew)
	{
		switch (field.getDisplayType())
		{
			case IFieldConstants.TEXT_FIELD :
				return new JSText(parent, field, getApplication(), isNew);

			case IFieldConstants.TEXT_AREA :
				return new JSTextArea(parent, field, getApplication(), isNew);

			case IFieldConstants.COMBOBOX :
				return new JSCombobox(parent, field, getApplication(), isNew);

			case IFieldConstants.RADIOS :
				return new JSRadios(parent, field, getApplication(), isNew);

			case IFieldConstants.CHECKS :
				return new JSChecks(parent, field, getApplication(), isNew);

			case IFieldConstants.CALENDAR :
				return new JSCalendar(parent, field, getApplication(), isNew);

			case IFieldConstants.PASSWORD :
				return new JSPassword(parent, field, getApplication(), isNew);
		}

		return super.createField(parent, field, isNew);
	}

	@Override
	@JSFunction
	public JSList newListForm(String formName, String dataSource, String textDataProviderID)
	{
		if (getForm(formName) != null) return null; // a form with that name already exists

		// create form
		MobileDWCJSForm listForm = (MobileDWCJSForm)newForm(formName, dataSource, null, false, 100, 380);
		listForm.setView(IBaseSMForm.LOCKED_TABLE_VIEW);

		// create list abstraction
		JSList listComponent = new JSList(listForm);

		// create other persists for remaining contents of list
		if (textDataProviderID != null) listComponent.setTextDataProviderID(textDataProviderID);

		return listComponent;
	}

	@Override
	public JSList getListForm(String name)
	{
		MobileDWCJSForm f = (MobileDWCJSForm)getForm(name);
		if (f != null && f.getView() == IBaseSMForm.LOCKED_TABLE_VIEW)
		{
			return new JSList(f);
		}
		return null;
	}

	@Override
	public JSList[] getListForms()
	{
		List<JSList> listFormsList = new ArrayList<JSList>();
		JSForm[] forms = getForms();
		if (forms != null)
		{
			for (JSForm form : forms)
			{
				if (form.getView() == IBaseSMForm.LOCKED_TABLE_VIEW)
				{
					listFormsList.add(new JSList((MobileDWCJSForm)form));
				}
			}
		}
		return listFormsList.toArray(new JSList[listFormsList.size()]);
	}
}
