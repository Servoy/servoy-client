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

import com.servoy.j2db.BasicFormController;
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
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.ui.IFormUI;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.ISplitPane;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.RuntimeSplitPane;
import com.servoy.j2db.util.Utils;

/**
 * This class represents a split pane in the web client
 *
 * @author gboros
 */
public class WebSplitPane extends Component implements ISplitPane, IDisplayRelatedData, IProviderStylePropertyChanges, ISupportSecuritySettings,
	ISupportWebBounds, ISupportWebTabSeq, ListSelectionListener, IWebFormContainer, ISupportSimulateBoundsProvider
{
	private final IApplication application;
	private final int orient;
	private Color background;
	private Border border;
	private Font font;
	private Color foreground;
	private Point location = new Point(0, 0);
	private Dimension size = new Dimension(0, 0);
	private String name;
	private String tooltip;
	private boolean opaque;
	private boolean accessible = true;
	private final List<ISwingFoundSet> related = new ArrayList<ISwingFoundSet>();
	private double dividerLocation;
	private int dividerSize = 5;
	private boolean continuousLayout;
	private double resizeWeight;
	private int leftFormMinSize, rightFormMinSize;

	protected IRecordInternal parentData;
	private final List<String> allRelationNames = new ArrayList<String>(2);
	private final Component splitter;
	private final Component[] splitComponents = new Component[2];
	private final WebTabHolder[] webTabs = new WebTabHolder[2];
	private final boolean[] paneChanged = new boolean[] { false, false };

	private int tabSequenceIndex = ISupportWebTabSeq.DEFAULT;
	private int leftPanelLastTabIndex = ISupportWebTabSeq.DEFAULT;
	private boolean sizeChanged = false;

	private String onDividerChangeMethodCmd;
	private IScriptExecuter scriptExecutor;
	private final RuntimeSplitPane scriptable;

	public WebSplitPane(IApplication application, RuntimeSplitPane scriptable, String name, int orient)
	{
		super(name);
		this.application = application;
		this.orient = orient;

		splitter = new Component("splitter"); //$NON-NLS-1$
		splitComponents[0] = new Component("websplit_left"); //$NON-NLS-1$
		splitComponents[1] = new Component("websplit_right"); //$NON-NLS-1$
		splitComponents[0].add(new Component("webform")); //$NON-NLS-1$
		splitComponents[1].add(new Component("webform")); //$NON-NLS-1$

		splitter.add(splitComponents[0]);
		add(splitter);
		add(splitComponents[1]);
		this.scriptable = scriptable;
	}

	public final RuntimeSplitPane getScriptObject()
	{
		return scriptable;
	}

	@Override
	public Color getBackground()
	{
		return background;
	}

	@Override
	public Border getBorder()
	{
		return border;
	}

	@Override
	public Font getFont()
	{
		return font;
	}

	@Override
	public Color getForeground()
	{
		return foreground;
	}

	@Override
	public Point getLocation()
	{
		return location;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Dimension getSize()
	{
		return size;
	}

	@Override
	public String getToolTipText()
	{
		return tooltip;
	}

	@Override
	public boolean isOpaque()
	{
		return opaque;
	}

	@Override
	public void setBackground(Color background)
	{
		this.background = background;
	}

	@Override
	public void setBorder(Border border)
	{
		this.border = border;
	}

	@Override
	public void setComponentEnabled(final boolean enabled)
	{
		if (accessible)
		{
			super.setEnabled(enabled);
			getStylePropertyChanges().setChanged();
		}
	}

	@Override
	public void setComponentVisible(boolean visible)
	{
		if (viewable)
		{
			setVisible(visible);
		}
	}

	@Override
	public void setCursor(Cursor cursor)
	{
	}

	@Override
	public void setFont(Font font)
	{
		this.font = font;
	}

	@Override
	public void setForeground(Color foreground)
	{
		this.foreground = foreground;
	}

	@Override
	public void setLocation(Point location)
	{
		this.location = location;
	}

	@Override
	public void setName(String n)
	{
		name = n;
	}

	@Override
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	@Override
	public void setSize(Dimension size)
	{
		if (this.size != null)
		{
			for (int tabIdx = 0; tabIdx < 2; tabIdx++)
			{
				if (webTabs[tabIdx] != null && webTabs[tabIdx].getPanel().isReady())
				{
					webTabs[tabIdx].getPanel().getWebForm().setFormWidth(0);
				}
			}
		}
		this.size = size;
	}

	@Override
	public void setToolTipText(String tooltip)
	{
		this.tooltip = Utils.stringIsEmpty(tooltip) ? null : tooltip;
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
		// cant return anything because the splitpane shows 2 forms, so which one to get?
		return null;
	}

	public String getSelectedRelationName()
	{
		return null;
	}

	public void notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			notifyVisibleForm(visible, tabIdx, invokeLaterRunnables);
		}
	}

	private boolean notifyVisibleForm(boolean visible, int tabIdx, List<Runnable> invokeLaterRunnables)
	{
		if (webTabs[tabIdx] != null)
		{
			WebTabFormLookup fl = webTabs[tabIdx].getPanel();
			FormController controller = fl.getWebForm().getController();

			//this is not needed when closing
			if (visible)
			{
				if (parentData != null) showFoundSet(fl, parentData, controller.getDefaultSortColumns());

				// Test if current one is there
				if (fl.isReady())
				{
					if (splitComponents[tabIdx].get(fl.getWebForm().getId()) != null)
					{
						// replace it
						splitComponents[tabIdx].replace(fl.getWebForm());
					}
					else
					{
						// else add it
						splitComponents[tabIdx].add(fl.getWebForm());
					}
					FormController fc = fl.getWebForm().getController();
					if (tabIdx == 1 && webTabs[0] != null) fc.recomputeTabSequence(leftPanelLastTabIndex);
					else fc.recomputeTabSequence(tabSequenceIndex);
				}
			}

			return controller.notifyVisible(visible, invokeLaterRunnables, true);
		}

		return false;
	}

	public void setRecord(IRecordInternal parentState, boolean stopEditing)
	{
		parentData = parentState;
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			if (webTabs[tabIdx] != null)
			{
				WebTabFormLookup fl = webTabs[tabIdx].getPanel();
				showFoundSet(fl, parentState, fl.getDefaultSort(parentData != null || fl.isReady()));
			}
		}
	}

	public boolean isCurrentForm(IFormUI formUI)
	{
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			if (webTabs[tabIdx] != null && webTabs[tabIdx].getPanel().getWebForm() == formUI)
			{
				return true;
			}
		}
		return false;
	}

	public void recomputeTabSequence()
	{
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			if (webTabs[tabIdx] != null && webTabs[tabIdx].getPanel().getWebForm() != null)
			{
				webTabs[tabIdx].getPanel().getWebForm().getController().recomputeTabSequence(tabSequenceIndex);
			}
		}
	}

	public boolean isReadOnly()
	{
		boolean isReadOnly = true;
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			isReadOnly = isReadOnly && (webTabs[tabIdx] != null ? webTabs[tabIdx].getPanel().isReadOnly() : false);
		}

		return isReadOnly;
	}

	public void setValidationEnabled(boolean mode)
	{
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		boolean stopUIEditing = true;
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			if (webTabs[tabIdx] != null && webTabs[tabIdx].getPanel().isReady())
			{
				stopUIEditing = stopUIEditing && webTabs[tabIdx].getPanel().getWebForm().getController().stopUIEditing(true);
			} // else there is nothing there that could be edited (avoid creating forms again with getWebForm() when the app. is shutting down)
		}
		return stopUIEditing;
	}

	public void destroy()
	{
		deregisterSelectionListeners();
		//TODO should deregister related foundsets??
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
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

	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)getStylePropertyChanges()).getPaddingAndBorder(size.height, border, new Insets(0, 0, 0, 0), 0, null);
	}

	public Rectangle getWebBounds()
	{
		Dimension d = ((ChangesRecorder)getStylePropertyChanges()).calculateWebSize(size.width, size.height, border, new Insets(0, 0, 0, 0), 0, null);
		return new Rectangle(location, d);
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (parentData != null)
		{
			if (webTabs[0] != null)
			{
				WebTabFormLookup panel = webTabs[0].getPanel();
				showFoundSet(panel, parentData, panel.getDefaultSort(true));
			}
			if (webTabs[1] != null)
			{
				WebTabFormLookup panel = webTabs[1].getPanel();
				showFoundSet(panel, parentData, panel.getDefaultSort(true));
			}
		}
	}

	private void showFoundSet(WebTabFormLookup flp, IRecordInternal parentState, List<SortColumn> sort)
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

	public int getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}


	public void setReadOnly(boolean b)
	{
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			if (webTabs[tabIdx] != null) webTabs[tabIdx].getPanel().setReadOnly(b);
		}
	}

	public void setLeftForm(IFormLookupPanel flp)
	{
		allRelationNames.add(0, flp == null ? null : flp.getRelationName());
		webTabs[0] = new WebTabHolder(flp.getFormName(), flp, null, null);
	}

	public IFormLookupPanel getLeftForm()
	{
		return webTabs[0] != null ? webTabs[0].getPanel() : null;
	}

	public void setRightForm(IFormLookupPanel flp)
	{
		allRelationNames.add(allRelationNames.size() > 0 ? 1 : 0, flp == null ? null : flp.getRelationName());
		webTabs[1] = new WebTabHolder(flp.getFormName(), flp, null, null);
	}

	public IFormLookupPanel getRightForm()
	{
		return webTabs[1] != null ? webTabs[1].getPanel() : null;
	}

	public IFormUI[] getChildForms()
	{
		IFormUI leftForm = null;
		if (webTabs[0] != null)
		{
			leftForm = webTabs[0].getPanel().getWebForm();
			if (leftForm != null && ((Component)leftForm).getParent() == null)
			{
				leftForm = null;
			}
		}
		IFormUI rightForm = null;
		if (webTabs[1] != null)
		{
			rightForm = webTabs[1].getPanel().getWebForm();
			if (rightForm != null && ((Component)rightForm).getParent() == null)
			{
				rightForm = null;
			}
		}
		return leftForm != null || rightForm != null ? new IFormUI[] { leftForm, rightForm } : null;
	}

	public IFormLookupPanel createFormLookupPanel(String tabname, String relationName, String formName)
	{
		return new WebTabFormLookup(tabname, relationName, formName, this, application);
	}

	public FormScope getForm(boolean bLeftForm)
	{
		int i = bLeftForm ? 0 : 1;
		if (webTabs[i] != null) return webTabs[i].getPanel().getWebForm().getController().getFormScope();
		return null;
	}

	private void setDividerLocationInternal(double newDividerLocation)
	{
		if (Math.abs(dividerLocation - newDividerLocation) > Double.MIN_VALUE)
		{
			dividerLocation = newDividerLocation;
			if (onDividerChangeMethodCmd != null && scriptExecutor != null)
			{
				scriptExecutor.executeFunction(onDividerChangeMethodCmd, new Object[] { new Integer(-1) }, false, WebSplitPane.this, false,
					StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID.getPropertyName(), true);
			}
		}
	}

	public void setDividerLocation(double newDividerLocation)
	{
		if (newDividerLocation < 0) return;
		setDividerLocationInternal(newDividerLocation);
		sizeChanged = true;
	}

	public void setRuntimeDividerLocation(double locationPos)
	{
		if (locationPos < 0) return;
		setDividerLocationInternal(locationPos);
		sizeChanged = true;
	}

	public double getDividerLocation()
	{
		return dividerLocation;
	}

	public void setDividerSize(int size)
	{
		dividerSize = size < 0 ? 0 : size;
		sizeChanged = true;
	}

	public int getDividerSize()
	{
		return dividerSize;
	}

	public double getResizeWeight()
	{
		return resizeWeight;
	}

	public void setResizeWeight(double resizeWeight)
	{
		this.resizeWeight = resizeWeight;
	}

	public boolean getContinuousLayout()
	{
		return continuousLayout;
	}

	public void setContinuousLayout(boolean b)
	{
		continuousLayout = b;
	}

	public void setFormMinSize(boolean bLeftForm, int minSize)
	{
		if (bLeftForm)
		{
			leftFormMinSize = minSize;
		}
		else
		{
			rightFormMinSize = minSize;
		}
	}

	public int getFormMinSize(boolean bLeftForm)
	{
		return bLeftForm ? leftFormMinSize : rightFormMinSize;
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
		if (form instanceof BasicFormController.JSForm)
		{
			f = (FormController)((BasicFormController.JSForm)form).getFormPanel();
			readOnly = f.isReadOnly();
		}

		if (f != null) fName = f.getName();
		if (form instanceof String) fName = (String)form;
		if (fName != null)
		{
			String tabname = fName;

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
				boolean bNotifyVisibleForm = notifyVisibleForm(false, bLeftForm ? 0 : 1, invokeLaterRunnables);
				Utils.invokeLater(application, invokeLaterRunnables);
				if (!bNotifyVisibleForm) return false;
			}

			WebTabFormLookup flp = (WebTabFormLookup)createFormLookupPanel(tabname, relationName, fName);
			if (f != null) flp.setReadOnly(readOnly);

			if (bLeftForm) setLeftForm(flp);
			else setRightForm(flp);
			if (relatedFs != null)
			{
				FormController fp = flp.getWebForm().getController();
				if (fp != null && flp.getRelationName() != null && flp.getRelationName().equals(relationName))
				{
					fp.loadData(relatedFs, null);
				}
			}

			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>(0);
			boolean bNotifyVisibleForm = notifyVisibleForm(true, bLeftForm ? 0 : 1, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
			if (bNotifyVisibleForm)
			{
				paneChanged[bLeftForm ? 0 : 1] = true;
			}
			return bNotifyVisibleForm;
		}
		else if (form == null)
		{
			IFormLookupPanel replacedForm = bLeftForm ? getLeftForm() : getRightForm();
			if (replacedForm != null)
			{
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>(0);
				boolean bNotifyVisibleForm = notifyVisibleForm(false, bLeftForm ? 0 : 1, invokeLaterRunnables);
				Utils.invokeLater(application, invokeLaterRunnables);
				if (!bNotifyVisibleForm) return false;
			}

			splitComponents[bLeftForm ? 0 : 1].replace(new Component("webform"));
			webTabs[bLeftForm ? 0 : 1] = null;
			paneChanged[bLeftForm ? 0 : 1] = true;
			return true;
		}
		return false;
	}

	public void setTabSequenceIndex(int tabIndex)
	{
		this.tabSequenceIndex = tabIndex;
	}

	public int getTabSequenceIndex()
	{
		return tabSequenceIndex;
	}

	public void setFormLastTabIndex(WebForm form, int lastTabIndex)
	{
		if (webTabs[0] != null && form.equals(webTabs[0].getPanel().getWebForm())) leftPanelLastTabIndex = lastTabIndex;
	}

	public void notifyResized()
	{
		for (int i = 0; i < 2; i++)
		{
			if (webTabs[i] != null && webTabs[i].getPanel().isReady())
			{
				WebForm webForm = webTabs[i].getPanel().getWebForm();
				FormController controller = webForm.getController();
				if (controller != null && webForm.isFormWidthHeightChanged())
				{
					controller.notifyResized();
					webForm.clearFormWidthHeightChangedFlag();
				}
			}
		}
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

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
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
		if (form instanceof WebTabFormLookup) ((WebTabFormLookup)form).getWebForm().setEnabled(enabled);
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
		// TODO Auto-generated method stub
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
		if (form instanceof WebTabFormLookup) return ((WebTabFormLookup)form).getWebForm().isEnabled();
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ITabPanel#getTabIndex()
	 */
	public int getTabIndex()
	{
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

	@Override
	public void uiRecreated()
	{
		recomputeTabSequence();
	}
}
