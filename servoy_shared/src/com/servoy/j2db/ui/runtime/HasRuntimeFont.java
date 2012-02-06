/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

/**
 * Runtime property interface for font.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeFont
{
	/**
	 * Gets or sets the font name, style, and size of an element. 
	 * 
	 * font name - the name of the font family.
	 * style - the type of the font. (plain = 0; bold = 1; italic = 2; bold-italic = 3).
	 * size - the size of the font (in points).
	 * 
	 * @sample
	 * %%prefix%%%%elementName%%.font = 'Tahoma,1,11';
	 *
	 */
	@JSGetter
	public String getFont();

	@JSSetter
	public void setFont(String spec);
}
