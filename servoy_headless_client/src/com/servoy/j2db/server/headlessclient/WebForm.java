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
package com.servoy.j2db.server.headlessclient;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.SimpleDoc;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.IMarkupCacheKeyProvider;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.ClientInfo;
import org.apache.wicket.util.string.UrlUtils;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.DesignModeCallbacks;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IView;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.printing.FormPreviewPanel;
import com.servoy.j2db.printing.PageList;
import com.servoy.j2db.printing.PrintPreview;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.scripting.RuntimeGroup;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.server.headlessclient.FormAnchorInfo.FormPartAnchorInfo;
import com.servoy.j2db.server.headlessclient.dataui.ChangesRecorder;
import com.servoy.j2db.server.headlessclient.dataui.FormLayoutProviderFactory;
import com.servoy.j2db.server.headlessclient.dataui.IFormLayoutProvider;
import com.servoy.j2db.server.headlessclient.dataui.ISupportWebTabSeq;
import com.servoy.j2db.server.headlessclient.dataui.RecordItemModel;
import com.servoy.j2db.server.headlessclient.dataui.StyleAppendingModifier;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;
import com.servoy.j2db.server.headlessclient.dataui.WebBeanHolder;
import com.servoy.j2db.server.headlessclient.dataui.WebCellBasedView;
import com.servoy.j2db.server.headlessclient.dataui.WebDataRenderer;
import com.servoy.j2db.server.headlessclient.dataui.WebDefaultRecordNavigator;
import com.servoy.j2db.server.headlessclient.dataui.WebImageBeanHolder;
import com.servoy.j2db.server.headlessclient.dataui.WebRecordView;
import com.servoy.j2db.server.headlessclient.dataui.WebSplitPane;
import com.servoy.j2db.server.headlessclient.dataui.WebTabFormLookup;
import com.servoy.j2db.server.headlessclient.dataui.WebTabPanel;
import com.servoy.j2db.smart.plugins.PluginManager;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptReadOnlyMethods;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.ui.scripting.RuntimePortal;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IAnchorConstants;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.OrientationApplier;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * 
 */
public class WebForm extends Panel implements IFormUIInternal<Component>, IMarkupCacheKeyProvider, IProviderStylePropertyChanges
{
	private static final long serialVersionUID = 1L;
	/**
	 * Tab sequence index space allowed for splitpanes.
	 */
	public static final int SEQUENCE_RANGE_SPLIT_PANE = 2000;
	/**
	 * Tab sequence index space allowed for tabpanels.
	 */
	public static final int SEQUENCE_RANGE_TAB_PANEL = 1500;
	/**
	 * Tab sequence index space allowed for web cell based views.
	 */
	public static final int SEQUENCE_RANGE_TABLE = 1000;

	private final String variation;
	private final FormController formController;

	private Point location;
	private Dimension size;
	private int formWidth = 0;
	private int formHeight = 0;
	private Component previousParent;
	private Color cfg;
	private Color cbg;
	private Font font;
	private Border border;
	private Cursor cursor;
	private boolean opaque;
	private String tooltip;
	private final WebMarkupContainer container;
	private boolean readonly;
	// the list of the components that will be marked to readonly
	private final List<Component> markedComponents;

	private List<Component> tabSeqComponentList = new ArrayList<Component>();
	private WebDefaultRecordNavigator defaultNavigator = null;

	private boolean destroyed;
	private IView view;

	private boolean isFormWidthHeightChanged;

	/**
	 * @param id
	 */
	public WebForm(final FormController controller)
	{
		super("webform"); //$NON-NLS-1$
		markedComponents = new ArrayList<Component>();
		TabIndexHelper.setUpTabIndexAttributeModifier(this, ISupportWebTabSeq.SKIP);
		this.variation = "form::" + controller.getForm().getSolution().getName() + ":" + controller.getName() + "::form"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		this.formController = controller;
		final IFormLayoutProvider layoutProvider = FormLayoutProviderFactory.getFormLayoutProvider(formController.getApplication(),
			formController.getApplication().getSolution(), formController.getForm(), formController.getName());
		TextualStyle panelStyle = layoutProvider.getLayoutForForm(0, false, true); // custom navigator is dropped inside tab panel.
		add(new StyleAppendingModifier(panelStyle)
		{
			@Override
			public boolean isEnabled(Component component)
			{
				return (component.findParent(WebTabPanel.class) != null) || (component.findParent(WebSplitPane.class) != null);
			}
		});

		container = new WebMarkupContainer("servoywebform"); //$NON-NLS-1$
		container.add(new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				WebTabPanel tabpanel = findParent(WebTabPanel.class);
				if (tabpanel != null)
				{
					return "min-width:0px;min-height:0px;";
				}
				return null;
			}
		}));
		// we need to explicitly make the form transparent, to override the
		// white color from the default CSS (the #webform class)
		// case 349263
		add(new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				WebTabPanel tabpanel = findParent(WebTabPanel.class);
				if (tabpanel != null)
				{
					if (controller.getForm().getTransparent() && !tabpanel.isOpaque())
					{
						return "background-color:transparent;";
					}
				}
				return null;
			}
		}));
		add(new AttributeAppender("class", new Model<String>()
		{
			private static final long serialVersionUID = 1332637522687352873L;

			@Override
			public String getObject()
			{
				return "yui-skin-sam";
			}
		}, " ")
		{
			@Override
			public boolean isEnabled(Component component)
			{
				return (component instanceof WebForm && ((WebForm)component).isDesignMode());
			}
		});
		// set fixed markup id so that element can always be found by markup id
		container.setOutputMarkupId(true);
		container.setMarkupId("form_" + ComponentFactory.stripIllegalCSSChars(formController.getName())); // same as in template generator //$NON-NLS-1$
		TabIndexHelper.setUpTabIndexAttributeModifier(container, ISupportWebTabSeq.SKIP);
		add(container);
		readonly = false;
		setOutputMarkupId(true);
	}

	public String getContainerMarkupId()
	{
		return container.getMarkupId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.wicket.MarkupContainer#hasAssociatedMarkup()
	 */
	@Override
	public boolean hasAssociatedMarkup()
	{
		return true;
	}

	/**
	 * @see com.servoy.j2db.IFormUI#getFormContext()
	 */
	public JSDataSet getFormContext()
	{
		WebForm current = this;
		WebTabPanel currentTabPanel = null;
		String currentBeanName = null;
		WebSplitPane currentSplitPane = null;
		IDataSet set = new BufferedDataSet(
			new String[] { "containername", "formname", "tabpanel/splitpane/beanname", "tabname", "tabindex" }, new ArrayList<Object[]>()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		set.addRow(new Object[] { null, current.formController.getName(), null, null, null });
		MarkupContainer parent = getParent();
		while (parent != null)
		{
			if (parent instanceof WebTabPanel)
			{
				currentTabPanel = (WebTabPanel)parent;
			}
			else if (parent instanceof WebSplitPane)
			{
				currentSplitPane = (WebSplitPane)parent;
			}
			else if (parent instanceof IServoyAwareBean && parent instanceof IComponent)
			{
				currentBeanName = ((IComponent)parent).getName();
			}
			else if (parent instanceof WebForm)
			{
				if (currentTabPanel != null)
				{
					int index = -1;
					String tabName = null;
					index = currentTabPanel.getTabIndex(current);
					if (index != -1)
					{
						tabName = currentTabPanel.getTabNameAt(index); // js method so +1
					}
					current = (WebForm)parent;
					set.addRow(0, new Object[] { null, current.formController.getName(), currentTabPanel.getName(), tabName, new Integer(index) });
				}
				else if (currentBeanName != null)
				{
					current = (WebForm)parent;
					set.addRow(0, new Object[] { null, current.formController.getName(), currentBeanName, null, null });
				}
				else if (currentSplitPane != null)
				{
					boolean isLeftForm = currentSplitPane.getLeftForm() != null &&
						current.equals(((WebTabFormLookup)currentSplitPane.getLeftForm()).getWebForm());
					current = (WebForm)parent;
					set.addRow(0, new Object[] { null, current.formController.getName(), currentSplitPane.getName(), null, new Integer(isLeftForm ? 1 : 2) });
				}
				current = (WebForm)parent;
				currentTabPanel = null;
				currentBeanName = null;
				currentSplitPane = null;
			}
			else if (parent instanceof MainPage)
			{
				String containerName = ((MainPage)parent).getContainerName();
				if (containerName != null)
				{
					for (int i = 0; i < set.getRowCount(); i++)
					{
						set.getRow(i)[0] = containerName;
					}
				}
				return new JSDataSet(formController.getApplication(), set);
			}
			parent = parent.getParent();
		}
		return new JSDataSet(formController.getApplication(), set);
	}

	/**
	 * @see org.apache.wicket.markup.IMarkupCacheKeyProvider#getCacheKey(org.apache.wicket.MarkupContainer, java.lang.Class)
	 */
	public String getCacheKey(MarkupContainer container, Class containerClass)
	{
		return variation;
	}

	private transient Markup markup = null;

	/**
	 * @see org.apache.wicket.MarkupContainer#getAssociatedMarkupStream(boolean)
	 */
	@Override
	public MarkupStream getAssociatedMarkupStream(boolean throwException)
	{
		if (markup == null)
		{
			markup = getApplication().getMarkupSettings().getMarkupCache().getMarkup(this, getClass(), false);
		}
		return new MarkupStream(markup);
	}

	public void setTabSeqComponents(List<Component> tabSequence)
	{
		boolean defaultSequence = true;
		if (tabSequence != null && tabSequence.size() > 0)
		{
			defaultSequence = false;
			IDataRenderer formEditorRenderer = formController.getDataRenderers()[FormController.FORM_EDITOR];
			if (formEditorRenderer instanceof WebCellBasedView && !tabSequence.contains(formEditorRenderer))
			{
				// this means that we have to identify components that are part of the table view and based on where these are located set the view's tabIndex;
				// when called from JS tabSequence will only contain table columns, not table view as opposed to initialization when tabSequence only contains the view
				int i;
				for (i = 0; i < tabSequence.size(); i++)
				{
					if (((WebCellBasedView)formEditorRenderer).isColumnIdentifierComponent(tabSequence.get(i)))
					{
						// table view should be added to tab sequence
						((WebCellBasedView)formEditorRenderer).setTabSeqComponents(tabSequence);

						tabSequence = new ArrayList<Component>(tabSequence);
						tabSequence.add(i, (Component)formEditorRenderer);
						break;
					}
				}
				i++;
				while (i < tabSequence.size())
				{
					if (((WebCellBasedView)formEditorRenderer).isColumnIdentifierComponent(tabSequence.get(i)))
					{
						tabSequence.remove(i);
					}
					else
					{
						i++;
					}
				}
			}

			int tabIndex = 1;
			for (Component comp : tabSequence)
			{
				if (comp instanceof WebTabPanel)
				{
					WebTabPanel wtp = (WebTabPanel)comp;
					wtp.setTabIndex(tabIndex);
					tabIndex += SEQUENCE_RANGE_TAB_PANEL;
					TabIndexHelper.setUpTabIndexAttributeModifier(comp, ISupportWebTabSeq.SKIP);
				}
				else if (comp instanceof WebCellBasedView)
				{
					WebCellBasedView tableView = (WebCellBasedView)comp;
					tableView.setTabIndex(tabIndex);
					tabIndex += SEQUENCE_RANGE_TABLE;
					TabIndexHelper.setUpTabIndexAttributeModifier(comp, ISupportWebTabSeq.SKIP);
				}
				else if (comp instanceof WebSplitPane)
				{
					WebSplitPane wsp = (WebSplitPane)comp;
					wsp.setTabIndex(tabIndex);
					tabIndex += SEQUENCE_RANGE_SPLIT_PANE;
					TabIndexHelper.setUpTabIndexAttributeModifier(comp, ISupportWebTabSeq.SKIP);
				}
				else
				{
					TabIndexHelper.setUpTabIndexAttributeModifier(comp, tabIndex);
					tabIndex++;
				}
			}
		}

		IDataRenderer[] dataRenderers = formController.getDataRenderers();
		for (IDataRenderer dr : dataRenderers)
		{
			if (dr != null)
			{
				// set attributeModifiers for all components
				if (dr instanceof WebCellBasedView)
				{
					if (defaultSequence)
					{
						// normal focus
						((WebCellBasedView)dr).setTabIndex(ISupportWebTabSeq.DEFAULT);
						((WebCellBasedView)dr).setTabSeqComponents(null);
					}
					else if (!tabSequence.contains(dr))
					{
						// it shouldn't gain focus when tabbing ...
						((WebCellBasedView)dr).setTabIndex(-1);
						((WebCellBasedView)dr).setTabSeqComponents(null);
					}
				}
				else
				{
					Iterator compIt = dr.getComponentIterator();
					if (compIt != null)
					{
						while (compIt.hasNext())
						{
							Object obj = compIt.next();
							if (obj instanceof ISupplyFocusChildren)
							{
								for (Component c : ((ISupplyFocusChildren<Component>)obj).getFocusChildren())
								{
									checkIfDefaultOrSkipFocus(defaultSequence, tabSequence, c);
								}
							}
							else if (obj instanceof Component)
							{
								checkIfDefaultOrSkipFocus(defaultSequence, tabSequence, (Component)obj);
							}
						}
					}
				}
			}
		}
		tabSeqComponentList = tabSequence;
	}

	private void checkIfDefaultOrSkipFocus(boolean defaultSequence, List<Component> tabSequence, Component c)
	{
		if (defaultSequence)
		{
			TabIndexHelper.setUpTabIndexAttributeModifier(c, ISupportWebTabSeq.DEFAULT);
		}
		else
		{
			boolean exists = false;
			for (Component entry : tabSequence)
			{
				if (entry.equals(c))
				{
					exists = true;
					break;
				}
			}
			if (!exists)
			{
				TabIndexHelper.setUpTabIndexAttributeModifier(c, -1);
			}
		}
	}

	public List<Component> getTabSeqComponents()
	{
		return tabSeqComponentList;
	}

	public ISupplyFocusChildren<Component> getDefaultNavigator()
	{
		return defaultNavigator;
	}

	public boolean isTraversalPolicyEnabled()
	{
		return true;
	}

	/**
	 * @see wicket.Component#getStyle()
	 */
	@Override
	public String getVariation()
	{
		return variation;
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#initView(com.servoy.j2db.IApplication, com.servoy.j2db.FormController, int)
	 */
	public IView initView(IApplication app, FormController fp, int viewType)
	{
		boolean addHeaders = true;
		view = null;
		final Form f = fp.getForm();
		DataRendererRecordModel rendererModel = new DataRendererRecordModel();

//		int viewType = f.getView();
		if (viewType == IForm.LIST_VIEW || viewType == FormController.LOCKED_LIST_VIEW)
		{
			viewType = FormController.LOCKED_TABLE_VIEW;
			addHeaders = false;//list views do not have headers
		}

		String orientation = OrientationApplier.getHTMLContainerOrientation(app.getLocale(), app.getSolution().getTextOrientation());
		IDataRenderer[] dataRenderers = fp.getDataRenderers();
		for (int i = 0; i < dataRenderers.length; i++)
		{
			if (i == Part.TITLE_HEADER || i == Part.HEADER || i == Part.LEADING_GRAND_SUMMARY || i == Part.TRAILING_GRAND_SUMMARY || i == Part.FOOTER)
			{
				if (dataRenderers[Part.HEADER] != null)
				{
					addHeaders = false;
				}

				WebDataRenderer dr = (WebDataRenderer)dataRenderers[i];
				if (dr != null)
				{
					dr.setDefaultModel(rendererModel);
					container.add(dr);
				}
			}
		}

		defaultNavigator = null;
		if (viewType == IForm.RECORD_VIEW || viewType == IForm.LOCKED_RECORD_VIEW)
		{
			view = new WebRecordView("View"); //$NON-NLS-1$
			if (f.getNavigatorID() == Form.NAVIGATOR_DEFAULT)
			{
				defaultNavigator = new WebDefaultRecordNavigator(this);
				((WebRecordView)view).add(defaultNavigator);
			}

			WebDataRenderer body = (WebDataRenderer)dataRenderers[FormController.FORM_EDITOR];//Body
			if (body != null)
			{
				final int scrollBars = f.getScrollbars();

				((WebRecordView)view).add(body);
				body.setDefaultModel(rendererModel);
				body.setParentView(view);
			}
			else
			{
				//((WebRecordView)view).add(new WebMarkupContainer());
			}
		}
		else if (viewType == FormController.TABLE_VIEW || viewType == FormController.LOCKED_TABLE_VIEW)
		{
//			try
//			{
//				Iterator it = f.getParts();
//				while (it.hasNext())
//				{
//					Part p = (Part) it.next();
//					if (p.getPartType() == Part.HEADER) 
//					{
//						addHeaders = false;
//						break;
//					}
//				}
//			}
//			catch (RepositoryException e)
//			{
//				Debug.error(e);
//			}
			Part body = null;
			Iterator<Part> e2 = fp.getForm().getParts();
			while (e2.hasNext())
			{
				Part part = e2.next();
				if (part.getPartType() == Part.BODY)
				{
					body = part;
					break;
				}
			}
			if (body == null)
			{
				// Special case, form in tableview with no body. just create a default view object.
				view = new WebRecordView("View"); //$NON-NLS-1$
				return view;
			}
			else
			{
				int startY = fp.getForm().getPartStartYPos(body.getID());
				int endY = body.getHeight();
				int sizeHint = endY;
				if ((sizeHint - startY) <= 40 && fp.getForm().getSize().height == sizeHint) // small body and body is last
				{
					sizeHint += Math.max(endY, 200 - sizeHint);
				}

				RuntimePortal viewScriptable = new RuntimePortal(new ChangesRecorder(null, null), app);
				view = new WebCellBasedView("View", app, viewScriptable, f, f, app.getFlattenedSolution().getDataproviderLookup(app.getFoundSetManager(), f),
					fp.getScriptExecuter(), addHeaders, startY, endY, sizeHint);
				viewScriptable.setComponent((WebCellBasedView)view);

				dataRenderers[FormController.FORM_EDITOR] = (WebCellBasedView)view;
			}
		}

		if (container.get("View") != null)
		{
			container.replace((WebMarkupContainer)view);
		}
		else
		{
			container.add((WebMarkupContainer)view);
		}
		return view;
	}


	/**
	 * @see com.servoy.j2db.IFormUI#getNavigatorParent() public ISupportNavigator getNavigatorParent() { MarkupContainer container = getParent();
	 *      while(container != null && !(container instanceof ISupportNavigator) ) { container = container.getParent(); } return (ISupportNavigator)container; }
	 */

	/**
	 * @see com.servoy.j2db.IFormUIInternal#getUndoManager()
	 */
	public ControllerUndoManager getUndoManager()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#destroy()
	 */
	public void destroy()
	{
		this.destroyed = true;
		if (getParent() != null && getRequestCycle() != null)
		{
			remove();
		}
	}

	/**
	 * @return the destroyed
	 */
	public boolean isDestroyed()
	{
		return destroyed;
	}

	/**
	 * Visitor for the wicket component. It will mark the components readOnly property;
	 * 
	 * @author Sisu
	 * 
	 */
	class WicketCompVisitorMarker implements IVisitor
	{
		private final List<Component> markedList;
		private final boolean readonlyFlag;

		public WicketCompVisitorMarker(List<Component> markedList, boolean readonlyFlag)
		{
			this.markedList = markedList;
			this.readonlyFlag = readonlyFlag;
		}

		public Object component(org.apache.wicket.Component component)
		{
			if (((IScriptableProvider)component).getScriptObject() instanceof IScriptReadOnlyMethods)
			{
				IScriptReadOnlyMethods scriptable = (IScriptReadOnlyMethods)((IScriptableProvider)component).getScriptObject();
				if (scriptable.js_isReadOnly() == false && readonlyFlag == true)
				{
					scriptable.js_setReadOnly(readonlyFlag);

					if (markedList.contains(component) == false)
					{
						markedList.add(component);
					}
				}
				else if (readonlyFlag == false)
				{
					if (markedList.contains(component) == true)
					{
						scriptable.js_setReadOnly(readonlyFlag);
					}
				}
			}
			return CONTINUE_TRAVERSAL;
		}

		public List<Component> getMarkedComponens()
		{
			return markedList;
		}
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#setReadOnly(boolean)
	 */
	public void setReadOnly(boolean b)
	{
		if (readonly != b)
		{
			readonly = b;
			WicketCompVisitorMarker visitorMarker = new WicketCompVisitorMarker(markedComponents, readonly);
			visitChildren(IScriptableProvider.class, visitorMarker);
			if (readonly == false)
			{
				markedComponents.clear();
			}
		}
	}

	public List<Component> getReadOnlyComponents()
	{
		return markedComponents;
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#updateFormUI()
	 */
	public void updateFormUI()
	{
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#getController()
	 */
	public FormController getController()
	{
		return formController;
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#showSortDialog(com.servoy.j2db.IApplication, java.lang.String)
	 */
	public void showSortDialog(IApplication application, String options)
	{
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setComponentVisible(boolean)
	 */
	public void setComponentVisible(boolean visible)
	{
		if (getRequestCycle() != null)//can be called when not in cycle when in shutdown
		{
			setVisible(visible);
		}
	}

	public void setComponentEnabled(final boolean b)
	{
		setEnabled(b);
		visitChildren(new IVisitor()
		{
			public Object component(org.apache.wicket.Component component)
			{
				if (component instanceof IComponent)
				{
					((IComponent)component).setComponentEnabled(b);
				}
				else
				{
					component.setEnabled(b);
				}
				return CONTINUE_TRAVERSAL;
			}

		});
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setLocation(java.awt.Point)
	 */
	public void setLocation(Point location)
	{
		this.location = location;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setSize(java.awt.Dimension)
	 */
	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public boolean isFormWidthHeightChanged()
	{
		return isFormWidthHeightChanged;
	}

	public void clearFormWidthHeightChangedFlag()
	{
		isFormWidthHeightChanged = false;
	}

	public void setFormWidth(int width)
	{
		isFormWidthHeightChanged = isFormWidthHeightChanged || (this.formWidth != width);
		this.formWidth = width;
	}

	public int getFormWidth()
	{
		return formWidth;
	}

	public int getPartHeight(int partType)
	{
		if (formController.getDataRenderers() != null && formController.getDataRenderers().length > partType)
		{
			IDataRenderer renderer = formController.getDataRenderers()[partType];
			if (renderer != null)
			{
				return renderer.getSize().height;
			}
		}
		return 0;
	}

	public void storeFormHeight(int formheight)
	{
		isFormWidthHeightChanged = isFormWidthHeightChanged || (this.formHeight != formheight);
		this.formHeight = formheight;
		int height = formheight;
		if (formController.getDataRenderers() != null)
		{
			for (int i = 0; i < formController.getDataRenderers().length; i++)
			{
				if (formController.getDataRenderers()[i] instanceof WebDataRenderer)
				{
					WebDataRenderer renderer = (WebDataRenderer)formController.getDataRenderers()[i];
					if (renderer != null && i != Part.BODY)
					{
						height -= renderer.getSize().height;
					}
				}
			}
			if (formController.getDataRenderers()[Part.BODY] instanceof WebDataRenderer)
			{
				formController.getDataRenderers()[Part.BODY].setSize(new Dimension(formController.getDataRenderers()[Part.BODY].getSize().width, height));
			}
		}
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setForeground(java.awt.Color)
	 */
	public void setForeground(Color cfg)
	{
		this.cfg = cfg;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setBackground(java.awt.Color)
	 */
	public void setBackground(Color cbg)
	{
		this.cbg = cbg;

	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setFont(java.awt.Font)
	 */
	public void setFont(Font font)
	{
		this.font = font;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setBorder(javax.swing.border.Border)
	 */
	public void setBorder(Border border)
	{
		this.border = border;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setName(java.lang.String)
	 */
	public void setName(String name)
	{
		// ignore can't be set
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getName()
	 */
	public String getName()
	{
		return getId();
	}


	/**
	 * @see com.servoy.j2db.ui.IComponent#setCursor(java.awt.Cursor)
	 */
	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setOpaque(boolean)
	 */
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setToolTipText(java.lang.String)
	 */
	public void setToolTipText(String tooltip)
	{
		if (Utils.stringIsEmpty(tooltip))
		{
			this.tooltip = null;
		}
		else
		{
			this.tooltip = tooltip;
		}
	}

	@Override
	public String toString()
	{
		return "WebForm[controller:" + getController() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	class DataRendererRecordModel extends RecordItemModel
	{
		private static final long serialVersionUID = 1L;

		/**
		 * @see com.servoy.j2db.server.headlessclient.dataui.RecordItemModel#getRecord()
		 */
		@Override
		protected IRecordInternal getRecord()
		{
			IFoundSetInternal fs = getController().getFoundSet();
			if (fs != null)
			{
				int index = fs.getSelectedIndex();
				if (index == -1)
				{
					return fs.getPrototypeState();
				}
				else
				{
					return fs.getRecord(index);
				}
			}
			else
			{
				if (getController().getApplication() instanceof WebClient && ((WebClient)getController().getApplication()).isClosing())
				{
					// client is closing so foundset is cleared
					return null;
				}
				if (getController().getApplication() instanceof SessionClient && ((SessionClient)getController().getApplication()).isShutDown())
				{
					// this is for batch processing
					return null;
				}
				if (isDestroyed())
				{
					return null;
				}
				Debug.log("No foundset in form found!", new RuntimeException()); //$NON-NLS-1$
				return null;
			}
		}
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#makeElementsScriptObject(org.mozilla.javascript.Scriptable, java.util.Map, com.servoy.j2db.ui.IDataRenderer[],
	 *      com.servoy.j2db.IView)
	 */
	public ElementScope makeElementsScriptObject(Scriptable fs, Map<String, Object[]> hmChildrenJavaMembers, IDataRenderer[] dataRenderers, IView v)
	{
		ElementScope es = new ElementScope(fs);

		int counter = 0;
		for (int i = FormController.FORM_RENDERER + 1; i < dataRenderers.length; i++)
		{
			IDataRenderer dr = dataRenderers[i];
			if (dr == null) continue;

			Object[] comps = null;
			if (dr instanceof WebMarkupContainer)
			{
				comps = new Object[((WebMarkupContainer)dr).size()];
				Iterator it = ((WebMarkupContainer)dr).iterator();
				int j = 0;
				while (it.hasNext())
				{
					comps[j++] = it.next();
				}
			}

			counter = registerComponentsToScope(fs, es, counter, comps, hmChildrenJavaMembers, dr);
		}

		if (v instanceof WebCellBasedView)
		{
			Object[] comps = ((WebCellBasedView)v).getComponents();
			counter = registerComponentsToScope(fs, es, counter, comps, hmChildrenJavaMembers, v);
		}

		es.setLocked(true);
		return es;
	}


	private int registerComponentsToScope(Scriptable fs, ElementScope es, int counter, Object[] comps, Map<String, Object[]> hmChildrenJavaMembers,
		Object parent)
	{
		if (comps != null)
		{
			for (Object comp : comps)
			{
				if (comp instanceof WebCellBasedView)
				{
					WebCellBasedView portal = (WebCellBasedView)comp;
					counter = registerComponentsToScope(fs, es, counter, portal.getComponents(), hmChildrenJavaMembers, comp);
				}

				String name = null;

				if (comp instanceof WrapperContainer)
				{
					comp = ((WrapperContainer)comp).getDelegate();
				}
				if (comp instanceof IComponent)
				{
					name = ((IComponent)comp).getName();
				}
				else if (comp instanceof java.awt.Component)
				{
					name = ((java.awt.Component)comp).getName();
				}

				if (comp instanceof WebImageBeanHolder)
				{
					comp = ((WebImageBeanHolder)comp).getDelegate();
				}
				else if (comp instanceof WebBeanHolder)
				{
					comp = ((WebBeanHolder)comp).getDelegate();
				}
				String groupName = FormElementGroup.getName((String)formController.getComponentProperty(comp, ComponentFactory.GROUPID_COMPONENT_PROPERTY));
				Object scriptable = comp;
				if (comp instanceof IScriptableProvider) scriptable = ((IScriptableProvider)comp).getScriptObject();
				JavaMembers jm = ScriptObjectRegistry.getJavaMembers(scriptable.getClass(), ScriptableObject.getTopLevelScope(fs));

				boolean named = name != null && !name.equals("") && !name.startsWith(ComponentFactory.WEB_ID_PREFIX); //$NON-NLS-1$ 
				if (groupName != null || named)
				{
					try
					{
						Scriptable s;
						if (parent instanceof WebCellBasedView)
						{
							s = new CellNativeJavaObject(fs, comp, jm, (WebCellBasedView)parent);
						}
						else
						{
							s = new NativeJavaObject(fs, scriptable, jm);
						}
						if (named)
						{
							es.put(name, fs, s);
							es.put(counter++, fs, s);
							hmChildrenJavaMembers.put(name, new Object[] { jm, scriptable });
						}
						if (groupName != null)
						{
							Object group = es.get(groupName, fs);
							if (group == Scriptable.NOT_FOUND)
							{
								group = new NativeJavaObject(fs, new RuntimeGroup(groupName), ScriptObjectRegistry.getJavaMembers(RuntimeGroup.class,
									ScriptableObject.getTopLevelScope(fs)));
								es.put(groupName, fs, group);
								es.put(counter++, fs, group);
							}
							if (scriptable instanceof IScriptBaseMethods && group instanceof NativeJavaObject &&
								((NativeJavaObject)group).unwrap() instanceof RuntimeGroup)
							{
								((RuntimeGroup)(((NativeJavaObject)group).unwrap())).addScriptBaseMethodsObj((IScriptBaseMethods)scriptable);
							}
						}
					}
					catch (Throwable ex)
					{
						Debug.error(ex);//incase classdefnot founds are thrown for beans,applets/plugins
					}
				}
			}
		}
		return counter;
	}

//	public void print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, int zoomFactor, PrinterJob printerJob)
//	{
//		print(showDialogs, printCurrentRecordOnly, showPrinterSelectDialog, printerJob);
//	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#print(boolean, boolean, boolean, java.awt.print.PrinterJob)
	 */
	public void print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob)
	{
		IFoundSetInternal fs = formController.getFoundSet();
		try
		{
			if (printCurrentRecordOnly)
			{
				fs = fs.copyCurrentRecordFoundSet();
			}
		}
		catch (ServoyException e1)
		{
			Debug.error(e1);
		}
		IApplication application = formController.getApplication();
		ByteArrayOutputStream baos = null;
		MainPage page = null;
		if (printerJob == null)
		{
			page = (MainPage)findPage();
			if (page == null)
			{
				IMainContainer tmp = ((FormManager)application.getFormManager()).getCurrentContainer();
				if (tmp instanceof MainPage) page = (MainPage)tmp;
			}
			// if "page" is still null then there is no wicket front-end for this client - so printing is not intended to reach the client; print on the server instead
			// (can happen for batch processors for example)
		}
		if (page != null)
		{
			baos = new ByteArrayOutputStream();

			StreamPrintServiceFactory[] factories = null;
			DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PAGEABLE;
			ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
			try
			{
				Thread.currentThread().setContextClassLoader(((PluginManager)application.getPluginManager()).getClassLoader());
				factories = StreamPrintServiceFactory.lookupStreamPrintServiceFactories(flavor, "application/pdf"); //$NON-NLS-1$
				if (factories == null || factories.length == 0)
				{
					Debug.error("No suitable pdf printer found"); //$NON-NLS-1$
					return;
				}
			}
			finally
			{
				Thread.currentThread().setContextClassLoader(savedClassLoader);
			}

			try
			{
				FormPreviewPanel fpp = new FormPreviewPanel(application, formController, fs);
				// AWT stuff happens here, so execute it in the AWT Event Thread - else exceptions can occur
				// for example in JEditorPane while getting the preferred size & stuff
				processFppInAWTEventQueue(fpp, application);
				StreamPrintService sps = factories[0].getPrintService(baos);
				Doc doc = new SimpleDoc(fpp.getPageable(), flavor, null);
				PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
				sps.createPrintJob().print(doc, pras);
				fpp.destroy();
			}
			catch (Exception ex)
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.printDocument"), ex); //$NON-NLS-1$
			}

			String contentType = "application/pdf"; //$NON-NLS-1$
			// Safari for windows (maybe for MAC too - can't say) does not download files
			// of "application/pdf" content-type; it only opens a blank page; to make it
			// work, for Safari we will change "application/pdf" to "application/octet-stream" (seems to trigger download)
			// BTW, "application/octet-stream" works for all browsers, but it is not that accurate
			if (application.getApplicationType() != IApplication.HEADLESS_CLIENT) // if it's not batch processor/jsp, because if it is, then getClientInfo() gives NullPointerException
			{
				ClientInfo info = Session.get().getClientInfo();
				if (info instanceof WebClientInfo)
				{
					String userAgent = ((WebClientInfo)info).getProperties().getNavigatorUserAgent();
					if (userAgent != null && userAgent.toLowerCase().contains("safari")) //$NON-NLS-1$
					{
						contentType = "application/octet-stream"; //$NON-NLS-1$
					}
				}
			}

			String url = page.serveResource(formController.getName() + ".pdf", baos.toByteArray(), contentType); //$NON-NLS-1$
			page.setShowURLCMD(url, "_self", null, 0, true); //$NON-NLS-1$
		}
		else
		{
			try
			{
				FormPreviewPanel fpp = new FormPreviewPanel(application, formController, fs);
				// AWT stuff happens here, so execute it in the AWT Event Thread - else exceptions can occur
				// for example in JEditorPane while getting the preferred size & stuff
				processFppInAWTEventQueue(fpp, application);
				PrintPreview.startPrinting(application, fpp.getPageable(), printerJob, formController.getPreferredPrinterName(), false, true);
				fpp.destroy();
			}
			catch (Exception ex)
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.printDocument"), ex); //$NON-NLS-1$
			}
		}
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#printPreview(boolean, boolean, java.awt.print.PrinterJob)
	 */
	public void printPreview(boolean showDialogs, boolean printCurrentRecordOnly, int zoomFactor, PrinterJob printerJob)
	{
		print(false, printCurrentRecordOnly, false, printerJob);
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#printXML(boolean)
	 */
	public String printXML(boolean printCurrentRecordOnly)
	{
		IApplication application = formController.getApplication();
		IFoundSetInternal fs = formController.getFoundSet();
		try
		{
			if (printCurrentRecordOnly)
			{
				fs = fs.copyCurrentRecordFoundSet();
			}

			FormPreviewPanel fpp = new FormPreviewPanel(application, formController, fs);
			// AWT stuff happens here, so execute it in the AWT Event Thread - else exceptions can occur
			// for example in JEditorPane while getting the preferred size & stuff
			processFppInAWTEventQueue(fpp, application);
			StringWriter w = new StringWriter();
			((PageList)fpp.getPageable()).toXML(w);
			fpp.destroy();
			return w.toString();
		}
		catch (Throwable ex)
		{
			application.reportError(application.getI18NMessage("servoy.formPanel.error.printDocument"), ex); //$NON-NLS-1$
		}
		return null;
	}

	private void processFppInAWTEventQueue(final FormPreviewPanel fpp, final IApplication application) throws InterruptedException, InvocationTargetException
	{
		SwingUtilities.invokeAndWait(new Runnable()
		{
			public void run()
			{
				// because some wicket widget properties are being used in the process() call, we need to set up
				// the thread local variables that they need in order to function - do this by using application.invoke...
				application.invokeAndWait(new Runnable()
				{
					public void run()
					{
						try
						{
							fpp.process();
						}
						catch (Exception e)
						{
							application.reportError(application.getI18NMessage("servoy.formPanel.error.printDocument"), e); //$NON-NLS-1$
						}
					}
				});
			}
		});
	}

	public Color getBackground()
	{
		return cbg;
	}

	public Border getBorder()
	{
		return border;
	}

	public Font getFont()
	{
		return font;
	}

	public Color getForeground()
	{
		return cfg;
	}

	public Point getLocation()
	{
		return location;
	}

	public Dimension getSize()
	{
		return size;
	}

	public boolean isOpaque()
	{
		return opaque;
	}

	public String getContainerName()
	{
		String name = null;
		MainPage mp = getMainPage();
		if (mp != null)
		{
			name = mp.getContainerName();
			if (name == null) name = IApplication.APP_WINDOW_NAME; // main container name is null for main app. window
		}
		return name;
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#isFormInWindow()
	 */
	public boolean isFormInWindow()
	{
		MainPage mp = getMainPage();
		if (mp != null)
		{
			return mp.isShowingInDialog();
		}
		return false;
	}

	private MainPage mainPage;

	public void setMainPage(MainPage mainPage)
	{
		this.mainPage = mainPage;
	}

	public MainPage getMainPage()
	{
		Page page = findPage();
		if (page instanceof MainPage)
		{
			return (MainPage)page;
		}
		return mainPage;
	}

	private FormAnchorInfo formAnchorInfo;

	private DesignModeBehavior designModeBehavior;

	private boolean uiRecreated;

	@SuppressWarnings("unchecked")
	public FormAnchorInfo getFormAnchorInfo(final boolean onlyChanged)
	{
		formAnchorInfo = new FormAnchorInfo(formController.getName(), formController.getForm().getSize(), formController.getForm().getUUID());

		final Map<String, ISupportAnchors> elements = new HashMap<String, ISupportAnchors>();
		Iterator<IPersist> e1 = formController.getForm().getAllObjects();
		while (e1.hasNext())
		{
			IPersist obj = e1.next();
			if (obj instanceof ISupportAnchors && obj instanceof ISupportBounds)
			{
				elements.put(ComponentFactory.getWebID(formController.getForm(), obj), (ISupportAnchors)obj);
			}
		}

		// In case we are in table view.
		if (view instanceof WebCellBasedView)
		{
			WebCellBasedView formPart = (WebCellBasedView)view;
			formAnchorInfo.addPart(Part.getDisplayName(Part.BODY), formPart.getMarkupId(), 50);
			formAnchorInfo.isTableView = true;
			formAnchorInfo.bodyContainerId = formPart.getMarkupId();
		}

		// Find the id of the form navigator, if any.
		visitChildren(Component.class, new IVisitor()
		{
			public Object component(Component component)
			{
				if (component instanceof WebDefaultRecordNavigator)
				{
					formAnchorInfo.navigatorWebId = component.getMarkupId();
					return IVisitor.CONTINUE_TRAVERSAL;
				}
				else if (component instanceof WebTabPanel)
				{
					return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
				}
				else return IVisitor.CONTINUE_TRAVERSAL;
			}
		});

		visitChildren(WebDataRenderer.class, new IVisitor()
		{
			public Object component(Component component)
			{
				WebDataRenderer formPart = (WebDataRenderer)component;

				final FormPartAnchorInfo part = formAnchorInfo.addPart(formPart.getFormPartName(), formPart.getMarkupId(), formPart.getSize().height);

				if (Part.getDisplayName(Part.BODY).equals(formPart.getFormPartName()))
				{
					Component parent = formPart.getParent();
					formAnchorInfo.bodyContainerId = parent.getMarkupId();
				}
				formPart.visitChildren(ISupportWebBounds.class, new IVisitor()
				{
					public Object component(Component comp)
					{
						// tab panels need to go all the time, because they need to have their tabs rearranged.
						// also buttons need to go all the time, because of lovely FF handling of <button> tags.
						if (onlyChanged && !(comp instanceof WebTabPanel) && !(comp instanceof IButton))
						{
							if (comp instanceof IProviderStylePropertyChanges)
							{
								if (!((IProviderStylePropertyChanges)comp).getStylePropertyChanges().isChanged())
								{
									boolean hasBgImage = false;
									if (comp instanceof ILabel && ((ILabel)comp).getMediaIcon() > 0)
									{
										hasBgImage = true;
									}
									if (comp instanceof ILabel && ((ILabel)comp).getImageURL() != null)
									{
										hasBgImage = true;
									}
									if (!hasBgImage) return IVisitor.CONTINUE_TRAVERSAL;
								}
							}
							else return IVisitor.CONTINUE_TRAVERSAL;
						}
						String id = comp.getId();
						ISupportAnchors obj = elements.get(id);
						if (obj != null)
						{
							int anchors = obj.getAnchors();
							if (((anchors > 0 && anchors != IAnchorConstants.DEFAULT)) || (comp instanceof WebTabPanel) || (comp instanceof IButton))
							{
								Rectangle r = ((ISupportWebBounds)comp).getWebBounds();
								if (r != null)
								{
									if (anchors == 0) anchors = IAnchorConstants.DEFAULT;

									int hAlign = -1;
									int vAlign = -1;
									if (obj instanceof ISupportTextSetup)
									{
										ISupportTextSetup alignedObj = (ISupportTextSetup)obj;
										hAlign = alignedObj.getHorizontalAlignment();
										vAlign = alignedObj.getVerticalAlignment();
									}

									part.addAnchoredElement(comp.getMarkupId(), anchors, r, hAlign, vAlign, comp.getClass());
								}

							}
						}
						return IVisitor.CONTINUE_TRAVERSAL;
					}
				});
				return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
		});

		return formAnchorInfo;
	}

	public int getAnchors(String componentId)
	{
		Iterator<IPersist> e1 = formController.getForm().getAllObjects();
		while (e1.hasNext())
		{
			IPersist obj = e1.next();
			if ((obj instanceof ISupportAnchors) && ComponentFactory.getWebID(formController.getForm(), obj).equals(componentId)) return ((ISupportAnchors)obj).getAnchors();
		}
		return IAnchorConstants.DEFAULT;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		return null;
	}

	public Component getFirstFocusableField()
	{
		return null;
	}

	public Component getLastFocusableField()
	{
		return null;
	}

	public boolean showYesNoQuestionDialog(IApplication application, String dlgMessage, String string)
	{
		// NOT USED
		return false;
	}

	public void focusField(Component field)
	{
		if (tabSeqComponentList != null && tabSeqComponentList.size() > 0)
		{
			if (field instanceof WebCellBasedView)
			{
				((WebCellBasedView)field).focusFirstField();
			}
			else
			{
				IMainContainer currentContainer = ((FormManager)formController.getApplication().getFormManager()).getCurrentContainer();
				if (currentContainer instanceof MainPage)
				{
					((MainPage)currentContainer).componentToFocus(field);
				}
				else
				{
					Debug.trace("focus couldnt be set on component " + field); //$NON-NLS-1$
				}
			}
		}
		else
		{
			// try to focus first component on page
			IMainContainer currentContainer = ((FormManager)formController.getApplication().getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				// can't find a suitable servoy field, use any wicket component
				Component first = getDefaultFirstComponent((MarkupContainer)currentContainer);
				if (first == null)
				{
					Iterator< ? > it = ((MarkupContainer)currentContainer).iterator();
					while (it.hasNext())
					{
						Object o = it.next();
						if (o instanceof Component)
						{
							first = (Component)o;
							break;
						}
					}
				}
				if (first != null) ((MainPage)currentContainer).componentToFocus(first);
				else Debug.trace("cannot find default first component"); //$NON-NLS-1$
			}
			else
			{
				Debug.trace("focus couldnt be set on default first component"); //$NON-NLS-1$
			}
		}
	}

	private Component getDefaultFirstComponent(MarkupContainer currentContainer)
	{
		Iterator< ? > it = currentContainer.iterator();
		while (it.hasNext())
		{
			Object o = it.next();
			if (o instanceof MarkupContainer)
			{
				Component c = getDefaultFirstComponent((MarkupContainer)o);
				if (c != null) return c;
			}
			else if (o instanceof Component && o instanceof IFieldComponent)
			{
				return (Component)o;
			}
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#setDesignMode(com.servoy.j2db.DesignModeCallbacks)
	 */
	public void setDesignMode(final DesignModeCallbacks callback)
	{
		visitChildren(IComponent.class, new IVisitor<Component>()
		{
			public Object component(Component component)
			{
				if (component instanceof IDesignModeListener)
				{
					((IDesignModeListener)component).setDesignMode(callback != null);
				}
				List<IBehavior> behaviors = component.getBehaviors();
				for (int i = 0; i < behaviors.size(); i++)
				{
					Object element = behaviors.get(i);
					if (element instanceof IDesignModeListener)
					{
						((IDesignModeListener)element).setDesignMode(callback != null);
					}
				}
				if (component instanceof ITabPanel)
				{
					return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
				}
				return IVisitor.CONTINUE_TRAVERSAL;
			}
		});

		if (designModeBehavior == null)
		{
			designModeBehavior = new DesignModeBehavior();
			add(designModeBehavior);
		}

		designModeBehavior.setDesignModeCallback(callback, formController);

		if (callback != null)
		{
			uiRecreated = true;
		}
	}

	/**
	 * @see org.apache.wicket.Component#onBeforeRender()
	 */
	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		if (previousParent != getParent())
		{
			formWidth = 0;
			previousParent = getParent();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.wicket.MarkupContainer#onRender(org.apache.wicket.markup.MarkupStream)
	 */
	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		uiRecreated = false;
	}

	long lastModifiedTime = 0;

	@SuppressWarnings("nls")
	@Override
	public void renderHead(HtmlHeaderContainer headercontainer)
	{
		super.renderHead(headercontainer);
		StringBuffer cssRef = new StringBuffer();
		cssRef.append("\n<link rel='stylesheet' type='text/css' href='");
		cssRef.append(UrlUtils.rewriteToContextRelative("servoy-webclient/formcss/", RequestCycle.get().getRequest()));
		cssRef.append(formController.getForm().getSolution().getName());
		cssRef.append('/');
		cssRef.append(formController.getName());
		cssRef.append("_t");
		if (lastModifiedTime == 0 || isUIRecreated())
		{
			lastModifiedTime = System.currentTimeMillis();
		}
		cssRef.append(lastModifiedTime);
		cssRef.append("t.css'/>\n");
		headercontainer.getHeaderResponse().renderString(cssRef.toString());
	}

	public boolean isDesignMode()
	{
		return designModeBehavior != null && designModeBehavior.isEnabled(null);
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#uiRecreated()
	 */
	public void uiRecreated()
	{
		// touch the form when recreated so that template generator will flush its css/html cache
		formController.getForm().setLastModified(System.currentTimeMillis());
		// remove the markup from the cache for this webform.
		//((ServoyMarkupCache)Application.get().getMarkupSettings().getMarkupCache()).removeFromCache(this);
		markup = null;
		uiRecreated = true;
	}

	public boolean isUIRecreated()
	{
		return uiRecreated;
	}

	/**
	 * @see com.servoy.j2db.ui.IProviderStylePropertyChanges#getStylePropertyChanges()
	 */
	public IStylePropertyChanges getStylePropertyChanges()
	{
		return new IStylePropertyChanges()
		{

			public void setValueChanged()
			{
			}

			public void setRendered()
			{
				uiRecreated = false;
			}

			public void setChanges(Properties changes)
			{
			}

			public void setChanged()
			{
			}

			public boolean isValueChanged()
			{
				return false;
			}

			public boolean isChanged()
			{
				if (!uiRecreated)
				{
					if (designModeBehavior != null && designModeBehavior.isEnabled(WebForm.this))
					{
						Object retValue = visitChildren(IProviderStylePropertyChanges.class, new Component.IVisitor()
						{
							public Object component(Component component)
							{
								IStylePropertyChanges stylePropertyChanges = ((IProviderStylePropertyChanges)component).getStylePropertyChanges();
								if (stylePropertyChanges.isValueChanged() || stylePropertyChanges.isChanged())
								{
									return Boolean.TRUE;
								}
								return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
							}
						});
						if (retValue instanceof Boolean && ((Boolean)retValue).booleanValue())
						{
							return true;
						}
					}
					return false;
				}
				return true;
			}

			public Properties getChanges()
			{
				return null;
			}
		};
	}

	public void prepareForSave(boolean looseFocus)
	{
		// was in FormController only called for SwingForm
	}
}
