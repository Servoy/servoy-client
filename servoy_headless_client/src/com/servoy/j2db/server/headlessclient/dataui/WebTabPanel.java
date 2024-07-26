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
import java.util.List;

import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.wicket.Component;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.FlattenedSolution;
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
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.ui.IFormUI;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.ui.scripting.RuntimeTabPanel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Represents a tabpanel in the webbrowser.
 *
 * @author jcompagner
 */
public class WebTabPanel extends Component implements ITabPanel, IDisplayRelatedData, IProviderStylePropertyChanges, ISupportSecuritySettings,
	ISupportWebBounds, ISupportWebTabSeq, ListSelectionListener, IWebFormContainer, ISupportSimulateBoundsProvider
{
	private static final long serialVersionUID = 1L;

	private final IApplication application;
	private WebTabFormLookup currentForm;
	protected IRecordInternal parentData;
	private final List<String> allRelationNames = new ArrayList<String>(5);
	protected final List<WebTabHolder> allTabs = new ArrayList<WebTabHolder>(5);
	private final List<ISwingFoundSet> related = new ArrayList<ISwingFoundSet>();

	private IScriptExecuter scriptExecutor;

	private String onTabChangeMethodCmd;
	private Object[] onTabChangeArgs;

	protected final int orient;
	private int tabSequenceIndex = ISupportWebTabSeq.DEFAULT;
	private Dimension tabSize;
	private final RuntimeTabPanel scriptable;

	public WebTabPanel(IApplication application, final RuntimeTabPanel scriptable, String name, int orient, boolean oneTab)
	{
		super(name);
		this.application = application;
		this.orient = orient;

		final boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$

		if (orient != TabPanel.SPLIT_HORIZONTAL && orient != TabPanel.SPLIT_VERTICAL) add(new Component("webform"));//temporary add, in case the tab panel does not contain any tabs //$NON-NLS-1$

		this.scriptable = scriptable;
	}

	public final RuntimeTabPanel getScriptObject()
	{
		return scriptable;
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
		return scriptable.getChangesRecorder();
	}

	private void setActiveTabPanel(WebTabFormLookup fl)
	{
		if (fl != currentForm)
		{
			WebTabFormLookup previous = currentForm;

			if (previous != null)
			{
				int stopped = application.getFoundSetManager().getEditRecordList().stopEditing(false);
				boolean cantStop = stopped != ISaveConstants.STOPPED && stopped != ISaveConstants.AUTO_SAVE_BLOCKED;
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
			WebForm webForm = currentForm.getWebForm();
			if (WebTabPanel.this.get(webForm.getId()) != null)
			{
				// replace it
				WebTabPanel.this.replace(webForm);
			}
			else
			{
				// else add it
				WebTabPanel.this.add(webForm);
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
				scriptExecutor.executeFunction(onTabChangeMethodCmd, Utils.arrayMerge((new Object[] { Integer.valueOf(previousIndex + 1) }), onTabChangeArgs),
					true, this, false, StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID.getPropertyName(), false);
			}
		}
	}

	public WebForm getCurrentForm()
	{
		return currentForm != null ? currentForm.getWebForm() : null;
	}

	public IFormUI[] getChildForms()
	{
		WebForm form = getCurrentForm();
		if (form != null && form.getParent() == null)
		{
			form = null;
		}
		return form != null ? new IFormUI[] { form } : null;
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
		super.remove(component);
	}

	public void recomputeTabSequence()
	{
		FormController fc = currentForm.getWebForm().getController();
		fc.recomputeTabSequence(tabSequenceIndex);
	}

	public boolean isCurrentForm(IFormUI formUI)
	{
		return getCurrentForm() == formUI;
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
		if (currentForm != null && isVisible() && (currentForm.getWebForm() == null || currentForm.getWebForm().getParent() != this))
		{
			if (currentForm.getWebForm() == null)
			{
				if (size() == 0)
				{
					// probably current form was destroyed from js code
					WebTabPanel.this.add(new Component("webform"));
				}
			}
			else if (get(currentForm.getWebForm().getId()) != null)
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

	public void setTabLayoutPolicy(int scroll_tab_layout)
	{
		//TODO ignore???
	}

	public IFormLookupPanel createFormLookupPanel(String tabname, String relationName, String formName)
	{
		return new WebTabFormLookup(tabname, relationName, formName, this, application);
	}

	@Override
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
		if (currentForm != null && currentForm.getWebForm() != null)
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
			controller.notifyVisible(visible, invokeLaterRunnables, true);
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
		for (WebTabHolder element : allTabs)
		{
			if (element.refreshTagStrings(resolver))
			{
				getStylePropertyChanges().setChanged();
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
		if (fp != null && flp.getRelationName() != null)
		{
			IFoundSetInternal relatedFoundset = parentState == null ? null : parentState.getRelatedFoundSet(flp.getRelationName(), sort);
			registerSelectionListeners(parentState, flp.getRelationName());
			fp.loadData(relatedFoundset, null);
		}

		ITagResolver resolver = getTagResolver(parentState);
		//refresh tab text
		for (WebTabHolder element : allTabs)
		{
			if (element.getPanel() == flp)
			{
				element.refreshTagStrings(resolver);
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
			IFoundSetInternal fs = currentRecord.getRelatedFoundSet(parts[i]);
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
		insertTab(text, iconData, flp, tip, allTabs.size(), false);
	}

	public void insertTab(String text, byte[] iconData, IFormLookupPanel flp, String tip, int index, boolean loaded)
	{
		allTabs.add(index, new WebTabHolder(text, flp, iconData, tip));
		allRelationNames.add(index, flp.getRelationName());
		getStylePropertyChanges().setChanged();

		if (allTabs.size() == 1 && loaded)
		{
			// it's the new active one! If the tabPanel is not loaded, don't do this because it will break execution order (it will be done when tabPanel gets shown)
			// (renderers are now being created - forms initialisation not complete, and we shouldn't generate any JS callbacks like notifyVisible() and such which can access these forms)
			setActiveTabPanel((WebTabFormLookup)flp);
		}
	}

	public void setTabForegroundAt(int index, Color fg)
	{
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
		FlattenedSolution fl = application.getFlattenedSolution();
		int mediaId = -1;
		if (iconURL != null && !"".equals(iconURL))
		{
			Media media = fl.getMedia(iconURL.replaceAll("media:///", ""));
			if (media != null) mediaId = media.getID();
			if (mediaId == -1)
			{
				Debug.warn("Form '" + formController.getName() + "' with tabpanel  '" + this.name + "' has tabicon  for tab '" + tabname +
					"'in with icon media url : " + iconURL + " not found");
			}
		}

		byte[] iconData = (mediaId == -1 ? null : ComponentFactory.loadIcon(fl, new Integer(mediaId)));

		int count = allTabs.size();
		int tabIndex = idx;
		if (tabIndex == -1 || tabIndex >= count)
		{
			tabIndex = count;
		}

		insertTab(application.getI18NMessageIfPrefixed(tabText), iconData, flp, application.getI18NMessageIfPrefixed(tabtooltip), tabIndex, true);

		if (fg != null) setTabForegroundAt(tabIndex, PersistHelper.createColor(fg));
		if (bg != null) setTabBackgroundAt(tabIndex, PersistHelper.createColor(bg));

		// TODO is this if really needed? (insertTab might activate the new tab, but loadData based on relationName only; if it
		// doesn't activate... will ever currentForm == flp?)
		// if the relatedFs is based on a different record then parentState, it would be wrong to use it... maybe we should only use the relationName
		// from the relatedFs - which is already in the relationName param
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

	public int getMnemonicAt(int i)
	{
		WebTabHolder holder = allTabs.get(i);
		return holder.getDisplayedMnemonic();
	}

	public void setMnemonicAt(int i, int m)
	{
		WebTabHolder holder = allTabs.get(i);
		holder.setDisplayedMnemonic(m);
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
				replace(new Component("webform"));
			}
		}
		return true;
	}

	public boolean removeAllTabs()
	{
		for (WebTabHolder comp : allTabs)
		{
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
			WebTabPanel.this.replace(new Component("webform"));//temporary add; //$NON-NLS-1$
		}
		else
		{
			// else add it
			WebTabPanel.this.add(new Component("webform"));//temporary add; //$NON-NLS-1$
		}
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
		for (WebTabHolder holder : allTabs)
		{
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
		holder.setText(s);
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
		for (WebTabHolder holder : allTabs)
		{
			holder.getPanel().setReadOnly(b);
		}
	}

	@Override
	public void setName(String n)
	{
		name = n;
	}

	private String name;

	@Override
	public String getName()
	{
		return name;
	}


	/*
	 * border---------------------------------------------------
	 */
	private Border border;

	@Override
	public void setBorder(Border border)
	{
		this.border = border;
	}

	@Override
	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	@Override
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	private boolean opaque;

	@Override
	public boolean isOpaque()
	{
		return opaque;
	}


	private String tooltip;

	@Override
	public void setToolTipText(String tooltip)
	{
		this.tooltip = Utils.stringIsEmpty(tooltip) ? null : tooltip;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		return tooltip;
	}


	/*
	 * font---------------------------------------------------
	 */
	@Override
	public void setFont(Font font)
	{
		this.font = font;
	}

	private Font font;

	@Override
	public Font getFont()
	{
		return font;
	}


	private Color background;

	@Override
	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	@Override
	public Color getBackground()
	{
		return background;
	}


	private Color foreground;

	@Override
	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	@Override
	public Color getForeground()
	{
		return foreground;
	}


	/*
	 * visible---------------------------------------------------
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		if (viewable)
		{
			setVisible(visible);
		}
	}

	@Override
	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
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

	@Override
	public void setLocation(Point location)
	{
		this.location = location;
	}

	@Override
	public Point getLocation()
	{
		return location;
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	@Override
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


	@Override
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

	public int getTabSequenceIndex()
	{
		return tabSequenceIndex;
	}

	/**
	 * @param current
	 * @return
	 */
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

	public void setTabSize(Dimension tabSize)
	{
		this.tabSize = tabSize;
	}

	public Dimension getTabSize()
	{
		return tabSize;
	}

	public void setHorizontalAlignment(int alignment)
	{

	}

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
	}


	@Override
	public void uiRecreated()
	{
		recomputeTabSequence();
	}
}
