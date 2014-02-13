/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.webclient2;

import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.IBasicFormManager.History;
import com.servoy.j2db.IBasicMainContainer;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;

/**
 * @author jcompagner
 *
 */
public class WebSocketRuntimeWindow extends RuntimeWindow implements IBasicMainContainer
{

	private static final String DIALOG_SERVICE = "$dialogService";

	private final History history;
	private final IWebSocketApplication application;
	private int x = -1;
	private int y = -1;
	private int width = -1;
	private int height = -1;
	private boolean visible;
	private String formName;

	private boolean titleSetThroughScripting;

	/**
	 * @param application
	 * @param windowName
	 * @param windowType
	 * @param parentWindow
	 */
	protected WebSocketRuntimeWindow(IWebSocketApplication application, String windowName, int windowType, RuntimeWindow parentWindow)
	{
		super(application, windowName, windowType, parentWindow);
		this.application = application;
		this.history = new History(application, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicMainContainer#getContainerName()
	 */
	@Override
	public String getContainerName()
	{
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicMainContainer#getController()
	 */
	@Override
	public IWebFormController getController()
	{
		if (formName == null) return null;
		return application.getFormManager().getForm(formName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicMainContainer#setController(com.servoy.j2db.IFormController)
	 */
	@Override
	public void setController(IFormController form)
	{
		this.formName = form != null ? form.getName() : null;
		// todo should this now be pushed?
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicMainContainer#getHistory()
	 */
	@Override
	public History getHistory()
	{
		return history;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#resetBounds()
	 */
	@Override
	public void resetBounds()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#setLocation(int, int)
	 */
	@Override
	public void setLocation(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#getX()
	 */
	@Override
	public int getX()
	{
		return x;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#getY()
	 */
	@Override
	public int getY()
	{
		return y;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#setSize(int, int)
	 */
	@Override
	public void setSize(int width, int height)
	{
		this.width = width;
		this.height = height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#getWidth()
	 */
	@Override
	public int getWidth()
	{
		return width;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#getHeight()
	 */
	@Override
	public int getHeight()
	{
		return height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#setTitle(java.lang.String, boolean)
	 */
	@Override
	public void setTitle(String title, boolean delayed)
	{
		// this call is done through scripting, remember this title.
		titleSetThroughScripting = true;
		super.setTitle(title);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(String title)
	{
		// if title is set through scripting then ignore calls to this one (done by form manager)
		if (!titleSetThroughScripting)
		{
			super.setTitle(title);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#isVisible()
	 */
	@Override
	public boolean isVisible()
	{
		return visible;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#toFront()
	 */
	@Override
	public void toFront()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#toBack()
	 */
	@Override
	public void toBack()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#getWrappedObject()
	 */
	@Override
	public Object getWrappedObject()
	{
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#hideUI()
	 */
	@Override
	public void hideUI()
	{
		visible = false;
		application.getActiveWebSocketClientEndpoint().executeServiceCall(DIALOG_SERVICE, "dismiss", new Object[] { getName() });

		// resume
		if (windowType == JSWindow.MODAL_DIALOG && application.getEventDispatcher() != null)
		{
			application.getEventDispatcher().resume(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.RuntimeWindow#doOldShow(java.lang.String, boolean, boolean)
	 */
	@Override
	protected void doOldShow(String formName, boolean closeAll, boolean legacyV3Behavior)
	{
		application.getFormManager().showFormInContainer(formName, this, getTitle(), true, windowName);
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("title", getTitle());
		Form form = application.getFlattenedSolution().getForm(formName);
		arguments.put("form", form);
		int wdth = width;
		int hght = height;
		if (wdth == -1) wdth = form.getSize().width;
		if (hght == -1) hght = form.getSize().height;
		Map<String, Integer> size = new HashMap<>();
		size.put("width", Integer.valueOf(wdth));
		size.put("height", Integer.valueOf(hght));
		arguments.put("size", size);
		application.getActiveWebSocketClientEndpoint().executeServiceCall(DIALOG_SERVICE, "show", new Object[] { getName(), arguments });
		visible = true;

		if (windowType == JSWindow.MODAL_DIALOG && application.getEventDispatcher() != null)
		{
			application.getEventDispatcher().suspend(this);
		}
	}
}
