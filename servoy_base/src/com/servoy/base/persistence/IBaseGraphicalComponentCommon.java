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

package com.servoy.base.persistence;

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Base interface for graphical components (for mobile as well as other clients).
 * 
 * @author rgansevles
 *
 * @since 7.0
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseGraphicalComponentCommon extends IBaseComponent
{
	/**
	 * The dataprovider of the component.
	 */
	String getDataProviderID();

	void setDataProviderID(String arg);

	/**
	 * The text that is displayed inside the component.
	 */
	String getText();

	void setText(String arg);

	/**
	 * Flag that enables or disables merging of data inside components using tags (placeholders).
	 * Tags (or placeholders) are words surrounded by %% on each side. There are data tags and
	 * standard tags. Data tags consist in names of dataproviders surrounded by %%. Standard tags
	 * are a set of predefined tags that are made available by the system.
	 * 
	 * See the "Merging data" section for more details about tags.
	 * 
	 * The default value of this flag is "false", that is merging of data is disabled by default.
	 */
	boolean getDisplaysTags();

	void setDisplaysTags(boolean arg);

}