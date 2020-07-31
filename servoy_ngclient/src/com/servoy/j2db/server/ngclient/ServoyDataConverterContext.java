/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.ICustomMessageLoader;

/**
 * Context for data converters
 * The FlattenedSolution can never be null, that one always should be given.
 * But the application and form can return null
 *
 * @author jcompagner
 */
public class ServoyDataConverterContext implements IServoyDataConverterContext
{
	private final FlattenedSolution flattenedSolution;
	private final INGApplication application;
	private final IWebFormController form;
	private final ICustomMessageLoader i18nLoader;

	public ServoyDataConverterContext(FlattenedSolution flattenedSolution)
	{
		this.flattenedSolution = flattenedSolution;
		this.application = null;
		this.form = null;
		this.i18nLoader = null;
	}

	public ServoyDataConverterContext(FlattenedSolution flattenedSolution, ICustomMessageLoader i18nLoader)
	{
		this.flattenedSolution = flattenedSolution;
		this.application = null;
		this.form = null;
		this.i18nLoader = i18nLoader;
	}

	public ServoyDataConverterContext(INGApplication application)
	{
		this.flattenedSolution = application.getFlattenedSolution();
		this.application = application;
		this.form = null;
		this.i18nLoader = null;
	}

	public ServoyDataConverterContext(IWebFormController form)
	{
		this.form = form;
		this.application = form.getApplication();
		this.flattenedSolution = application.getFlattenedSolution();
		this.i18nLoader = null;

	}

	@Override
	public FlattenedSolution getSolution()
	{
		return flattenedSolution;
	}

	@Override
	public INGApplication getApplication()
	{
		return application;
	}

	@Override
	public IWebFormController getForm()
	{
		return form;
	}

	@Override
	public ICustomMessageLoader getI18nLoader()
	{
		return this.i18nLoader;
	}
}
