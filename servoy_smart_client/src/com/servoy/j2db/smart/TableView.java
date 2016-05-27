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
package com.servoy.j2db.smart;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.CellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormWindow;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IProvideTabSequence;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IView;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.ISupportAsyncLoading;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dnd.DRAGNDROP;
import com.servoy.j2db.dnd.FormDataTransferHandler;
import com.servoy.j2db.dnd.ISupportDragNDropTextTransfer;
import com.servoy.j2db.dnd.JSDNDEvent;
import com.servoy.j2db.gui.FixedJTable;
import com.servoy.j2db.gui.LFAwareSortableHeaderRenderer;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportPrinting;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.ISupportSize;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.ISupportText;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabSeqComparator;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.smart.dataui.CellAdapter;
import com.servoy.j2db.smart.dataui.ColumnSortListener;
import com.servoy.j2db.smart.dataui.DataCheckBox;
import com.servoy.j2db.smart.dataui.TableTabSequenceHandler;
import com.servoy.j2db.smart.dataui.VisibleBean;
import com.servoy.j2db.ui.BaseEventExecutor;
import com.servoy.j2db.ui.DataRendererOnRenderWrapper;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportEventExecutor;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.ISupportRowStyling;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.runtime.HasRuntimeReadOnly;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;

/**
 * The tableview display controller
 *
 * @author jblok
 */
public class TableView extends FixedJTable implements IView, IDataRenderer, ISupportRowStyling, IProvideTabSequence, ISupportEditStateTracking
{
	private Component[] editorComponents;
	private Component[] rendererComponents;
	private DataAdapterList dal;
	private final IDataRenderer headerCache;
	private final IDataRenderer leadingGrandSummaryCache;
	private String rowBGColorScript;
	private List<Object> rowBGColorArgs;
	private final FormController fc;
	private boolean reorderble = true;
	private boolean resizeble = true;
	private boolean sortable = true;
	private final Map<Integer, Boolean> currentSortCol = new HashMap<Integer, Boolean>();
	private int keyReleaseToBeIgnored;
	private boolean wasEditingBeforeKeyPress;
	private final AbstractBase cellview;

	private TableColumn resizingColumn;
	private TableColumn draggingColumn;
	private List<TableColumn> tableColumnsBeforeDrag;
	private final TableTabSequenceHandler tableTabSequenceHandler;
	private boolean layoutChangingByJavascript = false;

	private final ISupportOnRenderCallback dataRendererOnRenderWrapper;
	private IStyleSheet styleSheet;
	private IStyleRule oddStyle, evenStyle, selectedStyle, headerStyle;
	private final List<CellAdapter> nonViewableColumns = new ArrayList<CellAdapter>();

	public TableView(IApplication app, final FormController fc, Form formForStyles, final AbstractBase cellview, final IScriptExecuter scriptExecuter,
		IDataRenderer headerComp, IDataRenderer leadingGrandSummaryComp, boolean printing)
	{
		super(app);
		dataRendererOnRenderWrapper = new DataRendererOnRenderWrapper(this);
		this.fc = fc;
		if (cellview instanceof ISupportSize)
		{
			Dimension size;
			if (cellview instanceof Form)
			{
				Form f = (Form)cellview;
				Iterator<Part> parts = f.getParts();
				int untilBody = 0;
				int bodyHeight = 0;
				while (parts.hasNext())
				{
					Part p = parts.next();
					if (p.getPartType() == Part.BODY)
					{
						bodyHeight = p.getHeight() - untilBody;
						break;
					}
					else
					{
						untilBody += p.getHeight();
					}
				}
				size = new Dimension(f.getWidth(), bodyHeight);
			}
			else
			{
				size = ((ISupportSize)cellview).getSize();
			}
			Dimension currentSize = getPreferredScrollableViewportSize();
			// Just some heuristics trying to satisfy both parties: those that
			// want the dialog to have the exact designtime size, and those who
			// design the body part with just one row, expecting that it will fill up
			// all available space.
			if (size.height * 4 < currentSize.height) size.height = currentSize.height;
			setPreferredScrollableViewportSize(size);
		}
		IDataProviderLookup dpl = app.getFlattenedSolution().getDataproviderLookup(app.getFoundSetManager(), cellview);
		headerCache = headerComp;
		leadingGrandSummaryCache = leadingGrandSummaryComp;
		this.cellview = cellview;
		int onRenderMethodID = 0;
		AbstractBase onRenderPersist;
		if (cellview instanceof Portal)
		{
			Portal meta = (Portal)cellview;
			Dimension d = meta.getIntercellSpacing();
			if (d != null) setIntercellSpacing(d);
			setShowHorizontalLines(meta.getShowHorizontalLines());
			setShowVerticalLines(meta.getShowVerticalLines());
			if (meta.getRowHeight() > 0) setRowHeight(meta.getRowHeight());
			reorderble = meta.getReorderable();
			getTableHeader().setReorderingAllowed(reorderble);
			resizeble = meta.getResizable();
			getTableHeader().setResizingAllowed(resizeble);
			sortable = meta.getSortable();
			setRowBGColorScript(meta.getRowBGColorCalculation(), meta.getFlattenedMethodArguments("rowBGColorCalculation")); //$NON-NLS-1$

			onRenderMethodID = meta.getOnRenderMethodID();
			onRenderPersist = meta;
		}
		else
		{
			setShowHorizontalLines(false);
			setShowVerticalLines(false);
			setIntercellSpacing(new Dimension());

			onRenderMethodID = fc.getForm().getOnRenderMethodID();
			onRenderPersist = fc.getForm();
		}

		if (cellview instanceof ISupportScrollbars)
		{
			if (((ISupportScrollbars)cellview).getScrollbars() == 0)
			{
				setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			}
			if ((((ISupportScrollbars)cellview).getScrollbars() &
				ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS)
			{
				setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			}
			if ((((ISupportScrollbars)cellview).getScrollbars() &
				ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED)
			{
				setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			}
		}

		if (onRenderMethodID > 0)
		{
			dataRendererOnRenderWrapper.getRenderEventExecutor().setRenderCallback(Integer.toString(onRenderMethodID),
				Utils.parseJSExpressions(onRenderPersist.getFlattenedMethodArguments("onRenderMethodID")));
			dataRendererOnRenderWrapper.getRenderEventExecutor().setRenderScriptExecuter(fc != null ? fc.getScriptExecuter() : null);
		}
		initDragNDrop(fc, 0);

		//setRowMargin(0);
		setAutoCreateColumnsFromModel(false);//this is very important for performance!!
		setAutoscrolls(true);

		setOpaque(false);//to make fields use background if fields are transparent

		TableColumnModel tcm = getColumnModel();
		if (tcm == null)
		{
			tcm = createDefaultColumnModel();
		}
		boolean b_accessible = true;
		boolean b_viewable = true;
		int access = app.getFlattenedSolution().getSecurityAccess(cellview.getUUID());
		if (access != -1)
		{
			b_accessible = ((access & IRepository.ACCESSIBLE) != 0);
			b_viewable = ((access & IRepository.VIEWABLE) != 0);
		}

		ActionListener buttonListener = new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						if (getEditorComponent() == e.getSource())
						{
							stopUIEditing(true);
						}
					}
				});
			}
		};

		SortedList<Pair<ISupportTabSeq, TableColumn>> columnTabSequence = new SortedList<Pair<ISupportTabSeq, TableColumn>>(
			new Comparator<Pair<ISupportTabSeq, TableColumn>>()
			{
				public int compare(Pair<ISupportTabSeq, TableColumn> o1, Pair<ISupportTabSeq, TableColumn> o2)
				{
					return TabSeqComparator.INSTANCE.compare(o1.getLeft(), o2.getLeft());
				}
			});
		List<Component> editors = new ArrayList<Component>();
		List<Component> renderers = new ArrayList<Component>();
		final Map<String, IPersist> labelsFor = new HashMap<String, IPersist>();
		LinkedHashMap<String, IDataAdapter> displays = new LinkedHashMap<String, IDataAdapter>();
		ArrayList<Integer> unmovableColumns = new ArrayList<Integer>();
		try
		{
			int index = 0;
			int start = 0;
			int height = 0;
			List<SortColumn> initialSort = null;

			if (cellview instanceof Form)
			{
				Form form = (Form)cellview;
				Part body = null;
				Iterator<Part> e2 = form.getParts();
				while (e2.hasNext())
				{
					Part part = e2.next();
					if (part.getPartType() == Part.BODY)
					{
						body = part;
						break;
					}
				}
				if (body != null)
				{
					start = form.getPartStartYPos(body.getID());
					yOffset = start;
					height = body.getHeight() - start;
				}
				else
				{
					//missing body, leave both start and height 0 so no component is added to the table
				}
				if (form.getInitialSort() != null)
				{
					try
					{
						initialSort = ((FoundSetManager)app.getFoundSetManager()).getSortColumns(app.getFlattenedSolution().getTable(form.getDataSource()),
							form.getInitialSort());
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
				}
			}
			else
			{
				Portal p = (Portal)cellview;
				start = p.getLocation().y;
				height = p.getSize().height;
				if (p.getInitialSort() != null)
				{
					Relation[] relations = app.getFlattenedSolution().getRelationSequence(p.getRelationName());
					if (relations != null && relations.length > 0)
					{
						initialSort = ((FoundSetManager)app.getFoundSetManager()).getSortColumns(
							app.getFlattenedSolution().getTable(relations[relations.length - 1].getForeignDataSource()), p.getInitialSort());
					}
				}
			}


			DragStartTester dragMouseListener = null;
			if (getTransferHandler() instanceof FormDataTransferHandler)
			{
				dragMouseListener = new DragStartTester();
			}
			List<IPersist> elements = ComponentFactory.sortElementsOnPositionAndGroup(cellview.getAllObjectsAsList());
			for (IPersist obj : elements)
			{
				if (obj instanceof IFormElement)
				{
					if (obj instanceof GraphicalComponent && ((GraphicalComponent)obj).getLabelFor() != null)
					{
						labelsFor.put(((GraphicalComponent)obj).getLabelFor(), obj);
						continue;
					}

					Dimension size = ((IFormElement)obj).getSize();
					Point l = ((IFormElement)obj).getLocation();
					if (l == null)
					{
						continue;//unkown where to add
					}

					if (printing && obj instanceof ISupportPrinting)
					{
						if (!((ISupportPrinting)obj).getPrintable()) continue;
					}

					if (l.y >= start && l.y < start + height)
					{
						Component editor = (Component)ComponentFactory.createComponent(app, formForStyles, obj, dpl, scriptExecuter, false);

						if (editor instanceof IScriptableProvider)
						{
							IScriptable scriptable = ((IScriptableProvider)editor).getScriptObject();
							if (scriptable instanceof ISupportOnRenderCallback && cellview instanceof Portal &&
								((ISupportOnRenderCallback)scriptable).getRenderEventExecutor() != null)
								ComponentFactoryHelper.addPortalOnRenderCallback((Portal)cellview,
									((ISupportOnRenderCallback)scriptable).getRenderEventExecutor(), obj, fc != null ? fc.getScriptExecuter() : null);
						}

						if (dragMouseListener != null && editor instanceof JComponent)
						{
							if (editor instanceof ISupportDragNDropTextTransfer) ((ISupportDragNDropTextTransfer)editor).clearTransferHandler();
							else((JComponent)editor).setTransferHandler(null);
							editor.addMouseListener(dragMouseListener);
							editor.addMouseMotionListener(dragMouseListener);
						}

						if (editor instanceof IScriptableProvider && ((IScriptableProvider)editor).getScriptObject() instanceof IRuntimeComponent)
						{
							((IRuntimeComponent)((IScriptableProvider)editor).getScriptObject()).setLocation(l.x, l.y);
							((IRuntimeComponent)((IScriptableProvider)editor).getScriptObject()).setSize(size.width, size.height);
						}
						else
						{
							editor.setLocation(l.x, l.y);
							editor.setSize(size.width, size.height);

						}
						// Test for a visible bean, then get the real component
						if (editor instanceof VisibleBean)
						{
							editor = ((VisibleBean)editor).getDelegate();
						}

						if (editor instanceof ISupportSecuritySettings)
						{
							if (!b_accessible) ((ISupportSecuritySettings)editor).setAccessible(false);
							if (!b_viewable) ((ISupportSecuritySettings)editor).setViewable(false);
						}

						if (editor instanceof JButton)
						{
							((JButton)editor).addActionListener(buttonListener);
						}
						editors.add(editor);
						if (editor instanceof JComponent)
						{
							Border border = ((JComponent)editor).getBorder();
							Border inner = null;
							if (border instanceof CompoundBorder)
							{
								inner = ((CompoundBorder)border).getInsideBorder();
								border = ((CompoundBorder)border).getOutsideBorder();
							}
							if (border instanceof LineBorder)
							{
								if (inner == null)
								{
									((JComponent)editor).setBorder(BorderFactory.createEmptyBorder());
								}
								else
								{
									((JComponent)editor).setBorder(inner);
								}
							}
						}

						Component renderer = (Component)ComponentFactory.createComponent(app, formForStyles, obj, dpl, null, false);
						if (renderer instanceof IScriptableProvider)
						{
							IScriptable scriptable = ((IScriptableProvider)renderer).getScriptObject();
							if (scriptable instanceof ISupportOnRenderCallback && cellview instanceof Portal &&
								((ISupportOnRenderCallback)scriptable).getRenderEventExecutor() != null)
								ComponentFactoryHelper.addPortalOnRenderCallback((Portal)cellview,
									((ISupportOnRenderCallback)scriptable).getRenderEventExecutor(), obj, fc != null ? fc.getScriptExecuter() : null);
						}

						if (dragMouseListener != null && renderer instanceof JComponent)
						{
							if (renderer instanceof ISupportDragNDropTextTransfer) ((ISupportDragNDropTextTransfer)renderer).clearTransferHandler();
							else((JComponent)renderer).setTransferHandler(null);
							editor.addMouseListener(dragMouseListener);
							editor.addMouseMotionListener(dragMouseListener);
						}

						if (renderer instanceof IScriptableProvider && ((IScriptableProvider)renderer).getScriptObject() instanceof IRuntimeComponent)
						{
							((IRuntimeComponent)((IScriptableProvider)renderer).getScriptObject()).setLocation(l.x, l.y);
							((IRuntimeComponent)((IScriptableProvider)renderer).getScriptObject()).setSize(size.width, size.height);
						}
						else
						{
							renderer.setLocation(l.x, l.y);
							renderer.setSize(size.width, size.height);

						}
						renderers.add(renderer);
						if (renderer instanceof JComponent)
						{
							Border border = ((JComponent)renderer).getBorder();
							Border inner = null;
							if (border instanceof CompoundBorder)
							{
								inner = ((CompoundBorder)border).getInsideBorder();
								border = ((CompoundBorder)border).getOutsideBorder();
							}
							if (border instanceof LineBorder)
							{
								if (!getShowHorizontalLines())
								{
									setShowHorizontalLines(true);
									setShowVerticalLines(true);
									setGridColor(((LineBorder)border).getLineColor());
									setIntercellSpacing(new Dimension(1, 1));
								}
								if (inner == null)
								{
									((JComponent)renderer).setBorder(BorderFactory.createEmptyBorder());
								}
								else
								{
									((JComponent)renderer).setBorder(inner);
								}
							}
						}
						if (renderer instanceof DataCheckBox)
						{
							DataCheckBox checkBox = ((DataCheckBox)renderer);
							boolean allowNull = true;
							if (cellview instanceof Form)
							{
								ITable formTable = app.getFoundSetManager().getTable(((Form)cellview).getDataSource());
								if (formTable instanceof Table)
								{
									if (checkBox.getDataProviderID() != null)
									{
										if (((Table)formTable).getColumn(checkBox.getDataProviderID()) != null)
										{
											allowNull = ((Table)formTable).getColumn(checkBox.getDataProviderID()).getAllowNull();
										}
									}
								}
							}
							checkBox.setAllowNull(allowNull);
						}

						if (renderer instanceof ISupportAsyncLoading)
						{
							//it is impossible to get lazy loaded images displaying correctly, due to needed repaintfire, which we cannot initiate
							((ISupportAsyncLoading)renderer).setAsyncLoadingEnabled(false);
						}
						if (renderer instanceof ISupportSecuritySettings)
						{
							if (!b_accessible) ((ISupportSecuritySettings)renderer).setAccessible(false);
							if (!b_viewable) ((ISupportSecuritySettings)renderer).setViewable(false);
						}
						if (index == 0 && cellview instanceof Form)
						{
							setRowHeight(renderer.getHeight());
						}
						String dataProviderID = null;
						if (editor instanceof IDisplayData)
						{
							dataProviderID = ((IDisplayData)editor).getDataProviderID();
						}
						if (cellview instanceof Portal && dataProviderID != null && !ScopesUtils.isVariableScope(dataProviderID))
						{
							if (dataProviderID.startsWith(((Portal)cellview).getRelationName() + '.'))
							{
								dataProviderID = dataProviderID.substring(((Portal)cellview).getRelationName().length() + 1);
							}

							if (renderer instanceof IFieldComponent)
							{
								((IFieldComponent)renderer).setDataProviderID(dataProviderID);
							}
							if (editor instanceof IFieldComponent)
							{
								((IFieldComponent)editor).setDataProviderID(dataProviderID);
							}
						}
						String name = ((IFormElement)obj).getName();
						String text = name;
						if (obj instanceof ISupportText)
						{
							text = app.getI18NMessageIfPrefixed(((ISupportText)obj).getText());
						}
						CellAdapter ca = new CellAdapter(app, this, index, ((IFormElement)obj).getSize().width, name, text, dataProviderID, renderer, editor);

						if (cellview instanceof Form && obj instanceof ISupportTabSeq && editor instanceof JComponent && ((ISupportTabSeq)obj).getTabSeq() >= 0)
						{
							columnTabSequence.add(new Pair<ISupportTabSeq, TableColumn>((ISupportTabSeq)obj, ca));
						}

						//set size behaviour
						if (obj instanceof ISupportAnchors)
						{
							int anchors = ((ISupportAnchors)obj).getAnchors();

							if (anchors <= 0) anchors = IAnchorConstants.DEFAULT;
							if (((anchors & IAnchorConstants.WEST) == IAnchorConstants.WEST) && ((anchors & IAnchorConstants.EAST) != IAnchorConstants.EAST) ||
								((anchors & IAnchorConstants.WEST) != IAnchorConstants.WEST) && ((anchors & IAnchorConstants.EAST) == IAnchorConstants.EAST))
							{
								if (cellview instanceof Portal && resizeble)
								{
									// do nothing in this case; let resize work
								}
								else
								{
									ca.setMinWidth(((IFormElement)obj).getSize().width);
									ca.setMaxWidth(((IFormElement)obj).getSize().width);
									ca.setResizable(false);
								}
							}

							if (((anchors & IAnchorConstants.NORTH) == IAnchorConstants.NORTH) &&
								((anchors & IAnchorConstants.SOUTH) == IAnchorConstants.SOUTH))
							{
								unmovableColumns.add(Integer.valueOf(index));
							}
						}
						int objAccess = app.getFlattenedSolution().getSecurityAccess(obj.getUUID());
						boolean b_visible = (objAccess == -1 || ((objAccess & IRepository.VIEWABLE) == IRepository.VIEWABLE));
						if (b_visible)
						{
							tcm.addColumn(ca);

							if (dataProviderID != null)
							{
								CellAdapter previous = (CellAdapter)displays.get(dataProviderID);
								if (previous != null)
								{
									previous.addDisplay(ca);
								}
								else
								{
									displays.put(dataProviderID, ca);
								}
							}
							index++;
						}
						else
						{
							nonViewableColumns.add(ca);
						}
					}
				}
			}

			editorComponents = editors.toArray(new Component[editors.size()]);
			rendererComponents = renderers.toArray(new Component[renderers.size()]);
			dal = new DataAdapterList(app, dpl, new HashMap<IPersist, Object>(), fc, displays, null);

			if (initialSort != null && initialSort.size() > 0)
			{
				for (SortColumn col : initialSort)
				{
					for (int i = 0; i < tcm.getColumnCount(); i++)
					{
						CellAdapter ca = (CellAdapter)tcm.getColumn(i);
						if (ca.getDataProviderID() != null && ca.getDataProviderID().equals(col.getDataProviderID()))
						{
							currentSortCol.put(Integer.valueOf(i), Boolean.valueOf((col.getSortOrder() == SortColumn.ASCENDING)));
						}
					}
				}
			}

			// We don't care much if these two icons are not found (set to null), because they end up
			// in a call to JLabel.setIcon(...) which accepts null as argument without problems.
			ImageIcon arrowUp = app.loadImage("arrow_up.gif"); //$NON-NLS-1$
			ImageIcon arrowDown = app.loadImage("arrow_down.gif"); //$NON-NLS-1$

			//due to the fact the Jtable model is little different inform all CellAdapter about DAL
			for (int i = 0; i < tcm.getColumnCount(); i++)
			{
				CellAdapter ca = (CellAdapter)tcm.getColumn(i);
				ca.setDataAdapterList(dal);
				String name = String.valueOf(ca.getIdentifier());
				GraphicalComponent gc = (GraphicalComponent)labelsFor.get(name);
				LFAwareSortableHeaderRenderer tcr = new LFAwareSortableHeaderRenderer(app, this, ca, arrowUp, arrowDown, gc, formForStyles);
				if (gc != null)
					ComponentFactory.applyBasicComponentProperties(app, tcr, gc, ComponentFactory.getStyleForBasicComponent(app, gc, formForStyles));
				ca.setHeaderRenderer(tcr);
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		ArrayList<TableColumn> cts = new ArrayList<TableColumn>(columnTabSequence.size());
		for (int i = 0; i < columnTabSequence.size(); i++)
		{
			cts.add(columnTabSequence.get(i).getRight());
		}
		tableTabSequenceHandler = new TableTabSequenceHandler(this, cts);
		setColumnModel(tcm);
		final JTableHeader theader = getTableHeader();
		if (theader != null)
		{
			if (theader instanceof UnmovableTableHeader)
			{
				int[] unmovableColumnsA = new int[unmovableColumns.size()];
				int i = 0;
				for (Integer c : unmovableColumns)
					unmovableColumnsA[i++] = c.intValue();
				((UnmovableTableHeader)theader).setUnmovableColumns(unmovableColumnsA);
			}
			theader.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (theader instanceof UnmovableTableHeader) ((UnmovableTableHeader)theader).dragStart();
					if (resizingColumn == null && draggingColumn == null && e.isPopupTrigger())
					{
						handleRightClick(e);
					}
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					if (theader instanceof UnmovableTableHeader) ((UnmovableTableHeader)theader).dragStop();
					if (resizingColumn != null || draggingColumn != null)
					{
						int nrColumns = TableView.this.getColumnCount();

						if (draggingColumn != null)
						{
							keepGroupOrder(draggingColumn);
						}

						for (int i = 0; i < nrColumns; i++)
						{
							CellAdapter column = (CellAdapter)TableView.this.getColumnModel().getColumn(i);
							updateCellAdapterComponentBounds(column, i, resizingColumn != null, draggingColumn != null || resizingColumn != null);
						}
					}
					else if (e.isPopupTrigger())
					{
						handleRightClick(e);
					}
					resizingColumn = null;
					draggingColumn = null;
					tableColumnsBeforeDrag = null;
				}

				private void handleRightClick(MouseEvent e)
				{
					int column = columnAtPoint(e.getPoint());
					CellAdapter ca = (CellAdapter)getColumnModel().getColumn(column);
					String name = String.valueOf(ca.getIdentifier());
					GraphicalComponent gc = (GraphicalComponent)labelsFor.get(name);
					if (gc != null && gc.getOnRightClickMethodID() > 0)
					{
						Component editor = ca.getEditor();

						JSEvent event = new JSEvent();
						event.setType(JSEvent.EventType.rightClick);
						event.setFormName(fc.getName());
						event.setElementName(gc.getName());
						event.setModifiers(e.getModifiers() == IEventExecutor.MODIFIERS_UNSPECIFIED ? 0 : e.getModifiers());
						event.setLocation(editor != null ? new Point(e.getPoint().x - editor.getX(), e.getPoint().y) : e.getPoint());
						event.setAbsoluteLocation(e.getLocationOnScreen());
						scriptExecuter.executeFunction(String.valueOf(gc.getOnRightClickMethodID()), new Object[] { event }, true, e.getComponent(), false,
							StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID.getPropertyName(), false);
					}
				}
			});
			theader.addMouseMotionListener(new MouseMotionAdapter()
			{
				@Override
				public void mouseDragged(MouseEvent e)
				{
					if (draggingColumn != getTableHeader().getDraggedColumn())
					{
						tableColumnsBeforeDrag = new ArrayList<TableColumn>();
						Enumeration<TableColumn> columns = getTableHeader().getColumnModel().getColumns();
						while (columns.hasMoreElements())
						{
							tableColumnsBeforeDrag.add(columns.nextElement());
						}
						draggingColumn = getTableHeader().getDraggedColumn();
					}
					resizingColumn = getTableHeader().getResizingColumn();
				}
			});
		}

		// add a column model listener so that the editor/renderer components have their X and width updated when columns
		// change those because of window resizes; needed especially when there is no row in the table (so the renderer/editor are not repositioned), but the
		// js_get... methods are called (and execute on the renderer/editor component) and correct results are expected
		getColumnModel().addColumnModelListener(new TableColumnModelListener()
		{
			public void columnMoved(TableColumnModelEvent e)
			{
				updateAllWidthsAndXs();
			}

			public void columnMarginChanged(ChangeEvent e)
			{
				updateAllWidthsAndXs();
			}

			private void updateAllWidthsAndXs()
			{
				if (isLayoutChangingViaJavascript() || isLayoutChangingViaUserHeaderAction()) return;
				int nrColumns = TableView.this.getColumnCount();

				for (int i = 0; i < nrColumns; i++)
				{
					CellAdapter column = (CellAdapter)TableView.this.getColumnModel().getColumn(i);
					updateCellAdapterComponentBounds(column, i, true, true);
				}
			}

			public void columnSelectionChanged(ListSelectionEvent e)
			{
				// not used
			}

			public void columnRemoved(TableColumnModelEvent e)
			{
				// not used
			}

			public void columnAdded(TableColumnModelEvent e)
			{
				// not used
			}
		});

		addMouseListener(new MouseAdapter()
		{
			// see comment Container -> processMouseEvent; jdk 1.6_13
			// 4508327: MOUSE_CLICKED should never be dispatched to a Component
			// other than that which received the MOUSE_PRESSED event.  If the
			// mouse is now over a different Component, don't dispatch the event.
			// The previous fix for a similar problem was associated with bug
			// 4155217.
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (!e.isConsumed() && !SwingUtilities.isLeftMouseButton(e))
				{
					// if the event is not consumed and it is the left mouse button
					// test if the selected record is under the mouse, this is disabled by default.
					Point p = e.getPoint();
					int row = rowAtPoint(p);
					int column = columnAtPoint(p);
					if (column == -1 || row == -1) return;
					ListSelectionModel sm = getSelectionModel();
					if (sm != null && !sm.isSelectedIndex(row))
					{
						if (isEditing() && !getCellEditor().stopCellEditing()) return;
						boolean ctrl = UIUtils.isCommandKeyDown(e);
						changeSelection(row, column, ctrl, !ctrl && e.isShiftDown());
					}
				}

				if (e.isPopupTrigger()) handlePopupTrigger(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger()) handlePopupTrigger(e);
			}

			private void handlePopupTrigger(MouseEvent e)
			{
				Point p = e.getPoint();
				int column = columnAtPoint(p);
				CellAdapter cellAdapter = (CellAdapter)getColumnModel().getColumn(column);
				if (cellAdapter != null)
				{
					Component editor = cellAdapter.getEditor();
					if (editor instanceof ISupportEventExecutor)
					{
						IEventExecutor ee = ((ISupportEventExecutor)editor).getEventExecutor();
						if (ee instanceof BaseEventExecutor && ee.hasRightClickCmd())
						{
							int row = rowAtPoint(p);
							if (isCellEditable(row, column))
							{
								editCellAt(row, column, e);
							}
							((BaseEventExecutor)ee).fireRightclickCommand(true, editor, e.getModifiers(), fc.getName(),
								new Point(e.getPoint().x - editor.getX(), e.getPoint().y - editor.getY()), null);
						}
					}
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				for (int i = 0; i < getColumnCount(); i++)
				{
					CellAdapter cellAdapter = (CellAdapter)getColumnModel().getColumn(i);
					if (cellAdapter != null && (cellAdapter.getRenderer() instanceof ILabel))
					{
						ToolTipManager.sharedInstance().unregisterComponent((JComponent)cellAdapter.getRenderer());
						cellAdapter.getRenderer().dispatchEvent(
							new MouseEvent(cellAdapter.getRenderer(), MouseEvent.MOUSE_EXITED, e.getWhen(), e.getModifiers(), e.getX(), e.getY(), 0, false));
					}
				}
				TableView.this.repaint();
			}
		});

		addMouseMotionListener(new MouseMotionAdapter()
		{
			private int column = -1;
			private int row = -1;

			@Override
			public void mouseMoved(final MouseEvent e)
			{
				Point p = e.getPoint();
				final int newcolumn = columnAtPoint(p);
				final int newrow = rowAtPoint(p);
				if ((newcolumn != column || newrow != row) && (column >= 0) && (row >= 0) && (column < getColumnCount()))
				{
					CellAdapter cellAdapter = (CellAdapter)getColumnModel().getColumn(column);
					if (cellAdapter != null && (cellAdapter.getRenderer() instanceof ILabel))
					{
						ToolTipManager.sharedInstance().unregisterComponent((JComponent)cellAdapter.getRenderer());
						cellAdapter.getRenderer().dispatchEvent(
							new MouseEvent(cellAdapter.getRenderer(), MouseEvent.MOUSE_EXITED, e.getWhen(), e.getModifiers(), e.getX(), e.getY(), 0, false));
						TableView.this.repaint(getCellRect(row, column, false));
					}
				}
				column = newcolumn;
				row = newrow;
				if (newcolumn >= 0 && newrow >= 0)
				{
					final CellAdapter cellAdapter = (CellAdapter)getColumnModel().getColumn(newcolumn);
					if (cellAdapter != null && (cellAdapter.getRenderer() instanceof ILabel))
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								ToolTipManager.sharedInstance().unregisterComponent((JComponent)cellAdapter.getRenderer());
								cellAdapter.getRenderer().dispatchEvent(new MouseEvent(cellAdapter.getRenderer(), MouseEvent.MOUSE_ENTERED, e.getWhen(),
									e.getModifiers(), e.getX(), e.getY(), 0, false));
								TableView.this.repaint(getCellRect(newrow, newcolumn, false));
							}
						});
					}
				}
			}
		});
	}

	/**
	 * Will return true when columns are being moved/resized by user using header drag operations.
	 * @return true when columns are being moved/resized by user using header drag operations.
	 */
	public boolean isLayoutChangingViaUserHeaderAction()
	{
		return tableColumnsBeforeDrag != null;
	}

	/**
	 * Will return true when columns are being moved/resized because of a JS call.
	 * @return true when columns are being moved/resized because of a JS call.
	 */
	public boolean isLayoutChangingViaJavascript()
	{
		return layoutChangingByJavascript;
	}

	public void setLayoutChangingViaJavascript(boolean b)
	{
		layoutChangingByJavascript = b;
	}

	private void keepGroupOrder(TableColumn column)
	{
		// check for groups, put the elements in the same order back together

		// first the columns left from the dragged column
		int offset = -1;
		while (moveColumnInSameGroup(column, offset))
		{
			offset--;
		}
		// then the columns right from the dragged column
		offset = 1;
		while (moveColumnInSameGroup(column, offset))
		{
			offset++;
		}
		// check if a column is breaking up a group
		for (TableColumn tc : tableColumnsBeforeDrag)
		{
			moveColumnInSameGroup(tc, 1);
		}
	}

	private boolean moveColumnInSameGroup(TableColumn column, int offset)
	{
		Component editor = ((CellAdapter)column).getEditor();
		String groupId = (String)fc.getComponentProperty(editor, ComponentFactory.GROUPID_COMPONENT_PROPERTY);
		if (groupId == null)
		{
			return false;
		}

		// find the columns offsetted to this column in the original columns
		int orgIindex = tableColumnsBeforeDrag.indexOf(column);
		if (orgIindex == -1)
		{
			return false; // strange, should not happen
		}
		if ((offset < 0 && orgIindex + offset < 0) || (offset > 0 && orgIindex + offset >= tableColumnsBeforeDrag.size()))
		{
			// no more columns in original set
			return false;
		}

		int currIndex = indexOfColumn(column);
		if (currIndex == -1)
		{
			return false; // strange, should not happen
		}

		TableColumn column2 = tableColumnsBeforeDrag.get(orgIindex + offset);
		Component editor2 = ((CellAdapter)column2).getEditor();
		if (!groupId.equals(fc.getComponentProperty(editor2, ComponentFactory.GROUPID_COMPONENT_PROPERTY)))
		{
			// original column at offset is not part of the same group
			return false;
		}

		int currIndex2 = indexOfColumn(column2);
		if (currIndex2 == -1)
		{
			return false; // strange, should not happen
		}

		// move this one next to the prev
		if (offset < 0)
		{
			if (currIndex2 > currIndex)
			{
				if (currIndex2 != currIndex + offset + 1)
				{
					getColumnModel().moveColumn(currIndex2, currIndex + offset + 1);
				}
			}
			else
			{
				if (currIndex2 != currIndex + offset)
				{
					getColumnModel().moveColumn(currIndex2, currIndex + offset);
				}
			}
		}
		else
		{
			if (currIndex2 < currIndex)
			{
				if (currIndex2 != currIndex + offset - 1)
				{
					getColumnModel().moveColumn(currIndex2, currIndex + offset - 1);
				}
			}
			else
			{
				if (currIndex2 != currIndex + offset)
				{
					getColumnModel().moveColumn(currIndex2, currIndex + offset);
				}
			}
		}

		// moved
		return true;
	}

	protected void updateCellAdapterComponentBounds(CellAdapter column, int columnIndex, boolean sizeChanged, boolean locationChanged)
	{
		Rectangle cellRect = TableView.this.getCellRect(-1, columnIndex, true); // the cell's x and height will be 0 as we are not interested in them
		int cm = getColumnModel().getColumnMargin();
		cellRect.x += cm / 2;
		cellRect.width -= cm;

		// in some cases the js_getLocationX, js_getWidth when called from javascript will return outdated values. For example, table has no row and user
		// resizes/moves columns using table header (and renderer/editor will not be relocated/repainted), or user resizes window and because of anchors
		// this results in a resize of the columns; the following calls will update the editor/renderer location in such cases
		updateComponentBounds(column.getEditor(), cellRect, sizeChanged, locationChanged);
		updateComponentBounds(column.getRenderer(), cellRect, sizeChanged, locationChanged);

		// the change of bounds performed by the two updateComponentBounds(...) calls will add component resize/move events for the editor component on the AWT queue (that we are on now);
		// in order for those resizes to be ignored by component listener registered in CellAdapter on editor, we call the update methods (that listener should only handle change bounds events that are comming from js_set... calls);
		// these updateEditor calls will be executed before the actual resize/move events reach the listener, even though the editor has already changed bounds
		if (locationChanged) column.updateEditorX();
		if (sizeChanged) column.updateEditorWidth();
	}

	protected void updateComponentBounds(Component component, Rectangle cellRect, boolean sizeChanged, boolean locationChanged)
	{
		int height;
		int y;
		if (component instanceof ISupportCachedLocationAndSize)
		{
			Dimension cachedSize = ((ISupportCachedLocationAndSize)component).getCachedSize();
			height = ((cachedSize != null) ? cachedSize.height : 0);
			Point cachedLocation = ((ISupportCachedLocationAndSize)component).getCachedLocation();
			y = ((cachedLocation != null) ? cachedLocation.y : 0);
		}
		else
		{
			height = component.getHeight();
			y = component.getY();
		}

		if (sizeChanged)
		{
			if (component instanceof IScriptableProvider && ((IScriptableProvider)component).getScriptObject() instanceof IRuntimeComponent)
			{
				((IRuntimeComponent)((IScriptableProvider)component).getScriptObject()).setSize(cellRect.width, height);
			}
			else
			{
				component.setSize(cellRect.width, height);
			}
		}
		if (locationChanged)
		{
			if (component instanceof IScriptableProvider && ((IScriptableProvider)component).getScriptObject() instanceof IRuntimeComponent)
			{
				((IRuntimeComponent)((IScriptableProvider)component).getScriptObject()).setLocation(cellRect.x, y);
			}
			else
			{
				component.setLocation(cellRect.x, y);
			}
		}
	}

	protected int indexOfColumn(TableColumn column)
	{
		Enumeration<TableColumn> columns = getColumnModel().getColumns();
		for (int i = 0; columns.hasMoreElements(); i++)
		{
			if (columns.nextElement().equals(column))
			{
				return i;
			}
		}
		return -1;
	}

	public void updateSortStatus(int sortCol, boolean sortAsc)
	{
		if (sortCol >= 0)
		{
			currentSortCol.put(Integer.valueOf(sortCol), Boolean.valueOf(sortAsc));
		}
		else
		{
			currentSortCol.clear();
		}
	}

	public boolean setSortStatus(IFoundSetInternal foundset)
	{
		if (foundset != null)
		{
			List<SortColumn> sortCols = foundset.getSortColumns();
			if (sortCols != null && sortCols.size() > 0)
			{
				boolean found = false;
				for (SortColumn sc : sortCols)
				{
					for (int i = 0; (i < getColumnModel().getColumnCount()); i++)
					{
						TableColumn tc = getColumnModel().getColumn(i);
						if (tc instanceof CellAdapter)
						{
							CellAdapter ca = (CellAdapter)tc;
							if (ca.getDataProviderID() != null)
							{
								List<String> sortingProviders = null;
								Component renderer = ca.getRenderer();
								if (renderer instanceof ISupportValueList && ((ISupportValueList)renderer).getValueList() != null)
								{
									try
									{
										sortingProviders = DBValueList.getShowDataproviders(((ISupportValueList)renderer).getValueList().getValueList(),
											(Table)foundset.getTable(), ca.getDataProviderID(), application.getFoundSetManager());
									}
									catch (RepositoryException ex)
									{
										Debug.error(ex);
									}
								}

								if (sortingProviders == null)
								{
									// no related sort, use sort on dataProviderID instead
									sortingProviders = Collections.singletonList(ca.getDataProviderID());
								}
								for (String sortingProvider : sortingProviders)
								{
									SortColumn existingSc;
									try
									{
										FoundSetManager fsm = (FoundSetManager)foundset.getFoundSetManager();
										existingSc = fsm.getSortColumn(foundset.getTable(), sortingProvider, false);
									}
									catch (Exception e)
									{
										Debug.error(e);
										continue;
									}
									if (sc.equalsIgnoreSortorder(existingSc))
									{
										if (!found)
										{
											// clear old sort
											updateSortStatus(-1, true);
										}
										found = true;
										updateSortStatus(ca.getModelIndex(), sc.getSortOrder() == SortColumn.ASCENDING);
									}
								}
							}
						}
					}
				}
				return found;
			}
		}
		return false;
	}

	public Map<Integer, Boolean> getCurrentSortColumn()
	{
		return this.currentSortCol;
	}

	@Override
	protected TableColumnModel createDefaultColumnModel()
	{
		return new DefaultTableColumnModel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void moveColumn(int columnIndex, int newIndex)
			{
				if (stopUIEditing(true))
				{
					super.moveColumn(columnIndex, newIndex);
				}
			}
		};
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		if ((headerCache == null) && (leadingGrandSummaryCache == null))
		{
			getTableHeader().setReorderingAllowed(reorderble && enabled);
			getTableHeader().setResizingAllowed(resizeble && enabled);
		}
	}

	@Override
	public void configureEnclosingScrollPane()
	{
		if ((headerCache == null) && (leadingGrandSummaryCache == null))
		{
			getTableHeader().setColumnModel(columnModel);
			if (sortable)
			{
				// Remove any previously registered mouse listeners of this kind.
				MouseListener[] ml = getTableHeader().getMouseListeners();
				for (MouseListener element : ml)
				{
					if (element instanceof ColumnSortListener) getTableHeader().removeMouseListener(element);
				}
				getTableHeader().addMouseListener(new ColumnSortListener(application, this, fc));
			}
		}
		super.configureEnclosingScrollPane();

		JScrollPane scrollPane = null;
		JViewport viewport = null;
		Container p = getParent();
		if (p instanceof JViewport)
		{
			Container gp = p.getParent();
			if (gp instanceof JScrollPane)
			{
				scrollPane = (JScrollPane)gp;
				// Make certain we are the viewPort's view and not, for
				// example, the rowHeaderView of the scrollPane -
				// an implementor of fixed columns might do this.
				viewport = scrollPane.getViewport();
				if (viewport != null && viewport.getView() != this)
				{
					viewport = null;
				}
			}
		}

		if ((headerCache != null) || (leadingGrandSummaryCache != null))
		{
			setTableHeader(null);
			if (viewport != null)
			{
				EnablePanel ep = new EnablePanel();
				ep.setOpaque(false);
				ep.setLayout(new BorderLayout());
				if (headerCache != null) ep.add((Component)headerCache, BorderLayout.CENTER);
				if (leadingGrandSummaryCache != null) ep.add((Component)leadingGrandSummaryCache, BorderLayout.SOUTH);
				scrollPane.setColumnHeaderView(ep);
			}
		}

		// need this to have the right bgcolor in the header above the content scrollbar
		if (viewport != null)
		{
			JViewport columnHeader = scrollPane.getColumnHeader();
			if (columnHeader != null)
			{
				columnHeader.setOpaque(false);
				Component columnHeaderView = columnHeader.getView();
				if (columnHeaderView instanceof JComponent) ((JComponent)columnHeaderView).setOpaque(false);
			}
		}
	}

	int previousRow = -1;
	int previousColumn = -1;

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.JComponent#getToolTipLocation(java.awt.event.MouseEvent)
	 */
	@Override
	public Point getToolTipLocation(MouseEvent event)
	{
		Point point = null;
		int row = rowAtPoint(event.getPoint());
		int column = columnAtPoint(event.getPoint());
		if (row != -1 && column != -1 && (row != previousRow || column != previousColumn))
		{
			point = event.getPoint();
			point.y = point.y + 20;
		}
		previousRow = row;
		previousColumn = column;
		return point;
	}

	/**
	 * @see com.servoy.j2db.IView#editCellAt(int)
	 */
	public boolean editCellAt(int row)
	{
		if (row < 0 || row >= getRowCount()) return false;

		int iCell = 0;
		Component comp = getCellEditor(row, iCell).getTableCellEditorComponent(this, null, true, row, iCell);
		//TODO check are there more components not valid??
		while (comp instanceof JButton || comp instanceof JLabel || !comp.isEnabled() ||
			(comp instanceof JTextComponent && !((JTextComponent)comp).isEditable()))
		{
			iCell++;
			if (iCell >= getColumnCount())
			{
				return false;
			}
			comp = ((CellAdapter)getCellEditor(row, iCell)).getEditor();
		}
		boolean b = editCellAt(row, iCell);
		if (b)
		{
			comp = getEditorComponent();
			if (comp != null) comp.requestFocus();
		}
		return b;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.gui.FixedJTable#editCellAt(int, int, java.util.EventObject)
	 */
	@Override
	public boolean editCellAt(int row, int column, EventObject e)
	{
		boolean b = super.editCellAt(row, column, e);
		Component comp = getEditorComponent();
		if (comp instanceof ISupportEventExecutor)
		{
			// focus lost event comes after editor is removed from hierarchy
			IEventExecutor executor = ((ISupportEventExecutor)comp).getEventExecutor();
			if (executor instanceof BaseEventExecutor)
			{
				((BaseEventExecutor)executor).setFormName(fc.getName());
			}
		}
		if (b && !isEditable() && comp instanceof IScriptableProvider && ((IScriptableProvider)comp).getScriptObject() instanceof HasRuntimeReadOnly &&
			!((HasRuntimeReadOnly)((IScriptableProvider)comp).getScriptObject()).isReadOnly())
		{
			// the component is editable again, we have to set it back
			((HasRuntimeReadOnly)((IScriptableProvider)comp).getScriptObject()).setReadOnly(true);
		}

		return b;
	}

	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
	{
		// if we are in find mode, no cell is being edited and enter is pressed then we should enter edit mode
		// on selected cell rather then perform the search (so the enter release must be marked as consumed/processed);
		// for example search for something => no results, choose modify last find in the dialog - now you must be able to enter
		// edit mode in the table to modify find criteria using ENTER (otherwise you need to use mouse for example in order not to loose
		// contents of text fields you want to modify)...
		boolean wasEditingBeforeEventWasProcessed = isEditing();
		boolean keyEventWasUsed = super.processKeyBinding(ks, e, condition, pressed);
		if ((wasEditingBeforeEventWasProcessed != isEditing()) && ks.getKeyEventType() == KeyEvent.KEY_PRESSED)
		{
			keyReleaseToBeIgnored = ks.getKeyCode();
			wasEditingBeforeKeyPress = wasEditingBeforeEventWasProcessed;
		}
		else if (keyReleaseToBeIgnored == ks.getKeyCode() && ks.getKeyEventType() == KeyEvent.KEY_RELEASED)
		{
			keyReleaseToBeIgnored = -1;
			return ((!wasEditingBeforeKeyPress && isEditing()) && ks.getKeyCode() == KeyEvent.VK_ENTER) ||
				(wasEditingBeforeKeyPress && ks.getKeyCode() == KeyEvent.VK_ESCAPE);
		}
		else if (ks.getKeyEventType() != KeyEvent.KEY_TYPED)
		{
			keyReleaseToBeIgnored = -1; // get rid of this value, because this in only implemented for simple scenarios
			// and it shouldn't cause trouble
		}
		return keyEventWasUsed;
	}

	/**
	 * @see com.servoy.j2db.IView#ensureIndexIsVisible(int)
	 */
	public void ensureIndexIsVisible(int i)
	{
		Rectangle cellRect = getCellRect(i, 0, true);
		scrollRectToVisible(cellRect);
	}

	public void setVisibleRect(Rectangle scrollPosition)
	{
		if (isVisible())
		{
			scrollRectToVisible(scrollPosition);
		}
	}

	/**
	 * @see com.servoy.j2db.IView#removeListSelectionListener(javax.swing.event.ListSelectionListener)
	 */
	public void removeListSelectionListener(ListSelectionListener l)
	{
		getSelectionModel().removeListSelectionListener(l);
	}

	private final DefaultListSelectionModel EMPTY_SELECTION = new DefaultListSelectionModel();
	private final DefaultTableModel EMPTY_DATA = new DefaultTableModel();

	public void destroy()
	{
		stop();
		if (dal != null)
		{
			dal.destroy();
		}
		removeAll();
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		if (dal != null) dal.notifyVisible(b, invokeLaterRunnables);
	}

	/**
	 * @see com.servoy.j2db.IView#setModel(com.servoy.j2db.dataprocessing.IFoundSetInternal)
	 */
	public void setModel(IFoundSetInternal fs)
	{
		if (fs == null)
		{
			// if model is set to null then remember the current row count
			// so calls to getRowCount() return that value so that the scroll bar position isn't set to 0.
			// because when the foundset is set back to that one it is probably the same foundset in the
			// same view so the scroll bar position should stick.
			prevRowCount = getModel().getRowCount();
			removeEditor();//safety
			// first make sure that the selection model is not adjusting anymore.
			// (TableUI will set it to true in onpress of mouse and this will happen before onrelease that would reset it)
			getSelectionModel().setValueIsAdjusting(false);
			setSelectionModel(EMPTY_SELECTION);
			super.setModel(EMPTY_DATA);
		}
		else
		{
			// JTable clears the selection in setModel()
			prevRowCount = -1;
			setSelectionModel(EMPTY_SELECTION);
			super.setModel((ISwingFoundSet)fs);
			setSelectionModel(((ISwingFoundSet)fs).getSelectionModel());
		}
		if (currentSortCol.size() > 0 && getTableHeader() != null)
		{
			setSortStatus(fs);
			getTableHeader().invalidate();
			getTableHeader().repaint();
		}
	}

	private int prevRowCount = -1;

	@Override
	public int getRowCount()
	{
		if (prevRowCount != -1)
		{
			// prev row count is set by a null model set.
			// now test if this table view is invisible
			boolean b = isVisible();

			Container parent = getParent();
			while (b && parent != null && !(parent instanceof MainPanel))
			{
				b = parent.isVisible();
				parent = parent.getParent();
			}
			if (b)
			{
				// one more try for fids
				while (parent != null && !(parent instanceof FormWindow))
				{
					parent = parent.getParent();
					if (parent instanceof FormWindow)
					{
						b = parent.isVisible();
					}
				}
			}
			// if this tableview is already invisible return the previous row count
			if (!b) return prevRowCount;
		}
		return super.getRowCount();
	}

	/**
	 * @see com.servoy.j2db.IView#start()
	 */
	public void start(final IApplication application)
	{
		getParent().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (isEnabled())
				{
					application.getFoundSetManager().getEditRecordList().stopEditing(false);
				}
			}
		});
	}

	/**
	 * @see com.servoy.j2db.IView#stop()
	 */
	public void stop()
	{
		setModel((FoundSet)null);
	}

	public Component[] getEditorComponents()
	{
		return editorComponents;
	}

	public Component[] getRendererComponents()
	{
		return rendererComponents;
	}

	public DataAdapterList getDataAdapterList()
	{
		return dal;
	}

	public void setAllNonFieldsEnabled(boolean b)
	{
	}

	public void setAllNonRowFieldsEnabled(boolean b)
	{
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (isEditing())
		{
			CellEditor ce = getCellEditor();
			if (ce != null)
			{
				if (looseFocus)
				{
					return ce.stopCellEditing();
				}
				else
				{
					ce.getCellEditorValue();
					if (ce instanceof CellAdapter)
					{
						Component editor = ((CellAdapter)ce).getEditor();
						// if the notify changed failed the mustTestLastValue is set and we shouldn't allow stopping:
						if (editor instanceof IDisplayData && !((IDisplayData)editor).isValueValid())
						{
							return false;
						}
					}
				}
			}
		}
		return true;
	}

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

	public void setComponentEnabled(boolean enabled)
	{
		setEnabled(enabled);
	}

	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
	}

	public void addDisplayComponent(IPersist obj, IDisplay display)
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.ui.IDataRenderer#refreshRecord(com.servoy.j2db.dataprocessing.IRecordInternal)
	 */
	public void refreshRecord(IRecordInternal record)
	{
		// have to set this because of relookup adapters.
		if (dal != null)
		{
			if (isEditing() && dal.getState() != record)
			{
				stopUIEditing(true);
			}
			dal.setRecord(record, true);
		}
		if (dataRendererOnRenderWrapper.getRenderEventExecutor().hasRenderCallback())
		{
			dataRendererOnRenderWrapper.getRenderEventExecutor().setRenderState(null, -1, false, true);
			dataRendererOnRenderWrapper.getRenderEventExecutor().fireOnRender(false);
		}

	}

	public boolean isColumnIdentifierComponent(Component comp)
	{
		Enumeration<TableColumn> columns = getColumnModel().getColumns();
		while (columns.hasMoreElements())
		{
			CellAdapter c = (CellAdapter)columns.nextElement();
			if (comp == c.getEditor())
			{
				return true;
			}
			else if (c.getEditor() instanceof ISupplyFocusChildren< ? >)
			{
				for (Object child : ((ISupplyFocusChildren< ? >)c.getEditor()).getFocusChildren())
				{
					if (child == comp) return true;
				}
			}
		}
		return false;
	}

	public boolean isDisplayingMoreThanOneRecord()
	{
		return true;
	}

	public void focusFirstField()
	{
		editCellAt(getSelectedRow());
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}

	public Iterator<IComponent> getComponentIterator()
	{
		//ignore
		return null;
	}

	public void add(IComponent c, String name)
	{
		//ignore
	}

	public void remove(IComponent c)
	{
		//ignore
	}

	public void setTabSeqComponents(List<Component> list)
	{
		tableTabSequenceHandler.setNewTabSequence(list);
	}

	public List<String> getTabSeqComponentNames()
	{
		return tableTabSequenceHandler.getTabSequence();
	}

	private final List<HasRuntimeReadOnly> componentsWithEditableStateChanged = new ArrayList<HasRuntimeReadOnly>();

	private boolean editable = true;

	private List<HasRuntimeReadOnly> getReadOnlyAwareComponents()
	{
		List<HasRuntimeReadOnly> readOnlyAwareComponents = new ArrayList<HasRuntimeReadOnly>();

		if (rendererComponents != null)
		{
			for (Component element : rendererComponents)
			{
				if (element instanceof IScriptableProvider && ((IScriptableProvider)element).getScriptObject() instanceof HasRuntimeReadOnly)
				{
					readOnlyAwareComponents.add(((HasRuntimeReadOnly)((IScriptableProvider)element).getScriptObject()));
				}
			}
		}
		if (editorComponents != null)
		{
			for (Component element : editorComponents)
			{
				if (element instanceof IScriptableProvider && ((IScriptableProvider)element).getScriptObject() instanceof HasRuntimeReadOnly)
				{
					readOnlyAwareComponents.add(((HasRuntimeReadOnly)((IScriptableProvider)element).getScriptObject()));
				}
			}
		}

		return readOnlyAwareComponents;
	}

	@Override
	public void setEditable(boolean editable)
	{
		super.setEditable(editable);
		this.editable = editable;
	}

	public void setEditableWithStateTracking(boolean b)
	{
		if (editable != b)
		{
			super.setEditable(b);
			this.editable = b;

			if (editable == false)
			{
				Iterator<HasRuntimeReadOnly> readOnlyAwareComponentsIte = getReadOnlyAwareComponents().iterator();
				HasRuntimeReadOnly el;
				while (readOnlyAwareComponentsIte.hasNext())
				{
					el = readOnlyAwareComponentsIte.next();

					if (el.isReadOnly() == false)
					{
						el.setReadOnly(true);
						if (componentsWithEditableStateChanged.contains(el) == false) componentsWithEditableStateChanged.add(el);
					}
				}
			}
			else
			{
				if (componentsWithEditableStateChanged.size() != 0)
				{
					for (HasRuntimeReadOnly component : componentsWithEditableStateChanged)
					{
						component.setReadOnly(false);
					}
				}
				componentsWithEditableStateChanged.clear();
			}

			repaint();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.gui.FixedJTable#isEditable()
	 */
	@Override
	public boolean isEditable()
	{
		return editable;
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		super.tableChanged(e);

		// We try to detect when a sort has been done on the foundset, and we update the arrows in the header accordingly.
		// This is just an heuristic for filtering out the sort event from all table changed events that are raised.
		if (getModel() instanceof IFoundSetInternal && getTableHeader() != null && fc != null && e.getColumn() == TableModelEvent.ALL_COLUMNS &&
			e.getFirstRow() == 0)
		{
			List<SortColumn> sortCols = ((IFoundSetInternal)getModel()).getSortColumns();
			if (sortCols != null && sortCols.size() > 0)
			{
				if (!setSortStatus((FoundSet)getModel()))
				{
					// clear
					updateSortStatus(-1, true);
				}
				getTableHeader().invalidate();
				getTableHeader().repaint();
			}
		}
	}

	public boolean shouldDisplaySortIcons()
	{
		if (fc != null && fc.getForm() != null && fc.getForm().getOnSortCmdMethodID() < 0) return false;
		return true;
	}

	public int onDrag(JSDNDEvent event)
	{
		int onDragID = 0;
		if (cellview instanceof Portal)
		{
			Portal cellviewPortal = (Portal)cellview;
			onDragID = cellviewPortal.getOnDragMethodID();
		}
		else
		{
			onDragID = fc.getForm().getOnDragMethodID();
		}

		if (onDragID > 0)
		{
			Object dragReturn = fc.executeFunction(Integer.toString(onDragID), new Object[] { event }, false, null, false, "onDragMethodID"); //$NON-NLS-1$
			if (dragReturn instanceof Number) return ((Number)dragReturn).intValue();
		}

		return DRAGNDROP.NONE;
	}

	public boolean onDragOver(JSDNDEvent event)
	{
		int onDragOverID = 0;
		if (cellview instanceof Portal)
		{
			Portal cellviewPortal = (Portal)cellview;
			onDragOverID = cellviewPortal.getOnDragOverMethodID();
		}
		else
		{
			onDragOverID = fc.getForm().getOnDragOverMethodID();
		}

		if (onDragOverID > 0)
		{
			Object dragOverReturn = fc.executeFunction(Integer.toString(onDragOverID), new Object[] { event }, false, null, false, "onDragOverMethodID"); //$NON-NLS-1$
			if (dragOverReturn instanceof Boolean) return ((Boolean)dragOverReturn).booleanValue();
		}

		return getOnDropMethodID() > 0;
	}

	private int getOnDropMethodID()
	{
		int onDropID = 0;
		if (cellview instanceof Portal)
		{
			Portal cellviewPortal = (Portal)cellview;
			onDropID = cellviewPortal.getOnDropMethodID();
		}
		else
		{
			onDropID = fc.getForm().getOnDropMethodID();
		}
		return onDropID;
	}

	public boolean onDrop(JSDNDEvent event)
	{
		int onDropID = getOnDropMethodID();

		if (onDropID > 0)
		{
			Object dropHappened = fc.executeFunction(Integer.toString(onDropID), new Object[] { event }, false, null, false, "onDropMethodID"); //$NON-NLS-1$
			if (dropHappened instanceof Boolean) return ((Boolean)dropHappened).booleanValue();
		}

		return false;
	}

	public void onDragEnd(JSDNDEvent event)
	{
		int onDragEndID = 0;
		if (cellview instanceof Portal)
		{
			Portal cellviewPortal = (Portal)cellview;
			onDragEndID = cellviewPortal.getOnDragEndMethodID();
		}
		else
		{
			onDragEndID = fc.getForm().getOnDragEndMethodID();
		}

		if (onDragEndID > 0)
		{
			fc.executeFunction(Integer.toString(onDragEndID), new Object[] { event }, false, null, false, "onDragEndMethodID"); //$NON-NLS-1$
		}
	}

	public IComponent getDragSource(Point xy)
	{
		int columnNrAtXY = columnAtPoint(xy);
		int rowNrAtXY = rowAtPoint(xy);
		if (columnNrAtXY > -1 && rowNrAtXY > -1)
		{
			CellAdapter column = (CellAdapter)getColumnModel().getColumn(columnNrAtXY);
			Component renderer = column.getTableCellRendererComponent(this, null, false, false, rowNrAtXY, columnNrAtXY);

			if (renderer instanceof IComponent && renderer.isEnabled()) return (IComponent)renderer;
		}

		return this;
	}

	public String getDragFormName()
	{
		return fc.getName();
	}

	public boolean isGridView()
	{
		return true;
	}

	public IRecordInternal getDragRecord(Point xy)
	{
		int rowNrAtXY = rowAtPoint(xy);
		if (rowNrAtXY > -1 && getModel() instanceof IFoundSetInternal) return ((IFoundSetInternal)getModel()).getRecord(rowNrAtXY);
		return null;
	}

	public int getYOffset()
	{
		return yOffset;
	}

	private int yOffset;

	public void initDragNDrop(FormController formController, int clientDesignYOffset)
	{
		this.yOffset = clientDesignYOffset;
		boolean enableDragDrop = false;
		if (cellview instanceof Portal)
		{
			Portal cellviewPortal = (Portal)cellview;
			enableDragDrop = (cellviewPortal.getOnDragMethodID() > 0 || cellviewPortal.getOnDragEndMethodID() > 0 ||
				cellviewPortal.getOnDragOverMethodID() > 0 || cellviewPortal.getOnDropMethodID() > 0);
		}
		else
		{
			Form form = fc.getForm();
			enableDragDrop = (form.getOnDragMethodID() > 0 || form.getOnDragEndMethodID() > 0 || form.getOnDragOverMethodID() > 0 ||
				form.getOnDropMethodID() > 0);
		}

		if (enableDragDrop && !GraphicsEnvironment.isHeadless())
		{
			setDragEnabled(true);

			setTransferHandler(FormDataTransferHandler.getInstance());

			new DropTarget(this, (DropTargetListener)FormDataTransferHandler.getInstance());

			addHierarchyListener(new HierarchyListener()
			{
				public void hierarchyChanged(HierarchyEvent e)
				{
					JComponent changedParent = (JComponent)e.getChangedParent();
					if (changedParent != null && e.getChanged() == TableView.this &&
						(e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) == HierarchyEvent.PARENT_CHANGED)
					{
						changedParent.setTransferHandler(FormDataTransferHandler.getInstance());
						new DropTarget(changedParent, (DropTargetListener)FormDataTransferHandler.getInstance());

						DragStartTester dragTester = new DragStartTester();
						changedParent.addMouseListener(dragTester);
						changedParent.addMouseMotionListener(dragTester);
						TableView.this.removeHierarchyListener(this);
					}
				}
			});
		}
	}

	/**
	 * @param convertMouseEvent
	 */
	private void exportDrag(MouseEvent e)
	{
		boolean isCTRLDown = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
		TransferHandler handler = getTransferHandler();
		handler.exportAsDrag(TableView.this, e, isCTRLDown ? TransferHandler.COPY : TransferHandler.MOVE);
	}

	private class DragStartTester extends MouseAdapter implements MouseMotionListener
	{
		boolean startDrag = false;

		@Override
		public void mouseMoved(MouseEvent e)
		{
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			if (startDrag) exportDrag(SwingUtilities.convertMouseEvent((Component)e.getSource(), e, TableView.this));
			startDrag = false;
		}

		/**
		 * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
		 */
		@Override
		public void mousePressed(MouseEvent e)
		{
			startDrag = true;
		}
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
		return cellview instanceof Portal ? IRuntimeComponent.PORTAL : IRuntimeComponent.FORM;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderWrapper#getOnRenderToString()
	 */
	public String getOnRenderToString()
	{
		return cellview.toString();
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		boolean isCellEditable = super.isCellEditable(row, column);
		if (isCellEditable)
		{
			Object o = getCellEditor(row, column);
			if (o instanceof CellAdapter && ((CellAdapter)o).getEditor() instanceof IScriptableProvider &&
				((IScriptableProvider)((CellAdapter)o).getEditor()).getScriptObject() instanceof ISupportOnRenderCallback)
			{
				Object value = getValueAt(row, column);
				boolean isSelected = isCellSelected(row, column);
				Component c = ((CellAdapter)o).getTableCellEditorComponent(this, value, isSelected, row, column);
				if (c instanceof IScriptableProvider)
				{
					IScriptable scriptable = ((IScriptableProvider)c).getScriptObject();
					if (scriptable instanceof ISupportOnRenderCallback)
					{
						RenderEventExecutor renderEventExecutor = ((ISupportOnRenderCallback)scriptable).getRenderEventExecutor();
						if (renderEventExecutor != null)
						{
							renderEventExecutor.fireOnRender(false);
							isCellEditable = c.isEnabled();
						}
					}
				}
			}
		}

		return isCellEditable;
	}

	public void setRowStyles(IStyleSheet styleSheet, IStyleRule oddStyle, IStyleRule evenStyle, IStyleRule selectedStyle, IStyleRule headerStyle)
	{
		this.styleSheet = styleSheet;
		this.oddStyle = oddStyle;
		this.evenStyle = evenStyle;
		this.selectedStyle = selectedStyle;
		this.headerStyle = headerStyle;

		// if we have border css styling ignore table lines
		if (styleSheet != null && ((oddStyle != null && styleSheet.hasBorder(oddStyle)) || (evenStyle != null && styleSheet.hasBorder(evenStyle)) ||
			(selectedStyle != null && styleSheet.hasBorder(selectedStyle))))
		{
			setShowHorizontalLines(false);
			setShowVerticalLines(false);
			setIntercellSpacing(new Dimension(0, 0));
		}
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#getOddStyle()
	 */
	public IStyleRule getRowOddStyle()
	{
		return oddStyle;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#getEvenStyle()
	 */
	public IStyleRule getRowEvenStyle()
	{
		return evenStyle;
	}


	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#getStyleSheet()
	 */
	public IStyleSheet getRowStyleSheet()
	{
		return styleSheet;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportRowStyling#getSelectedStyle()
	 */
	public IStyleRule getRowSelectedStyle()
	{
		return selectedStyle;
	}

	public IStyleRule getHeaderStyle()
	{
		return headerStyle;
	}

	@Override
	protected JTableHeader createDefaultTableHeader()
	{
		return new UnmovableTableHeader(columnModel);
	}

	public List<CellAdapter> getNonViewableColumns()
	{
		return nonViewableColumns;
	}

	class UnmovableTableHeader extends JTableHeader
	{
		int[] unmovableColumnIndexes;
		boolean isMouseDown;
		int mouseStartX;

		UnmovableTableHeader(TableColumnModel model)
		{
			super(model);
		}

		public void setUnmovableColumns(int[] unmovableColumnIndexes)
		{
			this.unmovableColumnIndexes = unmovableColumnIndexes;
		}

		boolean isUnmovableColumn(int viewIndex)
		{
			if (unmovableColumnIndexes != null)
			{
				for (int unmovableColumnIdx : unmovableColumnIndexes)
				{
					if (unmovableColumnIdx == viewIndex) return true;
				}
			}
			return false;
		}

		void dragStart()
		{
			mouseStartX = (int)MouseInfo.getPointerInfo().getLocation().getX();
			isMouseDown = true;
		}

		void dragStop()
		{
			isMouseDown = false;
		}

		@Override
		public TableColumn getDraggedColumn()
		{
			if (isMouseDown)
			{
				TableColumn draggedCol = super.getDraggedColumn();
				if (draggedCol != null)
				{
					int modelIndex = draggedCol.getModelIndex();
					int index = getTable().convertColumnIndexToView(modelIndex);

					if (isUnmovableColumn(index))
					{
						setDraggedColumn(null);
					}
					else
					{
						int mouseDelta = (int)MouseInfo.getPointerInfo().getLocation().getX() - mouseStartX;
						if (mouseDelta != 0)
						{
							int direction = (mouseDelta < 0) ? -1 : 1;
							boolean headerLeftToRight = getComponentOrientation().isLeftToRight();
							int newIndex = index + (headerLeftToRight ? direction : -direction);
							if (isUnmovableColumn(newIndex))
							{
								setDraggedColumn(null);
								repaint();
							}
						}
					}
				}
			}
			return super.getDraggedColumn();
		}
	}

	/*
	 * @see com.servoy.j2db.dnd.IFormDataDragNDrop#getDragNDropController()
	 */
	public FormController getDragNDropController()
	{
		return fc;
	};
}
