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



import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.Hashtable;

public class  AlignLayout
    implements java.awt.LayoutManager 
{
    public final int CENTER= 0;
    public final int NORTH= 1;
    public final int SOUTH= 2;
    private Hashtable alignedComponents;
    private int hgap;
    private int vgap;
    private int rows;
    private int cols;
    private int[] rowHeights;
    private int[] colWidths;

    public AlignLayout()
    {
        this(-1, 2, 2, 2); 
    }

    public AlignLayout(
        int int1, 
        int int2)
    {
        this(int1, int2, 2, 2); 
    }

    public AlignLayout(
        int int1, 
        int int2, 
        int int3, 
        int int4)
    {
        alignedComponents= new Hashtable(); 
        if  ( (int1 != 0) || (int2 != 0))
        {
            rows= int1; 
            cols= int2; 
            hgap= int3; 
            vgap= int4; 
            return; 
        }
        throw new IllegalArgumentException("invalid rows,cols");  //$NON-NLS-1$
    }

    public void addLayoutComponent(
        String string1, 
        Component component2)
    {
        int int3= 0; 
        if ("North".equals(string1)) //$NON-NLS-1$
        {
            int3= 1; 
        }
        else if ("South".equals(string1)) //$NON-NLS-1$
        {
            int3= 2; 
        }
        if (int3 != 0)
        {
            addAlignedComp(component2, int3); 
        }
    }

    public void removeLayoutComponent(
        Component component1)
    {
        removeAlignedComp(component1); 
    }

    private void getRowColSizes(
        boolean boolean1, 
        Container container2)
    {
        int int3= container2.getComponentCount(); 
        int int4= rows; 
        int int5= cols; 
        if (int4 <= 0)
        {
            int4= ((int3 + int5) - 1) / int5; 
        }
        else
        {
            int5= ((int3 + int4) - 1) / int4; 
        }
        rowHeights= new int[int4]; 
        colWidths= new int[int5]; 
        int int6;
        for (int6= 0; (int6 < int4); int6++)
        {
            rowHeights[int6]= ((int)0); 
        }

        int int7;
        for (int7= 0; (int7 < int5); int7++)
        {
            colWidths[int7]= ((int)0); 
        }

        int int8;
        for (int8= 0; (int8 < int3); int8++)
        {
            Component component9= container2.getComponent(int8); 
            Dimension dimension10;
            if (boolean1 == false)
            {
                dimension10= component9.getPreferredSize(); 
            }
            else
            {
                dimension10= component9.getMinimumSize(); 
            }
            int int11= int8 / int5; 
            if (dimension10.height > rowHeights[int11])
            {
                rowHeights[int11]= ((int)dimension10.height); 
            }
            int int12= int8 % int5; 
            if (dimension10.width <= colWidths[int12])
            {
                continue;
            }
            colWidths[int12]= ((int)dimension10.width); 
            continue;
        }

    }

    private final int totalSize(
        int[] int1)
    {
        int int2= 0; 
        if (int1 != null) 
        {
            int int3;
            for (int3= 0; (int3 < int1.length); int3++)
            {
                int2= int2 + int1[int3]; 
            }

        }
        return int2; 
    }

    public Dimension preferredLayoutSize(
        Container container1)
    {
        Insets insets2= container1.getInsets(); 
        getRowColSizes(false, container1); 
        int int3= totalSize(rowHeights); 
        int int4= totalSize(colWidths); 
        return new Dimension((((insets2.left + insets2.right) + int4) + ((colWidths.length + 1) * hgap)), (((insets2.top + insets2.bottom) + int3) + ((rowHeights.length + 1) * vgap))); 
    }

    public Dimension minimumLayoutSize(
        Container container1)
    {
        Insets insets2= container1.getInsets(); 
        getRowColSizes(true, container1); 
        int int3= totalSize(rowHeights); 
        int int4= totalSize(colWidths); 
        return new Dimension((((insets2.left + insets2.right) + int4) + ((colWidths.length + 1) * hgap)), (((insets2.top + insets2.bottom) + int3) + ((rowHeights.length + 1) * vgap))); 
    }

    public void layoutContainer(
        Container container1)
    {
        int int11;
        int int12;
        Insets insets2= container1.getInsets(); 
        int int3= container1.getComponentCount(); 
        getRowColSizes(false, container1); 
        int int4= rows; 
        int int5= cols; 
        if (int3 != 0)
        {
            if (int4 <= 0)
            {
                int4= ((int3 + int5) - 1) / int5; 
            }
            else
            {
                int5= ((int3 + int4) - 1) / int4; 
            }
            Dimension dimension6= container1.getSize(); 
            int int7= 0; 
            int int8= 0; 
            int int9= 0; 
            int int10;
            for (int10= insets2.left + hgap; (int9 < int5); int9++)
            {
                int11= 0; 
                for (int12= insets2.top + vgap; (int11 < int4); int11++)
                {
                    int int13= (int11 * int5) + int9; 
                    if (int13 < int3)
                    {
                        if (int9 != (int5 - 1))
                        {
                            int7= Math.min(colWidths[int9], ((dimension6.width - insets2.right) - int10)); 
                        }
                        else
                        {
                            int7= ((dimension6.width - insets2.right) - int10) - hgap; 
                        }
                        if (int7 < 0)
                        {
                            int7= 0; 
                        }
                        int8= Math.min(rowHeights[int11], ((dimension6.height - insets2.bottom) - int12)); 
                        if (int8 < 0)
                        {
                            int8= 0; 
                        }
                        Component component14= container1.getComponent(int13); 
                        int int15= int12; 
                        switch (getAlignment(component14))
                        {

                            default:
                            case 1:
                                break;

                            case 0:
                                int15= int12 + ((int8 - component14.getPreferredSize().height) / 2); 
                                break;

                            case 2:
                                int15= int12 + (int8 - component14.getPreferredSize().height); 
                                break;
                        }
                        int8= component14.getPreferredSize().height; 
                        component14.setBounds(int10, int15, int7, int8); 
                    }
                    int12= int12 + (rowHeights[int11] + vgap); 
                }

                int10= int10 + (colWidths[int9] + hgap); 
            }

            return; 
        }
    }

    private void addAlignedComp(
        Component component1, 
        int int2)
    {
        alignedComponents.put(component1, new Integer(int2)); 
    }

    private void removeAlignedComp(
        Component component1)
    {
        alignedComponents.remove(component1); 
    }

    private int getAlignment(
        Component component1)
    {
        Integer integer2= (Integer)alignedComponents.get(component1); 
        if (integer2 != null) 
        {
            return integer2.intValue(); 
        }
        return 0; 
    }
}

