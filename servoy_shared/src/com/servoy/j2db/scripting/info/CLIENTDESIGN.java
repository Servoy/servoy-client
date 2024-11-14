/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.scripting.info;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 *
 * <p>The <b>CLIENTDESIGN</b> outlines two constants: <b>HANDLES</b>, which determines available resizing handles, and <b>SELECTABLE</b>, which configures whether an element can be selected in the client design. These properties are set using the <code>putClientProperty</code> method.</p>
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@ServoyClientSupport(ng = true, wc = true, sc = true)
public class CLIENTDESIGN implements IPrefixedConstantsObject
{
	/**
	 * Property that can be set using elements['element_1'].putClientProperty(...), it sets the available handles in clientdesign
	 *
	 * @sample
	 * //by default all are present. ('l' stands for left, 't' stands for top, etc.)
	 * elements['element_1'].putClientProperty(CLIENTDESIGN.HANDLES, new Array('r', 'l')); // other options are 't', 'b', 'r', 'l', 'bl', 'br', 'tl', 'tr'
	 */
	public static final String HANDLES = "clientdesign.handles"; //$NON-NLS-1$

	/**
	 * Property that can be set using elements['element_1'].putClientProperty(...), it sets the selectable flag in clientdesign
	 *
	 * @sample
	 * //by default an element with an name is selectable in client design
	 * elements['element_1'].putClientProperty(CLIENTDESIGN.SELECTABLE, false);
	 */
	public static final String SELECTABLE = "clientdesign.selectable"; //$NON-NLS-1$

}
