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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupElement;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.WicketTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.resolver.IComponentResolver;
import org.apache.wicket.model.Model;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.server.headlessclient.yui.YUILoader;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IAnchorConstants;
import com.servoy.j2db.util.Utils;

/**
 * A component that renders the header of a {@link WebCellBasedView}
 * 
 * @author jcompagner,jblok
 */
public class SortableCellViewHeaders extends WebMarkupContainer implements IProviderStylePropertyChanges
{
	private static final long serialVersionUID = 1L;

	/** Each SortableTableHeader (without 's) must be attached to a group. */
	final private SortableCellViewHeaderGroup group;
	private final AbstractBase cellview;
	private final WebCellBasedView view;
	private final IApplication application;
	private final IHeaders headerManager;
	private final Form form;

	/**
	 * Construct.
	 * 
	 * @param view
	 * @param id The component's id; must not be null
	 * @param listView the underlying ListView
	 * @param cellview
	 * @param application
	 * @param headerManager the object who's headers will be populated with (IPersist, Component) pairs according to the header components that represent each
	 *            column - column given by the IPersist.
	 */
	public SortableCellViewHeaders(Form form, WebCellBasedView view, String id, final ServoyListView listView, AbstractBase cellview, IApplication application,
		Map<String, Boolean> initialSortMap, IHeaders headerManager)
	{
		super(id);//id is normally 'header'
		this.setOutputMarkupId(true);
		this.form = form;
		this.view = view;
		this.cellview = cellview;
		this.application = application;
		this.headerManager = headerManager;
		group = new SortableCellViewHeaderGroup(form, listView, cellview);
		if (initialSortMap != null) group.recordSort(initialSortMap);
		if (view.isScrollMode())
		{
			add(new StyleAppendingModifier(new Model<String>()
			{
				@Override
				public String getObject()
				{
					return "position: absolute; overflow: hidden; left: 0px; top: 0px;"; //$NON-NLS-1$
				}
			}));
		}
	}

	@Override
	public void renderHead(HtmlHeaderContainer headerContainer)
	{
		super.renderHead(headerContainer);
		if (isReorderableOrResizable()) YUILoader.renderDragNDrop(headerContainer.getHeaderResponse());
	}

	private boolean isReorderableOrResizable()
	{
		if (Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX"))) //$NON-NLS-1$
		{
			boolean isReorderable = false;
			boolean isResizable = false;

			Iterator<IPersist> iter = cellview.getAllObjects();
			while (iter.hasNext())
			{
				IPersist element = iter.next();
				if (element instanceof ISupportAnchors)
				{
					int anchors = ((ISupportAnchors)element).getAnchors();
					isResizable = ((anchors & IAnchorConstants.EAST) == IAnchorConstants.EAST) && ((anchors & IAnchorConstants.WEST) == IAnchorConstants.WEST);
					isResizable = isResizable && (!(cellview instanceof Portal) || ((Portal)cellview).getResizable());
					if (isResizable) return true;
					isReorderable = !(((anchors & IAnchorConstants.NORTH) == IAnchorConstants.NORTH) && ((anchors & IAnchorConstants.SOUTH) == IAnchorConstants.SOUTH));
					isReorderable = isReorderable && (!(cellview instanceof Portal) || ((Portal)cellview).getReorderable());
					if (isReorderable) return true;
				}
			}
		}

		return false;
	}

	private boolean resolve(MarkupStream markupStream, ComponentTag tag, String id)
	{
		if (tag.getName().equalsIgnoreCase("th")) //$NON-NLS-1$
		{
			// Get component name
			final String th_component_id = id;
			if ((th_component_id != null) && (get(th_component_id) == null))
			{
				final boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
				SortableCellViewHeader headerComponent = new SortableCellViewHeader(form, view, th_component_id, group, cellview, useAJAX, application);
				headerComponent.setOutputMarkupPlaceholderTag(true); // prepare for invisibility
				registerHeaderComponent(headerComponent);
				autoAdd(headerComponent, markupStream);
				headerComponent.resetAutoAdd();
				return true;
			}
		}
		return false;
	}

	private void registerHeaderComponent(SortableCellViewHeader headerComponent)
	{
		String id = headerComponent.getId();
		IPersist matchingElement = null;

		try
		{
			Iterator<IPersist> allElements = cellview.getAllObjects();
			while (allElements.hasNext())
			{
				IPersist someElement = allElements.next();
				if ((someElement instanceof Field || someElement instanceof GraphicalComponent || someElement instanceof Bean) &&
					id.equals(ComponentFactory.getWebID(form, someElement)) && (someElement instanceof ISupportName))
				{
//					// column headers cannot be changed from JS if the according element is not named
//					// so no need to link them
//					String name = ((ISupportName)someElement).getName();
//					if (name != null && name.trim().length() != 0)
//					{
					matchingElement = someElement;
					break;
//					}
				}
			}
		}
		catch (Exception e)
		{
			Debug.log("Cannot link a header component to it's element", e); //$NON-NLS-1$
		}

		if (matchingElement != null)
		{
			headerManager.registerHeader(matchingElement, headerComponent);
		}
	}


	/**
	 * Scan the related markup and attach a SortableListViewHeader to each &lt;th&gt; tag found.
	 */
	@Override
	protected void onRender(final MarkupStream markupStream)
	{
		// Must be <thead> tag
		ComponentTag tag = markupStream.getTag();
		checkComponentTag(tag, "thead"); //$NON-NLS-1$

		// Continue with default behaviour
		super.onRender(markupStream);
		getStylePropertyChanges().setRendered();
	}


	@Override
	protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag)
	{
		renderSortableCellViewHeaderTagBody(markupStream, openTag);
	}

	/**
	 * Renders markup for the body of a ComponentTag from the current position in the given markup stream. If the open tag passed in does not require a close
	 * tag, nothing happens. Markup is rendered until the closing tag for openTag is reached.
	 * 
	 * @param markupStream The markup stream
	 * @param openTag The open tag
	 */

	private int renderColumnIdx;
	private int headerMarkupStartIdx;
	private ArrayList<Component> orderedHeaders;
	private ArrayList<String> orderedHeaderIds;

	private void renderSortableCellViewHeaderTagBody(final MarkupStream markupStream, final ComponentTag openTag)
	{
		renderColumnIdx = 0;
		headerMarkupStartIdx = markupStream.getCurrentIndex();
		orderedHeaders = view.getOrderedHeaders();
		orderedHeaderIds = view.getOrderedHeaderIds();

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
		if (render == false)
		{
			// Tags like <p> do not require a close tag, but they may have.
			render = !openTag.hasNoCloseTag();
		}

		if (render == true)
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
			if (component != null && orderedHeaders.get(renderColumnIdx) != null)
			{
				if (component instanceof SortableCellViewHeader)
				{
					int currentIdx = markupStream.getCurrentIndex();
					renderHeader(renderColumnIdx, markupStream);
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
					if (container instanceof SortableCellViewHeaders)
					{
						// we should created the corect header, use the id from the orderedHeaders
						String headerId = orderedHeaderIds.get(renderColumnIdx);
						renderColumnIdx++;
						if (resolve(markupStream, tag, headerId))
						{
							return;
						}

					}

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
				final List<IComponentResolver> componentResolvers = getApplication().getPageSettings().getComponentResolvers();
				final Iterator<IComponentResolver> iterator = componentResolvers.iterator();
				while (iterator.hasNext())
				{
					final IComponentResolver resolver = iterator.next();
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

	private void renderHeader(int headerIdx, final MarkupStream markupStream)
	{
		Component header = orderedHeaders.get(headerIdx);

		markupStream.setCurrentIndex(headerMarkupStartIdx);
		boolean found = false;
		MarkupElement element;

		while (!found)
		{
			element = markupStream.next();
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
					if (component.equals(header))
					{
						component.render(markupStream);
						found = true;
					}
				}
			}
		}
	}


	protected ChangesRecorder jsChangeRecorder = new ChangesRecorder(null, null);

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
	}

	public void recordSort(Map<String, Boolean> sortMap)
	{
		group.recordSort(sortMap);
	}
}
