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
package com.servoy.j2db.smart.dataui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.text.html.CSS;

import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportPrintSliding;
import com.servoy.j2db.persistence.ISupportPrinting;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.smart.SwingForm;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.TabSequenceHelper;
import com.servoy.j2db.util.gui.AnchorLayout;
import com.servoy.j2db.util.gui.FixedSpringLayout;
import com.servoy.j2db.util.gui.Spring;

/**
 * Factory for Swing datarenderers
 * 
 * @author jblok
 */
public class DataRendererFactory implements IDataRendererFactory<Component>
{
	public IDataRenderer getEmptyDataRenderer(String Id, String name, IApplication app, boolean showSelection)
	{
		DataRenderer dr = new DataRenderer(app, showSelection);
		dr.putClientProperty("Id", Id);
		dr.setName(name);
		return dr;
	}

	private void setBasicSettings(IDataRenderer panel, Color bg, Dimension size, Point location, boolean printing)
	{
		DataRenderer dr = (DataRenderer)panel;
		if (dr != null)
		{
			if (bg != null)
			{
				dr.setBackground(bg);
			}
			if (printing)
			{
				dr.setLayout(new FixedSpringLayout());
			}
			else
			{
				dr.setLayout(new AnchorLayout(size));
				dr.setPreferredSize(size);
			}
			dr.setSize(size);
			dr.setLocation(location);
		}
	}

	public IDataRenderer createPortalRenderer(IApplication app, Portal objToRender, Form dataProviderLookup, IScriptExecuter listner, boolean printing,
		ControllerUndoManager undoManager) throws Exception
	{
		List<IPersist> allObjectsAsList = objToRender.getAllObjectsAsList();
		List<IFormElement> formElements = new ArrayList<IFormElement>(allObjectsAsList.size());
		for (IPersist persist : allObjectsAsList)
		{
			if (persist instanceof IFormElement)
			{
				formElements.add((IFormElement)persist);
			}
		}
		List<IFormElement> children = new SortedList<IFormElement>(new Comparator<IFormElement>()
		{
			public int compare(IFormElement o1, IFormElement o2)
			{
				// reverse order, right order for tab sequence
				int result = -PositionComparator.XY_PERSIST_COMPARATOR.compare(o1, o2);
				if (result == 0)
				{
					return (o1.getFormIndex() - o2.getFormIndex());
				}
				return result;
			}
		}, formElements);
		Iterator<IFormElement> e1 = children.iterator();
		Map emptyDataRenderers = new LinkedHashMap();
		DataRenderer dr = null;
		int height = objToRender.getRowHeight();
		boolean calculateHeight = (height == 0);
		if (height == 0) height = 100;//for safety

//		int leftBorder = 0;
//		int bottomBorder = 0;
//		if (objToRender.getShowHorizontalLines())
//		{
//			bottomBorder = 1;
//		}
//		if (objToRender.getShowVerticalLines())
//		{
//			leftBorder = 4;
//		}
		boolean showSelection = objToRender.getShowVerticalLines();
		dr = (DataRenderer)getEmptyDataRenderer(ComponentFactory.getWebID(dataProviderLookup, objToRender), "portal_" + objToRender.getName(), app,
			showSelection);

		setBasicSettings(dr, objToRender.getBackground(), new Dimension(objToRender.getSize().width, height), new Point(0, 0), printing);
		emptyDataRenderers.put(new Boolean(true), dr);//first is dummy saying is portal
		dr.setLocation(objToRender.getLocation());
		Border b = ComponentFactoryHelper.createBorder(objToRender.getBorderType());
		Insets ins = new Insets(0, 0, 0, 0);
		if (b != null)
		{
			ins = ComponentFactoryHelper.getBorderInsetsForNoComponent(b);
		}
//		if (b instanceof EmptyBorder)
//		{
//			ins = ((EmptyBorder)b).getBorderInsets();
//		}
		placeElements(e1, app, dataProviderLookup, listner, emptyDataRenderers, objToRender.getSize().width, -(objToRender.getLocation().x + ins.right),
			-ins.top, printing, true, undoManager, true, null);

		int biggest_width = 10;
		int biggest_height = calculateHeight ? 0 : height;
		Component[] comps = dr.getComponents();
		boolean hasRowBGColorCalc = objToRender.getRowBGColorCalculation() != null;
		for (Component element : comps)
		{
			int w = element.getLocation().x + element.getSize().width;
			if (w > biggest_width) biggest_width = w;

			if (hasRowBGColorCalc && element instanceof JComponent) ((JComponent)element).setOpaque(false);
			if (calculateHeight)
			{
				int h = element.getLocation().y + element.getSize().height;
				if (h > biggest_height) biggest_height = h;
			}
		}
		dr.setSize(new Dimension(biggest_width, biggest_height));
		dr.setPreferredSize(new Dimension(biggest_width, biggest_height));

		if (printing)
		{
			addSpringsBetweenComponents(app, dr);//sliding inside a multiline portal renderer is nor supported, be we have to attach the springs for resizes
		}
		return dr;
	}

	/**
	 * Fills all the panels provided in emptyDataRenderers with the components from the form
	 * 
	 * @param form the form to work with
	 * @param emptyDataRenderers the orderedHashmap with the Part -> DataRenderer
	 * @param insets for any border
	 * @param with of all parts
	 * @return usesSliding
	 */
	public Map completeRenderers(IApplication app, Form form, IScriptExecuter listner, Map emptyDataRenderers, int width, boolean printing,
		ControllerUndoManager undoManager, TabSequenceHelper<Component> tabSequence) throws Exception
	{
		int partHeight = 0;
		boolean formHasBgImage = false;
		Pair<IStyleSheet, IStyleRule> formStyle = ComponentFactory.getCSSPairStyleForForm(app, form);
		if (formStyle != null && formStyle.getRight() != null && formStyle.getRight().hasAttribute(CSS.Attribute.BACKGROUND_IMAGE.toString()))
		{
			formHasBgImage = true;
		}

		Iterator e2 = form.getParts();
		while (e2.hasNext())
		{
			Part part = (Part)e2.next();
			Color bg = ComponentFactory.getPartBackground(app, part, form);
			if (bg == null && printing) bg = Color.white;

			DataRenderer panel = (DataRenderer)emptyDataRenderers.get(part);
			if (panel != null)
			{
				panel.setDoubleBuffered(!printing);
				int total = Math.abs(part.getHeight() - partHeight);
				setBasicSettings(panel, bg, new Dimension(width, total), new Point(0, partHeight), printing);
			}
			partHeight = part.getHeight();
			// revert css3 features
			Pair<IStyleSheet, IStyleRule> pair = ComponentFactory.getStyleForBasicComponent(app, part, form);
			if (panel != null)
			{
//				panel.setBgColor(part.getBackground());
				if (pair != null && pair.getRight() != null && pair.getLeft() != null)
				{
//					panel.setCssRule(pair.getRight());
					Border border = pair.getLeft().getBorder(pair.getRight());
					if (border != null)
					{
						panel.setBorder(border);
					}
				}
//				boolean partHasBgColor = (part.getBackground() != null) ||
//					(pair != null && pair.getRight() != null && pair.getRight().hasAttribute(CSS.Attribute.BACKGROUND_COLOR.toString()));
//				if (!form.getTransparent() && (formHasBgImage || (partHasBgColor && bg.getAlpha() < 255)))
//				{
//					panel.setPaintBackgroundOnTopOfFormImage(true);
//				}
			}
		}

		//place all the elements
		Iterator<IFormElement> e1 = form.getFormElementsSortedByFormIndex();
		return placeElements(e1, app, form, listner, emptyDataRenderers, width, 0, 0, printing, false, undoManager, false, tabSequence);
	}

	//returns usesSliding
	private Map placeElements(Iterator<IFormElement> e1, IApplication app, Form form, IScriptExecuter listner, Map emptyDataRenderers, int width,
		int XCorrection, int YCorrection, boolean printing, boolean cutDataProviderNames, ControllerUndoManager undoManager, boolean isPortal,
		TabSequenceHelper<Component> tabSequence) throws Exception
	{
		IDataProviderLookup dataProviderLookup = app.getFlattenedSolution().getDataproviderLookup(app.getFoundSetManager(), form);
		Map listTocomplete = new HashMap();

		Map labelForComponents = new HashMap();

//		Insets insets = new Insets(0, 0, 0, 0);
		while (e1.hasNext())
		{
			Point l = null;
			IPersist obj = e1.next();
			l = ((IFormElement)obj).getLocation();

			if (l == null) continue;//unkown where to add
			if (printing && obj instanceof ISupportPrinting)
			{
				if (!((ISupportPrinting)obj).getPrintable()) continue;
			}

			Iterator it = emptyDataRenderers.values().iterator();
			while (it.hasNext())
			{
				DataRenderer panel = (DataRenderer)it.next();

				int start = panel.getLocation().y;
				if (l.y >= start && l.y < start + panel.getSize().height)
				{
					Component comp = (Component)ComponentFactory.createComponent(app, form, obj, dataProviderLookup, listner, printing);
					// Test for a visible bean, then get the real component
					if (comp instanceof VisibleBean)
					{
						comp = ((VisibleBean)comp).getDelegate();
					}

					if (comp != null)
					{
						if (obj instanceof Field && comp instanceof JComponent)
						{
							String name = ((Field)obj).getName();
							if (name != null && !"".equals(name))
							{
								labelForComponents.put(name, comp);
							}
						}
						else if (obj instanceof GraphicalComponent && comp instanceof JLabel)
						{
							String labelFor = ((GraphicalComponent)obj).getLabelFor();
							if (labelFor != null && !"".equals(labelFor))
							{
								labelForComponents.put(comp, labelFor);
							}

						}
						if (obj instanceof ISupportTabSeq && comp instanceof JComponent && (tabSequence != null))
						{
							tabSequence.add(panel, (ISupportTabSeq)obj, comp);
						}
						Component newComp = comp;
						if (newComp instanceof IDisplay)
						{
							//HACK:don;t no other way to do this.........
							if (newComp instanceof IDisplayData && cutDataProviderNames)
							{
								IDisplayData da = (IDisplayData)newComp;
								String id = da.getDataProviderID();
								if (id != null && !ScopesUtils.isVariableScope(id))
								{
									int index = id.indexOf('.'); // only cut first relation (so you can have relation chain inside portal)
									//TODO:check if part before . is same as relation name (objToRender.getRelationID() )
									if (index > 0)
									{
										id = id.substring(index + 1);
									}
									da.setDataProviderID(id);
								}
							}
							panel.addDisplayComponent(obj, (IDisplay)newComp);
						}
						comp.setLocation((l.x /* +insets.left */) + XCorrection, (l.y - start) + YCorrection);

						int index = 0;
						if (!printing && obj instanceof ISupportAnchors)
						{
							panel.add(comp, new Integer(((ISupportAnchors)obj).getAnchors()), index);
						}
						else if (printing)
						{
							if (obj instanceof ISupportPrintSliding && !isPortal)
							{
								int slide = ((ISupportPrintSliding)obj).getPrintSliding();
								if (slide != ISupportPrintSliding.NO_SLIDING)
								{
									listTocomplete.put(comp, new Integer(slide));
									panel.setUsingSliding(true);
								}
							}
							panel.add(comp, index);
						}
						else
						{
							panel.add(comp, index);
						}
					}
				}
			}
		}

		if (!printing)
		{
			Iterator it = labelForComponents.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry entry = (Entry)it.next();
				if (entry.getKey() instanceof JLabel)
				{
					JComponent component = (JComponent)labelForComponents.get(entry.getValue());
					if (component != null)
					{
						((JLabel)entry.getKey()).setLabelFor(component);
						if (component instanceof IFieldComponent)
						{
							((IFieldComponent)component).addLabelFor((ILabel)entry.getKey());
							if (!((IFieldComponent)component).isVisible())
							{
								((IFieldComponent)component).setComponentVisible(((IFieldComponent)component).isVisible());
							}
							if (!((IFieldComponent)component).isEnabled())
							{
								((IFieldComponent)component).setComponentEnabled(((IFieldComponent)component).isEnabled());
							}
						}
					}
				}
			}

		}

		Iterator it = emptyDataRenderers.values().iterator();
		while (it.hasNext())
		{
			DataRenderer panel = (DataRenderer)it.next();
			panel.createDataAdapter(app, dataProviderLookup, listner, undoManager);
		}
		return listTocomplete;
	}

	@Override
	public void prepareRenderers(IApplication application, Form form)
	{
	}

	public static void addSpringsBetweenComponents(IApplication application, DataRenderer panel)
	{
		Map componentsListToBeHandeldOnX = new HashMap();
		Map componentsListToBeHandeldOnY = new HashMap();
		Map componentsListToBeHandeldOnWidth = new HashMap();
		Map componentsListToBeHandeldOnHeigth = new HashMap();

		FixedSpringLayout sl = (FixedSpringLayout)panel.getLayout();

		FixedSpringLayout.Constraints pcons = sl.getConstraints(panel);

		Component[] allc = panel.getComponents();
		for (Component comp : allc)
		{
			if (comp instanceof EnableScrollPanel)
			{
				((EnableScrollPanel)comp).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				((EnableScrollPanel)comp).setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			}

			int slide = ISupportPrintSliding.NO_SLIDING;
			Map componentsListToBeHandeld = panel.getComponentsUsingSliding();
			if (componentsListToBeHandeld != null && componentsListToBeHandeld.containsKey(comp))
			{
				slide = ((Integer)componentsListToBeHandeld.get(comp)).intValue();

				for (Component ccomp : allc)
				{
					if (slide != ISupportPrintSliding.NO_SLIDING && ccomp != comp && ccomp.getBounds().intersects(comp.getBounds()))
					{
						application.reportJSError(
							application.getI18NMessage("servoy.sliding.error.overlap"), application.getI18NMessage("servoy.sliding.error.overlap.components", new Object[] { comp, ccomp })); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}

			Spring sx = Spring.constant(comp.getX());
			Spring sy = Spring.constant(comp.getY());
			Spring sw = Spring.constant(comp.getWidth());
			Spring sh = Spring.constant(comp.getHeight());

			if (((slide & ISupportPrintSliding.ALLOW_MOVE_MIN_X) == ISupportPrintSliding.ALLOW_MOVE_MIN_X) ||
				((slide & ISupportPrintSliding.ALLOW_MOVE_PLUS_X) == ISupportPrintSliding.ALLOW_MOVE_PLUS_X))
			{
				componentsListToBeHandeldOnX.put(comp, new Integer(slide));
			}
			if (((slide & ISupportPrintSliding.ALLOW_MOVE_MIN_Y) == ISupportPrintSliding.ALLOW_MOVE_MIN_Y) ||
				((slide & ISupportPrintSliding.ALLOW_MOVE_PLUS_Y) == ISupportPrintSliding.ALLOW_MOVE_PLUS_Y))
			{
				componentsListToBeHandeldOnY.put(comp, new Integer(slide));
			}
			if (((slide & ISupportPrintSliding.GROW_WIDTH) == ISupportPrintSliding.GROW_WIDTH) ||
				((slide & ISupportPrintSliding.SHRINK_WIDTH) == ISupportPrintSliding.SHRINK_WIDTH))
			{
//				if (comp instanceof EnableScrollPanel)
//				{
//					((EnableScrollPanel)comp).setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//					((EnableScrollPanel)comp).setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
//				}
				componentsListToBeHandeldOnWidth.put(comp, new Integer(slide));
				int max = Short.MAX_VALUE;
				int min = 0;

				if (((slide & ISupportPrintSliding.GROW_WIDTH) != ISupportPrintSliding.GROW_WIDTH) &&
					((slide & ISupportPrintSliding.SHRINK_WIDTH) == ISupportPrintSliding.SHRINK_WIDTH))
				{
					min = 0;
					max = comp.getWidth();
				}
				if (((slide & ISupportPrintSliding.GROW_WIDTH) == ISupportPrintSliding.GROW_WIDTH) &&
					((slide & ISupportPrintSliding.SHRINK_WIDTH) != ISupportPrintSliding.SHRINK_WIDTH))
				{
					min = comp.getWidth();
					max = Short.MAX_VALUE;
				}
				sw = new AbsWidthSpring(comp, min, max);
			}
			if (((slide & ISupportPrintSliding.GROW_HEIGHT) == ISupportPrintSliding.GROW_HEIGHT) ||
				((slide & ISupportPrintSliding.SHRINK_HEIGHT) == ISupportPrintSliding.SHRINK_HEIGHT))
			{
				componentsListToBeHandeldOnHeigth.put(comp, new Integer(slide));
				int max = Short.MAX_VALUE;
				int min = 0;

				if (((slide & ISupportPrintSliding.GROW_HEIGHT) != ISupportPrintSliding.GROW_HEIGHT) &&
					((slide & ISupportPrintSliding.SHRINK_HEIGHT) == ISupportPrintSliding.SHRINK_HEIGHT))
				{
					min = 0;
					max = comp.getHeight();
				}
				if (((slide & ISupportPrintSliding.GROW_HEIGHT) == ISupportPrintSliding.GROW_HEIGHT) &&
					((slide & ISupportPrintSliding.SHRINK_HEIGHT) != ISupportPrintSliding.SHRINK_HEIGHT))
				{
					min = comp.getHeight();
					max = Short.MAX_VALUE;
				}
				sh = new AbsHeightSpring(comp, min, max);
			}

			FixedSpringLayout.Constraints constr = sl.getConstraints(comp); //new FixedSpringLayout.Constraints(sx,sy,sw,sh);
			constr.setX(sx);
			constr.setY(sy);
			constr.setWidth(sw);
			constr.setHeight(sh);
		}


		Component[] allSortedXy = new Component[allc.length];
		System.arraycopy(allc, 0, allSortedXy, 0, allc.length);
		Arrays.sort(allSortedXy, PositionComparator.XY_COMPONENT_COMPARATOR);

		Component[] allSortedYx = new Component[allc.length];
		System.arraycopy(allc, 0, allSortedYx, 0, allc.length);
		Arrays.sort(allSortedYx, PositionComparator.YX_COMPONENT_COMPARATOR);

		// add west component spring for sliding components that can move horizontally, to set component location
		Iterator it1 = componentsListToBeHandeldOnX.keySet().iterator();
		while (it1.hasNext())
		{
			Component element1 = (Component)it1.next();
			BarSpring bs = null;

			int minXSpaceOnLeft = Integer.MAX_VALUE; // X space left to the element1's WEST border from 
			// components on X exactly on the left of element1
			ArrayList slidingComponentsOnLeft = new ArrayList(); // sliding X components that are exactly on the left of element1

			Component rightMostNonSlidingLastComponentOnLeft = null; // the right most non sliding component that is on the left of element1
			int xSpaceForRightmostNonSlidingLastComponentOnLeft = Integer.MAX_VALUE;

			for (Component element2 : allSortedYx)
			{
				boolean isExactlyOnLeftOfElement1 = true;
				if (element2 == element1) continue;

				Rectangle join = createSmallestHorizontalJoin(element2.getBounds(), element1.getBounds());
				if (join == null || join.isEmpty())
				{
					isExactlyOnLeftOfElement1 = false;
				}
				else
				{
					for (Component element3 : allSortedYx)
					{
						if (element3 != element1 && element3 != element2 && join.intersects(element3.getBounds()))
						{
							isExactlyOnLeftOfElement1 = false;
							break;//too large, contains other eleemnts in between
						}
					}
				}

				if (isExactlyOnLeftOfElement1)
				{
					int xSpace = element1.getX() - (element2.getX() + element2.getWidth());
					minXSpaceOnLeft = Math.min(minXSpaceOnLeft, xSpace);
					if (componentsListToBeHandeldOnWidth.containsKey(element2) || componentsListToBeHandeldOnX.containsKey(element2))
					{
						slidingComponentsOnLeft.add(element2);
					}
					else
					{
						if (xSpaceForRightmostNonSlidingLastComponentOnLeft > xSpace)
						{
							xSpaceForRightmostNonSlidingLastComponentOnLeft = xSpace;
							rightMostNonSlidingLastComponentOnLeft = element2;
						}
					}
				}
			}

			if (rightMostNonSlidingLastComponentOnLeft != null)
			{
				bs = new BarSpring();
				sl.getConstraints(element1).setConstraint(FixedSpringLayout.WEST, bs);
				bs.add(Spring.sum(Spring.constant(minXSpaceOnLeft, minXSpaceOnLeft, Short.MAX_VALUE),
					sl.getConstraint(FixedSpringLayout.EAST, rightMostNonSlidingLastComponentOnLeft)));
			}
			else if (slidingComponentsOnLeft.size() > 0)
			{
				bs = new BarSpring();
				sl.getConstraints(element1).setConstraint(FixedSpringLayout.WEST, bs);
			}

			for (int i = slidingComponentsOnLeft.size() - 1; i >= 0; i--)
			{
				bs.add(Spring.sum(Spring.constant(minXSpaceOnLeft, minXSpaceOnLeft, Short.MAX_VALUE),
					sl.getConstraint(FixedSpringLayout.EAST, (Component)slidingComponentsOnLeft.get(i))));
			}
		}

		// add north component spring for sliding components that allow vertical move, to set component location
		Iterator it2 = componentsListToBeHandeldOnY.keySet().iterator();
		while (it2.hasNext())
		{
			Component element1 = (Component)it2.next();
			BarSpring bs = null;

			int minYSpaceOnTop = Integer.MAX_VALUE; // Y space left to the element1's NORTH border from 
			// components on Y right on top of element1
			ArrayList slidingComponentsOnTop = new ArrayList(); // sliding Y components that are right on top of element1

			Component lowestNonSlidingLastComponentOnTop = null; // the lowest non sliding component that is on top of element1
			int ySpaceForLowestNonSlidingLastComponentOnTop = Integer.MAX_VALUE;

			for (Component element2 : allSortedXy)
			{
				boolean isRightOnTopOfElement1 = true;
				if (element2 == element1) continue;

				Rectangle join = createSmallestVerticalJoin(element2.getBounds(), element1.getBounds());
				if (join == null || join.isEmpty())
				{
					isRightOnTopOfElement1 = false;
				}
				else
				{
					for (Component element3 : allSortedXy)
					{
						if (element3 != element1 && element3 != element2 && join.intersects(element3.getBounds()))
						{
							isRightOnTopOfElement1 = false;
							break;//too large, contains other elements in between
						}
					}
				}

				if (isRightOnTopOfElement1)
				{
					int ySpace = element1.getY() - (element2.getY() + element2.getHeight());
					minYSpaceOnTop = Math.min(minYSpaceOnTop, ySpace);
					if (componentsListToBeHandeldOnHeigth.containsKey(element2) || componentsListToBeHandeldOnY.containsKey(element2))
					{
						slidingComponentsOnTop.add(element2);
					}
					else
					{
						if (ySpaceForLowestNonSlidingLastComponentOnTop > ySpace)
						{
							ySpaceForLowestNonSlidingLastComponentOnTop = ySpace;
							lowestNonSlidingLastComponentOnTop = element2;
						}
					}
				}
			}

			if (lowestNonSlidingLastComponentOnTop != null)
			{
				bs = new BarSpring();
				sl.getConstraints(element1).setConstraint(FixedSpringLayout.NORTH, bs);
				bs.add(Spring.sum(Spring.constant(minYSpaceOnTop, minYSpaceOnTop, Short.MAX_VALUE),
					sl.getConstraint(FixedSpringLayout.SOUTH, lowestNonSlidingLastComponentOnTop)));
			}
			else if (slidingComponentsOnTop.size() > 0)
			{
				bs = new BarSpring();
				sl.getConstraints(element1).setConstraint(FixedSpringLayout.NORTH, bs);
			}

			for (int i = slidingComponentsOnTop.size() - 1; i >= 0; i--)
			{
				bs.add(Spring.sum(Spring.constant(minYSpaceOnTop, minYSpaceOnTop, Short.MAX_VALUE),
					sl.getConstraint(FixedSpringLayout.SOUTH, (Component)slidingComponentsOnTop.get(i))));
			}
		}

		// add east panel spring, to set Panel width
		Rectangle pb = panel.getBounds();
		BarSpring bs = null;
		int spaceToRightMargin = 0;

		if (componentsListToBeHandeldOnWidth.size() > 0)
		{
			int minWidthLeft = Integer.MAX_VALUE; // width left to the part's EAST border from the right-most component on Y
			ArrayList lastSlidingComponentsOnX = new ArrayList(); // sliding X components that are last on X axis

			Component lowestNonSlidingLastComponentOnX = null; // the right most non sliding component that is last on X axis
			int widthLeftForLowestNonSlidingLastComponentOnX = Integer.MAX_VALUE;

			for (int i = allSortedXy.length - 1; i >= 0; i--)
			{
				boolean isALastComponentOnX = true; // are there no other components between it and the part east border?
				Component element = allSortedXy[i];
				Rectangle rect1 = new Rectangle(element.getBounds());
				int widthLeft = pb.width - (rect1.x + rect1.width);
				rect1.width += widthLeft;

				for (Component element2 : allSortedXy)
				{
					if (element2 != element && element.getBounds().intersects(element2.getBounds()))
					{
						//overlapping elements
						continue;
					}
					if (element2 != element && rect1.intersects(element2.getBounds()))
					{
						isALastComponentOnX = false;
						break; //too large, contains other elements in between
					}
				}
				if (isALastComponentOnX)
				{
					minWidthLeft = Math.min(minWidthLeft, widthLeft);
					if (componentsListToBeHandeldOnWidth.containsKey(element) || componentsListToBeHandeldOnX.containsKey(element))
					{
						lastSlidingComponentsOnX.add(element);
					}
					else
					{
						if (widthLeftForLowestNonSlidingLastComponentOnX > widthLeft)
						{
							widthLeftForLowestNonSlidingLastComponentOnX = widthLeft;
							lowestNonSlidingLastComponentOnX = element;
						}
					}
				}
			}

			// add springs needed to determine the width of the part
			if (lowestNonSlidingLastComponentOnX != null)
			{
				bs = new BarSpring();
				pcons.setConstraint(FixedSpringLayout.EAST, bs);
				bs.add(Spring.sum(Spring.constant(minWidthLeft, minWidthLeft, Short.MAX_VALUE),
					sl.getConstraint(FixedSpringLayout.EAST, lowestNonSlidingLastComponentOnX)));
				spaceToRightMargin = minWidthLeft;
			}
			else if (lastSlidingComponentsOnX.size() > 0)
			{
				bs = new BarSpring();
				pcons.setConstraint(FixedSpringLayout.EAST, bs);
				spaceToRightMargin = minWidthLeft;
			}

			for (int i = lastSlidingComponentsOnX.size() - 1; i >= 0; i--)
			{
				bs.add(Spring.sum(Spring.constant(minWidthLeft, minWidthLeft, Short.MAX_VALUE),
					sl.getConstraint(FixedSpringLayout.EAST, (Component)lastSlidingComponentsOnX.get(i))));
			}
			panel.setSpaceToRightMargin(spaceToRightMargin);
		}
		if (bs == null)//add fixed spring for panel size
		{
			pcons.setConstraint(FixedSpringLayout.EAST, Spring.constant(panel.getWidth()));
		}

		// add south panel spring, to set Panel height
		bs = null;
		if (componentsListToBeHandeldOnHeigth.size() > 0)
		{
			int minHeightLeft = Integer.MAX_VALUE; // height left to the part's SOUTH border from the lowest component on Y
			ArrayList lastSlidingComponentsOnY = new ArrayList(); // sliding Y components that are last on Y axis

			Component lowestNonSlidingLastComponentOnY = null; // the lowest non sliding component that is last on Y axis
			int heightLeftForLowestNonSlidingLastComponentOnY = Integer.MAX_VALUE;

			for (int i = allSortedYx.length - 1; i >= 0; i--)
			{
				boolean isALastComponentOnY = true; // are there no other components between it and the part south border?
				Component element = allSortedYx[i];
				Rectangle rect1 = new Rectangle(element.getBounds());
				int heightLeft = pb.height - (rect1.y + rect1.height);
				rect1.height += heightLeft;

				for (Component element2 : allSortedYx)
				{
					if (element2 != element && element.getBounds().intersects(element2.getBounds()))
					{
						//overlapping elements
						continue;
					}
					if (element2 != element && rect1.intersects(element2.getBounds()))
					{
						isALastComponentOnY = false;
						break;//too large, contains other elements in between
					}
				}
				if (isALastComponentOnY)
				{
					minHeightLeft = Math.min(minHeightLeft, heightLeft);
					if (componentsListToBeHandeldOnHeigth.containsKey(element) || componentsListToBeHandeldOnY.containsKey(element))
					{
						lastSlidingComponentsOnY.add(element);
					}
					else
					{
						if (heightLeftForLowestNonSlidingLastComponentOnY > heightLeft)
						{
							heightLeftForLowestNonSlidingLastComponentOnY = heightLeft;
							lowestNonSlidingLastComponentOnY = element;
						}
					}
				}
			}

			// add springs needed to determine the height of the part
			if (lowestNonSlidingLastComponentOnY != null)
			{
				bs = new BarSpring();
				pcons.setConstraint(FixedSpringLayout.SOUTH, bs);
				bs.add(Spring.sum(Spring.constant(minHeightLeft, minHeightLeft, Short.MAX_VALUE),
					sl.getConstraint(FixedSpringLayout.SOUTH, lowestNonSlidingLastComponentOnY)));
			}
			else if (lastSlidingComponentsOnY.size() > 0)
			{
				bs = new BarSpring();
				pcons.setConstraint(FixedSpringLayout.SOUTH, bs);
			}

			for (int i = lastSlidingComponentsOnY.size() - 1; i >= 0; i--)
			{
				bs.add(Spring.sum(Spring.constant(minHeightLeft, minHeightLeft, Short.MAX_VALUE),
					sl.getConstraint(FixedSpringLayout.SOUTH, (Component)lastSlidingComponentsOnY.get(i))));
			}
		}
		if (bs == null)//add fixed spring for panel size
		{
			pcons.setConstraint(FixedSpringLayout.SOUTH, Spring.constant(panel.getHeight()));
		}
	}

	private static Rectangle createSmallestHorizontalJoin(Rectangle r1, Rectangle r2)
	{
//		while(true)
//		{
		if (r1.x <= r2.x)
		{
			if (r2.y + r2.height < r1.y || r2.y > r1.y + r1.height) return new Rectangle();

			int y = Math.max(r1.y, r2.y);
			return new Rectangle(r1.x, y, (r2.x + r2.width) - r1.x, Math.min(r1.y + r1.height, r2.y + r2.height) - y);
		}
		else
		//swap
		{
//				Rectangle r = r1;
//				r1 = r2;
//				r2 = r;
			return new Rectangle();
		}
//		}
	}

	private static Rectangle createSmallestVerticalJoin(Rectangle r1, Rectangle r2)
	{
//		while(true)
//		{
		if (r1.y <= r2.y)
		{
			if (r2.x + r2.width < r1.x || r2.x > r1.x + r1.width) return new Rectangle();

			int x = Math.max(r1.x, r2.x);
			return new Rectangle(x, r1.y, Math.min(r1.x + r1.width, r2.x + r2.width) - x, (r2.y + r2.height) - r1.y);
		}
		else
		//swap
		{
//				Rectangle r = r1;
//				r1 = r2;
//				r2 = r;
			return new Rectangle();
		}
//		}
	}

	static class AbsWidthSpring extends Spring.AbstractSpring
	{
		private final Component c;
		private final int minWidth;
		private final int maxWidth;

		public AbsWidthSpring(Component c)
		{
			this.c = c;
			this.minWidth = c.getMinimumSize().height;
			this.maxWidth = c.getMaximumSize().width;
		}

		public AbsWidthSpring(Component c, int min, int max)
		{
			this.c = c;
			this.minWidth = min;
			this.maxWidth = max;
		}

		@Override
		public int getMinimumValue()
		{
			return minWidth;//c.getMinimumSize().width;
		}

		@Override
		public int getPreferredValue()
		{
			int pw = c.getPreferredSize().width;
			if (pw < minWidth) pw = minWidth;
			if (pw > maxWidth) pw = maxWidth;
			return pw;
		}

		@Override
		public int getMaximumValue()
		{
			// We will be doing arithmetic with the results of this call,
			// so if a returned value is Integer.MAX_VALUE we will get
			// arithmetic overflow. Truncate such values.
			return Math.min(Short.MAX_VALUE, maxWidth);
		}
	}

	static class AbsHeightSpring extends Spring.AbstractSpring
	{
		private final Component c;
		private final int minHeight;
		private final int maxHeight;

		public AbsHeightSpring(Component c)
		{
			this.c = c;
			this.minHeight = c.getMinimumSize().height;
			this.maxHeight = c.getMaximumSize().height;
		}

		public AbsHeightSpring(Component c, int min, int max)
		{
			this.c = c;
			this.minHeight = min;
			this.maxHeight = max;
		}

		@Override
		public int getMinimumValue()
		{
			return minHeight;
		}

		@Override
		public int getPreferredValue()
		{
			int ph = c.getPreferredSize().height;
			if (ph < minHeight) ph = minHeight;
			if (ph > maxHeight) ph = maxHeight;
			return ph;
		}

		@Override
		public int getMaximumValue()
		{
			return Math.min(Short.MAX_VALUE, maxHeight);
		}
	}

	private static class BarSpring extends Spring.StaticSpring
	{
		protected List springs;

		public BarSpring()
		{
			clear();
			springs = new ArrayList();
		}

		@Override
		public String toString()
		{
			return "BarSpring of " + springs.size(); //$NON-NLS-1$
		}

		public void add(Spring s)
		{
			springs.add(s);
		}

		@Override
		protected void clear()
		{
			min = pref = max = size = UNSET;
		}

		@Override
		public void setValue(int size)
		{
			if (size == UNSET)
			{
				if (this.size != UNSET)
				{
					super.setValue(size);
					Iterator it = springs.iterator();
					while (it.hasNext())
					{
						Spring element = (Spring)it.next();
						element.setValue(UNSET);
					}
//Debug.trace("Barsize - clear");			
					size = UNSET;
					return;
				}
			}

//Debug.trace("Barsize - SET size " + size);			
			super.setValue(size);

//			s1.setStrain(this.getStrain());
//			s2.setValue(size - s1.getValue());
		}

		@Override
		public int getMinimumValue()
		{
			if (min == UNSET)
			{
				min = 0;//Short.MAX_VALUE;
				Iterator it = springs.iterator();
				while (it.hasNext())
				{
					Spring element = (Spring)it.next();
					min = Math.max(min, element.getMinimumValue());
				}
			}
//Debug.trace("Barsize - min " + min);		
			return min;
		}

		@Override
		public int getPreferredValue()
		{
			if (pref == UNSET)
			{
				pref = 0;
				Iterator it = springs.iterator();
				while (it.hasNext())
				{
					Spring element = (Spring)it.next();
					pref = Math.max(pref, element.getPreferredValue());
				}
			}
//Debug.trace("Barsize - pref " + pref);			
			return pref;
		}

		@Override
		public int getMaximumValue()
		{
			if (max == UNSET)
			{
				max = Short.MAX_VALUE;
				Iterator it = springs.iterator();
				while (it.hasNext())
				{
					Spring element = (Spring)it.next();
					max = Math.min(max, element.getMaximumValue());
				}
			}
//Debug.trace("Barsize - max " + max);			
			return max;
		}

		@Override
		public int getValue()
		{
			if (size == UNSET)
			{
				size = 0;
				Iterator it = springs.iterator();
				while (it.hasNext())
				{
					Spring element = (Spring)it.next();
					size = Math.max(size, element.getValue());
				}
			}
//Debug.trace("Barsize - size " + size);			
			return size;
		}
	}

	public void extendTabSequence(List<Component> tabSequence, IFormUIInternal containerImpl)
	{
		SwingForm sf = (SwingForm)containerImpl;
		JComponent west = sf.getWest();
		if (west != null)
		{
			if (west instanceof ISupplyFocusChildren)
			{
				ISupplyFocusChildren<Component> s = (ISupplyFocusChildren<Component>)west;
				Component[] fchilds = s.getFocusChildren();
				for (Component element : fchilds)
				{
					tabSequence.add(element);
				}
			}
		}
	}

	public void applyTabSequence(List<Component> tabSequence, IFormUIInternal containerImpl)
	{
		SwingForm sf = (SwingForm)containerImpl;
		sf.setTabSeqComponents(tabSequence);
		if (tabSequence != null) sf.setFocusTraversalPolicy(ServoyFocusTraversalPolicy.datarenderPolicy);
		else sf.setFocusTraversalPolicy(ServoyFocusTraversalPolicy.defaultPolicy);
	}

	public void reapplyTabSequence(IFormUIInternal containerImpl, int newBaseTabSequenceIndex)
	{
		// ignore: used only in web client
	}

}
