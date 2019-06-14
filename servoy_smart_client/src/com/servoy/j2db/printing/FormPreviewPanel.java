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
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.smart.dataui.DataRenderer;
import com.servoy.j2db.smart.dataui.DataRendererFactory;
import com.servoy.j2db.ui.PropertyCopy;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.RendererParentWrapper;

/**
 * this class renders the pages in the printpreview and suplies pageble for real printing
 *
 * @author jblok
 */
public class FormPreviewPanel extends JPanel implements IPrintInfo
{
	private final IApplication application;
	private final FormController controllerBeingPreviewed;
	private IFoundSetInternal formData;//NOTE this can be different than form model from formBeingPreviewed

	private Map<Part, DataRenderer> part_panels;
	private PageFormat currentPageFormat;

	private static float zoomFactor;

	private int pageNumber;//current page number
	private final double factor;//scale factor (based on form printscale)
	private Dimension orgWidth;

	//the list of all pages
	private PageList plist;
	//root of processing tree, a chain is builded and processed to add (in nested loops) all renderes to pages
	private PartNode root;
	private RendererParentWrapper renderParent;

	public FormPreviewPanel(IApplication app, FormController formController, IFoundSetInternal formData)
	{
		super(false);
		application = app;
		controllerBeingPreviewed = formController;
		this.formData = formData;

		currentPageFormat = controllerBeingPreviewed.getPageFormat();
		if (currentPageFormat == null)
		{
			currentPageFormat = application.getPageFormat();
		}

		factor = 100d / controllerBeingPreviewed.getForm().getPaperPrintScale();
		zoomFactor = (float)factor;

		setOpaque(true);
		setBackground(Color.white);

		//set size of this panel
		orgWidth = new Dimension((int)(currentPageFormat.getWidth() * (1 / factor)), (int)(currentPageFormat.getHeight() * (1 / factor)));
		applySize();

		setBorder(BorderFactory.createMatteBorder(1, 1, 2, 2, Color.black));
		setLayout(null);
	}

	public void destroy()
	{
		formData = null;
		root = null;
		if (part_panels != null)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			Iterator<DataRenderer> it = part_panels.values().iterator();
			while (it.hasNext())
			{
				it.next().notifyVisible(false, invokeLaterRunnables);
			}
			//we ignore invokeLaterRunnables since we are in printing
			it = part_panels.values().iterator();
			while (it.hasNext())
			{
				it.next().destroy();
			}
			part_panels = null;
		}
		if (renderParent != null) renderParent.destroy();
	}

	//build the chain and fill the renderers,returns number of pages
	public int process() throws Exception
	{
		//clear
		root = null;

		//set size of this panel
		orgWidth = new Dimension((int)(currentPageFormat.getWidth() * (1 / factor)), (int)(currentPageFormat.getHeight() * (1 / factor)));
		applySize();

		part_panels = createPartPanels();
		Form form = controllerBeingPreviewed.getForm();
		int w = form.getWidth();//otherwise you cannot print multiple columns   (int) (application.getPageFormat().getImageableWidth()*(1/factor));
		try
		{
			application.getRuntimeProperties().put("isPrinting", Boolean.TRUE); //$NON-NLS-1$

			Map componentsUsingSliding = application.getDataRenderFactory().completeRenderers(application, form, controllerBeingPreviewed.getScriptExecuter(),
				part_panels, w, true, null, null);
			PropertyCopy.copyExistingPrintableProperties(application, controllerBeingPreviewed, part_panels);
			Iterator<DataRenderer> panels = part_panels.values().iterator();
			while (panels.hasNext())
			{
				DataRenderer panel = panels.next();
				panel.setComponentsUsingSliding(componentsUsingSliding);
				DataRendererFactory.addSpringsBetweenComponents(application, panel);
			}

			Debug.trace("usesSliding " + (componentsUsingSliding.size() != 0)); //$NON-NLS-1$
		}
		finally
		{
			application.getRuntimeProperties().put("isPrinting", null); //$NON-NLS-1$

		}

		//create list
		renderParent = application.getPrintingRendererParent();
		plist = new PageList(application, this, renderParent);

		PartNode node = null;

		//create the chain based on the sort,LAST node must be the body part (is virtal added if not present)
		Part body = null;
		FormController fp = ((FormManager)application.getFormManager()).leaseFormPanel(controllerBeingPreviewed.getName());
		if (fp != null && !fp.isShowingData())
		{
//			List lst = fp.getFormModel().getLastSearchColumns();
			if (fp.wantEmptyFoundSet())
			{
				if (fp.getFormModel() != null) fp.getFormModel().clear();
			}
			else
			{
				fp.loadAllRecords();
			}
//			fp.getFormModel().sort(lst);
		}
		List<SortColumn> sortColumns = ((FoundSet)formData).getLastSortColumns();
		if (formData.getSize() != 0)
		{
			if (sortColumns != null)
			{
				Set<String> consumed = new HashSet<String>();
				for (int i = 0; i < sortColumns.size(); i++)
				{
					SortColumn sc = sortColumns.get(i);
					Iterator<Part> it = part_panels.keySet().iterator();
					while (it.hasNext())
					{
						Part part = it.next();
						DataRenderer dr = part_panels.get(part);
						if (part.getPartType() == Part.BODY)
						{
							body = part;
							continue;
						}
						if (part.getPartType() != Part.LEADING_SUBSUMMARY && part.getPartType() != Part.TRAILING_SUBSUMMARY)
						{
							IRecordInternal state = new PageNumberState(formData, plist);
							plist.setNonRepeatingPart(part.getPartType(), new DataRendererDefinition(this, renderParent, part, dr, state));
							continue;
						}

						boolean match = false;
						int inlineCount = 0;
						List<SortColumn> partSortColumns = new ArrayList<SortColumn>();
						SortColumn lastMatch = sc;
						String groupByDataproviders = part.getGroupbyDataProviderIDs() != null ? part.getGroupbyDataProviderIDs() : "";
						StringTokenizer tk = new StringTokenizer("" + groupByDataproviders.toLowerCase(), ", "); //$NON-NLS-1$ //$NON-NLS-2$
						int tokenCount = tk.countTokens();
						String[] ids = new String[tokenCount];
						for (; inlineCount < tokenCount; inlineCount++)
						{
							String id = tk.nextToken();
							ids[inlineCount] = id;
							if (lastMatch.getDataProviderID().equals(id))
							{
								partSortColumns.add(lastMatch);
								if ((i + inlineCount + 1) < sortColumns.size())
								{
									lastMatch = sortColumns.get(i + inlineCount + 1);
									if (part.getPartType() == Part.LEADING_SUBSUMMARY && consumed.contains(lastMatch))
									{
										break;
									}
								}
								else
								{
									break;
								}
							}
							else
							{
								break;
							}
						}
						if (tokenCount > 0 && partSortColumns.size() == tokenCount) //did all match?
						{
							match = true;
							if (part.getPartType() == Part.LEADING_SUBSUMMARY)
							{
								for (String element : ids)
								{
									consumed.add(element);
								}
							}
						}
						if (match)
						{
							SortColumn[] array = new SortColumn[partSortColumns.size()];
							partSortColumns.toArray(array);

							if (root == null)//create root
							{
								root = new PartNode(this, part, dr, renderParent, array);
								node = root;
							}
							else
							{
								if (!tryToPlaceInExistingNodes(part, dr, array))
								{
									PartNode newNode = new PartNode(this, part, dr, renderParent, array);
									node.setChild(newNode);
									node = newNode;
								}
							}
						}
					}
				}

				PartNode newNode = null;
				if (body == null)
				{
					newNode = new PartNode(this, null, null, renderParent, null);//a virtual body (when no body is placed in the parts)
				}
				else
				{
					newNode = new PartNode(this, body, part_panels.get(body), renderParent, null);//the body
				}
				if (node != null)
				{
					node.setChild(newNode);
				}
				else
				{
					root = newNode;
				}
			}
			else
			//no sort...
			{
				if (body == null)//search for body
				{
					Iterator<Part> it = part_panels.keySet().iterator();
					while (it.hasNext())
					{
						Part part = it.next();
						DataRenderer dr = part_panels.get(part);
						IRecordInternal state = new PageNumberState(formData, plist);
						if (part.getPartType() == Part.BODY)
						{
							body = part;
							continue;
						}
						if (part.getPartType() != Part.LEADING_SUBSUMMARY && part.getPartType() != Part.TRAILING_SUBSUMMARY)
						{
							plist.setNonRepeatingPart(part.getPartType(), new DataRendererDefinition(this, renderParent, part, dr, state));
							continue;
						}
					}
				}
				if (body == null)
				{
					root = new PartNode(this, null, null, renderParent, null);//a virtual body (when no body is placed in the parts)
				}
				else
				//if (body != null)
				{
					root = new PartNode(this, body, part_panels.get(body), renderParent, null);//the body
				}
			}
		}

		try
		{
			application.getRuntimeProperties().put("isPrinting", Boolean.TRUE); //$NON-NLS-1$

			long t1 = System.currentTimeMillis();
			//fill the renderers with data
			if (root != null)
			{
				//dump chain
				Debug.trace("Root " + root); //$NON-NLS-1$

				QuerySelect sqlString = ((FoundSet)formData).getQuerySelectForReading();
				Table table = formData.getSQLSheet().getTable();

				FoundSet fs = (FoundSet)((FoundSetManager)application.getFoundSetManager()).getNewFoundSet(table, null, sortColumns);

				fs.browseAll(sqlString);
				long t3 = System.currentTimeMillis();
				List<DataRendererDefinition> childRetval = root.process(this, fs, table, sqlString);
				long t4 = System.currentTimeMillis();
				if (Debug.tracing())
				{
					Debug.trace("Database queries took " + ((t4 - t3) / 1000f) + " second"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (childRetval != null)
				{
					for (int i = 0; i < childRetval.size(); i++)
					{
						plist.addPanel(childRetval.get(i));
					}
				}
			}
			plist.finish();
			long t2 = System.currentTimeMillis();
			int pageCount = plist.getNumberOfPages();
			//dump
			if (Debug.tracing())
			{
				Debug.trace(plist);
				Debug.trace("Generated " + pageCount / ((t2 - t1) / 1000f) + " printable pages per second"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		finally
		{
			application.getRuntimeProperties().put("isPrinting", null); //$NON-NLS-1$
		}

		renderParent.removeAll();

		return plist.getNumberOfPages();
	}

	private boolean tryToPlaceInExistingNodes(Part p, DataRenderer r, SortColumn[] scs)
	{
		PartNode node = root;
		while (node != null)
		{
			if (p.getPartType() == Part.TRAILING_SUBSUMMARY && node.isLeading() && Arrays.equals(node.getSortColumns(), scs))
			{
				try
				{
					node.setSecondPartAsTrailingRenderer(p, r);
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
				return true;
			}
			node = node.getChild();
		}
		return false;
	}

	private Map<Part, DataRenderer> createPartPanels() throws RepositoryException
	{
		try
		{
			application.getRuntimeProperties().put("isPrinting", Boolean.TRUE); //$NON-NLS-1$

			part_panels = new LinkedHashMap<Part, DataRenderer>();
			Iterator<Part> it = controllerBeingPreviewed.getForm().getParts();
			while (it.hasNext())
			{
				Part part = it.next();
				// printing is always swing
				DataRenderer partpane = (DataRenderer)application.getDataRenderFactory().getEmptyDataRenderer(
					ComponentFactory.getWebID(controllerBeingPreviewed.getForm(), part), part.toString(), application, false);
				part_panels.put(part, partpane);
			}
			return part_panels;
		}
		finally
		{
			application.getRuntimeProperties().put("isPrinting", null); //$NON-NLS-1$

		}
	}

	public IApplication getApplication()
	{
		return application;
	}

	public void showPage(int page)
	{
		this.pageNumber = page;

		applySize();

		revalidate();
		application.invokeLater(new Runnable()
		{
			public void run()
			{
				repaint();
			}
		});
	}

	public void zoom(float a_zoomFactor)
	{
		zoomFactor = a_zoomFactor;

		applySize();

		if (getParent() != null)
		{
			invalidate();
			getParent().validate();
		}
	}

	private void applySize()
	{
		Dimension d = new Dimension(orgWidth);
		d.width *= zoomFactor;
		d.height *= zoomFactor;
		setPreferredSize(d);
		setSize(d);
	}

	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		AffineTransform save = ((Graphics2D)g).getTransform();
		AffineTransform at = (AffineTransform)save.clone();
		try
		{
			at.scale(zoomFactor, zoomFactor);//set zoom
			((Graphics2D)g).setTransform(at);

			drawMargins(g, currentPageFormat);
			if (plist != null)
			{
				PageDefinition page = plist.getPage(pageNumber);
				if (page != null) page.printPage(g, currentPageFormat);
			}
		}
		finally
		{
			((Graphics2D)g).setTransform(save);
			if (renderParent != null) renderParent.removeAll();
		}
	}

	public Pageable getPageable()
	{
		return plist;
	}

	public PageFormat getPageFormat()
	{
		return currentPageFormat;
	}

	public void setPageFormat(PageFormat pf)
	{
		currentPageFormat = pf;
		// push this new format to the controller so that the user does not have to
		// overrule the page format again for this form in this ssession.
		controllerBeingPreviewed.setPageFormat(pf);
	}

	public double getZoomFactor()
	{
		return factor;
	}

	private void drawMargins(Graphics g, PageFormat pf)
	{
		double imx = pf.getImageableX() * (1 / factor);
		double imy = pf.getImageableY() * (1 / factor);
		double imw = pf.getImageableWidth() * (1 / factor);
		double imh = pf.getImageableHeight() * (1 / factor);
		double h = pf.getHeight() * (1 / factor);
		double w = pf.getWidth() * (1 / factor);

		g.setColor(Color.lightGray);

		g.drawLine((int)imx - 1, 0, (int)imx - 1, (int)h); //left margin
		g.drawLine((int)Math.round(imx + imw), 0, (int)Math.round(imx + imw), (int)h); //rigth margin

		g.drawLine(0, (int)imy - 1, (int)w, (int)imy - 1); //top margin
		g.drawLine(0, (int)Math.round(imy + imh), (int)w, (int)Math.round(imy + imh)); //bottom margin
	}

	/**
	 *
	 */
	public void flushCachedData()//removePrintedStatesFromFoundSets()
	{
		if (root != null) root.removePrintedStatesFromFoundSets();
	}
}
