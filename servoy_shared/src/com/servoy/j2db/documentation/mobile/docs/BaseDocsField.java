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

import com.servoy.base.persistence.IBaseField;
import com.servoy.base.scripting.annotations.ServoyClientSupport;

/**
 * Dummy class for use in the documentation generator.
 * 
 * @author rgansevles
 */
@ServoyClientSupport(mc = true, sc = false, wc = false)
public class BaseDocsField implements IBaseField
{
	public String getDataProviderID()
	{
		return null;
	}

	public void setDataProviderID(String arg)
	{
	}

	public int getDisplayType()
	{
		return 0;
	}

	public void setDisplayType(int arg)
	{
	}

	public String getPlaceholderText()
	{
		return null;
	}

	public void setPlaceholderText(String arg)
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

	public String getStyleClass()
	{
		return null;
	}

	public void setStyleClass(String arg)
	{
	}

	public int getValuelistID()
	{
		return 0;
	}

	public void setValuelistID(int arg)
	{
	}

	public int getOnActionMethodID()
	{
		return 0;
	}

	public void setOnActionMethodID(int arg)
	{
	}

	public int getOnDataChangeMethodID()
	{
		return 0;
	}

	public void setOnDataChangeMethodID(int arg)
	{
	}

}