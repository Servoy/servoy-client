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
package com.servoy.j2db.smart.cmd;


import java.awt.event.KeyEvent;
import java.util.EventObject;

import javax.swing.KeyStroke;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.smart.J2DBClient;

/** 
 * Cmd to Print a diagram.  Only works under JDK 1.1. 
 */
public class CmdPrint extends AbstractCmd
{
	public static int OVERLAP = 0;

	public CmdPrint(ISmartClientApplication app)
	{
		super(
			app,
			"CmdPrint", app.getI18NMessage("servoy.menuitem.print"), "servoy.menuitem.print", app.getI18NMessage("servoy.menuitem.print.mnemonic").charAt(0), app.loadImage("print.gif")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, J2DBClient.menuShortcutKeyMask));
		setActionCommand("print"); //$NON-NLS-1$
	}

	@Override
	public UndoableEdit doIt(EventObject e)
	{
/*
 * Editor ce = application.getEditor(); String diagramName = (String) getArg("diagramName"); Boolean printPageNums = (Boolean) getArg("printPageNumbers");
 * boolean printNumbers = true; if (printPageNums != null) printNumbers = printPageNums.booleanValue();
 * 
 * String jobName = "Print Diagram"; if (diagramName != null) jobName = diagramName;
 * 
 * Toolkit tk = Toolkit.getDefaultToolkit(); Frame someFrame = Globals.someFrame(); PrintJob pjob = tk.getPrintJob(someFrame, jobName, new Properties()); if
 * (pjob != null) { Graphics pg = pjob.getGraphics(); Dimension d = pjob.getPageDimension();
 * 
 * int leftMargin = 15; int topMargin = 15; int rightMargin = 15; int bottomMargin = 40; int footer = 20; int printableWidth = d.width - leftMargin -
 * rightMargin; int printableHeight = d.height - topMargin - bottomMargin - footer; //System.out.println("pjob.getPageDimension() = " + d);
 * //application.setStatusText("page size is: " + d);
 * 
 * // For the printable area, tha actual origen of Argo is (11,12), // and the printable area from Argo is width = 586, and height = 769. // This was done on a
 * 300 dpi printer. The origen was translated // by a value of 15 to provide a bit of a buffer for different printers. Fig f = null; Rectangle rectSize = null;
 * Rectangle drawingArea = new Rectangle(0,0); Enumeration enum = ce.figs(); int count = 0; while (enum.hasMoreElements()) { f = (Fig) enum.nextElement();
 * rectSize = f.getBounds(); drawingArea.add(rectSize); } int pageNum = 1; for (int y=0; y <= drawingArea.height; y+=printableHeight-OVERLAP) { for (int x=0; x
 * <= drawingArea.width; x+=printableWidth-OVERLAP) { if (pg == null) { pg = pjob.getGraphics(); pageNum++; } application.setStatusText("Printing page " +
 * pageNum); pg.setClip(0, 0, d.width, d.height); pg.clipRect(leftMargin, topMargin, printableWidth, printableHeight); pg.translate(-x + rightMargin, -y +
 * topMargin); ce.print(pg); //System.out.println("x="+x+", y=" + y); pg.setClip(-30000, -30000, 60000, 60000); if (diagramName != null) { pg.setFont(new
 * Font("TimesRoman", Font.PLAIN, 9)); pg.setColor(Color.black); pg.drawString(diagramName, x + 10, y + printableHeight + footer); } if (printNumbers) {
 * pg.setFont(new Font("TimesRoman", Font.PLAIN, 9)); pg.setColor(Color.black); pg.drawString("Page " + pageNum, x + printableWidth - 40, y + printableHeight +
 * footer); } pg.dispose(); // flush page pg = null; } } pjob.end(); } application.setStatusText("Printing finished");
 */
		return null;
	}
}
