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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.xhtmlrenderer.css.constants.CSSName;

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
import com.servoy.j2db.persistence.ISupportPrinting;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.server.headlessclient.TabIndexHelper;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormUI;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.OrientationApplier;
import com.servoy.j2db.util.Pair;
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

	public static final int CONTAINER_RESERVATION_GAP = 50;
	public static final int MAXIMUM_TAB_INDEXES_ON_TABLEVIEW = 500;

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
			if (bg != null)
			{
				Pair<IStyleSheet, IStyleRule> formStyle = ComponentFactory.getCSSPairStyleForForm(application, form);
				if (formStyle != null && formStyle.getRight() != null && formStyle.getRight().hasAttribute(CSSName.BACKGROUND_IMAGE.toString()))
				{
					bg = null;
				}
			}
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
							(((obj instanceof Field) && WebAnchoringHelper.needsWrapperDivForAnchoring((Field)obj)) || (obj instanceof Bean) || ((obj instanceof GraphicalComponent) && ComponentFactory.isButton((GraphicalComponent)obj))))
						{
							panel.add(WebAnchoringHelper.getWrapperComponent(comp, obj, start, panel.getSize(), leftToRight));
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

	private void setBasicSettings(IDataRenderer dr, Color bg, Dimension size, Point location, boolean printing)
	{
		if (dr != null)
		{
			dr.setBackground(bg);
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

	private int goDownContainer(IWebFormContainer wtp, int goodTabIndex)
	{
		int localTabIndex = goodTabIndex;
		wtp.setTabSequenceIndex(goodTabIndex >= 0 ? localTabIndex++ : ISupportWebTabSeq.SKIP);
		TabIndexHelper.setUpTabIndexAttributeModifier((Component)wtp, ISupportWebTabSeq.SKIP);
		IFormUI[] forms = wtp.getChildForms();
		if (forms != null)
		{
			for (IFormUI form : forms)
			{
				if (form != null) localTabIndex = goDownForm((WebForm)form, goodTabIndex >= 0 ? localTabIndex : ISupportWebTabSeq.SKIP);
			}
		}
		if (goodTabIndex >= 0)
		{
			localTabIndex = getContainerGapIndex(localTabIndex, goodTabIndex);
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
			if (oo instanceof IWebFormContainer)
			{
				localTabIndex = goDownContainer((IWebFormContainer)oo, goodTabIndex >= 0 ? localTabIndex : ISupportWebTabSeq.SKIP);
			}
			else if (oo instanceof WebCellBasedView)
			{
				WebCellBasedView tableView = (WebCellBasedView)oo;
				tableView.setTabSequenceIndex(goodTabIndex >= 0 ? localTabIndex : ISupportWebTabSeq.SKIP);
				localTabIndex += MAXIMUM_TAB_INDEXES_ON_TABLEVIEW;
				TabIndexHelper.setUpTabIndexAttributeModifier(oo, ISupportWebTabSeq.SKIP);
			}
			else
			{
				TabIndexHelper.setUpTabIndexAttributeModifier(oo, goodTabIndex >= 0 ? localTabIndex++ : ISupportWebTabSeq.SKIP);
			}
		}
		return localTabIndex;
	}

	/**
	 * This method is the real tabIndex calculator for all forms in containers. When a container form is made visible, tabIndexes must be recalculated.
	 * Having the tab sequence tree, the tab sequence order is the preorder traversal of the tree. All elements after modified container in tab sequence order will be recalculated.
	 * A small gap for each container is used, so that chances that tabIndex should be changed when visible tab is different are smaller.
	 * Browsers cannot deal with really big numbers of tabIndex (maximum is around 32k). WebCellBasedView is also a special case, some bigger gap is used for that (it will fill tabIndexes itself).
	 */
	public void reapplyTabSequence(IFormUIInternal containerImpl, int delta)
	{
		WebForm wf = (WebForm)containerImpl;

		WebForm currentForm = wf;
		IWebFormContainer currentTabPanel = null;
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
						if (delta >= 0)
						{
							counter = getContainerGapIndex(counter, currentTabPanel.getTabSequenceIndex());
						}
						break;
					}
				}
			}

			while (iter.hasNext())
			{
				Component comp = iter.next();

				if (comp instanceof IWebFormContainer)
				{
					counter = goDownContainer((IWebFormContainer)comp, delta >= 0 ? counter : ISupportWebTabSeq.SKIP);
				}
				else if (comp instanceof WebCellBasedView)
				{
					WebCellBasedView tableView = (WebCellBasedView)comp;
					tableView.setTabSequenceIndex(delta >= 0 ? counter : ISupportWebTabSeq.SKIP);
					counter += MAXIMUM_TAB_INDEXES_ON_TABLEVIEW;
					TabIndexHelper.setUpTabIndexAttributeModifier(comp, ISupportWebTabSeq.SKIP);
				}
				else
				{
					TabIndexHelper.setUpTabIndexAttributeModifier(comp, delta >= 0 ? counter++ : ISupportWebTabSeq.SKIP);
				}
			}

			if (delta >= 0)
			{
				counter = getContainerGapIndex(counter, delta);
			}

			MarkupContainer parent = currentForm.getParent();
			while ((parent != null) && !(parent instanceof IWebFormContainer))
				parent = parent.getParent();
			if (parent != null)
			{
				if (parent instanceof WebSplitPane)
				{
					((WebSplitPane)parent).setFormLastTabIndex(currentForm, counter - 1);
				}

				currentTabPanel = (IWebFormContainer)parent;
				MarkupContainer tabParent = ((Component)currentTabPanel).getParent();
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

	public static int getContainerGapIndex(int realIndex, int offsetIndex)
	{
		return Math.max(offsetIndex + CONTAINER_RESERVATION_GAP, realIndex);
	}
}
