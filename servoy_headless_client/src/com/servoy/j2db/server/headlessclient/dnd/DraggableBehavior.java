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
package com.servoy.j2db.server.headlessclient.dnd;

import org.apache.wicket.Component;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IIgnoreDisabledComponentBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;

import com.servoy.j2db.dnd.DRAGNDROP;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.server.headlessclient.dataui.AbstractServoyDefaultAjaxBehavior;
import com.servoy.j2db.server.headlessclient.dataui.WebEventExecutor;
import com.servoy.j2db.ui.IComponent;

/**
 * Class used to add drag and drop support for web components 
 * 
 * @author gboros
 */
public abstract class DraggableBehavior extends AbstractServoyDefaultAjaxBehavior implements IIgnoreDisabledComponentBehavior
{
	private static final long serialVersionUID = 1L;

	/**
	 * Drag start action name.
	 */
	public static final String ACTION_DRAG_START = "aStart";
	/**
	 * Drag end action name.
	 */
	public static final String ACTION_DRAG_END = "aEnd";
	/**
	 * Drop hover action name.
	 */
	public static final String ACTION_DROP_HOVER = "aHover";
	/**
	 * Drop action name.
	 */
	public static final String ACTION_DROP = "aDrop";
	/**
	 * Action parameter name.
	 */
	public static final String PARAM_ACTION = "a";
	/**
	 * Draggable element id parameter name.
	 */
	public static final String PARAM_DRAGGABLE_ID = "draggableID";
	/**
	 * Target element id parameter name.
	 */
	public static final String PARAM_TARGET_ID = "targetID";
	/**
	 * Mouse x coordinate parameter name.
	 */
	public static final String PARAM_X = "xc";
	/**
	 * Mouse y coordinate parameter name.
	 */
	public static final String PARAM_Y = "yc";

	private boolean bUseProxy;
	private boolean bXConstraint;
	private boolean bYConstraint;

	private boolean isRenderOnHead = true;

	protected static boolean dropResult;

	/**
	 * Sets whatever the behavior javascript code will be rendered
	 * in the response head, or on the body
	 * 
	 * @param isRenderOnHead true to render the behavior javascript in the response head
	 */
	public void setRenderOnHead(boolean isRenderOnHead)
	{
		this.isRenderOnHead = isRenderOnHead;
	}

	/**
	 * Sets whatever a floating element (proxy) is showing for the dragging element.
	 * 
	 * @param bUseProxy whatever to show floating element for the drag
	 */
	public void setUseProxy(boolean bUseProxy)
	{
		this.bUseProxy = bUseProxy;
	}

	/**
	 * Returns whatever a floating element (proxy) is showing for the dragging element.
	 * 
	 * @return whatever floating element is showing for the drag
	 */
	public boolean isUseProxy()
	{
		return this.bUseProxy;
	}

	/**
	 * Sets whatever dragging is only possible vertically.
	 * 
	 * @param bXConstraint whatever dragging is only possible vertically
	 */
	public void setXConstraint(boolean bXConstraint)
	{
		this.bXConstraint = bXConstraint;
	}

	/**
	 * Returns whatever dragging is only possible vertically.
	 * 
	 * @return whatever dragging is only possible vertically.
	 */
	public boolean isXConstraint()
	{
		return this.bXConstraint;
	}

	/**
	 * Sets whatever dragging is only possible horizontally.
	 * 
	 * @param bYConstraint whatever dragging is only possible horizontally
	 */
	public void setYConstraint(boolean bYConstraint)
	{
		this.bYConstraint = bYConstraint;
	}

	/**
	 * Returns whatever dragging is only possible horizontally.
	 * 
	 * @return whatever dragging is only possible horizontally.
	 */
	public boolean isYConstraint()
	{
		return this.bYConstraint;
	}

	/**
	 * Sets the dragging data.
	 * 
	 * @param dragData the data
	 * @param mimeType the data mime type
	 */
	public void setDragData(Object dragData, String mimeType)
	{
		((WebClientSession)Session.get()).getDNDSessionInfo().setData(dragData, mimeType);
	}

	/**
	 * Gets the drag data.
	 * 
	 * @return the drag data
	 */
	public Object getDragData()
	{
		return ((WebClientSession)Session.get()).getDNDSessionInfo().getData();
	}

	/**
	 * Gets the drag data mime type.
	 * 
	 * @return the drag data mime type
	 */
	public String getDragDataMimeType()
	{
		return ((WebClientSession)Session.get()).getDNDSessionInfo().getMimeType();
	}

	/**
	 * Sets the current drag operation, a DRAGNDROP constant or a combination of that
	 * 
	 * @param currentDragOperation
	 */
	public void setCurrentDragOperation(int currentDragOperation)
	{
		((WebClientSession)Session.get()).getDNDSessionInfo().setCurrentOperation(currentDragOperation);
	}

	/**
	 * Returns the current drag operation, a DRAGNDROP constant or a combination of that
	 * @return
	 */
	public int getCurrentDragOperation()
	{
		return ((WebClientSession)Session.get()).getDNDSessionInfo().getCurrentOperation();
	}

	/**
	 * Sets the drag component.
	 * 
	 * @param component that is dragged
	 */
	public void setDragComponent(IComponent component)
	{
		((WebClientSession)Session.get()).getDNDSessionInfo().setComponent(component);
	}

	/**
	 * Returns the drag component.
	 * 
	 * @return the drag component
	 */
	public IComponent getDragComponent()
	{
		return ((WebClientSession)Session.get()).getDNDSessionInfo().getComponent();
	}

	/**
	 * Sets the drop result.
	 * 
	 * @param dropResult whatever it was a successful drop
	 */
	public void setDropResult(boolean dropResult)
	{
		((WebClientSession)Session.get()).getDNDSessionInfo().setDropResult(dropResult);
	}

	/**
	 * Returns the drop result.
	 * 
	 * @return whatever it was a successful drop
	 */
	public boolean getDropResult()
	{
		return ((WebClientSession)Session.get()).getDNDSessionInfo().getDropResult();
	}

	/**
	 * Get the child component with the specified id.
	 * 
	 * @param childId id of child component
	 * 
	 * @return the child component or null if not found
	 */
	public IComponent getBindedComponentChild(final String childId)
	{
		IComponent bindedComponentChild = null;
		Component bindedComponent = getComponent();
		if (bindedComponent instanceof MarkupContainer)
		{
			bindedComponentChild = (IComponent)((MarkupContainer)bindedComponent).visitChildren(IComponent.class, new IVisitor<Component>()
			{
				public Object component(Component component)
				{
					if (component.getMarkupId().equals(childId))
					{
						return component;
					}
					return IVisitor.CONTINUE_TRAVERSAL;
				}
			});
		}

		return bindedComponentChild;
	}

	@Override
	protected void onComponentRendered()
	{
		super.onComponentRendered();
		if (!isRenderOnHead) WebEventExecutor.generateDragAttach(getComponent(), null);
	}

	@Override
	protected void respond(AjaxRequestTarget ajaxRequestTarget)
	{
		Page componentPage = getComponent().getPage();
		Request componentRequest = getComponent().getRequest();
		String action = componentRequest.getParameter(PARAM_ACTION);
		String id = componentRequest.getParameter(PARAM_DRAGGABLE_ID);

		if (ACTION_DRAG_START.equals(action))
		{
			boolean dragStartReturn = onDragStart(id, Integer.parseInt(componentRequest.getParameter(PARAM_X)),
				Integer.parseInt(componentRequest.getParameter(PARAM_Y)), ajaxRequestTarget);
			if (!dragStartReturn) ajaxRequestTarget.appendJavascript("YAHOO.util.DragDropMgr.stopDrag(Servoy.DD.mouseDownEvent, false);");
		}
		else if (ACTION_DRAG_END.equals(action))
		{
			onDragEnd(id, Integer.parseInt(componentRequest.getParameter(PARAM_X)), Integer.parseInt(componentRequest.getParameter(PARAM_Y)), ajaxRequestTarget);
		}
		else if (ACTION_DROP_HOVER.equals(action))
		{
			onDropHover(id, componentRequest.getParameter(PARAM_TARGET_ID), ajaxRequestTarget);
		}
		else if (ACTION_DROP.equals(action))
		{
			onDrop(id, componentRequest.getParameter(PARAM_TARGET_ID), Integer.parseInt(componentRequest.getParameter(PARAM_X)),
				Integer.parseInt(componentRequest.getParameter(PARAM_Y)), ajaxRequestTarget);
		}

		WebEventExecutor.generateResponse(ajaxRequestTarget, componentPage);
	}


	/**
	 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#renderHead(org.apache.wicket.markup.html.IHeaderResponse)
	 */
	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		if (isRenderOnHead) WebEventExecutor.generateDragAttach(getComponent(), response);
	}

	/**
	 * Called when a drag is started.
	 * 
	 * @param id drag component id
	 * @param x mouse x coordinate
	 * @param y mouse y coordinate
	 * @param ajaxRequestTarget
	 * @return whatever the drag can start
	 */
	protected abstract boolean onDragStart(String id, int x, int y, AjaxRequestTarget ajaxRequestTarget);

	/**
	 * Called when a drag ends.
	 * 
	 * @param id target component id
	 * @param x mouse x coordinate
	 * @param y mouse y coordinate
	 * @param ajaxRequestTarget
	 */
	protected void onDragEnd(String id, int x, int y, AjaxRequestTarget ajaxRequestTarget)
	{
		setDragData(null, null);
		setCurrentDragOperation(DRAGNDROP.NONE);
	}

	/**
	 * Called on drop hover.
	 * 
	 * @param id drag component id
	 * @param targeid target component id
	 * @param ajaxRequestTarget
	 */
	protected abstract void onDropHover(String id, String targeid, AjaxRequestTarget ajaxRequestTarget);

	/**
	 * Called on drop.
	 * @param id drag component id
	 * @param targetid target component id
	 * @param x mouse x coordinate
	 * @param y mouse y coordinate
	 * @param ajaxRequestTarget
	 */
	protected abstract void onDrop(String id, String targetid, int x, int y, AjaxRequestTarget ajaxRequestTarget);
}