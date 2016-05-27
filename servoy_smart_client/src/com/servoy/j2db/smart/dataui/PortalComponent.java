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


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.CellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.ISupportAsyncLoading;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.printing.ISupportXMLOutput;
import com.servoy.j2db.printing.XMLPrintHelper;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.smart.TableView;
import com.servoy.j2db.ui.DataRendererOnRenderWrapper;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IPortalComponent;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.ISupportOnRenderWrapper;
import com.servoy.j2db.ui.ISupportRowStyling;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.scripting.RuntimePortal;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.IFocusCycleRoot;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ISupportFocusTransfer;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.editlist.AbstractEditListModel;
import com.servoy.j2db.util.editlist.JEditList;
import com.servoy.j2db.util.gui.AutoTransferFocusListener;

/**
 * Runtime swing portal component
 * @author jblok
 */
public class PortalComponent extends EnableScrollPanel implements ListSelectionListener, ISkinnable, IScrollPane, TableModelListener, ISupportXMLOutput,
	IPortalComponent, IFocusCycleRoot<Component>, ISupportFocusTransfer
{
	private static final long serialVersionUID = 1L;

	private static final String ACTION_GO_OUT_TO_NEXT = "goOutToNext";
	private static final String ACTION_GO_OUT_TO_PREV = "goOutToPrev";

	private final DefaultListSelectionModel EMPTY_SELECTION = new DefaultListSelectionModel();
	private final DefaultTableModel EMPTY_DATA = new DefaultTableModel();

	private TableView table;
	private Component[] editorComponents;
	private Component[] rendererComponents;

	private PortalMultilineList list;
	private DataRenderer renderer;
	private DataRenderer editor;

	private final IApplication application;
	private DataAdapterList dal;
	private String relationName;
	private List<SortColumn> defaultSort = null;

	private boolean transferFocusBackwards = false;
	private final RuntimePortal scriptable;

	public PortalComponent(final IApplication app, RuntimePortal scriptable, Form form, Portal meta, IScriptExecuter el, boolean printing)
	{
		application = app;
		setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, new JPanel());
		ComponentFactory.applyScrollBarsProperty(this, meta);

		if (meta.getMultiLine())
		{
			list = new PortalMultilineList(meta, el);
			try
			{
				renderer = (DataRenderer)application.getDataRenderFactory().createPortalRenderer(app, meta, (Form)meta.getParent(), el, printing, null);
				renderer.setRowBGColorProvider(meta.getRowBGColorCalculation(), meta.getFlattenedMethodArguments("rowBGColorCalculation"));
				editor = (DataRenderer)application.getDataRenderFactory().createPortalRenderer(app, meta, (Form)meta.getParent(), el, printing, null);
				FormBodyEditor formEditor = new FormBodyEditor(editor);
				dal = editor.getDataAdapterList();
				list.setPrototypeCellValue(new PrototypeState(null));//this call is extremely important, it prevent all rows retrieval
				list.setCellRenderer(renderer);
				list.setCellEditor(formEditor);
				java.awt.Color bg = meta.getBackground();
				if (bg != null) list.setBackground(bg);
				Component[] rendererComponents = renderer.getComponents();
				for (Component element : rendererComponents)
				{
					if (element instanceof ISupportAsyncLoading)
					{
						//in list it is impossible to get lazy loaded images displaying correctly, due to needed repaintfire, which we cannot initiate
						((ISupportAsyncLoading)element).setAsyncLoadingEnabled(false);
					}
				}

				Iterator<IPersist> portalObjectsIte = meta.getAllObjects();
				IPersist portalObj;
				while (portalObjectsIte.hasNext())
				{
					portalObj = portalObjectsIte.next();
					addOnRender(renderer.getFieldComponents().get(portalObj), meta, portalObj, el);
					addOnRender(editor.getFieldComponents().get(portalObj), meta, portalObj, el);
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
			list.setAutoscrolls(true);
			setViewportView(list);
		}
		else
		{
			table = new TableView(app, el == null ? /* design component */null : el.getFormController(), form, meta, el, null, null, printing);
			table.setOpaque(false);
			dal = table.getDataAdapterList();
			editorComponents = table.getEditorComponents();
			rendererComponents = table.getRendererComponents();

			java.awt.Color bg = meta.getBackground();
			if (bg != null)
			{
				getViewport().setBackground(bg);
				table.setBackground(bg);
			}
			setViewportView(table);
			// This is needed: http://forum.java.sun.com/thread.jspa?threadID=5148112&messageID=9751601
			table.setFocusCycleRoot(true);

			InputMap im = this.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), ACTION_GO_OUT_TO_NEXT);
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), ACTION_GO_OUT_TO_PREV);

			ActionMap am = this.getActionMap();
			am.put(ACTION_GO_OUT_TO_NEXT, new GoOutOfPortalComponentAction(false));
			am.put(ACTION_GO_OUT_TO_PREV, new GoOutOfPortalComponentAction(true));

			addJumpOutActionToComponent(table, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK), ACTION_GO_OUT_TO_NEXT, false);
			addJumpOutActionToComponent(table, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
				ACTION_GO_OUT_TO_PREV, true);
		}

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				application.getFoundSetManager().getEditRecordList().stopEditing(false);
			}
		});

		ComponentFactory.applyBasicComponentProperties(application, this, meta, ComponentFactory.getStyleForBasicComponent(application, meta, form));

		Relation[] relations = application.getFlattenedSolution().getRelationSequence(meta.getRelationName());
		if (relations != null)
		{
			relationName = meta.getRelationName();
			defaultSort = ((FoundSetManager)app.getFoundSetManager()).getSortColumns(
				application.getFlattenedSolution().getTable(relations[relations.length - 1].getForeignDataSource()), meta.getInitialSort());
		}

		setFocusCycleRoot(true);
		setFocusTraversalPolicy(ServoyFocusTraversalPolicy.datarenderPolicy);
		addFocusListener(new AutoTransferFocusListener(this, this));
		this.scriptable = scriptable;
		scriptable.setJComponent(list != null ? list : table);
	}

	public final RuntimePortal getScriptObject()
	{
		return scriptable;
	}

	// TODO: probably these should also be removed from the component at some point?
	private void addJumpOutActionToComponent(JComponent c, KeyStroke key, String actionName, boolean moveBackward)
	{
		if (!moveBackward)
		{
			Set originalForwardKeys = c.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
			Set newForwardKeys = new HashSet(originalForwardKeys);
			if (newForwardKeys.contains(key)) newForwardKeys.remove(key);
			c.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, newForwardKeys);
		}
		else
		{
			Set originalBackwardKeys = c.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
			Set newBackwardKeys = new HashSet(originalBackwardKeys);
			if (newBackwardKeys.contains(key)) newBackwardKeys.remove(key);
			c.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, newBackwardKeys);
		}

		InputMap im = c.getInputMap(WHEN_FOCUSED);
		im.put(key, actionName);
		ActionMap am = c.getActionMap();
		am.put(actionName, new GoOutOfPortalComponentAction(moveBackward));
	}

	public Component[] getEditorComponents()
	{
		if (table != null)
		{
			return editorComponents;
		}
		else
		{
			return editor.getComponents();
		}
	}

	public Component[] getRendererComponents()
	{
		if (table != null)
		{
			return rendererComponents;
		}
		else
		{
			return renderer.getComponents();
		}
	}

	public JComponent getRealComponent()
	{
		if (table != null) return table;
		return list;
	}

	public int getPreferredBreak(int preferredBreak)
	{
		if (currentData == null) return preferredBreak;

		int retval = preferredBreak;
		int i = 0;
		Rectangle r = new Rectangle();
		if (table != null)
		{
			if (table.getTableHeader() != null)
			{
				preferredBreak -= table.getTableHeader().getHeight();
				retval = preferredBreak;
			}
			while (i < currentData.getSize() && r != null && r.y <= preferredBreak)
			{
				r = table.getCellRect(i++, 0, true);
				if (r != null && r.y <= preferredBreak)
				{
					retval = r.y;
				}
				else
				{
					break;
				}
			}
			if (table.getTableHeader() != null)
			{
				retval += table.getTableHeader().getHeight();
			}
		}
		else
		{
			while (i < currentData.getSize() && r != null && r.y <= preferredBreak)
			{
				r = list.getCellBounds(i++, i);
				if (r != null && r.y <= preferredBreak)
				{
					retval = r.y;
				}
				else
				{
					break;
				}
			}
		}
		return retval;
	}

	public void destroy()
	{
		setRecord(null, false);//safety
		if (dal != null)
		{
			dal.destroy();
			dal = null;
		}
		if (table != null)
		{
			TableColumnModel tcm = table.getColumnModel();
			for (int i = 0; i < tcm.getColumnCount(); i++)
			{
				CellAdapter ca = (CellAdapter)tcm.getColumn(i);
				Object o = ca.getEditor();
				if (o instanceof IDestroyable)
				{
					((IDestroyable)o).destroy();
				}
				o = ca.getRenderer();
				if (o instanceof IDestroyable)
				{
					((IDestroyable)o).destroy();
				}
			}
		}
		if (renderer != null)
		{
			renderer.destroy();
			renderer = null;
		}
		if (editor != null)
		{
			editor.destroy();
			editor = null;
		}
	}

	@Override
	public Rectangle getBounds()
	{
		boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")); //$NON-NLS-1$
		if (isPrinting && currentData != null)
		{
			if (((FoundSet)currentData).hadMoreRows())
			{
				((FoundSet)currentData).queryForAllPKs();
			}
			currentData.getSelectionModel().hideSelectionForPrinting();
		}
		return super.getBounds();
	}

	@Override
	public void setUI(ComponentUI ui)
	{
		super.setUI(ui);
	}

	public void setValidationEnabled(boolean b)
	{
		if (dal != null) dal.setFindMode(!b);
	}

	private ISwingFoundSet currentData;

	public void setRecord(IRecordInternal state, boolean stopEditing)
	{
		if (stopEditing)
		{
			stopUIEditing(true);
		}

		IFoundSetInternal relatedFoundSet = state == null ? null : state.getRelatedFoundSet(relationName, getDefaultSort());

		if (currentData == relatedFoundSet) return;//if is same changes are seen by model listener

		if (currentData != null)
		{
			currentData.removeTableModelListener(this);
			ListSelectionModel lsm = currentData.getSelectionModel();
			lsm.removeListSelectionListener(this);
		}

		currentData = (ISwingFoundSet)relatedFoundSet;
		ISupportOnRenderCallback portalRenderComponent = null;
		if (table != null)
		{
			if (currentData == null)
			{
				table.removeEditor();//safety
				table.setSelectionModel(EMPTY_SELECTION);
				table.setModel(EMPTY_DATA);
			}
			else
			{
				ListSelectionModel lsm = currentData.getSelectionModel();

				int selected = currentData.getSelectedIndex();
				table.setSelectionModel(lsm);
				table.setModel((IFoundSetInternal)currentData);
				currentData.setSelectedIndex(selected);

				currentData.addTableModelListener(this);
				lsm.addListSelectionListener(this);

				valueChanged(null, stopEditing);
			}

			portalRenderComponent = table.getOnRenderComponent();
		}
		else
		{
			if (currentData == null)
			{
				list.setSelectionModel(EMPTY_SELECTION);
				list.setModel(new AbstractEditListModel()
				{
					public Object getElementAt(int i)
					{
						return null;
					}

					public int getSize()
					{
						return 0;
					}
				});
			}
			else
			{
				list.setSelectionModel(EMPTY_SELECTION);
				ListSelectionModel lsm = currentData.getSelectionModel();
				list.setModel(currentData);
				list.setSelectionModel(lsm);

				currentData.addTableModelListener(this);
				lsm.addListSelectionListener(this);

				valueChanged(null, stopEditing);
			}

			portalRenderComponent = list.getOnRender();
		}
		scriptable.setFoundset(currentData);

		if (currentData != null && portalRenderComponent != null && portalRenderComponent.getRenderEventExecutor().hasRenderCallback())
		{
			portalRenderComponent.getRenderEventExecutor().setRenderState(null, -1, false, true);
			portalRenderComponent.getRenderEventExecutor().fireOnRender(false);
		}
	}

	public String getSelectedRelationName()
	{
		return relationName;
	}

	public String[] getAllRelationNames()
	{
		String relationName = getSelectedRelationName();
		if (relationName == null)
		{
			return new String[0];
		}
		else
		{
			return new String[] { relationName };
		}
	}

	public void notifyVisible(boolean b, List invokeLaterRunnables)
	{
		if (dal != null)
		{
			dal.notifyVisible(b, invokeLaterRunnables);
		}
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (table != null && table.isEditing())
		{
			CellEditor ce = table.getCellEditor();
			if (ce != null && !ce.stopCellEditing()) return false;
		}
		else if (list != null && list.isEditing())
		{
			CellEditor ce = list.getCellEditor();
			if (ce != null && !ce.stopCellEditing()) return false;
		}
		if (dal != null) return dal.stopUIEditing(looseFocus);
		return true;
	}

	public void valueChanged(ListSelectionEvent e)
	{
		valueChanged(e, true);
	}

	private int lastAdjusting;

	private void valueChanged(ListSelectionEvent eventCanBeNull, boolean stopAnyEdit)
	{
		IFoundSetInternal s = null;
		if (table != null)
		{
			TableModel tm = table.getModel();
			if (tm instanceof IFoundSetInternal)
			{
				s = (IFoundSetInternal)tm;
			}
		}
		else
		{
			ListModel tm = list.getModel();
			if (tm instanceof IFoundSetInternal)
			{
				s = (IFoundSetInternal)tm;
			}
		}
		if (s != null)
		{
			if (dal != null)
			{
				int index = getRecordIndex();
				if (index != -1)
				{
					IRecordInternal state = s.getRecord(index);
					if (state != null)//can happen when notified from client and itself not having a selection yet
					{
						if (eventCanBeNull != null && eventCanBeNull.getValueIsAdjusting())
						{
							if (lastAdjusting != index)
							{
								lastAdjusting = index;

								application.getFoundSetManager().getEditRecordList().stopIfEditing(s);
							}
						}
						else
						{
							lastAdjusting = -1;
							dal.setRecord(state, stopAnyEdit);
						}
					}
					else
					{
						dal.setRecord(null, stopAnyEdit);
					}
				}
				else
				{
					dal.setRecord(null, stopAnyEdit);
				}
			}
		}
	}

	private String rowBGColorScript;
	private List<Object> rowBGColorArgs;

	public void setRowBGColorScript(String str, List<Object> args)
	{
		rowBGColorScript = str;
		rowBGColorArgs = args;
	}

	public String getRowBGColorScript()
	{
		return rowBGColorScript;
	}

	public List<Object> getRowBGColorArgs()
	{
		return rowBGColorArgs;
	}

	private int getRecordIndex()
	{
		if (table != null)
		{
			return table.getSelectedRow();
		}
		else
		{
			return list.getSelectedIndex();
		}
	}

	public void setRecordIndex(int i)
	{
		if (table != null)
		{
			table.getSelectionModel().setSelectionInterval(i, i);
			Rectangle cellRect = table.getCellRect(i, 0, false);
			if (cellRect != null)
			{
				table.scrollRectToVisible(cellRect);
			}
		}
		else
		{
			list.setSelectedIndex(i);
			list.ensureIndexIsVisible(i);
		}
	}

	@Override
	public void setBackground(Color c)
	{
		super.setBackground(c);
		getViewport().setBackground(c);
		if (table != null)
		{
			table.setBackground(c);
		}
		else
		{
			if (list != null) list.setBackground(c);
			if (renderer != null) renderer.setBackground(c);
			if (editor != null) editor.setBackground(c);
		}
	}


	@Override
	public void setForeground(Color c)
	{
		getViewport().setForeground(c);
		if (table != null)
		{
//			table.setForeground(c);
		}
		else
		{
			if (list != null) list.setForeground(c);
			if (renderer != null) renderer.setForeground(c);
			if (editor != null) editor.setForeground(c);
		}
	}


	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
			if (table != null)
			{
				if (!b) table.setEnabled(b);
			}
			else
			{
				if (!b) list.setEnabled(b);
			}
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

	public boolean isReadOnly()
	{
		if (list != null)
		{
			return !list.isEditable();
		}
		else
		{
			return !table.isEditable();
		}
	}

	public void setReadOnly(boolean b)
	{
		if (list != null)
		{
			list.setEditable(!b);
		}
		else
		{
			table.setEditable(!b);
		}
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

	/*
	 * visible---------------------------------------------------
	 */
	public void setComponentVisible(boolean b)
	{
		if (viewable)
		{
			setVisible(b);
		}
	}

	public List<SortColumn> getDefaultSort()
	{
		return defaultSort;
	}

	public void tableChanged(TableModelEvent e)
	{
		boolean isEditing = false;
		if (table != null)
		{
			isEditing = table.isEditing();
		}
		else
		{
			isEditing = list.isEditing();
		}

		if (isEditing && e.getType() == TableModelEvent.DELETE)
		{
			if (e.getFirstRow() >= currentData.getSelectedIndex() && currentData.getSelectedIndex() <= e.getLastRow())
			{
				stopUIEditing(true);
			}
		}
		else if (isEditing && e.getType() == TableModelEvent.INSERT)
		{
			stopUIEditing(true);
		}

		int index = getRecordIndex();//is Selected Index
		if (e.getFirstRow() == index)
		{
			valueChanged(null, false);//fire value chance becouse selection does not fire
		}
	}

	public void toXML(Writer w, Rectangle rec) throws IOException
	{
		if (currentData != null)
		{
			w.write("<ROWSCOMPONENT x=\"" + rec.x + "\" y=\"" + rec.y + "\" width=\"" + rec.width + "\" height=\"" + rec.height + "\" >"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			if (table == null)
			{
				w.write("<ROWS count=\"" + currentData.getSize() + "\" >"); //$NON-NLS-1$ //$NON-NLS-2$
				DataRenderer formRenderer = (DataRenderer)list.getCellRenderer();
				for (int ii = 0; ii < currentData.getSize(); ii++)
				{
					w.write("<ROW>"); //$NON-NLS-1$
					IRecordInternal state = currentData.getRecord(ii);
					formRenderer.getDataAdapterList().setRecord(state, true);
					Component[] comps = formRenderer.getComponents();
					for (Component component : comps)
					{
						XMLPrintHelper.handleComponent(w, component, component.getBounds(), null);
					}
					w.write("</ROW>"); //$NON-NLS-1$
				}
				w.write("</ROWS>"); //$NON-NLS-1$
			}
			else
			{
				w.write("<ROWS count=\"" + currentData.getSize() + "\" >"); //$NON-NLS-1$//$NON-NLS-2$
				for (int ii = 0; ii < currentData.getSize(); ii++)
				{
					w.write("<ROW>"); //$NON-NLS-1$
					IRecordInternal state = currentData.getRecord(ii);
					TableColumnModel tcm = table.getColumnModel();
					for (int i = 0; i < tcm.getColumnCount(); i++)
					{
						CellAdapter ca = (CellAdapter)tcm.getColumn(i);
						String dataProviderID = ca.getDataProviderID();
						Component component = ca.getRenderer();
						XMLPrintHelper.handleComponent(w, component, component.getBounds(), state.getValue(dataProviderID));
					}
					w.write("</ROW>"); //$NON-NLS-1$
				}
				w.write("</ROWS>"); //$NON-NLS-1$
			}
			w.write("</ROWSCOMPONENT>"); //$NON-NLS-1$
		}
	}

	@Override
	public void setOpaque(boolean isOpaque)
	{
		if (table != null)
		{
			table.getTableHeader().setOpaque(isOpaque);
		}
		else if (list != null)
		{
			list.setOpaque(isOpaque);
		}

		getViewport().setOpaque(isOpaque);
		super.setOpaque(isOpaque);
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		if (getColumnHeader() != null && isOpaque() != getColumnHeader().isOpaque()) this.getColumnHeader().setOpaque(isOpaque());
	}


	/**
	 * @param javaObject
	 * @todo Generated comment
	 */
	public void editCellFor(JComponent javaObject)
	{
		if (table != null)
		{
			int selectedRow = table.getSelectedRow();
			for (int i = 0; i < table.getColumnCount(); i++)
			{
				if (((CellAdapter)table.getCellEditor(selectedRow, i)).getEditor() == javaObject)
				{
					table.editCellAt(selectedRow, i);
					break;
				}
			}
		}
		else if (list != null)
		{
			list.editCellAt(list.getSelectedIndex());
			javaObject.requestFocus();
		}
	}

	@Override
	public String toString()
	{
		return "PortalComponent[" + getName() + ":" + getBounds();//$NON-NLS-1$ //$NON-NLS-2$
	}

	public String getId()
	{
		return (String)getClientProperty("Id");
	}


	@Override
	protected void printChildren(Graphics g)
	{
		super.printChildren(g);
		super.printBorder(g); // print border after children print, to prevent child backgrounds ontop of border
	}

	@Override
	protected void printBorder(Graphics g)
	{
		//intentionally empty to have the border drawn after the content
	}

	public boolean isTraversalPolicyEnabled()
	{
		return true;
	}

	public Component getFirstFocusableField()
	{
		return getViewport().getView();
	}

	public Component getLastFocusableField()
	{
		return getViewport().getView();
	}

	public List<Component> getTabSeqComponents()
	{
		List<Component> tabSeq = new ArrayList<Component>();
		tabSeq.add(getViewport().getView());
		return tabSeq;
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

	private class GoOutOfPortalComponentAction extends AbstractAction
	{
		private final boolean moveBackward;

		public GoOutOfPortalComponentAction(boolean moveBackward)
		{
			this.moveBackward = moveBackward;
		}

		public void actionPerformed(ActionEvent e)
		{
			PortalComponent.this.setTransferFocusBackwards(moveBackward);
			KeyboardFocusManager.getCurrentKeyboardFocusManager().upFocusCycle();
		}
	}

	private IStyleSheet styleSheet;
	private IStyleRule oddStyle;
	private IStyleRule evenStyle;
	private IStyleRule selectedStyle;
	private IStyleRule headerStyle;

	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#setStyles(javax.swing.text.html.StyleSheet, javax.swing.text.Style, javax.swing.text.Style)
	 */
	public void setRowStyles(IStyleSheet styleSheet, IStyleRule oddStyle, IStyleRule evenStyle, IStyleRule selectedStyle, IStyleRule headerStyle)
	{
		this.styleSheet = styleSheet;
		this.oddStyle = oddStyle;
		this.evenStyle = evenStyle;
		this.selectedStyle = selectedStyle;
		this.headerStyle = headerStyle;

		if (table != null)
		{
			table.setRowStyles(styleSheet, oddStyle, evenStyle, selectedStyle, headerStyle);
		}
		else if (list != null)
		{
			list.setRowStyles(styleSheet, oddStyle, evenStyle, selectedStyle, headerStyle);
			if (oddStyle != null || evenStyle != null)
			{
				for (Component c : renderer.getComponents())
				{
					if (c instanceof JComponent) ((JComponent)c).setOpaque(false);
				}
				for (Component c : editor.getComponents())
				{
					if (c instanceof JComponent) ((JComponent)c).setOpaque(false);
				}
			}
		}
	}

	public IStyleSheet getRowStyleSheet()
	{
		return styleSheet;
	}

	public IStyleRule getRowOddStyle()
	{
		return oddStyle;
	}

	public IStyleRule getRowEvenStyle()
	{
		return evenStyle;
	}

	public IStyleRule getRowSelectedStyle()
	{
		return selectedStyle;
	}

	public IStyleRule getHeaderStyle()
	{
		return headerStyle;
	}

	private void addOnRender(IDisplay display, Portal portal, IPersist portalObj, IScriptExecuter el)
	{
		if (display instanceof IScriptableProvider)
		{
			IScriptable s = ((IScriptableProvider)display).getScriptObject();

			if (s instanceof ISupportOnRenderCallback && ((ISupportOnRenderCallback)s).getRenderEventExecutor() != null)
			{
				ComponentFactoryHelper.addPortalOnRenderCallback(portal, ((ISupportOnRenderCallback)s).getRenderEventExecutor(), portalObj, el);
			}
		}
	}

	private class PortalMultilineList extends JEditList implements ISupportRowStyling, ISupportOnRenderWrapper
	{

		private final ISupportOnRenderCallback dataRendererOnRenderWrapper;
		private final Portal portal;

		private IStyleSheet ss;
		private IStyleRule oStyle;
		private IStyleRule eStyle;
		private IStyleRule sStyle;
		private IStyleRule hStyle;

		PortalMultilineList(Portal portal, IScriptExecuter se)
		{
			this.portal = portal;
			dataRendererOnRenderWrapper = new DataRendererOnRenderWrapper(this);
			int onRenderMethodID = portal.getOnRenderMethodID();
			if (onRenderMethodID > 0)
			{
				dataRendererOnRenderWrapper.getRenderEventExecutor().setRenderCallback(Integer.toString(onRenderMethodID),
					Utils.parseJSExpressions(portal.getFlattenedMethodArguments("onRenderMethodID")));
				dataRendererOnRenderWrapper.getRenderEventExecutor().setRenderScriptExecuter(se);
			}
		}

		public ISupportOnRenderCallback getOnRender()
		{
			return dataRendererOnRenderWrapper;
		}

		public void setRowStyles(IStyleSheet styleSheet, IStyleRule oddStyle, IStyleRule evenStyle, IStyleRule selectedStyle, IStyleRule headerStyle)
		{
			this.ss = styleSheet;
			this.oStyle = oddStyle;
			this.eStyle = evenStyle;
			this.sStyle = selectedStyle;
			this.hStyle = headerStyle;
		}

		public IStyleSheet getRowStyleSheet()
		{
			return ss;
		}

		public IStyleRule getRowOddStyle()
		{
			return oStyle;
		}

		public IStyleRule getRowEvenStyle()
		{
			return eStyle;
		}

		/*
		 * @see com.servoy.j2db.ui.ISupportOnRenderWrapper#getOnRenderComponent()
		 */
		public ISupportOnRenderCallback getOnRenderComponent()
		{
			return dataRendererOnRenderWrapper;
		}

		/*
		 * @see com.servoy.j2db.ui.ISupportOnRenderWrapper#getOnRenderElementType()
		 */
		public String getOnRenderElementType()
		{
			return IRuntimeComponent.PORTAL;
		}

		/*
		 * @see com.servoy.j2db.ui.ISupportOnRenderWrapper#getOnRenderToString()
		 */
		public String getOnRenderToString()
		{
			return portal.toString();
		}

		/*
		 * @see com.servoy.j2db.ui.ISupportRowStyling#getSelectedStyle()
		 */
		public IStyleRule getRowSelectedStyle()
		{
			return sStyle;
		}

		public IStyleRule getHeaderStyle()
		{
			return hStyle;
		}
	}
}