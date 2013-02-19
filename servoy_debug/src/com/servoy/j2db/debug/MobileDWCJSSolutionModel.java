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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;

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
}
