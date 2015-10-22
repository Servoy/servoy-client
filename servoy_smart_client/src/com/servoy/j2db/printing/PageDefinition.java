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
package com.servoy.j2db.printing;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.RepaintManager;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.smart.dataui.DataRenderer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.Utils;

/**
 * Represents one page which is printable
 * 
 * @author jblok
 */
public class PageDefinition implements Printable
{
	private final IApplication application;
	private final List<DataRendererDefinition> panels;
	private final Dimension size;
	private int heightLeft;
	private int nextYPosition = 0;
	private int nextXPosition = 0;
	private int skipOnY = 0;

	private int insetIndex;
	private final IPrintInfo printInfo;
	private final RendererParentWrapper renderParent;

	PageDefinition(IApplication app, IPrintInfo pp, RendererParentWrapper renderParent, Dimension d)
	{
		application = app;
		printInfo = pp;
		panels = new ArrayList<DataRendererDefinition>();
		size = d;
		heightLeft = size.height;
		this.renderParent = renderParent;
	}

	public int getHeightLeft()
	{
		return heightLeft;
	}

	public int print(Graphics g, PageFormat pf, int pageNumber)
	{
		int retval;
		try
		{
			application.getRuntimeProperties().put("isPrinting", Boolean.TRUE); //$NON-NLS-1$
			application.getRuntimeProperties().put("printGraphics", g); //$NON-NLS-1$

			//set util flag for use in printing
			application.getScriptEngine().getJSApplication().setDidLastPrintPreviewPrint(true);

			//start actual printing
			retval = printPage(g, pf);
		}
		finally
		{
			application.getRuntimeProperties().put("printGraphics", null); //$NON-NLS-1$
			application.getRuntimeProperties().put("isPrinting", null); //$NON-NLS-1$
		}
		return retval;
	}

	int printPage(Graphics g, PageFormat pf)
	{
		Graphics2D graphics2D = (Graphics2D)g;

		double f = printInfo.getZoomFactor();
		double imx = pf.getImageableX() * (1 / f);
		double imy = pf.getImageableY() * (1 / f);
		double imw = pf.getImageableWidth() * (1 / f);
		double imh = pf.getImageableHeight() * (1 / f);

		Rectangle saveClip = graphics2D.getClipBounds();
		Rectangle newClip = new Rectangle((int)imx, (int)imy, (int)imw, (int)imh);

		AffineTransform at = new AffineTransform();
		boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")); //$NON-NLS-1$
		if (isPrinting)
		{
			at.scale(f, f);
			if (f < 1)
			{
				newClip = new Rectangle((int)(pf.getImageableX() * f), (int)(pf.getImageableY() * f), (int)(pf.getImageableWidth() * (1 / f)),
					(int)(pf.getImageableHeight() * (1 / f)));
			}
			else
			{
				newClip = new Rectangle((int)(pf.getImageableX() * (1 / f)), (int)(pf.getImageableY() * (1 / f)), (int)(pf.getImageableWidth() * f),
					(int)(pf.getImageableHeight() * f));
			}
		}
		at.translate((int)imx, (int)imy);

		Rectangle result = newClip.intersection(saveClip);
		graphics2D.setClip(result);//keep within im.

		AffineTransform save = graphics2D.getTransform();
		graphics2D.transform(at);//do transformation
		RepaintManager currentManager = RepaintManager.currentManager(renderParent.getParent());
		boolean isDoubleBufferingEnabled = currentManager.isDoubleBufferingEnabled();
		Object oldPrintState = application.getRuntimeProperties().get("isPrinting"); //$NON-NLS-1$
		try
		{
			System.setProperty("component.isprinting", "true"); //done for Kunststoff LAF (disables bg shading) //$NON-NLS-1$ //$NON-NLS-2$
			application.getRuntimeProperties().put("isPrinting", Boolean.TRUE); // show high resolution images in print preview also (with zoom), not only when really printing (main idea is to get the closest look possible to the real printed material) //$NON-NLS-1$
			currentManager.setDoubleBufferingEnabled(false);
			//-------start rendering------------------------------------
			render(renderParent, graphics2D);
			//----------end rendering------------------------------------
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			application.getRuntimeProperties().put("isPrinting", oldPrintState); //$NON-NLS-1$
			currentManager.setDoubleBufferingEnabled(isDoubleBufferingEnabled);
			System.setProperty("component.isprinting", ""); //done for Kunststoff LAF (disables bg shading)  //$NON-NLS-1$//$NON-NLS-2$
			graphics2D.setTransform(save);
			graphics2D.setClip(saveClip);
			printInfo.flushCachedData();
		}
		return Printable.PAGE_EXISTS;
	}

	private void render(RendererParentWrapper c, Graphics2D graphics2D)
	{
		Rectangle clip = graphics2D.getClipBounds();
		boolean didClip = false;

		graphics2D.setColor(Color.black);

		Iterator<DataRendererDefinition> it = panels.iterator();
		while (it.hasNext())
		{
			DataRendererDefinition drd = it.next();
			DataRenderer partpane = drd.getDataRenderer();

			if (partpane == null) continue;//is empty body
			int Y = drd.getYlocation() - drd.getStartYOrgin();
			int X = drd.getXlocation();

			try
			{
				if (drd.getPart() != null && drd.getPart().getPartType() != Part.LEADING_GRAND_SUMMARY &&
					drd.getPart().getPartType() != Part.TRAILING_GRAND_SUMMARY)//setting the state in grand summary clears the grand sum
				{
					IRecordInternal state = drd.getState();
					if (state instanceof PageNumberState)
					{
						((PageNumberState)state).initPagePositionAndSize(this);
					}
				}
				if (drd.getFullSize().height != drd.getSize().height || drd.getStartYOrgin() != 0)
				{
					Rectangle newClip = new Rectangle(clip);//clone
//System.out.println("Y "+Y+" ,Ylocation "+drd.getYlocation());
					newClip.y = Math.max(Y, drd.getYlocation());
					newClip.height = drd.getSize().height;//Math.min(drd.getSize().height,clip.height);//only allow smaller
					graphics2D.setClip(newClip.intersection(clip));
					didClip = true;
				}
				else
				{
					didClip = false;
				}
//System.out.println("drd "+drd+" didClip "+didClip);

				//translate and scale does effect clipping !!!!!!!!
				graphics2D.translate(X, Y);//set the right position (the component paint ALWAY fom its 0,0 position, becouse of the absulut positioning inside it)

				//only needed to get it painted (position is irrelevant)
				partpane.setBounds(0, 0, drd.getSize().width, drd.getFullSize().height);

				//needed for correct drawing
				drd.tempAddToParent(c, true, false, false);//this does also fill the renderer again with record data, must do also to make the summaries work

				//draw the pane
				drd.printAll(graphics2D);
			}
			finally
			{
				//needed for correct drawing
				drd.tempRemoveFromParent(c);//remove imm.

				//correct translate back
				graphics2D.translate(-X, -Y);

				//restore
				if (didClip) graphics2D.setClip(clip);
			}
		}
	}

	private boolean normalPanelsAdded = false;

	boolean areNormalPanelsAdded()
	{
		return normalPanelsAdded;
	}

	// see if panel can be added aside or it will be added below; we must recalculate the panel's height
	// for each of these scenarios, because the location of the panel may determine certain "grow" width and height
	// fields to be cut by the page margins - and then they grow downwards to show their data 
	void seeWhereThePanelWillBeAdded(DataRendererDefinition panel)
	{
		if (nextXPosition != 0)
		{
			// see if it fits aside; if it does then add it and compute new nextXPosition/skipNextYPosition;
			// if it doesn't fit aside calculate the next x=0 location where it will be added
			Dimension panelSize = panel.getSize();

			if (nextXPosition + panelSize.width > size.width)
			{
				// try to see if the width that is too large is not determined by growing fields;
				// maybe, knowing an available width, the panel will be able to change it's size to fit
				Dimension newSize = panel.computeNewWidthForXLocation(renderParent, nextXPosition, false);
				if ((nextXPosition + newSize.width > size.width) || (newSize.height > heightLeft))
				{
					// it really does not fit aside... so move it below
					nextXPosition = 0;
					nextYPosition += skipOnY;
					heightLeft -= skipOnY;
					skipOnY = 0;
				}
				else
				{
					// will be put aside; so tell the panel to use the new size
					panel.computeNewWidthForXLocation(renderParent, nextXPosition, true);
				}
			} // else it fits without trying to resize it
		}

		if (nextXPosition == 0)
		{
			// it will be added below the currently used position (it was not added aside...)
			// calculate it's height & more (manage changes produced by growing field limitation to page width) 
			panel.computeNewWidthForXLocation(renderParent, 0, true);
		}
	}

	void addPanel(DataRendererDefinition panel)
	{
		normalPanelsAdded = true;
		panels.add(insetIndex, panel);
		insetIndex++;

		panel.setXlocation(nextXPosition);
		panel.setYlocation(nextYPosition);

		// calculate the coordinates where the next panel should be added aside (if it fits)
		Dimension panelSize = panel.getSize();
		nextXPosition += panelSize.width;
		skipOnY = Math.max(skipOnY, panelSize.height);
	}

	private boolean hasHeader = false;

	void addHeaderPanel(DataRendererDefinition panel)
	{
		panels.add(0, panel);
		heightLeft -= panel.getSize().height;
		panel.setYlocation(nextYPosition);
		nextYPosition += panel.getSize().height;
		hasHeader = true;
		insetIndex++;
	}

	void addFooterPanel(DataRendererDefinition panel)
	{
		panels.add(panel);
		heightLeft -= panel.getSize().height;
		panel.setYlocation(size.height - panel.getSize().height);
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("----start page---- "); //$NON-NLS-1$
		sb.append("[" + PersistHelper.createDimensionString(size) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("\n"); //$NON-NLS-1$
		for (int i = 0; i < panels.size(); i++)
		{
			sb.append(panels.get(i));
			sb.append("\n"); //$NON-NLS-1$
		}
		sb.append("----end page----\n"); //$NON-NLS-1$
		return sb.toString();
	}

	public void toXML(Writer w) throws IOException
	{
		w.write("<PAGE>"); //$NON-NLS-1$
		w.write("<PARTS count=\"" + panels.size() + "\" >"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < panels.size(); i++)
		{
			DataRendererDefinition drd = panels.get(i);
			IRecordInternal state = drd.getState();
			if (state instanceof PageNumberState)
			{
				((PageNumberState)state).initPagePositionAndSize(this);
			}
			DataRenderer partpane = drd.getDataRenderer();
			partpane.getDataAdapterList().setRecord(state, true); //fill with data
			drd.toXML(w);
		}
		w.write("</PARTS>"); //$NON-NLS-1$
		w.write("</PAGE>"); //$NON-NLS-1$
	}

	List<DataRendererDefinition> getPanels()
	{
		return panels;
	}

	void handleSink()
	{
		heightLeft -= skipOnY;
		if (heightLeft > 0)
		{
			for (int i = panels.size() - 1; i >= 0; i--)
			{
				DataRendererDefinition drd = panels.get(i);
				Part part = drd.getPart();
				if (part != null)
				{
					if (part.getPartType() == Part.FOOTER || part.getPartType() == Part.TITLE_FOOTER)
					{
						continue;
					}
					if (part.getSinkWhenLast())
					{
						drd.setYlocation(drd.getYlocation() + heightLeft); //sink
						heightLeft = 0;
					}
				}
				break;
			}
		}
	}
}
