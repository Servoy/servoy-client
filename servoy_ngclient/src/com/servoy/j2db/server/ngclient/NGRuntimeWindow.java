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

package com.servoy.j2db.server.ngclient;

import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.IBasicFormManager.History;
import com.servoy.j2db.IBasicMainContainer;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.server.ngclient.component.WebFormController;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class NGRuntimeWindow extends RuntimeWindow implements IBasicMainContainer
{
	private final History history;
	private int x = -1;
	private int y = -1;
	private int width = -1;
	private int height = -1;
	private boolean visible;
	private String formName;

	/**
	 * @param application
	 * @param windowName
	 * @param windowType
	 * @param parentWindow
	 */
	protected NGRuntimeWindow(INGApplication application, String windowName, int windowType, RuntimeWindow parentWindow)
	{
		super(application, windowName, windowType, parentWindow);
		this.history = new History(application, this);
	}

	@Override
	public INGApplication getApplication()
	{
		return (INGApplication)super.getApplication();
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
		return getApplication().getFormManager().getForm(formName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicMainContainer#setController(com.servoy.j2db.IFormController)
	 */
	@Override
	public void setController(IFormController form)
	{
		if (form != null)
		{
			this.formName = form.getName();
			switchForm((WebFormController)form);
		}
		else
		{
			this.formName = null;
		}
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
		this.storeBounds = false;
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "resetBounds", new Object[] { this.getName() });

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.scripting.RuntimeWindow#setLocation(int, int)
	 */
	@Override
	public void setLocation(int x, int y)
	{
		if (this.x != x || this.y != y)
		{
			this.x = x;
			this.y = y;
			Map<String, Integer> location = new HashMap<>();
			location.put("x", x);
			location.put("y", y);
			getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "setLocation",
				new Object[] { this.getName(), location });
		}
	}

	public void updateLocation(int x, int y)
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
		if (this.width != width || this.width != width)
		{
			this.width = width;
			this.height = height;
			Map<String, Integer> size = new HashMap<>();
			size.put("width", width);
			size.put("height", height);
			getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "setSize",
				new Object[] { this.getName(), size });
		}
	}

	public void updateSize(int width, int height)
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


	@Override
	public void setInitialBounds(int x, int y, int width, int height)
	{
		super.setInitialBounds(x, y, width, height);
		Map<String, Integer> initialBounds = new HashMap<>();
		initialBounds.put("x", this.initialBounds.x);
		initialBounds.put("y", this.initialBounds.y);
		initialBounds.put("width", this.initialBounds.width);
		initialBounds.put("height", this.initialBounds.height);
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "setInitialBounds",
			new Object[] { this.getName(), initialBounds });
	}

	@Override
	public void setTitle(String title, boolean delayed)
	{
		super.setTitle(title);
		sendTitle(title);
	}

	@Override
	public void setTitle(String title)
	{
		sendTitle(title);
	}

	private void sendTitle(String title)
	{
		String titleString = "";
		if (windowType == 2)
		{
			Solution solution = getApplication().getSolution();
			String solutionTitle = solution.getTitleText();
			if (solutionTitle == null)
			{
				titleString = solution.getName();
			}
			else if (!solutionTitle.equals("<empty>")) //$NON-NLS-1$
			{
				titleString = solutionTitle;
			}

			titleString = getApplication().getI18NMessageIfPrefixed(titleString);

			if (title != null && !title.trim().equals("") && !"<empty>".equals(title) && title != null) //$NON-NLS-1$ //$NON-NLS-2$
			{
				String nameString = getApplication().getI18NMessageIfPrefixed(title);
				IWebFormController formController = getController();
				if (formController != null)
				{
					String name2 = Text.processTags(nameString, formController.getFormUI().getDataAdapterList());
					if (name2 != null) nameString = name2;
				}
				else
				{
					String name2 = Text.processTags(nameString, TagResolver.createResolver(new PrototypeState(null)));
					if (name2 != null) nameString = name2;
				}
				if (!nameString.trim().equals("")) //$NON-NLS-1$
				{
					if ("".equals(titleString)) //$NON-NLS-1$
					{
						titleString += nameString;
					}
					else
					{
						titleString += " - " + nameString; //$NON-NLS-1$
					}
				}
			}
			String appName = "Servoy Web Client"; //$NON-NLS-1$
			boolean branding = Utils.getAsBoolean(getApplication().getSettings().getProperty("servoy.branding", "false")); //$NON-NLS-1$ //$NON-NLS-2$
			String appTitle = getApplication().getSettings().getProperty("servoy.branding.windowtitle"); //$NON-NLS-1$
			if (branding && appTitle != null)
			{
				appName = appTitle;
			}
			if (titleString.equals("")) //$NON-NLS-1$
			{
				titleString = appName;
			}
			else
			{
				titleString += " - " + appName; //$NON-NLS-1$
			}
		}
		else
		{
			titleString = title;
		}
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "setTitle",
			new Object[] { this.getName(), titleString });
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
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "toFront", new Object[] { this.getName() });

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.scripting.RuntimeWindow#toBack()
	 */
	@Override
	public void toBack()
	{
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "toBack", new Object[] { this.getName() });
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

	@Override
	public void setOpacity(float opacity)
	{
		super.setOpacity(opacity);
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "setOpacity", new Object[] { getName(), opacity });
	}

	@Override
	public void setResizable(boolean resizable)
	{
		super.setResizable(resizable);
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "setResizable",
			new Object[] { getName(), resizable });
	}

	@Override
	public void setUndecorated(boolean undecorated)
	{
		super.setUndecorated(undecorated);
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "setUndecorated",
			new Object[] { getName(), undecorated });
	}

	@Override
	public void setTransparent(boolean isTransparent)
	{
		super.setTransparent(isTransparent);
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "setTransparent",
			new Object[] { getName(), isTransparent });
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
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "hide", new Object[] { getName() });

		// resume
		if (windowType == JSWindow.MODAL_DIALOG && getApplication().getWebsocketSession().getEventDispatcher() != null)
		{
			getApplication().getWebsocketSession().getEventDispatcher().resume(this);
		}
	}

	@Override
	public void setStoreBounds(boolean storeBounds)
	{
		super.setStoreBounds(storeBounds);
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "setStoreBounds",
			new Object[] { getName(), String.valueOf(storeBounds) });
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.scripting.RuntimeWindow#doOldShow(java.lang.String, boolean, boolean)
	 */
	@Override
	protected void doOldShow(String formName, boolean closeAll, boolean legacyV3Behavior)
	{
		getApplication().getFormManager().showFormInContainer(formName, this, getTitle(), true, windowName);
		IWebFormController controller = getApplication().getFormManager().getForm(formName);
		if (controller != null)
		{
			controller.getFormUI().setParentWindowName(getName());
		}
		Map<String, Object> arguments = new HashMap<String, Object>();
		//arguments.put("title", getTitle());
		Form form = getApplication().getFlattenedSolution().getForm(formName);
		arguments.put("form", form.getName());
		/* arguments.put("windowType", windowType); */
		Map<String, Integer> size = new HashMap<>();
		size.put("width", form.getSize().width);
		size.put("height", form.getSize().height);
		arguments.put("formSize", size);

		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "show", new Object[] { getName(), arguments });
		visible = true;
		this.formName = formName;
		if (windowType == JSWindow.MODAL_DIALOG && getApplication().getWebsocketSession().getEventDispatcher() != null)
		{
			getApplication().getWebsocketSession().getEventDispatcher().suspend(this);
		}
	}

	private void switchForm(IWebFormController currentForm)
	{
		visible = true;
		currentForm.getFormUI().setParentWindowName(getName());
		Map<String, Object> mainForm = new HashMap<String, Object>();
		mainForm.put("templateURL", currentForm.getForm().getName());
		mainForm.put("width", Integer.valueOf(currentForm.getForm().getWidth()));
		mainForm.put("name", currentForm.getName());

		Map<String, Object> navigatorForm = new HashMap<String, Object>();
		int navigatorId = currentForm.getForm().getNavigatorID();
		if (currentForm.getFormUI() instanceof WebGridFormUI && navigatorId == Form.NAVIGATOR_DEFAULT)
		{
			navigatorId = Form.NAVIGATOR_NONE;
		}
		switch (navigatorId)
		{
			case Form.NAVIGATOR_NONE :
			{
				// just make it an empty object.
				navigatorForm.put("width", 0);
				break;
			}
			case Form.NAVIGATOR_DEFAULT :
			{
				navigatorForm.put("templateURL", "servoydefault/navigator/default_navigator_container.html");
				navigatorForm.put("width", 70);
				break;
			}
			case Form.NAVIGATOR_IGNORE :
			{
				// just leave what it is now.
				break;
			}
			default :
			{
				Form navForm = getApplication().getFlattenedSolution().getForm(navigatorId);
				if (navForm != null)
				{
					getApplication().getFormManager().getForm(navForm.getName()).getFormUI().setParentWindowName(getName());
					navigatorForm.put("templateURL", navForm.getName());
					navigatorForm.put("width", Integer.valueOf(navForm.getWidth()));
					getApplication().getWebsocketSession().touchForm(getApplication().getFlattenedSolution().getFlattenedForm(navForm), null, true);
				}
			}
		}
		getApplication().getWebsocketSession().touchForm(currentForm.getForm(), null, true);
		getApplication().getWebsocketSession().executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "switchForm",
			new Object[] { getName(), mainForm, navigatorForm });
		sendTitle(title);
	}
}
