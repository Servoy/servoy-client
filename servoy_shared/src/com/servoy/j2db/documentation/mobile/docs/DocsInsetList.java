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

package com.servoy.j2db.documentation.mobile.docs;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.scripting.solutionhelper.IBaseSHInsetList;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * Dummy class for use in the documentation generator.
 * 
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "InsetList", scriptingName = "InsetList")
@ServoyClientSupport(mc = true, sc = false, wc = false)
public class DocsInsetList implements IBaseSHInsetList
{
	/**
	 * Dataprovider for countbubble in inset list.
	 */
	@Override
	public java.lang.String getCountDataProviderID()
	{
		return null;
	}

	@Override
	public void setCountDataProviderID(java.lang.String countDataProviderID)
	{
	}

	/**
	 * Text property for main text in inset list.
	 */
	@Override
	public java.lang.String getText()
	{
		return null;
	}

	@Override
	public void setText(java.lang.String text)
	{
	}

	/**
	 * Dataprovider for main text in inset list. Overrides text property.
	 */
	@Override
	public java.lang.String getTextDataProviderID()
	{
		return null;
	}

	@Override
	public void setTextDataProviderID(java.lang.String textDataPRoviderID)
	{
	}

	/**
	 * Text property for sub text in inset list.
	 */
	@Override
	public java.lang.String getSubtext()
	{
		return null;
	}

	@Override
	public void setSubtext(java.lang.String subtext)
	{
	}

	/**
	 * Dataprovider for sub text in inset list. Overrides subtext property.
	 */
	@Override
	public java.lang.String getSubtextDataProviderID()
	{
		return null;
	}

	@Override
	public void setSubtextDataProviderID(java.lang.String subtextDataProviderID)
	{
	}

	/**
	 * Icon type for inset list.
	 * <p>possible values
	 * <ul>
	 * <li>alert</li>
	 * <li>arrow-d</li>
	 * <li>arrow-l</li>
	 * <li>arrow-r</li>
	 * <li>arrow-u</li>
	 * <li>back</li>
	 * <li>check</li>
	 * <li>delete</li>
	 * <li>forward</li>
	 * <li>gear</li>
	 * <li>grid</li>
	 * <li>home</li>
	 * <li>info</li>
	 * <li>minus</li>
	 * <li>plus</li>
	 * <li>refresh</li>
	 * <li>search</li>
	 * <li>star</li>
	 * </ul> 
	 */
	@Override
	public java.lang.String getDataIconType()
	{
		return null;
	}

	@Override
	public void setDataIconType(java.lang.String dataIconType)
	{
	}

	/**
	 * Dataprovider for icon type in inset list. Overrides dataIconType property.
	 */
	@Override
	public java.lang.String getDataIconDataProviderID()
	{
		return null;
	}

	@Override
	public void setDataIconDataProviderID(java.lang.String dataIconDataProviderID)
	{
	}

	/**
	 * The method that is executed when the component is clicked.
	 */
	@Override
	public IBaseSMMethod getOnAction()
	{
		return null;
	}

	@Override
	public void setOnAction(IBaseSMMethod method)
	{
	}

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
}