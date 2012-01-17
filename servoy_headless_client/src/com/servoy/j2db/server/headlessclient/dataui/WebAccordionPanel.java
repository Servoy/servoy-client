/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.version.undo.Change;
import org.odlabs.wiquery.ui.accordion.Accordion;
import org.odlabs.wiquery.ui.accordion.AccordionAnimated;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISaveConstants;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.TabIndexHelper;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.server.headlessclient.dataui.WebTabPanel.ServoyTabIcon;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.ui.scripting.RuntimeAccordionPanel;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents an accordion panel in the webbrowser.
 * 
 * @author lvostinar
 * @since 6.1
 *
 */
public class WebAccordionPanel extends WebMarkupContainer implements ITabPanel, IDisplayRelatedData, IProviderStylePropertyChanges, ISupportSecuritySettings,
	ISupportWebBounds, ISupportWebTabSeq, ListSelectionListener, IWebFormContainer
{
	private static final long serialVersionUID = 1L;

	private final IApplication application;
	private WebTabFormLookup currentForm;
	private Accordion accordion;
	protected IRecordInternal parentData;
	private final List<String> allRelationNames = new ArrayList<String>(5);
	protected final List<WebTabHolder> allTabs = new ArrayList<WebTabHolder>(5);
	private final List<ISwingFoundSet> related = new ArrayList<ISwingFoundSet>();

	private IScriptExecuter scriptExecutor;

	private String onTabChangeMethodCmd;
	private Object[] onTabChangeArgs;

	private int tabSequenceIndex = ISupportWebTabSeq.DEFAULT;
	private final RuntimeAccordionPanel scriptable;

	public WebAccordionPanel(IApplication application, final RuntimeAccordionPanel scriptable, String name)
	{
		super(name);
		this.application = application;

		setOutputMarkupPlaceholderTag(true);
		accordion = new Accordion("accordion_" + name); //$NON-NLS-1$
		add(accordion);
		// disable animation, see http://forum.jquery.com/topic/jquery-accordion-not-work-on-ie-7
		accordion.setAnimated(new AccordionAnimated(Boolean.FALSE));
		accordion.setFillSpace(true);
		IModel<Integer> tabsModel = new AbstractReadOnlyModel<Integer>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Integer getObject()
			{
				return Integer.valueOf(allTabs.size());
			}
		};
		final boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$

		accordion.add(new Loop("tabs", tabsModel) //$NON-NLS-1$
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(final LoopItem item)
			{
				item.setRenderBodyOnly(true);
				final WebTabHolder holder = allTabs.get(item.getIteration());
				ServoySubmitLink link = new ServoySubmitLink("tablink", useAJAX)//$NON-NLS-1$
				{
					@Override
					public void onClick(AjaxRequestTarget target)
					{
						Page page = findPage();
						if (page != null)
						{
							boolean needsRelayout = false;
							if (currentForm != null && currentForm != holder.getPanel() && currentForm.getFormName().equals(holder.getPanel().getFormName()))
							{
								needsRelayout = true;
							}
							setActiveTabPanel(holder.getPanel());
							if (target != null)
							{
								if (needsRelayout && page instanceof MainPage && ((MainPage)page).getController() != null)
								{
									if (Utils.getAsBoolean(((MainPage)page).getController().getApplication().getRuntimeProperties().get("enableAnchors"))) //$NON-NLS-1$
									{
										target.appendJavascript("layoutEntirePage();"); //$NON-NLS-1$
									}
								}
								relinkFormIfNeeded();
								accordion.activate(target, item.getIteration());
								WebEventExecutor.generateResponse(target, page);
							}
						}
					}

					@Override
					protected void disableLink(ComponentTag tag)
					{
						// do nothing here
					}
				};
				link.setEnabled(holder.isEnabled() && WebAccordionPanel.this.isEnabled());
				if (holder.getIcon() != null)
				{
					accordion.hideIcons();
				}
				if (holder.getTooltip() != null)
				{
					link.setMetaData(TooltipAttributeModifier.TOOLTIP_METADATA, holder.getTooltip());
				}
				TabIndexHelper.setUpTabIndexAttributeModifier(link, tabSequenceIndex);
				link.add(TooltipAttributeModifier.INSTANCE);

				Label label = new Label("linktext", new Model<String>(holder.getText())); //$NON-NLS-1$
				label.setEscapeModelStrings(false);
				link.add(label);
				ServoyTabIcon icon = new ServoyTabIcon("icon", holder, scriptable); //$NON-NLS-1$
				if (holder.getIcon() != null)
				{
					icon.add(new StyleAppendingModifier(new Model<String>()
					{
						@Override
						public String getObject()
						{
							return "float: left;"; //$NON-NLS-1$
						}
					}));
				}
				link.add(icon);
				item.add(link);
				item.add(new Label("webform", new Model<String>(""))); // temporary add  //$NON-NLS-1$//$NON-NLS-2$
				label.add(new StyleAppendingModifier(new Model<String>()
				{
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject()
					{
						String style = "white-space: nowrap;"; //$NON-NLS-1$
						if (font != null)
						{
							Pair<String, String>[] fontPropetiesPair = PersistHelper.createFontCSSProperties(PersistHelper.createFontString(font));
							if (fontPropetiesPair != null)
							{
								for (Pair<String, String> element : fontPropetiesPair)
								{
									if (element == null) continue;
									style += element.getLeft() + ": " + element.getRight() + ";"; //$NON-NLS-1$ //$NON-NLS-2$
								}
							}
						}
						if (holder.getForeground() != null)
						{
							style += " color:" + PersistHelper.createColorString(holder.getForeground()); //$NON-NLS-1$
						}
						else if (foreground != null)
						{
							style += " color:" + PersistHelper.createColorString(foreground); //$NON-NLS-1$
						}
						return style;
					}
				}));
			}
		});
		add(new AbstractServoyDefaultAjaxBehavior()
		{
			@Override
			protected void respond(AjaxRequestTarget target)
			{
			}

			@Override
			public void renderHead(IHeaderResponse response)
			{
				super.renderHead(response);
				int index = getTabIndex();
				if (index > 0) // first tab will be activated by default
				{
					response.renderOnDomReadyJavascript(accordion.activate(index).getStatement().toString());
				}
				// avoid flickering, see also tabpanel
				response.renderOnDomReadyJavascript("var accordion = document.getElementById('" + WebAccordionPanel.this.getMarkupId() +
					"');if (accordion){accordion.style.visibility = 'visible';}");
			}
		});

		add(new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				return "visibility: hidden;"; //$NON-NLS-1$
			}
		}));
		add(StyleAttributeModifierModel.INSTANCE);
		this.scriptable = scriptable;
		((ChangesRecorder)scriptable.getChangesRecorder()).setDefaultBorderAndPadding(null, TemplateGenerator.DEFAULT_LABEL_PADDING);
	}

	public final RuntimeAccordionPanel getScriptObject()
	{
		return scriptable;
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
	}

	private void setActiveTabPanel(WebTabFormLookup fl)
	{
		if (fl != currentForm)
		{
			WebTabFormLookup previous = currentForm;

			int stopped = application.getFoundSetManager().getEditRecordList().stopEditing(false);
			boolean cantStop = stopped != ISaveConstants.STOPPED && stopped != ISaveConstants.AUTO_SAVE_BLOCKED;
			if (previous != null)
			{
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
				boolean ok = previous.notifyVisible(false, invokeLaterRunnables);
				Utils.invokeLater(application, invokeLaterRunnables);
				if (cantStop || !ok)
				{
					return;
				}
			}

			int previousIndex = -1;
			for (int i = 0; i < allTabs.size(); i++)
			{
				WebTabHolder holder = allTabs.get(i);
				if (holder.getPanel() == previous)
				{
					previousIndex = i;
					break;
				}
			}

			if (previousIndex != -1)
			{
				final int changedIndex = previousIndex;
				addStateChange(new Change()
				{
					@Override
					public void undo()
					{
						if (allTabs.size() > changedIndex)
						{
							WebTabHolder holder = allTabs.get(changedIndex);
							setActiveTabPanel(holder.getPanel());
						}
					}
				});
			}

			List<Runnable> invokeLaterRunnables2 = new ArrayList<Runnable>();
			setCurrentForm(fl, previousIndex, invokeLaterRunnables2);
			Utils.invokeLater(application, invokeLaterRunnables2);
		}
	}

	private void setCurrentForm(WebTabFormLookup fl, int previousIndex, List<Runnable> invokeLaterRunnables)
	{
		if (fl != null && !fl.isFormReady()) return;

		getStylePropertyChanges().setChanged();
		currentForm = fl;
		if (parentData != null)
		{
			showFoundSet(currentForm, parentData, getDefaultSort());
		}

		// Test if current one is there
		if (currentForm.isReady())
		{
			addCurrentFormComponent();
			recomputeTabSequence();
			boolean visible = true;
			WebForm webform = findParent(WebForm.class);
			if (webform != null)
			{
				visible = webform.getController().isFormVisible();
			}
			currentForm.notifyVisible(visible, invokeLaterRunnables);

			if (onTabChangeMethodCmd != null && previousIndex != -1)
			{
				scriptExecutor.executeFunction(onTabChangeMethodCmd, Utils.arrayMerge((new Object[] { Integer.valueOf(previousIndex + 1) }), onTabChangeArgs),
					true, this, false, StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID.getPropertyName(), false);
			}
		}
	}

	private void addCurrentFormComponent()
	{
		if (currentForm.isReady())
		{
			int index = getTabIndex();
			if (index >= 0)
			{
				MarkupContainer parent = (MarkupContainer)((MarkupContainer)accordion.get(0)).get(index);
				if (parent != null)
				{
					if (parent.get(currentForm.getWebForm().getId()) != null)
					{
						// replace it
						parent.replace(currentForm.getWebForm());
					}
					else
					{
						// else add it
						parent.add(currentForm.getWebForm());
					}
				}
			}
		}
	}

	public WebForm getCurrentForm()
	{
		return currentForm != null ? currentForm.getWebForm() : null;
	}

	/**
	 * @see org.apache.wicket.MarkupContainer#remove(org.apache.wicket.Component)
	 */
	@Override
	public void remove(Component component)
	{
		if (currentForm != null && currentForm.isReady() && component == currentForm.getWebForm())
		{
			currentForm.setWebForm(null);
			//replace(new Label("webform", new Model<String>("")));
		}
		else super.remove(component);
	}

	private void recomputeTabSequence()
	{
		FormController fc = currentForm.getWebForm().getController();
		fc.recomputeTabSequence(tabSequenceIndex);
	}

	/**
	 * @see wicket.MarkupContainer#onRender(wicket.markup.MarkupStream)
	 */
	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		getStylePropertyChanges().setRendered();
	}

	@Override
	protected void onBeforeRender()
	{
		//tab has to be initialized now.. see also MainPage.listview.onBeforRender..
		initalizeFirstTab();
		super.onBeforeRender();
		relinkFormIfNeeded();

	}

	public void initalizeFirstTab()
	{
		if (currentForm == null && allTabs.size() > 0)
		{
			WebTabHolder holder = allTabs.get(0);
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			setCurrentForm(holder.getPanel(), -1, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
		}
		else if (currentForm != null && currentForm.getWebForm() == null)
		{
			// webForm was removed from this tabpanel of the current Form (reuse or destroyed)
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			setCurrentForm(currentForm, -1, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);

		}
		return;
	}

	private void relinkFormIfNeeded()
	{
		if (currentForm != null && isVisibleInHierarchy() && currentForm.getWebForm().findParent(ITabPanel.class) != this)
		{
			addCurrentFormComponent();
		}
	}

	public void setTabLayoutPolicy(int scroll_tab_layout)
	{
		//TODO ignore???
	}

	public IFormLookupPanel createFormLookupPanel(String tabname, String relationName, String formName)
	{
		return new WebTabFormLookup(tabname, relationName, formName, this, application);
	}

	public void setCursor(Cursor cursor)
	{
	}

	public void setValidationEnabled(boolean b)
	{
	}

	public void notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		if (currentForm == null && allTabs.size() > 0)
		{
			WebTabHolder holder = allTabs.get(0);
			setCurrentForm(holder.getPanel(), -1, invokeLaterRunnables);
		}
		if (currentForm != null)
		{
			FormController controller = currentForm.getWebForm().getController();

			//this is not needed when closing
			if (visible && parentData != null)
			{
				showFoundSet(currentForm, parentData, controller.getDefaultSortColumns());

				// Test if current one is there
				if (currentForm.isReady())
				{
					addCurrentFormComponent();
					recomputeTabSequence();
				}
			}
			controller.notifyVisible(visible, invokeLaterRunnables);
		}
	}

	public void notifyResized()
	{
		if (currentForm != null && currentForm.isReady())
		{
			WebForm webForm = currentForm.getWebForm();
			FormController controller = webForm.getController();
			if (controller != null && webForm.isFormWidthHeightChanged())
			{
				controller.notifyResized();
				webForm.clearFormWidthHeightChangedFlag();
			}
		}
	}

	public void setRecord(IRecordInternal parentState, boolean stopEditing)
	{
		parentData = parentState;
		if (currentForm != null)
		{

			showFoundSet(currentForm, parentState, getDefaultSort());
		}
		else if (allTabs.size() > 0)
		{
			showFoundSet(allTabs.get(0).getPanel(), parentState, getDefaultSort());
		}
		ITagResolver resolver = getTagResolver(parentState);
		for (int i = 0; i < allTabs.size(); i++)
		{
			WebTabHolder element = allTabs.get(i);
			if (element.getTagText() != null)
			{
				String t = Text.processTags(element.getTagText(), resolver);
				String elementNewText = Utils.stringReplace(TemplateGenerator.getSafeText(t), " ", "&nbsp;"); //$NON-NLS-1$ //$NON-NLS-2$
				if (!element.getText().equals(elementNewText))
				{
					element.setText(elementNewText);
					getStylePropertyChanges().setChanged();
				}
			}
		}
	}

	private ITagResolver getTagResolver(IRecordInternal parentState)
	{
		ITagResolver resolver;
		WebForm webForm = findParent(WebForm.class);
		if (webForm != null)
		{
			resolver = webForm.getController().getTagResolver();
		}
		else
		{
			resolver = TagResolver.createResolver(parentState);
		}
		return resolver;
	}

	protected void showFoundSet(WebTabFormLookup flp, IRecordInternal parentState, List<SortColumn> sort)
	{
		deregisterSelectionListeners();

		if (!flp.isReady()) return;

		FormController fp = flp.getWebForm().getController();
		if (fp != null && flp.getRelationName() != null)
		{
			IFoundSetInternal relatedFoundset = parentState == null ? null : parentState.getRelatedFoundSet(flp.getRelationName(), sort);
			if (relatedFoundset != null) registerSelectionListeners(parentState, flp.getRelationName());
			fp.loadData(relatedFoundset, null);
		}

		ITagResolver resolver = getTagResolver(parentState);
		//refresh tab text
		for (int i = 0; i < allTabs.size(); i++)
		{
			WebTabHolder element = allTabs.get(i);
			if (element.getPanel() == flp)
			{
				if (element.getTagText() != null)
				{
					String t = Text.processTags(element.getTagText(), resolver);
					element.setText(Utils.stringReplace(TemplateGenerator.getSafeText(t), " ", "&nbsp;")); //$NON-NLS-1$ //$NON-NLS-2$
				}
				break;
			}
		}
	}

	private void registerSelectionListeners(IRecordInternal parentState, String relationName)
	{
		String[] parts = relationName.split("\\."); //$NON-NLS-1$
		IRecordInternal currentRecord = parentState;
		for (int i = 0; currentRecord != null && i < parts.length - 1; i++)
		{
			IFoundSetInternal fs = currentRecord.getRelatedFoundSet(parts[i], null);
			if (fs instanceof ISwingFoundSet)
			{
				related.add((ISwingFoundSet)fs);
				((ISwingFoundSet)fs).getSelectionModel().addListSelectionListener(this);
			}
			currentRecord = (fs == null) ? null : fs.getRecord(fs.getSelectedIndex());
		}
	}

	private void deregisterSelectionListeners()
	{
		for (ISwingFoundSet fs : related)
		{
			fs.getSelectionModel().removeListSelectionListener(this);
		}
		related.clear();
	}

	public String getSelectedRelationName()
	{
		if (currentForm != null)
		{
			return currentForm.getRelationName();
		}
		return null;
	}

	public String[] getAllRelationNames()
	{
		String[] retval = new String[allRelationNames.size()];
		for (int i = 0; i < retval.length; i++)
		{
			Object relationName = allRelationNames.get(i);
			if (relationName != null)
			{
				retval[i] = relationName.toString();
			}
		}
		return retval;
	}

	public List<SortColumn> getDefaultSort()
	{
		if (currentForm != null)
		{
			// extra test, if the current record is null and the form is not ready just return an empty list.
			// record can be null in the destroy, then creating the form doesn't make any sense.
			return currentForm.getDefaultSort(parentData != null || currentForm.isReady());
		}
		else if (allTabs.size() > 0)
		{
			WebTabHolder holder = allTabs.get(0);
			return holder.getPanel().getDefaultSort(parentData != null || holder.getPanel().isReady());
		}
		return null;
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (currentForm != null && currentForm.isReady())
		{
			return currentForm.getWebForm().getController().stopUIEditing(true);
		}
		return true;
	}

	public void destroy()
	{
		deregisterSelectionListeners();

		//TODO should deregister related foundsets??
	}

	public void addTab(String text, int iconMediaId, IFormLookupPanel flp, String tip)
	{
		byte[] iconData = ComponentFactory.loadIcon(application.getFlattenedSolution(), new Integer(iconMediaId));
		insertTab(text, iconData, flp, tip, allTabs.size());
	}

	public void addTab(String text, byte[] iconData, IFormLookupPanel flp, String tip)
	{
		insertTab(text, iconData, flp, tip, allTabs.size());
	}

	public void insertTab(String text, byte[] iconData, IFormLookupPanel flp, String tip, int index)
	{
		allTabs.add(index, new WebTabHolder(text, flp, iconData, tip));
		allRelationNames.add(index, flp.getRelationName());
		getStylePropertyChanges().setChanged();
	}

	public void setTabForegroundAt(int index, Color fg)
	{
		if (index >= 0 && index < allTabs.size())
		{
			WebTabHolder holder = allTabs.get(index);
			holder.setForeground(fg);
		}
	}

	public void setTabBackgroundAt(int index, Color bg)
	{
	}

	public boolean addTab(IForm formController, String formName, String tabname, String tabText, String tabtooltip, String iconURL, String fg, String bg,
		String relationName, RelatedFoundSet relatedFs, int idx)
	{
		if (formController != null)
		{
			//to make sure we don't have recursion on adding a tab, to a tabpanel, that is based 
			//on the form that the tabpanel is placed on
			WebForm webForm = findParent(WebForm.class);
			if (webForm != null)
			{
				FormController parentFormController = webForm.getController();
				if (parentFormController != null && parentFormController.equals(formController))
				{
					return false;
				}
			}
		}

		WebTabFormLookup flp = (WebTabFormLookup)createFormLookupPanel(tabname, relationName, formName);
		if (formController != null) flp.setReadOnly(formController.isReadOnly());
		byte[] iconData = null;
		//TODO handle icon

		int count = allTabs.size();
		int tabIndex = idx;
		if (tabIndex == -1 || tabIndex >= count)
		{
			tabIndex = count;
			addTab(application.getI18NMessageIfPrefixed(tabText), iconData, flp, application.getI18NMessageIfPrefixed(tabtooltip));
		}
		else
		{
			insertTab(application.getI18NMessageIfPrefixed(tabText), iconData, flp, application.getI18NMessageIfPrefixed(tabtooltip), tabIndex);
		}
		if (fg != null) setTabForegroundAt(tabIndex, PersistHelper.createColor(fg));
		if (bg != null) setTabBackgroundAt(tabIndex, PersistHelper.createColor(bg));

		if (relatedFs != null && currentForm == flp)
		{
			FormController fp = flp.getWebForm().getController();
			if (fp != null && flp.getRelationName() != null && flp.getRelationName().equals(relationName))
			{
				fp.loadData(relatedFs, null);
			}
		}
		return true;
	}

	public int getMaxTabIndex()
	{
		return allTabs.size() - 1;
	}

	public String getTabFormNameAt(int i)
	{
		WebTabHolder holder = allTabs.get(i);
		return holder.getPanel().getFormName();
	}

	public int getTabIndex()
	{
		for (int i = 0; i < allTabs.size(); i++)
		{
			if (currentForm == null)
			{
				// no current form set yet, default to first tab
				return 0;
			}
			if (allTabs.get(i).getPanel() == currentForm)
			{
				return i;
			}
		}
		return -1;
	}

	public String getTabNameAt(int i)
	{
		WebTabHolder holder = allTabs.get(i);
		return holder.getPanel().getName();
	}

	public String getTabTextAt(int i)
	{
		WebTabHolder holder = allTabs.get(i);
		return holder.getText();
	}

	public boolean isTabEnabledAt(int index)
	{
		WebTabHolder holder = allTabs.get(index);
		return holder.isEnabled();
	}

	public boolean removeTabAt(int index)
	{
		WebTabHolder holder = allTabs.get(index);
		List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
		boolean ok = holder.getPanel().notifyVisible(false, invokeLaterRunnables);
		Utils.invokeLater(application, invokeLaterRunnables);
		if (!ok)
		{
			return false;
		}
		allTabs.remove(index);
		if (holder.getPanel() == currentForm)
		{
			if (allTabs.size() > 0)
			{
				setActiveTabPanel(allTabs.get(0).getPanel());
			}
			else
			{
				//safety
				currentForm = null;
			}
		}
		return true;
	}

	public boolean removeAllTabs()
	{
		for (int i = 0; i < allTabs.size(); i++)
		{
			WebTabHolder comp = allTabs.get(i);
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			boolean ok = comp.getPanel().notifyVisible(false, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
			if (!ok)
			{
				return false;
			}
		}
		allTabs.clear();
		allRelationNames.clear();

		//safety
		currentForm = null;

		return true;
	}

	public void setTabEnabledAt(int i, boolean b)
	{
		WebTabHolder holder = allTabs.get(i);
		holder.setEnabled(b);
	}

	public void setTabIndex(int index)
	{
		setActiveTabPanel(allTabs.get(index).getPanel());
	}

	public void setTabIndex(String name)
	{
		for (int i = 0; i < allTabs.size(); i++)
		{
			WebTabHolder holder = allTabs.get(i);
			if (Utils.stringSafeEquals(holder.getPanel().getName(), name))
			{
				setActiveTabPanel(holder.getPanel());
				break;
			}
		}
	}

	public void setTabTextAt(int i, String s)
	{
		WebTabHolder holder = allTabs.get(i);
		holder.setText(TemplateGenerator.getSafeText(s));
	}

	/*
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		if (currentForm != null)
		{
			return currentForm.isReadOnly();
		}
		return false;
	}

	public void setReadOnly(boolean b)
	{
		for (int i = 0; i < allTabs.size(); i++)
		{
			WebTabHolder holder = allTabs.get(i);
			holder.getPanel().setReadOnly(b);
		}
	}

	public void setName(String n)
	{
		name = n;
	}

	private String name;

	public String getName()
	{
		return name;
	}


	/*
	 * border---------------------------------------------------
	 */
	private Border border;

	public void setBorder(Border border)
	{
		this.border = border;
	}

	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	private boolean opaque;

	public boolean isOpaque()
	{
		return opaque;
	}


	private String tooltip;

	public void setToolTipText(String tooltip)
	{
		this.tooltip = Utils.stringIsEmpty(tooltip) ? null : tooltip;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		return tooltip;
	}


	/*
	 * font---------------------------------------------------
	 */
	public void setFont(Font font)
	{
		this.font = font;
	}

	private Font font;

	public Font getFont()
	{
		return font;
	}

	private Color background;

	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	public Color getBackground()
	{
		return background;
	}


	private Color foreground;

	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	public Color getForeground()
	{
		return foreground;
	}


	/*
	 * visible---------------------------------------------------
	 */
	public void setComponentVisible(boolean visible)
	{
		if (viewable)
		{
			setVisible(visible);
		}
	}


	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
			accordion.setDisabled(!b);
			visitChildren(IComponent.class, new IVisitor<Component>()
			{
				public Object component(Component component)
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
			getStylePropertyChanges().setChanged();
		}
	}

	private boolean accessible = true;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}

	private boolean viewable = true;

	public void setViewable(boolean b)
	{
		if (!b) setComponentVisible(b);
		this.viewable = b;
	}

	public boolean isViewable()
	{
		return viewable;
	}

	/*
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

	public int getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	public void setLocation(Point location)
	{
		this.location = location;
	}

	public Point getLocation()
	{
		return location;
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	public Dimension getSize()
	{
		return size;
	}

	public Rectangle getWebBounds()
	{
		Dimension d = ((ChangesRecorder)getStylePropertyChanges()).calculateWebSize(size.width, size.height, border, new Insets(0, 0, 0, 0), 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)getStylePropertyChanges()).getPaddingAndBorder(size.height, border, new Insets(0, 0, 0, 0), 0, null);
	}


	public void setSize(Dimension size)
	{
		if (this.size != null && currentForm != null && currentForm.isReady())
		{
			currentForm.getWebForm().setFormWidth(0);
		}
		this.size = size;
	}

	/**
	 * @see com.servoy.j2db.ui.ITabPanel#addScriptExecuter(com.servoy.j2db.IScriptExecuter)
	 */
	public void addScriptExecuter(IScriptExecuter executor)
	{
		this.scriptExecutor = executor;
	}

	/**
	 * @see com.servoy.j2db.ui.ITabPanel#setOnTabChangeMethodCmd(java.lang.String, TabPanel)
	 */
	public void setOnTabChangeMethodCmd(String onTabChangeMethodCmd, Object[] onTabChangeArgs)
	{
		this.onTabChangeMethodCmd = onTabChangeMethodCmd;
		this.onTabChangeArgs = onTabChangeArgs;
	}

	public void setTabSequenceIndex(int tabIndex)
	{
		this.tabSequenceIndex = tabIndex;
	}

	public int getTabIndex(WebForm current)
	{
		if (currentForm != null && currentForm.getWebForm() == current)
		{
			Object o = scriptable.js_getTabIndex();
			if (o instanceof Integer)
			{
				if (((Integer)o).intValue() == -1) return -1;
				return ((Integer)o).intValue() - 1;
			}
		}
		for (int i = 0; i < allTabs.size(); i++)
		{
			WebTabHolder holder = allTabs.get(i);
			if (holder.getPanel().getFormName() == current.getController().getName())
			{
				return i;
			}
		}
		return -1;
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (parentData != null)
		{
			showFoundSet(currentForm, parentData, getDefaultSort());
		}
	}

	public void setHorizontalAlignment(int alignment)
	{

	}
}
