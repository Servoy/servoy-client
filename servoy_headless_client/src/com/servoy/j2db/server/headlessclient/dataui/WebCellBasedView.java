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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.Style;
import javax.swing.text.html.StyleSheet;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupElement;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.WicketTag;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.resolver.IComponentResolver;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.ClientInfo;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IView;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.FindState;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.FoundSetListWrapper;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.dataprocessing.Row;
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
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.TabSeqComparator;
import com.servoy.j2db.scripting.JSEvent.EventType;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.TabIndexHelper;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.server.headlessclient.dataui.drag.DraggableBehavior;
import com.servoy.j2db.ui.DataRendererOnRenderWrapper;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IPortalComponent;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptInputMethods;
import com.servoy.j2db.ui.IScriptPortalComponentMethods;
import com.servoy.j2db.ui.IScriptReadOnlyMethods;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportEventExecutor;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.PropertyCopy;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IAnchorConstants;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * This class is normally used to show a portal or tableview
 * 
 * @author jblok
 */
public class WebCellBasedView extends WebMarkupContainer implements IView, IPortalComponent, IScriptPortalComponentMethods, IDataRenderer,
	IProviderStylePropertyChanges, TableModelListener, ListSelectionListener, ISupportWebBounds, ISupportWebTabSeq
{
	private static final int SCROLLBAR_SIZE = 17;
	private static final long serialVersionUID = 1L;

	private final LinkedHashMap<IPersist, Component> elementToColumnIdentifierComponent = new LinkedHashMap<IPersist, Component>(); // IPersist -> column identifier components - used by JavaScript
	private final HashMap<IPersist, Integer> elementTabIndexes = new HashMap<IPersist, Integer>();
	private final LinkedHashMap<Component, IPersist> cellToElement = new LinkedHashMap<Component, IPersist>(); // each cell component -> IPersist (on the form)
	private final Map<IPersist, Component> elementToColumnHeader = new HashMap<IPersist, Component>(); // links each column identifier component
	// to a column header component (if such a component exists)

	private String relationName;
	private List<SortColumn> defaultSort = null;

	private SortableCellViewHeaders headers;
	private final WebCellBasedViewListView table;
	private final IModel<FoundSetListWrapper> data = new Model<FoundSetListWrapper>();
	private PagingNavigator pagingNavigator;
	boolean showPageNavigator = true;
	private final ChangesRecorder jsChangeRecorder; //incase this class is a portal
	private DataAdapterList dal;

	private boolean initialSortAsc = true;
	private String initialSortColumnName = null;
	private String initialSortedColumnId = null;

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
	private StyleSheet styleSheet;
	private Style oddStyle, evenStyle;

	public static class CellContainer extends WebMarkupContainer
	{
		public CellContainer(String id)
		{
			super(id);
		}

		public static Component getContentsForCell(Component child)
		{
			if (child instanceof CellContainer)
			{
				Iterator< ? extends Component> it = ((CellContainer)child).iterator();
				if (it.hasNext())
				{
					return it.next();
				}
				Debug.log("Strange - CellContainer with no child..."); //$NON-NLS-1$
			}
			return child;
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

	private class WebCellBasedViewListView extends PageableListView<IRecordInternal>
	{
		private final AbstractBase cellview;
		private final IDataProviderLookup dataProviderLookup;
		private final IScriptExecuter el;
		private final int startY, endY;
		private final Form form;

		public WebCellBasedViewListView(String id, IModel<FoundSetListWrapper> model, int rowsPerPage, AbstractBase cellview,
			IDataProviderLookup dataProviderLookup, IScriptExecuter el, int startY, int endY, Form form)
		{
			super(id, model, rowsPerPage);
			this.cellview = cellview;
			this.dataProviderLookup = dataProviderLookup;
			this.el = el;
			this.startY = startY;
			this.endY = endY;
			this.form = form;
		}

		@Override
		protected void onBeforeRender()
		{
			getViewSize();
			final int firstIndex = getStartIndex();
			for (int i = 0; i < getViewSize(); i++)
			{
				// Get index
				final int index = firstIndex + i;
				getList().get(index);
				updateListItem(index);
			}
			updateHeaders();

			super.onBeforeRender();
			permitRemovedCellComponentsToBeCollected();
		}

		/**
		 * Create a new ListItem for list item at index.
		 * 
		 * @param index
		 * @return ListItem
		 */
		@Override
		protected ListItem<IRecordInternal> newItem(final int index)
		{
			if (WebCellBasedView.this.addHeaders)
			{
				return new ReorderableListItem(index, getListItemModel(getModel(), index));
			}
			else
			{
				return new WebCellBasedViewListItem(index, getListItemModel(getModel(), index));
			}
		}

		private void permitRemovedCellComponentsToBeCollected()
		{
			// cellToElement hash table remembers the IPersist instance for each cell that was created;
			// when a cell is no longer used (it's list item is removed), the cell must be deleted from the
			// hash table as well in order to avoid memory leaks
			List<Component> validChildren = new ArrayList<Component>();

			int firstIndex = getStartIndex();
			int index;
			for (int i = 0; i < getViewSize(); i++)
			{
				index = firstIndex + i;
				ListItem<IRecordInternal> item = (ListItem<IRecordInternal>)get(Integer.toString(index));
				if (item != null)
				{
					Iterator< ? extends Component> children = item.iterator();
					while (children.hasNext())
					{
						Component child = CellContainer.getContentsForCell(children.next());
						validChildren.add(child);
					}
				}
			}

			Iterator<Component> hashedCells = cellToElement.keySet().iterator();
			while (hashedCells.hasNext())
			{
				if (!validChildren.contains(hashedCells.next()))
				{
					hashedCells.remove(); // the cell is no longer used...
				}
			}
		}

		private void updateListItem(int index)
		{
			ListItem<IRecordInternal> item = (ListItem<IRecordInternal>)get(Integer.toString(index));
			if (item != null)
			{
				// update it's model to reflect the correct record (records may have been added/deleted)
				// and the model may no longer be correct (points to a record with another index)
				item.setModel(getListItemModel(getModel(), index));

				// re-apply all changes to the list item and it's child components
				setUpItem(item, false);
			}
		}

		@Override
		protected void populateItem(ListItem<IRecordInternal> listItem)
		{
			setUpItem(listItem, true);
		}

		private void setUpItem(final ListItem<IRecordInternal> listItem, boolean createComponents)
		{
			if (!createComponents)
			{
				// this list item has been set up once before - reset previous behaviors  
				List<IBehavior> allBehaviors = listItem.getBehaviors();
				for (int i = 0; i < allBehaviors.size(); i++)
				{
					listItem.remove(allBehaviors.get(i));
				}
			}

			final IRecordInternal rec = listItem.getModelObject();
			Object color = WebCellBasedView.this.getListItemBgColor(listItem, false);
			final Object compColor;
			if (color != null && !(color instanceof Undefined))
			{
				listItem.add(new StyleAppendingModifier(new Model<String>("background-color: " + color.toString()))); //$NON-NLS-1$
				compColor = color;
			}
			else
			{
				listItem.add(new AttributeModifier("class", new Model<String>((listItem.getIndex() % 2) == 0 ? "even" : "odd"))); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				compColor = null;
			}

			final int visibleRowIndex = listItem.getIndex() % getRowsPerPage();
			if (createComponents)
			{
				createComponents(application, form, cellview, dataProviderLookup, el, startY, endY, new ItemAdd()
				{
					public void add(IPersist element, final Component comp)
					{
						Component component = elementToColumnIdentifierComponent.values().iterator().next();
						if ((component instanceof IComponent) && (comp instanceof IScriptBaseMethods))
						{
							IScriptBaseMethods ic = (IScriptBaseMethods)comp;
							ic.js_setSize(ic.js_getWidth(), ((IComponent)component).getSize().height);
						}
						cellToElement.put(comp, element);
						Component listItemChild = comp;
						if (element instanceof ISupportName)
						{
							String elementName = ((ISupportName)element).getName();
							if ((elementName != null) && (elementName.trim().length() > 0) || WebCellBasedView.this.addHeaders)
							{
								// this column's cells can be made invisible (and <td> tag is the one that has to change)
								// so we will link this <td> to a wicket component
								listItemChild = new CellContainer(comp.getId() + '_');
								listItemChild.setOutputMarkupPlaceholderTag(true);
								((MarkupContainer)listItemChild).add(comp);
							}
						}

						listItem.add(listItemChild);
						setUpComponent(comp, rec, compColor, visibleRowIndex);
					}
				});
			}
			else
			{
				// we only need to set up again all components in the list item (refresh them)
				Iterator< ? extends Component> children = listItem.iterator();
				while (children.hasNext())
				{
					Component child = CellContainer.getContentsForCell(children.next());
					// re-initialize :) it - apply js_ user changes applied on the column identifier component
					// and other initializations...
					initializeComponent(child, cellview, cellToElement.get(child));
					setUpComponent(child, rec, compColor, visibleRowIndex);
				}
			}


			//listItem.add(new SimpleAttributeModifier("onfocus", "Wicket.Log.info('ONFOCUS')"));

			enableChildrenInContainer(this, isEnabled());
		}

		private void setUpComponent(Component comp, IRecordInternal record, Object compColor, int visibleRowIndex)
		{
			// set correct tab index
			if (tabIndex < 0)
			{
				TabIndexHelper.setUpTabIndexAttributeModifier(comp, tabIndex);
			}
			else
			{
				if (elementTabIndexes.size() > 0)
				{
					Integer idx = elementTabIndexes.get(cellToElement.get(comp));
					if (idx == null)
					{
						TabIndexHelper.setUpTabIndexAttributeModifier(comp, ISupportWebTabSeq.SKIP);
					}
					else
					{
						TabIndexHelper.setUpTabIndexAttributeModifier(comp, tabIndex + 1 + visibleRowIndex * elementTabIndexes.size() + idx.intValue());
					}
				}
				else
				{
					TabIndexHelper.setUpTabIndexAttributeModifier(comp, tabIndex + 1);
				}
			}

			if (compColor != null)
			{
				comp.add(new StyleAppendingModifier(new Model<String>("background-color: " + compColor.toString()))); //$NON-NLS-1$
			}
			if (js_isReadOnly() && validationEnabled && comp instanceof IScriptReadOnlyMethods) // if in find mode, the field should not be readonly
			{
				((IScriptReadOnlyMethods)comp).js_setReadOnly(true);
			}
			if (!isEnabled() && comp instanceof IComponent)
			{
				((IComponent)comp).setComponentEnabled(false);
			}
			if (comp instanceof IDisplayRelatedData && record != null)
			{
				((IDisplayRelatedData)comp).setRecord(record, true);
			}

			MarkupContainer parent = comp.getParent();
			if (parent instanceof CellContainer)
			{
				// apply properties that need to be applied to <td> tag instead
				parent.setVisible(comp.isVisible());
			}
		}

		@Override
		protected IModel<IRecordInternal> getListItemModel(final IModel< ? extends List<IRecordInternal>> listViewModel, final int index)
		{
			List<IRecordInternal> list = listViewModel.getObject();
			if (list != null)
			{
				IRecordInternal r = list.get(index);
				if (r instanceof FindState)
				{
					return new FindStateItemModel(r);
				}
				else
				{
					return new FoundsetRecordItemModel(this, r, index);
				}
			}
			return null;
		}
	}

	private class WebCellBasedViewListItem extends ListItem<IRecordInternal>
	{
		public WebCellBasedViewListItem(int index, IModel<IRecordInternal> model)
		{
			super(index, model);
			setOutputMarkupId(true);
		}

		@Override
		public String getMarkupId()
		{
			return WebCellBasedView.this.getMarkupId() + '_' + super.getMarkupId();
		}

		@Override
		protected void onBeforeRender()
		{
			updateComponentsRenderState(null, Arrays.binarySearch(getSelectedIndexes(), getIndex()) >= 0);
			super.onBeforeRender();
			Iterator< ? extends Component> it = iterator();
			while (it.hasNext())
			{
				Component component = it.next();
				if (component instanceof CellContainer)
				{
					Object c = ((CellContainer)component).iterator().next();
					if (c instanceof WebDataComboBox)
					{
						//HACK: update variable if current value is in valuelist (because setValueObject is not called)
						((WebDataComboBox)c).refreshValueInList();
					}
				}
			}
		}

		public void updateComponentsRenderState(AjaxRequestTarget target, boolean isSelected)
		{
			Iterator< ? extends Component> it = iterator();
			while (it.hasNext())
			{
				Component component = it.next();
				if (component instanceof CellContainer)
				{
					Object c = ((CellContainer)component).iterator().next();
					if (updateComponentRenderState(c, isSelected) && target != null && c instanceof Component) target.addComponent((Component)c);
				}
			}
		}

		private boolean updateComponentRenderState(Object component, boolean isSelected)
		{
			if (component instanceof ISupportEventExecutor && component instanceof IProviderStylePropertyChanges)
			{
				IEventExecutor ee = ((ISupportEventExecutor)component).getEventExecutor();
				if (ee != null && ee.hasRenderCallback())
				{
					((RenderEventExecutor)ee).setRenderState(getModelObject(), getIndex(), isSelected);
					((IProviderStylePropertyChanges)component).getStylePropertyChanges().setChanged();
					return true;
				}
			}

			return false;
		}
	}

	private class ReorderableListItem extends WebCellBasedViewListItem
	{
		public ReorderableListItem(int index, IModel<IRecordInternal> model)
		{
			super(index, model);
		}

		@Override
		protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag)
		{
			renderReorderableTagBody(markupStream, openTag);
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

		private void renderReorderableTagBody(final MarkupStream markupStream, final ComponentTag openTag)
		{
			renderColumnIdx = 0;
			headerMarkupStartIdx = markupStream.getCurrentIndex();
			orderedHeaders = WebCellBasedView.this.getOrderedHeaders();

			if ((markupStream != null) && (markupStream.getCurrentIndex() > 0))
			{
				// If the original tag has been changed from open-close to open-body-close,
				// than historically renderComponentTagBody gets called, but actually
				// it shouldn't do anything since there is no body for that tag.
				ComponentTag origOpenTag = (ComponentTag)markupStream.get(markupStream.getCurrentIndex() - 1);
				if (origOpenTag.isOpenClose())
				{
					return;
				}
			}

			// If the open tag requires a close tag
			boolean render = openTag.requiresCloseTag();
			if (!render)
			{
				// Tags like <p> do not require a close tag, but they may have.
				render = !openTag.hasNoCloseTag();
			}
			if (render)
			{
				// Loop through the markup in this container
				while (markupStream.hasMore() && !markupStream.get().closes(openTag))
				{
					// Render markup element. Doing so must advance the markup
					// stream
					final int index = markupStream.getCurrentIndex();
					_renderNext(markupStream);
					if (index == markupStream.getCurrentIndex())
					{
						markupStream.throwMarkupException("Markup element at index " + index + " failed to advance the markup stream"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}

		/**
		 * Renders the next element of markup in the given markup stream.
		 * 
		 * @param markupStream The markup stream
		 */
		private final void _renderNext(final MarkupStream markupStream)
		{
			// Get the current markup element
			final MarkupElement element = markupStream.get();

			// If it a tag like <wicket..> or <span wicket:id="..." >
			if ((element instanceof ComponentTag) && !markupStream.atCloseTag())
			{
				// Get element as tag
				final ComponentTag tag = (ComponentTag)element;

				// Get component id
				final String id = tag.getId();

				// Get the component for the id from the given container
				final Component component = get(id);

				// Failed to find it?
				if (component != null)
				{
					if (component instanceof CellContainer || component instanceof IComponent)
					{
						int currentIdx = markupStream.getCurrentIndex();
						renderColumnCell(renderColumnIdx, markupStream);
						renderColumnIdx++;
						markupStream.setCurrentIndex(currentIdx);
						markupStream.skipComponent();
					}
				}
				else
				{
					// 2rd try: Components like Border and Panel might implement
					// the ComponentResolver interface as well.
					MarkupContainer container = this;
					while (container != null)
					{
						if (container instanceof IComponentResolver)
						{
							if (((IComponentResolver)container).resolve(this, markupStream, tag))
							{
								return;
							}
						}

						container = container.findParent(MarkupContainer.class);
					}

					// 3rd try: Try application's component resolvers
					for (IComponentResolver resolver : getApplication().getPageSettings().getComponentResolvers())
					{
						if (resolver.resolve(this, markupStream, tag))
						{
							return;
						}
					}

					if (tag instanceof WicketTag)
					{
						if (((WicketTag)tag).isChildTag())
						{
							markupStream.throwMarkupException("Found " + tag.toString() + " but no <wicket:extend>"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						else
						{
							markupStream.throwMarkupException("Failed to handle: " + tag.toString()); //$NON-NLS-1$
						}
					}

					// No one was able to handle the component id
					markupStream.throwMarkupException("Unable to find component with id '" + id + "' in " + this + ". This means that you declared wicket:id=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
						id + " in your markup, but that you either did not add the " + "component to your page at all, or that the hierarchy does not match."); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			else
			{
				getResponse().write(element.toCharSequence());
				markupStream.next();
			}
		}

		private void renderColumnCell(int columnIdx, final MarkupStream markupStream)
		{
			Component header = orderedHeaders.get(columnIdx);

			markupStream.setCurrentIndex(headerMarkupStartIdx);
			boolean found = false;
			MarkupElement element;

			while (!found)
			{
				element = markupStream.next();
				if (element == null) throw new RuntimeException("can't find the element for the header componet: " + header); //$NON-NLS-1$
				if ((element instanceof ComponentTag) && !markupStream.atCloseTag())
				{
					// Get element as tag
					final ComponentTag tag = (ComponentTag)element;

					// Get component id
					final String id = tag.getId();

					// Get the component for the id from the given container
					final Component component = get(id);

					// Failed to find it?
					if (component != null)
					{
						if (component instanceof CellContainer || component instanceof IComponent)
						{
							if (component.getId().startsWith(header.getId()))
							{
								if (component instanceof CellContainer)
								{
									Object c = ((CellContainer)component).iterator().next();
									if (c instanceof IProviderStylePropertyChanges)
									{
										// ignore location changes
										Properties prop = ((IProviderStylePropertyChanges)c).getStylePropertyChanges().getChanges();
										prop.remove("left"); //$NON-NLS-1$
										prop.remove("top"); //$NON-NLS-1$
									}
								}

								component.render(markupStream);
								found = true;
							}
						}
					}
				}
			}
		}
	}

	public WebCellBasedView(final String id, final IApplication application, final Form form, final AbstractBase cellview,
		final IDataProviderLookup dataProviderLookup, final IScriptExecuter el, boolean addHeaders, final int startY, final int endY, final int sizeHint)
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

		this.bodyWidthHint = form.getWidth();

		useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		useAnchors = Utils.getAsBoolean(application.getRuntimeProperties().get("enableAnchors")); //$NON-NLS-1$
		setOutputMarkupPlaceholderTag(true);

		dataRendererOnRenderWrapper = new DataRendererOnRenderWrapper(this);

		if (!useAJAX) bodyHeightHint = sizeHint;

		jsChangeRecorder = new ChangesRecorder(null, null)
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
					Iterator<Component> iterator = elementToColumnIdentifierComponent.values().iterator();
					while (iterator.hasNext())
					{
						Component object = iterator.next();
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
				if (retval)
				{
					MainPage page = (MainPage)getPage();
					page.getPageContributor().addTableToRender(WebCellBasedView.this);
					setRendered();
				}
				return false;
			}

			@Override
			public void setRendered()
			{
				super.setRendered();
				Iterator<Component> iterator = elementToColumnIdentifierComponent.values().iterator();
				while (iterator.hasNext())
				{
					Component comp = iterator.next();
					if (comp instanceof IProviderStylePropertyChanges)
					{
						((IProviderStylePropertyChanges)comp).getStylePropertyChanges().setRendered();
					}
				}
			}
		};

		add(new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				WebTabPanel tabpanel = findParent(WebTabPanel.class);
				if (tabpanel != null)
				{
					return ""; //$NON-NLS-1$
				}
				return "overflow: auto;"; //$NON-NLS-1$
			}
		}));
		if (cellview instanceof BaseComponent)
		{
			ComponentFactory.applyBasicComponentProperties(application, this, (BaseComponent)cellview,
				ComponentFactory.getStyleForBasicComponent(application, (BaseComponent)cellview, form));
		}

		boolean sortable = true;
		String initialSortString = null;
		int onRenderMethodID = 0;
		if (cellview instanceof Portal)
		{
			Portal p = (Portal)cellview;
			setRowBGColorScript(p.getRowBGColorCalculation(), p.getInstanceMethodArguments("rowBGColorCalculation")); //$NON-NLS-1$
			sortable = p.getSortable();
			initialSortString = p.getInitialSort();
			onRenderMethodID = p.getOnRenderMethodID();
		}
		else if (cellview instanceof Form)
		{
			initialSortString = form.getInitialSort();
			onRenderMethodID = form.getOnRenderMethodID();
		}

		if (onRenderMethodID > 0)
		{
			dataRendererOnRenderWrapper.getRenderEventExecutor().setRenderCallback(Integer.toString(onRenderMethodID));
			dataRendererOnRenderWrapper.getRenderEventExecutor().setRenderScriptExecuter(fc != null ? fc.getScriptExecuter() : null);
		}

		initDragNDrop(fc, startY);

		if (sortable)
		{
			if (initialSortString != null)
			{
				StringTokenizer tokByComma = new StringTokenizer(initialSortString, ","); //$NON-NLS-1$
				if (tokByComma.hasMoreTokens())
				{
					String initialSortFirstToken = tokByComma.nextToken();
					StringTokenizer tokBySpace = new StringTokenizer(initialSortFirstToken);
					if (tokBySpace.hasMoreTokens())
					{
						initialSortColumnName = tokBySpace.nextToken();

						if (tokBySpace.hasMoreTokens())
						{
							String sortDir = tokBySpace.nextToken();
							if (sortDir.equalsIgnoreCase("DESC")) initialSortAsc = false; //$NON-NLS-1$
						}
					}
				}
			}
			// If no initial sort was specified, then default will be the first PK column.
			if (initialSortColumnName == null)
			{
				try
				{
					String dataSource = null;
					if (cellview instanceof Portal)
					{
						Portal p = (Portal)cellview;
						String relation = p.getRelationName();
						int lastDot = relation.lastIndexOf(".");
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
						if (pkColumnNames.hasNext())
						{
							initialSortColumnName = pkColumnNames.next();
							initialSortAsc = true;
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
			Iterator<IPersist> components = cellview.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			while (components.hasNext())
			{
				IPersist element = components.next();
				if (element instanceof Field || element instanceof GraphicalComponent)
				{
					if (element instanceof GraphicalComponent && ((GraphicalComponent)element).getLabelFor() != null)
					{
						labelsFor.put(((GraphicalComponent)element).getLabelFor(), element);
						continue;
					}
					Point l = ((IFormElement)element).getLocation();
					if (l == null)
					{
						continue;// unknown where to add
					}
					if (l.y >= startY && l.y < endY)
					{
						int height = ((IFormElement)element).getSize().height;
						if (height > maxHeight) maxHeight = height;
					}
				}
			}
			if (maxHeight == 0) maxHeight = 20;
		}
		catch (Exception ex1)
		{
			Debug.error("Error getting max size out of components", ex1); //$NON-NLS-1$
		}

		// Add the table
		table = new WebCellBasedViewListView("rows", data, 1, cellview, //$NON-NLS-1$
			dataProviderLookup, el, startY, endY, form);
		table.setReuseItems(true);
		add(table);

		final LinkedHashMap<String, IDataAdapter> dataadapters = new LinkedHashMap<String, IDataAdapter>();
		final SortedList<IPersist> columnTabSequence = new SortedList<IPersist>(TabSeqComparator.INSTANCE); // in fact ISupportTabSeq persists
		createComponents(application, form, cellview, dataProviderLookup, el, startY, endY, new ItemAdd()
		{
			public void add(IPersist element, Component comp)
			{

				if (element instanceof IFormElement && comp instanceof IComponent)
				{
					((IComponent)comp).setLocation(((IFormElement)element).getLocation());
					((IComponent)comp).setSize(((IFormElement)element).getSize());
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
						if (dataprovider.equals(initialSortColumnName)) initialSortedColumnId = comp.getId();
					}
				}
			}
		});
		for (int i = columnTabSequence.size() - 1; i >= 0; i--)
		{
			elementTabIndexes.put(columnTabSequence.get(i), Integer.valueOf(i));
		}

		// Add the (sortable) header (and define how to sort the different columns)
		if (addHeaders)
		{
			// elementToColumnHeader will be filled up by SortableCellViewHeaders when components in it are resolved
			headers = new SortableCellViewHeaders(form, this, "header", table, cellview, application, initialSortedColumnId, initialSortAsc, new IHeaders() //$NON-NLS-1$
				{

					public void registerHeader(IPersist matchingElement, Component headerComponent)
					{
						SortableCellViewHeader sortableHeader = (SortableCellViewHeader)headerComponent;
						// set headerComponent width
						Component columnIdentifier = WebCellBasedView.this.elementToColumnIdentifierComponent.get(matchingElement);

						if (columnIdentifier instanceof IProviderStylePropertyChanges)
						{
							String width = (String)((IProviderStylePropertyChanges)columnIdentifier).getStylePropertyChanges().getChanges().get("offsetWidth"); //$NON-NLS-1$
							if (width != null)
							{
								sortableHeader.setWidth(Integer.parseInt(width.substring(0, width.length() - 2)));
							}
							else if (matchingElement instanceof BaseComponent) sortableHeader.setWidth(((BaseComponent)matchingElement).getSize().width);
						}
						sortableHeader.setTabIndex(tabIndex);
						sortableHeader.setScriptExecuter(el);
						WebCellBasedView.this.registerHeader(matchingElement, headerComponent);
					}
				});
			add(headers);
		}

		// Add a table navigator
		if (useAJAX)
		{
			add(pagingNavigator = new ServoyAjaxPagingNavigator("navigator", table)); //$NON-NLS-1$
			add(new AbstractServoyDefaultAjaxBehavior()
			{
				private boolean responded = false;

				@SuppressWarnings("nls")
				@Override
				public void renderHead(IHeaderResponse response)
				{
					super.renderHead(response);
					String cellViewId = WebCellBasedView.this.getMarkupId();

					StringBuffer sb = new StringBuffer();

					if (useAnchors)
					{
						sb.append("if(typeof(tablesPreferredHeight) != \"undefined\")\n").append("{\n"); //$NON-NLS-1$ //$NON-NLS-2$
						sb.append("tablesPreferredHeight['").append(cellViewId).append("'] = new Array();\n"); //$NON-NLS-1$ //$NON-NLS-2$
						sb.append("tablesPreferredHeight['").append(cellViewId).append("']['height'] = ").append(bodyHeightHint).append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						sb.append("tablesPreferredHeight['").append(cellViewId).append("']['width'] = ").append(bodyWidthHint).append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						sb.append("tablesPreferredHeight['").append(cellViewId).append("']['callback'] = '").append(getCallbackUrl()).append("';\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						sb.append("}\n"); //$NON-NLS-1$
					}
					else if (!responded) // this flag guards against the possibility of endless loop that keeps replacing whole table view generated by following JS code + code in "respond(...)" that alters bodyHeightHint
					{
						sb.append("var preferredSize = getPreferredTableSize('").append(cellViewId); //$NON-NLS-1$ //$NON-NLS-2$
						sb.append("');\n"); //$NON-NLS-1$ 
						sb.append("if(preferredSize[1] != ").append(bodyHeightHint).append(") wicketAjaxGet('").append(getCallbackUrl()).append( //$NON-NLS-1$ //$NON-NLS-2$
							"&bodyWidth=' + preferredSize[0] + '&bodyHeight=' + preferredSize[1]);\n"); //$NON-NLS-1$ 
					}
					response.renderOnLoadJavascript(sb.toString());
					responded = false;
				}

				@Override
				protected void respond(AjaxRequestTarget target)
				{
					responded = true;

					String sBodyWidthHint = getComponent().getRequest().getParameter("bodyWidth"); //$NON-NLS-1$ 
					String sBodyHeightHint = getComponent().getRequest().getParameter("bodyHeight"); //$NON-NLS-1$ 
					bodyWidthHint = Integer.parseInt(sBodyWidthHint);
					bodyHeightHint = Integer.parseInt(sBodyHeightHint);

					int totalDefaultWidth = 0;
					int totalWidthToStretch = 0;
					int stretchedElementsCount = 0;
					for (IPersist element : elementToColumnIdentifierComponent.keySet())
					{
						Component c = elementToColumnIdentifierComponent.get(element);
						if (c instanceof IScriptBaseMethods)
						{
							int width = ((IScriptBaseMethods)c).js_getWidth();
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
							int delta = bodyWidthHint - totalDefaultWidth;
							distributeExtraSpace(delta, totalWidthToStretch, null, true);
							setHeadersWidth();
						}
					}

					WebTabPanel tabPanel = findParent(WebTabPanel.class);
					if (tabPanel != null)
					{
						int bodyDesignHeight = endY - startY;
						int otherPartsHeight = (cellview instanceof Portal) ? 0 : formDesignHeight - bodyDesignHeight;
						tabPanel.setTabSize(new Dimension(Integer.parseInt(sBodyWidthHint), bodyHeightHint + otherPartsHeight));
					}
					WebCellBasedView.this.setVisibilityAllowed(true);
					WebCellBasedView.this.jsChangeRecorder.setChanged();
					WebEventExecutor.generateResponse(target, getComponent().getPage());
				}
			});
		}
		else
		{
			add(pagingNavigator = new ServoySubmitPagingNavigator("navigator", table)); //$NON-NLS-1$
		}

		//hide all further records (and navigator) if explicitly told that there should be no vertical scrollbar 
		int scrollbars = 0;
		if (cellview instanceof Portal)
		{
			scrollbars = ((Portal)cellview).getScrollbars();
		}
		if (cellview instanceof Form)
		{
			scrollbars = ((Form)cellview).getScrollbars();
		}
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
	}

	public void setTabIndex(int tabIndex)
	{
		this.tabIndex = tabIndex;
		((ISupportWebTabSeq)pagingNavigator).setTabIndex(tabIndex + WebForm.SEQUENCE_RANGE_TABLE - 1);
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

	public void moveColumn(SortableCellViewHeader headerColumn, int x, AjaxRequestTarget ajaxRequestTarget)
	{
		if (headerColumn.getWidth() / 2 < Math.abs(x)) // we have a move
		{
			List<Component> orderedHeaders = getOrderedHeaders();

			SortableCellViewHeader nextHeader;
			int movedHeaderIdx = orderedHeaders.indexOf(headerColumn);
			int nextHeaderIdx;
			int moveToHeaderIdx = -1;

			if (x > 0) // moved to right
			{
				nextHeaderIdx = movedHeaderIdx + 1;
				int offset = (x - headerColumn.getWidth() / 2);
				while (nextHeaderIdx < orderedHeaders.size())
				{
					nextHeader = (SortableCellViewHeader)orderedHeaders.get(nextHeaderIdx);
					if (offset < nextHeader.getWidth() || nextHeaderIdx == orderedHeaders.size() - 1) // over next element or end
					{
						if (offset < nextHeader.getWidth() / 2) // move before
						{
							moveToHeaderIdx = nextHeaderIdx - 1;
						}
						else
						// move after
						{
							if (nextHeaderIdx + 1 < orderedHeaders.size())
							{
								moveToHeaderIdx = nextHeaderIdx;
							}
							else
							{
								moveToHeaderIdx = orderedHeaders.size() - 1;
							}
						}
						break;
					}
					else
					{
						offset -= nextHeader.getWidth();
						nextHeaderIdx++;
					}
				}
			}
			else
			// moved left
			{
				nextHeaderIdx = movedHeaderIdx - 1;
				int offset = (Math.abs(x) - headerColumn.getWidth() / 2);
				while (nextHeaderIdx > -1)
				{
					nextHeader = (SortableCellViewHeader)orderedHeaders.get(nextHeaderIdx);
					if (offset < nextHeader.getWidth() || nextHeaderIdx == 0) // over next element or end
					{
						if (offset < nextHeader.getWidth() / 2) // move after
						{
							moveToHeaderIdx = nextHeaderIdx + 1;
						}
						else
						// move before
						{
							moveToHeaderIdx = nextHeaderIdx;
						}
						break;
					}
					else
					{
						offset -= nextHeader.getWidth();
						nextHeaderIdx--;
					}
				}
			}

			if (moveToHeaderIdx == -1 || movedHeaderIdx == moveToHeaderIdx)
			{
				return;
			}

			List<Component> headerColumnsBeforeDrag = orderedHeaders;

			moveColumn(orderedHeaders, movedHeaderIdx, moveToHeaderIdx);

			keepGroupOrder(headerColumn, headerColumnsBeforeDrag);


			if (headers != null)
			{
				this.headers.getStylePropertyChanges().setChanged();
			}
			this.getStylePropertyChanges().setChanged();
			WebEventExecutor.generateResponse(ajaxRequestTarget, getPage());
		}
	}

	private void moveColumn(List<Component> orderedHeaders, int columnIndex, int newIndex)
	{
		List<Component> orderedHeaderCopy = new ArrayList<Component>(orderedHeaders);
		orderedHeaderCopy.add(newIndex, orderedHeaderCopy.remove(columnIndex));

		updateXLocationForColumns(orderedHeaderCopy);
	}

	private void updateXLocationForColumns(List<Component> orderedHeaderCopy)
	{
		int startX = 0;
		for (Component c : orderedHeaderCopy)
		{
			for (IPersist p : elementToColumnHeader.keySet())
			{
				if (elementToColumnHeader.get(p).equals(c))
				{
					Component columnIdentifierComponent = elementToColumnIdentifierComponent.get(p);
					Point oldLocation = ((IComponent)columnIdentifierComponent).getLocation();
					if (oldLocation == null)
					{
						oldLocation = ((ISupportBounds)p).getLocation();
					}
					((IComponent)columnIdentifierComponent).setLocation(new Point(startX, (int)oldLocation.getY()));
					startX += ((IComponent)columnIdentifierComponent).getSize().width;
				}
			}
		}
	}

	private void keepGroupOrder(SortableCellViewHeader headerColumn, List<Component> headerColumnsBeforeDrag)
	{
		// check for groups, put the elements in the same order back together

		// first the columns left from the dragged column
		int offset = -1;
		while (moveColumnInSameGroup(headerColumn, headerColumnsBeforeDrag, offset))
		{
			offset--;
		}
		// then the columns right from the dragged column
		offset = 1;
		while (moveColumnInSameGroup(headerColumn, headerColumnsBeforeDrag, offset))
		{
			offset++;
		}
		// check if a column is breaking up a group
		for (Component hc : headerColumnsBeforeDrag)
		{
			moveColumnInSameGroup(hc, headerColumnsBeforeDrag, 1);
		}
	}

	private boolean moveColumnInSameGroup(Component headerColumn, List<Component> headerColumnsBeforeDrag, int offset)
	{
		String groupId = (String)dal.getFormController().getComponentProperty(getIdentifierComponent(headerColumn), ComponentFactory.GROUPID_COMPONENT_PROPERTY);
		if (groupId == null)
		{
			return false;
		}

		// find the columns offsetted to this column in the original columns
		int orgIindex = headerColumnsBeforeDrag.indexOf(headerColumn);
		if (orgIindex == -1)
		{
			return false; // strange, should not happen
		}
		if ((offset < 0 && orgIindex + offset < 0) || (offset > 0 && orgIindex + offset >= headerColumnsBeforeDrag.size()))
		{
			// no more columns in original set
			return false;
		}

		List<Component> orderedHeaders = getOrderedHeaders();

		int currIndex = orderedHeaders.indexOf(headerColumn);
		if (currIndex == -1)
		{
			return false; // strange, should not happen
		}

		Component column2 = headerColumnsBeforeDrag.get(orgIindex + offset);
		if (!groupId.equals(dal.getFormController().getComponentProperty(getIdentifierComponent(column2), ComponentFactory.GROUPID_COMPONENT_PROPERTY)))
		{
			// original column at offset is not part of the same group
			return false;
		}

		int currIndex2 = orderedHeaders.indexOf(column2);
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
					moveColumn(orderedHeaders, currIndex2, currIndex + offset + 1);
				}
			}
			else
			{
				if (currIndex2 != currIndex + offset)
				{
					moveColumn(orderedHeaders, currIndex2, currIndex + offset);
				}
			}
		}
		else
		{
			if (currIndex2 < currIndex)
			{
				if (currIndex2 != currIndex + offset - 1)
				{
					moveColumn(orderedHeaders, currIndex2, currIndex + offset - 1);
				}
			}
			else
			{
				if (currIndex2 != currIndex + offset)
				{
					moveColumn(orderedHeaders, currIndex2, currIndex + offset);
				}
			}
		}

		// moved
		return true;
	}

	public void resizeColumn(SortableCellViewHeader headerColumn, int x)
	{
		int totalWidthToStretch = 0;
		IPersist resizedPersist = null;
		for (IPersist p : elementToColumnHeader.keySet())
		{
			Component c = elementToColumnIdentifierComponent.get(p);
			if (c instanceof IScriptBaseMethods)
			{
				IScriptBaseMethods ic = (IScriptBaseMethods)c;
				if (elementToColumnHeader.get(p).equals(headerColumn))
				{
					int height = ic.js_getHeight();
					Iterator<Component> alreadyAddedComponents = cellToElement.keySet().iterator();
					if (alreadyAddedComponents.hasNext())
					{
						Component firstAddedComponent = alreadyAddedComponents.next();
						if ((firstAddedComponent instanceof IComponent)) height = ((IComponent)firstAddedComponent).getSize().height;
					}
					ic.js_setSize(ic.js_getWidth() + x, height);
					if (ic instanceof IProviderStylePropertyChanges)
					{
						resizedComponent = c;
						((IProviderStylePropertyChanges)ic).getStylePropertyChanges().setRendered(); // avoid the tableview to render because of this change
					}
					resizedPersist = p;
				}
				else
				{
					totalWidthToStretch += ic.js_getWidth();
				}
			}
		}
		if (shouldFillAllHorizontalSpace()) distributeExtraSpace(-x, totalWidthToStretch, resizedPersist, false);
		setHeadersWidth();
	}

	// set headers width according to cell's width
	private void setHeadersWidth()
	{
		Iterator<IPersist> columnPersistIte = elementToColumnIdentifierComponent.keySet().iterator();

		IPersist columnPersist;
		Component columnHeader, columnCell;
		while (columnPersistIte.hasNext())
		{
			columnPersist = columnPersistIte.next();
			columnCell = elementToColumnIdentifierComponent.get(columnPersist);
			columnHeader = elementToColumnHeader.get(columnPersist);
			if (columnCell instanceof IProviderStylePropertyChanges)
			{
				String width = (String)((IProviderStylePropertyChanges)columnCell).getStylePropertyChanges().getChanges().get("offsetWidth"); //$NON-NLS-1$

				if (columnHeader instanceof SortableCellViewHeader)
				{
					SortableCellViewHeader sortableColumnHeader = (SortableCellViewHeader)columnHeader;
					if (width != null) sortableColumnHeader.setWidth(Integer.parseInt(width.substring(0, width.length() - 2)));
					else if (columnPersist instanceof BaseComponent) sortableColumnHeader.setWidth(((BaseComponent)columnPersist).getSize().width);
				}
			}
		}
	}

	private void registerHeader(IPersist matchingElement, Component headerComponent)
	{
		elementToColumnHeader.put(matchingElement, headerComponent);
		updateHeader(headerComponent, elementToColumnIdentifierComponent.get(matchingElement));
	}

	private void updateHeaders()
	{
		Iterator<IPersist> it = elementToColumnHeader.keySet().iterator();

		while (it.hasNext())
		{
			IPersist element = it.next();
			Component columnHeader = elementToColumnHeader.get(element);
			Component columnIdentifier = elementToColumnIdentifierComponent.get(element);

			updateHeader(columnHeader, columnIdentifier);
		}
	}

	private void enableChildrenInContainer(MarkupContainer container, final boolean b)
	{
		container.visitChildren(new IVisitor<Component>()
		{
			public Object component(Component component)
			{
				if (b && !component.isEnabled())
				{
					// component may be disabled by scripting, do not enable it
					return CONTINUE_TRAVERSAL;
				}
				if (component instanceof IComponent)
				{
					((IComponent)component).setComponentEnabled(b);
				}
				else
				{
					component.setEnabled(b);
				}
				return CONTINUE_TRAVERSAL;
			}
		});
	}

	private void updateHeader(Component columnHeader, Component columnIdentifier)
	{
		columnHeader.setVisible(columnIdentifier.isVisible());
		columnHeader.setEnabled(isEnabled());
		if (columnHeader instanceof MarkupContainer) enableChildrenInContainer((MarkupContainer)columnHeader, isEnabled());
	}

	/**
	 * Requests focus for the cell in the web cell view corresponding to the selected record and to the given column identifier component.
	 * 
	 * @param columnIdentifierComponent the Component that identifies a column for java script.
	 */
	public void setColumnThatRequestsFocus(final Component columnIdentifierComponent)
	{
		if (currentData == null) return;
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
			Component cell = null;
			ListItem<IRecordInternal> li = (ListItem<IRecordInternal>)table.get(Integer.toString(selectedIndex));
			if (li != null)
			{
				Iterator< ? extends Component> cells = li.iterator();
				while (cells.hasNext())
				{
					Component someCell = CellContainer.getContentsForCell(cells.next());
					IPersist element = cellToElement.get(someCell);
					if (element != null && elementToColumnIdentifierComponent.get(element) == columnIdentifierComponent)
					{
						cell = someCell;
						break;
					}
				}
			}
			else
			{
				// the desired component is not created yet, however for requestFocus we only need the correct id
				cell = new WebComponent(columnIdentifierComponent.getId());
				cell.setOutputMarkupId(true);
			}

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
				Debug.log("Cannot find the cell to focus for record index " + selectedIndex + "and column " + columnIdentifierComponent.getMarkupId()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	Map<String, IPersist> labelsFor = new HashMap<String, IPersist>();

	private void createComponents(final IApplication app, final Form form, final AbstractBase view, final IDataProviderLookup dataProviderLookup,
		final IScriptExecuter el, final int viewStartY, final int endY, final ItemAdd output)
	{
		List<IPersist> elements = ComponentFactory.sortElementsOnPositionAndGroup(view.getAllObjectsAsList());
		int startX = 0;
		for (int i = 0; i < elements.size(); i++)
		{
			IPersist element = elements.get(i);
			if (element instanceof Field || element instanceof GraphicalComponent || element instanceof Bean)
			{

				if (element instanceof GraphicalComponent && ((GraphicalComponent)element).getLabelFor() != null)
				{
					labelsFor.put(((GraphicalComponent)element).getLabelFor(), element);
					continue;
				}

				Point l = ((IFormElement)element).getLocation();
				if (l == null)
				{
					continue; // unknown where to add
				}

				if (l.y >= viewStartY && l.y < endY)
				{
					IComponent c = ComponentFactory.createComponent(app, form, element, dataProviderLookup, el, false);

					initializeComponent((Component)c, view, element);
					output.add(element, (Component)c);

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

	private void initializeComponent(final Component c, AbstractBase view, Object element)
	{
		if (view instanceof Portal && c instanceof IDisplayData) // Don't know any other place for this
		{
			String id = ((IDisplayData)c).getDataProviderID();
			if (id != null && !id.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				if (id.startsWith(((Portal)view).getRelationName() + '.'))
				{
					((IDisplayData)c).setDataProviderID(id.substring(((Portal)cellview).getRelationName().length() + 1));
				}
			}
		}
		if (c instanceof WebDataCheckBox)
		{
			((WebDataCheckBox)c).setText(""); //$NON-NLS-1$
		}
		if (element != null)
		{
			// apply to this cell the state of the columnIdentifier IComponent
			PropertyCopy.copyElementProps((IComponent)elementToColumnIdentifierComponent.get(element), (IComponent)c);
		}
		else
		{
			Debug.log("Cannot find the IPersist element for cell " + c.getMarkupId()); //$NON-NLS-1$
		}
		if (c instanceof IDisplayData)
		{
			IDisplayData cdd = (IDisplayData)c;
			if (!(dal != null && dal.getFormScope() != null && cdd.getDataProviderID() != null && dal.getFormScope().get(cdd.getDataProviderID()) != Scriptable.NOT_FOUND)) // skip for form variables
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
		Model<String> componentClassModel = new Model<String>()
		{
			@Override
			public String getObject()
			{
				return c.getId();
			}
		};

		c.add(new AttributeModifier("class", true, componentClassModel)); //$NON-NLS-1$
	}

	/**
	 * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
	 */
	public void tableChanged(TableModelEvent e)
	{
		// If it is one row change, only update/touch that row 
		if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == e.getLastRow())
		{
			Component component = table.get(Integer.toString(e.getFirstRow()));
			if (component instanceof ListItem)
			{
				((ListItem)component).visitChildren(IProviderStylePropertyChanges.class, new IVisitor<Component>()
				{
					public Object component(Component comp)
					{
						((IProviderStylePropertyChanges)comp).getStylePropertyChanges().setChanged();
						return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
				});
			}
		}
		else
		{
			getStylePropertyChanges().setChanged();
		}

		// We try to detect when a sort has been done on the foundset, and we update the arrows in the header accordingly.
		// This is just an heuristic for filtering out the sort event from all table changed events that are raised.
		if (currentData != null && e.getColumn() == TableModelEvent.ALL_COLUMNS && e.getFirstRow() == 0)
		{
			List<SortColumn> sortCols = currentData.getSortColumns();
			if (sortCols != null && sortCols.size() > 0)
			{
				SortColumn sc = sortCols.get(0);

				for (IPersist persist : elementToColumnHeader.keySet())
				{
					Component comp = elementToColumnIdentifierComponent.get(persist);
					SortableCellViewHeader sortableCellViewHeader = (SortableCellViewHeader)elementToColumnHeader.get(persist);
					boolean solved = false;
					if (comp instanceof IDisplayData)
					{
						IDisplayData dispComp = (IDisplayData)comp;
						if (sc.getDataProviderID().equals(dispComp.getDataProviderID()))
						{
							boolean descending = sc.getSortOrder() == SortColumn.DESCENDING;
							sortableCellViewHeader.setResizeImage(descending ? SortableCellViewHeader.R_ARROW_UP : SortableCellViewHeader.R_ARROW_DOWN);
							headers.recordSort(comp.getMarkupId(), !descending);
							solved = true;
						}
					}
					if (!solved)
					{
						sortableCellViewHeader.setResizeImage(SortableCellViewHeader.R_ARROW_OFF);
					}
				}
			}
		}
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
	}

	/**
	 * @see wicket.MarkupContainer#onRender(wicket.markup.MarkupStream)
	 */
	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		getStylePropertyChanges().setRendered();
	}

	@Override
	protected void onBeforeRender()
	{
		WebTabPanel tabPanel = findParent(WebTabPanel.class);
		if (tabPanel != null)
		{
			Dimension tabSize = tabPanel.getTabSize();
			if (tabSize != null)
			{
				bodyHeightHint = (int)tabSize.getHeight();
				bodyHeightHint -= getOtherFormPartsHeight();
			}
		}
		else if (bodyHeightHint == -1)
		{
			bodyHeightHint = ((WebClientInfo)RequestCycle.get().getSession().getClientInfo()).getProperties().getBrowserHeight();
			bodyHeightHint -= getOtherFormPartsHeight();
		}

		if (isCurrentDataChanged)
		{
			if (bodyHeightHint == -1) bodyHeightHint = sizeHint;
			isCurrentDataChanged = false;
		}

		if (bodyHeightHint != -1)
		{
			int oldRowsPerPage = table.getRowsPerPage();

			Pair<Boolean, Pair<Integer, Integer>> rowsCalculation = needsMoreThanOnePage(bodyHeightHint);
			int maxRows = rowsCalculation.getRight().getLeft().intValue();
			table.setRowsPerPage(maxRows);

			// set headers width according to cell's width
			setHeadersWidth();
			int firstSelectedIndex = 0;
			if (currentData != null)
			{
				selectedIndexes = getSelectedIndexes();
				firstSelectedIndex = currentData.getSelectedIndex();
			}

			// if rowPerPage changed & the selected was visible, switch to the page so it remain visible
			int currentPage = table.getCurrentPage();
			if (maxRows != oldRowsPerPage && currentPage * oldRowsPerPage <= firstSelectedIndex && (currentPage + 1) * oldRowsPerPage > firstSelectedIndex) table.setCurrentPage(firstSelectedIndex < 1
				? 0 : firstSelectedIndex / maxRows);
		}
		pagingNavigator.setVisible(showPageNavigator && table.getPageCount() > 1);
		if (dataRendererOnRenderWrapper.getRenderEventExecutor().hasRenderCallback())
		{
			dataRendererOnRenderWrapper.getRenderEventExecutor().setRenderState(null, -1, false);
			dataRendererOnRenderWrapper.getRenderEventExecutor().fireOnRender(dataRendererOnRenderWrapper, false);
		}
		super.onBeforeRender();
	}

	public Object[] getComponents()
	{
		return elementToColumnIdentifierComponent.values().toArray();
	}

	public Object[] getHeaderComponents()
	{
		return elementToColumnHeader.values().toArray();
	}

	public PageableListView<IRecordInternal> getTable()
	{
		return this.table;
	}

	public void destroy()
	{
		if (dal != null) dal.destroy();
		if (currentData instanceof ISwingFoundSet)
		{
			((ISwingFoundSet)currentData).removeTableModelListener(this);
			((ISwingFoundSet)currentData).getSelectionModel().removeListSelectionListener(this);
		}
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
	private String bgColorScript;
	private List<Object> bgColorArgs;

	private boolean isReadOnly;

	private boolean validationEnabled = true;

	public void setModel(IFoundSetInternal fs)
	{
		if (currentData == fs) return;// if is same changes are seen by model listner

		if (currentData instanceof ISwingFoundSet)
		{
			((ISwingFoundSet)currentData).removeTableModelListener(this);
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
			data.setObject(FoundSetListWrapper.EMPTY);
		}
		else
		{
			// ListSelectionModel lsm = currentData.getSelectionModel();

			// int selected = currentData.getSelectedIndex();
			// table.setSelectionModel(lsm);
			// table.setModel((TableModel)currentData);
			data.setObject(new FoundSetListWrapper((FoundSet)currentData));
			// currentData.setSelectedIndex(selected);

			if (currentData instanceof ISwingFoundSet)
			{
				((ISwingFoundSet)currentData).addTableModelListener(this);
				((ISwingFoundSet)currentData).getSelectionModel().addListSelectionListener(this);
			}
			// lsm.addListSelectionListener(this);

			// valueChanged(null,stopEditing);
		}
		for (Object header : getHeaderComponents())
		{
			((SortableCellViewHeader)header).setResizeImage(SortableCellViewHeader.R_ARROW_OFF);
		}
	}

	public void valueChanged(ListSelectionEvent e)
	{
		//test if selection did move to another page
		if (currentData != null && !e.getValueIsAdjusting())
		{
			int newSelectedIndex = currentData.getSelectedIndex();
			int newPageIndex = newSelectedIndex / table.getRowsPerPage();
			if (table.getCurrentPage() != newPageIndex)
			{
				table.setCurrentPage(newPageIndex);
				// if table row selection color must work then this must be outside this if. 
				getStylePropertyChanges().setChanged();
			}
		}
	}


	public void setValidationEnabled(boolean b)
	{
		if (validationEnabled != b)
		{
			// find mode / edit mode switch
			getStylePropertyChanges().setChanged();
		}
		validationEnabled = b;
		dal.setFindMode(!b);
	}

	public boolean stopUIEditing(final boolean looseFocus)
	{
		Object hasInvalidValue = visitChildren(IDisplayData.class, new IVisitor<Component>()
		{
			public Object component(Component component)
			{
				if (!((IDisplayData)component).stopUIEditing(looseFocus))
				{
					return Boolean.TRUE;
				}
				return IVisitor.CONTINUE_TRAVERSAL;
			}
		});

		return hasInvalidValue != Boolean.TRUE;
	}

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

	public boolean isDisplayingMoreThanOneRecord()
	{
		return true;
	}

	public void setEditable(boolean findMode)
	{
	}

	/*
	 * scrollable---------------------------------------------------
	 */
	public void js_setScroll(int x, int y)
	{
		// todo ignore in webclient?
	}

	public int js_getScrollX()
	{
		// todo ignore in webclient?
		return 0;
	}

	public int js_getScrollY()
	{
		// todo ignore in webclient?
		return 0;
	}

	/*
	 * data related methods---------------------------------------------------
	 */
	public String js_getSortColumns()
	{
		if (currentData != null)
		{
			List<SortColumn> lst = currentData.getSortColumns();
			StringBuffer sb = new StringBuffer();
			if (lst.size() > 0)
			{
				for (int i = 0; i < lst.size(); i++)
				{
					SortColumn sc = lst.get(i);
					sb.append(sc.toString());
					sb.append(", "); //$NON-NLS-1$
				}
				sb.setLength(sb.length() - 2);
			}
			return sb.toString();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptPortalComponentMethods#js_getRecordIndex()
	 */
	public int js_getRecordIndex()
	{
		if (currentData != null) return currentData.getSelectedIndex() + 1;
		return 0;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptPortalComponentMethods#js_setRecordIndex(int)
	 */
	public void js_setRecordIndex(int i)
	{
		if (i >= 1 && i <= js_getMaxRecordIndex())
		{
			currentData.setSelectedIndex(i - 1);
		}
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptPortalComponentMethods#jsFunction_getSelectedIndex()
	 */
	public int jsFunction_getSelectedIndex()
	{
		return currentData.getSelectedIndex() + 1;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptPortalComponentMethods#jsFunction_setSelectedIndex(int)
	 */
	public void jsFunction_setSelectedIndex(int i)
	{
		if (i >= 1 && i <= js_getMaxRecordIndex())
		{
			currentData.setSelectedIndex(i - 1);
		}
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptPortalComponentMethods#js_getMaxRecordIndex()
	 */
	public int js_getMaxRecordIndex()
	{
		return currentData.getSize();
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptPortalComponentMethods#js_deleteRecord()
	 */
	public void js_deleteRecord()
	{
		if (currentData != null)
		{
			try
			{
				currentData.deleteRecord(currentData.getSelectedIndex());
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptPortalComponentMethods#js_newRecord(java.lang.Object[])
	 */
	public void js_newRecord(Object[] vargs)
	{
		boolean addOnTop = true;
		if (vargs != null && vargs.length >= 1 && vargs[0] instanceof Boolean)
		{
			addOnTop = ((Boolean)vargs[0]).booleanValue();
		}
		if (currentData != null)
		{
			try
			{
				int i = currentData.newRecord(addOnTop ? 0 : Integer.MAX_VALUE, true);
				currentData.setSelectedIndex(i);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptPortalComponentMethods#js_duplicateRecord(java.lang.Object[])
	 */
	public void js_duplicateRecord(Object[] vargs)
	{
		boolean addOnTop = true;
		if (vargs != null && vargs.length >= 1 && vargs[0] instanceof Boolean)
		{
			addOnTop = ((Boolean)vargs[0]).booleanValue();
		}
		if (currentData != null)
		{
			try
			{
				int i = currentData.duplicateRecord(currentData.getSelectedIndex(), addOnTop ? 0 : Integer.MAX_VALUE);
				currentData.setSelectedIndex(i);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}


	/*
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return isReadOnly;
	}

	public void js_setReadOnly(boolean b)
	{
		isReadOnly = b;
		getStylePropertyChanges().setChanged();
	}

	public boolean js_isReadOnly()
	{
		return isReadOnly;
	}


	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return "PORTAL"; //$NON-NLS-1$
	}

	/*
	 * name---------------------------------------------------
	 */
	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}

	public void setName(String n)
	{
		name = n;
	}

	private String name;

	public String getName()
	{
		return name;
	}


	/*
	 * border---------------------------------------------------
	 */
	private Border border;

	public void setBorder(Border border)
	{
		this.border = border;
	}

	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
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
	public Font getFont()
	{
		return font;
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(background);
	}

	public void js_setBgcolor(String bgcolor)
	{
		background = PersistHelper.createColor(bgcolor);
		jsChangeRecorder.setBgcolor(bgcolor);
	}

	private Color background;

	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	public Color getBackground()
	{
		return background;
	}


	/*
	 * fgcolor---------------------------------------------------
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(foreground);
	}

	public void js_setFgcolor(String fgcolor)
	{
		foreground = PersistHelper.createColor(fgcolor);
		jsChangeRecorder.setFgcolor(fgcolor);
	}

	private Color foreground;

	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	public Color getForeground()
	{
		return foreground;
	}


	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
		jsChangeRecorder.setBorder(spec);
	}

	/*
	 * visible---------------------------------------------------
	 */
	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
	}

	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean visible)
	{
		setVisible(visible);
		jsChangeRecorder.setVisible(visible);
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
			if (pagingNavigator != null) pagingNavigator.setEnabled(b);
			if (table != null) table.setEnabled(b);
			if (headers != null) headers.setEnabled(b);
			super.setEnabled(b);
			getStylePropertyChanges().setChanged();
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
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

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
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	public void js_setLocation(int x, int y)
	{
		location = new Point(x, y);
		jsChangeRecorder.setLocation(x, y);
	}

	public void setLocation(Point location)
	{
		this.location = location;
	}

	public Point getLocation()
	{
		return location;
	}

	/*
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		if (clientProperties == null)
		{
			clientProperties = new HashMap<Object, Object>();
		}
		clientProperties.put(key, value);
	}

	private Map<Object, Object> clientProperties;

	public Object js_getClientProperty(Object key)
	{
		if (clientProperties == null) return null;
		return clientProperties.get(key);
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

	public Dimension getSize()
	{
		return size;
	}

	public void js_setSize(int width, int height)
	{
		size = new Dimension(width, height);
		jsChangeRecorder.setSize(width, height, border, new Insets(0, 0, 0, 0), 0);
	}

	public Rectangle getWebBounds()
	{
		Dimension d = jsChangeRecorder.calculateWebSize(size.width, size.height, border, new Insets(0, 0, 0, 0), 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return jsChangeRecorder.getPaddingAndBorder(size.height, border, new Insets(0, 0, 0, 0), 0, null);
	}


	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public int js_getWidth()
	{
		return size.width;
	}

	public int js_getHeight()
	{
		return size.height;
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
			orderedPersistColumnIdentifierComponent.add(new PersistColumnIdentifierComponent(entry.getKey(), (IComponent)entry.getValue()));
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
			if (!(c instanceof WebBaseButton || c instanceof WebBaseLabel || !c.isEnabled() || (validationEnabled && c instanceof IScriptInputMethods && !((IScriptInputMethods)c).js_isEditable())))
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
			if (elementTabIndexes.size() > 0) jsChangeRecorder.setChanged();
			elementTabIndexes.clear();
		}
		else
		{
			jsChangeRecorder.setChanged();
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
	public String getToolTipText()
	{
		return tooltip;
	}


	private Object getListItemBgColor(ListItem<IRecordInternal> listItem, boolean isSelected)
	{
		Object color = null;
		final IRecordInternal rec = listItem.getModelObject();
		String rowBGColorProvider = getRowBGColorScript();
		Row rawData = null;
		if (rec != null && (rawData = rec.getRawData()) != null)
		{
			if (rowBGColorProvider != null)
			{
				// TODO type and name should be get somehow if this is possible, we have to know the specific cell/column for that.
				String type = null;//(renderer instanceof IScriptBaseMethods) ? ((IScriptBaseMethods)renderer).js_getElementType() : null;
				String cellName = null;//(renderer instanceof IScriptBaseMethods) ? ((IScriptBaseMethods)renderer).js_getName() : null;

				if (rawData.containsCalculation(rowBGColorProvider))
				{
					// TODO this should be done better....
					// isEdited is always false
					Record.VALIDATE_CALCS.set(Boolean.FALSE);
					try
					{
						color = rec.getParentFoundSet().getCalculationValue(
							rec,
							rowBGColorProvider,
							Utils.arrayMerge(new Object[] { new Integer(listItem.getIndex()), new Boolean(isSelected), type, cellName, Boolean.FALSE },
								Utils.parseJSExpressions(getRowBGColorArgs())), null);
					}
					finally
					{
						Record.VALIDATE_CALCS.set(null);
					}
				}
				else
				{
					try
					{
						FormController currentForm = dal.getFormController();
						color = currentForm.executeFunction(rowBGColorProvider, Utils.arrayMerge(new Object[] { new Integer(listItem.getIndex()), new Boolean(
							isSelected), type, cellName, currentForm.getName(), rec, Boolean.FALSE }, Utils.parseJSExpressions(getRowBGColorArgs())), false,
							null, true, null);
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
				}
			}

			StyleSheet ss = getStyleSheet();
			Style style = (listItem.getIndex() % 2 == 0) ? getOddStyle() : getEvenStyle(); // because index = 0 means record = 1
			if (ss != null && style != null)
			{
				color = PersistHelper.createColorString(ss.getBackground(style));
			}
		}

		return color;
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
		return false;
	}

	public boolean onDrop(JSDNDEvent event)
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
		// don't need this, ignore
		return null;
	}

	public String getDragFormName()
	{
		return getDataAdapterList().getFormController().getName();
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
			enableDragDrop = (cellviewPortal.getOnDragMethodID() > 0 || cellviewPortal.getOnDragEndMethodID() > 0 || cellviewPortal.getOnDragOverMethodID() > 0 || cellviewPortal.getOnDropMethodID() > 0);
		}
		else
		{
			Form form = formController.getForm();
			enableDragDrop = (form.getOnDragMethodID() > 0 || form.getOnDragEndMethodID() > 0 || form.getOnDragOverMethodID() > 0 || form.getOnDropMethodID() > 0);
		}

		if (enableDragDrop)
		{
			dragNdropController = formController;
			addDragNDropBehavior();
		}
	}

	public FormController getDragNDropController()
	{
		return dragNdropController;
	}

	private void addDragNDropBehavior()
	{
		DraggableBehavior compDragBehavior = new DraggableBehavior()
		{
			@Override
			protected void onDragEnd(String id, int x, int y, AjaxRequestTarget ajaxRequestTarget)
			{
				JSDNDEvent event = WebCellBasedView.this.createScriptEvent(EventType.onDragEnd, getDragComponent(), null);
				event.setData(getDragData());
				event.setDataMimeType(getDragDataMimeType());
				event.setDragResult(getDropResult() ? getCurrentDragOperation() : DRAGNDROP.NONE);
				WebCellBasedView.this.onDragEnd(event);

				super.onDragEnd(id, x, y, ajaxRequestTarget);
			}

			@Override
			protected void onDragStart(final String id, int x, int y, AjaxRequestTarget ajaxRequestTarget)
			{
				IComponent comp = getBindedComponentChild(id);
				JSDNDEvent event = WebCellBasedView.this.createScriptEvent(EventType.onDrag, comp, new Point(x, y));
				setCurrentDragOperation(WebCellBasedView.this.onDrag(event));
				setDragData(event.getData(), event.getDataMimeType());
				setDragComponent(comp);
				setDropResult(false);
			}

			@Override
			protected void onDrop(String id, final String targetid, int x, int y, AjaxRequestTarget ajaxRequestTarget)
			{
				if (getCurrentDragOperation() != DRAGNDROP.NONE)
				{
					IComponent comp = getBindedComponentChild(targetid);
					JSDNDEvent event = WebCellBasedView.this.createScriptEvent(EventType.onDrop, comp, new Point(x, y));
					event.setData(getDragData());
					event.setDataMimeType(getDragDataMimeType());
					setDropResult(WebCellBasedView.this.onDrop(event));
				}
			}

			@Override
			protected void onDropHover(String id, final String targetid, AjaxRequestTarget ajaxRequestTarget)
			{
				if (getCurrentDragOperation() != DRAGNDROP.NONE)
				{
					IComponent comp = getBindedComponentChild(targetid);
					JSDNDEvent event = WebCellBasedView.this.createScriptEvent(EventType.onDragOver, comp, null);
					event.setData(getDragData());
					event.setDataMimeType(getDragDataMimeType());
					WebCellBasedView.this.onDragOver(event);
				}
			}

			@Override
			public IComponent getBindedComponentChild(final String childId)
			{
				IComponent comp = super.getBindedComponentChild(childId);
				if (comp == null) comp = WebCellBasedView.this;
				return comp;
			}
		};
		compDragBehavior.setUseProxy(true);
		add(compDragBehavior);
	}

	public JSDNDEvent createScriptEvent(EventType type, IComponent dragSource, Point xy)
	{
		JSDNDEvent jsEvent = new JSDNDEvent();
		jsEvent.setType(type);
		jsEvent.setFormName(getDragFormName());
		if (dragSource instanceof IDataRenderer)
		{
			IDataRenderer dr = (IDataRenderer)dragSource;
			FormController fct = dr.getDataAdapterList().getFormController();
			jsEvent.setSource(fct.getFormScope());
			jsEvent.setElementName(fct.getName());
		}
		else
		{
			jsEvent.setSource(dragSource);
			if (dragSource != null)
			{
				if (dragSource instanceof Component)
				{
					WebCellBasedViewListItem listItem = ((Component)dragSource).findParent(WebCellBasedViewListItem.class);
					if (listItem != null)
					{
						IRecordInternal dragRecord = listItem.getModelObject();
						if (dragRecord instanceof Record) jsEvent.setRecord((Record)dragRecord);
					}
				}
				String dragSourceName = dragSource.getName();
				if (dragSourceName == null) dragSourceName = dragSource.getId();
				jsEvent.setElementName(dragSourceName);
			}
		}

		if (xy != null) jsEvent.setLocation(xy);

		return jsEvent;
	}

	public String getRowSelectionScript()
	{
		if (currentData == null) return null;
		List<Integer> indexToUpdate;
		if (bgColorScript != null && (indexToUpdate = getIndexToUpdate()) != null)
		{
			int firstRow = table.getCurrentPage() * table.getRowsPerPage();
			int lastRow = firstRow + table.getViewSize() - 1;
			int[] newSelectedIndexes = getSelectedIndexes();

			AppendingStringBuffer sab = new AppendingStringBuffer();
			for (int rowIdx : indexToUpdate)
			{
				if (rowIdx >= firstRow && rowIdx <= lastRow)
				{
					ListItem<IRecordInternal> selectedListItem = (ListItem<IRecordInternal>)table.get(Integer.toString(rowIdx));
					if (selectedListItem != null)
					{
						String selectedId = selectedListItem.getMarkupId();
						Object selectedColor = getListItemBgColor(selectedListItem, Arrays.binarySearch(newSelectedIndexes, rowIdx) >= 0);
						selectedColor = (selectedColor == null ? "" : selectedColor.toString()); //$NON-NLS-1$
						sab.append("Servoy.TableView.setRowBgColor('").append(selectedId).append("', '").append(selectedColor).append("');\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}

			String rowSelectionScript = sab.toString();
			if (rowSelectionScript.length() > 0) return rowSelectionScript;
		}
		return null;
	}

	public void updateRowComponentsRenderState(AjaxRequestTarget target)
	{
		if (currentData == null) return;
		List<Integer> indexToUpdate;
		if ((indexToUpdate = getIndexToUpdate()) != null)
		{
			int firstRow = table.getCurrentPage() * table.getRowsPerPage();
			int lastRow = firstRow + table.getViewSize() - 1;
			int[] newSelectedIndexes = getSelectedIndexes();

			for (int rowIdx : indexToUpdate)
			{
				if (rowIdx >= firstRow && rowIdx <= lastRow)
				{
					ListItem<IRecordInternal> selectedListItem = (ListItem<IRecordInternal>)table.get(Integer.toString(rowIdx));
					if (selectedListItem instanceof WebCellBasedViewListItem)
					{
						((WebCellBasedViewListItem)selectedListItem).updateComponentsRenderState(target, Arrays.binarySearch(newSelectedIndexes, rowIdx) >= 0);
					}
				}
			}

			selectedIndexes = newSelectedIndexes;
		}
	}

	@SuppressWarnings("nls")
	public String getColumnResizeScript()
	{
		if (resizedComponent instanceof IProviderStylePropertyChanges)
		{
			String tableId = getMarkupId();
			String columnId = resizedComponent.getMarkupId();
			String sWidth = (String)((IProviderStylePropertyChanges)resizedComponent).getStylePropertyChanges().getChanges().get("width"); //$NON-NLS-1$
			if (sWidth != null)
			{
				resizedComponent = null;
				return new AppendingStringBuffer("Servoy.TableView.setTableColumnWidth('").append(tableId).append("', '").append(columnId).append("', ").append(
					Integer.parseInt(sWidth.substring(0, sWidth.length() - 2))).append(")").toString();
			}
		}

		return null;
	}

	private List<Integer> getIndexToUpdate()
	{
		if (currentData == null || selectedIndexes == null) return null;

		List<Integer> indexesToUpdate = new ArrayList<Integer>();
		List<Integer> oldSelectedIndexes = new ArrayList<Integer>();
		for (int oldSelected : selectedIndexes)
			oldSelectedIndexes.add(new Integer(oldSelected));

		int[] newSelectedIndexes = getSelectedIndexes();
		for (int sel : newSelectedIndexes)
		{
			Integer selection = new Integer(sel);
			if (oldSelectedIndexes.indexOf(selection) == -1) indexesToUpdate.add(selection);
		}
		indexesToUpdate.addAll(oldSelectedIndexes);

		return indexesToUpdate.size() > 0 ? indexesToUpdate : null;
	}

	private int[] getSelectedIndexes()
	{
		if (currentData instanceof FoundSet) return ((FoundSet)currentData).getSelectedIndexes();
		else return new int[] { currentData.getSelectedIndex() };
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
		else
		{
			// iphone/mac issue (#185741)
			ClientInfo webClientInfo = Session.get().getClientInfo();
			if (webClientInfo instanceof WebClientInfo && ((WebClientInfo)webClientInfo).getProperties().isBrowserSafari())
			{
				reservedHeight = 5;
			}
		}
		int totalRealHeight = reservedHeight + getOtherFormPartsHeight();

		int maxRows = Math.max((height - reservedHeight) / maxHeight, 1);
		// if only 1px is missing for another row, increase the maxRows;
		// windows web clients does not return accurately the clientHeight property
		if (maxHeight - ((height - reservedHeight) % maxHeight) < 2) maxRows++;

		boolean moreThanOnePage = currentData != null && currentData.getSize() > maxRows;
		if (moreThanOnePage)
		{
			reservedHeight += 20; // the page navigator
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
		IScriptBaseMethods lastStretched = null;
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
				if (c instanceof IScriptBaseMethods)
				{
					IScriptBaseMethods ic = (IScriptBaseMethods)c;
					int thisDelta = delta * ic.js_getWidth() / totalWidthToStretch;
					consumedDelta += thisDelta;
					int newWidth = ic.js_getWidth() + thisDelta;

					int height = ic.js_getHeight();
					Iterator<Component> alreadyAddedComponents = cellToElement.keySet().iterator();
					if (alreadyAddedComponents.hasNext())
					{
						Component firstAddedComponent = alreadyAddedComponents.next();
						if ((firstAddedComponent instanceof IComponent)) height = ((IComponent)firstAddedComponent).getSize().height;
					}
					ic.js_setSize(newWidth, height);

					lastStretched = ic;
				}
			}
		}
		// we can have some leftover due to rounding errors, just put it into the last stretched column.
		if ((delta - consumedDelta != 0) && (lastStretched != null))
		{
			lastStretched.js_setSize(lastStretched.js_getWidth() + delta - consumedDelta, lastStretched.js_getHeight());
		}

		updateXLocationForColumns(getOrderedHeaders());
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
		return cellview instanceof Portal ? "PORTAL" : "FORM"; //$NON-NLS-1$ //$NON-NLS-2$
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
	public Style getOddStyle()
	{
		return oddStyle;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#getEvenStyle()
	 */
	public Style getEvenStyle()
	{
		return evenStyle;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#setStyles(javax.swing.text.html.StyleSheet, javax.swing.text.Style, javax.swing.text.Style)
	 */
	public void setStyles(StyleSheet styleSheet, Style oddStyle, Style evenStyle)
	{
		this.styleSheet = styleSheet;
		this.oddStyle = oddStyle;
		this.evenStyle = evenStyle;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#getStyleSheet()
	 */
	public StyleSheet getStyleSheet()
	{
		return styleSheet;
	}
}

class FindStateItemModel extends RecordItemModel
{

	private final IRecordInternal record;

	public FindStateItemModel(IRecordInternal r)
	{
		record = r;
	}

	@Override
	protected IRecordInternal getRecord()
	{
		return record;
	}

}

class FoundsetRecordItemModel extends RecordItemModel
{
	private static final long serialVersionUID = 1L;

	private transient IRecordInternal record;//we need to keep reference since pk can change

	/** The ListView's list model */
	private final ListView<IRecordInternal> listView;

	/* The list item's index */
	private final Object[] pk;

	private final int index;

	/**
	 * @param listView The ListView
	 * @param index 
	 * @param pk The pk of the record that must be shown
	 */
	public FoundsetRecordItemModel(ListView<IRecordInternal> listView, IRecordInternal r, int index)
	{
		super();
		record = r;
		this.pk = record.getPK();
		this.listView = listView;
		this.index = index;
	}

	public int getRowIndex()
	{
		return index;
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.RecordItemModel#getRecord()
	 */
	@Override
	protected IRecordInternal getRecord()
	{
		if (record == null)
		{
			// Re-attach the model object based on index and ListView model object
			Object object = listView.getModelObject();
			if (object instanceof FoundSetListWrapper)
			{
				record = ((FoundSetListWrapper)object).getRecord(pk);
			}
		}
		return record;
	}
}
