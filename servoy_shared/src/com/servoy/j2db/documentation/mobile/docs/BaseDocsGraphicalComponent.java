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

import java.awt.Dimension;
import java.awt.Point;

import com.servoy.base.persistence.IBaseGraphicalComponent;
import com.servoy.base.scripting.annotations.ServoyClientSupport;

/**
 * Dummy class for use in the documentation generator.
 * 
 * @author rgansevles
 */
@ServoyClientSupport(mc = true, sc = false, wc = false)
public class BaseDocsGraphicalComponent implements IBaseGraphicalComponent
{
	public Dimension getSize()
	{
		return null;
	}

	public void setSize(Dimension arg)
	{
	}

	public Point getLocation()
	{
		return null;
	}

	public void setLocation(Point arg)
	{
	}

	public boolean getVisible()
	{
		return false;
	}

	public void setVisible(boolean args)
	{
	}

	public boolean getEnabled()
	{
		return false;
	}

	public void setEnabled(boolean arg)
	{
	}

	public String getName()
	{
		return null;
	}

	public void setName(String arg)
	{
	}

	public String getGroupID()
	{
		return null;
	}

	public void setGroupID(String arg)
	{
	}

	/**
	 * The name of the style class that should be applied to this component.
	 * In mobile this is used as the scheme which is one of a-e.
	 */
	public String getStyleClass()
	{
		return null;
	}

	public void setStyleClass(String arg)
	{
	}

	public String getDataProviderID()
	{
		return null;
	}

	public void setDataProviderID(String arg)
	{
	}

	public String getText()
	{
		return null;
	}

	@Override
	public void setText(String arg)
	{
	}

	public boolean getDisplaysTags()
	{
		return false;
	}

	public void setDisplaysTags(boolean arg)
	{
	}

	public int getOnActionMethodID()
	{
		return 0;
	}

	public void setOnActionMethodID(int arg)
	{
	}

	/**
	 * Dataprovider for header text to field component
	 */
	public String getTitleDataProviderID()
	{
		return null;
	}

	@SuppressWarnings("unused")
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
	public boolean getTitleDisplaysTags()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public void setTitleDisplaysTags(boolean arg)
	{
	}

	/**
	 * Header text to component
	 */
	public String getTitleText()
	{
		return null;
	}

	@SuppressWarnings("unused")
	public void setTitleText(String arg)
	{
	}
}