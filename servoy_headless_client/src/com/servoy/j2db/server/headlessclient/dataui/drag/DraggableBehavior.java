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
package com.servoy.j2db.server.headlessclient.dataui.drag;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
public abstract class DraggableBehavior extends AbstractServoyDefaultAjaxBehavior
{
	private static final long serialVersionUID = 1L;

	public static final String ACTION_DRAG_START = "aStart";
	public static final String ACTION_DRAG_END = "aEnd";
	public static final String ACTION_DROP_HOVER = "aHover";
	public static final String ACTION_DROP = "aDrop";

	public static final String PARAM_ACTION = "a";
	public static final String PARAM_DRAGGABLE_ID = "draggableID";
	public static final String PARAM_TARGET_ID = "targetID";
	public static final String PARAM_X = "xc";
	public static final String PARAM_Y = "yc";

	private boolean bUseProxy;
	private boolean bXConstraint;
	private boolean bYConstraint;

	private boolean isRenderOnHead = true;

	protected static boolean dropResult;

	public void setRenderOnHead(boolean isRenderOnHead)
	{
		this.isRenderOnHead = isRenderOnHead;
	}

	public void setUseProxy(boolean bUseProxy)
	{
		this.bUseProxy = bUseProxy;
	}

	public boolean isUseProxy()
	{
		return this.bUseProxy;
	}

	public void setXConstraint(boolean bXConstraint)
	{
		this.bXConstraint = bXConstraint;
	}

	public boolean isXConstraint()
	{
		return this.bXConstraint;
	}

	public void setYConstraint(boolean bYConstraint)
	{
		this.bYConstraint = bYConstraint;
	}

	public boolean isYConstraint()
	{
		return this.bYConstraint;
	}

	public void setDragData(Object dragData)
	{
		((WebClientSession)Session.get()).getDNDSessionInfo().setData(dragData);
	}

	public Object getDragData()
	{
		return ((WebClientSession)Session.get()).getDNDSessionInfo().getData();
	}

	public void setCurrentDragOperation(int currentDragOperation)
	{
		((WebClientSession)Session.get()).getDNDSessionInfo().setCurrentOperation(currentDragOperation);
	}

	public int getCurrentDragOperation()
	{
		return ((WebClientSession)Session.get()).getDNDSessionInfo().getCurrentOperation();
	}

	public void setDragComponent(IComponent component)
	{
		((WebClientSession)Session.get()).getDNDSessionInfo().setComponent(component);
	}

	public IComponent getDragComponent()
	{
		return ((WebClientSession)Session.get()).getDNDSessionInfo().getComponent();
	}

	public void setDropResult(boolean dropResult)
	{
		((WebClientSession)Session.get()).getDNDSessionInfo().setDropResult(dropResult);
	}

	public boolean getDropResult()
	{
		return ((WebClientSession)Session.get()).getDNDSessionInfo().getDropResult();
	}

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
			onDragStart(id, Integer.parseInt(componentRequest.getParameter(PARAM_X)), Integer.parseInt(componentRequest.getParameter(PARAM_Y)),
				ajaxRequestTarget);
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

	protected abstract void onDragStart(String id, int x, int y, AjaxRequestTarget ajaxRequestTarget);

	protected void onDragEnd(String id, int x, int y, AjaxRequestTarget ajaxRequestTarget)
	{
		setDragData(null);
		setCurrentDragOperation(DRAGNDROP.NONE);
	}

	protected abstract void onDropHover(String id, String targeid, AjaxRequestTarget ajaxRequestTarget);

	protected abstract void onDrop(String id, String targetid, int x, int y, AjaxRequestTarget ajaxRequestTarget);
}