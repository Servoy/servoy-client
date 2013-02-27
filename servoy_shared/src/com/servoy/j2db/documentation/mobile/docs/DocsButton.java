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
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * Dummy class for use in the documentation generator.
 * 
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Button", scriptingName = "Button")
@ServoyClientSupport(mc = true, sc = false, wc = false)
public class DocsButton extends DocsGraphicalComponent
{
	/**
	 * Icon for a button, this must be one of:
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
	public String getDataIcon()
	{
		return null;
	}

	@SuppressWarnings("unused")
	public void setDataIcon(String dataIcon)
	{
	}
}