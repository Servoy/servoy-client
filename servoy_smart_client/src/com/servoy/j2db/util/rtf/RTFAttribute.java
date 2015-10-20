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
package com.servoy.j2db.util.rtf;

import java.io.IOException;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;

/** 
 * This interface describes a class which defines a 1-1 mapping between
 * an RTF keyword and a SwingText attribute.
 */
interface RTFAttribute
{
    static final int D_CHARACTER = 0;
    static final int D_PARAGRAPH = 1;
    static final int D_SECTION = 2;
    static final int D_DOCUMENT = 3;
    static final int D_META = 4;

    /* These next three should really be public variables,
       but you can't declare public variables in an interface... */
    /* int domain; */
    public int domain();
    /* String swingName; */
    public Object swingName();
    /* String rtfName; */
    public String rtfName();

    public boolean set(MutableAttributeSet target);
    public boolean set(MutableAttributeSet target, int parameter);

    public boolean setDefault(MutableAttributeSet target);

    /* TODO: This method is poorly thought out */
    public boolean write(AttributeSet source,
		         RTFGenerator target,
			 boolean force)
        throws IOException;

    public boolean writeValue(Object value,
			      RTFGenerator target,
			      boolean force)
        throws IOException;
}
