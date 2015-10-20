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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.DefaultFocusTraversalPolicy;
import java.awt.Font;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.servoy.base.util.ITagResolver;
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
import com.servoy.j2db.gui.EnableTabPanel;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.smart.SwingForm;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.ui.scripting.AbstractRuntimeTabPaneAlike;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.IFocusCycleRoot;
import com.servoy.j2db.util.ISupportFocusTransfer;
import com.servoy.j2db.util.ITabPaneAlike;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.AutoTransferFocusListener;

/**
 * The Servoy tabpanel
 *
 * @author jblok
 */
public class SpecialTabPanel extends EnablePanel implements IDisplayRelatedData, ChangeListener, ISupportSecuritySettings, ITabPanel,
	IFocusCycleRoot<Component>, ISupportFocusTransfer, ListSelectionListener
{
	private static final long serialVersionUID = 1L;

	private final IApplication application;

	protected IRecordInternal parentData;
	private FormLookupPanel currentForm;//reference to test if not already showing
	private ITabPaneAlike enclosingComponent;
	private final List<String> originalTabText;
	private final List<String> originalTabTooltip;
	private final List<String> allRelationNames = new ArrayList<String>();
	private final List<ISwingFoundSet> related = new ArrayList<ISwingFoundSet>();

	private boolean validationEnabled = true;

	private IScriptExecuter scriptExecutor;
	private String onTabChangeMethod;
	private Object[] onTabChangeArgs;

	private final List<Component> tabSeqComponentList = new ArrayList<Component>();
	private boolean transferFocusBackwards = false;
	private final AbstractRuntimeTabPaneAlike scriptable;

	public SpecialTabPanel(IApplication app, AbstractRuntimeTabPaneAlike scriptable, int orient, boolean oneTab)
	{
		this(app, scriptable, orient, oneTab, null);
	}

	protected SpecialTabPanel(IApplication app, AbstractRuntimeTabPaneAlike scriptable, int orient, boolean oneTab, ITabPaneAlike enclosingComponent)
	{
		super();
		application = app;
		originalTabText = new ArrayList<String>();
		originalTabTooltip = new ArrayList<String>();
		setLayout(new BorderLayout());

		if (enclosingComponent == null)
		{
			if (orient == TabPanel.HIDE || (orient == TabPanel.DEFAULT_ORIENTATION && oneTab))
			{
				this.enclosingComponent = new TablessPanel(application);
				setFocusTraversalPolicy(ServoyFocusTraversalPolicy.datarenderPolicy);
			}
			else if (orient == TabPanel.ACCORDION_PANEL)
			{
				this.enclosingComponent = new AccordionPanel(application);
				ToolTipManager.sharedInstance().registerComponent((JComponent)this.enclosingComponent);
				setFocusTraversalPolicy(ServoyFocusTraversalPolicy.defaultPolicy);
			}
			else
			{
				this.enclosingComponent = new TabbedPanel(application);
				ToolTipManager.sharedInstance().registerComponent((EnableTabPanel)this.enclosingComponent);
				if (orient == SwingConstants.TOP || orient == SwingConstants.LEFT || orient == SwingConstants.BOTTOM || orient == SwingConstants.RIGHT)
				{
					this.enclosingComponent.setTabPlacement(orient);
				}
				setFocusTraversalPolicy(new DefaultFocusTraversalPolicy()
				{
					@Override
					public Component getComponentBefore(Container aContainer, Component aComponent)
					{
						if (!(aComponent instanceof TabbedPanel))
						{
							return super.getComponentBefore(aContainer, aComponent);
						}
						else
						{
							// go out of this tab panel
							Container focusRoot = aContainer.getFocusCycleRootAncestor();
							return ServoyFocusTraversalPolicy.datarenderPolicy.getComponentBefore(focusRoot, aContainer);
						}
					}
				});
			}
		}
		else this.enclosingComponent = enclosingComponent;

		add((Component)this.enclosingComponent, BorderLayout.CENTER);
		this.enclosingComponent.addChangeListener(this);
		setFocusCycleRoot(true);

		tabSeqComponentList.add((Component)this.enclosingComponent);

		addFocusListener(new AutoTransferFocusListener(this, this));

		this.scriptable = scriptable;
		scriptable.setEnclosingComponent((JComponent)this.enclosingComponent);
	}

	public final AbstractRuntimeTabPaneAlike getScriptObject()
	{
		return scriptable;
	}

	/**
	 * @return the enclosingComponent
	 */
	public ITabPaneAlike getEnclosingComponent()
	{
		return enclosingComponent;
	}

	public boolean isTransferFocusBackwards()
	{
		return transferFocusBackwards;
	}

	public void setTransferFocusBackwards(boolean transferBackwards)
	{
		this.transferFocusBackwards = transferBackwards;
	}

	/**
	 * @see com.servoy.j2db.ui.ITabPanel#addScriptExecuter(com.servoy.j2db.IScriptExecuter)
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		this.scriptExecutor = el;
	}

	public void destroy()
	{
		deregisterSelectionListeners();

		// should deregister related foundsets??
		if (enclosingComponent instanceof EnableTabPanel)
		{
			ToolTipManager.sharedInstance().unregisterComponent((EnableTabPanel)enclosingComponent);
		}
	}

	public void setValidationEnabled(boolean validationEnabled)
	{
		this.validationEnabled = validationEnabled;
	}

	public void notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		currentForm = (FormLookupPanel)enclosingComponent.getSelectedComponent();
		if (currentForm != null)
		{
			if (visible)//this is not needed when closing
			{
				FormController fp = currentForm.getFormPanel();//makes sure is visible
				if (fp != null)
				{
					if (parentData != null)
					{
						showFoundSet(currentForm, parentData, currentForm.getDefaultSort());
					}
				}
			}
			currentForm.notifyVisible(visible, invokeLaterRunnables);
		}
	}

	public void setRecord(IRecordInternal parentState, boolean stopEditing)
	{
		parentData = parentState;
		if (currentForm != null) // enclosingComponent may already point to the next (uninitialised) form, see stateChanged()
		{
			showFoundSet(currentForm, parentState, getDefaultSort());
		}
		ITagResolver resolver = getTagResolver(parentState);
		for (int i = 0; i < originalTabText.size(); i++)
		{
			String element = originalTabText.get(i);
			if (element != null)
			{
				enclosingComponent.setTitleAt(i, Text.processTags(element, resolver));
			}
		}
		for (int i = 0; i < originalTabTooltip.size(); i++)
		{
			String tooltip = originalTabTooltip.get(i);
			if (tooltip != null)
			{
				enclosingComponent.setToolTipTextAt(i, Text.processTags(tooltip, resolver));
			}
		}
	}

	protected void showFoundSet(FormLookupPanel flp, IRecordInternal parentState, List<SortColumn> sort)
	{
		deregisterSelectionListeners();

		if (!flp.isReady()) return;

		try
		{

			FormController fp = flp.getFormPanel();
			if (fp != null && flp.getRelationName() != null)
			{
				IFoundSetInternal relatedFoundSet = parentState == null ? null : parentState.getRelatedFoundSet(flp.getRelationName(), sort);
				registerSelectionListeners(parentState, flp.getRelationName());
				fp.loadData(relatedFoundSet, null);
			}

			ITagResolver resolver = getTagResolver(parentState);
			//refresh tab text
			int i = enclosingComponent.getTabIndex(flp);
			String element = i < originalTabText.size() ? originalTabText.get(i) : null;
			if (element != null)
			{
				enclosingComponent.setTitleAt(i, Text.processTags(element, resolver));
			}
			String tooltip = i < originalTabTooltip.size() ? originalTabTooltip.get(i) : null;
			if (tooltip != null)
			{
				enclosingComponent.setToolTipTextAt(i, Text.processTags(tooltip, resolver));
			}
		}
		catch (RuntimeException re)
		{
			application.handleException("Error setting the foundset of the relation " + flp.getRelationName() + " on the tab with form " + flp.getFormName(),
				re);
			throw re;
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

	/**
	 * @param parentState
	 * @return
	 */
	private ITagResolver getTagResolver(IRecordInternal parentState)
	{
		ITagResolver resolver;
		Container parent = getParent();
		while (!(parent instanceof SwingForm) && parent != null)
		{
			parent = parent.getParent();
		}
		if (parent instanceof SwingForm)
		{
			resolver = ((SwingForm)parent).getController().getTagResolver();
		}
		else
		{
			resolver = TagResolver.createResolver(parentState);
		}
		return resolver;
	}

	public String getSelectedRelationName()
	{
		if (currentForm != null) // enclosingComponent may already point to the next (uninitialised) form, see stateChanged()
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
		if (currentForm != null) // enclosingComponent may already point to the next (uninitialised) form, see stateChanged()
		{
			return currentForm.getDefaultSort();
		}
		return null;
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (currentForm != null)
		{
			return currentForm.stopUIEditing(looseFocus);
		}
		return true;
	}


	public void stateChanged(ChangeEvent e)
	{
		FormLookupPanel flp = (FormLookupPanel)enclosingComponent.getSelectedComponent();
		if (currentForm != flp)
		{
			//hold a reference so that when calling saveData the lastSelected is not replaced by the current selected.
			FormLookupPanel previous = currentForm;

			// stopEditing may trigger calls to this panel, note that currentForm still points to the previous tab
			// and the new one (enclosingComponent.getSelectedComponent()) may not be initialized yet,
			// so refer to currentForm and not to enclosingComponent.getSelectedComponent() in callbacks.
			int stopped = application.getFoundSetManager().getEditRecordList().stopEditing(false);
			boolean cantStop = stopped != ISaveConstants.STOPPED && stopped != ISaveConstants.AUTO_SAVE_BLOCKED;
			if (previous != null)
			{
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
				boolean ok = previous.notifyVisible(false, invokeLaterRunnables);
				Utils.invokeLater(application, invokeLaterRunnables);
				if ((cantStop || !ok))
				{
					currentForm = previous;
					enclosingComponent.setSelectedComponent(previous);
					return;
				}
			}
			int previousIndex = enclosingComponent.getTabIndex(previous);

			currentForm = flp;

			if (flp != null)
			{
				if (parentData != null)
				{
					flp.getFormPanel();//make sure the flp is ready
					showFoundSet(flp, parentData, flp.getDefaultSort());
				}
				List<Runnable> invokeLaterRunnables2 = new ArrayList<Runnable>();
				flp.notifyVisible(true, invokeLaterRunnables2);
				Utils.invokeLater(application, invokeLaterRunnables2);
				if (onTabChangeMethod != null && previousIndex != -1)
				{
					scriptExecutor.executeFunction(onTabChangeMethod, Utils.arrayMerge((new Object[] { Integer.valueOf(previousIndex + 1) }), onTabChangeArgs),
						true, this, false, StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID.getPropertyName(), false);
				}
			}
		}
	}

	public boolean removeAllTabs()
	{
		if (getMaxTabIndex() == -1) return true;

		boolean retval = false;
		String tmp = onTabChangeMethod;
		try
		{
			onTabChangeMethod = null;
			retval = enclosingComponent.removeAllTabs();
			if (retval)
			{
				originalTabText.clear();
				originalTabTooltip.clear();
				allRelationNames.clear();

				//safety
				currentForm = null;
			}
		}
		finally
		{
			onTabChangeMethod = tmp;
		}
		return retval;
	}

	public boolean addTab(IForm formController, String formName, String tabname, String tabText, String tooltip, String iconURL, String fg, String bg,
		String relationName, RelatedFoundSet relatedFs, int idx)
	{
		//to make sure we don't have recursion on adding a tab, to a tabpanel, that is based
		//on the form that the tabpanel is placed on
		if (formController != null)
		{
			Container parent = getParent();
			while (!(parent instanceof SwingForm) && parent != null)
			{
				parent = parent.getParent();
			}
			if (parent != null)
			{
				FormController parentFormController = ((SwingForm)parent).getController();
				if (parentFormController != null && parentFormController.equals(formController))
				{
					return false;
				}
			}
		}

		FormLookupPanel flp = (FormLookupPanel)createFormLookupPanel(tabname, relationName, formName);
		if (formController != null) flp.setReadOnly(formController.isReadOnly());
		Icon icon = null;
		if (iconURL != null && !"".equals(iconURL)) //$NON-NLS-1$
		{
			try
			{
				URL url = new URL(iconURL);
				icon = new ImageIcon(url);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		int tabIndex = idx;
		if (tabIndex == -1 || tabIndex >= enclosingComponent.getTabCount())
		{
			tabIndex = enclosingComponent.getTabCount();
			addTab(application.getI18NMessageIfPrefixed(tabText), icon, flp, application.getI18NMessageIfPrefixed(tooltip));
		}
		else
		{
			insertTab(application.getI18NMessageIfPrefixed(tabText), icon, flp, application.getI18NMessageIfPrefixed(tooltip), tabIndex);
		}
		if (fg != null) setTabForegroundAt(tabIndex, PersistHelper.createColor(fg));
		if (bg != null) setTabBackgroundAt(tabIndex, PersistHelper.createColor(bg));

		if (relatedFs != null && enclosingComponent.getSelectedComponent() == flp)
		{
			FormController fp = flp.getFormPanel();
			if (fp != null && flp.getRelationName() != null && flp.getRelationName().equals(relationName))
			{
				fp.loadData(relatedFs, null);
			}
		}
		return true;
	}

	public void setTabTextAt(int i, String text)
	{
		originalTabText.set(i, text);
		enclosingComponent.setTitleAt(i, text);
	}

	public String getTabTextAt(int i)
	{
		return enclosingComponent.getTitleAt(i);
	}

	public void setMnemonicAt(int i, int m)
	{
		enclosingComponent.setMnemonicAt(i, Character.toUpperCase(m));
	}

	public int getMnemonicAt(int i)
	{
		return enclosingComponent.getMnemonicAt(i);
	}

	public String getTabNameAt(int i)
	{
		return enclosingComponent.getNameAt(i);
	}

	public String getTabFormNameAt(int i)
	{
		return enclosingComponent.getFormNameAt(i);
	}

	@Override
	public void setBackground(Color bg)
	{
		super.setBackground(bg);
		if (enclosingComponent != null) enclosingComponent.setBackground(bg);
	}


	@Override
	public void setForeground(Color fg)
	{
		super.setForeground(fg);
		if (enclosingComponent != null) enclosingComponent.setForeground(fg);
	}


	public void setComponentVisible(boolean b_visible)
	{
		if (viewable)
		{
			setVisible(b_visible);
		}
	}


	/*
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !isEnabled();
	}

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
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

	public int getAbsoluteFormLocationY()
	{
		Container parent = getParent();
		while ((parent != null) && !(parent instanceof IDataRenderer))
		{
			parent = parent.getParent();
		}
		if (parent != null)
		{
			return ((IDataRenderer)parent).getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	@Override
	public void setOpaque(boolean isOpaque)
	{
		// always transparent to support semi transparent colors
		super.setOpaque(false);
		if (enclosingComponent instanceof JComponent) ((JComponent)enclosingComponent).setOpaque(isOpaque);
	}

	public void setTabIndex(int index)
	{
		enclosingComponent.setSelectedIndex(index);
	}

	public void setTabIndex(String name)
	{
		for (int i = 0; i < enclosingComponent.getTabCount(); i++)
		{
			if (Utils.stringSafeEquals(enclosingComponent.getNameAt(i), name))
			{
				enclosingComponent.setSelectedIndex(i);
				break;
			}
		}
	}

	public int getTabIndex()
	{
		return enclosingComponent.getSelectedIndex();
	}

	public int getMaxTabIndex()
	{
		return enclosingComponent.getTabCount() - 1;
	}

	@Override
	public void setFont(Font font)
	{
		super.setFont(font);
		if (enclosingComponent != null) enclosingComponent.setFont(font);
	}

	public void setTabForegroundAt(int index, Color fg)
	{
		enclosingComponent.setForegroundAt(index, fg);
	}

	public void setTabBackgroundAt(int index, Color bg)
	{
		enclosingComponent.setBackgroundAt(index, bg);
	}

	public void setTabEnabledAt(int index, boolean enabled)
	{
		enclosingComponent.setEnabledAt(index, enabled);
	}

	public boolean isTabEnabledAt(int index)
	{
		return enclosingComponent.isEnabledAt(index);
	}

	public Color getForegroundAt(int index)
	{
		return enclosingComponent.getForegroundAt(index);
	}

	public Color getBackgroundAt(int index)
	{
		return enclosingComponent.getBackgroundAt(index);
	}

	public void addTab(String text, int iconMediaId, IFormLookupPanel flp, String tip)
	{
		Icon icon = null;
		if (iconMediaId > 0)
		{
			try
			{
				icon = ImageLoader.getIcon(ComponentFactory.loadIcon(application.getFlattenedSolution(), new Integer(iconMediaId)), -1, -1, true);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		addTab(text, icon, flp, tip);
	}

	public void addTab(String text, Icon icon, IFormLookupPanel flp, String tip)
	{
		String tmp = onTabChangeMethod;
		try
		{
			onTabChangeMethod = null;
			originalTabText.add(((text == null || text.indexOf("%%") == -1) ? null : text)); //$NON-NLS-1$
			originalTabTooltip.add(((tip == null || tip.indexOf("%%") == -1) ? null : tip)); //$NON-NLS-1$
			allRelationNames.add(flp.getRelationName());
			enclosingComponent.addTab(flp.getName(), text, icon, (Component)flp, tip);
		}
		finally
		{
			onTabChangeMethod = tmp;
		}
	}

	public void insertTab(String text, Icon icon, IFormLookupPanel flp, String tip, int index)
	{
		String tmp = onTabChangeMethod;
		onTabChangeMethod = null;
		try
		{
			originalTabText.add(index, ((text == null || text.indexOf("%%") == -1) ? null : text)); //$NON-NLS-1$
			originalTabTooltip.add(((tip == null || tip.indexOf("%%") == -1) ? null : tip)); //$NON-NLS-1$
			allRelationNames.add(index, flp.getRelationName());
			enclosingComponent.insertTab(flp.getName(), text, icon, (Component)flp, tip, index);
		}
		finally
		{
			onTabChangeMethod = tmp;
		}
	}

	public boolean removeTabAt(int index)
	{
		boolean retval = false;
		String tmp = onTabChangeMethod;
		try
		{
			onTabChangeMethod = null;
			retval = enclosingComponent.removeTabAtPos(index);
			if (retval)
			{
				if (index < originalTabText.size()) originalTabText.remove(index);
				if (index < originalTabTooltip.size()) originalTabTooltip.remove(index);
				allRelationNames.remove(index);

				// safety
				if (allRelationNames.size() == 0)
				{
					currentForm = null;
				}
			}
		}
		finally
		{
			onTabChangeMethod = tmp;
		}

		return retval;
	}

	public void setTabLayoutPolicy(int scroll_tab_layout)
	{
		enclosingComponent.setTabLayoutPolicy(scroll_tab_layout);
	}

	/**
	 * @see com.servoy.j2db.ui.ITabPanel#setOnTabChangeMethodCmd(int, TabPanel)
	 */
	public void setOnTabChangeMethodCmd(String onTabChangeMethod, Object[] onTabChangeArgs)
	{
		this.onTabChangeMethod = onTabChangeMethod;
		this.onTabChangeArgs = onTabChangeArgs;
	}

	public IFormLookupPanel createFormLookupPanel(String tabname, String relationName, String formName)
	{
		return new FormLookupPanel(application, tabname, relationName, formName);
	}

	@Override
	public String toString()
	{
		return "SpecialTabPanel, name='" + getName() + "', hash " + hashCode(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}

	public Component getFirstFocusableField()
	{
		return (Component)enclosingComponent;
	}

	public List<Component> getTabSeqComponents()
	{
		return tabSeqComponentList;
	}

	public void setTabSeqComponents(List<Component> tabSequence)
	{
		// ignore
	}

	public boolean isTraversalPolicyEnabled()
	{
		return true;
	}

	public Component getLastFocusableField()
	{
		return (Component)enclosingComponent;
	}


	public void valueChanged(ListSelectionEvent e)
	{
		if (parentData != null && currentForm != null) // enclosingComponent may already point to the next (uninitialised) form, see stateChanged()
		{
			currentForm.getFormPanel();//make sure the flp is ready
			showFoundSet(currentForm, parentData, currentForm.getDefaultSort());
		}
	}

	public void setHorizontalAlignment(int alignment)
	{
		if (enclosingComponent instanceof AccordionPanel)
		{
			((AccordionPanel)enclosingComponent).setAllTabsAlignment(alignment);
		}

	}

}
