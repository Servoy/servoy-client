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
 with this program; if not, see http:/www.gnu.org/licenses or write to the Free
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.wicket.Component;
import org.mozilla.javascript.Scriptable;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IProvideTabSequence;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IView;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.dnd.DRAGNDROP;
import com.servoy.j2db.dnd.JSDNDEvent;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.ISupportSize;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.TabSeqComparator;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.WrapperContainer;
import com.servoy.j2db.ui.DataRendererOnRenderWrapper;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IPortalComponent;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptRenderMethods;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.ISupportRowStyling;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.PropertyCopy;
import com.servoy.j2db.ui.RenderableWrapper;
import com.servoy.j2db.ui.runtime.HasRuntimeClientProperty;
import com.servoy.j2db.ui.runtime.HasRuntimeReadOnly;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.scripting.AbstractRuntimeBaseComponent;
import com.servoy.j2db.ui.scripting.RuntimePortal;
import com.servoy.j2db.util.AppendingStringBuffer;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.OrientationApplier;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * This class is normally used to show a portal or tableview
 *
 * @author jblok
 */
public class WebCellBasedView extends Component implements IView, IPortalComponent, IDataRenderer, IProviderStylePropertyChanges,
	ListSelectionListener, ISupportWebBounds, ISupportWebTabSeq, ISupportRowStyling, IProvideTabSequence
{
	private static final int SCROLLBAR_SIZE = 17;
	private static final long serialVersionUID = 1L;
	private final double NEW_PAGE_MULITPLIER;

	private final LinkedHashMap<IPersist, Component> elementToColumnIdentifierComponent = new LinkedHashMap<>(); // IPersist -> column identifier components - used by JavaScript
	private final HashMap<IPersist, Integer> elementTabIndexes = new HashMap<>();
	private final LinkedHashMap<Component, IPersist> cellToElement = new LinkedHashMap<>(); // each cell component -> IPersist (on the form)
	private final Map<IPersist, Component> elementToColumnHeader = new HashMap<>(); // links each column identifier component
	// to a column header component (if such a component exists)

	private final Map<IPersist, Map<Object, Object>> elementToClientProperties = new HashMap<>();

	private String relationName;
	private List<SortColumn> defaultSort = null;

	private final Component tableContainerBody;
	private final WebCellBasedViewListView table;
	boolean showPageNavigator = true;
	private DataAdapterList dal;

	private final Map<String, Boolean> initialSortColumnNames = new HashMap<>();
	private final Map<String, Boolean> initialSortedColumns = new HashMap<>();

	private final boolean addHeaders;

	private int tabIndex;

	private final IApplication application;
	private final AbstractBase cellview;
	protected final FormController fc;
	private final int startY, endY, sizeHint, formDesignHeight;
	private int maxHeight;
	private int bodyHeightHint = -1;
	private int bodyWidthHint = -1;

	private final boolean useAJAX, useAnchors;
	private Component resizedComponent; // the component that has been resized because of a column resize

	private final ISupportOnRenderCallback dataRendererOnRenderWrapper;
	private IStyleSheet styleSheet;
	private IStyleRule oddStyle, evenStyle, selectedStyle, headerStyle;

	private boolean bodySizeHintSetFromClient;
	private Component loadingInfo; // used to show loading info when rendering is postponed waiting for size info response from browser\
	private String lastRenderedPath;
	private boolean isAnchored;
	private final RuntimePortal scriptable;

	private boolean isScrollMode;
	private int maxRowsPerPage;

	private boolean isScrollFirstShow = true;
	// hide the body of this table/list (in case isKeepLoadedRowsInScrollMode == false) on initial render to avoid a possible flicker effect due to initial show followed by quick scroll and empty space divs being added
	boolean displayNoneUntilAfterRender = false;
	private boolean isKeepLoadedRowsInScrollMode;
	private boolean hasTopBuffer, hasBottomBuffer;
	private int currentScrollTop;
	private int currentScrollLeft;
	private int topPhHeight;
	private int scrollableHeaderHeight;
	public boolean selectionChanged = false;

	private boolean isLeftToRightOrientation;
	private Dimension formBodySize;

	private boolean isListViewMode;

	private StringBuilder javascriptForScrollBehaviorRenderHead;

	private final boolean isRendering = false;


	public static class CellContainer extends Component
	{
		private final Component childComp;

		public CellContainer(Component childComp)
		{
			super(childComp.getId() + '_');
			this.childComp = childComp;
		}

		public static Component getContentsForCell(Component child)
		{
			if (child instanceof CellContainer)
			{
				Iterator< ? extends Component> it = child.iterator();
				if (it.hasNext())
				{
					return it.next();
				}
				Debug.log("Strange - CellContainer with no child..."); //$NON-NLS-1$
			}

			return child instanceof WrapperContainer ? ((WrapperContainer)child).getDelegate() : child;
		}
	}

	interface ItemAdd
	{
		void add(IPersist element, Component comp);
	}

	/*
	 * (Persist, ColumnIdentifierComponent) used to compare based on X position; if ColumnIdentifierComponent X position is not available, the persist position
	 * is used
	 */
	private static class PersistColumnIdentifierComponent implements Comparable<PersistColumnIdentifierComponent>
	{
		private final IPersist persist;
		private final IComponent component;

		public PersistColumnIdentifierComponent(IPersist persist, IComponent component)
		{
			this.persist = persist;
			this.component = component;
		}

		public IPersist getPersist()
		{
			return persist;
		}

		public IComponent getComponent()
		{
			return component;
		}

		public int compareTo(PersistColumnIdentifierComponent pc)
		{
			if (pc == null) return -1;
			IComponent c = pc.getComponent();

			Point componentLocation = component.getLocation();
			if (componentLocation == null)
			{
				componentLocation = ((ISupportBounds)persist).getLocation();
			}

			Point cLocation = c.getLocation();
			if (cLocation == null)
			{
				cLocation = ((ISupportBounds)pc.getPersist()).getLocation();
			}

			return PositionComparator.comparePoint(true, componentLocation, cLocation);
		}
	}

	private class WebCellBasedViewListView extends ServoyListView<IRecordInternal>
	{
		private final AbstractBase listCellview;
		private final IDataProviderLookup dataProviderLookup;
		private final IScriptExecuter el;
		private final int listStartY, listEndY;
		private final Form form;

		private final ArrayList<IRecordInternal> removedListItems = new ArrayList<IRecordInternal>();

		public WebCellBasedViewListView(String id, int rowsPerPage, AbstractBase cellview,
			IDataProviderLookup dataProviderLookup, IScriptExecuter el, int startY, int endY, Form form)
		{
			super(id, rowsPerPage);
			this.listCellview = cellview;
			this.dataProviderLookup = dataProviderLookup;
			this.el = el;
			this.listStartY = startY;
			this.listEndY = endY;
			this.form = form;
		}

		/**
		 * Create a new ListItem for list item at index.
		 *
		 * @param index
		 * @return ListItem
		 */
		protected Component newItem(final int index)
		{
			if (WebCellBasedView.this.addHeaders)
			{
				return new ReorderableListItem(this, index);
			}
			else
			{
				return new WebCellBasedViewListItem(this, index);
			}
		}

		private boolean isRecordSelected(IRecordInternal rec)
		{
			if (rec == null)
			{
				return false;
			}
			IFoundSetInternal parentFoundSet = rec.getParentFoundSet();
			return Arrays.binarySearch(parentFoundSet.getSelectedIndexes(), parentFoundSet.getRecordIndex(rec)) >= 0;
		}

		private void setUpComponent(Component comp, IRecordInternal record, Object compColor, Object fgColor, Object compFont, Object listItemBorder,
			int visibleRowIndex)
		{

			if (scriptable.isReadOnly() && validationEnabled && comp instanceof IScriptableProvider &&
				((IScriptableProvider)comp).getScriptObject() instanceof HasRuntimeReadOnly) // if in find mode, the field should not be readonly
			{
				((HasRuntimeReadOnly)((IScriptableProvider)comp).getScriptObject()).setReadOnly(true);
			}

			if (!isEnabled() && comp instanceof IComponent)
			{
				comp.setComponentEnabled(false);
			}
			if (comp instanceof IDisplayRelatedData && record != null)
			{
				((IDisplayRelatedData)comp).setRecord(record, true);
			}

			Component parent = comp.getParent();
			if (parent instanceof CellContainer)
			{
				// apply properties that need to be applied to <td> tag instead
				parent.setVisible(comp.isVisible());
			}
		}

		public Component getOrCreateListItem(int index)
		{
			return new Component("" + index);
		}

		/**
		 * @return
		 */
		public int getStartIndex()
		{
			// TODO Auto-generated method stub
			return 0;
		}

	}

	public class WebCellBasedViewListViewItem extends Component implements IProviderStylePropertyChanges
	{
		public Component listItem;
		private final IStylePropertyChanges changesRecorder = new ChangesRecorder();

		public WebCellBasedViewListViewItem(Component listItem)
		{
			super("listViewItem"); //$NON-NLS-1$
			this.listItem = listItem;

		}


		public IStylePropertyChanges getStylePropertyChanges()
		{
			return changesRecorder;
		}

	}

	private class WebCellBasedViewListItem extends Component
	{
		private Component listContainer;
		protected WebCellBasedViewListView parentView;
		private final String id;

		public WebCellBasedViewListItem(WebCellBasedViewListView parentView, int index)
		{
			super("" + index);
			this.parentView = parentView;
			id = WebCellBasedView.this.getMarkupId() + '_' + super.getMarkupId();
		}

		@Override
		public String getMarkupId()
		{
			return id;
		}

		public Component getListContainer()
		{
			if (listContainer == null)
			{
				if (isListViewMode())
				{
					listContainer = new WebCellBasedViewListViewItem(this);

					add(listContainer);
				}
				else listContainer = this;
			}
			return listContainer;
		}
	}

	private class ReorderableListItem extends WebCellBasedViewListItem
	{
		public ReorderableListItem(WebCellBasedViewListView parentView, int index)
		{
			super(parentView, index);
		}

		/**
		 * Renders markup for the body of a ComponentTag from the current position in the given markup stream. If the open tag passed in does not require a
		 * close tag, nothing happens. Markup is rendered until the closing tag for openTag is reached.
		 *
		 * @param markupStream The markup stream
		 * @param openTag The open tag
		 */

		private int renderColumnIdx;
		private int headerMarkupStartIdx;
		private List<Component> orderedHeaders;

	}

	interface JSAppendTarget
	{
		void appendJavascript(String javascript);

		boolean isBeforeRender();
	}

	public WebCellBasedView(final String id, final IApplication application, RuntimePortal scriptable, final Form form, final AbstractBase cellview,
		final IDataProviderLookup dataProviderLookup, final IScriptExecuter el, boolean addHeaders, final int startY, final int endY, final int sizeHint,
		int viewType)
	{
		super(id);
		this.application = application;
		this.cellview = cellview;
		this.fc = el.getFormController();
		this.addHeaders = addHeaders;
		this.startY = startY;
		this.endY = endY;
		this.formDesignHeight = form.getSize().height;
		this.sizeHint = sizeHint;
		this.isListViewMode = viewType == IForm.LIST_VIEW || viewType == FormController.LOCKED_LIST_VIEW;
		this.bodyWidthHint = form.getWidth();

		double multiplier = Utils.getAsDouble(Settings.getInstance().getProperty("servoy.webclient.scrolling.tableview.multiplier", "2"));

		if (multiplier > 1.1)
		{
			NEW_PAGE_MULITPLIER = multiplier;
		}
		else
		{
			NEW_PAGE_MULITPLIER = 1.1;
		}

		useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		useAnchors = Utils.getAsBoolean(application.getRuntimeProperties().get("enableAnchors")); //$NON-NLS-1$

		// a cell based view should just never version itself if in ajax mode
		// (all things like next page or rerenders are done on the actual page not a version you can go back to (url doesn't change)
		isKeepLoadedRowsInScrollMode = Boolean.TRUE.equals(application.getClientProperty(IApplication.TABLEVIEW_WC_SCROLLABLE_KEEP_LOADED_ROWS));

		dataRendererOnRenderWrapper = new DataRendererOnRenderWrapper(this);

		loadingInfo = new Component("info"); //$NON-NLS-1$
		loadingInfo.setVisible(false);
		add(loadingInfo);

		if (!useAJAX) bodyHeightHint = sizeHint;

		String orientation = OrientationApplier.getHTMLContainerOrientation(application.getLocale(), application.getSolution().getTextOrientation());
		isLeftToRightOrientation = !OrientationApplier.RTL.equalsIgnoreCase(orientation);

		int tFormHeight = 0;
		Iterator<Part> partIte = form.getParts();
		while (partIte.hasNext())
		{
			Part p = partIte.next();
			if (p.getPartType() == Part.BODY)
			{
				tFormHeight = p.getHeight() - startY;
				break;
			}
		}
		formBodySize = new Dimension(form.getWidth(), tFormHeight);


		ChangesRecorder jsChangeRecorder = new ChangesRecorder(null, null)
		{
			@Override
			public boolean isChanged()
			{
				boolean retval = false;
				if (super.isChanged())
				{
					retval = true;
					//TODO: change this; we should not do this, but it is not re-rendered otherwise
					return retval;
				}

				if (!retval)
				{
					for (Component object : elementToColumnIdentifierComponent.values())
					{
						if (object instanceof IProviderStylePropertyChanges)
						{
							if (((IProviderStylePropertyChanges)object).getStylePropertyChanges().isChanged())
							{
								retval = true;
								break;
							}
						}
					}
				}
				return false;
			}

			@Override
			public void setRendered()
			{
				super.setRendered();
				for (Component comp : elementToColumnIdentifierComponent.values())
				{
					if (comp instanceof IProviderStylePropertyChanges)
					{
						((IProviderStylePropertyChanges)comp).getStylePropertyChanges().setRendered();
					}
				}
			}
		};
		this.scriptable = scriptable;
		((ChangesRecorder)scriptable.getChangesRecorder()).setAdditionalChangesRecorder(jsChangeRecorder);

		final int scrollbars = (cellview instanceof ISupportScrollbars) ? ((ISupportScrollbars)cellview).getScrollbars() : 0;
		if (cellview instanceof BaseComponent)
		{
			ComponentFactory.applyBasicComponentProperties(application, this, (BaseComponent)cellview,
				ComponentFactory.getStyleForBasicComponent(application, cellview, form));
		}

		boolean sortable = true;
		String initialSortString = null;
		String onRenderMethodUUID = null;
		AbstractBase onRenderPersist = null;
		if (cellview instanceof Portal)
		{
			Portal p = (Portal)cellview;
			isListViewMode = p.getMultiLine();

			setRowBGColorScript(p.getRowBGColorCalculation(), p.getFlattenedMethodArguments("rowBGColorCalculation")); //$NON-NLS-1$
			sortable = p.getSortable();
			initialSortString = p.getInitialSort();
			onRenderMethodUUID = p.getOnRenderMethodID();
			onRenderPersist = p;

			int portalAnchors = p.getAnchors();
			isAnchored = (((portalAnchors & IAnchorConstants.NORTH) > 0) && ((portalAnchors & IAnchorConstants.SOUTH) > 0)) ||
				(((portalAnchors & IAnchorConstants.EAST) > 0) && ((portalAnchors & IAnchorConstants.WEST) > 0));
		}
		else if (cellview instanceof Form)
		{
			initialSortString = form.getInitialSort();
			onRenderMethodUUID = form.getOnRenderMethodID();
			onRenderPersist = form;
			isAnchored = true;
		}

		if (onRenderMethodUUID != null)
		{
			dataRendererOnRenderWrapper.getRenderEventExecutor().setRenderCallback(onRenderMethodUUID,
				Utils.parseJSExpressions(onRenderPersist.getFlattenedMethodArguments("onRenderMethodID")));
			dataRendererOnRenderWrapper.getRenderEventExecutor().setRenderScriptExecuter(fc != null ? fc.getScriptExecuter() : null);
		}

		initDragNDrop(fc, startY);

		if (sortable)
		{
			if (initialSortString != null)
			{
				StringTokenizer tokByComma = new StringTokenizer(initialSortString, ","); //$NON-NLS-1$
				while (tokByComma.hasMoreTokens())
				{
					String initialSortFirstToken = tokByComma.nextToken();
					StringTokenizer tokBySpace = new StringTokenizer(initialSortFirstToken);
					if (tokBySpace.hasMoreTokens())
					{
						String initialSortColumnName = tokBySpace.nextToken();

						if (tokBySpace.hasMoreTokens())
						{
							String sortDir = tokBySpace.nextToken();
							boolean initialSortAsc = true;
							if (sortDir.equalsIgnoreCase("DESC")) initialSortAsc = false; //$NON-NLS-1$
							initialSortColumnNames.put(initialSortColumnName, new Boolean(initialSortAsc));
						}
					}
				}
			}
			// If no initial sort was specified, then default will be the first PK column.
			if (initialSortColumnNames.size() == 0)
			{
				try
				{
					String dataSource = null;
					if (cellview instanceof Portal)
					{
						Portal p = (Portal)cellview;
						String relation = p.getRelationName();
						int lastDot = relation.lastIndexOf("."); //$NON-NLS-1$
						if (lastDot >= 0)
						{
							relation = relation.substring(lastDot + 1);
						}
						Relation rel = application.getFlattenedSolution().getRelation(relation);
						if (rel != null)
						{
							dataSource = rel.getForeignDataSource();
						}
					}
					else
					{
						dataSource = form.getDataSource();
					}
					if (dataSource != null)
					{
						Iterator<String> pkColumnNames = application.getFoundSetManager().getTable(dataSource).getRowIdentColumnNames();
						while (pkColumnNames.hasNext())
						{
							initialSortColumnNames.put(pkColumnNames.next(), Boolean.TRUE);
						}
					}
				}
				catch (RepositoryException e)
				{
					// We just don't set the initial sort to the PK.
					Debug.log("Failed to get PK columns for table.", e); //$NON-NLS-1$
				}
			}
		}

		maxHeight = 0;
		try
		{
			if (isListViewMode() && !(cellview instanceof Portal))
			{
				int headerHeight = 0;
				int titleHeaderHeight = 0;
				Iterator<Part> pIte = form.getParts();
				while (pIte.hasNext())
				{
					Part p = pIte.next();
					if (p.getPartType() == Part.BODY)
					{
						maxHeight = p.getHeight();
					}
					else if (p.getPartType() == Part.HEADER)
					{
						headerHeight = p.getHeight();
					}
					else if (p.getPartType() == Part.TITLE_HEADER)
					{
						titleHeaderHeight = p.getHeight();
					}
				}
				if (headerHeight != 0)
				{
					maxHeight = maxHeight - headerHeight;
				}
				else
				{
					maxHeight = maxHeight - titleHeaderHeight;
				}
			}
			else
			{
				int minElY = 0;
				boolean isMinElYSet = false;
				int height;
				Iterator<IPersist> components = cellview.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
				while (components.hasNext())
				{
					IPersist element = components.next();
					if (element instanceof Field || element instanceof GraphicalComponent)
					{
						if (element instanceof GraphicalComponent && ((GraphicalComponent)element).getLabelFor() != null)
						{
							if (isInView(cellview, ((GraphicalComponent)element).getLabelFor()))
							{
								labelsFor.put(((GraphicalComponent)element).getLabelFor(), element);
							}
							continue;
						}
						Point l = ((IFormElement)element).getLocation();
						if (l == null)
						{
							continue;// unknown where to add
						}
						if (l.y >= startY && l.y < endY)
						{

							if (isListViewMode())
							{
								height = l.y + ((IFormElement)element).getSize().height;
								if (!isMinElYSet || minElY > l.y)
								{
									minElY = l.y;
									isMinElYSet = true;
								}
							}
							else
							{
								height = ((IFormElement)element).getSize().height;
							}
							if (height > maxHeight) maxHeight = height;
						}
					}
				}
				maxHeight = maxHeight - minElY;
			}
			if (maxHeight == 0) maxHeight = 20;
		}
		catch (Exception ex1)
		{
			Debug.error("Error getting max size out of components", ex1); //$NON-NLS-1$
		}

		// Add the table
		tableContainerBody = new Component("rowsContainerBody"); //$NON-NLS-1$


		table = new WebCellBasedViewListView("rows", 1, cellview, //$NON-NLS-1$
			dataProviderLookup, el, startY, endY, form);


		tableContainerBody.add(table);

		add(tableContainerBody);

		final LinkedHashMap<String, IDataAdapter> dataadapters = new LinkedHashMap<String, IDataAdapter>();
		final SortedList<IPersist> columnTabSequence = new SortedList<IPersist>(TabSeqComparator.INSTANCE); // in fact ISupportTabSeq persists
		createComponents(application, form, cellview, dataProviderLookup, el, startY, endY, new ItemAdd()
		{
			public void add(IPersist element, Component comp)
			{

				if (element instanceof IFormElement && comp instanceof IComponent)
				{
					comp.setLocation(((IFormElement)element).getLocation());
					comp.setSize(((IFormElement)element).getSize());
				}

				elementToColumnIdentifierComponent.put(element, comp);
				if (cellview instanceof Form && element instanceof ISupportTabSeq && ((ISupportTabSeq)element).getTabSeq() >= 0)
				{
					columnTabSequence.add(element);
				}
				if (comp instanceof IDisplayData)
				{
					String dataprovider = ((IDisplayData)comp).getDataProviderID();

					WebCellAdapter previous = (WebCellAdapter)dataadapters.get(dataprovider);
					if (previous == null)
					{
						WebCellAdapter wca = new WebCellAdapter(dataprovider, WebCellBasedView.this);
						dataadapters.put(dataprovider, wca);
					}
					if (dataprovider != null)
					{
						if (initialSortColumnNames.containsKey(dataprovider)) initialSortedColumns.put(comp.getId(), initialSortColumnNames.get(dataprovider));
					}
				}
			}
		});
		for (int i = columnTabSequence.size() - 1; i >= 0; i--)
		{
			elementTabIndexes.put(columnTabSequence.get(i), Integer.valueOf(i));
		}

		//hide all further records (and navigator) if explicitly told that there should be no vertical scrollbar
		showPageNavigator = !((scrollbars & ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) == ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER);

		try
		{
			if (cellview instanceof Portal)
			{
				relationName = ((Portal)cellview).getRelationName();
				Relation[] rels = application.getFlattenedSolution().getRelationSequence(((Portal)cellview).getRelationName());

				if (rels != null)
				{
					Relation r = rels[rels.length - 1];
					if (r != null)
					{
						defaultSort = ((FoundSetManager)application.getFoundSetManager()).getSortColumns(
							application.getFoundSetManager().getTable(r.getForeignDataSource()), ((Portal)cellview).getInitialSort());
					}
				}
			}
			else
			{
				defaultSort = ((FoundSetManager)application.getFoundSetManager()).getSortColumns(((Form)cellview).getDataSource(),
					((Form)cellview).getInitialSort());
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
			defaultSort = new ArrayList<SortColumn>(1);
		}

		try
		{
			dal = new DataAdapterList(application, dataProviderLookup, elementToColumnIdentifierComponent, el.getFormController(), dataadapters, null);
		}
		catch (RepositoryException ex)
		{
			Debug.error(ex);
		}

		table.setPageabeMode(!isScrollMode());

	}

	/**
	 * Check if the element which has the label is in the table view (body part).
	 * @return true if the element is in the table view, false otherwise
	 */
	private boolean isInView(AbstractBase abstractBase, String labelFor)
	{
		if (labelFor != null && !labelFor.equals(""))
		{
			Iterator<IPersist> components = abstractBase.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			while (components.hasNext())
			{
				IPersist element = components.next();
				if (element instanceof ISupportName && labelFor.equals(((ISupportName)element).getName()) && element instanceof ISupportBounds &&
					element instanceof ISupportSize)
				{
					Point loc = ((ISupportBounds)element).getLocation();
					return loc.y >= startY && loc.y < endY;
				}
			}
		}
		return false;
	}

	private static String scrollBarDefinitionToOverflowAttribute(int scrollbarDefinition, boolean isScrollMode, boolean isScrollingElement, boolean emptyData)
	{
		String overflow = "";
		if (isScrollMode && !isScrollingElement)
		{
			if (emptyData && (scrollbarDefinition == 0 ||
				(scrollbarDefinition & ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED))
			{
				// special situation, we have no content so the content element doesn't have scrollbars
				// however maybe scrollbars are needed if many columns, so put auto on main element as well
				overflow += "overflow-x: auto;"; //$NON-NLS-1$
			}
			else
			{
				overflow += "overflow-x: hidden;"; //$NON-NLS-1$
			}
		}
		else if ((scrollbarDefinition & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER)
		{
			overflow += "overflow-x: hidden;"; //$NON-NLS-1$
		}
		else if ((scrollbarDefinition & ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS)
		{
			overflow += "overflow-x: scroll;"; //$NON-NLS-1$
		}
		else
		{
			overflow += "overflow-x: auto;"; //$NON-NLS-1$
		}
		if (isScrollMode && !isScrollingElement)
		{
			overflow += "overflow-y: hidden;"; //$NON-NLS-1$
		}
		else if ((scrollbarDefinition & ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) == ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER)
		{
			overflow += "overflow-y: hidden;"; //$NON-NLS-1$
		}
		else if ((scrollbarDefinition & ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS)
		{
			overflow += "overflow-y: scroll;"; //$NON-NLS-1$
		}
		else
		{
			overflow += "overflow-y: auto;"; //$NON-NLS-1$
		}

		return overflow;
	}

	public final RuntimePortal getScriptObject()
	{
		return scriptable;
	}

	public void setTabSequenceIndex(int tabIndex)
	{
		this.tabIndex = tabIndex;
	}

	private final ArrayList<String> orderedHeaderIds = new ArrayList<String>();

	public ArrayList<Component> getOrderedHeaders()
	{
		ArrayList<Component> orderedHeaders = new ArrayList<Component>();
		List<PersistColumnIdentifierComponent> orderedPersistColumnIdentifierComponent = getOrderedPersistColumnIdentifierComponents();
		orderedHeaderIds.clear();

		Component c;
		for (PersistColumnIdentifierComponent pc : orderedPersistColumnIdentifierComponent)
		{
			c = elementToColumnHeader.get(pc.getPersist());
			if (c == null) orderedHeaders.add(null);
			else orderedHeaders.add(elementToColumnHeader.get(pc.getPersist()));
			orderedHeaderIds.add(pc.getComponent().getId());
		}

		return orderedHeaders;
	}

	public ArrayList<String> getOrderedHeaderIds()
	{
		return orderedHeaderIds;
	}

	private Component getIdentifierComponent(Component headerColumn)
	{
		for (Map.Entry<IPersist, Component> entry : elementToColumnHeader.entrySet())
		{
			if (entry.getValue() == headerColumn)
			{
				return elementToColumnIdentifierComponent.get(entry.getKey());
			}
		}
		return null;
	}

	private Component focusRequestingColIdentComponent = null;

	/**
	 * Requests focus for the cell in the web cell view corresponding to the selected record and to the given column identifier component.
	 *
	 * @param columnIdentifierComponent the Component that identifies a column for java script.
	 */
	public void setColumnThatRequestsFocus(final Component columnIdentifierComponent)
	{
		focusRequestingColIdentComponent = null;

		if (currentData == null) return;

		Component cell = getCellToFocus(columnIdentifierComponent);
		if (cell != null)
		{
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				((MainPage)currentContainer).componentToFocus(cell);
			}
			else
			{
				Debug.trace("focus couldnt be set on component " + cell); //$NON-NLS-1$
			}
		}
		else
		{
			focusRequestingColIdentComponent = columnIdentifierComponent;
		}
	}

	/**
	 * @param columnIdentifierComponent
	 * @return
	 */
	private Component getCellToFocus(final Component columnIdentifierComponent)
	{
		Component cell = null;

		// this means that the given column of the cell view wants to be focused =>
		// we must focus the cell component that is part of the currently selected record
		int selectedIndex = currentData.getSelectedIndex();
		if (selectedIndex < 0 && currentData.getSize() > 0)
		{
			selectedIndex = 0;
		}

		if (selectedIndex >= 0)
		{
			// we found a record to use - now we must locate the cell component inside this record
			Component li = table.get(Integer.toString(selectedIndex));
			if (li != null)
			{
				Iterator< ? extends Component> cells = li.iterator();
				while (cells.hasNext())
				{
					Component someCell = cells.next();
					if (isListViewMode())
					{
						if (someCell instanceof WebCellBasedViewListItem)
						{
							someCell = ((WebCellBasedViewListItem)someCell).getListContainer();
						}
						if (someCell instanceof Component)
						{
							for (int i = 0; i < someCell.size(); i++)
							{
								Component currentComponent = someCell.get(i + "");
								if (currentComponent instanceof WrapperContainer) currentComponent = ((WrapperContainer)currentComponent).getDelegate();
								IPersist element = cellToElement.get(currentComponent);
								if (element != null && elementToColumnIdentifierComponent.get(element) == columnIdentifierComponent)
								{
									return currentComponent;
								}
							}
						}
					}
					else
					{
						someCell = CellContainer.getContentsForCell(someCell);
						IPersist element = cellToElement.get(someCell);
						if (element != null && elementToColumnIdentifierComponent.get(element) == columnIdentifierComponent)
						{
							cell = someCell;
							break;
						}
					}
				}
			}
		}
		return cell;
	}

	Map<String, IPersist> labelsFor = new HashMap<String, IPersist>();

	private void createComponents(final IApplication app, final Form form, final AbstractBase view, final IDataProviderLookup dataProviderLookup,
		final IScriptExecuter el, final int viewStartY, final int viewEndY, final ItemAdd output)
	{
		List<IPersist> elements = ComponentFactory.sortElementsOnPositionAndGroup(view.getAllObjectsAsList());
		int startX = 0;
		for (IPersist element : elements)
		{
			if (element instanceof Field || element instanceof GraphicalComponent || element instanceof Bean)
			{
				if (!isListViewMode())
				{
					if (element instanceof GraphicalComponent && ((GraphicalComponent)element).getLabelFor() != null)
					{
						if (isInView(cellview, ((GraphicalComponent)element).getLabelFor()))
						{
							labelsFor.put(((GraphicalComponent)element).getLabelFor(), element);
						}
						continue;
					}
				}

				Point l = ((IFormElement)element).getLocation();
				if (l == null)
				{
					continue; // unknown where to add
				}

				if (l.y >= viewStartY && l.y < viewEndY)
				{
					IComponent c = ComponentFactory.createComponent(app, form, element, dataProviderLookup, el, false);

					if (cellview instanceof Portal && c instanceof IScriptableProvider)
					{
						IScriptable s = ((IScriptableProvider)c).getScriptObject();
						if (s instanceof ISupportOnRenderCallback && ((ISupportOnRenderCallback)s).getRenderEventExecutor() != null)
							ComponentFactoryHelper.addPortalOnRenderCallback((Portal)cellview, ((ISupportOnRenderCallback)s).getRenderEventExecutor(), element,
								fc != null ? fc.getScriptExecuter() : null);
					}

					initializeComponent((Component)c, view, element);
					output.add(element, (Component)c);

					if (!isListViewMode())
					{
						// reset location.x as defined in this order, elements are ordered by location.x which is modified in drag-n-drop
						Point loc = c.getLocation();
						if (loc != null)
						{
							c.setLocation(new Point(startX, loc.y));
						}

						Dimension csize = c.getSize();
						startX += (csize != null) ? csize.width : ((IFormElement)element).getSize().width;
					}
				}
			}
		}
	}

	@SuppressWarnings("nls")
	private void initializeComponent(final Component c, AbstractBase view, IPersist element)
	{
		if (dal != null && dal.isDestroyed())
		{
			Debug.error("Trying to initialize a component: " + c + " of " + view + " element: " + element + " that is in a destroyed tableview",
				new RuntimeException());
			return;
		}
		if (view instanceof Portal && c instanceof IDisplayData) // Don't know any other place for this
		{
			String id = ((IDisplayData)c).getDataProviderID();
			if (id != null && !ScopesUtils.isVariableScope(id) && id.startsWith(((Portal)view).getRelationName() + '.'))
			{
				((IDisplayData)c).setDataProviderID(id.substring(((Portal)cellview).getRelationName().length() + 1));
			}
		}
		if (!isListViewMode() && c instanceof WebDataCheckBox)
		{
			((WebDataCheckBox)c).setText(""); //$NON-NLS-1$
		}

		if (element != null)
		{
			// apply to this cell the state of the columnIdentifier IComponent, do keep the location that is set by the tableview when creating these components the first time.
			// for listview this is the location to use.
			Point loc = c.getLocation();
			int height = c.getSize().height;
			PropertyCopy.copyElementProps(elementToColumnIdentifierComponent.get(element), c);
			if (!isListViewMode())
			{
				c.setLocation(loc);
				//it shouldn't be possible to change the height
				if (c instanceof IScriptableProvider)
				{
					IScriptable so = ((IScriptableProvider)c).getScriptObject();
					if (so instanceof IRuntimeComponent)
					{
						IRuntimeComponent ic = (IRuntimeComponent)so;
						if (ic.getHeight() != height)
						{
							ic.setSize(ic.getWidth(), height);
						}
					}
				}
			}
		}
		else
		{
			Debug.log("Cannot find the IPersist element for cell " + c.getMarkupId()); //$NON-NLS-1$
		}
		if (c instanceof IDisplayData)
		{
			IDisplayData cdd = (IDisplayData)c;
			if (!(dal != null && dal.getFormScope() != null && cdd.getDataProviderID() != null &&
				dal.getFormScope().get(cdd.getDataProviderID()) != Scriptable.NOT_FOUND)) // skip for form variables
			{
				cdd.setValidationEnabled(validationEnabled);
			}
		}
		else if (c instanceof IDisplayRelatedData)
		{
			((IDisplayRelatedData)c).setValidationEnabled(validationEnabled);
		}
		else if (c instanceof IServoyAwareBean)
		{
			((IServoyAwareBean)c).setValidationEnabled(validationEnabled);
		}


		if (c instanceof ISupportValueList)
		{
			ISupportValueList idVl = (ISupportValueList)elementToColumnIdentifierComponent.get(element);
			IValueList list;
			if (idVl != null && (list = idVl.getValueList()) != null)
			{
				ValueList valuelist = application.getFlattenedSolution().getValueList(list.getName());
				if (valuelist != null && valuelist.getValueListType() == IValueListConstants.CUSTOM_VALUES)
				{
					((ISupportValueList)c).setValueList(list);
				}
			}
		}

		applyClientProperties(c, element);
	}

	/*
	 * Number of updated list items from the last rendering
	 */
	private int nrUpdatedListItems;

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
	}

	private ArrayList<Component> visibleColummIdentifierComponents;

	private ArrayList<Component> getVisibleColummIdentifierComponents()
	{
		ArrayList<Component> colummIdentifierComponents = new ArrayList<Component>();
		Component c;
		for (Component element : elementToColumnIdentifierComponent.values())
		{
			c = element;
			if (c.isVisible()) colummIdentifierComponents.add(c);
		}
		return colummIdentifierComponents;
	}

	public Object[] getComponents()
	{
		return elementToColumnIdentifierComponent.values().toArray();
	}

	public Object[] getHeaderComponents()
	{
		return elementToColumnHeader.values().toArray();
	}

	public WebCellBasedViewListView getTable()
	{
		return this.table;
	}

	public void destroy()
	{
		if (dal != null) dal.destroy();
		if (currentData instanceof ISwingFoundSet)
		{
			((ISwingFoundSet)currentData).getSelectionModel().removeListSelectionListener(this);
		}
		for (Component comp : cellToElement.keySet())
		{
			if (comp instanceof IDestroyable)
			{
				((IDestroyable)comp).destroy();
			}
		}
		cellToElement.clear();
	}

	public String getSelectedRelationName()
	{
		return relationName;
	}

	public String[] getAllRelationNames()
	{
		String selectedRelation = getSelectedRelationName();
		if (selectedRelation == null)
		{
			return new String[0];
		}
		else
		{
			return new String[] { selectedRelation };
		}
	}

	public List<SortColumn> getDefaultSort()
	{
		if (currentData != null && defaultSort.size() == 0)
		{
			defaultSort = currentData.getSortColumns();
		}
		return defaultSort;
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		dal.notifyVisible(b, invokeLaterRunnables);
	}

	public void setRecord(IRecordInternal state, boolean stopEditing)
	{
		if (stopEditing)
		{
			stopUIEditing(true);
		}
		setModel(state == null ? null : state.getRelatedFoundSet(relationName, getDefaultSort()));
	}

	private IFoundSetInternal currentData;
	private boolean isCurrentDataChanged;
	private int[] selectedIndexes;
	private int[] selectedIndexesBeforUpdate; // used by getRowSelectionScript
	private String bgColorScript;
	private List<Object> bgColorArgs;

	private boolean isReadOnly;

	private boolean validationEnabled = true;

	public void setModel(IFoundSetInternal fs)
	{
		if (currentData == fs) return;// if is same changes are seen by model listener

		if (currentData instanceof ISwingFoundSet)
		{
			((ISwingFoundSet)currentData).getSelectionModel().removeListSelectionListener(this);
			// ListSelectionModel lsm = currentData.getSelectionModel();
			// lsm.removeListSelectionListener(this);
		}

		currentData = fs;
		isCurrentDataChanged = true;
		getStylePropertyChanges().setChanged();
		if (currentData == null)
		{
			// table.setSelectionModel(new DefaultListSelectionModel());
			// table.setModel(new DefaultTableModel());
		}
		else
		{
			// ListSelectionModel lsm = currentData.getSelectionModel();

			// int selected = currentData.getSelectedIndex();
			// table.setSelectionModel(lsm);
			// table.setModel((TableModel)currentData);
			// currentData.setSelectedIndex(selected);

			if (currentData instanceof ISwingFoundSet)
			{
				((ISwingFoundSet)currentData).getSelectionModel().addListSelectionListener(this);
			}
			// lsm.addListSelectionListener(this);

			// valueChanged(null,stopEditing);
		}
		scriptable.setFoundset(currentData);


		if (isScrollMode()) resetScrollParams();
	}

	private boolean isSelectionByCellAction;

	public void setSelectionMadeByCellAction()
	{
		isSelectionByCellAction = true;
	}

	public void clearSelectionByCellActionFlag()
	{
		isSelectionByCellAction = false;
	}

	public boolean isSelectionByCellAction()
	{
		return isSelectionByCellAction;
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (currentData != null && !e.getValueIsAdjusting())
		{
			boolean isTableChanged = false;
			if (!isScrollFirstShow) selectionChanged = true;
			//in case selection changed outside of an action on the component, and it's a list view with left-bar selection mark (so, no selection color),
			// we need to re-render the view
		}

		if (!isScrollMode()) //test if selection did move to another page
		{
			int newSelectedIndex = currentData.getSelectedIndex();
			int newPageIndex = newSelectedIndex / table.getRowsPerPage();
			if (table.getCurrentPage() != newPageIndex)
			{
				// try to lock the page of this cellbasedview, so that concurrent rendering can't or won't happen.
				table.setCurrentPage(newPageIndex);
				// if table row selection color must work then this must be outside this if.
			}
		}
	}


	public void setValidationEnabled(boolean b)
	{
		if (validationEnabled != b)
		{
			// find mode / edit mode switch
			if (isScrollMode()) resetScrollParams();
			getStylePropertyChanges().setChanged();
		}
		validationEnabled = b;
		dal.setFindMode(!b);

	}

	public boolean stopUIEditing(final boolean looseFocus)
	{
		return true;
	}

	@Override
	public void setCursor(Cursor cursor)
	{
	}

	public String getRowBGColorScript()
	{
		return bgColorScript;
	}

	public List<Object> getRowBGColorArgs()
	{
		return bgColorArgs;
	}

	public void setRowBGColorScript(String bgColorScript, List<Object> args)
	{
		this.bgColorScript = bgColorScript;
		this.bgColorArgs = args;
	}

	public boolean editCellAt(int i)
	{
		return false;
	}

	public boolean isEditing()
	{
		return false;
	}

	public void requestFocus()
	{
	}

	public void start(IApplication app)
	{
	}

	public void stop()
	{
	}

	public void ensureIndexIsVisible(int index)
	{
	}

	public Rectangle getVisibleRect()
	{
		return null;
	}

	public void setVisibleRect(Rectangle scrollPosition)
	{

	}

	public boolean isDisplayingMoreThanOneRecord()
	{
		return true;
	}

	public void setEditable(boolean findMode)
	{
	}


	public void setRecordIndex(int i)
	{
		if (currentData != null)
		{
			currentData.setSelectedIndex(i);
		}
	}

	/*
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return isReadOnly;
	}

	public void setReadOnly(boolean b)
	{
		isReadOnly = b;
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

//	public boolean js_isTransparent()
//	{
//		return !opaque;
//	}
//	public void js_setTransparent(boolean b)
//	{
//		opaque = !b;
//		jsChangeRecorder.setTransparent(b);
//	}
	@Override
	public boolean isOpaque()
	{
		return opaque;
	}


	/*
	 * tooltip---------------------------------------------------
	 */
//	public String js_getToolTipText()
//	{
//		return tooltip;
//	}
	private String tooltip;

	@Override
	public void setToolTipText(String tooltip)
	{
		if (Utils.stringIsEmpty(tooltip))
		{
			this.tooltip = null;
		}
		else
		{
			this.tooltip = tooltip;
		}
	}

//	public void js_setToolTipText(String tooltip)
//	{
//		this.tooltip = tooltip;
//	}


	/*
	 * font---------------------------------------------------
	 */
	@Override
	public void setFont(Font font)
	{
		this.font = font;
	}

	private Font font;

//	public void js_setFont(String spec)
//	{
//		font = PersistHelper.createFont(spec);
//		jsChangeRecorder.setFont(spec);
//	}
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

	/**
	 * @see wicket.Component#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		return super.isEnabled() || !validationEnabled;
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
		this.size = size;
	}

	/**
	 * @see com.servoy.j2db.ui.IDataRenderer#addDisplayComponent(com.servoy.j2db.persistence.IPersist, com.servoy.j2db.dataprocessing.IDisplay)
	 */
	public void addDisplayComponent(IPersist obj, IDisplay display)
	{
		//ignore
	}

	private ArrayList<PersistColumnIdentifierComponent> getOrderedPersistColumnIdentifierComponents()
	{
		ArrayList<PersistColumnIdentifierComponent> orderedPersistColumnIdentifierComponent = new ArrayList<PersistColumnIdentifierComponent>();

		for (Entry<IPersist, Component> entry : elementToColumnIdentifierComponent.entrySet())
		{
			orderedPersistColumnIdentifierComponent.add(new PersistColumnIdentifierComponent(entry.getKey(), entry.getValue()));
		}
		Collections.sort(orderedPersistColumnIdentifierComponent);
		return orderedPersistColumnIdentifierComponent;
	}

	public void focusFirstField()
	{
		// find column that should get focus
		ArrayList<PersistColumnIdentifierComponent> orderedPersistColumnIdentifierComponent = getOrderedPersistColumnIdentifierComponents();
		Component firstFocusableColumnIdentifier = null;
		for (PersistColumnIdentifierComponent pci : orderedPersistColumnIdentifierComponent)
		{
			IComponent c = pci.getComponent();
			if (!(c instanceof WebBaseButton || c instanceof WebBaseLabel || !c.isEnabled() ||
				(validationEnabled && c instanceof IFieldComponent && !((IFieldComponent)c).isEditable())))
			{
				firstFocusableColumnIdentifier = (Component)c;
				break;
			}
		}
		if (firstFocusableColumnIdentifier != null) setColumnThatRequestsFocus(firstFocusableColumnIdentifier);
	}

	/**
	 * @see com.servoy.j2db.ui.IDataRenderer#getDataAdapterList()
	 */
	public DataAdapterList getDataAdapterList()
	{
		return dal;
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
	}

	/**
	 * @see com.servoy.j2db.ui.IDataRenderer#setAllNonFieldsEnabled(boolean)
	 */
	public void setAllNonFieldsEnabled(boolean enabled)
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.ui.IDataRenderer#setAllNonRowFieldsEnabled(boolean)
	 */
	public void setAllNonRowFieldsEnabled(boolean enabled)
	{
		//ignore
	}

	public void add(IComponent c, String n)
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.ui.IContainer#getComponentIterator()
	 */
	public Iterator<IComponent> getComponentIterator()
	{
		//ignore
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.IContainer#remove(com.servoy.j2db.ui.IComponent)
	 */
	public void remove(IComponent c)
	{
		//ignore
	}

	public void setTabSeqComponents(List<Component> list)
	{
		if (list == null || list.size() == 0)
		{
			if (elementTabIndexes.size() > 0) getStylePropertyChanges().setChanged();
			elementTabIndexes.clear();
		}
		else
		{
			getStylePropertyChanges().setChanged();
			elementTabIndexes.clear();
			int columnTabIndex = 0;
			for (Component rowIdComponent : list)
			{
				for (Entry<IPersist, Component> entry : elementToColumnIdentifierComponent.entrySet())
				{
					if (componentIdentifiesColumn(rowIdComponent, entry.getValue()))
					{
						elementTabIndexes.put(entry.getKey(), Integer.valueOf(columnTabIndex));
						columnTabIndex++;
						break;
					}
				}
			}
		}
	}

	public List<String> getTabSeqComponentNames()
	{
		//elementTabIndexes is not sorted. We have to sort the indexes of the tabs first.
		SortedMap<Integer, IPersist> sortedTabIndexes = new TreeMap<Integer, IPersist>();
		for (IPersist key : elementTabIndexes.keySet())
		{
			sortedTabIndexes.put(elementTabIndexes.get(key), key);
		}
		List<String> tabSeqNames = new ArrayList<String>();
		for (int i : sortedTabIndexes.keySet())
		{
			IPersist key = sortedTabIndexes.get(i);
			if (key instanceof ISupportName)
			{
				String name = ((ISupportName)key).getName();
				if (name != null)
				{
					tabSeqNames.add(name);
				}
			}
		}
		return tabSeqNames;
	}

	private boolean componentIdentifiesColumn(Component rowIdComponent, Component value)
	{
		if (rowIdComponent == value)
		{
			return true;
		}
		else if (value instanceof ISupplyFocusChildren< ? >)
		{
			for (Object child : ((ISupplyFocusChildren< ? >)value).getFocusChildren())
			{
				if (child == rowIdComponent) return true;
			}
		}
		return false;
	}

	public boolean isColumnIdentifierComponent(Component c)
	{
		return elementToColumnIdentifierComponent.containsValue(c);
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		return tooltip;
	}

	private void applyStyleOnComponent(Component comp, Object bgColor, Object fgColor, Object compFont, Object listItemBorder)
	{
		if (comp instanceof IScriptableProvider)
		{
			IScriptable s = ((IScriptableProvider)comp).getScriptObject();

			if (s instanceof IRuntimeComponent)
			{
				IRuntimeComponent sbm = (IRuntimeComponent)s;
				RenderableWrapper sbmRW = null;
				if (s instanceof ISupportOnRenderCallback)
				{
					IScriptRenderMethods sr = ((ISupportOnRenderCallback)s).getRenderable();
					if (sr instanceof RenderableWrapper) sbmRW = (RenderableWrapper)sr;
				}

				if (bgColor != null)
				{
					if (sbmRW != null) sbmRW.clearProperty(RenderableWrapper.PROPERTY_BGCOLOR);
					String oldColor = sbm.getBgcolor();
					sbm.setBgcolor(bgColor.toString());
					if (sbm instanceof AbstractRuntimeBaseComponent && ((AbstractRuntimeBaseComponent)sbm).getComponent() instanceof WebDataLookupField)
					{
						((WebDataLookupField)((AbstractRuntimeBaseComponent)sbm).getComponent()).setListColor(PersistHelper.createColor(oldColor));
					}
					if (sbm.isTransparent())
					{
						// apply the bg color even if transparent by clearing the transparent flag in the property changes map
						if (comp instanceof IProviderStylePropertyChanges &&
							((IProviderStylePropertyChanges)comp).getStylePropertyChanges() instanceof IStylePropertyChangesRecorder)
						{
							((IStylePropertyChangesRecorder)(((IProviderStylePropertyChanges)comp).getStylePropertyChanges())).setTransparent(false);
						}
					}
				}
				else
				{
					if (sbmRW != null && !Utils.equalObjects(sbmRW.getOnRenderSetProperties().get(RenderableWrapper.PROPERTY_BGCOLOR), sbm.getBgcolor()))
						sbmRW.clearProperty(RenderableWrapper.PROPERTY_BGCOLOR);
					sbm.setBgcolor(sbm.getBgcolor());
				}

				if (fgColor != null)
				{
					if (sbmRW != null) sbmRW.clearProperty(RenderableWrapper.PROPERTY_FGCOLOR);
					sbm.setFgcolor(fgColor.toString());
				}
				else
				{
					if (sbmRW != null && !Utils.equalObjects(sbmRW.getOnRenderSetProperties().get(RenderableWrapper.PROPERTY_FGCOLOR), sbm.getFgcolor()))
					{
						sbmRW.clearProperty(RenderableWrapper.PROPERTY_FGCOLOR);
					}
					sbm.setFgcolor(sbm.getFgcolor());
				}

				if (compFont != null)
				{
					if (sbmRW != null && !Utils.equalObjects(sbmRW.getOnRenderSetProperties().get(RenderableWrapper.PROPERTY_FONT), sbm.getFont()))
						sbmRW.clearProperty(RenderableWrapper.PROPERTY_FONT);
					sbm.setFont(compFont.toString());
				}
				else
				{
					if (sbmRW != null) sbmRW.clearProperty(RenderableWrapper.PROPERTY_FONT);
					sbm.setFont(sbm.getFont());
				}


				if (listItemBorder != null)
				{
					// TODO left / right part of this list item border should only be applied on first / last components in the row (for table view)
					// like it is done in servoy.js when client side styling is used

					String newBorder = listItemBorder.toString();
					Border currentBorder = ComponentFactoryHelper.createBorder(sbm.getBorder());
					Border marginBorder = null;
					if (currentBorder instanceof EmptyBorder)
					{
						marginBorder = currentBorder;
					}
					else if (currentBorder instanceof CompoundBorder && ((CompoundBorder)currentBorder).getInsideBorder() instanceof EmptyBorder)
					{
						marginBorder = ((CompoundBorder)currentBorder).getInsideBorder();
					}

					if (marginBorder != null)
					{
						newBorder = ComponentFactoryHelper.createBorderString(
							BorderFactory.createCompoundBorder(ComponentFactoryHelper.createBorder(newBorder), marginBorder));
					}
					if (sbmRW != null) sbmRW.clearProperty(RenderableWrapper.PROPERTY_BORDER);
					sbm.setBorder(newBorder);
					// reset size so the web size will be recalculated based on the new border
					sbm.setSize(sbm.getWidth(), sbm.getHeight());
				}
				else
				{
					if (sbmRW != null && !Utils.equalObjects(sbmRW.getOnRenderSetProperties().get(RenderableWrapper.PROPERTY_BORDER), sbm.getBorder()))
						sbmRW.clearProperty(RenderableWrapper.PROPERTY_BORDER);
					sbm.setBorder(sbm.getBorder());
				}
			}
		}
	}

	public int onDrag(JSDNDEvent event)
	{
		String onDragUUID = null;
		if (cellview instanceof Portal)
		{
			Portal cellviewPortal = (Portal)cellview;
			onDragUUID = cellviewPortal.getOnDragMethodID();
		}
		else
		{
			onDragUUID = fc.getForm().getOnDragMethodID();
		}

		if (onDragUUID != null)
		{
			Object dragReturn = fc.executeFunction(onDragUUID, new Object[] { event }, false, null, false, "onDragMethodID"); //$NON-NLS-1$
			if (dragReturn instanceof Number) return ((Number)dragReturn).intValue();
		}

		return DRAGNDROP.NONE;
	}

	public boolean onDragOver(JSDNDEvent event)
	{
		String onDragOverUUID = null;
		if (cellview instanceof Portal)
		{
			Portal cellviewPortal = (Portal)cellview;
			onDragOverUUID = cellviewPortal.getOnDragOverMethodID();
		}
		else
		{
			onDragOverUUID = fc.getForm().getOnDragOverMethodID();
		}

		if (onDragOverUUID != null)
		{
			Object dragOverReturn = fc.executeFunction(onDragOverUUID, new Object[] { event }, false, null, false, "onDragOverMethodID"); //$NON-NLS-1$
			if (dragOverReturn instanceof Boolean) return ((Boolean)dragOverReturn).booleanValue();
		}
		return getOnDropMethodID() != null;
	}

	private String getOnDropMethodID()
	{
		String onDropUUID = null;
		if (cellview instanceof Portal)
		{
			Portal cellviewPortal = (Portal)cellview;
			onDropUUID = cellviewPortal.getOnDropMethodID();
		}
		else
		{
			onDropUUID = fc.getForm().getOnDropMethodID();
		}
		return onDropUUID;
	}

	public boolean onDrop(JSDNDEvent event)
	{
		String onDropUUID = getOnDropMethodID();
		if (onDropUUID != null)
		{
			Object dropHappened = fc.executeFunction(onDropUUID, new Object[] { event }, false, null, false, "onDropMethodID"); //$NON-NLS-1$
			if (dropHappened instanceof Boolean) return ((Boolean)dropHappened).booleanValue();
		}
		return false;
	}

	public void onDragEnd(JSDNDEvent event)
	{
		String onDragEndUUID = null;
		if (cellview instanceof Portal)
		{
			Portal cellviewPortal = (Portal)cellview;
			onDragEndUUID = cellviewPortal.getOnDragEndMethodID();
		}
		else
		{
			onDragEndUUID = fc.getForm().getOnDragEndMethodID();
		}

		if (onDragEndUUID != null)
		{
			fc.executeFunction(onDragEndUUID, new Object[] { event }, false, null, false, "onDragEndMethodID"); //$NON-NLS-1$
		}
	}

	public IComponent getDragSource(Point xy)
	{
		// don't need this, ignore
		return null;
	}

	public String getDragFormName()
	{
		return getDataAdapterList().getFormController().getName();
	}

	public boolean isGridView()
	{
		return true;
	}

	public boolean isListViewMode()
	{
		return isListViewMode;
	}

	public IRecordInternal getDragRecord(Point xy)
	{
		// don't need this, ignore
		return null;
	}

	public int getYOffset()
	{
		return yOffset;
	}

	private int yOffset;
	private FormController dragNdropController;

	public void initDragNDrop(FormController formController, int clientDesignYOffset)
	{
		this.yOffset = clientDesignYOffset;
		boolean enableDragDrop = false;
		if (cellview instanceof Portal)
		{
			Portal cellviewPortal = (Portal)cellview;
			enableDragDrop = (cellviewPortal.getOnDragMethodID() != null || cellviewPortal.getOnDragEndMethodID() != null ||
				cellviewPortal.getOnDragOverMethodID() != null || cellviewPortal.getOnDropMethodID() != null);
		}
		else
		{
			Form form = formController.getForm();
			enableDragDrop = (form.getOnDragMethodID() != null || form.getOnDragEndMethodID() != null || form.getOnDragOverMethodID() != null ||
				form.getOnDropMethodID() != null);
		}

	}

	public FormController getDragNDropController()
	{
		return dragNdropController;
	}

	private final ArrayList<String> labelsCSSClasses = new ArrayList<String>();
	private boolean labelsCssRendered;

	public void addLabelCssClass(String cssClass)
	{
		if (!isListViewMode() && labelsCSSClasses.indexOf(cssClass) == -1) labelsCSSClasses.add(cssClass);
	}

	public String getTableLabelCSSClass(String cssClass)
	{
		return isListViewMode() ? null : new StringBuilder(getMarkupId()).append("_").append(cssClass).append("_lb").toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}


	private Boolean hasOnRender;

	private boolean hasOnRender()
	{
		if (hasOnRender == null)
		{
			hasOnRender = new Boolean(false);
			if (dataRendererOnRenderWrapper.getRenderEventExecutor().hasRenderCallback())
			{
				hasOnRender = new Boolean(true);
			}
			else
			{
				Component comp;
				for (Component element : elementToColumnIdentifierComponent.values())
				{
					comp = element;
					if (comp instanceof IScriptableProvider)
					{
						IScriptable s = ((IScriptableProvider)comp).getScriptObject();
						if (s instanceof ISupportOnRenderCallback && ((ISupportOnRenderCallback)s).getRenderEventExecutor().hasRenderCallback())
						{
							hasOnRender = new Boolean(true);
							break;
						}
					}
				}
			}
		}

		return hasOnRender.booleanValue();
	}

	/**
	 * This function is used by {@link #getRowSelectionScript()}
	 * @param preCaoncatenation TODO
	 *
	 */
	private String toJsArrayString(List<String> arr, String preCaoncatenation)
	{
		StringBuilder jsArr = new StringBuilder();
		jsArr.append('[');
		boolean firstElemntFlag = true;
		for (String str : arr)
		{
			if (!firstElemntFlag)
			{
				jsArr.append(",");
			}
			else
			{
				firstElemntFlag = false;
			}

			if (str == null)
			{
				jsArr.append("''");
			}
			else
			{
				jsArr.append("'" + preCaoncatenation).append(str).append("'");
			}
		}
		jsArr.append(']');
		return jsArr.toString();
	}

	/**
	 * Used by  {@link #getRowSelectionScript()}
	 */
	private void splitFontStyle(Object fontString, StringBuilder fstyle, StringBuilder fweight, StringBuilder fsize, StringBuilder ffamily)
	{
		if (fontString != null)
		{
			Pair<String, String> fontCSSProps[] = PersistHelper.createFontCSSProperties(fontString.toString());
			for (Pair<String, String> fontCSSProp : fontCSSProps)
			{
				if (fontCSSProp != null)
				{
					String key = fontCSSProp.getLeft();
					String value = fontCSSProp.getRight();
					if (value == null) value = ""; //$NON-NLS-1$
					if ("font-style".equals(key)) //$NON-NLS-1$
						fstyle.append(value);
					else if ("font-weight".equals(key)) //$NON-NLS-1$
						fweight.append(value);
					else if ("font-size".equals(key)) //$NON-NLS-1$
						fsize.append(value);
					else if ("font-family".equals(key)) //$NON-NLS-1$
						ffamily.append(value);
				}
			}
		}
	}

	private void splitBorderStyle(Object borderStyle, StringBuilder bstyle, StringBuilder bwidth, StringBuilder bcolor)
	{
		if (borderStyle != null)
		{
			Properties borderProperties = new Properties();
			ComponentFactoryHelper.createBorderCSSProperties(borderStyle.toString(), borderProperties);
			if (!borderProperties.containsKey("border-style") && borderProperties.containsKey("border-top-style"))
			{
				bstyle.append(borderProperties.getProperty("border-top-style", ""));
				bstyle.append(" ");
				bstyle.append(borderProperties.getProperty("border-right-style", ""));
				bstyle.append(" ");
				bstyle.append(borderProperties.getProperty("border-bottom-style", ""));
				bstyle.append(" ");
				bstyle.append(borderProperties.getProperty("border-left-style", ""));
			}
			else
			{
				bstyle.append(borderProperties.getProperty("border-style", "")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			bwidth.append(borderProperties.getProperty("border-width", "")); //$NON-NLS-1$ //$NON-NLS-2$
			bcolor.append(borderProperties.getProperty("border-color", "")); //$NON-NLS-1$ //$NON-NLS-2$
			if (bcolor.length() == 0)
			{
				bcolor.append(borderProperties.getProperty("border-top-color", "")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	public String getRowSelectionScript(boolean allCurrentPageRows)
	{
		return getRowSelectionScript(getIndexToUpdate(allCurrentPageRows));
	}

	boolean hasOddEvenSelected()
	{
		if ((getRowSelectedStyle() != null && getRowSelectedStyle().getAttributeCount() > 0) ||
			(getRowEvenStyle() != null && getRowEvenStyle().getAttributeCount() > 0) || (getRowOddStyle() != null && getRowOddStyle().getAttributeCount() > 0))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public String getRowSelectionScript(List<Integer> indexToUpdate)
	{
		if (currentData == null) return null;
		if (!hasOnRender() && (bgColorScript != null || hasOddEvenSelected()) && indexToUpdate != null)
		{
			int firstRow = table.isPageableMode() ? table.getCurrentPage() * table.getRowsPerPage() : table.getStartIndex();
			int lastRow = firstRow + table.getViewSize() - 1;
			int[] newSelectedIndexes = getSelectedIndexes();

			AppendingStringBuffer sab = new AppendingStringBuffer();
			for (int rowIdx : indexToUpdate)
			{
				ArrayList<String> bgRuntimeColorjsArray = new ArrayList<String>();
				ArrayList<String> fgRuntimeColorjsArray = new ArrayList<String>();
				ArrayList<String> fstyleJsAray = new ArrayList<String>();
				ArrayList<String> fweightJsAray = new ArrayList<String>();
				ArrayList<String> fsizeJsAray = new ArrayList<String>();
				ArrayList<String> ffamilyJsAray = new ArrayList<String>();
				ArrayList<String> bstyleJsAray = new ArrayList<String>();
				ArrayList<String> bwidthJsAray = new ArrayList<String>();
				ArrayList<String> bcolorJsAray = new ArrayList<String>();

				if (rowIdx >= firstRow && rowIdx <= lastRow)
				{
					Component selectedListItem = table.get(Integer.toString(rowIdx));
					if (selectedListItem != null)
					{
						String selectedId = selectedListItem.getMarkupId();
						boolean isSelected = Arrays.binarySearch(newSelectedIndexes, rowIdx) >= 0;

						// IF ONLY SELCTED STYLE RULE is defined then apply runtimeComonent style properties
						if (bgColorScript == null && !isSelected && (getRowOddStyle().getAttributeCount() == 0) && (getRowEvenStyle().getAttributeCount() == 0))
						{

							Iterable< ? extends Component> it = Utils.iterate(selectedListItem.iterator());
							Component cellContents;
							for (Component c : it)
							{
								if (c instanceof CellContainer)
								{
									CellContainer cell = (CellContainer)c;
									cellContents = cell.iterator().next();
								}
								else
								{
									cellContents = c;
								}

								if (cellContents instanceof IScriptableProvider)
								{

									IScriptable scriptableComponent = ((IScriptableProvider)cellContents).getScriptObject();
									if (scriptableComponent instanceof IRuntimeComponent)
									{
										IRuntimeComponent runtimeComponent = (IRuntimeComponent)scriptableComponent;
										//bgcolor
										bgRuntimeColorjsArray.add(runtimeComponent.getBgcolor());
										//fgcolor
										fgRuntimeColorjsArray.add(runtimeComponent.getFgcolor());

										// font style
										String fontStyle = runtimeComponent.getFont();
										StringBuilder fstyle = new StringBuilder(""), fweight = new StringBuilder(""), fsize = new StringBuilder(""), //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
											ffamily = new StringBuilder(""); //$NON-NLS-1$
										splitFontStyle(fontStyle, fstyle, fweight, fsize, ffamily);
										fstyleJsAray.add(fstyle.toString());
										fweightJsAray.add(fweight.toString());
										fsizeJsAray.add(fsize.toString());
										ffamilyJsAray.add(ffamily.toString());

										// border style
										String borderStyle = runtimeComponent.getBorder();
										StringBuilder bstyle = new StringBuilder(""), bwidth = new StringBuilder(""), bcolor = new StringBuilder(""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										splitBorderStyle(borderStyle, bstyle, bwidth, bcolor);
										bstyleJsAray.add(bstyle.toString());
										bwidthJsAray.add(bwidth.toString());
										bcolorJsAray.add(bcolor.toString());
									}
								}
							}
						}


						String selectedColor = null, selectedFgColor = null, selectedFont = null, selectedBorder = null;
						if (!isListViewMode())
							selectedColor = (selectedColor == null ? "" : selectedColor.toString()); //$NON-NLS-1$
						selectedFgColor = (selectedFgColor == null) ? "" : selectedFgColor.toString(); //$NON-NLS-1$

						// font styles
						StringBuilder fstyle = new StringBuilder(""), fweight = new StringBuilder(""), fsize = new StringBuilder(""), //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
							ffamily = new StringBuilder(""); //$NON-NLS-1$
						splitFontStyle(selectedFont, fstyle, fweight, fsize, ffamily);
						//border styles
						StringBuilder bstyle = new StringBuilder(""), bwidth = new StringBuilder(""), bcolor = new StringBuilder(""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						splitBorderStyle(selectedBorder, bstyle, bwidth, bcolor);

						if (bgColorScript == null && !isSelected && (getRowOddStyle().getAttributeCount() == 0) &&
							(getRowEvenStyle().getAttributeCount() == 0) && !isListViewMode())
						{
							//backgroundcolor and color are sent as final inline string
							sab.append("Servoy.TableView.setRowStyle('"). //$NON-NLS-1$
								append(selectedId).append("', "). //$NON-NLS-1$
								append(toJsArrayString(bgRuntimeColorjsArray, "background-color:")).append(","). //$NON-NLS-1$
								append(toJsArrayString(fgRuntimeColorjsArray, "color:")).append(","). //$NON-NLS-1$
								append(toJsArrayString(fstyleJsAray, "")).append(", "). //$NON-NLS-1$
								append(toJsArrayString(fweightJsAray, "")).append(", "). //$NON-NLS-1$
								append(toJsArrayString(fsizeJsAray, "")).append(", "). //$NON-NLS-1$
								append(toJsArrayString(ffamilyJsAray, "")).append(", "). //$NON-NLS-1$
								append(toJsArrayString(bstyleJsAray, "")).append(", "). //$NON-NLS-1$
								append(toJsArrayString(bwidthJsAray, "")).append(","). //$NON-NLS-1$
								append(toJsArrayString(bcolorJsAray, "")).append(","). //$NON-NLS-1$
								append(isListViewMode()).append(");\n"); //$NON-NLS-1$
						}
						else
						{
							sab.append("Servoy.TableView.setRowStyle('"). //$NON-NLS-1$
								append(selectedId).append("', '"). //$NON-NLS-1$
								append(selectedColor).append("', '"). //$NON-NLS-1$
								append(selectedFgColor).append("', '"). //$NON-NLS-1$
								append(fstyle).append("', '"). //$NON-NLS-1$
								append(fweight).append("', '"). //$NON-NLS-1$
								append(fsize).append("', '"). //$NON-NLS-1$
								append(ffamily).append("', '"). //$NON-NLS-1$
								append(bstyle).append("', '"). //$NON-NLS-1$
								append(bwidth).append("', '"). //$NON-NLS-1$
								append(bcolor).append("', "). //$NON-NLS-1$
								append(isListViewMode()).append(");\n"); //$NON-NLS-1$
						}
					}
				}
			}

			String rowSelectionScript = sab.toString();
			if (rowSelectionScript.length() > 0) return rowSelectionScript;
		}
		return null;
	}

	@SuppressWarnings("nls")
	public String getColumnResizeScript()
	{
		if (resizedComponent instanceof IProviderStylePropertyChanges)
		{
			String tableId = getMarkupId();
			String classId = resizedComponent.getId();
			String sWidth = (String)((IProviderStylePropertyChanges)resizedComponent).getStylePropertyChanges().getChanges().get("width"); //$NON-NLS-1$
			if (sWidth != null)
			{
				resizedComponent = null;
				return new AppendingStringBuffer("Servoy.TableView.setTableColumnWidth('").append(tableId).append("', '").append(classId).append("', ").append(
					Integer.parseInt(sWidth.substring(0, sWidth.length() - 2))).append(")").toString();
			}
		}

		return null;
	}

	public List<Integer> getIndexToUpdate(boolean allCurrentPageIndexes)
	{
		if (allCurrentPageIndexes)
		{
			List<Integer> _selectedIndexes = new ArrayList<Integer>();
			int firstRow = table.isPageableMode() ? table.getCurrentPage() * table.getRowsPerPage() : table.getStartIndex();
			int lastRow = firstRow + table.getViewSize() - 1;
			for (int index = firstRow; index <= lastRow; index++)
			{
				_selectedIndexes.add(Integer.valueOf(index));
			}
			return _selectedIndexes;
		}
		else
		{

			if (currentData == null) return null;

			List<Integer> indexesToUpdate = new ArrayList<Integer>();
			List<Integer> oldSelectedIndexes = new ArrayList<Integer>();
			List<Integer> newSelectedIndexesA = new ArrayList<Integer>();

			//selectedIndexesBeforUpdateRenderState  together with selectedIndexes form the 'old' selected indexes
			//selectedIndexesBeforUpdateRenderState was introduced for the case when selecting a previously  selected row
			if (selectedIndexesBeforUpdate != null)
			{
				for (int oldSelected : selectedIndexesBeforUpdate)
					oldSelectedIndexes.add(new Integer(oldSelected));
			}
			if (selectedIndexes != null)
			{ // !!needed because of case when selecting the previously selected index
				for (int oldSelected : selectedIndexes)
					if (oldSelectedIndexes.indexOf(new Integer(oldSelected)) == -1) oldSelectedIndexes.add(new Integer(oldSelected));
			}

			int[] newSelectedIndexes = getSelectedIndexes();
			for (int sel : newSelectedIndexes)
			{
				Integer selection = new Integer(sel);
				newSelectedIndexesA.add(selection);
				// add new selection
				if (oldSelectedIndexes.indexOf(selection) == -1) indexesToUpdate.add(selection);
			}

			for (int sel : oldSelectedIndexes)
			{
				Integer selection = new Integer(sel);
				// add removed selection
				if (newSelectedIndexesA.indexOf(selection) == -1)
				{
					indexesToUpdate.add(selection);
				}
				else if ((selectedIndexesBeforUpdate != null && selectedIndexes != null) && !Arrays.equals(selectedIndexesBeforUpdate, selectedIndexes)) // selected a previously selected row case
				{// !!!!needed because of case when selecting the previously selected index
					indexesToUpdate.add(selection);
				}
			}

			return (indexesToUpdate.size() > 0) ? indexesToUpdate : null;
		}
	}

	private int[] getSelectedIndexes()
	{
		return currentData.getSelectedIndexes();
	}

	/**
	 * Estimates if, for a given height, the table will need more than one page
	 * to display all data (and thus will need a page navigator).
	 *
	 * Returns:
	 * - a flag telling if more than one page is needed;
	 * - the number of max rows that fit in one page;
	 * - the total height in pixels used up by the table.
	 */
	private Pair<Boolean, Pair<Integer, Integer>> needsMoreThanOnePage(int height)
	{
		int reservedHeight = 0;
		if (addHeaders)
		{
			reservedHeight = 20; // extra 20 == the header
		}

		int totalRealHeight = reservedHeight + getOtherFormPartsHeight();

		int maxRows = Math.max((height - reservedHeight) / maxHeight, 1);
		// if only 1px is missing for another row, increase the maxRows;
		// windows web clients does not return accurately the clientHeight property
		if (maxHeight - ((height - reservedHeight) % maxHeight) < 2) maxRows++;

		boolean moreThanOnePage = currentData != null && currentData.getSize() > maxRows;
		if (moreThanOnePage)
		{
			if (!isScrollMode()) reservedHeight += 20; // the page navigator
			maxRows = Math.max((height - reservedHeight) / maxHeight, 1);
			// if only 1px is missing for another row, increase the maxRows;
			// windows web clients does not return accurately the clientHeight property
			if (maxHeight - ((height - reservedHeight) % maxHeight) < 2) maxRows++;
		}

		if (currentData != null) totalRealHeight += Math.min(currentData.getSize(), maxRows) * maxHeight;

		Pair<Integer, Integer> heights = new Pair<Integer, Integer>(new Integer(maxRows), new Integer(totalRealHeight));
		return new Pair<Boolean, Pair<Integer, Integer>>(new Boolean(moreThanOnePage), heights);
	}

	private boolean shouldFillAllHorizontalSpace()
	{
		if (isListViewMode()) return false;

		boolean shouldFillAllHorizSpace = false;
		if (cellview instanceof ISupportScrollbars)
		{
			int scrollbars = ((ISupportScrollbars)cellview).getScrollbars();
			if ((scrollbars & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER)
			{
				for (IPersist element : elementToColumnIdentifierComponent.keySet())
				{
					if (element instanceof ISupportAnchors)
					{
						int anchors = ((ISupportAnchors)element).getAnchors();
						if (((anchors & IAnchorConstants.EAST) != 0) && ((anchors & IAnchorConstants.WEST) != 0))
						{
							shouldFillAllHorizSpace = true;
							break;
						}
					}
				}
			}
		}
		return shouldFillAllHorizSpace;
	}

	/**
	 * Distributes an amount of horizontal free space to some or the columns of the table.
	 *
	 * Can be called in two situations:
	 *
	 * 1. When the browser windows is resized.
	 *
	 * In this case the positive/negative extra space gets distributed to those columns that are
	 * anchored left + right.
	 *
	 * 2. When a column is resized
	 *
	 * In this case the positive/negative extra space gets distributed to all other columns,
	 * regardless of their anchoring.
	 *
	 * In both scenarios the extra space is distributed proportionally to the sizes of the
	 * involved columns.
	 */
	private void distributeExtraSpace(int delta, int totalWidthToStretch, IPersist dontTouchThis, boolean onlyAnchoredColumns)
	{
		if (totalWidthToStretch == 0) return;

		int consumedDelta = 0;
		IRuntimeComponent lastStretched = null;
		for (IPersist element : elementToColumnIdentifierComponent.keySet())
		{
			boolean distributeToThisColumn = true;
			if (dontTouchThis != null && element.equals(dontTouchThis)) distributeToThisColumn = false;
			if (distributeToThisColumn && onlyAnchoredColumns)
			{
				if (element instanceof ISupportAnchors)
				{
					int anchors = ((ISupportAnchors)element).getAnchors();
					if (((anchors & IAnchorConstants.EAST) == 0) || ((anchors & IAnchorConstants.WEST) == 0)) distributeToThisColumn = false;
				}
				else distributeToThisColumn = false;
			}

			if (distributeToThisColumn)
			{
				Component c = elementToColumnIdentifierComponent.get(element);
				if (c instanceof IScriptableProvider && ((IScriptableProvider)c).getScriptObject() instanceof IRuntimeComponent && c.isVisible())
				{
					IRuntimeComponent ic = (IRuntimeComponent)((IScriptableProvider)c).getScriptObject();
					int thisDelta = delta * ic.getWidth() / totalWidthToStretch;
					consumedDelta += thisDelta;
					int newWidth = ic.getWidth() + thisDelta;

					int height = ic.getHeight();
					Iterator<Component> alreadyAddedComponents = cellToElement.keySet().iterator();
					if (alreadyAddedComponents.hasNext())
					{
						Component firstAddedComponent = alreadyAddedComponents.next();
						if ((firstAddedComponent instanceof IComponent)) height = firstAddedComponent.getSize().height;
					}
					ic.setSize(newWidth, height);

					lastStretched = ic;
				}
			}
		}
		// we can have some leftover due to rounding errors, just put it into the last stretched column.
		if ((delta - consumedDelta != 0) && (lastStretched != null))
		{
			lastStretched.setSize(lastStretched.getWidth() + delta - consumedDelta, lastStretched.getHeight());
		}

	}

	private void distributeExtraSpace()
	{
		int totalDefaultWidth = 0;
		int totalWidthToStretch = 0;
		int stretchedElementsCount = 0;
		for (IPersist element : elementToColumnIdentifierComponent.keySet())
		{
			Object scriptobject = elementToColumnIdentifierComponent.get(element);
			if (!((Component)scriptobject).isVisible()) continue;
			if (scriptobject instanceof IScriptableProvider)
			{
				scriptobject = ((IScriptableProvider)scriptobject).getScriptObject();
			}
			if (scriptobject instanceof IRuntimeComponent)
			{
				int width = ((IRuntimeComponent)scriptobject).getWidth();
				totalDefaultWidth += width;
				if (element instanceof ISupportAnchors)
				{
					int anchors = ((ISupportAnchors)element).getAnchors();
					if (((anchors & IAnchorConstants.EAST) != 0) && ((anchors & IAnchorConstants.WEST) != 0))
					{
						totalWidthToStretch += width;
						stretchedElementsCount++;
					}
				}
			}
		}

		boolean shouldFillAllHorizSpace = shouldFillAllHorizontalSpace();
		if (shouldFillAllHorizSpace)
		{
			if (stretchedElementsCount > 0)
			{
				int delta = getDisplayBodyWidthHint() - totalDefaultWidth;
				distributeExtraSpace(delta, totalWidthToStretch, null, true);
			}
		}
	}

	private int getDisplayBodyWidthHint()
	{
		int displayBodyWidthHint = bodyWidthHint;

		int scrollbars = (cellview instanceof ISupportScrollbars) ? ((ISupportScrollbars)cellview).getScrollbars() : 0;
		boolean hasVerticalScrollbarsAlways = (ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS & scrollbars) != 0 ? true : false;

		if ((isScrollMode() && (needsMoreThanOnePage(displayBodyWidthHint).getLeft().booleanValue() || hasVerticalScrollbarsAlways)))
		{
			displayBodyWidthHint -= SCROLLBAR_SIZE; // extract the vertical scrollbar width
		}

		return displayBodyWidthHint;
	}

	private int getOtherFormPartsHeight()
	{
		int bodyDesignHeight = endY - startY;
		int otherPartsHeight = (cellview instanceof Portal) ? 0 : formDesignHeight - bodyDesignHeight;
		return otherPartsHeight;
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

	public void setRowStyles(IStyleSheet styleSheet, IStyleRule oddStyle, IStyleRule evenStyle, IStyleRule selectedStyle, IStyleRule headerStyle)
	{
		this.styleSheet = styleSheet;
		this.oddStyle = oddStyle;
		this.evenStyle = evenStyle;
		this.selectedStyle = selectedStyle;
		this.headerStyle = headerStyle;
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

	public boolean isScrollMode()
	{
		return useAJAX && isScrollMode;
	}


	private void resetScrollParams()
	{
		WebCellBasedView.this.isScrollFirstShow = true;
		WebCellBasedView.this.hasTopBuffer = false;
		WebCellBasedView.this.hasBottomBuffer = true;
		WebCellBasedView.this.currentScrollTop = 0;
		WebCellBasedView.this.topPhHeight = 0;
		WebCellBasedView.this.currentScrollLeft = 0;
	}


	/**
	 * put client property for current and future components.
	 */
	public void putClientProperty(IPersist persist, Object key, Object value)
	{
		Map<Object, Object> clientProperties = elementToClientProperties.get(persist);
		if (clientProperties == null)
		{
			elementToClientProperties.put(persist, clientProperties = new HashMap<Object, Object>());
		}
		clientProperties.put(key, value);

		for (Entry<Component, IPersist> entry : cellToElement.entrySet())
		{
			if (entry.getValue().equals(persist))
			{
				Component component = entry.getKey();
				if (component instanceof IScriptableProvider)
				{
					IScriptable scriptObject = ((IScriptableProvider)component).getScriptObject();
					if (scriptObject instanceof HasRuntimeClientProperty)
					{
						((HasRuntimeClientProperty)scriptObject).putClientProperty(key, value);
					}
				}
			}
		}
	}

	public Object getClientProperty(IPersist persist, Object key)
	{
		Map<Object, Object> clientProperties = elementToClientProperties.get(persist);
		if (clientProperties != null)
		{
			return clientProperties.get(key);
		}
		return null;
	}

	/**
	 *
	 * Apply the previously set client properties to the new component.
	 */
	private void applyClientProperties(Component component, IPersist persist)
	{
		Map<Object, Object> clientProperties = elementToClientProperties.get(persist);
		if (clientProperties != null && component instanceof IScriptableProvider)
		{
			IScriptable scriptObject = ((IScriptableProvider)component).getScriptObject();
			if (scriptObject instanceof HasRuntimeClientProperty)
			{
				for (Entry<Object, Object> entry : clientProperties.entrySet())
				{
					((HasRuntimeClientProperty)scriptObject).putClientProperty(entry.getKey(), entry.getValue());
				}
			}
		}
	}
}
