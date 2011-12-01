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

package com.servoy.j2db.server.headlessclient;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequestCycle;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IWebClientApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.plugins.IWebRuntimeWindow;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.server.headlessclient.dataui.WebDefaultRecordNavigator;

/**
 * Web implementation of the JSWindow.
 * @author acostescu
 * @since 6.0
 */
public class WebRuntimeWindow extends RuntimeWindow implements IWebRuntimeWindow
{
	protected final IWebClientApplication application;
	boolean firstShow = true;

	public WebRuntimeWindow(IWebClientApplication application, String windowName, int windowType, RuntimeWindow parentWindow)
	{
		super(application, windowName, windowType, parentWindow);
		this.application = application;
	}

	@Override
	protected void doOldShow(String formName, boolean closeAll, boolean legacyV3Behavior)
	{
		FormManager fm = (FormManager)application.getFormManager();
		IMainContainer parentContainer = getParentContainerForShow(fm);
		if (((WebRequestCycle)RequestCycle.get()).getWebRequest().isAjax() && !((MainPage)parentContainer).isShowPageInDialogDelayed() &&
			!((MainPage)parentContainer).isPopupClosing())
		{
			IMainContainer dialogContainer = fm.getOrCreateMainContainer(windowName);
			// In case this modal dialog wants to show another modal dialog during onStart event, we make sure it
			// will be showed in postponed mode. Otherwise a stack of nested modal dialogs will not display OK.
			((MainPage)dialogContainer).setShowPageInDialogDelayed(true);

			if (formName != null)
			{
				final FormController fp = fm.showFormInMainPanel(formName, dialogContainer, title, closeAll || !legacyV3Behavior, windowName);
				if (fp != null && fp.getName().equals(formName) && dialogContainer != parentContainer)
				{
					Rectangle r2;
					if (FormManager.FULL_SCREEN.equals(initialBounds))
					{
						r2 = initialBounds;
					}
					else
					{
						r2 = getSizeAndLocation(initialBounds, dialogContainer, fp);
						if (Application.get().getDebugSettings().isAjaxDebugModeEnabled())
						{
							r2.height += 40;
						}
					}

					if (windowType == JSWindow.WINDOW)
					{
						((MainPage)parentContainer).showPopupWindow((MainPage)dialogContainer, title, r2, resizable, closeAll || !legacyV3Behavior);
					}
					else
					{
						((MainPage)parentContainer).showPopupDiv((MainPage)dialogContainer, title, r2, resizable, closeAll || !legacyV3Behavior,
							(windowType == JSWindow.MODAL_DIALOG), firstShow);
						firstShow = false;
					}
				}
			}
		}
		else
		{
			((MainPage)parentContainer).setShowPageInDialogDelayed(windowType, formName, initialBounds, title, resizable, showTextToolbar, closeAll, windowName);
		}
		if (getTitle() != null) setTitle(getTitle());
	}

	private IMainContainer getParentContainerForShow(FormManager fm)
	{
		IMainContainer parentContainer = null;
		if (initialParentWindow != null && initialParentWindow.isVisible()) parentContainer = fm.getMainContainer(initialParentWindow.getName());
		if (parentContainer == null) parentContainer = fm.getCurrentContainer();
		return parentContainer;
	}

	@Override
	public int getHeight()
	{
		MainPage mp = getMainPage();
		if (mp == null) mp = (MainPage)((FormManager)application.getFormManager()).getMainContainer(null);
		if (mp != null) return mp.getHeight(); // can never be null normally...
		else return 0;
	}

	@Override
	public int getWidth()
	{
		MainPage mp = getMainPage();
		if (mp == null) mp = (MainPage)((FormManager)application.getFormManager()).getMainContainer(null);
		if (mp != null) return mp.getWidth(); // can never be null normally...
		else return 0;
	}

	@Override
	public int getX()
	{
		MainPage mp = getMainPage();
		if (mp == null) mp = (MainPage)((FormManager)application.getFormManager()).getMainContainer(null);
		if (mp != null) return mp.getX(); // can never be null normally...
		else return 0;
	}

	@Override
	public int getY()
	{
		MainPage mp = getMainPage();
		if (mp == null) mp = (MainPage)((FormManager)application.getFormManager()).getMainContainer(null);
		if (mp != null) return mp.getY(); // can never be null normally...
		else return 0;
	}

	@Override
	public void hideUI()
	{
		MainPage mp = getMainPage();
		if (mp != null) mp.close();
	}

	@Override
	public void setTitle(String title)
	{
		MainPage mp = getMainPage();
		if (mp != null) mp.setTitle(title);
		super.setTitle(title);
	}

	@Override
	public Object getWrappedObject()
	{
		return null; // not yet used in WC
	}

	@Override
	public void setLocation(int x, int y)
	{
		initialBounds.x = x;
		initialBounds.y = y;
		MainPage dialogContainer = (MainPage)((FormManager)application.getFormManager()).getOrCreateMainContainer(windowName);
		if (windowType == JSWindow.WINDOW)
		{
			if (dialogContainer != null && dialogContainer.isShowingInWindow())
			{
				dialogContainer.appendJavaScriptChanges("window.moveTo(" + x + "," + y + ");");
			}
		}
		else
		{
			if (dialogContainer != null && dialogContainer.isShowingInDialog())
			{
				dialogContainer.setDialogBounds(windowName, x, y, -1, -1);
			}
		}
	}

	@Override
	public void setSize(int width, int height)
	{
		initialBounds.width = width;
		initialBounds.height = height;
		MainPage dialogContainer = (MainPage)((FormManager)application.getFormManager()).getOrCreateMainContainer(windowName);
		if (windowType == JSWindow.WINDOW)
		{
			if (dialogContainer != null && dialogContainer.isShowingInWindow())
			{
				dialogContainer.appendJavaScriptChanges("window.resizeTo(" + width + "," + height + ");");
			}
		}
		else
		{
			if (dialogContainer != null && dialogContainer.isShowingInDialog())
			{
				dialogContainer.setDialogBounds(windowName, -1, -1, width, height);
			}
		}
	}

	@Override
	public void toBack()
	{
		MainPage mp = getMainPage();
		if (mp != null)
		{
			mp.toBack();
		}
	}

	@Override
	public void toFront()
	{
		MainPage mp = getMainPage();
		if (mp != null)
		{
			mp.toFront();
		}
	}

	@Override
	public boolean isVisible()
	{
		MainPage mp = getMainPage();
		if (mp != null)
		{
			return (mp.getController() != null) ? mp.getController().isFormVisible() : false;
		}
		return false;
	}

	private Rectangle getSizeAndLocation(Rectangle r, IMainContainer container, final FormController fp)
	{
		int navid = fp.getForm().getNavigatorID();
		Dimension size = new Dimension(fp.getForm().getSize());
		if (navid == Form.NAVIGATOR_DEFAULT && fp.getForm().getView() != FormController.TABLE_VIEW &&
			fp.getForm().getView() != FormController.LOCKED_TABLE_VIEW)
		{
			size.width += WebDefaultRecordNavigator.DEFAULT_WIDTH;
			if (size.height < WebDefaultRecordNavigator.DEFAULT_HEIGHT_WEB) size.height = WebDefaultRecordNavigator.DEFAULT_HEIGHT_WEB;
		}
		else if (navid != Form.NAVIGATOR_NONE)
		{
			FormController currentNavFC = container.getNavigator();
			if (currentNavFC != null)
			{
				size.width += currentNavFC.getForm().getSize().width;
				int navHeight = currentNavFC.getForm().getSize().height;
				if (size.height < navHeight) size.height = navHeight;
			}
		}

		// Why 22 here? From Wicket CSS:
		// "div.wicket-modal div.w_right_1" brings 10px through "margin-left" property
		// "div.wicket-modal div.w_right_1" brings 10px through "margin-right" property
		// "div.wicket-modal div.w_right_1" brings 2px through "border" property (1px from left and 1px from right)
		size.width += 22;

		Rectangle r2 = new Rectangle(size);
		if (r != null)
		{
			if (r.height > 0)
			{
				r2.height = r.height;
			}
			if (r.width > 0)
			{
				r2.width = r.width;
			}
			r2.x = r.x;
			r2.y = r.y;
		}
		return r2;
	}

	protected MainPage getMainPage()
	{
		return (MainPage)((FormManager)application.getFormManager()).getMainContainer(windowName);
	}

}