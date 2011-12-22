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
package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;

import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportPrinting;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.server.headlessclient.TabIndexHelper;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.server.headlessclient.WrapperContainer;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.util.IAnchorConstants;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.OrientationApplier;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.TabSequenceHelper;
import com.servoy.j2db.util.Utils;

/**
 * The web implementation of the {@link IDataRendererFactory}
 * 
 * @author jcompagner
 */
public class WebDataRendererFactory implements IDataRendererFactory<Component>
{

	/**
	 * 
	 */
	public WebDataRendererFactory()
	{
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IDataRendererFactory#completeRenderers(com.servoy.j2db.IApplication, com.servoy.j2db.persistence.Form,
	 * com.servoy.j2db.IScriptExecuter, java.util.Map, int, boolean, com.servoy.j2db.ControllerUndoManager)
	 */
	public Map completeRenderers(IApplication application, Form form, IScriptExecuter scriptExecuter, Map emptyDataRenderers, int width, boolean printing,
		ControllerUndoManager undoManager, TabSequenceHelper<Component> tabSequence) throws Exception
	{
		int partHeight = 0;
		Iterator e2 = form.getParts();
		while (e2.hasNext())
		{
			Part part = (Part)e2.next();
			Color bg = ComponentFactory.getPartBackground(application, part, form);
			if (form.getTransparent()) bg = null;
			if (bg == null && printing) bg = Color.white;

			IDataRenderer panel = (IDataRenderer)emptyDataRenderers.get(part);
			if (panel != null)
			{
				int total = Math.abs(part.getHeight() - partHeight);
				setBasicSettings(panel, bg, new Dimension(width, total), new Point(0, partHeight), printing);
			}
			partHeight = part.getHeight();
		}

		//place all the elements
		Iterator<IFormElement> e1 = form.getFormElementsSortedByFormIndex();
		return placeElements(e1, application, form, scriptExecuter, emptyDataRenderers, 0, 0, printing, undoManager, tabSequence);
	}

	private Map placeElements(Iterator<IFormElement> e1, IApplication app, Form form, IScriptExecuter listner, Map emptyDataRenderers, int XCorrection,
		int YCorrection, boolean printing, ControllerUndoManager undoManager, TabSequenceHelper<Component> tabSequence) throws Exception
	{
		final boolean useAJAX = Utils.getAsBoolean(app.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$

		IDataProviderLookup dataProviderLookup = app.getFlattenedSolution().getDataproviderLookup(app.getFoundSetManager(), form);

		Map listTocomplete = new HashMap();

		Map labelForComponents = new HashMap();

		String orientation = OrientationApplier.getHTMLContainerOrientation(app.getLocale(), app.getSolution().getTextOrientation());
		boolean leftToRight = !"rtl".equalsIgnoreCase(orientation); //$NON-NLS-1$

		boolean isAnchoringEnabled = Utils.getAsBoolean(app.getRuntimeProperties().get("enableAnchors")); //$NON-NLS-1$

//		Insets insets = new Insets(0, 0, 0, 0);
		while (e1.hasNext())
		{
			Point l = null;
			IFormElement obj = e1.next();
			l = (obj).getLocation();

			if (l == null) continue;//unknown where to add
			if (printing && obj instanceof ISupportPrinting)
			{
				if (!((ISupportPrinting)obj).getPrintable()) continue;
			}

			Iterator it = emptyDataRenderers.values().iterator();
			while (it.hasNext())
			{
				WebDataRenderer panel = (WebDataRenderer)it.next();
//					Border border = panel.getBorder();
//					if (border instanceof EmptyBorder)
//					{
//						insets = ((EmptyBorder)border).getBorderInsets();
//					}

				int start = panel.getLocation().y;
				if (l.y >= start && l.y < start + panel.getSize().height)
				{
					org.apache.wicket.Component comp = (org.apache.wicket.Component)ComponentFactory.createComponent(app, form, obj, dataProviderLookup,
						listner, printing);
					// Test for a visible bean, then get the real component
//						if (comp instanceof VisibleBean)
//						{
//							comp = ((VisibleBean)comp).getDelegate();
//						}

					if (comp != null)
					{
						if (obj instanceof Field)
						{
							String name = ((Field)obj).getName();
							if (name != null && !"".equals(name))
							{
								labelForComponents.put(name, comp);
							}
						}
						else if (obj instanceof GraphicalComponent && (comp instanceof WebBaseLabel || comp instanceof WebBaseSubmitLink))
						{
							String labelFor = ((GraphicalComponent)obj).getLabelFor();
							if (labelFor != null && !"".equals(labelFor))
							{
								labelForComponents.put(comp, labelFor);
							}

						}
						if ((obj instanceof ISupportTabSeq) && (tabSequence != null))
						{
							tabSequence.add(panel, (ISupportTabSeq)obj, comp);
						}
						org.apache.wicket.Component newComp = comp;
						if (newComp instanceof IDisplay)
						{
							panel.addDisplayComponent(obj, (IDisplay)newComp);
						}
						else if (newComp instanceof WebImageBeanHolder)
						{
							WebImageBeanHolder wiBeanHolder = (WebImageBeanHolder)newComp;
							Object bean = wiBeanHolder.getDelegate();
							if (bean instanceof IServoyAwareBean)
							{
								IServoyAwareBean ourBean = (IServoyAwareBean)bean;
								panel.addDisplayComponent(obj, ourBean);
							}
						}
						((IComponent)comp).setLocation(new Point((l.x /* +insets.left */) + XCorrection, (l.y - start) + YCorrection));

						if (form.getOnRecordEditStartMethodID() > 0 && comp instanceof IFieldComponent)
						{
							if (useAJAX && comp instanceof IDisplayData && ((IDisplayData)comp).getDataProviderID() != null &&
								!ScopesUtils.isVariableScope(((IDisplayData)comp).getDataProviderID()))
							{
								StartEditOnFocusGainedEventBehavior.addNewBehaviour(comp);
							}
						}
						// For some components, if anchoring is enabled, we need to add a wrapper <div> for anchoring to work:
						// - some of the fields
						// - buttons
						// - beans
						if (isAnchoringEnabled &&
							(((obj instanceof Field) && TemplateGenerator.needsWrapperDivForAnchoring((Field)obj)) || (obj instanceof Bean) || ((obj instanceof GraphicalComponent) && ComponentFactory.isButton((GraphicalComponent)obj))))
						{
							MarkupContainer compWrapper = new WrapperContainer(ComponentFactory.getWebID(null, obj) + TemplateGenerator.WRAPPER_SUFFIX, comp);
							Dimension s = (obj).getSize();
							int anchors = 0;
							if (obj instanceof ISupportAnchors) anchors = ((ISupportAnchors)obj).getAnchors();
							int offsetWidth = s.width;
							int offsetHeight = s.height;
							if (comp instanceof ISupportWebBounds)
							{
								Rectangle b = ((ISupportWebBounds)comp).getWebBounds();
								offsetWidth = b.width;
								offsetHeight = b.height;
							}
							final String styleToReturn = computeWrapperDivStyle(l.y, l.x, offsetWidth, offsetHeight, s.width, s.height, anchors, start, start +
								panel.getSize().height, panel.getSize().width, leftToRight);
							// first the default
							compWrapper.add(new StyleAppendingModifier(new AbstractReadOnlyModel<String>()
							{
								@Override
								public String getObject()
								{
									return styleToReturn;
								}
							}));
							// then the style t hat can be set on the wrapped component
							compWrapper.add(StyleAttributeModifierModel.INSTANCE);
							// TODO: this needs to be done in a cleaner way. See what is the relation between
							// margin, padding and border when calculating the websize in ChangesRecorder vs. TemplateGenerator.
							// Looks like one of the three is not taken into account during calculations. For now decided to remove
							// the margin and leave the padding and border.
							comp.add(new StyleAppendingModifier(new AbstractReadOnlyModel<String>()
							{
								@Override
								public String getObject()
								{
									return "margin: 0px;"; //$NON-NLS-1$
								}
							}));
							compWrapper.add(comp);
							panel.add(compWrapper);
						}
						else
						{
							panel.add(comp);
						}
					}
				}
			}
		}

		Iterator it = labelForComponents.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry entry = (Entry)it.next();
			Object key = entry.getKey();
			if (key instanceof WebBaseLabel || key instanceof WebBaseSubmitLink)
			{
				IFieldComponent component = (IFieldComponent)labelForComponents.get(entry.getValue());
				if (component != null)
				{
					if (key instanceof WebBaseLabel)
					{
						((WebBaseLabel)entry.getKey()).setLabelFor(component);
					}
					else
					{
						((WebBaseSubmitLink)entry.getKey()).setLabelFor(component);
					}
					(component).addLabelFor((ILabel)entry.getKey());
					if (!component.isVisible())
					{
						component.setComponentVisible(component.isVisible());
					}
					if (!component.isEnabled())
					{
						component.setComponentEnabled(component.isEnabled());
					}
				}
			}
		}

		it = emptyDataRenderers.values().iterator();
		while (it.hasNext())
		{
			WebDataRenderer panel = (WebDataRenderer)it.next();
			panel.createDataAdapter(app, dataProviderLookup, listner, undoManager);
		}
		return listTocomplete;
	}

	private String computeWrapperDivStyle(int top, int left, int width, int height, int offsetWidth, int offsetHeight, int anchorFlags, int partStartY,
		int partEndY, int partWidth, boolean leftToRight)
	{
		Hashtable<String, String> style = new Hashtable<String, String>();
		if (top != -1) style.put("top", top + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		if (left != -1) style.put("left", left + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		if (width != -1) style.put("width", width + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		if (height != -1) style.put("height", height + "px"); //$NON-NLS-1$ //$NON-NLS-2$

		boolean anchoredTop = (anchorFlags & IAnchorConstants.NORTH) != 0;
		boolean anchoredRight = (anchorFlags & IAnchorConstants.EAST) != 0;
		boolean anchoredBottom = (anchorFlags & IAnchorConstants.SOUTH) != 0;
		boolean anchoredLeft = (anchorFlags & IAnchorConstants.WEST) != 0;

		if (!anchoredLeft && !anchoredRight) anchoredLeft = true;
		if (!anchoredTop && !anchoredBottom) anchoredTop = true;

		int deltaLeft = leftToRight ? 0 : offsetWidth - width;
		int deltaRight = leftToRight ? offsetWidth - width : 0;
		int deltaBottom = offsetHeight - height;

		if (anchoredTop) style.put("top", (top - partStartY) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		else style.remove("top"); //$NON-NLS-1$
		if (anchoredBottom) style.put("bottom", (partEndY - top - offsetHeight + deltaBottom) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!anchoredTop || !anchoredBottom) style.put("height", height + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		else style.remove("height"); //$NON-NLS-1$
		if (anchoredLeft) style.put("left", (left + deltaLeft) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		else style.remove("left"); //$NON-NLS-1$
		if (anchoredRight) style.put("right", (partWidth - left - offsetWidth + deltaRight) + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!anchoredLeft || !anchoredRight) style.put("width", width + "px"); //$NON-NLS-1$ //$NON-NLS-2$
		else style.remove("width"); //$NON-NLS-1$
		style.put("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$

		StringBuffer sb = new StringBuffer();
		for (String key : style.keySet())
		{
			String value = style.get(key);
			sb.append(key);
			sb.append(": "); //$NON-NLS-1$
			sb.append(value);
			sb.append("; "); //$NON-NLS-1$
		}
		return sb.toString();
	}

	private void setBasicSettings(IDataRenderer dr, Color bg, Dimension size, Point location, boolean printing)
	{
		if (dr != null)
		{
			if (bg != null)
			{
				dr.setBackground(bg);
			}
			// printing is not possible in web
//			if (printing)
//			{
//				dr.setLayout(new SpringLayout());
//			}
//			else
//			{
//				dr.setLayout(new AnchorLayout(size));
//			}
			dr.setSize(size);
			dr.setLocation(location);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IDataRendererFactory#getEmptyDataRenderer(java.lang.String, com.servoy.j2db.IApplication)
	 */
	public IDataRenderer getEmptyDataRenderer(String id, String name, IApplication application, boolean showSelection)
	{
		return new WebDataRenderer(id, name, application);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IDataRendererFactory#createPortalRenderer(com.servoy.j2db.IApplication, com.servoy.j2db.persistence.Portal,
	 * com.servoy.j2db.persistence.Form, com.servoy.j2db.IScriptExecuter, boolean, com.servoy.j2db.ControllerUndoManager)
	 */
	public IDataRenderer createPortalRenderer(IApplication app, Portal meta, Form form, IScriptExecuter el, boolean printing, ControllerUndoManager undoManager)
		throws Exception
	{
		return null;
	}

	public void extendTabSequence(List<Component> tabSequence, IFormUIInternal containerImpl)
	{
		WebForm wf = (WebForm)containerImpl;
		ISupplyFocusChildren<Component> defaultNav = wf.getDefaultNavigator();
		if (defaultNav != null)
		{
			Component[] fchilds = defaultNav.getFocusChildren();
			for (Component element : fchilds)
				tabSequence.add(element);
		}
	}

	public void applyTabSequence(List<Component> tabSequence, IFormUIInternal containerImpl)
	{
		WebForm wf = (WebForm)containerImpl;
		wf.setTabSeqComponents(tabSequence);
	}

	private int goDownTabPanel(WebTabPanel wtp, int goodTabIndex)
	{
		int localTabIndex = goodTabIndex;
		wtp.setTabIndex(goodTabIndex >= 0 ? localTabIndex++ : ISupportWebTabSeq.SKIP);
		TabIndexHelper.setUpTabIndexAttributeModifier(wtp, ISupportWebTabSeq.SKIP);
		Iterator it = wtp.iterator();
		while (it.hasNext())
		{
			Object o = it.next();
			if (o instanceof WebForm) localTabIndex = goDownForm((WebForm)o, goodTabIndex >= 0 ? localTabIndex : ISupportWebTabSeq.SKIP);
		}
		return localTabIndex;
	}

	private int goDownForm(WebForm wf, int goodTabIndex)
	{
		int localTabIndex = goodTabIndex;
		List<Component> tabSeq = wf.getTabSeqComponents();
		Iterator<Component> it = tabSeq.iterator();
		while (it.hasNext())
		{
			Component oo = it.next();
			if (oo instanceof WebTabPanel)
			{
				localTabIndex = goDownTabPanel((WebTabPanel)oo, goodTabIndex >= 0 ? localTabIndex : ISupportWebTabSeq.SKIP);
			}
			else if (oo instanceof WebCellBasedView)
			{
				WebCellBasedView tableView = (WebCellBasedView)oo;
				tableView.setTabIndex(goodTabIndex >= 0 ? localTabIndex : ISupportWebTabSeq.SKIP);
				localTabIndex += WebForm.SEQUENCE_RANGE_TABLE;
				TabIndexHelper.setUpTabIndexAttributeModifier(oo, ISupportWebTabSeq.SKIP);
			}
			else
			{
				TabIndexHelper.setUpTabIndexAttributeModifier(oo, goodTabIndex >= 0 ? localTabIndex++ : ISupportWebTabSeq.SKIP);
			}
		}
		return localTabIndex;
	}

	public void reapplyTabSequence(IFormUIInternal containerImpl, int delta)
	{
		WebForm wf = (WebForm)containerImpl;

		WebForm currentForm = wf;
		MarkupContainer currentTabPanel = null;
		boolean ready = false;
		int counter = delta + 1;
		do
		{
			List<Component> existingTabSequence = currentForm.getTabSeqComponents();
			Iterator<Component> iter = existingTabSequence.iterator();

			if (!containerImpl.equals(currentForm))
			{
				while (iter.hasNext())
				{
					Component c = iter.next();
					if (c.equals(currentTabPanel))
					{
						break;
					}
				}
			}

			while (iter.hasNext())
			{
				Component comp = iter.next();

				if (comp instanceof WebTabPanel)
				{
					WebTabPanel wtp = (WebTabPanel)comp;
					counter = goDownTabPanel(wtp, delta >= 0 ? counter : ISupportWebTabSeq.SKIP);
				}
				else if (comp instanceof WebCellBasedView)
				{
					WebCellBasedView tableView = (WebCellBasedView)comp;
					tableView.setTabIndex(delta >= 0 ? counter : ISupportWebTabSeq.SKIP);
					counter += WebForm.SEQUENCE_RANGE_TABLE;
					TabIndexHelper.setUpTabIndexAttributeModifier(comp, ISupportWebTabSeq.SKIP);
				}
				else
				{
					TabIndexHelper.setUpTabIndexAttributeModifier(comp, delta >= 0 ? counter++ : ISupportWebTabSeq.SKIP);
				}
			}

			MarkupContainer parent = currentForm.getParent();
			while ((parent != null) && !(parent instanceof WebTabPanel) && !(parent instanceof WebSplitPane))
				parent = parent.getParent();
			if (parent != null)
			{
				if (parent instanceof WebSplitPane)
				{
					((WebSplitPane)parent).setFormLastTabIndex(currentForm, counter - 1);
				}

				currentTabPanel = parent;
				MarkupContainer tabParent = currentTabPanel.getParent();
				while ((tabParent != null) && !(tabParent instanceof WebForm))
					tabParent = tabParent.getParent();
				if (tabParent != null)
				{
					currentForm = (WebForm)tabParent;
				}
				else
				{
					ready = true;
				}
			}
			else
			{
				ready = true;
			}
		}
		while (!ready);
	}
}
