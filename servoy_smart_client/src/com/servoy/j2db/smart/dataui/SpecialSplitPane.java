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
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.ui.ISplitPane;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.ui.scripting.RuntimeSplitPane;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.IFocusCycleRoot;
import com.servoy.j2db.util.ISupportFocusTransfer;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.AutoTransferFocusListener;


public class SpecialSplitPane extends EnablePanel implements ISplitPane, IDisplayRelatedData, ISupportSecuritySettings, IFocusCycleRoot<Component>,
	ISupportFocusTransfer, ListSelectionListener
{
	private final IApplication application;
	private final SplitPane splitPane;
	private final List<String> allRelationNames = new ArrayList<String>(2);
	private final List<ISwingFoundSet> related = new ArrayList<ISwingFoundSet>();
	private IRecordInternal parentData;
	private boolean accessible = true;
	private final List<Component> tabSeqComponentList = new ArrayList<Component>();
	private boolean transferFocusBackwards = false;

	private String onDividerChangeMethodCmd;
	private IScriptExecuter scriptExecutor;
	private final RuntimeSplitPane scriptable;

	public SpecialSplitPane(IApplication app, RuntimeSplitPane scriptable, int orient, boolean design)
	{
		super();
		application = app;
		setLayout(new BorderLayout());
		splitPane = new SplitPane(orient, design);
		splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (onDividerChangeMethodCmd != null && scriptExecutor != null)
				{
					scriptExecutor.executeFunction(onDividerChangeMethodCmd, new Object[] { new Integer(-1) }, false, SpecialSplitPane.this, false,
						StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID.getPropertyName(), true);
				}
			}
		});
		add(splitPane, BorderLayout.CENTER);

		setFocusTraversalPolicy(ServoyFocusTraversalPolicy.defaultPolicy);
		tabSeqComponentList.add(splitPane);
		addFocusListener(new AutoTransferFocusListener(this, this));
		this.scriptable = scriptable;
		if (scriptable != null /* design mode */) scriptable.setEnclosingComponent(splitPane);
	}

	public final RuntimeSplitPane getScriptObject()
	{
		return scriptable;
	}

	/**
	 * @return the splitPane
	 */
	public SplitPane getSplitPane()
	{
		return splitPane;
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}

	public void setComponentEnabled(boolean enabled)
	{
		if (accessible)
		{
			super.setEnabled(enabled);
		}
	}

	public void setComponentVisible(boolean visible)
	{
		if (viewable)
		{
			setVisible(visible);
		}
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
		return null;
	}

	public String getSelectedRelationName()
	{
		return null;
	}

	public void notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		Component[] components = splitPane.getSplitComponents();
		for (Component c : components)
		{
			if (c instanceof FormLookupPanel)
			{
				notifyVisibleForm(visible, (FormLookupPanel)c, invokeLaterRunnables);
			}
		}
	}

	private boolean notifyVisibleForm(boolean visible, FormLookupPanel flp, List<Runnable> invokeLaterRunnables)
	{
		if (visible)//this is not needed when closing
		{
			FormController fp = flp.getFormPanel();//makes sure is visible
			if (fp != null)
			{
				if (parentData != null)
				{
					showFoundSet(flp, parentData, flp.getDefaultSort());
				}
			}
		}

		return flp.notifyVisible(visible, invokeLaterRunnables);
	}

	public void setRecord(IRecordInternal parentState, boolean stopEditing)
	{
		parentData = parentState;
		Component[] components = splitPane.getSplitComponents();
		FormLookupPanel flp;
		for (Component c : components)
		{
			if (c instanceof FormLookupPanel)
			{
				flp = (FormLookupPanel)c;
				showFoundSet(flp, parentState, flp.getDefaultSort());
			}
		}
	}

	public boolean isReadOnly()
	{
		return !isEnabled();
	}

	public void setValidationEnabled(boolean validationEnabled)
	{
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		Component[] components = splitPane.getSplitComponents();
		FormLookupPanel flp;
		boolean stopUIEditing = true;
		for (Component c : components)
		{
			if (c instanceof FormLookupPanel)
			{
				flp = (FormLookupPanel)c;
				if (flp.isReady())
				{
					stopUIEditing = stopUIEditing && flp.stopUIEditing(looseFocus);
				}
			}
		}

		return stopUIEditing;
	}

	public void destroy()
	{
		deregisterSelectionListeners();
	}

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

	public void valueChanged(ListSelectionEvent e)
	{
		if (parentData != null)
		{
			Component[] components = splitPane.getSplitComponents();
			FormLookupPanel flp;
			for (Component c : components)
			{
				if (c instanceof FormLookupPanel)
				{
					flp = (FormLookupPanel)c;
					flp.getFormPanel();//make sure the flp is ready
					showFoundSet(flp, parentData, flp.getDefaultSort());
				}
			}
		}
	}

	@Override
	public void setOpaque(boolean isOpaque)
	{
		if (splitPane != null) splitPane.setOpaque(isOpaque);
		super.setOpaque(isOpaque);
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
	public void setBackground(Color bgcolor)
	{
		if (splitPane != null && bgcolor != null && !bgcolor.equals(UIManager.getColor("Panel.background"))) //$NON-NLS-1$
		{
			splitPane.setBackground(bgcolor);
		}
	}

	@Override
	public Color getBackground()
	{
		if (splitPane != null) return splitPane.getBackground();
		else return super.getBackground();
	}

	@Override
	public void setBorder(Border b)
	{
		if (splitPane != null) splitPane.setBorder(b);
	}

	@Override
	public Border getBorder()
	{
		if (splitPane != null) return splitPane.getBorder();
		else return super.getBorder();
	}

	private void showFoundSet(FormLookupPanel flp, IRecordInternal parentState, List<SortColumn> sort)
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

	public void setLeftForm(IFormLookupPanel flp)
	{
		allRelationNames.add(0, flp == null ? null : flp.getRelationName());
		splitPane.setLeftForm((FormLookupPanel)flp);
	}

	public IFormLookupPanel getLeftForm()
	{
		Component leftComponent = splitPane.getLeftComponent();
		return (leftComponent instanceof IFormLookupPanel) ? (IFormLookupPanel)leftComponent : null;
	}

	public void setRightForm(IFormLookupPanel flp)
	{
		allRelationNames.add(allRelationNames.size() > 0 ? 1 : 0, flp == null ? null : flp.getRelationName());
		splitPane.setRightForm((FormLookupPanel)flp);
	}

	public IFormLookupPanel getRightForm()
	{
		Component rightComponent = splitPane.getRightComponent();
		return (rightComponent instanceof IFormLookupPanel) ? (IFormLookupPanel)rightComponent : null;
	}

	public FormLookupPanel createFormLookupPanel(String tabname, String relationName, String formName)
	{
		return new FormLookupPanel(application, tabname, relationName, formName);
	}

	public FormScope getForm(boolean bLeftForm)
	{
		Component component = bLeftForm ? splitPane.getLeftComponent() : splitPane.getRightComponent();
		if (component instanceof FormLookupPanel) return ((FormLookupPanel)component).getFormPanel().getFormScope();
		return null;
	}

	public void setDividerLocation(final double location)
	{
		setRuntimeDividerLocation(location);
	}

	public void setRuntimeDividerLocation(final double location)
	{
		if (location < 1)
		{
			splitPane.setDividerLocation(location);
			if (!isValid())
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						// if not valid, sizes may be incorrect; hopefully now we have correct values
						// first call is for getter to work immediately
						splitPane.setDividerLocation(location);
					}
				});
			}
		}
		else splitPane.setDividerLocation((int)location);
	}

	public double getDividerLocation()
	{
		return splitPane.getDividerLocation();
	}

	public void setDividerSize(int size)
	{
		splitPane.setDividerSize(size < 1 ? -1 : size);
	}

	public int getDividerSize()
	{
		return splitPane.getDividerSize();
	}

	public double getResizeWeight()
	{
		return splitPane.getResizeWeight();
	}

	public void setResizeWeight(double resizeWeight)
	{
		splitPane.setResizeWeight(resizeWeight);
	}

	public boolean getContinuousLayout()
	{
		return splitPane.isContinuousLayout();
	}

	public void setContinuousLayout(boolean b)
	{
		splitPane.setContinuousLayout(b);
	}


	public int getFormMinSize(boolean bLeftForm)
	{
		return bLeftForm ? splitPane.getLeftFormMinSize() : splitPane.getRightFormMinSize();
	}

	public void setFormMinSize(boolean bLeftForm, int minSize)
	{
		if (bLeftForm)
		{
			splitPane.setLeftFormMinSize(minSize);
		}
		else
		{
			splitPane.setRightFormMinSize(minSize);
		}
	}

	public boolean setForm(boolean bLeftForm, Object form, Object relation)
	{
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
			f = (FormController)((FormController.JSForm)form).getFormPanel();
			readOnly = f.isReadOnly();
		}

		if (f != null) fName = f.getName();
		if (form instanceof String) fName = (String)form;
		if (fName != null)
		{
			String name = fName;
			RelatedFoundSet relatedFs = null;
			String relationName = null;

			if (relation instanceof RelatedFoundSet)
			{
				relatedFs = (RelatedFoundSet)relation;
			}
			else if (relation instanceof String)
			{
				relationName = (String)relation;
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

			IFormLookupPanel replacedForm = bLeftForm ? getLeftForm() : getRightForm();
			if (replacedForm != null)
			{
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>(0);
				boolean bNotifyVisibleForm = notifyVisibleForm(false, (FormLookupPanel)replacedForm, invokeLaterRunnables);
				Utils.invokeLater(application, invokeLaterRunnables);
				if (!bNotifyVisibleForm) return false;
			}

			FormLookupPanel flp = createFormLookupPanel(name, relationName, fName);
			if (f != null) flp.setReadOnly(readOnly);

			if (bLeftForm) setLeftForm(flp);
			else setRightForm(flp);
			if (relatedFs != null)
			{
				FormController fp = flp.getFormPanel();
				if (fp != null && flp.getRelationName() != null && flp.getRelationName().equals(relationName))
				{
					fp.loadData(relatedFs, null);
				}
			}

			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>(0);
			boolean bNotifyVisibleForm = notifyVisibleForm(true, flp, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
			return bNotifyVisibleForm;
		}
		return false;
	}

	public Component getFirstFocusableField()
	{
		return splitPane;
	}

	public Component getLastFocusableField()
	{
		return splitPane;
	}

	public List<Component> getTabSeqComponents()
	{
		return tabSeqComponentList;
	}

	public boolean isTraversalPolicyEnabled()
	{
		return true;
	}

	public void setTabSeqComponents(List<Component> tabSequence)
	{
		// ignore
	}

	public boolean isTransferFocusBackwards()
	{
		return transferFocusBackwards;
	}

	public void setTransferFocusBackwards(boolean transferBackwards)
	{
		this.transferFocusBackwards = transferBackwards;
	}

	@Override
	protected void paintBorder(Graphics g)
	{
		Border border = splitPane.getBorder();
		if (border != null)
		{
			border.paintBorder(splitPane, g, 0, 0, getWidth(), getHeight());
		}
	}

	@Override
	public String toString()
	{
		return "SpecialSplitPane, name='" + getName() + "', hash " + hashCode(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISplitPane#setOnDividerChangeMethodCmd(java.lang.String)
	 */
	public void setOnDividerChangeMethodCmd(String onDividerChangeMethodCmd)
	{
		this.onDividerChangeMethodCmd = onDividerChangeMethodCmd;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISplitPane#addScriptExecuter(com.servoy.j2db.IScriptExecuter)
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		this.scriptExecutor = el;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#setTabLayoutPolicy(int)
	 */
	public void setTabLayoutPolicy(int scroll_tab_layout)
	{
		// IGNORE
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#addTab(java.lang.String, int, com.servoy.j2db.ui.IFormLookupPanel, java.lang.String)
	 */
	public void addTab(String text, int iconMediaId, IFormLookupPanel flp, String tooltip)
	{
		// IGNORE
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#setTabForegroundAt(int, java.awt.Color)
	 */
	public void setTabForegroundAt(int index, Color fg)
	{
		// IGNORE
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#setTabBackgroundAt(int, java.awt.Color)
	 */
	public void setTabBackgroundAt(int index, Color bg)
	{
		// IGNORE
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#setTabEnabledAt(int, boolean)
	 */
	public void setTabEnabledAt(int index, boolean enabled)
	{
		IFormLookupPanel form = index == 0 ? getLeftForm() : index == 1 ? getRightForm() : null;
		if (form instanceof FormLookupPanel) ((FormLookupPanel)form).setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#setOnTabChangeMethodCmd(java.lang.String, java.lang.Object[])
	 */
	public void setOnTabChangeMethodCmd(String onTabChangeMethodCmd, Object[] onTabChangeArgs)
	{
		// IGNORE
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#removeTabAt(int)
	 */
	public boolean removeTabAt(int index)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#removeAllTabs()
	 */
	public boolean removeAllTabs()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#addTab(com.servoy.j2db.IForm, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.dataprocessing.RelatedFoundSet, int)
	 */
	public boolean addTab(IForm formController, String formName, String tabname, String tabText, String tooltip, String iconURL, String fg, String bg,
		String relationName, RelatedFoundSet relatedFs, int tabIndex)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#setTabTextAt(int, java.lang.String)
	 */
	public void setTabTextAt(int i, String text)
	{
		// IGNORE
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#getTabTextAt(int)
	 */
	public String getTabTextAt(int i)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#setMnemonicAt(int, int)
	 */
	public void setMnemonicAt(int i, int mnemonic)
	{
		// IGNORE
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#getMnemonicAt(int)
	 */
	public int getMnemonicAt(int i)
	{
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#getTabNameAt(int)
	 */
	public String getTabNameAt(int i)
	{
		IFormLookupPanel form = i == 0 ? getLeftForm() : i == 1 ? getRightForm() : null;
		return form != null ? form.getName() : null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#getTabFormNameAt(int)
	 */
	public String getTabFormNameAt(int i)
	{
		IFormLookupPanel form = i == 0 ? getLeftForm() : i == 1 ? getRightForm() : null;
		return form != null ? form.getFormName() : null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#setTabIndex(int)
	 */
	public void setTabIndex(int index)
	{
		// IGNORE
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#setTabIndex(java.lang.String)
	 */
	public void setTabIndex(String name)
	{
		// IGNORE

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#isTabEnabledAt(int)
	 */
	public boolean isTabEnabledAt(int index)
	{
		IFormLookupPanel form = index == 0 ? getLeftForm() : index == 1 ? getRightForm() : null;
		if (form instanceof FormLookupPanel) return ((FormLookupPanel)form).isEnabled();
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#getTabIndex()
	 */
	public int getTabIndex()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#getMaxTabIndex()
	 */
	public int getMaxTabIndex()
	{
		return 1;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#setHorizontalAlignment(int)
	 */
	public void setHorizontalAlignment(int alignment)
	{
		// IGNORE
	}
}