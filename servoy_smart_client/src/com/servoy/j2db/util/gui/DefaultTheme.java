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
package com.servoy.j2db.util.gui;



import java.awt.Font;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

public class DefaultTheme extends DefaultMetalTheme 
{
	// primary colors
	private final ColorUIResource primary1 = new ColorUIResource(32, 32, 32);
	private final ColorUIResource primary2 = new ColorUIResource(160, 160, 180);
	private final ColorUIResource primary3 = new ColorUIResource(200, 200, 224);
	
	
	// secondary colors
	private final ColorUIResource secondary1 = new ColorUIResource(130, 130, 130);
	private final ColorUIResource secondary2 = new ColorUIResource(180, 180, 180);
	private final ColorUIResource secondary3 = new ColorUIResource(224, 224, 224);

    private FontUIResource controlFont;
    private FontUIResource menuFont;
    private FontUIResource windowTitleFont;
    private FontUIResource monospacedFont;

    /**
     * Crates this Theme
     */
    public DefaultTheme()
    {
    	Font font1 = createFont("Tahoma",Font.PLAIN, 11); //$NON-NLS-1$
    	Font font2 = createFont("Tahoma", Font.BOLD, 11); //$NON-NLS-1$
    	
        menuFont = new FontUIResource(font1);
        controlFont = new FontUIResource(font1);
        windowTitleFont =  new FontUIResource(font2);
        monospacedFont = new FontUIResource(font1);
    }

	private Font createFont(String name,int style,int size)
	{
        Font font = new Font(name, style, size); 
        return ( (font == null) ? new Font("Dialog", style, size) : font );  //$NON-NLS-1$
	}

	// methods

	public String getName() { return "Default Theme"; } //$NON-NLS-1$
	
	
	protected ColorUIResource getPrimary1() { return primary1; }
	protected ColorUIResource getPrimary2() { return primary2; }
	protected ColorUIResource getPrimary3() { return primary3; }
	
	protected ColorUIResource getSecondary1() { return secondary1; }
	protected ColorUIResource getSecondary2() { return secondary2; }
	protected ColorUIResource getSecondary3() { return secondary3; }

    /**
     * The Font of Labels in many cases
     */
    public FontUIResource getControlTextFont()
    {
        return controlFont;
    }

    /**
     * The Font of Menus and MenuItems
     */
    public FontUIResource getMenuTextFont()
    {
        return menuFont;
    }

    /**
     * The Font of Nodes in JTrees
     */
    public FontUIResource getSystemTextFont()
    {
        return controlFont;
    }

    /**
     * The Font in TextFields, EditorPanes, etc.
     */
    public FontUIResource getUserTextFont()
    {
        return controlFont;
    }

    /**
     * The Font of the Title of JInternalFrames
     */
    public FontUIResource getWindowTitleFont()
    {
        return windowTitleFont;
    }

}