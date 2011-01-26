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
import com.servoy.j2db.gui.EnableTabPanel;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.smart.SwingForm;
import com.servoy.j2db.ui.IAccessible;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.ui.ITabPanel;
import com.servoy.j2db.util.AutoTransferFocusListener;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.IFocusCycleRoot;
import com.servoy.j2db.util.ISupportFocusTransfer;
import com.servoy.j2db.util.ITabPaneAlike;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * The Servoy tabpanel
 * 
 * @author jblok
 */
public class SpecialTabPanel extends EnablePanel implements IDisplayRelatedData, ChangeListener, IAccessible, ITabPanel, IFocusCycleRoot<Component>,
	ISupportFocusTransfer, ListSelectionListener
{
	private static final long serialVersionUID = 1L;

	private final IApplication application;

	protected IRecordInternal parentData;
	private FormLookupPanel currentForm;//reference to test if not already showing
	private ITabPaneAlike enclosingComponent;
	private final List<String> originalTabText;
	private final List<String> allRelationNames = new ArrayList<String>();
	private final List<ISwingFoundSet> related = new ArrayList<ISwingFoundSet>();

	private boolean validationEnabled = true;

	private IScriptExecuter scriptExecutor;
	private String onTabChangeMethod;
	private Object[] onTabChangeArgs;

	private final List<Component> tabSeqComponentList = new ArrayList<Component>();
	private boolean transferFocusBackwards = false;

	public SpecialTabPanel(IApplication app, int orient, boolean oneTab)
	{
		this(app, orient, oneTab, null);
	}

	protected SpecialTabPanel(IApplication app, int orient, boolean oneTab, ITabPaneAlike enclosingComponent)
	{
		super();
		application = app;
		originalTabText = new ArrayList<String>();
		setLayout(new BorderLayout());

		if (enclosingComponent == null)
		{
			if (orient == TabPanel.HIDE || (orient == TabPanel.DEFAULT && oneTab))
			{
				this.enclosingComponent = new TablessPanel(application);
				setFocusTraversalPolicy(ServoyFocusTraversalPolicy.datarenderPolicy);
			}
			else
			{
				this.enclosingComponent = new TabbedPanel(application);
				ToolTipManager.sharedInstance().registerComponent((EnableTabPanel)this.enclosingComponent);
				if (orient == SwingConstants.TOP || orient == SwingConstants.LEFT || orient == SwingConstants.BOTTOM || orient == SwingConstants.RIGHT)
				{
					this.enclosingComponent.setTabPlacement(orient);
				}
				setFocusTraversalPolicy(ServoyFocusTraversalPolicy.defaultPolicy);
			}
		}
		else this.enclosingComponent = enclosingComponent;

		add((Component)this.enclosingComponent, BorderLayout.CENTER);
		this.enclosingComponent.addChangeListener(this);
		setFocusCycleRoot(true);

		tabSeqComponentList.add((Component)this.enclosingComponent);

		addFocusListener(new AutoTransferFocusListener(this, this));
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
		FormLookupPanel flp = (FormLookupPanel)enclosingComponent.getSelectedComponent();
		if (flp != null)
		{
			showFoundSet(flp, parentState, getDefaultSort());
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
				if (relatedFoundSet != null) registerSelectionListeners(parentState, flp.getRelationName());
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
		FormLookupPanel flp = (FormLookupPanel)enclosingComponent.getSelectedComponent();
		if (flp != null)
		{
			return flp.getRelationName();
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
		FormLookupPanel flp = (FormLookupPanel)enclosingComponent.getSelectedComponent();
		if (flp != null)
		{
			return flp.getDefaultSort();
		}
		else
		{
			return null;
		}
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
				if (validationEnabled && onTabChangeMethod != null && previousIndex != -1)
				{
					scriptExecutor.executeFunction(onTabChangeMethod, Utils.arrayMerge((new Object[] { new Integer(previousIndex + 1) }), onTabChangeArgs),
						true, this, false, "onTabChangeMethodID", false); //$NON-NLS-1$
				}
			}
		}
	}

	public boolean js_removeTabAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex() && removeTabAt(i - 1))
		{
			return true;
		}
		return false;
	}

	public boolean js_removeAllTabs()
	{
		if (js_getMaxTabIndex() == 0) return true;

		boolean retval = false;
		String tmp = onTabChangeMethod;
		try
		{
			onTabChangeMethod = null;
			retval = enclosingComponent.removeAllTabs();
			if (retval)
			{
				originalTabText.clear();
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

	public boolean js_addTab(Object[] vargs)
	{
		if (vargs.length < 1) return false;

		int index = 0;
		Object form = vargs[index++];

		FormController f = null;
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

		if (f != null)
		{
			String name = f.getName();
			if (vargs.length > 6)
			{
				name = (String)vargs[index++];
			}
			String tabText = name;
			if (vargs.length >= 3)
			{
				tabText = (String)vargs[index++];
			}
			String tooltip = ""; //$NON-NLS-1$
			if (vargs.length >= 4)
			{
				tooltip = (String)vargs[index++];
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
				if (!relatedFs.getDataSource().equals(f.getDataSource()))
				{
					return false;
				}
				// TODO do this check to check if the parent table has this relation? How to get the parent table 
//				Table parentTable = null;
//				application.getSolution().getRelations(Solution.SOLUTION+Solution.MODULES, parentTable, true, false);
			}
			FormLookupPanel flp = (FormLookupPanel)createFormLookupPanel(name, relationName, f.getName());
			flp.setReadOnly(readOnly);
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
		return false;
	}

	@Deprecated
	public String js_getTabBGColorAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return PersistHelper.createColorString(getBackgroundAt(i - 1));
		}
		return null;
	}

	public String js_getTabFGColorAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return PersistHelper.createColorString(getForegroundAt(i - 1));
		}
		return null;
	}

	public void js_setTabFGColorAt(int i, String clr)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			setTabForegroundAt(i - 1, PersistHelper.createColor(clr));
		}
	}

	@Deprecated
	public void js_setTabBGColorAt(int i, String clr)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			setTabBackgroundAt(i - 1, PersistHelper.createColor(clr));
		}
	}

	public void js_setTabTextAt(int i, String text)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			originalTabText.set(i - 1, text);
			enclosingComponent.setTitleAt(i - 1, text);
		}
	}

	public String js_getTabTextAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return enclosingComponent.getTitleAt(i - 1);
		}
		return null;
	}

	public String js_getTabNameAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return enclosingComponent.getNameAt(i - 1);
		}
		return null;
	}

	public String js_getTabFormNameAt(int i)
	{
		if (i >= 1 && i <= js_getMaxTabIndex())
		{
			return enclosingComponent.getFormNameAt(i - 1);
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

	/*
	 * bgcolor---------------------------------------------------
	 */
	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(enclosingComponent.getBackground());
	}

	public void js_setBgcolor(String clr)
	{
		enclosingComponent.setBackground(PersistHelper.createColor(clr));
	}

	@Override
	public void setBackground(Color bg)
	{
		super.setBackground(bg);
		if (enclosingComponent != null) enclosingComponent.setBackground(bg);
	}


	/*
	 * fgcolor---------------------------------------------------
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(enclosingComponent.getForeground());
	}

	public void js_setFgcolor(String clr)
	{
		enclosingComponent.setForeground(PersistHelper.createColor(clr));
	}

	@Override
	public void setForeground(Color fg)
	{
		super.setForeground(fg);
		if (enclosingComponent != null) enclosingComponent.setForeground(fg);
	}


	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
	}

	/*
	 * visible---------------------------------------------------
	 */
	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean b)
	{
		setVisible(b);
	}

	public void setComponentVisible(boolean b_visible)
	{
		setVisible(b_visible);
	}


	/*
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !isEnabled();
	}

	public boolean js_isReadOnly()
	{
		return enclosingComponent.isReadOnly();
	}

	public void js_setReadOnly(boolean b)
	{
		enclosingComponent.setReadOnly(b);
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
	 * tooltip---------------------------------------------------
	 */
	public void js_setToolTipText(String txt)
	{
		enclosingComponent.setToolTipText(txt);
	}

	public String js_getToolTipText()
	{
		return enclosingComponent.getToolTipText();
	}


	/*
	 * location---------------------------------------------------
	 */
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

	public void js_setLocation(int x, int y)
	{
		setLocation(x, y);
	}

	/*
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		if (!(enclosingComponent instanceof TablessPanel))
		{
			putClientProperty(key, value);
		}
		enclosingComponent.putClientProperty(key, value);
	}

	public Object js_getClientProperty(Object key)
	{
		return getClientProperty(key);
	}


	/*
	 * size---------------------------------------------------
	 */
	public void js_setSize(int x, int y)
	{
		setSize(x, y);
		revalidate();
		repaint();
	}

	public int js_getWidth()
	{
		return getSize().width;
	}

	public int js_getHeight()
	{
		return getSize().height;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	public boolean js_isTransparent()
	{
		return !isOpaque();
	}

	public void js_setTransparent(boolean b)
	{
		setOpaque(!b);
		repaint();
	}

	@Override
	public void setOpaque(boolean isOpaque)
	{
		super.setOpaque(isOpaque);
		if (enclosingComponent instanceof JComponent) ((JComponent)enclosingComponent).setOpaque(isOpaque);
	}


	/*
	 * jsmethods---------------------------------------------------
	 */


	public void js_setTabIndex(Object arg)
	{
		int index = Utils.getAsInteger(arg);
		if (index >= 1 && index <= js_getMaxTabIndex())
		{
			enclosingComponent.setSelectedIndex(index - 1);
		}
		else
		{
			String tabName = "" + arg; //$NON-NLS-1$
			if (Utils.stringIsEmpty(tabName)) return;
			for (int i = 0; i < enclosingComponent.getTabCount(); i++)
			{
				String currentName = enclosingComponent.getNameAt(i);
				if (Utils.stringSafeEquals(currentName, tabName))
				{
					enclosingComponent.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	/**
	 * Get the selected tab form name
	 *
	 * @sample var formName = %%prefix%%%%elementName%%.getSelectedTabFormName();
	 */
	@Deprecated
	public String js_getSelectedTabFormName()
	{
		return js_getTabFormNameAt(((Integer)js_getTabIndex()).intValue());
	}

	/**
	 * Sets the enabled status for a specific tab
	 *
	 * @sample 
	 *
	 * @param index 
	 *
	 * @param enabled 
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
	}

	/**
	 * Gets the enabled status from a specific tab
	 *
	 * @sample 
	 *
	 * @param index 
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
			return isTabEnabledAt(i - 1);
		}
		return false;
	}

	public String js_getElementType()
	{
		return "TABPANEL"; //$NON-NLS-1$
	}

	/**
	 * Set / Get the selectedTabIndex; it can be set, with number or string(tabName)
	 *
	 * @sample
	 * //Set / Get the selectedTabIndex
	 * var current = tabpanel.tabIndex;
	 * tabpanel.tabIndex = current+1;
	 * //or you can do it using a tab name;
	 * //tabpanel.tabIndex = 'tabName'
	 */
	public Object js_getTabIndex()
	{
		int selectedIndex = enclosingComponent.getSelectedIndex();
		if (selectedIndex >= 0)
		{
			return new Integer(selectedIndex + 1);
		}
		return new Integer(-1);
	}

	public int js_getMaxTabIndex()
	{
		return enclosingComponent.getTabCount();
	}

	public void js_setFont(String spec)
	{
		enclosingComponent.setFont(PersistHelper.createFont(spec));
	}

	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
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

	public void addTab(String text, Icon icon, IFormLookupPanel flp, String tip)
	{
		String tmp = onTabChangeMethod;
		try
		{
			onTabChangeMethod = null;
			originalTabText.add(((text == null || text.indexOf("%%") == -1) ? null : text)); //$NON-NLS-1$
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
		if (parentData != null)
		{
			FormLookupPanel flp = (FormLookupPanel)enclosingComponent.getSelectedComponent();
			if (flp != null)
			{
				flp.getFormPanel();//make sure the flp is ready
				showFoundSet(flp, parentData, flp.getDefaultSort());
			}
		}
	}

}
