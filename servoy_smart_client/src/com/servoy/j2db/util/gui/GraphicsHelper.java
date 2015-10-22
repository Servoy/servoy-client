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



import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;

import javax.swing.JComponent;

public class GraphicsHelper
{
	public static void drawDottedRect(Graphics g,int x, int y, int w, int h)
	{
		drawDotLine(g,x,y,x+w,y);
		drawDotLine(g,x+w,y,x+w,y+h);
		drawDotLine(g,x,y+h,x+w,y+h);
		drawDotLine(g,x,y,x,y+h);
	}
	
	public static void drawDotLine(Graphics g,int x0, int y0, int x1, int y1)
	{
		drawDots(g,x0,y0,x1,y1,2);
	}
	
	public static void drawDots(Graphics g,int x0, int y0, int x1, int y1,int interval)
    {
        if (y0==y1)
        {
            for (int i = x0; i<x1; i+=interval)
            {
                g.drawLine(i,y0, i, y1);
            }
        }
        else
        {
            for (int i = y0; i<y1; i+=interval)
            {
                g.drawLine(x0, i, x1, i);
            }
        }
    }
	
	public static void setDefaultComponentsTabOrder(Vector v) 
	{
//        Component children[] = co.getComponents();
//        Component tmp;
		Object tmp;
        int i,j,c;

        /** Get the tab order from the geometry **/
        for(i=0,c = v.size() ; i < c ; i++) {
            for(j=i ; j < c ; j++) {
                if(i==j)
                    continue;
                if(compareTabOrder((Component)v.elementAt(j),(Component)v.elementAt(i))) 
				{
                    tmp = v.elementAt(i);
//					  tmp = children[i];
					v.setElementAt(v.elementAt(j),i);
//                    children[i] = children[j];
					v.setElementAt(tmp,j);
//                    children[j] = tmp;
                }                    
            }
        }
//		return children;
    }

    public static boolean compareTabOrder(Component a,Component b) 
	{
        Rectangle bounds;
        int ay,by;
        int ax,bx;
        if(a instanceof JComponent) {
            ay = ((JComponent)a).getY();
            ax = ((JComponent)a).getX();
        } else {
            bounds = a.getBounds();
            ay = bounds.y;
            ax = bounds.x;
        }

        if(b instanceof JComponent) {
            by = ((JComponent)b).getY();
            bx = ((JComponent)b).getX();
        } else {
            bounds = b.getBounds();
            by = bounds.y;
            bx = bounds.x;
        }

        if(Math.abs(ay - by) < 10) {
            return (ax < bx);
        }
        return (ay < by);
    }

}
