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
 * Runtime property interface for mnemonic.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeMnemonic
{
	/**
	 * Gets or sets the specified character(s) - typically an underlined letter- used with/without the modifier key(s) for the label, button or image. 
	 * 
	 * Modifiers key values: 
	 * 1 SHIFT 
	 * 2 CTRL 
	 * 4 Meta/CMD (Macintosh)
	 * 8 ALT(Windows, Unix); OPTION (Macintosh) 
	 * 
	 * NOTE: A mnemonic is usually a single key used with/without the CTRL, CMD, SHIFT, ALT, or OPTION key(s) to activate a menu item or command - depending, in part on whether the menmonic applies in a command line or graphic interface. For one description, you can refer to this web page: http://msdn.microsoft.com/en-us/library/bb158536.aspx or perform a search in a web browser search engine using the criteria "mnemonic".
	 * NOTE2: Mnemonic is only supported in Smart Client.
	 * 
	 * @sample
	 * //gets the mnemonic of the element
	 * var my_mnemoic = %%prefix%%%%elementName%%.mnemonic;
	 * 
	 * //sets the mnemonic of the element
	 * %%prefix%%%%elementName%%.mnemonic = 'f';
	 */
	@JSGetter
	public String getMnemonic();

	@JSSetter
	public void setMnemonic(String mnemonic);
}
