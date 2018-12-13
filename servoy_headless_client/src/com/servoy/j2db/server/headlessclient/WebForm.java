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
import java.util.TreeMap;
import java.util.WeakHashMap;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.SimpleDoc;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
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
import org.xhtmlrenderer.css.constants.CSSName;

import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.DesignModeCallbacks;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormExecutionState;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicMainContainer;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.ISupportFormExecutionState;
import com.servoy.j2db.ISupportNavigator;
import com.servoy.j2db.IView;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.plugins.PluginManager;
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
import com.servoy.j2db.server.headlessclient.dataui.IImageDisplay;
import com.servoy.j2db.server.headlessclient.dataui.ISupportWebTabSeq;
import com.servoy.j2db.server.headlessclient.dataui.IWebFormContainer;
import com.servoy.j2db.server.headlessclient.dataui.RecordItemModel;
import com.servoy.j2db.server.headlessclient.dataui.StyleAppendingModifier;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;
import com.servoy.j2db.server.headlessclient.dataui.WebAccordionPanel;
import com.servoy.j2db.server.headlessclient.dataui.WebBaseButton;
import com.servoy.j2db.server.headlessclient.dataui.WebBaseSelectBox;
import com.servoy.j2db.server.headlessclient.dataui.WebBeanHolder;
import com.servoy.j2db.server.headlessclient.dataui.WebCellBasedView;
import com.servoy.j2db.server.headlessclient.dataui.WebDataCheckBoxChoice;
import com.servoy.j2db.server.headlessclient.dataui.WebDataRadioChoice;
import com.servoy.j2db.server.headlessclient.dataui.WebDataRenderer;
import com.servoy.j2db.server.headlessclient.dataui.WebDataRendererFactory;
import com.servoy.j2db.server.headlessclient.dataui.WebDefaultRecordNavigator;
import com.servoy.j2db.server.headlessclient.dataui.WebImageBeanHolder;
import com.servoy.j2db.server.headlessclient.dataui.WebRecordView;
import com.servoy.j2db.server.headlessclient.dataui.WebSplitPane;
import com.servoy.j2db.server.headlessclient.dataui.WebTabFormLookup;
import com.servoy.j2db.server.headlessclient.dataui.WebTabPanel;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.ui.runtime.HasRuntimeEnabled;
import com.servoy.j2db.ui.runtime.HasRuntimeReadOnly;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.scripting.RuntimePortal;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.RoundedBorder;

/**
 * @author jcompagner
 *
 */
public class WebForm extends Panel
	implements IFormUIInternal<Component>, IMarkupCacheKeyProvider, IProviderStylePropertyChanges, ISupportSimulateBounds, ISupportFormExecutionState
{
	private static final long serialVersionUID = 1L;

	private final String variation;
	private final FormController formController;

	private final StyleAppendingModifier hiddenBeforeShow = new StyleAppendingModifier(new Model<String>("visibility:hidden"));
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
	private boolean enabled;
//	private final List<Component> markedReadOnlyComponents;
	private final List<Component> markedEnabledComponents;
	private List<Component> tabSeqComponentList = new ArrayList<Component>();
	private WebDefaultRecordNavigator defaultNavigator = null;

	private boolean destroyed;
	private IView view;

	private boolean isFormWidthHeightChanged;
	protected IStylePropertyChanges jsChangeRecorder;

	/**
	 * @param id
	 */
	public WebForm(final FormController controller)
	{
		super("webform"); //$NON-NLS-1$
		markedEnabledComponents = new ArrayList<Component>();
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
				// jquery accordion will handle the layout styling, cannot set our style
				return ((component.findParent(IWebFormContainer.class) != null) &&
					!(component.findParent(IWebFormContainer.class) instanceof WebAccordionPanel));
			}
		});

		add(new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				IWebFormContainer container = findParent(IWebFormContainer.class);
				if (container != null && !(container instanceof WebAccordionPanel) && container.getBorder() instanceof TitledBorder)
				{
					int offset = ComponentFactoryHelper.getTitledBorderHeight(container.getBorder());
					return "top: " + offset + "px;";
				}

				return "";
			}
		}));

		container = new WebMarkupContainer("servoywebform") //$NON-NLS-1$
		{
			@Override
			protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
			{
				if (getBorder() instanceof TitledBorder)
				{
					getResponse().write(WebBaseButton.getTitledBorderOpenMarkup((TitledBorder)getBorder()));
				}
				super.onComponentTagBody(markupStream, openTag);
				if (getBorder() instanceof TitledBorder)
				{
					getResponse().write(WebBaseButton.getTitledBorderCloseMarkup());
				}
			}
		};
		container.add(new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				IWebFormContainer tabpanel = findParent(IWebFormContainer.class);
				if (tabpanel != null)
				{
					return "min-width:0px;min-height:0px;";
				}
				return null;
			}
		}));
//		container.add(new StyleAppendingModifier(new Model<String>()
//		{
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public String getObject()
//			{
//				int offset = ((TitledBorder)getBorder()).getTitleFont().getSize() + 4;//add legend height
//				return "top: " + offset + "px;";
//			}
//		})
//		{
//			@Override
//			public boolean isEnabled(Component component)
//			{
//				if (getBorder() instanceof TitledBorder)
//				{
//					return super.isEnabled(component);
//				}
//				return false;
//			}
//		});
		// we need to explicitly make the form transparent, to override the
		// white color from the default CSS (the #webform class)
		// case 349263
		add(new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				// in case of dialogs, tab/split/... panel, popup form (from window plugin), this component must
				// also have proper rounded border and transparency when needed (it's style can be tweaked from overridden default styles
				// see TemplateGenerator - bkcolor white)
				String styleAddition = "";
				if (getBorder() instanceof RoundedBorder)
				{
					float[] radius = ((RoundedBorder)getBorder()).getRadius();
					StringBuilder builder = new StringBuilder();
					builder.append("border-radius:");
					for (int i = 0; i < 8; i++)
					{
						builder.append(radius[i]);
						builder.append("px ");
						if (i == 3) builder.append("/ ");
					}
					builder.append(";");
					styleAddition = builder.toString();
				}
				IStyleRule formStyle = controller.getFormStyle();
				boolean hasSemiTransparentBackground = false;
				if (formStyle != null && formStyle.hasAttribute(CSSName.BACKGROUND_COLOR.toString()) &&
					formStyle.getValue(CSSName.BACKGROUND_COLOR.toString()).contains(PersistHelper.COLOR_RGBA_DEF)) hasSemiTransparentBackground = true;
				if (controller.getForm().getTransparent() || hasSemiTransparentBackground)
				{
					styleAddition += "background:transparent;"; //$NON-NLS-1$
				}
				return styleAddition;
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

		add(hiddenBeforeShow);
		hiddenBeforeShow.setEnabled(false);
		// set fixed markup id so that element can always be found by markup id
		container.setOutputMarkupId(true);
		container.setMarkupId("form_" + ComponentFactory.stripIllegalCSSChars(formController.getName())); // same as in template generator //$NON-NLS-1$
		TabIndexHelper.setUpTabIndexAttributeModifier(container, ISupportWebTabSeq.SKIP);
		add(container);
		readonly = false;
		enabled = true;
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

	public JSDataSet getFormContext()
	{
		WebForm current = this;
		ITabPanel currentTabPanel = null;
		String currentBeanName = null;
		WebSplitPane currentSplitPane = null;
		IDataSet set = new BufferedDataSet(
			new String[] { "containername", "formname", "tabpanel/splitpane/accordion/beanname", "tabname", "tabindex", "tabindex1based" }, //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$//$NON-NLS-6$
			new ArrayList<Object[]>());
		set.addRow(new Object[] { null, current.formController.getName(), null, null, null, null });
		MarkupContainer parent = getParent();
		while (parent != null)
		{
			if (parent instanceof WebSplitPane)
			{
				currentSplitPane = (WebSplitPane)parent;
			}
			else if (parent instanceof ITabPanel)
			{
				currentTabPanel = (ITabPanel)parent;
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
					if (currentTabPanel instanceof WebTabPanel)
					{
						index = ((WebTabPanel)currentTabPanel).getTabIndex(current);
					}
					else if (currentTabPanel instanceof WebAccordionPanel)
					{
						index = ((WebAccordionPanel)currentTabPanel).getTabIndex(current);
					}
					if (index != -1)
					{
						tabName = currentTabPanel.getTabNameAt(index); // js method so +1
					}
					current = (WebForm)parent;
					set.addRow(0, new Object[] { null, current.formController.getName(), currentTabPanel.getName(), tabName, new Integer(index), new Integer(
						index + 1) });
				}
				else if (currentBeanName != null)
				{
					current = (WebForm)parent;
					set.addRow(0, new Object[] { null, current.formController.getName(), currentBeanName, null, null, null });
				}
				else if (currentSplitPane != null)
				{
					int idx = currentSplitPane.getLeftForm() != null && current.equals(((WebTabFormLookup)currentSplitPane.getLeftForm()).getWebForm()) ? 0 : 1;
					current = (WebForm)parent;
					set.addRow(0, new Object[] { null, current.formController.getName(), currentSplitPane.getName(), currentSplitPane.getTabNameAt(
						idx), new Integer(idx + 1), new Integer(idx + 1) });
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
				if (comp instanceof IWebFormContainer)
				{
					((IWebFormContainer)comp).setTabSequenceIndex(tabIndex);
					tabIndex = WebDataRendererFactory.getContainerGapIndex(tabIndex + WebDataRendererFactory.CONTAINER_RESERVATION_GAP, tabIndex);
					TabIndexHelper.setUpTabIndexAttributeModifier(comp, ISupportWebTabSeq.SKIP);
				}
				else if (comp instanceof WebCellBasedView)
				{
					WebCellBasedView tableView = (WebCellBasedView)comp;
					tableView.setTabSequenceIndex(tabIndex);
					tabIndex += WebDataRendererFactory.MAXIMUM_TAB_INDEXES_ON_TABLEVIEW;
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
						((WebCellBasedView)dr).setTabSequenceIndex(ISupportWebTabSeq.DEFAULT);
						((WebCellBasedView)dr).setTabSeqComponents(null);
					}
					else if (!tabSequence.contains(dr))
					{
						// it shouldn't gain focus when tabbing ...
						((WebCellBasedView)dr).setTabSequenceIndex(-1);
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
			addHeaders = false;//list views do not have headers
		}

//		String orientation = OrientationApplier.getHTMLContainerOrientation(app.getLocale(), app.getSolution().getTextOrientation());
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
				((WebRecordView)view).add(body);
				body.setDefaultModel(rendererModel);
				body.setParentView(view);
			}
			else
			{
				//((WebRecordView)view).add(new WebMarkupContainer());
			}
		}
		else if (viewType == FormController.TABLE_VIEW || viewType == FormController.LOCKED_TABLE_VIEW || viewType == IForm.LIST_VIEW ||
			viewType == FormController.LOCKED_LIST_VIEW)
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
					fp.getScriptExecuter(), addHeaders, startY, endY, sizeHint, viewType);
				viewScriptable.setComponent((WebCellBasedView)view, f);
				if (dataRenderers[FormController.FORM_EDITOR] != null) dataRenderers[FormController.FORM_EDITOR].destroy();
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
			try
			{
				remove();
			}
			catch (Exception e)
			{
				Debug.log("error destroying the webform", e); //$NON-NLS-1$
			}
		}
		jsChangeRecorder = null;
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
	private static class WicketCompVisitorMarker implements IVisitor<Component>
	{
		private final boolean readonlyFlag;

		public WicketCompVisitorMarker(boolean readonlyFlag)
		{
			this.readonlyFlag = readonlyFlag;
		}

		public Object component(Component component)
		{
//			if (component instanceof WebForm)
//			{
//				FormManager formManager = (FormManager)((WebForm)component).getController().getApplication().getFormManager();
//				if (!formManager.isFormReadOnly(((WebForm)component).getController().getName()))
//				{
//					((WebForm)component).getController().setReadOnly(readonlyFlag);
//				}
//				return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
//			}
//			else if (((IScriptableProvider)component).getScriptObject() instanceof HasRuntimeReadOnly)
			if (((IScriptableProvider)component).getScriptObject() instanceof HasRuntimeReadOnly)
			{
				HasRuntimeReadOnly scriptable = (HasRuntimeReadOnly)((IScriptableProvider)component).getScriptObject();
				scriptable.setReadOnly(readonlyFlag);
			}

//				if (!scriptable.isReadOnly() && readonlyFlag)
//				{
//					scriptable.setReadOnly(readonlyFlag);
//					if (!markedList.contains(component))
//					{
//						markedList.add(component);
//					}
//				}
//				else if (!readonlyFlag && markedList.contains(component))
//				{
//					scriptable.setReadOnly(readonlyFlag);
//				}
			return CONTINUE_TRAVERSAL;
		}
	}

	private static class WicketCompVisitorMarker2 implements IVisitor<Component>
	{
		private final boolean enabledFlag;
		private final List<Component> markedList;

		public WicketCompVisitorMarker2(List<Component> markedList, boolean enabledFlag)
		{
			this.markedList = markedList;
			this.enabledFlag = enabledFlag;
		}

		public Object component(Component component)
		{
			if (component instanceof WebForm)
			{
				((WebForm)component).getController().setComponentEnabled(enabledFlag);

				return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
			else if (((IScriptableProvider)component).getScriptObject() instanceof HasRuntimeEnabled)
			{
				HasRuntimeEnabled scriptable = (HasRuntimeEnabled)((IScriptableProvider)component).getScriptObject();
				if (scriptable.isEnabled() && !enabledFlag)
				{
					scriptable.setEnabled(enabledFlag);
					if (!markedList.contains(component))
					{
						markedList.add(component);
					}
				}
				else if (enabledFlag && markedList.contains(component))
				{
					scriptable.setEnabled(enabledFlag);
				}
			}
			return CONTINUE_TRAVERSAL;
		}
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormUIInternal#isReadOnly()
	 */
	@Override
	public boolean isReadOnly()
	{
		return readonly;
	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#setReadOnly(boolean)
	 */
	public void setReadOnly(boolean b)
	{
		readonly = b;
		visitChildren(IScriptableProvider.class, new WicketCompVisitorMarker(b));
//		if (readonly != b)
//		{
//			readonly = b;
//			visitChildren(IScriptableProvider.class, new WicketCompVisitorMarker(markedReadOnlyComponents, readonly));
//			if (!readonly)
//			{
//				markedReadOnlyComponents.clear();
//			}
//		}
//		else
//		{
//			visitChildren(WebForm.class, new WicketCompVisitorMarker(markedReadOnlyComponents, b));
//		}
	}

//	public List<Component> getReadOnlyComponents()
//	{
//		return markedReadOnlyComponents;
//	}

	/**
	 * @see com.servoy.j2db.IFormUIInternal#updateFormUI()
	 */
	public void updateFormUI()
	{
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormUIInternal#touch()
	 */
	@Override
	public void touch()
	{
		MainPage page = getMainPage();
		if (page != null)
		{
			page.touch();
		}
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

	private Boolean enableChanged = Boolean.FALSE;

	public void setComponentEnabled(final boolean b)
	{
		enableChanged = Boolean.TRUE;

		if (enabled != b)
		{
			enabled = b;
			visitChildren(IScriptableProvider.class, new WicketCompVisitorMarker2(markedEnabledComponents, enabled));
			if (enabled)
			{
				markedEnabledComponents.clear();
			}
		}
		else
		{
			visitChildren(WebForm.class, new WicketCompVisitorMarker2(markedEnabledComponents, b));
		}


//		setEnabled(enabled);
//		visitChildren(WebForm.class, new WicketCompVisitorMarker2(markedEnabledComponents, b));
		//if form is in a tabpanel, mark parent tabpanel as changed
		MarkupContainer parent = getParent();
		if (parent instanceof WebTabPanel && ((WebTabPanel)parent).getOrient() != TabPanel.HIDE &&
			((WebTabPanel)parent).getOrient() != TabPanel.SPLIT_HORIZONTAL && ((WebTabPanel)parent).getOrient() != TabPanel.SPLIT_VERTICAL)
		{
			((WebTabPanel)getParent()).getStylePropertyChanges().setChanged();
		}
		else
		{
			//check if form is in an accordeon panel
			WebAccordionPanel accPanel = findParent(WebAccordionPanel.class);
			if (accPanel != null)
			{
				accPanel.getStylePropertyChanges().setChanged();
			}
		}
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
								group = new NativeJavaObject(fs, new RuntimeGroup(groupName),
									ScriptObjectRegistry.getJavaMembers(RuntimeGroup.class, ScriptableObject.getTopLevelScope(fs)));
								es.put(groupName, fs, group);
								es.put(counter++, fs, group);
							}
							if (scriptable instanceof IRuntimeComponent && group instanceof NativeJavaObject &&
								((NativeJavaObject)group).unwrap() instanceof RuntimeGroup)
							{
								((RuntimeGroup)(((NativeJavaObject)group).unwrap())).addScriptBaseMethodsObj((IRuntimeComponent)scriptable);
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

			String url = page.serveResource(formController.getName() + ".pdf", baos.toByteArray(), contentType, "attachment"); //$NON-NLS-1$ //$NON-NLS-2$
			page.setShowURLCMD(url, "_self", null, 0, false); //$NON-NLS-1$
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
		if (mainPage != null) return mainPage;
		Page page = findPage();
		if (page instanceof MainPage)
		{
			return (MainPage)page;
		}
		return null;
	}

	public MainPage findMainPage()
	{
		WebForm currentForm = this;
		while (currentForm != null)
		{
			MainPage parentPage = currentForm.getMainPage();
			if (parentPage != null) return parentPage;
			currentForm = currentForm.findParent(WebForm.class);
		}
		IMainContainer currentContainer = ((FormManager)formController.getApplication().getFormManager()).getCurrentContainer();
		if (currentContainer instanceof MainPage)
		{
			return (MainPage)currentContainer;
		}
		return null;
	}

	private FormAnchorInfo formAnchorInfo;

	private DesignModeBehavior designModeBehavior;

	private boolean uiRecreated;

	@SuppressWarnings("unchecked")
	public FormAnchorInfo getFormAnchorInfo()
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

									String imageDisplayURL = null;
									boolean isRandomParamRemoved = false;
									if (comp instanceof IImageDisplay)
									{
										Object[] aImageDisplayURL = WebBaseButton.getImageDisplayURL((IImageDisplay)comp, false);
										imageDisplayURL = (String)aImageDisplayURL[0];
										isRandomParamRemoved = ((Boolean)aImageDisplayURL[1]).booleanValue();
									}
									part.addAnchoredElement(comp.getMarkupId(), anchors, r, hAlign, vAlign, comp.getClass(), imageDisplayURL,
										isRandomParamRemoved);
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
		IPersist obj = getPersist(componentId);
		if ((obj instanceof ISupportAnchors)) return ((ISupportAnchors)obj).getAnchors();

		return IAnchorConstants.DEFAULT;
	}

	private IPersist getPersist(String componentId)
	{
		Iterator<IPersist> e1 = formController.getForm().getAllObjects();
		while (e1.hasNext())
		{
			IPersist obj = e1.next();
			if (ComponentFactory.getWebID(formController.getForm(), obj).equals(componentId)) return obj;
		}
		return null;
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
				MainPage parentPage = findMainPage();
				if (parentPage != null)
				{
					parentPage.componentToFocus(field);
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
			MainPage currentContainer = findMainPage();
			if (currentContainer != null)
			{
				// can't find a suitable servoy field, use any wicket component
				Component first = getDefaultFirstComponent(currentContainer);
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
				if (first != null) currentContainer.componentToFocus(first);
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

	private final WeakHashMap<DesignModeCallbacks, String[]> designModeSelection = new WeakHashMap<DesignModeCallbacks, String[]>();
	private final WeakHashMap<IFieldComponent, Boolean> compEditableStatusBeforeDesignMode = new WeakHashMap<IFieldComponent, Boolean>();

	/**
	 * @see com.servoy.j2db.IFormUIInternal#setDesignMode(com.servoy.j2db.DesignModeCallbacks)
	 */
	public void setDesignMode(final DesignModeCallbacks callback)
	{
		final boolean designModeFlag = callback != null;

		visitChildren(IComponent.class, new IVisitor<Component>()
		{
			public Object component(Component component)
			{
				if (designModeFlag)
				{
					if (component instanceof WebBaseSelectBox || component instanceof WebDataCheckBoxChoice || component instanceof WebDataRadioChoice)
					{
						compEditableStatusBeforeDesignMode.put((IFieldComponent)component, Boolean.valueOf(((IFieldComponent)component).isEditable()));
						((IFieldComponent)component).setEditable(false);
					}
				}
				else if (component instanceof IFieldComponent)
				{
					if (compEditableStatusBeforeDesignMode.containsKey(component))
					{
						((IFieldComponent)component).setEditable(compEditableStatusBeforeDesignMode.remove(component).booleanValue());
					}
				}

				if (component instanceof IDesignModeListener)
				{
					((IDesignModeListener)component).setDesignMode(designModeFlag);
				}
				List<IBehavior> behaviors = component.getBehaviors();
				for (int i = 0; i < behaviors.size(); i++)
				{
					Object element = behaviors.get(i);
					if (element instanceof IDesignModeListener)
					{
						((IDesignModeListener)element).setDesignMode(designModeFlag);
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

		if (callback != null)
		{
			String[] selection = designModeSelection.get(callback);
			designModeBehavior.setSelectedComponents(selection);
		}
		else
		{
			if (designModeBehavior != null)
			{
				String[] selectedComponentsNames = designModeBehavior.getSelectedComponentsNames();
				if (selectedComponentsNames != null) designModeSelection.put(designModeBehavior.getDesignModeCallback(), selectedComponentsNames);
			}
		}
		designModeBehavior.setDesignModeCallback(callback, formController);

		// we need to recreate for both enabling and disabling of design mode
		// for adding the client side js code when enabling, and for clearing the
		// currently selected element when disabling
		uiRecreated = true;
	}

	/**
	 * @see org.apache.wicket.Component#onBeforeRender()
	 */
	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		Component container = (Component)findParent(IWebFormContainer.class);
		if (container == null)
		{
			container = getParent();
		}
		if (previousParent != null && previousParent != container)
		{
			// we show this form in another container, we must refresh the size
			formWidth = 0;
		}
		previousParent = container;
		//if recreateUI is called on a form in a tabpannel the tabs bar flickers if the background collor isnot the same as the form containing the tab pannel ... So the form in the  tab is shown after rearrageTabsInTabPanel() is done
		if (isUIRecreated() && getParent() instanceof WebTabPanel)
		{
			hiddenBeforeShow.setEnabled(true);
		}
		else
		{
			hiddenBeforeShow.setEnabled(false);
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
		getStylePropertyChanges().setRendered();
		enableChanged = Boolean.FALSE;
	}

	long lastModifiedTime = 0;

	@SuppressWarnings("nls")
	@Override
	public void renderHead(HtmlHeaderContainer headercontainer)
	{
		super.renderHead(headercontainer);
		Response response = headercontainer.getHeaderResponse().getResponse();
		response.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"");

		StringBuilder sb = new StringBuilder();
		sb.append(UrlUtils.rewriteToContextRelative("servoy-webclient/formcss/", RequestCycle.get().getRequest()));
		sb.append(formController.getForm().getSolution().getName());
		sb.append("/");
		sb.append(formController.getName());
		sb.append("_t");
		long prevLMT = lastModifiedTime;
		if (lastModifiedTime == 0 || isUIRecreated())
		{
			lastModifiedTime = System.currentTimeMillis();
		}
		sb.append(Long.toString(lastModifiedTime));
		sb.append("t.css");
		response.write(RequestCycle.get().getOriginalResponse().encodeURL(sb));
		response.write("\" id=\"formcss_");
		response.write(formController.getName());
		response.write(Long.toString(lastModifiedTime));
		response.write("\"");
		getResponse().println(" />");

		if (isUIRecreated())
		{
			if (this.getParent() instanceof WebTabPanel)
			{
				String tabPanelId = this.getParent().getMarkupId();
				String showWebFormjs = "$('#" + getMarkupId() + "').css('visibility','" + (isVisible() ? "inherit" : "hidden") + "');";
				//show WebForm after rearrangeTabsInTabPanel() is done
				String jsCall = "rearrageTabsInTabPanel('" + tabPanelId + "');" + showWebFormjs;

				headercontainer.getHeaderResponse().renderOnLoadJavascript(jsCall);
			}
			StringBuffer cssRef = new StringBuffer();
			cssRef.append("Servoy.Utils.removeFormCssLink('formcss_");
			cssRef.append(formController.getName());
			cssRef.append(prevLMT);
			cssRef.append("');");
			headercontainer.getHeaderResponse().renderJavascript(cssRef, null);
		}

		if (isFormInWindow())
		{
			List<Component> componentz = getTabSeqComponents();
			int max = -1;
			int min = Integer.MAX_VALUE;
			String maxTabIndexElemId = null;
			String minTabIndexElemId = null;
			for (Component c : componentz)
			{
				int tabIndex = TabIndexHelper.getTabIndex(c);
				if (tabIndex > max)
				{
					max = tabIndex;
					maxTabIndexElemId = c.getMarkupId();
				}
				if (tabIndex != -1 && tabIndex < min)
				{
					min = tabIndex;
					minTabIndexElemId = c.getMarkupId();
				}
			}
			if (minTabIndexElemId != null && maxTabIndexElemId != null)
			{
				headercontainer.getHeaderResponse().renderOnLoadJavascript(
					"Servoy.TabCycleHandling.registerListeners('" + minTabIndexElemId + "','" + maxTabIndexElemId + "');");
			}
		}

		if (isFormInFormPopup())
		{
			TreeMap<String, String> tm = new TreeMap<String, String>();
			for (Component c : getTabSeqComponents())
			{
				tm.put(String.valueOf(TabIndexHelper.getTabIndex(c)), c.getMarkupId());
			}
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append("Servoy.TabCycleHandling.forceTabbingSequence([");
			for (Map.Entry<String, String> entry : tm.entrySet())
			{
				stringBuffer.append("'" + entry.getValue() + "',");
			}
			stringBuffer.deleteCharAt(stringBuffer.length() - 1);
			stringBuffer.append("]);");
			headercontainer.getHeaderResponse().renderOnLoadJavascript(stringBuffer.toString());
		}
	}

	private boolean isFormInFormPopup()
	{
		return findParent(IRepeatingView.class) != null;
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

		IWebFormContainer webContainer = findParent(IWebFormContainer.class);
		if (webContainer != null && webContainer.isCurrentForm(this))
		{
			webContainer.uiRecreated();
		}

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
		if (jsChangeRecorder == null) jsChangeRecorder = new IStylePropertyChanges()
		{

			private boolean changed = false;

			public void setValueChanged()
			{
			}

			public void setRendered()
			{
				uiRecreated = false;
				changed = false;
			}

			public void setChanges(Properties changes)
			{
			}

			public void setChanged()
			{
				changed = true; // plugins can call this (see WebClientUtils)
			}

			public boolean isValueChanged()
			{
				return false;
			}

			public boolean isChanged()
			{
				if (changed || (enableChanged != null && enableChanged.booleanValue()))
				{
					return true;
				}
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

			public String getJSProperty(String key)
			{
				return null;
			}
		};
		return jsChangeRecorder;
	}

	public void prepareForSave(boolean looseFocus)
	{
		// was in FormController only called for SwingForm
	}

	public Rectangle getBounds(IComponent component)
	{
		if (component instanceof Component && Utils.getAsBoolean(formController.getApplication().getRuntimeProperties().get("enableAnchors"))) //$NON-NLS-1$
		{
			IPersist persist = getPersist(((Component)component).getId());
			if (persist instanceof ISupportAnchors && ((ISupportAnchors)persist).getAnchors() > 0 &&
				((ISupportAnchors)persist).getAnchors() != IAnchorConstants.DEFAULT)
			{
				int anchors = ((ISupportAnchors)persist).getAnchors();
				IDataRenderer renderer = ((Component)component).findParent(IDataRenderer.class);
				int partHeight = 0;
				if (renderer != null)
				{
					partHeight = renderer.getSize().height;
				}
				int designWidth = formController.getForm().getWidth();
				int designHeight = 0;
				Part part = formController.getForm().getPartAt(((BaseComponent)persist).getLocation().y);
				if (part != null)
				{
					int top = formController.getForm().getPartStartYPos(part.getID());
					designHeight = part.getHeight() - top;
				}
				if (partHeight > 0 && formWidth > 0 && designWidth > 0 && designHeight > 0)
				{
					int navid = formController.getForm().getNavigatorID();
					int navigatorWidth = 0;
					if (navid == Form.NAVIGATOR_DEFAULT && formController.getForm().getView() != FormController.TABLE_VIEW &&
						formController.getForm().getView() != FormController.LOCKED_TABLE_VIEW)
					{
						navigatorWidth = WebDefaultRecordNavigator.DEFAULT_WIDTH;
					}
					else if (navid != Form.NAVIGATOR_NONE)
					{
						ISupportNavigator navigatorSupport = findParent(ISupportNavigator.class);
						if (navigatorSupport != null)
						{
							FormController currentNavFC = navigatorSupport.getNavigator();
							if (currentNavFC != null)
							{
								navigatorWidth = currentNavFC.getForm().getWidth();
							}
						}
					}
					Rectangle bounds = new Rectangle(component.getLocation(), component.getSize());
					if ((anchors & IAnchorConstants.EAST) != 0 && (anchors & IAnchorConstants.WEST) == 0)
					{
						bounds.x += formWidth - designWidth - navigatorWidth;
					}
					if ((anchors & IAnchorConstants.SOUTH) != 0 && (anchors & IAnchorConstants.NORTH) == 0)
					{
						bounds.y += partHeight - designHeight;
					}
					if ((anchors & IAnchorConstants.EAST) != 0 && (anchors & IAnchorConstants.WEST) != 0 && (formWidth - designWidth - navigatorWidth > 0))
					{
						bounds.width += formWidth - designWidth - navigatorWidth;
					}
					if ((anchors & IAnchorConstants.SOUTH) != 0 && (anchors & IAnchorConstants.NORTH) != 0 && (partHeight - designHeight > 0))
					{
						bounds.height += partHeight - designHeight;
					}
					return bounds;
				}
			}
		}
		return null;
	}

	@Override
	public void changeFocusIfInvalid(List<Runnable> invokeLaterRunnables)
	{
		// not used
	}

	/*
	 * @see com.servoy.j2db.ISupportFormExecutionState#formMethodExecution()
	 */
	@Override
	public FormExecutionState formMethodExecution()
	{
		IBasicMainContainer currentContainer = getController().getBasicFormManager().getCurrentContainer();
		MainPage formPage = getMainPage();
		if (currentContainer != formPage)
		{
			FormExecutionState formExecutionState = new FormExecutionState();
			formExecutionState.mainContainer = currentContainer;
			formExecutionState.mainContainerName = currentContainer.getContainerName();
			((WebFormManager)getController().getBasicFormManager()).setCurrentContainer(formPage, getContainerName());
			return formExecutionState;
		}
		else return null;
	}

	/*
	 * @see com.servoy.j2db.ISupportFormExecutionState#formMethodExecuted(com.servoy.j2db.FormExecutionState)
	 */
	@Override
	public void formMethodExecuted(FormExecutionState formExecutionState)
	{
		if (formExecutionState != null)
		{
			((WebFormManager)getController().getBasicFormManager()).setCurrentContainer((IMainContainer)formExecutionState.mainContainer,
				formExecutionState.mainContainerName);
		}
	}
}
