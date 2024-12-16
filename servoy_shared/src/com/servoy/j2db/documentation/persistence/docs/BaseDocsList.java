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

import com.servoy.base.scripting.solutionhelper.IBaseSHList;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * <p>
 * A dummy class intended for use within the documentation generator. It provides placeholder
 * implementations for list-related properties and methods, such as text, subtext, icons, and event handling,
 * typically used in UI components. These methods are non-functional and return default <code>null</code> values.
 * </p>
 *
 * @author rgansevles
 */
public class BaseDocsList implements IBaseSHList
{
	/**
	 * Dataprovider for countbubble in list.
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
	 * Text property for main text in list.
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
	 * Dataprovider for main text in list. Overrides text property.
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
	 * Text property for sub text in list.
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
	 * Dataprovider for sub text in list. Overrides subtext property.
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
	 * Icon type for list.
	 * Possible values:
	 *  alert
	 *  arrow-d
	 *  arrow-l
	 *  arrow-r
	 *  arrow-u
	 *  back
	 *  bars
	 *  check
	 *  delete
	 *  edit
	 *  forward
	 *  gear
	 *  grid
	 *  home
	 *  info
	 *  minus
	 *  plus
	 *  refresh
	 *  search
	 *  star
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
	 * Dataprovider for icon type in list. Overrides dataIconType property.
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
	@ServoyDocumented(memberKind = ServoyDocumented.MEMBER_KIND_EVENT)
	public IBaseSMMethod getOnAction()
	{
		return null;
	}

	@Override
	public void setOnAction(IBaseSMMethod method)
	{
	}

	/**
	 * The styleClass of the list.
	 */
	@Override
	public String getListStyleClass()
	{
		return null;
	}

	@Override
	public void setListStyleClass(String styleClass)
	{
	}
}