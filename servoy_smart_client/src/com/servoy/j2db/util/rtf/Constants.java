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

/**
   Class to hold dictionary keys used by the RTF reader/writer.
   These should be moved into StyleConstants.
*/
class Constants
{
    /** An array of TabStops */
    static final String Tabs = "tabs"; //$NON-NLS-1$

    /** The name of the character set the original RTF file was in */
    static final String RTFCharacterSet = "rtfCharacterSet"; //$NON-NLS-1$

    /** Indicates the domain of a Style */
    static final String StyleType = "style:type"; //$NON-NLS-1$

    /** Value for StyleType indicating a section style */
    static final String STSection = "section"; //$NON-NLS-1$
    /** Value for StyleType indicating a paragraph style */
    static final String STParagraph = "paragraph"; //$NON-NLS-1$
    /** Value for StyleType indicating a character style */
    static final String STCharacter = "character"; //$NON-NLS-1$

    /** The style of the text following this style */
    static final String StyleNext = "style:nextStyle"; //$NON-NLS-1$

    /** Whether the style is additive */
    static final String StyleAdditive = "style:additive"; //$NON-NLS-1$

    /** Whether the style is hidden from the user */
    static final String StyleHidden = "style:hidden"; //$NON-NLS-1$

    /* Miscellaneous character attributes */
    static final String Caps          = "caps"; //$NON-NLS-1$
    static final String Deleted       = "deleted"; //$NON-NLS-1$
    static final String Outline       = "outl"; //$NON-NLS-1$
    static final String SmallCaps     = "scaps"; //$NON-NLS-1$
    static final String Shadow        = "shad"; //$NON-NLS-1$
    static final String Strikethrough = "strike"; //$NON-NLS-1$
    static final String Hidden        = "v"; //$NON-NLS-1$

    /* Miscellaneous document attributes */
    static final String PaperWidth    = "paperw"; //$NON-NLS-1$
    static final String PaperHeight   = "paperh"; //$NON-NLS-1$
    static final String MarginLeft    = "margl"; //$NON-NLS-1$
    static final String MarginRight   = "margr"; //$NON-NLS-1$
    static final String MarginTop     = "margt"; //$NON-NLS-1$
    static final String MarginBottom  = "margb"; //$NON-NLS-1$
    static final String GutterWidth   = "gutter"; //$NON-NLS-1$

    /* This is both a document and a paragraph attribute */
    static final String WidowControl  = "widowctrl"; //$NON-NLS-1$
}
