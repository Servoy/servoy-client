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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.websocket.CurrentWindow;

import com.servoy.j2db.IBasicFormManager.History;
import com.servoy.j2db.IBasicMainContainer;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.server.ngclient.component.WebFormController;
import com.servoy.j2db.server.shared.IPerfomanceRegistry;
import com.servoy.j2db.server.shared.PerformanceData;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.UUID;
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
	private Integer navigatorID = null;

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

	@Override
	public String getContainerName()
	{
		return getName();
	}

	@Override
	public IWebFormController getController()
	{
		if (formName == null) return null;
		return getApplication().getFormManager().getForm(formName);
	}

	public IWebFormController getNavigator()
	{
		if (navigatorID != null && navigatorID > 0)
		{
			Form navigatorForm = getApplication().getFlattenedSolution().getForm(navigatorID);
			navigatorID = null;
			if (navigatorForm != null)
			{
				return getApplication().getFormManager().getForm(navigatorForm.getName());
			}
		}
		return null;
	}

	public void setNavigator(Integer navigatorID)
	{
		this.navigatorID = navigatorID;
	}

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

	@Override
	public History getHistory()
	{
		return history;
	}


	@Override
	public void resetBounds()
	{
		this.storeBounds = false;
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("resetBounds",
			new Object[] { this.getName() });

	}

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
			getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("setLocation",
				new Object[] { this.getName(), location });
		}
	}

	public void updateLocation(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	@Override
	public int getX()
	{
		return x;
	}

	@Override
	public int getY()
	{
		return y;
	}

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
			getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("setSize",
				new Object[] { this.getName(), size });
		}
	}

	public void updateSize(int width, int height)
	{
		this.width = width;
		this.height = height;
	}

	@Override
	public int getWidth()
	{
		updateSizeIfNeeded();
		return width;
	}

	@Override
	public int getHeight()
	{
		updateSizeIfNeeded();
		return height;
	}

	private void updateSizeIfNeeded()
	{
		if (width == -1 || height == -1)
		{
			try
			{
				Object result = getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeServiceCall("getSize",
					new Object[] { this.getName() });

				if (result instanceof JSONObject)
				{
					updateSize(((JSONObject)result).optInt("width", -1), ((JSONObject)result).optInt("height", -1));
				}
			}
			catch (IOException ex)
			{
				Debug.warn(ex);
			}
		}
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
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("setInitialBounds",
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
		if (windowType == JSWindow.WINDOW)
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
			String appName = "Servoy NG Client"; //$NON-NLS-1$
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
			titleString = getApplication().getI18NMessageIfPrefixed(title);
		}
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("setTitle",
			new Object[] { this.getName(), titleString });
	}

	@Override
	public boolean isVisible()
	{
		return visible;
	}

	@Override
	public void toFront()
	{
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("toFront",
			new Object[] { this.getName() });

	}

	@Override
	public void toBack()
	{
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("toBack",
			new Object[] { this.getName() });
	}

	@Override
	public Object getWrappedObject()
	{
		return this;
	}

	@Override
	public void setOpacity(float opacity)
	{
		super.setOpacity(opacity);
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("setOpacity",
			new Object[] { getName(), opacity });
	}

	@Override
	public void setResizable(boolean resizable)
	{
		super.setResizable(resizable);
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("setResizable",
			new Object[] { getName(), resizable });
	}

	@Override
	public void setUndecorated(boolean undecorated)
	{
		super.setUndecorated(undecorated);
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("setUndecorated",
			new Object[] { getName(), undecorated });
	}

	@Override
	public void setTransparent(boolean isTransparent)
	{
		super.setTransparent(isTransparent);
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("setTransparent",
			new Object[] { getName(), isTransparent });
	}

	@Override
	public void hideUI()
	{
		visible = false;
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("hide",
			new Object[] { getName() });

		// assume that just after hiding the window, currentController in js is the main window controller
		JSWindow parent = getParent();
		IFormController formController = null;
		if (parent != null)
		{
			formController = parent.getImpl().getController();
			getApplication().getRuntimeWindowManager().setCurrentWindowName(parent.getImpl().getName());
		}
		else if (getApplication().getRuntimeWindowManager().getMainApplicationWindow() != null)
		{
			if (getApplication().getRuntimeWindowManager().getMainApplicationWindow() != null)
				formController = getApplication().getRuntimeWindowManager().getMainApplicationWindow().getController();
		}
		if (formController instanceof IWebFormController) getApplication().getFormManager().setCurrentControllerJS((IWebFormController)formController);

		// resume
		if (windowType == JSWindow.MODAL_DIALOG && getApplication().getWebsocketSession().getEventDispatcher() != null)
		{
			final IEventDispatcher eventDispatcher = getApplication().getWebsocketSession().getEventDispatcher();
			if (eventDispatcher.isEventDispatchThread())
			{
				eventDispatcher.resume(this);
			}
			else
			{
				eventDispatcher.addEvent(new Runnable()
				{
					@Override
					public void run()
					{
						eventDispatcher.resume(NGRuntimeWindow.this);
					}
				});
			}
		}
	}

	@Override
	public void setStoreBounds(boolean storeBounds)
	{
		super.setStoreBounds(storeBounds);
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("setStoreBounds",
			new Object[] { getName(), String.valueOf(storeBounds) });
	}

	@Override
	protected void doOldShow(String formName, boolean closeAll, boolean legacyV3Behavior)
	{
		String currentWindowName = getApplication().getRuntimeWindowManager().getCurrentWindowName();
		IWebFormController controller = getApplication().getFormManager().getForm(formName);
		if (controller != null)
		{
			getApplication().getFormManager().showFormInContainer(formName, this, getTitle(), true, windowName);
			this.formName = formName;
			controller.getFormUI().setParentWindowName(getName());
			//show panel as main
			switchForm(controller);
		}

		String titleArg = getTitle();
		titleArg = titleArg == null ? formName : titleArg;
		titleArg = getApplication().getI18NMessageIfPrefixed(titleArg);
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("show",
			new Object[] { getName(), formName, titleArg });

		if (windowType == JSWindow.MODAL_DIALOG && getApplication().getWebsocketSession().getEventDispatcher() != null)
		{
			try
			{
				try
				{
					CurrentWindow.get().sendChanges();
				}
				catch (IOException e)
				{
					Debug.log(e);
				}

				IPerfomanceRegistry perfRegistry = null;
				try
				{
					perfRegistry = (getApplication().getApplicationServerAccess() != null
						? getApplication().getApplicationServerAccess().getFunctionPerfomanceRegistry() : null);
				}
				catch (RemoteException e)
				{
					Debug.error(e);
				}

				Pair<UUID, UUID> perfId = null;
				PerformanceData performanceData = null;
				if (perfRegistry != null)
				{
					performanceData = perfRegistry.getPerformanceData(getApplication().getSolutionName());
					perfId = performanceData != null ? performanceData.startSubAction("$windowService.show", System.currentTimeMillis(),
						IDataServer.METHOD_CALL_WAITING_FOR_USER_INPUT, getApplication().getClientID()) : null;
				}
				try
				{
					getApplication().getWebsocketSession().getEventDispatcher().suspend(this, IEventDispatcher.EVENT_LEVEL_DEFAULT,
						IEventDispatcher.NO_TIMEOUT);
				}
				finally
				{
					if (perfId != null) performanceData.endSubAction(perfId);
				}

				// this is now a hide of this window, set back the window name just before this show.
				getApplication().getRuntimeWindowManager().setCurrentWindowName(currentWindowName);
			}
			catch (CancellationException e)
			{
				throw e; // full browser refresh while waiting for modal to close?
			}
			catch (TimeoutException e)
			{
				Debug.error("Modal dialog suspend timed out. This should never happen");
			}
		}
	}

	private void switchForm(IWebFormController currentForm)
	{
		visible = true;
		// set the parent and current window ,
		currentForm.getFormUI().setParentWindowName(getName());
		getApplication().getFormManager().getFormAndSetCurrentWindow(formName);
		Map<String, Object> mainForm = new HashMap<String, Object>();
		Map<String, Integer> size = new HashMap<>();
		size.put("width", currentForm.getForm().getSize().width);
		size.put("height", currentForm.getForm().getSize().height);
		mainForm.put("size", size);
		mainForm.put("name", currentForm.getName());

		Map<String, Object> navigatorForm = getNavigatorProperties(currentForm);
		NGClientWindow.getCurrentWindow().touchForm(currentForm.getForm(), currentForm.getName(), true, false);
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("switchForm",
			new Object[] { getName(), mainForm, navigatorForm });
		sendTitle(title);
	}

	/**
	 * Get the navigator properties based on the navigatorID of the form.
	 * @param formController
	 * @return a map which contains navigator properties such as name, size.
	 */
	private Map<String, Object> getNavigatorProperties(IWebFormController formController)
	{
		Map<String, Object> navigatorForm = new HashMap<String, Object>();
		int navigatorId = formController.getForm().getNavigatorID();
		if (formController.getFormUI() instanceof WebListFormUI && navigatorId == Form.NAVIGATOR_DEFAULT)
		{
			navigatorId = Form.NAVIGATOR_NONE;
		}
		switch (navigatorId)
		{
			case Form.NAVIGATOR_NONE :
				break;
			case Form.NAVIGATOR_DEFAULT :
			{
				navigatorForm.put("name", "servoycore/navigator/default_navigator_container.html");
				Map<String, Integer> navSize = new HashMap<>();
				navSize.put("width", 70);
				navigatorForm.put("size", navSize);
				break;
			}
			case Form.NAVIGATOR_IGNORE :
			{
				if (history.getIndex() > 0)
				{
					String prevForm = history.getFormName(history.getIndex() - 1);
					if (prevForm != null)
					{
						navigatorForm = getApplication().getFormManager().getForm(prevForm).getNavigatorProperties();
					}
				}
				break;
			}
			default :
			{
				Form navForm = getApplication().getFlattenedSolution().getForm(navigatorId);
				if (navForm != null)
				{
					getApplication().getFormManager().getForm(navForm.getName()).getFormUI().setParentWindowName(getName());
					navigatorForm.put("name", navForm.getName());
					Map<String, Integer> navSize = new HashMap<>();
					navSize.put("width", navForm.getSize().width);
					navSize.put("height", navForm.getSize().height);
					navigatorForm.put("size", navSize);
					NGClientWindow.getCurrentWindow().touchForm(getApplication().getFlattenedSolution().getFlattenedForm(navForm), null, true, false);
				}
			}
		}


		if (navigatorForm.isEmpty()) // Form.NAVIGATOR_NONE
		{
			// just make it an empty object.
			Map<String, Integer> navSize = new HashMap<>();
			navSize.put("width", 0);
			navigatorForm.put("size", navSize);
		}

		formController.setNavigatorProperties(navigatorForm);
		return navigatorForm;
	}

	@Override
	public void destroy()
	{
		super.destroy();
		if (visible) hideUI();

		IWebFormController controller = getController();
		if (controller != null && controller.getFormUI().getParentWindowName() == getName())
		{
			controller.getFormUI().setParentWindowName(null);
		}

		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("destroy",
			new Object[] { getName() });
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.scripting.RuntimeWindow#setCSSClass(java.lang.String)
	 */
	@Override
	public void setCSSClass(String cssClassName)
	{
		getApplication().getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("setCSSClassName",
			new Object[] { getName(), cssClassName });
	}
}
