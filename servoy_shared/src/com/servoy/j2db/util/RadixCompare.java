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
package com.servoy.j2db.util;

public interface RadixCompare
{

    // Treat key as if it were a string of bytes
    // number returned must lie in the range 0..255!
    public abstract int getKeyByteAt(Object a, int offset);

    // to sort fixed length Latin1 strings you might write:
    // return ((String)a).charAt(offset)&0xff;

    // to sort fixed length Unicode strings you might write:
    //  if (offset%2 == 0 /* e.g. even */)
    //     return (((String)a).charAt(offset/2)>>>8)&0xff; // high byte
    //   else
    //     return ((String)a).charAt(offset/2)&0xff; // low byte

    // To sort binary integers you have to split them up into 4
    // unsigned bytes treating the most significant byte as offset 0.
    // If you were trying to sort signed ints, you would have
    // to add a bias to them to make them all appear positive
    // before breaking them up.
} // end class RadixCompare
