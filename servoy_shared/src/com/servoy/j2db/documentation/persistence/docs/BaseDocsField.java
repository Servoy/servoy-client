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

import java.awt.Dimension;
import java.util.Map;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.persistence.Field;

/**
 * Dummy class for use in the documentation generator.
 *
 * NOTES:
 * - we do not have to document this class, as all field types have corresponding classes;
 * - adding only the needed mobile specific annotations on members
 *
 *
 * @author rgansevles
 */
public class BaseDocsField extends Field implements IComponentWithTitle, IBaseDocsComponent
{

	private static final long serialVersionUID = 1L;

	BaseDocsField()
	{
		super(null, 0, null);
	}

	/**
	 * Dataprovider for header text to field component
	 */
	public String getTitleDataProviderID()
	{
		return null;
	}

	public void setTitleDataProviderID(String arg)
	{
	}

	/**
	 * Flag for header text to field component that enables or disables merging of data inside components using tags (placeholders).
	 * Tags (or placeholders) are words surrounded by %% on each side. There are data tags and
	 * standard tags. Data tags consist in names of dataproviders surrounded by %%. Standard tags
	 * are a set of predefined tags that are made available by the system.
	 *
	 * See the "Merging data" section for more details about tags.
	 *
	 * The default value of this flag is "false", that is merging of data is disabled by default.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getTitleDisplaysTags()
	{
		return false;
	}

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setTitleDisplaysTags(boolean arg)
	{
	}

	/**
	 * Header text to component
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public String getTitleText()
	{
		return null;
	}

	public void setTitleText(String arg)
	{
	}

	@ServoyClientSupport(mc = false, wc = true, sc = true)
	@Override
	public Dimension getSize()
	{
		return super.getSize();
	}

	@Override
	public Map<String, Object> getDesignTimeProperties()
	{
		return null;
	}

	@Override
	public void setDesignTimeProperties(Map<String, Object> map)
	{
	}
}