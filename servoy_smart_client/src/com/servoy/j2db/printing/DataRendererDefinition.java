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


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.ISupportPrintSliding;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.smart.dataui.DataRenderer;
import com.servoy.j2db.smart.dataui.PortalComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.Utils;

/**
 * This class tays together the renderer the data and the part in one enity which can be rendered onto a page(defination)
 * 
 * @author jblok
 */
public class DataRendererDefinition implements Cloneable //cloneable for page border break
{
	private Part part;
	private final DataRenderer renderer;
	private int startYOrgin;
	private Dimension fixedSize;
	private Dimension fullSize;
	private int Ylocation;
	private int Xlocation;
	private final Map bounds;

	private IRecordInternal state;

	private IFoundSetInternal set;
	private int index;

	private int maxPageWidth = Integer.MAX_VALUE;
	private final IApplication application;

	DataRendererDefinition(IPrintInfo pi, RendererParentWrapper renderParent, Part p, DataRenderer r, IFoundSetInternal set, int index)
	{
		this(pi, renderParent, p, r);
		this.set = set;
		this.index = index;
		init(renderParent);
	}

	DataRendererDefinition(IPrintInfo pi, RendererParentWrapper renderParent, Part p, DataRenderer r, IRecordInternal s)
	{
		this(pi, renderParent, p, r);
		state = s;
		init(renderParent);
	}

	public DataRendererDefinition(IPrintInfo pi, RendererParentWrapper renderParent, DataRenderer r)
	{
		application = pi.getApplication();
		renderer = r;
		bounds = new HashMap();
		if (renderer != null)//renderer can be null if no body
		{
			List invokeLaterRunnables = new ArrayList();
			renderer.notifyVisible(true, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
		}
		startYOrgin = 0;
		init(renderParent);
	}

	private DataRendererDefinition(IPrintInfo pi, RendererParentWrapper renderParent, Part p, DataRenderer r)
	{
		maxPageWidth = (int)(pi.getPageFormat().getImageableWidth() * (1 / pi.getZoomFactor()));
		application = pi.getApplication();
		part = p;
		renderer = r;
		bounds = new HashMap();
		if (renderer != null)//renderer can be null if no body
		{
			List invokeLaterRunnables = new ArrayList();
			renderer.notifyVisible(true, invokeLaterRunnables);
			Utils.invokeLater(pi.getApplication(), invokeLaterRunnables);
		}
		startYOrgin = 0;
	}

	private void init(RendererParentWrapper renderParent)
	{
		if (renderer != null)//can be empty, simulating body part
		{
			boolean isUsingPrintSliding = renderer.isUsingSliding();
			boolean remainderIsLostAnyway = (part != null && part.getDiscardRemainderAfterBreak() && part.getPageBreakAfterOccurrence() == 1);
			if (remainderIsLostAnyway)
			{
				isUsingPrintSliding = false;//optimize
			}
			try
			{
				tempAddToParent(renderParent, isUsingPrintSliding, true, true);

				if (renderer == null)//can be empty, simulating body part
				{
					fullSize = new Dimension();
				}
				else
				{
					fullSize = renderer.getPreferredSize();
				}
			}
			finally
			{
				tempRemoveFromParent(renderParent);
			}
		}
	}

	Dimension computeNewWidthForXLocation(RendererParentWrapper renderParent, int xLocation, boolean saveNewLayout)
	{
		ComputeNewWidthForXLocationRunnable r = new ComputeNewWidthForXLocationRunnable(renderParent, xLocation, saveNewLayout);
		if (application.isEventDispatchThread())
		{
			r.run();
		}
		else
		{
			application.invokeAndWait(r);
		}
		return r.result;
	}

	class ComputeNewWidthForXLocationRunnable implements Runnable
	{

		private final int xLocation;
		private final boolean saveNewLayout;
		private Dimension result;
		private final RendererParentWrapper renderParent;

		public ComputeNewWidthForXLocationRunnable(RendererParentWrapper renderParent, int xLocation, boolean saveNewLayout)
		{
			this.renderParent = renderParent;
			this.xLocation = xLocation;
			this.saveNewLayout = saveNewLayout;
		}

		public void run()
		{
			result = computeNewWidthForXLocation();
		}

		public Dimension getResult()
		{
			return result;
		}

		private Dimension computeNewWidthForXLocation()
		{
			try
			{
				int slide;
				boolean mustLayoutAgain = false;
				ArrayList restrainedComponents = new ArrayList();

				// see if some components that grow in width need and can to be limited to the page's right margin;
				// if such components are found and they also
				// need to change their height due to this limitation, compute new size
				Map sliding = renderer.getComponentsUsingSliding();
				if (sliding == null) return fullSize; // cannot do needed work, so return current width
				Iterator slidingComponents = sliding.keySet().iterator();
				while (slidingComponents.hasNext())
				{
					Component component = (Component)slidingComponents.next();
					slide = ((Integer)sliding.get(component)).intValue();

					// set max border width, so fields right hand side do not grow out of page
					if ((slide & ISupportPrintSliding.GROW_WIDTH) == ISupportPrintSliding.GROW_WIDTH)
					{
						Rectangle r = (Rectangle)bounds.get(component);
						if (r == null) return fullSize; // cannot do needed work, so return current width
						r = new Rectangle(r);
						// check if the "right hand border" of fields allowed to grow in width are still within page margin
						int spaceToMargin = renderer.getSpaceToRightMargin();
						if (xLocation + r.x + r.width + spaceToMargin > maxPageWidth)
						{
							r.width = maxPageWidth - r.x - xLocation - spaceToMargin;
							if (r.width > 0) // make no sense to set negative size, component is already
							// moved off page 
							{
								if (saveNewLayout)
								{
									bounds.put(component, r); // for components that do not implement IFixedPreferredWidth
								}
								if (component instanceof IFixedPreferredWidth &&
									((slide & ISupportPrintSliding.GROW_HEIGHT) == ISupportPrintSliding.GROW_HEIGHT || (slide & ISupportPrintSliding.SHRINK_HEIGHT) == ISupportPrintSliding.SHRINK_HEIGHT))
								{
									// if the component implements this interface and is allowed to grow/shrink vertically, it means that it might
									// want to request a different height after it has been limited to fit in the page width
									((IFixedPreferredWidth)component).setPreferredWidth(r.width);
									restrainedComponents.add(component);
									mustLayoutAgain = true;
								}
							}
						}
					}
				} // end while

				if (mustLayoutAgain)
				{
					try
					{
						tempAddToParent(renderParent, true, true, saveNewLayout);
						Dimension newSize = renderer.getPreferredSize();
						if (saveNewLayout)
						{
							fullSize = newSize;
						}
						return newSize;
					}
					finally
					{
						tempRemoveFromParent(renderParent);
						for (int i = restrainedComponents.size() - 1; i >= 0; i--)
						{
							((IFixedPreferredWidth)restrainedComponents.get(i)).setPreferredWidth(-1);
						}
					}
				}
				else
				{
					return fullSize; // the size does not change if the page is added at the specified X location
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				if (fullSize != null) return fullSize;
				return new Dimension(0, 0);
			}
		}

	}

	void tempAddToParent(final RendererParentWrapper renderParent, final boolean fillWithData, final boolean doLayout, final boolean saveComponentBounds)
	{
		try
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					try
					{
						if (!doLayout)
						{
							//restore the bound to match exactly to the init phase
							//(and no fields are cut in half due to different html render positions)
							restoreSavedComponentBounds();
						}

						//data is needed to see how (much) to slide
						if (fillWithData)
						{
							IRecordInternal a_state = getState();
							//lookup all field needed, so the are present in the state when needed for rendering
							if (a_state != null) renderer.getDataAdapterList().setRecord(a_state, true);
						}

						//always needed for correct layout of text-areas/mediafields
						renderParent.add(renderer);

						List<Component> invisibleComponents = new ArrayList<Component>();
						Component[] comps = renderer.getComponents();
						for (int i = 0; i < comps.length; i++)
						{
							int slide = ISupportPrintSliding.NO_SLIDING;
							Map componentsListToBeHandeld = renderer.getComponentsUsingSliding();
							if (componentsListToBeHandeld != null && componentsListToBeHandeld.containsKey(comps[i]))
							{
								slide = ((Integer)componentsListToBeHandeld.get(comps[i])).intValue();
							}

							//set components invisible if empty/null and should shrink
							if (((slide & ISupportPrintSliding.SHRINK_HEIGHT) == ISupportPrintSliding.SHRINK_HEIGHT) &&
								((slide & ISupportPrintSliding.SHRINK_WIDTH) == ISupportPrintSliding.SHRINK_WIDTH) && comps[i] instanceof IDisplayData)
							{
								boolean visible = !(((IDisplayData)comps[i]).getValueObject() == null || ((IDisplayData)comps[i]).getValueObject().toString().trim().length() == 0);
								comps[i].setVisible(visible);
								if (!visible)
								{
									comps[i].setSize(0, 0);
									invisibleComponents.add(comps[i]);
								}
							}
						}
						if (doLayout)
						{
							renderer.invalidate();
							renderer.validate();

							for (Component invisibleComponent : invisibleComponents)
							{
								invisibleComponent.setPreferredSize(new Dimension(0, 0));
							}

							//1) do first
							renderer.doLayout();

							//2) do second time, some times the after the fist time something is changed which causes stuff to render correctly
							renderer.doLayout();

							if (saveComponentBounds)
							{
								//store the prefered sizes so we don't have todo layouting again (by adding / removing)
								for (Component element : comps)
								{
									bounds.put(element, new Rectangle(element.getBounds()));
								}
							}

							for (Component invisibleComponent : invisibleComponents)
							{
								invisibleComponent.setPreferredSize(null);
							}
						}
						else
						{
							//for bounds change, important for html/text-areas
							Iterator it = bounds.keySet().iterator();
							while (it.hasNext())
							{
								Component c = (Component)it.next();
								c.validate();//relayouts the internals of html/text-areas
							}
						}
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
				}

			};
			if (application.isEventDispatchThread())
			{
				r.run();
			}
			else
			{
				application.invokeAndWait(r);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	void restoreSavedComponentBounds()
	{
		Iterator it = bounds.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry elem = (Map.Entry)it.next();
			Component c = (Component)elem.getKey();
			c.setBounds((Rectangle)elem.getValue());
		}
	}

	void tempRemoveFromParent(final RendererParentWrapper renderParent)
	{
		try
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					//needed for correct drawing
					renderParent.remove(renderer);

					//clean panel
					renderParent.invalidate();
				}
			};
			if (application.isEventDispatchThread())
			{
				r.run();
			}
			else
			{
				application.invokeAndWait(r);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	public DataRenderer getDataRenderer()
	{
		return renderer;
	}

	public IRecordInternal getState()
	{
		if (state == null)
		{
			if (set != null)
			{
				return set.getRecord(index);
			}
			return null;
		}
		else
		{
			return state;
		}
	}

	/**
	 * Gets the size.
	 * 
	 * @return Returns a Dimension
	 */
	public Dimension getFullSize()
	{
		return fullSize;
	}

	/**
	 * Gets the size.
	 * 
	 * @return Returns a Dimension
	 */
	public Dimension getSize()
	{
		if (fixedSize == null)
		{
			return fullSize;
		}
		else
		{
			return fixedSize;
		}
	}

	/**
	 * Sets the size. used for breaking parts
	 * 
	 * @param size The size to set
	 */
	public void setFixedSize(Dimension size)
	{
		this.fixedSize = size;
	}

	/**
	 * Gets the startYOrgin,which is the starting Y draw location for the internal renderer
	 */
	public int getStartYOrgin()
	{
		return startYOrgin;
	}

	public void setStartYOrgin(int startYOrgin)
	{
		this.startYOrgin = startYOrgin;
	}

	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	@Override
	public String toString()
	{
		return "prefsize: [" + PersistHelper.createDimensionString(fullSize) + "] size: [" + PersistHelper.createDimensionString(fixedSize) + "] Xlocation: " + Xlocation + " Ylocation: " + Ylocation + " startYOrgin: " + startYOrgin + " type: " + (part == null ? "" : part.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	}

	public void toXML(Writer w) throws IOException
	{
		if (part != null && getStartYOrgin() == 0)//t oprevent duplicates in normal printing not seen due to clipping
		{
			w.write("<PART type=\"" + Part.getDisplayName(part.getPartType()) + "\" x=\"" + getXlocation() + "\" y=\"" + getYlocation() + "\" width=\"" + getSize().width + "\" height=\"" + getSize().height + "\" >"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$//$NON-NLS-6$
			Iterator it = bounds.keySet().iterator();
			while (it.hasNext())
			{
				Component element = (Component)it.next();
				Rectangle rec = (Rectangle)bounds.get(element);
				XMLPrintHelper.handleComponent(w, element, rec, null);
			}
			w.write("</PART>"); //$NON-NLS-1$
		}
	}


	public boolean getAllowBreakAcrossPageBounds()
	{
		if (part != null)
		{
			return part.getAllowBreakAcrossPageBounds();
		}
		else
		{
			return true;
		}
	}

	public boolean getDiscardRemainderAfterBreak()
	{
		if (part != null)
		{
			return part.getDiscardRemainderAfterBreak();
		}
		else
		{
			return false;
		}
	}

	public boolean getPageBreakBefore()
	{
		if (part != null)
		{
			return part.getPageBreakBefore();
		}
		else
		{
			return false;
		}
	}

	public int getPageBreakAfterOccurrence()
	{
		if (part != null)
		{
			return part.getPageBreakAfterOccurrence();
		}
		else
		{
			return 0;
		}
	}

	public boolean getRestartPageNumber()
	{
		if (part != null)
		{
			return part.getRestartPageNumber();
		}
		else
		{
			return false;
		}
	}

	public void printAll(Graphics g)
	{
		//print
		renderer.printAll(g);
	}

	/**
	 * Gets the ylocation.
	 * 
	 * @return Returns a int
	 */
	public int getYlocation()
	{
		return Ylocation;
	}

	/**
	 * Sets the ylocation.
	 * 
	 * @param ylocation The ylocation to set
	 */
	public void setYlocation(int ylocation)
	{
		Ylocation = ylocation;
	}

	/**
	 * Returns the xlocation.
	 * 
	 * @return int
	 */
	public int getXlocation()
	{
		return Xlocation;
	}

	/**
	 * Sets the xlocation.
	 * 
	 * @param xlocation The xlocation to set
	 */
	public void setXlocation(int xlocation)
	{
		Xlocation = xlocation;
	}

	// return value must be <= then argument; return values < 0 mean that no good break was found
	public int getPreferedBreak(RendererParentWrapper renderParent, int breakPosition, int normalPageBodyAreaHeight)
	{
		int validBreakPosition = getPreferedBreakInternal(renderParent, breakPosition);

		// when you have the following situation: empty vertical space followed by compact block of component(s) (no valid break
		// position), the result in breakPosition will be the end of the empty space... But this can lead for
		// example to first page containing only a few pixels of empty space and the rest of the content moving
		// to the second - where it will still break
		if (validBreakPosition > 0 && validBreakPosition != breakPosition)
		{
			// so the initially suggested break position was moved up due to some intersecting components;
			// if those components do not even fit on the next page, we might as well break them on this page
			int nextPageValidBreak = getPreferedBreakInternal(renderParent, validBreakPosition + normalPageBodyAreaHeight);
			if (nextPageValidBreak == validBreakPosition)
			{
				validBreakPosition = breakPosition; // did not find any valid breaks for leftovers on next page; so break it in this page
			}
		}
		return validBreakPosition;
	}

	private int getPreferedBreakInternal(RendererParentWrapper renderParent, int breakPosition)
	{
		// the break position is relative to the remaining area to be used from the renderer; create the absolute breakk position for the renderer
		Rectangle breakRect = new Rectangle(0, startYOrgin + breakPosition, getFullSize().width, 1);
		int firstConsideredBreak = -1; // if we do not find a satisfactory break point, we use the first intersecting component desired break point
		Component lastComponent = null; // the last component that changed the preferred break point

		Iterator it = bounds.keySet().iterator();
		while (it.hasNext())
		{
			Component component = (Component)it.next();
			Rectangle cbounds = (Rectangle)bounds.get(component);
			if (cbounds.intersects(breakRect) && component != lastComponent)
			{
				Component comp = component;
				int pos;

				if (comp instanceof PortalComponent)
				{
					Rectangle allocation = new Rectangle(cbounds);
					PortalComponent pc = (PortalComponent)comp;

					Insets ins2 = pc.getBorder().getBorderInsets(pc);
					allocation.x += ins2.left;
					allocation.y += ins2.top;
					allocation.width -= ins2.right;
					allocation.height -= ins2.bottom;

					pos = pc.getPreferredBreak((startYOrgin + breakPosition - allocation.y)); // give free space and receive preferred break relative to component
					if (pos != 0)
					{
						pos = pos + allocation.y - startYOrgin; // make preferred break relative to the renderer
						Debug.trace("PortalComponent " + cbounds + " requested break for " + breakPosition + " used break " + pos); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					else
					{
						// no use breaking immediately after the border (no useful info)
						pos = cbounds.getLocation().y - startYOrgin;
					}
				}
				else
				{
					if (component instanceof IDelegate)
					{
						comp = (Component)((IDelegate)component).getDelegate();
					}

					if (comp instanceof JTextComponent)
					{
						if (comp instanceof JTextField)
						{
							pos = cbounds.getLocation().y - startYOrgin; //optimize, cannot break single line components, return the Y location
						}
						else
						{
							JTextComponent tcomp = (JTextComponent)comp;
							Rectangle allocation = new Rectangle(cbounds);
							if (component instanceof JComponent)
							{
								Insets ins2 = ((JComponent)component).getBorder().getBorderInsets(component);//==scrollpane border
								if (ins2 != null)
								{
									allocation.x += ins2.left;
									allocation.y += ins2.top;
									allocation.width -= ins2.right;
									allocation.height -= ins2.bottom;
								}
							}
							Insets ins2 = tcomp.getBorder().getBorderInsets(component);//==margin
							if (ins2 != null)
							{
								allocation.x += ins2.left;
								allocation.y += ins2.top;
								allocation.width -= ins2.right;
								allocation.height -= ins2.bottom;
							}

							pos = searchDesiredBreakThroughViews((startYOrgin + breakPosition - allocation.y), 0, tcomp, 0, new Rectangle(0, 0,
								allocation.width, allocation.height));
							if (pos != 0)
							{
								pos = pos - startYOrgin + allocation.y; //correct for location
								Debug.trace("JTextComponent " + cbounds + " requested break for " + breakPosition + " used break " + pos); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							}
							else
							{
								// no use breaking immediately after the border (no useful info)
								pos = cbounds.getLocation().y - startYOrgin;
							}
						}
					}
					else
					// for all other components
					{
						pos = cbounds.getLocation().y - startYOrgin; //optimize, better stop before comp
					}
				}
				// the current intersected component decided where it would want the page to break (pos); compare with the current break position
				if (breakPosition != pos)
				{
					// we have to modify the desired (page) break point
					breakPosition = pos;
					breakRect.y = startYOrgin + breakPosition;
					it = bounds.keySet().iterator(); // restart search for components that intersect the new considered break position
					if ((firstConsideredBreak == -1) && (breakPosition > 0))
					{
						firstConsideredBreak = breakPosition;
					}
					lastComponent = component;
				} // else the current break is OK from this component's point of view

				if (breakPosition < 0)
				{
					break; // the components that we would find below 0 from this part are already printed - so we did not find a convenient
					// break point in the given vertical space
				}
			}
		}
		if (breakPosition <= 0)
		{
			// no satisfactory normal break position; returns the first found break position (that can be < 0 too if not found)
			return firstConsideredBreak;
		}
		else
		{
			// found a good break position
			return breakPosition;
		}
	}

	private int searchDesiredBreakThroughViews(final int pos, int returnValue, final JTextComponent tcomp, int parentY, Rectangle allocation)
	{
		// see if last kid view passes the desired break position or not (if not, then the desired position is fine)
		final boolean[] viewsPassDesiredBreak = new boolean[1];

		application.invokeAndWait(new Runnable()
		{
			public void run()
			{
				Rectangle r = null;
				int lastPosition = tcomp.getDocument().getLength() - 1;
				if (lastPosition >= 0)
				{
					try
					{
						r = tcomp.modelToView(tcomp.getDocument().getLength() - 1);
					}
					catch (BadLocationException e)
					{
						viewsPassDesiredBreak[0] = true;
					}
				}
				else
				{
					r = new Rectangle(0, 0, 0, 0);
				}
				viewsPassDesiredBreak[0] = (r.y + r.height >= pos);
			}
		});

		if (viewsPassDesiredBreak[0])
		{
			return walkView(pos, returnValue, tcomp.getUI().getRootView(tcomp), parentY, allocation);
		}
		else
		{
			return pos;
		}
	}

	// Recursively walks a view hierarchy
	private int walkView(int pos, int returnValue, View view, int parentY, Rectangle allocation)
	{
		//Get number of children views
		int n = view.getViewCount();

		// Visit the children of this view
		for (int i = 0; i < n; i++)
		{
			View kid = view.getView(i);

			Shape kidshape = view.getChildAllocation(i, allocation);
			if (kidshape == null) continue;

			Rectangle kidbox = kidshape.getBounds();
			int kidY = ((int)kidbox.getY()) + parentY;


//			//check for pagebreak <br id="pagebreak">
//			int pagebreak = 0;
//			if (kid != null)
//			{
//				Element e = kid.getElement();
//				if (e != null && "br".equals(e.getName()))
//				{
//					AttributeSet as = e.getAttributes();
//					if (as != null)
//					{
//						Object val = as.getAttribute(HTML.Attribute.ID);
//						if (val != null && "pagebreak".equalsIgnoreCase(val.toString()))
//						{
//							if ( (kidY+kidbox.height) <= pos)
//							{
//								Debug.error("pagebreak "+(kidY+kidbox.height));
//								pagebreak = (kidY+kidbox.height);
//							}
//						}
//					}
//				}
//			}

			if (kidY > returnValue && kidY <= pos)
			{
//				Debug.trace("Found Y "+Y+" (diff "+(Y-returnValue)+" )");
				returnValue = kidY;
			}
			returnValue = walkView(pos, returnValue, kid, kidY, allocation);
		}
		return returnValue;
	}

	Part getPart()
	{
		return part;
	}

}
