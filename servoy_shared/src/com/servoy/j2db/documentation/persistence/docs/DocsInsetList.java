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

package com.servoy.j2db.documentation.persistence.docs;

import java.awt.Point;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.scripting.solutionhelper.IBaseSHInsetList;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IRepository;

/**
 * Dummy class for use in the documentation generator.
 *
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "InsetList", scriptingName = "InsetList", typeCode = IRepository.PORTALS)
@ServoyClientSupport(mc = true, wc = false, sc = false, ng = false)
public class DocsInsetList extends BaseDocsList implements IBaseSHInsetList
{
	/**
	 * The name of the relationship between the table related to the currently active
	 * form and the table you want to show data from in the inset list.
	 */
	@Override
	public java.lang.String getRelationName()
	{
		return null;
	}

	@Override
	public void setRelationName(java.lang.String relationName)
	{
	}

	/**
	 * Header text property for inset list.
	 */
	@Override
	public java.lang.String getHeaderText()
	{
		return null;
	}

	@Override
	public void setHeaderText(java.lang.String headerText)
	{
	}

	/**
	 * Dataprovider for header text in inset list. Overrides headerText property.
	 */
	@Override
	public java.lang.String getHeaderDataProviderID()
	{
		return null;
	}

	@Override
	public void setHeaderDataProviderID(java.lang.String headerDataProviderID)
	{
	}

	/**
	 * The name of the component. Through this name it can also accessed in methods.
	 */
	@Override
	public java.lang.String getName()
	{
		return null;
	}

	@Override
	public void setName(java.lang.String name)
	{
	}

	/**
	 * The styleClass of the header.
	 */
	@Override
	public String getHeaderStyleClass()
	{
		return null;
	}

	@Override
	public void setHeaderStyleClass(String styleClass)
	{
	}

	/**
	 * The x and y position of the component, in pixels, separated by a comma.
	 */
	public Point getLocation()
	{
		return null;
	}

	@SuppressWarnings("unused")
	public void setLocation(Point arg)
	{
	}
}