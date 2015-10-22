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


import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.SafeArrayList;

/**
 * The object holding all the pages and being pageble
 * 
 * @author jblok
 */
public class PageList implements Pageable
{
	private final IApplication application;
	private final List pages;
	private final DataRendererDefinition[] dataRendererDefinitions;//default static renderers for all pages
	private PageDefinition currentPageDefinition;
	private final Dimension size;
	private final IPrintInfo printInfo;
	private final RendererParentWrapper renderParent;
	private final Map occurrenceBreak; //part->count
	private boolean restartPageNumbers;
	private int normalPageBodyAreaHeight;

	public PageList(IApplication app, IPrintInfo pp, RendererParentWrapper renderParent)
	{
		application = app;
		printInfo = pp;
		pages = new ArrayList();
		size = new Dimension((int)(printInfo.getPageFormat().getImageableWidth() * (1 / printInfo.getZoomFactor())),
			(int)(printInfo.getPageFormat().getImageableHeight() * (1 / printInfo.getZoomFactor())));
		normalPageBodyAreaHeight = size.height;

		dataRendererDefinitions = new DataRendererDefinition[11];
		this.renderParent = renderParent;
		occurrenceBreak = new HashMap();
	}

	public void setNonRepeatingPart(int type, DataRendererDefinition rendererDef)
	{
		if (rendererDef.getPart() != null && rendererDef.getPart().getPartType() != Part.BODY)
		{
			rendererDef.setFixedSize(new Dimension(size.width, rendererDef.getSize().height)); //set to page width for background and such
		}

		dataRendererDefinitions[type] = rendererDef;

		// update normalPageBodyAreaHeight if necessary
		if (type == Part.HEADER)
		{
			normalPageBodyAreaHeight = size.height - rendererDef.getSize().height;
			if (dataRendererDefinitions[Part.FOOTER] != null)
			{
				normalPageBodyAreaHeight -= dataRendererDefinitions[Part.FOOTER].getSize().getHeight();
			}
		}
		else if (type == Part.FOOTER)
		{
			normalPageBodyAreaHeight = size.height - rendererDef.getSize().height;
			if (dataRendererDefinitions[Part.HEADER] != null)
			{
				normalPageBodyAreaHeight -= dataRendererDefinitions[Part.HEADER].getSize().getHeight();
			}
		}
	}

	//pageble implementation
	public PageFormat getPageFormat(int pageIndex)
	{
		return printInfo.getPageFormat();
	}

	public Printable getPrintable(int pageIndex)
	{
		return getPage(pageIndex);
	}

	public int getPageSectionLength(PageDefinition pd)
	{
		int idx = pages.indexOf(pd);

		// find the section of the given page
		int previousRestartIndex = 0;
		int nextRestartIndex = pages.size();
		for (int i = idx; i > previousRestartIndex; i--)
		{
			Integer in = (Integer)restartList.get(i);
			if (in != null)
			{
				previousRestartIndex = i;
			}
		}
		for (int i = idx + 1; i < nextRestartIndex; i++)
		{
			Integer in = (Integer)restartList.get(i);
			if (in != null)
			{
				nextRestartIndex = i;
			}
		}
		return nextRestartIndex - previousRestartIndex;
	}

	public int getNumberOfPages()
	{
		return pages.size();
	}

	public int getPageNumber(PageDefinition pd)
	{
		int idx = pages.indexOf(pd);
		for (int i = idx; i >= 0; i--)
		{
			Integer in = (Integer)restartList.get(i);
			if (in != null)
			{
				return (idx - i) + 1;
			}
		}
		return idx + 1;
	}

	private final List restartList = new SafeArrayList();

	public void restartPageNumber(PageDefinition pd)
	{
		restartList.set(pages.indexOf(pd), new Integer(0));
	}

	public PageDefinition getPage(int p)
	{
		if (pages.size() != 0)
		{
			return (PageDefinition)pages.get(p);
		}
		else
		{
			return null;
		}
	}

	//do smart adding to corect page(s) etc
	public void addPanel(DataRendererDefinition rendererDef)
	{
		addPanel(rendererDef, true);
	}

	public void addPanel(DataRendererDefinition rendererDef, boolean firstPageOfRenderer)
	{
		if (currentPageDefinition == null) createNewPageDefinition();

		if (rendererDef.getDataRenderer() == null) return;//is empty body

		if (rendererDef.getPart() != null && rendererDef.getPart().getPartType() != Part.BODY)
		{
			rendererDef.setFixedSize(new Dimension(size.width, rendererDef.getSize().height)); //set to page width for background and such
		}

		if ((currentPageDefinition.areNormalPanelsAdded() || pages.size() == 0) && rendererDef.getPageBreakBefore()) createNewPageDefinition();
		// first page could leave very little space left after adding header/titleheader/footer/grandLeadingSummary so allow it to show even without normal panel added

		currentPageDefinition.seeWhereThePanelWillBeAdded(rendererDef);
		int left = currentPageDefinition.getHeightLeft();
		int panelHeigth = rendererDef.getFullSize().height;
		if (left > 5 && (panelHeigth <= left || rendererDef.getAllowBreakAcrossPageBounds()))//check if leftover to big enough
		{
			DataRendererDefinition rendererDefClone = null;
			int oldYOrgin = rendererDef.getStartYOrgin();
			if (panelHeigth - oldYOrgin > left)
			{
				if (!rendererDef.getDiscardRemainderAfterBreak())
				{
					try
					{
						rendererDefClone = (DataRendererDefinition)rendererDef.clone();
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}

				int newLeft = rendererDef.getPreferedBreak(renderParent, left, normalPageBodyAreaHeight);
				if (newLeft <= 0)
				{
					Debug.warn("No nice page break location found so as not to split form components in two..."); //$NON-NLS-1$
				}
				else
				{
					left = newLeft;
				}
				rendererDef.setFixedSize(new Dimension(rendererDef.getSize().width, left));
			}

			currentPageDefinition.addPanel(rendererDef);
			if (!restartPageNumbers && firstPageOfRenderer && rendererDef.getRestartPageNumber())
			{
				restartPageNumbers = true; // after all panels are added to a page definition, we will remember if that page resets the page numbers or not
			}

			if ((panelHeigth - oldYOrgin) > left)
			{
				createNewPageDefinition();

				if (rendererDefClone != null)
				{
					int newStartYOrgin = oldYOrgin + left;
					rendererDefClone.setStartYOrgin(newStartYOrgin);
					rendererDefClone.setFixedSize(new Dimension(rendererDef.getSize().width, panelHeigth - newStartYOrgin));
					addPanel(rendererDefClone, false);
				}
			}
			else
			{
				doOccurrenceBreak(rendererDef);
			}
		}
		else
		{
			// first page could leave very little space left after adding header/titleheader/footer/grandLeadingSummary so allow it to show even without normal panel added (else we might force this panel in a few pixels of space just so that we have normal panels added...)
			if (currentPageDefinition.areNormalPanelsAdded() || pages.size() == 0)
			{
				createNewPageDefinition();
			}
			if (currentPageDefinition.getHeightLeft() > 5 &&
				(panelHeigth <= currentPageDefinition.getHeightLeft() || rendererDef.getAllowBreakAcrossPageBounds())) // check this in order to avoid stack overflow
			{
				// normal case
				addPanel(rendererDef);
			}
			else
			{
				// not normal case; page height is < 5 pixels? strange
				currentPageDefinition.addPanel(rendererDef);
				if (!restartPageNumbers && firstPageOfRenderer && rendererDef.getRestartPageNumber())
				{
					restartPageNumbers = true; // after all panels are added to a page definition, we will remember if that page resets the page numbers or not
				}
			}
			doOccurrenceBreak(rendererDef);
		}
	}

	private void doOccurrenceBreak(DataRendererDefinition rendererDef)
	{
		//do page break occurence
		int pbo = rendererDef.getPageBreakAfterOccurrence();
		if (pbo > 0)
		{
			Part part = rendererDef.getPart();
			if (part != null)
			{
				Integer count = (Integer)occurrenceBreak.get(part);
				if (count == null) count = new Integer(1);//already one is added

				if (count.intValue() == pbo)
				{
					createNewPageDefinition();
					count = new Integer(0);
				}
				occurrenceBreak.put(part, new Integer(count.intValue() + 1));
			}
		}
	}

	//create a new page well initialized with static partrenders
	private void createNewPageDefinition()
	{
		if (currentPageDefinition != null)
		{
			addCurrentPageDefinition();
			currentPageDefinition = null;
		}
		if (currentPageDefinition == null)
		{
			try
			{
				currentPageDefinition = new PageDefinition(application, printInfo, renderParent, size);
//				Debug.trace("Creating page "+pages.size());
				if (pages.size() == 0)
				{
					if (dataRendererDefinitions[Part.TITLE_HEADER] != null)
					{
						currentPageDefinition.addHeaderPanel(dataRendererDefinitions[Part.TITLE_HEADER]);
					}
					else
					{
						if (dataRendererDefinitions[Part.HEADER] != null) currentPageDefinition.addHeaderPanel((DataRendererDefinition)dataRendererDefinitions[Part.HEADER].clone());
					}

					if (dataRendererDefinitions[Part.TITLE_FOOTER] != null)
					{
						currentPageDefinition.addFooterPanel(dataRendererDefinitions[Part.TITLE_FOOTER]);
					}
					else
					{
						if (dataRendererDefinitions[Part.FOOTER] != null) currentPageDefinition.addFooterPanel((DataRendererDefinition)dataRendererDefinitions[Part.FOOTER].clone());
					}

					if (dataRendererDefinitions[Part.LEADING_GRAND_SUMMARY] != null)
					{
						addPanel(dataRendererDefinitions[Part.LEADING_GRAND_SUMMARY]); // allow it to fill more pages if needed; add it after header/footer as it might create new pages itself and the current page def. should be ready for that
					}

					if (pages.size() == 0 && // this means that LEADING_GRAND_SUMMARY didn't occupy more then one page
						((dataRendererDefinitions[Part.TITLE_HEADER] != null && dataRendererDefinitions[Part.TITLE_HEADER].getPageBreakAfterOccurrence() == 1) || (dataRendererDefinitions[Part.TITLE_FOOTER] != null && dataRendererDefinitions[Part.TITLE_FOOTER].getPageBreakAfterOccurrence() == 1)))
					{
						// first page is not meant to contain any more data in this case...
						createNewPageDefinition();
					}
				}
				else
				{
					if (dataRendererDefinitions[Part.HEADER] != null) currentPageDefinition.addHeaderPanel((DataRendererDefinition)dataRendererDefinitions[Part.HEADER].clone());
					if (dataRendererDefinitions[Part.FOOTER] != null) currentPageDefinition.addFooterPanel((DataRendererDefinition)dataRendererDefinitions[Part.FOOTER].clone());
				}

			}
			catch (CloneNotSupportedException e)
			{
				Debug.error(e);
			}
		}
	}

	//must be called after being done with processing chain
	public void finish()
	{
		if (currentPageDefinition == null) createNewPageDefinition();
		if (dataRendererDefinitions[Part.TRAILING_GRAND_SUMMARY] != null)
		{
			addPanel(dataRendererDefinitions[Part.TRAILING_GRAND_SUMMARY]);
		}
		if (currentPageDefinition.areNormalPanelsAdded())
		{
			addCurrentPageDefinition();
		}
		currentPageDefinition = null;

		for (int i = 0; i < pages.size(); i++)
		{
			((PageDefinition)pages.get(i)).handleSink();
		}
	}

	private void addCurrentPageDefinition()
	{
		pages.add(currentPageDefinition);
		// not see it it has any "reset page numbers" part
		if (restartPageNumbers)
		{
			restartPageNumber(currentPageDefinition);
			restartPageNumbers = false;
		}
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("----#start list#----\n"); //$NON-NLS-1$
		for (int i = 0; i < pages.size(); i++)
		{
			sb.append("\n"); //$NON-NLS-1$
			sb.append("page "); //$NON-NLS-1$
			sb.append(i);
			sb.append("\n"); //$NON-NLS-1$
			sb.append(pages.get(i));
		}
		sb.append("----#end list#----\n"); //$NON-NLS-1$
		return sb.toString();
	}

	public void toXML(Writer w) throws IOException
	{
		w.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"); //$NON-NLS-1$
		w.write("<SERVOYREPORT version=\"1\" >"); //$NON-NLS-1$
		w.write("<PAGES count=\"" + pages.size() + "\" >"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 0; i < pages.size(); i++)
		{
			((PageDefinition)pages.get(i)).toXML(w);
		}
		w.write("</PAGES>"); //$NON-NLS-1$
		w.write("</SERVOYREPORT>"); //$NON-NLS-1$
	}

}
