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
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.ui.IAccessible;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.ui.ISplitPane;
import com.servoy.j2db.util.AutoTransferFocusListener;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.IFocusCycleRoot;
import com.servoy.j2db.util.ISupportFocusTransfer;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;


public class SpecialSplitPane extends EnablePanel implements ISplitPane, IDisplayRelatedData, IAccessible, IFocusCycleRoot<Component>, ISupportFocusTransfer,
	ListSelectionListener
{
	private final IApplication application;
	private final SplitPane splitPane;
	private final List<String> allRelationNames = new ArrayList<String>(2);
	private final List<ISwingFoundSet> related = new ArrayList<ISwingFoundSet>();
	private IRecordInternal parentData;
	private boolean accessible = true;
	private boolean validationEnabled = true;
	private final List<Component> tabSeqComponentList = new ArrayList<Component>();
	private boolean transferFocusBackwards = false;

	public SpecialSplitPane(IApplication app, int orient)
	{
		super();
		application = app;
		setLayout(new BorderLayout());
		splitPane = new SplitPane(app, orient);
		add(splitPane, BorderLayout.CENTER);

		setFocusTraversalPolicy(ServoyFocusTraversalPolicy.defaultPolicy);
		tabSeqComponentList.add(splitPane);
		addFocusListener(new AutoTransferFocusListener(this, this));
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
		setVisible(visible);
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
		this.validationEnabled = validationEnabled;
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

	public String js_getToolTipText()
	{
		return splitPane.getToolTipText();
	}

	public boolean js_isTransparent()
	{
		return !isOpaque();
	}

	public void js_setFont(String spec)
	{
		splitPane.setFont(PersistHelper.createFont(spec));
	}

	public void js_setToolTipText(String tooltip)
	{
		splitPane.setToolTipText(tooltip);
	}

	@Override
	public void setOpaque(boolean isOpaque)
	{
		if (splitPane != null) splitPane.setOpaque(isOpaque);
		super.setOpaque(isOpaque);
	}

	public void js_setTransparent(boolean b)
	{
		setOpaque(!b);
		repaint();
	}

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

	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(splitPane.getBackground());
	}

	public Object js_getClientProperty(Object key)
	{
		return getClientProperty(key);
	}

	public String js_getElementType()
	{
		return "SPLITPANE"; //$NON-NLS-1$
	}

	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(splitPane.getForeground());
	}

	public int js_getHeight()
	{
		return getSize().height;
	}

	public int js_getLocationX()
	{
		return getLocation().x;
	}

	public int js_getLocationY()
	{
		return getLocation().y;
	}

	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}

	public int js_getWidth()
	{
		return getSize().width;
	}

	public boolean js_isEnabled()
	{
		return isEnabled();
	}

	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_putClientProperty(Object key, Object value)
	{
		putClientProperty(key, value);
		splitPane.putClientProperty(key, value);
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

	public void js_setBgcolor(String clr)
	{
		setBackground(PersistHelper.createColor(clr));
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

	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
	}

	public void js_setEnabled(boolean b)
	{
		setComponentEnabled(b);
	}

	public void js_setFgcolor(String clr)
	{
		splitPane.setForeground(PersistHelper.createColor(clr));
	}

	public void js_setLocation(int x, int y)
	{
		setLocation(x, y);
	}

	public void js_setSize(int width, int height)
	{
		setSize(width, height);
		revalidate();
		repaint();
	}

	public void js_setVisible(boolean b)
	{
		setVisible(b);
	}

	public boolean js_isReadOnly()
	{
		return splitPane.isReadOnly();
	}

	public void js_setReadOnly(boolean b)
	{
		splitPane.setReadOnly(b);
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
				if (relatedFoundSet != null) registerSelectionListeners(parentState, flp.getRelationName());
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
		allRelationNames.add(1, flp == null ? null : flp.getRelationName());
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

	public boolean js_setLeftForm(Object form, Object relation)
	{
		return setForm(true, form, relation);
	}

	public FormScope js_getLeftForm()
	{
		Component leftComponent = splitPane.getLeftComponent();
		if (leftComponent instanceof FormLookupPanel) return ((FormLookupPanel)leftComponent).getFormPanel().getFormScope();
		return null;
	}

	public boolean js_setRightForm(Object form, Object relation)
	{
		return setForm(false, form, relation);
	}

	public FormScope js_getRightForm()
	{
		Component rightComponent = splitPane.getRightComponent();
		if (rightComponent instanceof FormLookupPanel) return ((FormLookupPanel)rightComponent).getFormPanel().getFormScope();
		return null;
	}

	public void js_setDividerLocation(final double location)
	{
		application.invokeLater(new Runnable()
		{
			public void run()
			{
				if (location < 1) splitPane.setDividerLocation(location);
				else splitPane.setDividerLocation((int)location);
			}
		});
	}

	public double js_getDividerLocation()
	{
		return splitPane.getDividerLocation();
	}

	public void js_setDividerSize(int size)
	{
		splitPane.setDividerSize(size < 0 ? -1 : size);
	}

	public int js_getDividerSize()
	{
		return splitPane.getDividerSize();
	}

	public double js_getResizeWeight()
	{
		return splitPane.getResizeWeight();
	}

	public void js_setResizeWeight(double resizeWeight)
	{
		splitPane.setResizeWeight(resizeWeight);
	}

	public boolean js_getContinuousLayout()
	{
		return splitPane.isContinuousLayout();
	}

	public void js_setContinuousLayout(boolean b)
	{
		splitPane.setContinuousLayout(b);
	}


	public int js_getRightFormMinSize()
	{
		return splitPane.getRightFormMinSize();
	}

	public void js_setRightFormMinSize(int minSize)
	{
		splitPane.setRightFormMinSize(minSize);
	}

	public int js_getLeftFormMinSize()
	{
		return splitPane.getLeftFormMinSize();
	}

	public void js_setLeftFormMinSize(int minSize)
	{
		splitPane.setLeftFormMinSize(minSize);
	}

	private boolean setForm(boolean bLeftForm, Object form, Object relation)
	{
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
				if (!relatedFs.getDataSource().equals(f.getDataSource()))
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

			FormLookupPanel flp = createFormLookupPanel(name, relationName, f.getName());
			flp.setReadOnly(readOnly);

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
}