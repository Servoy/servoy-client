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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IResourceListener;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.version.undo.Change;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
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
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.TabIndexHelper;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.ui.IAccessible;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.IAnchorConstants;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents a tabpanel in the webbrowser.
 * 
 * @author jcompagner
 */
public class WebTabPanel extends WebMarkupContainer implements ITabPanel, IDisplayRelatedData, IProviderStylePropertyChanges, IAccessible, ISupportWebBounds,
	ISupportWebTabSeq, ListSelectionListener
{
	private static final long serialVersionUID = 1L;

	private final IApplication application;
	private WebTabFormLookup currentForm;
	protected IRecordInternal parentData;
	private final List<String> allRelationNames = new ArrayList<String>(5);
	protected final List<WebTabHolder> allTabs = new ArrayList<WebTabHolder>(5);
	private final ChangesRecorder jsChangeRecorder = new ChangesRecorder(new Insets(0, 0, 0, 0), new Insets(0, 0, 0, 0));
	private final List<ISwingFoundSet> related = new ArrayList<ISwingFoundSet>();

	private IScriptExecuter scriptExecutor;

	private String onTabChangeMethodCmd;
	private Object[] onTabChangeArgs;

	protected final int orient;
	private int tabSequenceIndex = ISupportWebTabSeq.DEFAULT;
	private Dimension tabSize;

	public WebTabPanel(IApplication application, String name, int orient, boolean oneTab)
	{
		super(name);
		this.application = application;
		this.orient = orient;

		final boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		setOutputMarkupPlaceholderTag(true);

		if (orient != TabPanel.SPLIT_HORIZONTAL && orient != TabPanel.SPLIT_VERTICAL) add(new Label("webform", new Model<String>("")));//temporary add, in case the tab panel does not contain any tabs //$NON-NLS-1$ //$NON-NLS-2$

		// TODO check ignore orient and oneTab??
		IModel<Integer> tabsModel = new AbstractReadOnlyModel<Integer>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Integer getObject()
			{
				return new Integer(allTabs.size());
			}
		};

		if (orient != TabPanel.HIDE && orient != TabPanel.SPLIT_HORIZONTAL && orient != TabPanel.SPLIT_VERTICAL && !(orient == TabPanel.DEFAULT && oneTab))
		{
			add(new Loop("tablinks", tabsModel) //$NON-NLS-1$
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void populateItem(LoopItem item)
				{
					final WebTabHolder holder = allTabs.get(item.getIteration());
					MarkupContainer link = null;
					link = new ServoySubmitLink("tablink", useAJAX) //$NON-NLS-1$
					{
						private static final long serialVersionUID = 1L;

						/**
						 * @see wicket.ajax.markup.html.AjaxFallbackLink#onClick(wicket.ajax.AjaxRequestTarget)
						 */
						@Override
						public void onClick(AjaxRequestTarget target)
						{
							Page page = findPage();
							if (page != null)
							{
								setActiveTabPanel(holder.getPanel());
								if (target != null)
								{
									if (currentForm != null) addFormForFullAnchorRendering(currentForm.getWebForm(), (MainPage)page);
									relinkAtTabPanel(WebTabPanel.this);
									WebEventExecutor.generateResponse(target, page);
								}
							}
						}

						private void addFormForFullAnchorRendering(WebForm form, final MainPage mainPage)
						{
							mainPage.addFormForFullAnchorRendering(form);
							form.visitChildren(WebTabPanel.class, new IVisitor<WebTabPanel>()
							{
								public Object component(WebTabPanel tabPanel)
								{
									if (tabPanel.currentForm != null) addFormForFullAnchorRendering(tabPanel.currentForm.getWebForm(), mainPage);
									return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
								}
							});
						}

						private void relinkAtForm(WebForm form)
						{
							form.visitChildren(WebTabPanel.class, new IVisitor<WebTabPanel>()
							{
								public Object component(WebTabPanel wtp)
								{
									relinkAtTabPanel(wtp);
									return IVisitor.CONTINUE_TRAVERSAL;
								}
							});
						}

						private void relinkAtTabPanel(WebTabPanel wtp)
						{
							wtp.relinkFormIfNeeded();
							wtp.visitChildren(WebForm.class, new IVisitor<WebForm>()
							{
								public Object component(WebForm form)
								{
									relinkAtForm(form);
									return IVisitor.CONTINUE_TRAVERSAL;
								}
							});
						}

						@Override
						protected void disableLink(final ComponentTag tag)
						{
							// if the tag is an anchor proper
							if (tag.getName().equalsIgnoreCase("a") || tag.getName().equalsIgnoreCase("link") || tag.getName().equalsIgnoreCase("area")) //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
							{
								// Remove any href from the old link
								tag.remove("href"); //$NON-NLS-1$
								tag.remove("onclick"); //$NON-NLS-1$
							}
						}

					};
					TabIndexHelper.setUpTabIndexAttributeModifier(link, tabSequenceIndex);

					if (item.getIteration() == 0) link.add(new AttributeModifier("firsttab", true, new Model<Boolean>(Boolean.TRUE))); //$NON-NLS-1$
					link.setEnabled(holder.isEnabled() && WebTabPanel.this.isEnabled());

//					ServoyTabIcon tabIcon = new ServoyTabIcon("icon", holder); //$NON-NLS-1$
//					link.add(tabIcon);

					Label label = new Label("linktext", new Model<String>(holder.getText())); //$NON-NLS-1$
					label.setEscapeModelStrings(false);
					link.add(label);
					item.add(link);
					IModel<String> selectedOrDisabledClass = new AbstractReadOnlyModel<String>()
					{
						private static final long serialVersionUID = 1L;

						@Override
						public String getObject()
						{
							if (!holder.isEnabled() || !WebTabPanel.this.isEnabled())
							{
								if (currentForm == holder.getPanel())
								{
									return "disabled_selected_tab"; //$NON-NLS-1$
								}
								return "disabled_tab"; //$NON-NLS-1$
							}
							else
							{
								if (currentForm == holder.getPanel())
								{
									return "selected_tab"; //$NON-NLS-1$
								}
								return "deselected_tab"; //$NON-NLS-1$
							}
						}
					};
					item.add(new AttributeModifier("class", true, selectedOrDisabledClass)); //$NON-NLS-1$
					label.add(new StyleAppendingModifier(new Model<String>()
					{
						private static final long serialVersionUID = 1L;

						@Override
						public String getObject()
						{
							String style = "white-space: nowrap;"; //$NON-NLS-1$
							if (foreground != null)
							{
								style += " color:" + PersistHelper.createColorString(foreground); //$NON-NLS-1$
							}
							return style;
						}
					}));
				}
			});

			// All tab panels get their tabs rearranged after they make it to the browser.
			// On Chrome & Safari the tab rearrangement produces an ugly flicker effect, because
			// initially the tabs are not visible and then they are made visible. By
			// sending the tab as invisible and turning it to visible only after the tabs
			// are arranged, this jumping/flickering effect is gone. However a small delay can now be
			// noticed in Chrome & Safari, which should also be eliminated somehow.
			// The tab panel is set to visible in function "rearrageTabsInTabPanel" from "servoy.js".
			add(new StyleAppendingModifier(new Model<String>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject()
				{
					return "visibility: hidden;"; //$NON-NLS-1$
				}
			}));

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
					boolean dontRearrangeHere = false;

					if (!(getRequestCycle().getRequestTarget() instanceof AjaxRequestTarget) &&
						Utils.getAsBoolean(((MainPage)getPage()).getController().getApplication().getRuntimeProperties().get("enableAnchors"))) //$NON-NLS-1$
					{
						Component parentForm = getParent();
						while ((parentForm != null) && !(parentForm instanceof WebForm))
							parentForm = parentForm.getParent();
						if (parentForm != null)
						{
							int anch = ((WebForm)parentForm).getAnchors(WebTabPanel.this.getMarkupId());
							if (anch != 0 && anch != IAnchorConstants.DEFAULT) dontRearrangeHere = true;
						}
					}
					if (!dontRearrangeHere)
					{
						String jsCall = "rearrageTabsInTabPanel('" + WebTabPanel.this.getMarkupId() + "');"; //$NON-NLS-1$ //$NON-NLS-2$
						// Safari and Konqueror have some problems with the "domready" event, so for those 
						// browsers we'll use the "load" event. Otherwise use "domready", it reduces the flicker
						// effect when rearranging the tabs.
						ClientProperties clp = ((WebClientInfo)Session.get().getClientInfo()).getProperties();
						if (clp.isBrowserKonqueror() || clp.isBrowserSafari()) response.renderOnLoadJavascript(jsCall);
						else response.renderOnDomReadyJavascript(jsCall);
					}
				}

			});
		}
		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);
	}

	/**
	 * @return the orient
	 */
	public int getOrient()
	{
		return orient;
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
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

	/**
	 * @param fl
	 * @param previousIndex
	 */
	private void setCurrentForm(WebTabFormLookup fl, int previousIndex, List<Runnable> invokeLaterRunnables)
	{
		jsChangeRecorder.setChanged();
		currentForm = fl;
		if (parentData != null)
		{
			showFoundSet(currentForm, parentData, getDefaultSort());
		}

		// Test if current one is there
		if (currentForm.isReady())
		{
			if (WebTabPanel.this.get(currentForm.getWebForm().getId()) != null)
			{
				// replace it
				WebTabPanel.this.replace(currentForm.getWebForm());
			}
			else
			{
				// else add it
				WebTabPanel.this.add(currentForm.getWebForm());
			}
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
				scriptExecutor.executeFunction(onTabChangeMethodCmd, Utils.arrayMerge((new Object[] { new Integer(previousIndex + 1) }), onTabChangeArgs),
					true, this, false, "onTabChangeMethodID", false); //$NON-NLS-1$
			}
		}
	}

	/**
	 * @see org.apache.wicket.MarkupContainer#remove(org.apache.wicket.Component)
	 */
	@Override
	public void remove(Component component)
	{
		if (currentForm != null && component == currentForm.getWebForm())
		{
			currentForm.setWebForm(null);
		}
		super.remove(component);
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
		jsChangeRecorder.setRendered();
	}


	/**
	 * @return
	 */
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
		if (currentForm != null && isVisibleInHierarchy() && currentForm.getWebForm().getParent() != this)
		{
			if (get(currentForm.getWebForm().getId()) != null)
			{
				// replace it
				replace(currentForm.getWebForm());
			}
			else
			{
				// else add it
				add(currentForm.getWebForm());
			}
		}
	}

	/**
	 * @see wicket.Component#onAttach()
	 */
	@Override
	protected void onBeforeRender()
	{
		if (orient != TabPanel.SPLIT_HORIZONTAL && orient != TabPanel.SPLIT_VERTICAL)
		{
			//tab has to be initialized now.. see also MainPage.listview.onBeforRender..
			initalizeFirstTab();
			super.onBeforeRender();
			relinkFormIfNeeded();
		}
		else super.onBeforeRender();

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
					if (WebTabPanel.this.get(currentForm.getWebForm().getId()) != null)
					{
						// replace it
						WebTabPanel.this.replace(currentForm.getWebForm());
					}
					else
					{
						// else add it
						WebTabPanel.this.add(currentForm.getWebForm());
					}
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
					jsChangeRecorder.setChanged();
				}
			}
		}
	}

	/**
	 * @param parentState
	 * @return
	 */
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
		if (parentState != null && fp != null && flp.getRelationName() != null)
		{
			IFoundSetInternal relatedFoundset = parentState.getRelatedFoundSet(flp.getRelationName(), sort);
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

	/*
	 * tab support----------------------------------------------------------------------------
	 */
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
		jsChangeRecorder.setChanged();
	}

	public void setTabForegroundAt(int index, Color fg)
	{
	}

	public void setTabBackgroundAt(int index, Color bg)
	{
	}

	public boolean js_addTab(Object[] vargs)
	{
		if (vargs.length < 1) return false;

		int index = 0;
		Object form = vargs[index++];

		FormController f = null;
		String fName = null;
		boolean readOnly = false;
		if (form instanceof FormController)
		{
			f = (FormController)form;
			readOnly = f.isReadOnly();
		}
		if (form instanceof FormController.JSForm)
		{
			f = ((FormController.JSForm)form).getFormPanel();
			readOnly = f.isReadOnly();
		}
		if (f != null) fName = f.getName();
		if (form instanceof String) fName = (String)form;

		if (fName != null)
		{
			String tabName = fName;
			if (vargs.length > 6)
			{
				tabName = (String)vargs[index++];
			}
			String tabText = tabName;
			if (vargs.length >= 3)
			{
				tabText = (String)vargs[index++];
			}
			String tabTooltip = ""; //$NON-NLS-1$
			if (vargs.length >= 4)
			{
				tabTooltip = (String)vargs[index++];
			}
			String iconURL = ""; //$NON-NLS-1$
			if (vargs.length >= 5)
			{
				iconURL = (String)vargs[index++];
			}
			String fg = null;
			if (vargs.length >= 6)
			{
				fg = (String)vargs[index++];
			}
			String bg = null;
			if (vargs.length >= 7)
			{
				bg = (String)vargs[index++];
			}

			RelatedFoundSet relatedFs = null;
			String relationName = null;
			int tabIndex = -1;
			if (vargs.length > 7)
			{
				Object object = vargs[index++];
				if (object instanceof RelatedFoundSet)
				{
					relatedFs = (RelatedFoundSet)object;
				}
				else if (object instanceof String)
				{
					relationName = (String)object;
				}
				else if (object instanceof Number)
				{
					tabIndex = ((Number)object).intValue();
				}
			}
			if (vargs.length > 8)
			{
				tabIndex = Utils.getAsInteger(vargs[index++]);
			}

			if (relatedFs != null)
			{
				relationName = relatedFs.getRelationName();
				if (f != null && !relatedFs.getDataSource().equals(f.getDataSource()))
				{
					return false;
				}
				// TODO do this check to check if the parent table has this relation? How to get the parent table 
//				Table parentTable = null;
//				application.getSolution().getRelations(Solution.SOLUTION+Solution.MODULES, parentTable, true, false);
			}
			WebTabFormLookup flp = (WebTabFormLookup)createFormLookupPanel(tabName, relationName, fName);
			if (f != null) flp.setReadOnly(readOnly);
			byte[] iconData = null;
			//TODO handle icon

			int count = allTabs.size();
			if (tabIndex == -1 || tabIndex >= count)
			{
				tabIndex = count;
				addTab(application.getI18NMessageIfPrefixed(tabText), iconData, flp, application.getI18NMessageIfPrefixed(tabTooltip));
			}
			else
			{
				insertTab(application.getI18NMessageIfPrefixed(tabText), iconData, flp, application.getI18NMessageIfPrefixed(tabTooltip), tabIndex);
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
		return false;
	}

	public int js_getMaxTabIndex()
	{
		return allTabs.size();
	}

	public String js_getTabBGColorAt(int i)
	{
		return null;
	}

	public String js_getTabFGColorAt(int i)
	{
		return null;
	}

	public String js_getTabFormNameAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			WebTabHolder holder = allTabs.get(i - 1);
			return holder.getPanel().getFormName();
		}
		return null;
	}

	public String js_getTabRelationNameAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return allRelationNames.get(i - 1);
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return "TABPANEL"; //$NON-NLS-1$
	}

	public Object js_getTabIndex()
	{
		for (int i = 0; i < allTabs.size(); i++)
		{
			if (currentForm == null)
			{
				// no current form set yet, default to first tab
				return new Integer(1);
			}
			WebTabHolder holder = allTabs.get(i);
			if (holder.getPanel() == currentForm)
			{
				return new Integer(i + 1);
			}
		}
		return new Integer(-1);
	}

	public String js_getTabNameAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			WebTabHolder holder = allTabs.get(i - 1);
			return holder.getPanel().getName();
		}
		return null;
	}

	public String js_getTabTextAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			WebTabHolder holder = allTabs.get(i - 1);
			return holder.getText();
		}
		return null;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public boolean js_isTabEnabled(int i)
	{
		return js_isTabEnabledAt(i);
	}

	public boolean js_isTabEnabledAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			WebTabHolder holder = allTabs.get(i - 1);
			return holder.isEnabled();
		}
		return false;
	}

	public boolean js_removeTabAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			WebTabHolder holder = allTabs.get(i - 1);
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			boolean ok = holder.getPanel().notifyVisible(false, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
			if (!ok)
			{
				return false;
			}
			allTabs.remove(i - 1);
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
		}
		jsChangeRecorder.setChanged();
		return true;
	}

	public boolean js_removeAllTabs()
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

		if (WebTabPanel.this.get("webform") != null) //$NON-NLS-1$
		{
			// replace it
			WebTabPanel.this.replace(new Label("webform", new Model<String>("")));//temporary add; //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			// else add it
			WebTabPanel.this.add(new Label("webform", new Model<String>("")));//temporary add; //$NON-NLS-1$ //$NON-NLS-2$
		}
		jsChangeRecorder.setChanged();
		return true;
	}

	public void js_setTabBGColorAt(int i, String s)
	{
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public void js_setTabEnabled(int i, boolean b)
	{
		js_setTabEnabledAt(i, b);
	}

	public void js_setTabEnabledAt(int i, boolean b)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			setTabEnabledAt(i - 1, b);
		}
		jsChangeRecorder.setChanged();
	}

	public void setTabEnabledAt(int i, boolean b)
	{
		WebTabHolder holder = allTabs.get(i);
		holder.setEnabled(b);
		jsChangeRecorder.setChanged();
	}

	public void js_setTabFGColorAt(int i, String s)
	{
	}

	public void js_setTabIndex(Object arg)
	{
		int index = Utils.getAsInteger(arg);
		if (index >= 1 && index <= js_getMaxTabIndex())
		{
			WebTabHolder holder = allTabs.get(index - 1);
			setActiveTabPanel(holder.getPanel());
		}
		else
		{
			String tabName = "" + arg; //$NON-NLS-1$
			if (Utils.stringIsEmpty(tabName)) return;
			for (int i = 0; i < allTabs.size(); i++)
			{
				WebTabHolder holder = allTabs.get(i);
				String currentName = holder.getPanel().getName();
				if (Utils.stringSafeEquals(currentName, tabName))
				{
					setActiveTabPanel(holder.getPanel());
					break;
				}
			}
		}
	}

	public void js_setTabTextAt(int i, String s)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			WebTabHolder holder = allTabs.get(i - 1);
			holder.setText(TemplateGenerator.getSafeText(s));
		}
		jsChangeRecorder.setChanged();
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

	public void js_setReadOnly(boolean b)
	{
		for (int i = 0; i < allTabs.size(); i++)
		{
			WebTabHolder holder = allTabs.get(i);
			holder.getPanel().setReadOnly(b);
		}
		jsChangeRecorder.setChanged();
	}

	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}


	/*
	 * name---------------------------------------------------
	 */
	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
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

	public boolean js_isTransparent()
	{
		return !opaque;
	}

	public void js_setTransparent(boolean b)
	{
		opaque = !b;
		jsChangeRecorder.setTransparent(b);
	}

	public boolean isOpaque()
	{
		return opaque;
	}


	/*
	 * tooltip---------------------------------------------------
	 */
	public String js_getToolTipText()
	{
		return tooltip;
	}

	private String tooltip;

	public void setToolTipText(String tooltip)
	{
		if (Utils.stringIsEmpty(tooltip))
		{
			tooltip = null;
		}
		this.tooltip = tooltip;
	}

	public void js_setToolTipText(String tip)
	{
		setToolTipText(tip);
		jsChangeRecorder.setChanged();
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

	public void js_setFont(String spec)
	{
		font = PersistHelper.createFont(spec);
		jsChangeRecorder.setFont(spec);
	}

	public Font getFont()
	{
		return font;
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(background);
	}

	public void js_setBgcolor(String bgcolor)
	{
		background = PersistHelper.createColor(bgcolor);
		jsChangeRecorder.setBgcolor(bgcolor);
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


	/*
	 * fgcolor---------------------------------------------------
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(foreground);
	}

	public void js_setFgcolor(String fgcolor)
	{
		foreground = PersistHelper.createColor(fgcolor);
		jsChangeRecorder.setChanged();
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


	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
		jsChangeRecorder.setBorder(spec);
	}

	/*
	 * visible---------------------------------------------------
	 */
	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
	}

	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean visible)
	{
		setVisible(visible);
		jsChangeRecorder.setVisible(visible);
	}


	/*
	 * enabled---------------------------------------------------
	 */
	public void js_setEnabled(final boolean b)
	{
		setComponentEnabled(b);
	}

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
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
			jsChangeRecorder.setChanged();
		}
	}

	public boolean js_isEnabled()
	{
		return isEnabled();
	}

	private boolean accessible = true;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}


	/*
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

	public int js_getLocationX()
	{
		return getLocation().x;
	}

	public int js_getLocationY()
	{
		return getLocation().y;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getAbsoluteFormLocationY()
	 */
	public int js_getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	public void js_setLocation(int x, int y)
	{
		location = new Point(x, y);
		jsChangeRecorder.setLocation(x, y);
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
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		if (clientProperties == null)
		{
			clientProperties = new HashMap<Object, Object>();
		}
		clientProperties.put(key, value);
	}

	private Map<Object, Object> clientProperties;

	public Object js_getClientProperty(Object key)
	{
		if (clientProperties == null) return null;
		return clientProperties.get(key);
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	public Dimension getSize()
	{
		return size;
	}

	public void js_setSize(int width, int height)
	{
		size = new Dimension(width, height);
		jsChangeRecorder.setSize(width, height, border, new Insets(0, 0, 0, 0), 0);
		if (currentForm != null && currentForm.isReady())
		{
			currentForm.getWebForm().setFormWidth(0);
		}
	}

	public Rectangle getWebBounds()
	{
		Dimension d = jsChangeRecorder.calculateWebSize(size.width, size.height, border, new Insets(0, 0, 0, 0), 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return jsChangeRecorder.getPaddingAndBorder(size.height, border, new Insets(0, 0, 0, 0), 0, null);
	}


	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public int js_getWidth()
	{
		return size.width;
	}

	public int js_getHeight()
	{
		return size.height;
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

	public void setTabIndex(int tabIndex)
	{
		this.tabSequenceIndex = tabIndex;
	}

	/**
	 * @param current
	 * @return
	 */
	public int getTabIndex(WebForm current)
	{
		if (currentForm != null && currentForm.getWebForm() == current)
		{
			Object o = js_getTabIndex();
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

	public void setTabSize(Dimension tabSize)
	{
		this.tabSize = tabSize;
	}

	public Dimension getTabSize()
	{
		return tabSize;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public String js_getSelectedTabFormName()
	{
		if (currentForm != null) return currentForm.getFormName();
		return null;
	}


	private class ServoyTabIcon extends Image implements IResourceListener
	{
		private final WebTabHolder holder;

		public ServoyTabIcon(String id, final WebTabHolder holder)
		{
			super(id);
			this.holder = holder;
			if (holder.getIcon() != null) setImageResource(holder.getIcon());
			else setImageResource(new MediaResource(WebDataImgMediaField.emptyImage, 0));
			add(new StyleAppendingModifier(new Model<String>()
			{
				@Override
				public String getObject()
				{
					String result = ""; //$NON-NLS-1$
					if (holder.getIcon() != null)
					{
						result += "width: " + holder.getIcon().getWidth() + "px; height: " + holder.getIcon().getHeight() + "px";
						if (!js_isEnabled())
						{
							result += "; filter:alpha(opacity=50);-moz-opacity:.50;opacity:.50"; //$NON-NLS-1$
						}
					}
					else
					{
						result += "width: 0px; height: 0px";
					}
					return result;
				}
			}));
			add(new AttributeModifier("src", new Model<String>()
			{
				@Override
				public String getObject()
				{
					String styleAttribute = "";
					if (holder.getIcon() != null)
					{
						CharSequence url = urlFor(IResourceListener.INTERFACE) + "&r=" + Math.random(); //$NON-NLS-1$
						styleAttribute += getResponse().encodeURL(url);
					}
					return styleAttribute;
				}
			}));
		}

		@Override
		public void onResourceRequested()
		{
			if (holder.getIcon() != null)
			{
				holder.getIcon().onResourceRequested();
			}
		}
	}

}
