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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Area;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.CSS;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IView;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.component.ServoyBeanState;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.DisplaysAdapter;
import com.servoy.j2db.dataprocessing.FindState;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.smart.ListView;
import com.servoy.j2db.smart.TableView;
import com.servoy.j2db.ui.IScriptRenderMethods;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.ISupportRowStyling;
import com.servoy.j2db.ui.ISupportsDoubleBackground;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.RenderableWrapper;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.scripting.IFormatScriptComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.Internalize;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * Tableview renderer/editor wrapper (for use in swing)
 * @author jblok
 */
public class CellAdapter extends TableColumn implements TableCellEditor, TableCellRenderer, IDataAdapter, ItemListener, ActionListener, IEditListener
{
	private static final long serialVersionUID = 1L;

	private static final Object NONE = new Object();

	/** row to dataprovider map that holds a temp value to test lazy loading */
	private final Set<String> rowAndDataprovider = new HashSet<String>();

	//holds list of all displays
	private List<CellAdapter> displays;

	private final IApplication application;
	private final Component renderer;
	private final Component editor;
	private int editorX;
	private int editorWidth;
	private final String dataProviderID;
	private IRecordInternal currentEditingState;
	private DataAdapterList dal;
	private final TableView table;
	private String name;

	private Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	protected static JComponent empty = new JLabel();

	// We need a place to store the color the JLabel should be returned
	// to after its foreground and background colors have been set
	// to the selection background color.
	// These ivars will be made protected when their names are finalized.
	private Color unselectedForeground;
	private Color unselectedBackground;
	private Font unselectedFont;

	private Color lastEditorBgColor;
	private Color lastEditorFgColor;
	private Font lastEditorFont;

	private Color componentBgColor;
	private Color componentFgColor;
	private Font componentFont;

	private boolean adjusting = false;
	private Object lastInvalidValue = NONE;

	private RenderableWrapper renderableWrapper;

	public CellAdapter(IApplication app, final TableView table, final int index, int width, String name, String title, String dataProviderID,
		Component renderer, Component editor)
	{
		super(index, width);
		this.application = app;
		this.table = table;
		this.dataProviderID = dataProviderID;
		this.renderer = renderer;
		this.name = name;
		if (renderer != null)
		{
			componentBgColor = renderer.getBackground();
			componentFgColor = renderer.getForeground();
			componentFont = renderer.getFont();

			if (renderer instanceof IScriptableProvider)
			{
				IScriptable scriptable = ((IScriptableProvider)renderer).getScriptObject();
				if (scriptable instanceof ISupportOnRenderCallback)
				{
					IScriptRenderMethods renderable = ((ISupportOnRenderCallback)scriptable).getRenderable();
					if (renderable instanceof RenderableWrapper) renderableWrapper = (RenderableWrapper)renderable;
				}
			}

			renderer.addComponentListener(new ComponentListener()
			{
				public void componentShown(ComponentEvent e)
				{
					// if visibility change is done onRender, don't change the column
					if (renderableWrapper != null && renderableWrapper.getProperty(RenderableWrapper.PROPERTY_VISIBLE) != null)
					{
						return;
					}

					int i = table.getColumnModel().getColumnCount();
					table.getColumnModel().addColumn(CellAdapter.this);

					// make sure it appears back in it's design position
					int realColumnIndex = getRealColumnIndex(table.getColumnModel().getColumns(), index);
					if (realColumnIndex < i)
					{
						table.getColumnModel().moveColumn(i, realColumnIndex);
					}
				}

				private int getRealColumnIndex(Enumeration<TableColumn> columns, int idx)
				{
					// some other columns (with indexes < index) might be hidden (removed from the table)
					// - so in order to correctly position this column we must see the real column index in the
					// table where is should appear - based on index and the indexes of other columns (hidden & shown);
					// visible columns are still added to the table column model
					int realIndex = 0;

					while (columns.hasMoreElements())
					{
						TableColumn ca = columns.nextElement();
						if (ca.getModelIndex() < idx)
						{
							realIndex++;
						}
					}

					return realIndex;
				}

				public void componentHidden(ComponentEvent e)
				{
					// if visibility change is done onRender, don't change the column
					if (renderableWrapper != null && renderableWrapper.getProperty(RenderableWrapper.PROPERTY_VISIBLE) != null)
					{
						return;
					}
					table.getColumnModel().removeColumn(CellAdapter.this);
				}

				public void componentResized(ComponentEvent e)
				{
				}

				public void componentMoved(ComponentEvent e)
				{
				}
			});
			if (renderer instanceof JComponent)
			{
				Border rBorder = ((JComponent)renderer).getBorder();
				noFocusBorder = rBorder;
			}
		}
		this.editor = editor;
		if (editor != null)
		{
			updateEditorX();
			updateEditorWidth();

			editor.addComponentListener(new ComponentListener()
			{
				public void componentHidden(ComponentEvent e)
				{
				}

				public void componentMoved(ComponentEvent e)
				{
					int sourceX = (e.getComponent() instanceof ISupportCachedLocationAndSize)
						? ((ISupportCachedLocationAndSize)e.getComponent()).getCachedLocation().x : e.getComponent().getLocation().x;
					if (editorX != sourceX) // see also updateEditorX and updateEditorWidth call hierarchy to understand this better
					{
						onEditorChanged();
						editorX = sourceX; // just to make sure
					}
				}

				public void componentResized(ComponentEvent e)
				{
					int sourceWidth = (e.getComponent() instanceof ISupportCachedLocationAndSize)
						? ((ISupportCachedLocationAndSize)e.getComponent()).getCachedSize().width : e.getComponent().getSize().width;
					if (editorWidth != sourceWidth) // see also updateEditorX and updateEditorWidth call hierarchy to understand this better
					{
						onEditorChanged();
						editorWidth = sourceWidth; // just to make sure
					}
				}

				public void componentShown(ComponentEvent e)
				{
				}

			});
		}

		super.setCellRenderer(this);
		super.setCellEditor(this);

		if (title == null || title.length() == 0)
		{
			setHeaderValue(" "); //$NON-NLS-1$
		}
		else
		{
			setHeaderValue(title);
		}

		setIdentifier(editor != null ? editor.getName() : name);

		if (editor instanceof DataCheckBox)
		{
			((DataCheckBox)editor).addItemListener(this);
			((DataCheckBox)editor).setText(""); //$NON-NLS-1$
			((DataCheckBox)renderer).setText(""); //$NON-NLS-1$
		}
		else if (editor instanceof DataComboBox)
		{
			((DataComboBox)editor).addItemListener(this);
		}
		else if (editor instanceof DataCalendar)
		{
			((DataCalendar)editor).addActionListener(this);
			((DataCalendar)editor).addEditListener(this);
		}
		else if (editor instanceof DataField)
		{
			((DataField)editor).addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent e)
				{
					if (!e.isTemporary())
					{
						application.invokeLater(new Runnable()
						{
							public void run()
							{
								// if it already has focus again, igore this.
								if (CellAdapter.this.editor.hasFocus()) return;
								// if the tableview itself has focus also ignore this
								if (CellAdapter.this.table.hasFocus()) return;
								// only stop the cell editor if it is still this one.
								TableCellEditor tableCellEditor = table.getCellEditor();
								if (tableCellEditor == CellAdapter.this) tableCellEditor.stopCellEditing();
							}
						});
					}
				}
			});
			((DataField)editor).addEditListener(this);
		}
		else if (editor instanceof IDisplayData)
		{
			((IDisplayData)editor).addEditListener(this);
		}

		// editor is never transparent
		// if the editor must be transparent, then make sure that the getBackGround call done below
		// is done on a none transparent field (and reset the transparency)
		if (!editor.isOpaque() && editor instanceof JComponent)
		{
			((JComponent)editor).setOpaque(true);
			unselectedBackground = editor.getBackground();
			((JComponent)editor).setOpaque(false);
		}
		else
		{
			unselectedBackground = editor.getBackground();
		}
		unselectedForeground = editor.getForeground();
		unselectedFont = editor.getFont();

	}

	public void setDataAdapterList(DataAdapterList dal)
	{
		this.dal = dal;
		if (editor instanceof IDisplayData)
		{
			((IDisplayData)editor).setTagResolver(dal);
		}
	}

	private boolean findMode = false;

	public void setFindMode(boolean b)
	{
		findMode = b;
		if (editor instanceof IDisplayData)
		{
			IDisplayData dt = (IDisplayData)editor;
			if (!(dal.getFormScope() != null && dt.getDataProviderID() != null && dal.getFormScope().get(dt.getDataProviderID()) != Scriptable.NOT_FOUND)) // skip for form variables
			{
				dt.setValidationEnabled(!b);
			}
		}
		if (renderer instanceof IDisplayData)
		{
			IDisplayData dt = (IDisplayData)renderer;
			if (!(dal.getFormScope() != null && dt.getDataProviderID() != null && dal.getFormScope().get(dt.getDataProviderID()) != Scriptable.NOT_FOUND)) // skip for form variables
			{
				dt.setValidationEnabled(!b);
			}
		}
		if (displays != null)
		{
			for (int d = 0; d < displays.size(); d++)
			{
				displays.get(d).setFindMode(b);
			}
		}
		if (table != null && !table.isEditing()) currentEditingState = null;
	}

	private boolean isVisible(Component comp)
	{
		boolean isVisible = false;
		if (comp != null)
		{
			if (comp instanceof IScriptableProvider && ((IScriptableProvider)comp).getScriptObject() instanceof ISupportOnRenderCallback &&
				((ISupportOnRenderCallback)((IScriptableProvider)comp).getScriptObject()).getRenderEventExecutor().hasRenderCallback())
			{
				isVisible = true;
			}
			else
			{
				isVisible = comp.isVisible();
			}
		}

		return isVisible;
	}

	/*
	 * @see TableCellEditor#getTableCellEditorComponent(JTable, Object, boolean, int, int)
	 */
	public Component getTableCellEditorComponent(JTable jtable, Object value, boolean isSelected, int row, int column)
	{
		if (editor == null || !isVisible(editor) || !(jtable.getModel() instanceof IFoundSetInternal))
		{
			return empty;
		}

		IRecordInternal newRec = ((IFoundSetInternal)jtable.getModel()).getRecord(row);

		if (isSelected)
		{
			Color bgColor = getBgColor(jtable, isSelected, row, true);
			if (bgColor != null && editor instanceof JComponent) ((JComponent)editor).setOpaque(true);
			if (bgColor == null)
			{
				bgColor = unselectedBackground; // unselected background is the default background color of the editor.
			}
			lastEditorBgColor = bgColor;
			if (editor instanceof ISupportsDoubleBackground)
			{
				((ISupportsDoubleBackground)editor).setBackground(bgColor, unselectedBackground);
			}
			else
			{
				editor.setBackground(bgColor);
			}


			Color fgColor = getFgColor(jtable, isSelected, row);
			if (fgColor == null)
			{
				fgColor = unselectedForeground; // unselected foreground is the default foreground color of the editor.
			}
			lastEditorFgColor = fgColor;
			if (editor instanceof ISupportsDoubleBackground)
			{
				((ISupportsDoubleBackground)editor).setForeground(fgColor, unselectedForeground);
			}
			else
			{
				editor.setForeground(fgColor);
			}

			Font font = getFont(jtable, isSelected, row);
			if (font == null)
			{
				font = unselectedFont; // unselected font is the default font of the editor.
			}
			lastEditorFont = font;
			editor.setFont(font);


			Border styleBorder = getBorder(jtable, isSelected, row);
			if (styleBorder != null && editor instanceof JComponent)
			{
				styleBorder = new ReducedBorder(styleBorder, 0);

				if (editor instanceof AbstractButton && !((AbstractButton)editor).isBorderPainted())
				{
					((AbstractButton)editor).setBorderPainted(true);
				}

				Border marginBorder = null;
				if (noFocusBorder instanceof EmptyBorder)
				{
					marginBorder = noFocusBorder;
				}
				else if (noFocusBorder instanceof CompoundBorder && ((CompoundBorder)noFocusBorder).getInsideBorder() instanceof EmptyBorder)
				{
					marginBorder = ((CompoundBorder)noFocusBorder).getInsideBorder();
				}

				// if we have margin set on the component, keep it along with the style border
				if (marginBorder != null)
				{
					styleBorder = new CompoundBorder(styleBorder, marginBorder);
				}


				((JComponent)editor).setBorder(styleBorder);
			}

		}

//		try
//		{
//			if (currentEditingState != null && newRec != currentEditingState && currentEditingState.isEditing())
//			{
//				currentEditingState.stopEditing();
//			}
//		}
//		catch (Exception e)
//		{
//			Debug.error(e);
//		}
		currentEditingState = newRec;

		if (currentEditingState != null)
		{
			if (editor instanceof IScriptableProvider)
			{
				IScriptable scriptable = ((IScriptableProvider)editor).getScriptObject();
				if (scriptable instanceof ISupportOnRenderCallback)
				{
					RenderEventExecutor renderEventExecutor = ((ISupportOnRenderCallback)scriptable).getRenderEventExecutor();
					if (renderEventExecutor != null && renderEventExecutor.hasRenderCallback())
					{
						renderEventExecutor.setRenderState(currentEditingState, row, isSelected, true);
					}
				}
			}
			// if not enabled or not editable do not start the edit
			if (editor instanceof IDisplay && ((IDisplay)editor).isEnabled() && !((IDisplay)editor).isReadOnly())
			{
				DisplaysAdapter.startEdit(dal, (IDisplay)editor, currentEditingState);
			}


			if (editor instanceof IDisplayRelatedData)
			{
				IDisplayRelatedData drd = (IDisplayRelatedData)editor;

				IRecordInternal state = ((IFoundSetInternal)jtable.getModel()).getRecord(row);
				if (state != null)
				{
					drd.setRecord(state, true);
				}
			}
			if (editor instanceof IDisplayData) // && dataProviderID != null)
			{
				try
				{
					Object data = dal.getValueObject(currentEditingState, dataProviderID);
					if (data instanceof DbIdentValue)
					{
						data = ((DbIdentValue)data).getPkValue();
					}
					convertAndSetValue(((IDisplayData)editor), data);
				}
				catch (IllegalArgumentException iae)
				{
					Debug.error(iae);
				}
			}
			if (editor instanceof IServoyAwareBean)
			{
				((IServoyAwareBean)editor).setSelectedRecord(new ServoyBeanState(currentEditingState, dal.getFormScope()));
			}
			if (editor instanceof IScriptableProvider && !(editor instanceof IDisplayData) && !(editor instanceof IDisplayRelatedData))
			{
				IScriptable scriptable = ((IScriptableProvider)editor).getScriptObject();
				if (scriptable instanceof ISupportOnRenderCallback)
				{
					RenderEventExecutor renderEventExecutor = ((ISupportOnRenderCallback)scriptable).getRenderEventExecutor();
					if (renderEventExecutor != null && renderEventExecutor.hasRenderCallback())
					{
						renderEventExecutor.fireOnRender(editor instanceof JTextComponent ? ((JTextComponent)editor).isEditable() : false);
					}
				}
			}
		}
		return editor.isVisible() ? editor : empty;
	}

	private ISwingFoundSet tableViewFoundset = null;

	/*
	 * @see TableCellRenderer#getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
	 */
	public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected, boolean hasFocus, final int row, final int column)
	{
		if (renderer == null || !isVisible(renderer) || !(jtable.getModel() instanceof IFoundSetInternal))
		{
			return empty;
		}

		final ISwingFoundSet foundset = (ISwingFoundSet)jtable.getModel();
		if (foundset != tableViewFoundset)
		{
			// foundset changed
			this.tableViewFoundset = foundset;
			rowAndDataprovider.clear();
		}
		final IRecordInternal state;
		try
		{
			state = foundset.getRecord(row);
		}
		catch (RuntimeException re)
		{
			Debug.error("Error getting row ", re); //$NON-NLS-1$
			return empty;
		}

		RenderEventExecutor renderEventExecutor = null;
		IScriptRenderMethods renderable = null;
		if (renderer instanceof IScriptableProvider)
		{
			IScriptable scriptable = ((IScriptableProvider)renderer).getScriptObject();
			if (scriptable instanceof ISupportOnRenderCallback)
			{
				renderEventExecutor = ((ISupportOnRenderCallback)scriptable).getRenderEventExecutor();
				renderable = ((ISupportOnRenderCallback)scriptable).getRenderable();
			}
		}

		Color bgColor = getBgColor(jtable, isSelected, row, false);
		if (bgColor != null && renderer instanceof JComponent) ((JComponent)renderer).setOpaque(true);
		Color fgColor = getFgColor(jtable, isSelected, row);
		Font font = getFont(jtable, isSelected, row);

		// set the sizes of the to render component also in the editor if the editor is not used.
		// so that getLocation and getWidth in scripting on tableviews do work.
		if (editor != null && editor.getParent() == null)
		{
			Rectangle cellRect = jtable.getCellRect(row, column, false);
			editor.setLocation(cellRect.x, cellRect.y);
			editor.setSize(cellRect.width, cellRect.height);
		}

		boolean isRenderWithOnRender = renderEventExecutor != null && renderEventExecutor.hasRenderCallback() && renderable instanceof RenderableWrapper;
		Color renderBgColor = null;
		if (isSelected)
		{
			if (!isRenderWithOnRender || renderEventExecutor.isDifferentRenderState(state, row, isSelected))
			{
				Color tableSelectionColor = jtable.getSelectionForeground();
				if (bgColor != null)
				{
					tableSelectionColor = adjustColorDifference(bgColor, tableSelectionColor);
				}

				((RenderableWrapper)renderable).clearProperty(RenderableWrapper.PROPERTY_FGCOLOR);
				renderer.setForeground(fgColor != null ? fgColor : tableSelectionColor);
				((RenderableWrapper)renderable).clearProperty(RenderableWrapper.PROPERTY_BGCOLOR);
				renderBgColor = (bgColor != null ? bgColor : jtable.getSelectionBackground());
				renderer.setBackground(renderBgColor);

				if (font != null)
				{
					((RenderableWrapper)renderable).clearProperty(RenderableWrapper.PROPERTY_FONT);
					renderer.setFont(font);
				}
			}
			else if (isRenderWithOnRender && foundset.getSize() == 1)
			{
				// if the foundset contains a single record, we need to force trigger onRender
				// because renderEventExecutor refers already to the changed render state
				renderEventExecutor.setRenderStateChanged();
			}
		}
		else
		{
			if (isRenderWithOnRender)
			{
				if (renderEventExecutor.isDifferentRenderState(state, row, isSelected))
				{
					Color newBGColor = bgColor != null ? bgColor : componentBgColor;
					if (newBGColor != null)
					{
						((RenderableWrapper)renderable).clearProperty(RenderableWrapper.PROPERTY_BGCOLOR);
						renderBgColor = newBGColor;
						renderer.setBackground(renderBgColor);
					}
					Color newFGColor = fgColor != null ? fgColor : componentFgColor;
					if (newFGColor != null)
					{
						((RenderableWrapper)renderable).clearProperty(RenderableWrapper.PROPERTY_FGCOLOR);
						renderer.setForeground(newFGColor);
					}
					else if (newBGColor != null)
					{
						renderer.setForeground(adjustColorDifference(newBGColor, jtable.getForeground()));
					}
					Font newFont = font != null ? font : componentFont;
					if (newFont != null)
					{
						((RenderableWrapper)renderable).clearProperty(RenderableWrapper.PROPERTY_FONT);
						renderer.setFont(newFont);
					}
				}
			}
			else
			{
				// now get the editors background. if we don't do that then scripting doesn't show up
				Color background = editor.getBackground();
				if (background != null && !background.equals(lastEditorBgColor))
				{
					unselectedBackground = background;
					lastEditorBgColor = background;
				}
				Font editorFont = editor.getFont();
				if (editorFont != null && !editorFont.equals(lastEditorFont))
				{
					unselectedFont = editorFont;
				}

				if (editor instanceof IDisplayData && ((IDisplayData)editor).isValueValid() || !(editor instanceof IDisplayData))
				{
					Color foreground = editor.getForeground();
					if (foreground != null && !foreground.equals(lastEditorFgColor))
					{
						unselectedForeground = foreground;
					}

					Color currentForeground = (fgColor != null ? fgColor : (unselectedForeground != null) ? unselectedForeground : jtable.getForeground());
					renderer.setForeground(currentForeground);
				}

				Color currentColor = (bgColor != null ? bgColor : (unselectedBackground != null) ? unselectedBackground : jtable.getBackground());
				renderer.setBackground(currentColor);

				Font currentFont = (font != null ? font : (unselectedFont != null) ? unselectedFont : jtable.getFont());
				renderer.setFont(currentFont);
			}
		}

		if (renderer instanceof JComponent)
		{
			applyRowBorder((JComponent)renderer, jtable, isSelected, row, hasFocus);
		}

		boolean printing = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")); //$NON-NLS-1$

		if (renderEventExecutor != null && renderEventExecutor.hasRenderCallback())
		{
			renderEventExecutor.setRenderState(state, row, isSelected, true);
		}

		if (renderer instanceof IDisplayRelatedData)
		{
			IDisplayRelatedData drd = (IDisplayRelatedData)renderer;
			String relationName = drd.getSelectedRelationName();
			if (state != null)
			{
				if (relationName != null)
				{
					if (!printing && !state.isRelatedFoundSetLoaded(relationName, null))
					{
						IApplication app = dal.getApplication();
						((IDisplayData)renderer).setValueObject(null);
						String key = row + "_" + relationName + "_" + null; //$NON-NLS-1$ //$NON-NLS-2$
						if (!rowAndDataprovider.contains(key))
						{
							rowAndDataprovider.add(key);
							Runnable r = new ASynchonizedCellLoad(app, jtable, foundset, row, jtable.convertColumnIndexToModel(column), relationName,
								drd.getDefaultSort(), null);
							app.getScheduledExecutor().execute(r);
						}
						return renderer.isVisible() ? renderer : empty;
					}
				}
				drd.setRecord(state, true);
			}
		}
		if (renderer instanceof IDisplayData)
		{
			if (state != null)
			{
				Object data = null;
				if (dataProviderID != null)
				{
					int index = -1;
					if (!printing && (index = dataProviderID.indexOf('.')) > 0)
					{
						if (!ScopesUtils.isVariableScope(dataProviderID))
						{
							String partName = dataProviderID.substring(0, index);
							final String restName = dataProviderID.substring(index + 1);
							String relationName = partName;
							if (relationName != null && !state.isRelatedFoundSetLoaded(relationName, restName))
							{
								IApplication app = dal.getApplication();
								((IDisplayData)renderer).setValueObject(null);
								String key = row + "_" + relationName + "_" + restName; //$NON-NLS-1$ //$NON-NLS-2$
								if (!rowAndDataprovider.contains(key))
								{
									rowAndDataprovider.add(key);

									List<SortColumn> defaultPKSortColumns = null;
									try
									{
										defaultPKSortColumns = app.getFoundSetManager().getDefaultPKSortColumns(
											app.getFlattenedSolution().getRelation(relationName).getForeignDataSource());
									}
									catch (ServoyException e)
									{
										Debug.error(e);
									}
									Runnable r = new ASynchonizedCellLoad(app, jtable, foundset, row, jtable.convertColumnIndexToModel(column), relationName,
										defaultPKSortColumns, restName);
									app.getScheduledExecutor().execute(r);
								}
								return renderer.isVisible() ? renderer : empty;
							}
							IFoundSetInternal rfs = state.getRelatedFoundSet(relationName);
							if (rfs != null)
							{
								int selected = rfs.getSelectedIndex();
								// in printing selected row will be -1, but aggregates
								// should still go through record 0
								if (selected == -1 && rfs.getSize() > 0)
								{
									selected = 0;
								}
								final IRecordInternal relState = rfs.getRecord(selected);
								if (testCalc(restName, relState, row, jtable.convertColumnIndexToModel(column), foundset)) return renderer;
							}
						}
					}
					if (!((IDisplayData)renderer).needEntireState() && !printing &&
						testCalc(dataProviderID, state, row, jtable.convertColumnIndexToModel(column), foundset))
					{
						return renderer;
					}
					try
					{
						data = dal.getValueObject(state, dataProviderID);
					}
					catch (IllegalArgumentException iae)
					{
						Debug.error(iae);
						data = "<conversion error>"; //$NON-NLS-1$
					}
				}
				((IDisplayData)renderer).setTagResolver(new ITagResolver()
				{
					public String getStringValue(String nm)
					{
						return TagResolver.formatObject(dal.getValueObject(state, nm), application.getLocale(), dal.getApplication().getSettings());
					}
				});
				if (data instanceof DbIdentValue)
				{
					data = ((DbIdentValue)data).getPkValue();
				}
				convertAndSetValue(((IDisplayData)renderer), data);
			}
		}
		if (renderer instanceof IServoyAwareBean && state != null)
		{
			((IServoyAwareBean)renderer).setSelectedRecord(new ServoyBeanState(state, dal.getFormScope()));
		}

		if (!(renderer instanceof IDisplayData) && !(renderer instanceof IDisplayRelatedData) && renderEventExecutor != null &&
			renderEventExecutor.hasRenderCallback())
		{
			renderEventExecutor.fireOnRender(false);
		}

		// when enabled state is changed during onRender, the bgcolor is cleared; make sure we have that set again
		// if the bgcolor is not changed during onRender
		if (isRenderWithOnRender && renderBgColor != null && ((RenderableWrapper)renderable).getProperty(RenderableWrapper.PROPERTY_BGCOLOR) == null)
		{
			renderer.setBackground(renderBgColor);
		}

		return renderer.isVisible() ? renderer : empty;
	}

	/**
	 * Ensures that the background color difference  in background color and foreground color is at least "half" of the color range
	 * @param bgColor
	 * @param foregroundColor
	 * @return
	 */
	private Color adjustColorDifference(Color bgColor, Color foregroundColor)
	{
		int red = Math.abs(foregroundColor.getRed() - bgColor.getRed());
		int blue = Math.abs(foregroundColor.getBlue() - bgColor.getBlue());
		int green = Math.abs(foregroundColor.getGreen() - bgColor.getBlue());

		if (red < 128 && blue < 128 && green < 128)
		{
			red = Math.abs(foregroundColor.getRed() - 255);
			blue = Math.abs(foregroundColor.getBlue() - 255);
			green = Math.abs(foregroundColor.getGreen() - 255);
			return Internalize.intern(new Color(red, blue, green));
		}
		return foregroundColor;
	}

	private Object getStyleAttributeForRow(JTable jtable, boolean isSelected, int row, ISupportRowStyling.ATTRIBUTE rowStyleAttribute)
	{
		Object rowStyleAttrValue = null;
		IRecordInternal state = ((IFoundSetInternal)jtable.getModel()).getRecord(row);
		boolean specialStateCase = (state instanceof PrototypeState || state instanceof FindState);
		if (/* !(renderer instanceof JButton) && */!specialStateCase)
		{
			if (jtable instanceof ISupportRowStyling)
			{
				ISupportRowStyling oddEvenStyling = (ISupportRowStyling)jtable;

				IStyleSheet ss = oddEvenStyling.getRowStyleSheet();
				IStyleRule style = isSelected ? oddEvenStyling.getRowSelectedStyle() : null;
				if (style != null && style.getAttributeCount() == 0) style = null;
				if (style == null)
				{
					style = (row % 2 == 0) ? oddEvenStyling.getRowOddStyle() : oddEvenStyling.getRowEvenStyle(); // because index = 0 means record = 1
				}

				rowStyleAttrValue = getStyleAttribute(ss, style, rowStyleAttribute);
			}
		}

		return rowStyleAttrValue;
	}

	private Object getStyleAttribute(IStyleSheet styleSheet, IStyleRule style, ISupportRowStyling.ATTRIBUTE styleAttribute)
	{
		if (styleSheet != null && style != null)
		{
			switch (styleAttribute)
			{
				case BGCOLOR :
					return style.getValue(CSS.Attribute.BACKGROUND_COLOR.toString()) != null ? styleSheet.getBackground(style) : null;
				case FGCOLOR :
					return style.getValue(CSS.Attribute.COLOR.toString()) != null ? styleSheet.getForeground(style) : null;
				case FONT :
					return styleSheet.hasFont(style) ? styleSheet.getFont(style) : null;
				case BORDER :
					return styleSheet.hasBorder(style) ? styleSheet.getBorder(style) : null;

			}
		}

		return null;
	}

	public Color getHeaderBgColor(ISupportRowStyling rowStyling)
	{
		return (Color)getStyleAttribute(rowStyling.getRowStyleSheet(), rowStyling.getHeaderStyle(), ISupportRowStyling.ATTRIBUTE.BGCOLOR);
	}

	public Color getHeaderFgColor(ISupportRowStyling rowStyling)
	{
		return (Color)getStyleAttribute(rowStyling.getRowStyleSheet(), rowStyling.getHeaderStyle(), ISupportRowStyling.ATTRIBUTE.FGCOLOR);
	}

	public Font getHeaderFont(ISupportRowStyling rowStyling)
	{
		return (Font)getStyleAttribute(rowStyling.getRowStyleSheet(), rowStyling.getHeaderStyle(), ISupportRowStyling.ATTRIBUTE.FONT);
	}

	public Border getHeaderBorder(ISupportRowStyling rowStyling)
	{
		return (Border)getStyleAttribute(rowStyling.getRowStyleSheet(), rowStyling.getHeaderStyle(), ISupportRowStyling.ATTRIBUTE.BORDER);
	}

	private Color getFgColor(JTable jtable, boolean isSelected, int row)
	{
		return (Color)getStyleAttributeForRow(jtable, isSelected, row, ISupportRowStyling.ATTRIBUTE.FGCOLOR);
	}

	private Font getFont(JTable jtable, boolean isSelected, int row)
	{
		return (Font)getStyleAttributeForRow(jtable, isSelected, row, ISupportRowStyling.ATTRIBUTE.FONT);
	}

	private Border getBorder(JTable jtable, boolean isSelected, int row)
	{
		return (Border)getStyleAttributeForRow(jtable, isSelected, row, ISupportRowStyling.ATTRIBUTE.BORDER);
	}

	private Color getBgColor(JTable jtable, boolean isSelected, int row, boolean isEdited)
	{
		Color bgColor = null;
		IRecordInternal state = ((IFoundSetInternal)jtable.getModel()).getRecord(row);
		boolean specialStateCase = (state instanceof PrototypeState || state instanceof FindState || state == null || state.getRawData() == null);
		if (/* !(renderer instanceof JButton) && */!specialStateCase)
		{
			ISwingFoundSet foundset = (ISwingFoundSet)jtable.getModel();
			bgColor = (Color)getStyleAttributeForRow(jtable, isSelected, row, ISupportRowStyling.ATTRIBUTE.BGCOLOR);

			String strRowBGColorProvider = null;
			List<Object> rowBGColorArgs = null;

			if (jtable instanceof IView)
			{
				strRowBGColorProvider = ((IView)jtable).getRowBGColorScript();
				rowBGColorArgs = ((IView)jtable).getRowBGColorArgs();
			}
			if (strRowBGColorProvider == null) strRowBGColorProvider = "servoy_row_bgcolor"; //$NON-NLS-1$

			boolean isRowBGColorCalculation = state.getRawData().containsCalculation(strRowBGColorProvider);
			if (!isRowBGColorCalculation && strRowBGColorProvider.equals("servoy_row_bgcolor")) //$NON-NLS-1$
			{
				strRowBGColorProvider = ""; //$NON-NLS-1$
			}
			if (strRowBGColorProvider != null && !"".equals(strRowBGColorProvider)) //$NON-NLS-1$
			{
				Object bg_color = null;
				// TODO this should be done better....
				Record.VALIDATE_CALCS.set(Boolean.FALSE);
				try
				{
					String type = (editor instanceof IScriptableProvider && ((IScriptableProvider)editor).getScriptObject() instanceof IRuntimeComponent)
						? ((IRuntimeComponent)((IScriptableProvider)editor).getScriptObject()).getElementType() : null;
					String nm = (editor instanceof IDisplayData) ? ((IDisplayData)editor).getDataProviderID() : null;
					if (isRowBGColorCalculation)
					{
						bg_color = foundset.getCalculationValue(
							state,
							strRowBGColorProvider,
							Utils.arrayMerge((new Object[] { new Integer(row), new Boolean(isSelected), type, nm, new Boolean(isEdited) }),
								Utils.parseJSExpressions(rowBGColorArgs)), null);
					}
					else
					{
						try
						{
							FormController currentForm = dal.getFormController();
							bg_color = currentForm.executeFunction(strRowBGColorProvider, Utils.arrayMerge((new Object[] { new Integer(row), new Boolean(
								isSelected), type, nm, currentForm.getName(), state, new Boolean(isEdited) }), Utils.parseJSExpressions(rowBGColorArgs)),
								false, null, true, null);
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
				}
				finally
				{
					Record.VALIDATE_CALCS.set(null);
				}

				if (bg_color != null && !(bg_color.toString().trim().length() == 0) && !(bg_color instanceof Undefined))
				{
					bgColor = PersistHelper.createColor(bg_color.toString());
				}
			}
		}
		return bgColor;
	}

	private void applyRowBorder(JComponent component, JTable jtable, boolean isSelected, int row, boolean hasFocus)
	{
		Border styleBorder = getBorder(jtable, isSelected, row);
		if (styleBorder != null)
		{
			styleBorder = new ReducedBorder(styleBorder, 0);

			if (component instanceof AbstractButton && !((AbstractButton)component).isBorderPainted())
			{
				((AbstractButton)component).setBorderPainted(true);
			}

		}
		else
		{
			styleBorder = noFocusBorder;
		}

		Border marginBorder = null;
		if (noFocusBorder instanceof EmptyBorder)
		{
			marginBorder = noFocusBorder;
		}
		else if (noFocusBorder instanceof CompoundBorder && ((CompoundBorder)noFocusBorder).getInsideBorder() instanceof EmptyBorder)
		{
			marginBorder = ((CompoundBorder)noFocusBorder).getInsideBorder();
		}

		Border adjustedBorder = null;

		if (!hasFocus)
		{
			if (styleBorder instanceof ReducedBorder && marginBorder != null)
			{
				styleBorder = new CompoundBorder(styleBorder, marginBorder);
			}
			adjustedBorder = styleBorder;
		}
		else
		{
			adjustedBorder = UIManager.getBorder("Table.focusCellHighlightBorder"); //$NON-NLS-1$
			if (styleBorder != null)
			{
				if (styleBorder instanceof ReducedBorder)
				{
					if (marginBorder != null)
					{
						adjustedBorder = new CompoundBorder(styleBorder, new CompoundBorder(adjustedBorder, marginBorder));
					}
					else
					{
						adjustedBorder = new CompoundBorder(styleBorder, adjustedBorder);
					}
				}
				else if (styleBorder instanceof CompoundBorder)
				{
					Border insideBorder = ((CompoundBorder)styleBorder).getInsideBorder();
					Border outsideBorder = ((CompoundBorder)styleBorder).getOutsideBorder();
					if (outsideBorder instanceof SpecialMatteBorder) adjustedBorder = new CompoundBorder(adjustedBorder, styleBorder);
					else adjustedBorder = new CompoundBorder(adjustedBorder, insideBorder);
				}
				else if (styleBorder instanceof SpecialMatteBorder)
				{
					adjustedBorder = new CompoundBorder(adjustedBorder, styleBorder);
				}
				else
				{
					// keep the renderer content at the same position,
					// create a focus border with the same border insets
					Insets noFocusBorderInsets = styleBorder.getBorderInsets(component);
					Insets adjustedBorderInsets = adjustedBorder.getBorderInsets(component);
					EmptyBorder emptyInsideBorder = new EmptyBorder(Math.max(0, noFocusBorderInsets.top - adjustedBorderInsets.top), Math.max(0,
						noFocusBorderInsets.left - adjustedBorderInsets.left), Math.max(0, noFocusBorderInsets.bottom - adjustedBorderInsets.bottom), Math.max(
						0, noFocusBorderInsets.right - adjustedBorderInsets.right));

					adjustedBorder = new CompoundBorder(adjustedBorder, emptyInsideBorder);
				}
			}
		}

		component.setBorder(adjustedBorder);
	}

	private boolean testCalc(final String possibleCalcDataprovider, final IRecordInternal state, final int row, final int column, final ISwingFoundSet foundset)
	{
		if (state != null && !(state instanceof PrototypeState || state instanceof FindState) &&
			state.getRawData().containsCalculation(possibleCalcDataprovider) && state.getRawData().mustRecalculate(possibleCalcDataprovider, true))
		{
			IApplication app = dal.getApplication();
			convertAndSetValue(((IDisplayData)renderer), state.getRawData().getValue(possibleCalcDataprovider));

			final String key = row + "_" + possibleCalcDataprovider; //$NON-NLS-1$
			if (!rowAndDataprovider.contains(key))
			{
				rowAndDataprovider.add(key);
				app.getScheduledExecutor().execute(new Runnable()
				{

					public void run()
					{
						state.getValue(possibleCalcDataprovider);
						application.invokeLater(new Runnable()
						{
							public void run()
							{
								rowAndDataprovider.remove(key);
								foundset.fireTableModelEvent(row, row, column, TableModelEvent.UPDATE);
								Container parent = table.getParent();
								while (parent != null && !(parent instanceof ListView))
								{
									parent = parent.getParent();
								}
								if (parent instanceof ListView)
								{
									((ListView)parent).repaint();
								}
							}
						});
					}
				});
			}
			return true;
		}
		return false;
	}

	@Override
	public Object getHeaderValue()
	{
		Object value = super.getHeaderValue();
		return Text.processTags((String)value, dal);
	}

	class ASynchonizedCellLoad implements Runnable
	{
		private final IApplication app;
		private final ISwingFoundSet foundset;
		private final int row;
		private final int column;
		private final String relationName;
		private final List<SortColumn> sort;
		private final String restName;
		private final JTable jtable;

		ASynchonizedCellLoad(IApplication app, JTable jtable, ISwingFoundSet foundset, int row, int column, String relationName, List<SortColumn> sort,
			String restName)
		{
			this.app = app;
			this.foundset = foundset;
			this.row = row;
			this.column = column;
			this.relationName = relationName;
			this.sort = sort;

			this.restName = restName;

			this.jtable = jtable;
		}

		public void run()
		{
			try
			{
				final IRecordInternal state = foundset.getRecord(row);
				if (state != null)
				{
					// only retrieve
					if (!((J2DBClient)app).isConnected())
					{
						if (Debug.tracing())
						{
							Debug.trace("Client not connected, rescheduling it with a timeout of 5 seconds, clientid: " + app.getClientID()); //$NON-NLS-1$
						}
						app.getScheduledExecutor().schedule(this, 5, TimeUnit.SECONDS);
					}
					else
					{
						IFoundSetInternal fs = state.getRelatedFoundSet(relationName, sort);
						// this triggers an update of related foundset if mustQueryForUpdates is true
						// needed when foundset is not touched but still needs to be up to date
						if (fs != null) fs.getSize();
						if (fs == null && !((J2DBClient)app).isConnected())
						{
							if (Debug.tracing())
							{
								Debug.trace("Client got disconnected, rescheduling it with a timeout of 5 seconds, clientid: " + app.getClientID()); //$NON-NLS-1$
							}
							app.getScheduledExecutor().schedule(this, 5, TimeUnit.SECONDS);
						}
						else
						{
							if (fs != null && restName != null)
							{
								fs.getDataProviderValue(restName);// only do lookup for aggregate
							}
							app.invokeLater(new Runnable()
							{
								public void run()
								{
									rowAndDataprovider.remove(row + "_" + relationName + "_" + restName); //$NON-NLS-1$ //$NON-NLS-2$
									foundset.fireTableModelEvent(row, row, column, TableModelEvent.UPDATE);
									Container parent = jtable.getParent();
									while (parent != null && !(parent instanceof ListView))
									{
										parent = parent.getParent();
									}
									if (parent instanceof ListView)
									{
										((ListView)parent).repaint();
									}
								}
							});
						}
					}
				}
			}
			catch (RuntimeException re)
			{
				if (Debug.tracing())
				{
					Debug.trace("Exception in asyn load, rescheduling it with a timeout of 5 seconds, clientid: " + app.getClientID()); //$NON-NLS-1$
				}
				app.getScheduledExecutor().schedule(this, 5, TimeUnit.SECONDS);
			}
		}
	}

	/*
	 * @see IDataAdapter#setState(State)
	 */
	public void setRecord(IRecordInternal state)
	{
		// this is called but never do anything with this value, the renderer and
		// editer have to lookup there values them selfs
	}

	/*
	 * @see IDataAdapter#getDataProviderID()
	 */
	public String getDataProviderID()
	{
		return dataProviderID;
	}

	/*
	 * _____________________________________________________________ DataListener
	 */
	private final List<IDataAdapter> listeners = new ArrayList<IDataAdapter>();

	public void addDataListener(IDataAdapter l)
	{
		if (!listeners.contains(l) && l != this) listeners.add(l);
	}

	public void removeDataListener(IDataAdapter dataListener)
	{
		listeners.remove(dataListener);
	}

	private void fireModificationEvent(IRecord record)
	{
		// Also notify the table about changes, there seems no other way to do this...
		TableModel parent = table.getModel();
		if (parent instanceof ISwingFoundSet)
		{
			int index = ((ISwingFoundSet)parent).getRecordIndex(record);
			if (index != -1) ((ISwingFoundSet)parent).fireTableModelEvent(index, index, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
		}
	}

	private boolean gettingEditorValue = false;

	public Object getCellEditorValue()
	{
		// test if currentEditing state isn't deleted already
		if (currentEditingState == null || dataProviderID == null || (currentEditingState != null && currentEditingState.getParentFoundSet() == null)) return null;

		Object comp = editor;
		if ((comp instanceof IDisplay && ((IDisplay)comp).isReadOnly()) || gettingEditorValue)
		{
			return currentEditingState.getValue(getDataProviderID());
		}
		try
		{
			gettingEditorValue = true;
			if (comp instanceof IDelegate< ? >)
			{
				comp = ((IDelegate< ? >)comp).getDelegate();
			}

			//HACK:needed for commit value copied from other hack 'processfocus' in DataField
			if (comp instanceof DataField && ((DataField)comp).isEditable())
			{
				DataField edit = (DataField)comp;
				boolean needEntireState = edit.needEntireState();
				try
				{
					edit.setNeedEntireState(false);
					int fb = edit.getFocusLostBehavior();
					if (fb == JFormattedTextField.COMMIT || fb == JFormattedTextField.COMMIT_OR_REVERT)
					{
						try
						{
							edit.commitEdit();
							// Give it a chance to reformat.
							edit.setValueObject(edit.getValue());
						}
						catch (ParseException pe)
						{
							return null;
						}
					}
					else if (fb == JFormattedTextField.REVERT)
					{
						edit.setValueObject(edit.getValue());
					}
				}
				finally
				{
					edit.setNeedEntireState(needEntireState);
				}

			}

			Object obj = null;
			if (editor instanceof IDisplayData)
			{
				IDisplayData displayData = (IDisplayData)editor;
				obj = Utils.removeJavascripLinkFromDisplay(displayData, null);

				if (!findMode)
				{
					// use UI converter to convert from UI value to record value
					obj = ComponentFormat.applyUIConverterFromObject(displayData, obj, dataProviderID, application.getFoundSetManager());
				}

				// if the editor is not enable or is readonly dont try to set any value.
				if (!displayData.isEnabled() || displayData.isReadOnly()) return obj;
				// then make sure the current state is in edit, if not, try to start it else just return.
				// this can happen when toggeling with readonly. case 233226 or 232188
				if (!currentEditingState.isEditing() && !currentEditingState.startEditing()) return obj;

				try
				{
					if (currentEditingState != null && (obj == null || "".equals(obj)) && currentEditingState.getValue(dataProviderID) == null) //$NON-NLS-1$
					{
						return null;
					}
				}
				catch (IllegalArgumentException iae)
				{
					Debug.error(iae);
				}
				Object oldVal = null;
				if (currentEditingState instanceof FindState)
				{
					if (displayData instanceof IScriptableProvider && ((IScriptableProvider)displayData).getScriptObject() instanceof IFormatScriptComponent &&
						((IFormatScriptComponent)((IScriptableProvider)displayData).getScriptObject()).getComponentFormat() != null)
					{
						((FindState)currentEditingState).setFormat(dataProviderID,
							((IFormatScriptComponent)((IScriptableProvider)displayData).getScriptObject()).getComponentFormat().parsedFormat);
					}
					try
					{
						oldVal = currentEditingState.getValue(dataProviderID);
					}
					catch (IllegalArgumentException iae)
					{
						Debug.error("Error getting the previous value", iae); //$NON-NLS-1$
						oldVal = null;
					}
					currentEditingState.setValue(dataProviderID, obj);
					if (!Utils.equalObjects(oldVal, obj))
					{
						// call notifyLastNewValue changed so that the onChangeEvent will be fired and called when attached.
						displayData.notifyLastNewValueWasChange(oldVal, obj);
						obj = dal.getValueObject(currentEditingState, dataProviderID);
					}
				}
				else
				{
					if (!displayData.isValueValid() && Utils.equalObjects(lastInvalidValue, obj))
					{
						// already validated
						return obj;
					}
					try
					{
						adjusting = true;
						try
						{
							oldVal = currentEditingState.getValue(dataProviderID);
						}
						catch (IllegalArgumentException iae)
						{
							Debug.error("Error getting the previous value", iae); //$NON-NLS-1$
						}
						try
						{
							if (oldVal == Scriptable.NOT_FOUND && dal.getFormScope().has(dataProviderID, dal.getFormScope()))
							{
								oldVal = dal.getFormScope().get(dataProviderID);
								dal.getFormScope().put(dataProviderID, obj);
								IFoundSetInternal foundset = currentEditingState.getParentFoundSet();
								if (foundset instanceof FoundSet) ((FoundSet)foundset).fireFoundSetChanged();
							}
							else currentEditingState.setValue(dataProviderID, obj);
						}
						catch (IllegalArgumentException e)
						{
							Debug.trace(e);
							displayData.setValueValid(false, oldVal);
							application.handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, e));

							Object stateValue = null;
							try
							{
								stateValue = dal.getValueObject(currentEditingState, dataProviderID);
							}
							catch (IllegalArgumentException iae)
							{
								Debug.error(iae);
							}
							Object displayValue;
							if (Utils.equalObjects(oldVal, stateValue))
							{
								// reset display to typed value
								displayValue = obj;
							}
							else
							{
								// reset display to changed value in validator method
								displayValue = stateValue;
							}
							convertAndSetValue(displayData, displayValue);
							return displayValue;
						}

						if (!Utils.equalObjects(oldVal, obj))
						{
							fireModificationEvent(currentEditingState);
							displayData.notifyLastNewValueWasChange(oldVal, obj);
							obj = dal.getValueObject(currentEditingState, dataProviderID);
							convertAndSetValue(displayData, obj);// we also want to reset the value in the current display if changed by script
						}
						else if (!displayData.isValueValid())
						{
							displayData.notifyLastNewValueWasChange(null, obj);
						}
						else
						{
							displayData.setValueValid(true, null);
						}
					}
					finally
					{
						adjusting = false;
						if (displayData.isValueValid())
						{
							lastInvalidValue = NONE;
						}
						else
						{
							lastInvalidValue = obj;
						}
					}
				}

			}
			return obj;
		}
		finally
		{
			gettingEditorValue = false;
		}
	}

	/**
	 * @param obj
	 * @param displayData
	 */
	public void convertAndSetValue(IDisplayData displayData, Object obj)
	{
		if (!findMode)
		{
			// use UI converter to convert from UI value to record value
			displayData.setValueObject(ComponentFormat.applyUIConverterToObject(displayData, obj, dataProviderID, application.getFoundSetManager()));
		}
		else
		{
			displayData.setValueObject(obj);
		}
	}

	/*
	 * @see CellEditor#isCellEditable(EventObject)
	 */
	public boolean isCellEditable(EventObject anEvent)
	{
		if (editor.isEnabled() || hasOnRender(editor))
		{
			// if we enable this the onAction is not fired and it is not possible
			// to copy text from non editable field
			// if (editor instanceof JTextComponent)
			// {
			// return ((JTextComponent)editor).isEditable();
			// }
			return true;
		}
		return false;
	}

	private boolean hasOnRender(Component c)
	{
		if (c instanceof IScriptableProvider)
		{
			IScriptable scriptable = ((IScriptableProvider)c).getScriptObject();
			if (scriptable instanceof ISupportOnRenderCallback)
			{
				RenderEventExecutor renderEventExecutor = ((ISupportOnRenderCallback)scriptable).getRenderEventExecutor();
				return renderEventExecutor != null && renderEventExecutor.hasRenderCallback();
			}
		}
		return false;
	}

	/*
	 * @see CellEditor#shouldSelectCell(EventObject)
	 */
	public boolean shouldSelectCell(EventObject anEvent)
	{
		return true;
	}

	private boolean isStopping = false;// we are not completly following the swing model which can couse repetive calls by jtable due to fire of tableContentChange

	public boolean stopCellEditing()
	{
		if (!isStopping)
		{
			try
			{
				isStopping = true;

				// Get the current celleditor value for testing notify changed.
				getCellEditorValue();

				// if the notify changed failed the mustTestLastValue is set and we shouldn't allow stopping:
				if (editor instanceof IDisplayData && !((IDisplayData)editor).isValueValid())
				{
					return false;
				}
				CellEditorListener l = listener;
				if (l != null)
				{
					// TODO this also triggers (or can trigger) a getCellEditorValue() call.. (so the vallue is set in the record, conversion/validation is called again)
					l.editingStopped(new ChangeEvent(this));
				}
				return true;
			}
			finally
			{
				isStopping = false;
			}
		}
		else
		{
			return true;
		}
	}

	/*
	 * @see CellEditor#cancelCellEditing()
	 */
	public void cancelCellEditing()
	{
		if (listener != null) listener.editingCanceled(new ChangeEvent(this));
	}

	private CellEditorListener listener = null; // allow only one

	/*
	 * @see CellEditor#addCellEditorListener(CellEditorListener)
	 */
	public void addCellEditorListener(CellEditorListener l)
	{
		listener = l;
	}

	/*
	 * @see CellEditor#removeCellEditorListener(CellEditorListener)
	 */
	public void removeCellEditorListener(CellEditorListener l)
	{
		listener = null;
	}

	public void displayValueChanged(ModificationEvent event)
	{
		valueChanged(event);
	}

	/*
	 * @see JSModificationListener#valueChanged(ModificationEvent)
	 */
	public void valueChanged(ModificationEvent e)
	{
		if (adjusting) return;

		try
		{
			adjusting = true;
			// ignore globals in a cell adapter, will be handled by the row manager
			if (!table.isEditing() && ScopesUtils.isVariableScope(e.getName()))
			{
				// test if it is a related
				if (dataProviderID != null && dataProviderID.indexOf('.') != -1)
				{
					TableModel parent = table.getModel();
					if (parent instanceof ISwingFoundSet)
					{
						// it could be based on that global so fire a table event.
						((ISwingFoundSet)parent).fireTableModelEvent(0, parent.getRowCount() - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
					}
				}
				return;
			}

			// refresh value
			IRecord s = e.getRecord();
			if (s == null)
			{
				TableModel tm = table.getModel();
				if (tm instanceof IFoundSetInternal)
				{
					IFoundSetInternal fs = (IFoundSetInternal)tm;
					int selRow = fs.getSelectedIndex();
					if (selRow != -1)
					{
						s = fs.getRecord(selRow);
					}
				}
			}
			if (s != null)
			{
				Object obj = e.getValue();
				if (e.getName().equals(dataProviderID))
				{
					fireModificationEvent(s);// make sure the change is seen and pushed to display by jtable
				}
				else
				{
					obj = dal.getValueObject(s, dataProviderID);
					if (obj == Scriptable.NOT_FOUND)
					{
						obj = null;
					}
				}
				if (s == currentEditingState && table.getEditorComponent() == editor && editor instanceof IDisplayData)
				{
					convertAndSetValue(((IDisplayData)editor), obj);
				}
			}
		}
		finally
		{
			adjusting = false;
		}
	}

	@Override
	public String toString()
	{
		return "CellAdapter " + dataProviderID + ",  hash " + hashCode(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns the editor.
	 *
	 * @return Component
	 */
	public Component getEditor()
	{
		return editor;
	}

	/**
	 * Returns the renderer.
	 *
	 * @return Component
	 */
	public Component getRenderer()
	{
		return renderer;
	}

	public String getName()
	{
		return name;
	}

	private void onEditorChanged()
	{
		try
		{
			table.setLayoutChangingViaJavascript(true);
			ArrayList<CellAdapter> cellAdaptersList = new ArrayList<CellAdapter>();
			Enumeration<TableColumn> columnsEnum = table.getColumnModel().getColumns();
			TableColumn column;
			while (columnsEnum.hasMoreElements())
			{
				column = columnsEnum.nextElement();
				cellAdaptersList.add((CellAdapter)column);
			}
			for (CellAdapter ca : cellAdaptersList)
				table.getColumnModel().removeColumn(ca);

			Collections.sort(cellAdaptersList, new Comparator<CellAdapter>()
			{
				public int compare(CellAdapter o1, CellAdapter o2)
				{
					Component editor1 = o1.getEditor();
					Component editor2 = o2.getEditor();

					Point p1 = ((editor1 instanceof ISupportCachedLocationAndSize) ? ((ISupportCachedLocationAndSize)editor1).getCachedLocation()
						: editor1.getLocation());
					Point p2 = ((editor2 instanceof ISupportCachedLocationAndSize) ? ((ISupportCachedLocationAndSize)editor2).getCachedLocation()
						: editor2.getLocation());
					return PositionComparator.comparePoint(true, p1, p2);
				}
			});

			for (CellAdapter cellAdapter : cellAdaptersList)
			{
				cellAdapter.updateEditorX();
				cellAdapter.updateEditorWidth();
				cellAdapter.resetEditorSize();
				table.getColumnModel().addColumn(cellAdapter);
			}
		}
		finally
		{
			table.setLayoutChangingViaJavascript(false);
		}
	}

	public void updateEditorX()
	{
		this.editorX = (editor instanceof ISupportCachedLocationAndSize) ? ((ISupportCachedLocationAndSize)editor).getCachedLocation().x
			: editor.getLocation().x;
	}

	public void updateEditorWidth()
	{
		this.editorWidth = (editor instanceof ISupportCachedLocationAndSize) ? ((ISupportCachedLocationAndSize)editor).getCachedSize().width
			: editor.getSize().width;
	}

	private void resetEditorSize()
	{
		Dimension size;
		if (editor instanceof ISupportCachedLocationAndSize)
		{
			size = ((ISupportCachedLocationAndSize)editor).getCachedSize();
		}
		else
		{
			size = editor.getSize();
		}
		CellAdapter.this.setPreferredWidth(size != null ? size.width + table.getColumnModel().getColumnMargin() : 0);
	}

	public void itemStateChanged(ItemEvent e)
	{
		if (e.getSource() instanceof DataComboBox && e.getStateChange() == ItemEvent.DESELECTED)
		{
			return;
		}
		stopCellEditing();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (currentEditingState != null) currentEditingState.startEditing();
	}

	public void addDisplay(CellAdapter ca)
	{
		if (displays == null) displays = new ArrayList<CellAdapter>();
		displays.add(ca);
	}

	public void commitEdit(IDisplayData e)
	{
		getCellEditorValue();
	}

	public void startEdit(IDisplayData e)
	{
		// ignore
	}

	class ReducedBorder implements Border
	{
		public static final int LEFT = 2;
		public static final int RIGHT = 4;
		public static final int TOP = 8;
		public static final int BOTTOM = 16;

		private final Border sourceBorder;
		private final int hideMask;

		ReducedBorder(Border sourceBorder, int hideMask)
		{
			this.sourceBorder = sourceBorder;
			this.hideMask = hideMask;
		}

		/*
		 * @see javax.swing.border.Border#paintBorder(java.awt.Component, java.awt.Graphics, int, int, int, int)
		 */
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
		{
			Insets sourceBorderInsets = sourceBorder.getBorderInsets(c);
			Area borderClip = new Area(new Rectangle(x, y, width, height));
			if ((hideMask & LEFT) != 0)
			{
				borderClip.subtract(new Area(new Rectangle(x, y + sourceBorderInsets.top, sourceBorderInsets.left, height - sourceBorderInsets.top -
					sourceBorderInsets.bottom)));
			}
			if ((hideMask & RIGHT) != 0)
			{
				borderClip.subtract(new Area(new Rectangle(x + width - sourceBorderInsets.right, y + sourceBorderInsets.top, sourceBorderInsets.right, height -
					sourceBorderInsets.top - sourceBorderInsets.bottom)));
			}
			if ((hideMask & TOP) != 0)
			{
				borderClip.subtract(new Area(new Rectangle(x + sourceBorderInsets.left, y, width - sourceBorderInsets.left - sourceBorderInsets.right,
					sourceBorderInsets.top)));
			}
			if ((hideMask & BOTTOM) != 0)
			{
				borderClip.subtract(new Area(new Rectangle(x + sourceBorderInsets.left, y + height - sourceBorderInsets.bottom, width -
					sourceBorderInsets.left - sourceBorderInsets.right, sourceBorderInsets.bottom)));
			}
			g.setClip(borderClip);
			sourceBorder.paintBorder(c, g, x, y, width, height);
		}

		/*
		 * @see javax.swing.border.Border#getBorderInsets(java.awt.Component)
		 */
		public Insets getBorderInsets(Component c)
		{
			Insets sourceBorderInsets = sourceBorder.getBorderInsets(c);
			int left = (hideMask & LEFT) != 0 ? 0 : sourceBorderInsets.left;
			int right = (hideMask & RIGHT) != 0 ? 0 : sourceBorderInsets.right;
			int top = (hideMask & TOP) != 0 ? 0 : sourceBorderInsets.top;
			int bottom = (hideMask & BOTTOM) != 0 ? 0 : sourceBorderInsets.bottom;

			return new Insets(top, left, bottom, right);
		}

		/*
		 * @see javax.swing.border.Border#isBorderOpaque()
		 */
		public boolean isBorderOpaque()
		{
			return sourceBorder.isBorderOpaque();
		}
	}
}
